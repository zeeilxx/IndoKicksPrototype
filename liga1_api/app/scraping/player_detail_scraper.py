# app/scraping/player_detail_scraper.py
import re
import zlib
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import quote

from bs4 import BeautifulSoup

from app.utils.cache import cache_get, cache_set
from app.utils.http_client import fetch_html
from app.scraping.player_pic_scraper import extract_player_photo_url


SITE_BASE = "https://www.ligaindonesiabaru.com"
PLAYER_SINGLE_BASE = f"{SITE_BASE}/clubs/singleplayer"


def norm(s: str) -> str:
    return " ".join((s or "").split()).strip()


def stable_int32_id(s: str) -> int:
    u = zlib.crc32(s.encode("utf-8")) & 0xFFFFFFFF
    return u - 0x100000000 if u >= 0x80000000 else u


def season_to_range(season: int) -> str:
    # 2024 -> 2024-25
    return f"{season}-{(season + 1) % 100:02d}"


def player_comp_candidates(season: int) -> List[str]:
    # yang sering muncul: bri_liga_1_2024-25
    sfx = season_to_range(season)
    return [
        f"bri_liga_1_{sfx}",
        f"bri_super_league_{sfx}",
        f"bri_liga_1_{season}-{season+1}",  # fallback aneh (jaga2)
    ]


def build_player_url(player_slug: str, season: int, token: str) -> str:
    slug = player_slug.strip().lower()
    tok = token.strip()
    # NOTE: token perlu di-query param, kalau nggak kadang konten incomplete
    return f"{PLAYER_SINGLE_BASE}/{player_comp_candidates(season)[0]}/{quote(slug)}?token={quote(tok)}"


def is_real_player_page(html: str) -> bool:
    """
    Hindari false-positive login modal.
    Anggap valid kalau ada marker konten pemain yang jelas.
    """
    soup = BeautifulSoup(html or "", "lxml")
    text = soup.get_text(" ", strip=True).lower()

    # marker kuat
    markers = [
        "riwayat klub",
        "riwayat pemain",
        "statistik",
        "penampilan",
        "kartu kuning",
        "umpan sukses",
        "tekel",
        "intersepsi",
    ]
    return any(m in text for m in markers)


def find_table_after_heading(soup: BeautifulSoup, heading_text: str) -> Optional[Any]:
    """
    Cari <table> yang paling dekat setelah heading yang match heading_text
    """
    # cari elemen yang text-nya mengandung heading_text
    target = soup.find(string=re.compile(re.escape(heading_text), re.IGNORECASE))
    if not target:
        return None

    # naikkan ke parent yang masuk akal
    node = target.parent
    # cari table setelahnya
    table = node.find_next("table")
    return table


def parse_riwayat_klub(soup: BeautifulSoup) -> List[Dict[str, Any]]:
    table = find_table_after_heading(soup, "Riwayat Klub")
    if not table:
        return []

    rows = table.find_all("tr")
    if not rows:
        return []

    out: List[Dict[str, Any]] = []
    for tr in rows[1:]:
        tds = [norm(td.get_text(" ", strip=True)) for td in tr.find_all(["td", "th"])]
        if len(tds) < 4:
            continue

        # Struktur yang terlihat di page:
        # Kompetisi | Tahun | Klub | Penampilan | Gol | Penyelamatan | Kartu
        comp = tds[0] if len(tds) >= 1 else None
        year = tds[1] if len(tds) >= 2 else None
        club = tds[2] if len(tds) >= 3 else None
        apps = tds[3] if len(tds) >= 4 else None
        goals = tds[4] if len(tds) >= 5 else None
        saves = tds[5] if len(tds) >= 6 else None
        cards = tds[6] if len(tds) >= 7 else None

        def to_int(x: Optional[str]) -> Optional[int]:
            x = (x or "").strip()
            return int(x) if x.isdigit() else None

        out.append(
            {
                "competition": comp,
                "season_year": to_int(year),
                "club": club,
                "appearances": to_int(apps),
                "goals": to_int(goals),
                "saves": to_int(saves),
                "cards": cards,  # format biasanya "5/0"
            }
        )

    return out


