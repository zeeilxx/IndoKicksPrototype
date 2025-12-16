import re
import zlib
from typing import Any, Dict, Optional
from urllib.parse import quote

from bs4 import BeautifulSoup

from app.utils.cache import cache_get, cache_set
from app.utils.http_client import fetch_html

SITE_BASE = "https://www.ligaindonesiabaru.com"
CLUB_SINGLE_BASE = f"{SITE_BASE}/clubs/single"

PLAYER_DETAIL_HREF_RE = re.compile(r"/clubs/singleplayer/", re.IGNORECASE)

CARD_RE = re.compile(
    r"(?P<number>\d{1,3})\s+(?P<name>.+?)\s+Negara\s+(?P<nation>.+?)\s+Penampilan\s+(?P<apps>\d+)\s+Usia\s+(?P<age>\d+)",
    re.IGNORECASE,
)


def norm(s: str) -> str:
    return " ".join((s or "").split()).strip()


def stable_int32_id(s: str) -> int:
    u = zlib.crc32(s.encode("utf-8")) & 0xFFFFFFFF
    return u - 0x100000000 if u >= 0x80000000 else u


def season_to_range(season: int) -> str:
    return f"{season}-{(season + 1) % 100:02d}"


def clubs_comp_code(season: int) -> str:
    return f"bri_liga_1_{season_to_range(season)}"


def build_club_url(team_slug: str, season: int) -> str:
    team_slug = team_slug.strip().upper()
    return f"{CLUB_SINGLE_BASE}/{clubs_comp_code(season)}/{quote(team_slug)}"


def abs_url(href: str) -> str:
    href = (href or "").strip()
    if not href:
        return ""
    if href.startswith("http"):
        return href
    if href.startswith("/"):
        return f"{SITE_BASE}{href}"
    return f"{SITE_BASE}/{href}"


def is_real_login_page(html: str) -> bool:
    soup = BeautifulSoup(html or "", "lxml")
    text = soup.get_text(" ", strip=True).lower()
    lower_html = (html or "").lower()

    club_markers = ["berdiri", "pelatih", "stadion", "lokasi", "pemain", "ofisial"]
    has_club_marker = any(m in text for m in club_markers)

    has_password = ('type="password"' in lower_html) or ("name=\"password\"" in lower_html)
    title_login = "<title>login" in lower_html

    return (not has_club_marker) and (has_password or title_login)


def parse_player_card_text(text: str) -> Optional[Dict[str, Any]]:
    t = norm(text)
    m = CARD_RE.search(t)
    if not m:
        return None
    return {
        "number": int(m.group("number")),
        "name": norm(m.group("name")),
        "nationality": norm(m.group("nation")).title(),
        "apps": int(m.group("apps")),
        "age": int(m.group("age")),
    }


async def scrape_team_squad(team_slug: str, season: int, ttl_seconds: int = 1800) -> Dict[str, Any]:
    team_slug = team_slug.strip().upper()
    url = build_club_url(team_slug, season)

    ck = f"{season}:{team_slug}"
    cached = cache_get("team_squad", ck)
    if cached is not None:
        return cached

    resp = await fetch_html(url)
    if not resp["ok"]:
        out = {"ok": False, "error": resp["error"], "players": [], "_source": {"club_url": url, "final_url": resp["final_url"]}}
        cache_set("team_squad", ck, out, ttl_seconds=60)
        return out

    html = resp["text"] or ""
    if is_real_login_page(html):
        out = {
            "ok": False,
            "error": "Blocked / served LOGIN HTML for club page (anti-bot/session).",
            "players": [],
            "_debug": {"snippet": html[:350]},
            "_source": {"club_url": url, "final_url": resp["final_url"]},
        }
        cache_set("team_squad", ck, out, ttl_seconds=120)
        return out

    soup = BeautifulSoup(html, "lxml")

    # link detail pemain (paling reliable)
    player_links = []
    for a in soup.find_all("a", href=True):
        if PLAYER_DETAIL_HREF_RE.search(a["href"] or ""):
            player_links.append(a)

    uniq: Dict[int, Dict[str, Any]] = {}

    for a in player_links:
        detail_url = abs_url(a.get("href") or "")
        pid = stable_int32_id(detail_url)

        container = a.find_parent(["div", "li", "section"]) or a.parent
        card_text = container.get_text(" ", strip=True) if container else a.get_text(" ", strip=True)

        parsed = parse_player_card_text(card_text)
        if not parsed:
            parsed = {
                "number": None,
                "name": norm(a.get_text(" ", strip=True)) or None,
                "nationality": None,
                "apps": None,
                "age": None,
            }

        uniq[pid] = {
            "player": {
                "id": pid,
                "name": parsed.get("name"),
                "age": parsed.get("age"),
                "nationality": parsed.get("nationality"),
                "photo": None,
            },
            "statistics": [
                {
                    "games": {
                        "appearences": parsed.get("apps"),
                        "number": parsed.get("number"),
                        "position": None,
                    },
                    "_source": {"detail_url": detail_url},
                }
            ],
        }

    players = list(uniq.values())
    players.sort(key=lambda x: (x["statistics"][0]["games"]["number"] is None, x["statistics"][0]["games"]["number"] or 9999))

    out = {
        "ok": True,
        "team_slug": team_slug,
        "season": season,
        "players": players,
        "_source": {"club_url": url, "final_url": resp["final_url"], "found_player_links": len(player_links)},
    }
    cache_set("team_squad", ck, out, ttl_seconds=ttl_seconds)
    return out
