import re
from typing import Any, Dict, Optional
from urllib.parse import quote

from bs4 import BeautifulSoup

from app.utils.cache import cache_get, cache_set
from app.utils.http_client import fetch_html

SITE_BASE = "https://www.ligaindonesiabaru.com"
CLUB_SINGLE_BASE = f"{SITE_BASE}/clubs/single"


def norm(s: str) -> str:
    return " ".join((s or "").split()).strip()


def season_to_range(season: int) -> str:
    return f"{season}-{(season + 1) % 100:02d}"


def clubs_comp_code(season: int) -> str:
    return f"bri_liga_1_{season_to_range(season)}"


def build_club_url(team_slug: str, season: int) -> str:
    team_slug = team_slug.strip().upper()
    return f"{CLUB_SINGLE_BASE}/{clubs_comp_code(season)}/{quote(team_slug)}"


def is_real_login_page(html: str) -> bool:
    """
    Halaman club ada modal login → jangan false positive.
    Login page biasanya: tidak ada marker klub, tapi ada password form.
    """
    soup = BeautifulSoup(html or "", "lxml")
    text = soup.get_text(" ", strip=True).lower()
    lower_html = (html or "").lower()

    club_markers = ["berdiri", "pelatih", "stadion", "lokasi", "pemain", "ofisial", "jadwal", "hasil"]
    has_club_marker = any(m in text for m in club_markers)

    has_password = ('type="password"' in lower_html) or ("name=\"password\"" in lower_html)
    title_login = "<title>login" in lower_html

    return (not has_club_marker) and (has_password or title_login)


def _find_team_name(soup: BeautifulSoup, fallback_slug: str) -> str:
    h = soup.find(["h1", "h2"])
    if h:
        t = norm(h.get_text(" ", strip=True))
        if t:
            return t
    return fallback_slug.replace("_", " ")


def _to_int_year(s: Optional[str]) -> Optional[int]:
    if not s:
        return None
    m = re.search(r"(\d{4})", s)
    if not m:
        return None
    try:
        return int(m.group(1))
    except Exception:
        return None


def _kv_from_li(li_text: str, label: str) -> Optional[str]:
    """
    Ambil value dari text <li> yang mengandung label.
    Contoh li_text:
      "Pelatih: FABIO ARAUJO LEFUNDES"
      atau
      "Pelatih:\nFABIO ARAUJO LEFUNDES"
    """
    t = norm(li_text)
    if not t:
        return None

    # label match fleksibel (dengan/tanpa kolon)
    # mis: "Pelatih:" atau "Pelatih"
    pat = re.compile(rf"^\s*{re.escape(label)}\s*:?\s*(.*)$", re.IGNORECASE)
    m = pat.match(t)
    if m:
        v = norm(m.group(1))
        return v or None

    # kalau struktur newline: label di awal, value di baris berikutnya
    parts = [norm(x) for x in li_text.split("\n") if norm(x)]
    if parts:
        head = parts[0].rstrip(":").strip().lower()
        if head == label.lower():
            if len(parts) >= 2:
                return parts[1]
    return None


def _find_kv_anywhere(soup: BeautifulSoup, label: str) -> Optional[str]:
    """
    Cari label di:
    1) <li> (paling umum)
    2) fallback: elemen apapun yang textnya == label lalu ambil sibling text
    """
    # 1) scan <li>
    for li in soup.find_all("li"):
        raw = li.get_text("\n", strip=True)
        if label.lower() in raw.lower():
            v = _kv_from_li(raw, label)
            if v:
                return v

    # 2) fallback scan semua elemen (lebih lambat tapi aman)
    for el in soup.find_all(True):
        txt = norm(el.get_text(" ", strip=True))
        if not txt:
            continue
        if txt.rstrip(":").strip().lower() == label.lower():
            # ambil next sibling text yang non-empty
            sib = el.find_next_sibling()
            while sib is not None:
                v = norm(sib.get_text(" ", strip=True))
                if v:
                    return v
                sib = sib.find_next_sibling()

    return None


async def scrape_team_detail(team_slug: str, season: int, ttl_seconds: int = 1800) -> Dict[str, Any]:
    team_slug = team_slug.strip().upper()
    url = build_club_url(team_slug, season)

    ck = f"{season}:{team_slug}"
    cached = cache_get("team_detail", ck)
    if cached is not None:
        return cached

    resp = await fetch_html(url)
    if not resp["ok"]:
        out = {"ok": False, "error": resp["error"], "_source": {"club_url": url, "final_url": resp["final_url"]}}
        cache_set("team_detail", ck, out, ttl_seconds=60)
        return out

    html = resp["text"] or ""
    if is_real_login_page(html):
        out = {
            "ok": False,
            "error": "Blocked / served LOGIN HTML for club page (anti-bot/session).",
            "_debug": {"snippet": html[:350]},
            "_source": {"club_url": url, "final_url": resp["final_url"]},
        }
        cache_set("team_detail", ck, out, ttl_seconds=120)
        return out

    soup = BeautifulSoup(html, "lxml")

    team_name = _find_team_name(soup, team_slug)

    founded_txt = _find_kv_anywhere(soup, "Berdiri")
    coach_txt = _find_kv_anywhere(soup, "Pelatih")
    stadium_txt = _find_kv_anywhere(soup, "Stadion")
    location_txt = _find_kv_anywhere(soup, "Lokasi")

    meta = {
        "team_name": team_name,
        "founded": _to_int_year(founded_txt),
        "coach": coach_txt,
        "stadium": stadium_txt,
        "location": location_txt,
    }

    out = {
        "ok": True,
        "team_slug": team_slug,
        "season": season,
        "meta": meta,
        "_source": {"club_url": url, "final_url": resp["final_url"]},
    }
    cache_set("team_detail", ck, out, ttl_seconds=ttl_seconds)
    return out