def parse_riwayat_pemain(soup: BeautifulSoup, limit: int = 50) -> List[Dict[str, Any]]:
    table = find_table_after_heading(soup, "Riwayat Pemain")
    if not table:
        return []

    rows = table.find_all("tr")
    if len(rows) < 2:
        return []

    # cek header dulu biar yakin ini tabel pertandingan, bukan tabel riwayat klub
    header = [norm(th.get_text(" ", strip=True)) for th in rows[0].find_all(["th", "td"])]
    header_join = " ".join(header).lower()

    # kalau header mengandung "kompetisi" & "tahun" berarti itu tabel Riwayat Klub -> JANGAN parse sebagai match_history
    if ("kompetisi" in header_join) and ("tahun" in header_join):
        return []

    # match table biasanya punya kolom "Pertandingan" / "Match" / "Lawan"
    if not any(k in header_join for k in ["pertandingan", "match", "lawan"]):
        return []

    out: List[Dict[str, Any]] = []

    def to_int(x: str) -> Optional[int]:
        x = (x or "").strip()
        return int(x) if x.isdigit() else None

    for tr in rows[1:]:
        tds = [norm(td.get_text(" ", strip=True)) for td in tr.find_all(["td", "th"])]
        if len(tds) < 1:
            continue

        match = tds[0]
        goals = to_int(tds[1]) if len(tds) > 1 else None
        saves = to_int(tds[2]) if len(tds) > 2 else None
        cards = tds[3] if len(tds) > 3 else None

        out.append({"match": match, "goals": goals, "saves": saves, "cards": cards})

        if len(out) >= limit:
            break

    return out

NAV_NOISE_RE = re.compile(
    r"\b(MEDIA|BERITA|RILIS|SISI LAIN|FOTO|VIDEO|BOOKLET|LAPORAN|STATS|GALERI|DOKUMEN|WASIT|PUBLIKASI)\b",
    re.IGNORECASE,
)

def clean_club_value(x: Optional[str]) -> Optional[str]:
    if not x:
        return None
    x = norm(x)

    # buang noise nav/menu
    if NAV_NOISE_RE.search(x):
        # potong sebelum noise pertama
        x = NAV_NOISE_RE.split(x)[0]
        x = norm(x)

    # kalau masih kepanjangan / aneh, anggap invalid
    if len(x) > 40:
        return None

    return x.upper() if x else None


def dom_value_after_label(soup: BeautifulSoup, label: str) -> Optional[str]:
    """
    Cari text node yang mengandung label, lalu ambil text 'value' terdekat setelahnya.
    Works walau struktur div berubah-ubah.
    """
    # cari yang persis label / mengandung label
    node = soup.find(string=re.compile(rf"^\s*{re.escape(label)}\s*$", re.IGNORECASE))
    if not node:
        # fallback: label muncul bareng (mis: "Klub : PERSIJA")
        node = soup.find(string=re.compile(rf"\b{re.escape(label)}\b", re.IGNORECASE))
        if not node:
            return None

    el = node.parent

    # 1) coba sibling berikutnya
    sib = el.find_next_sibling()
    if sib:
        val = norm(sib.get_text(" ", strip=True))
        if val and val.lower() != label.lower():
            return val

    # 2) coba element setelah label (next element)
    nxt = el.find_next()
    if nxt and nxt is not el:
        val = norm(nxt.get_text(" ", strip=True))
        if val and val.lower() != label.lower() and label.lower() not in val.lower():
            return val

    # 3) fallback: parse format "Label : Value"
    txt = norm(el.get_text(" ", strip=True))
    m = re.search(rf"\b{re.escape(label)}\s*[:\-]\s*(.+)$", txt, re.IGNORECASE)
    if m:
        return norm(m.group(1))

    return None


def parse_basic_profile_from_dom(soup: BeautifulSoup) -> Dict[str, Any]:
    club_raw = dom_value_after_label(soup, "Klub")
    pos_raw = dom_value_after_label(soup, "Posisi")
    age_raw = dom_value_after_label(soup, "Usia")
    nat_raw = dom_value_after_label(soup, "Negara")

    club = clean_club_value(club_raw)

    age = None
    if age_raw:
        m = re.search(r"\d{1,2}", age_raw)
        age = int(m.group(0)) if m else None

    position = norm(pos_raw).upper() if pos_raw else None
    nationality = norm(nat_raw).title() if nat_raw else None

    full_name = None
    h = soup.find(["h1", "h2"])
    if h:
        full_name = norm(h.get_text(" ", strip=True))

    number = None
    t = soup.get_text(" ", strip=True)
    m = re.search(r"(#|No\.?)\s*(\d{1,3})\b", t, re.IGNORECASE)
    if m:
        number = int(m.group(2))

    return {
        "club": club,
        "position": position,
        "age": age,
        "nationality": nationality,
        "number": number,
        "full_name": full_name,
        "photo": None,  # will be filled after we know final_url
    }



def parse_key_stats(full_text: str) -> Dict[str, Any]:
    """
    Ambil statistik penting yang muncul di halaman:
    Penampilan, Gol, Assist, Tembakan, Tembakan Tepat Sasaran,
    Umpan sukses a/b, Tekel, Intersepsi, Sapuan, Kartu Kuning, Kartu Merah, Pelanggaran, Offside
    """
    t = norm(full_text)

    def get_int(label: str) -> Optional[int]:
        m = re.search(rf"\b{re.escape(label)}\s+(\d+)\b", t, re.IGNORECASE)
        return int(m.group(1)) if m else None

    def get_ratio(label: str) -> Optional[Dict[str, int]]:
        m = re.search(rf"\b{re.escape(label)}\s+(\d+)\s*/\s*(\d+)\b", t, re.IGNORECASE)
        if not m:
            return None
        return {"success": int(m.group(1)), "total": int(m.group(2))}

    stats: Dict[str, Any] = {
        "appearances": get_int("Penampilan"),
        "goals": get_int("Gol"),
        "assists": get_int("Assist"),
        "shots": get_int("Tembakan"),
        "shots_on_target": get_int("Tembakan Tepat Sasaran"),
        "passes": get_ratio("Umpan sukses"),
        "tackles": get_int("Tekel"),
        "interceptions": get_int("Intersepsi"),
        "clearances": get_int("Sapuan"),
        "yellow_cards": get_int("Kartu Kuning"),
        "red_cards": get_int("Kartu Merah"),
        "fouls": get_int("Pelanggaran"),
        "offsides": get_int("Offside"),
    }

    return stats


async def scrape_player_detail(
    player_slug: str,
    season: int,
    token: str,
    ttl_seconds: int = 1800,
) -> Dict[str, Any]:
    """
    Returns:
    {
      ok: bool,
      player_slug, season, token,
      profile: {...},
      key_stats: {...},
      club_history: [...],
      match_history: [...],
      _source: {...}
    }
    """
    slug = player_slug.strip().lower()
    tok = token.strip()
    if not tok:
        return {"ok": False, "error": "token is required", "_source": {}}

    ck = f"{season}:{slug}:{tok}"
    cached = cache_get("player_detail", ck)
    if cached is not None:
        return cached

    # coba beberapa comp code (biar season berubah tetap jalan)
    last_err = None
    final_url = None
    html = None
    used_url = None

    for comp in player_comp_candidates(season):
        url = f"{PLAYER_SINGLE_BASE}/{comp}/{quote(slug)}?token={quote(tok)}"
        resp = await fetch_html(url)
        final_url = resp.get("final_url")
        used_url = url

        if not resp["ok"]:
            last_err = resp["error"]
            continue

        if is_real_player_page(resp["text"]):
            html = resp["text"]
            break

        # kalau HTML ada tapi bukan halaman pemain, simpan snippet buat debug
        last_err = "HTML loaded but missing player markers (maybe blocked/changed layout)."
        html = resp["text"]  # keep for debug, but continue to other comp
        html = None

    if html is None:
        out = {
            "ok": False,
            "error": last_err or "Failed to load player page",
            "_source": {"player_url": used_url, "final_url": final_url},
        }
        cache_set("player_detail", ck, out, ttl_seconds=120)
        return out

    soup = BeautifulSoup(html, "lxml")
    full_text = soup.get_text("\n", strip=True)

    profile = parse_basic_profile_from_dom(soup)
    base_for_assets = final_url or used_url or SITE_BASE
    profile["photo"] = extract_player_photo_url(soup, base_url=base_for_assets)
    key_stats = parse_key_stats(full_text)
    club_history = parse_riwayat_klub(soup)
    match_history = parse_riwayat_pemain(soup, limit=60)

    # display name fallback
    display_name = profile.get("full_name") or slug.replace("_", " ").title()

    out = {
        "ok": True,
        "player_slug": slug,
        "season": season,
        "token": tok,
        "profile": profile,
        "key_stats": key_stats,
        "club_history": club_history,
        "match_history": match_history,
        "_source": {
            "player_url": used_url,
            "final_url": final_url,
        },
        "_debug": {
            "note": "If some fields None, site text/labels may differ per player.",
        },
    }
    cache_set("player_detail", ck, out, ttl_seconds=ttl_seconds)
    return out
