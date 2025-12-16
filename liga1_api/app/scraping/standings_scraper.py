import zlib
from datetime import datetime
from typing import Dict, Any, List, Optional
from urllib.parse import urljoin

import httpx
from bs4 import BeautifulSoup

from app.utils.cache import cache_get, cache_set
from app.scraping.club_logo_scraper import resolve_club_logo_url

SITE_BASE = "https://www.ligaindonesiabaru.com"
TABLE_URL = f"{SITE_BASE}/table/index"

WDL = {"W", "D", "L"}


def stable_int32_id(s: str) -> int:
    u = zlib.crc32(s.encode("utf-8")) & 0xFFFFFFFF
    return u - 0x100000000 if u >= 0x80000000 else u


def norm(s: str) -> str:
    return " ".join((s or "").split()).strip()


async def fetch_table_html(competition_code: str, ttl_seconds: int = 600) -> Optional[str]:
    key = f"table_html:{competition_code}"
    cached = cache_get("standings_html", key)
    if cached is not None:
        return cached

    url = f"{TABLE_URL}/{competition_code}"
    async with httpx.AsyncClient(
        timeout=20,
        follow_redirects=True,
        headers={"User-Agent": "bri-liga-scraper/3.0"},
    ) as client:
        r = await client.get(url)
        if r.status_code == 404:
            cache_set("standings_html", key, None, ttl_seconds=180)
            return None
        r.raise_for_status()

    cache_set("standings_html", key, r.text, ttl_seconds=ttl_seconds)
    return r.text


def _parse_table_rows(soup: BeautifulSoup) -> List[Dict[str, Any]]:
    """
    Parse from actual table rows (fast).
    Expected columns: Pos, Club, Main, Menang, Seri, Kalah, GM, GK, SG, Poin, Form
    Adds: club_href (relative href to club page) if found.
    """
    rows_out: List[Dict[str, Any]] = []

    tables = soup.find_all("table")
    for table in tables:
        for tr in table.find_all("tr"):
            tds = tr.find_all(["td", "th"])
            cols = [norm(td.get_text(" ", strip=True)) for td in tds]
            if len(cols) < 10:
                continue

            # rank must be integer
            if not cols[0].isdigit():
                continue
            rank = int(cols[0])

            # club text
            club = cols[1]
            if not club or club.lower() in {"club", "klub"}:
                continue

            # try capture href from the 2nd cell (club cell)
            club_href = None
            if len(tds) >= 2:
                a = tds[1].find("a", href=True)
                if a and a["href"]:
                    club_href = norm(a["href"])

            # numeric stats
            nums: List[int] = []
            for c in cols[2:]:
                parts = c.split()
                if parts and all(p in WDL for p in parts) and 3 <= len(parts) <= 10:
                    break
                if c.lstrip("-").isdigit():
                    nums.append(int(c))

            if len(nums) < 8:
                continue
            played, win, draw, lose, gf, ga, gd, pts = nums[:8]

            # form (optional)
            form = None
            last = cols[-1].split()
            if last and all(x in WDL for x in last):
                form = "".join(last)

            rows_out.append({
                "rank": rank,
                "club": club,
                "club_href": club_href,  # ✅ new
                "played": played,
                "win": win,
                "draw": draw,
                "lose": lose,
                "gf": gf,
                "ga": ga,
                "gd": gd,
                "pts": pts,
                "form": form
            })

    # dedupe by rank
    by_rank: Dict[int, Dict[str, Any]] = {}
    for r in rows_out:
        by_rank[r["rank"]] = r
    return [by_rank[k] for k in sorted(by_rank.keys())]


def _build_api_response(
    league_id: int,
    season: int,
    competition_code: str,
    rows: List[Dict[str, Any]],
    logo_map: Dict[str, Optional[str]],
    league_name: str = "Liga 1",
    country: str = "Indonesia"
) -> Dict[str, Any]:
    now_iso = datetime.utcnow().isoformat() + "Z"

    standings_items = []
    for r in rows:
        team_id = stable_int32_id(f"TEAM:{r['club'].upper()}")

        logo = None
        href = r.get("club_href")
        if href:
            logo = logo_map.get(href)

        standings_items.append({
            "rank": r["rank"],
            "team": {"id": team_id, "name": r["club"], "logo": logo},
            "points": r["pts"],
            "goalsDiff": r["gd"],
            "group": league_name,
            "form": r["form"],
            "status": "same",
            "description": None,
            "all": {
                "played": r["played"],
                "win": r["win"],
                "draw": r["draw"],
                "lose": r["lose"],
                "goals": {"for": r["gf"], "against": r["ga"]},
            },
            "home": None,
            "away": None,
            "update": now_iso
        })

    return {
        "get": "standings",
        "parameters": {"league": str(league_id), "season": str(season)},
        "errors": [],
        "results": 1,
        "paging": {"current": 1, "total": 1},
        "response": [{
            "league": {
                "id": league_id,
                "name": league_name,
                "country": country,
                "logo": None,
                "flag": None,
                "season": season,
                "standings": [standings_items],
                "_source": {"table_url": f"{TABLE_URL}/{competition_code}"}
            }
        }]
    }


async def scrape_standings(
    competition_code: str,
    league_id: int,
    season: int,
    ttl_seconds: int = 600
) -> Dict[str, Any]:
    # cache final JSON
    key = f"standings_json:{competition_code}:{league_id}:{season}"
    cached = cache_get("standings_json", key)
    if cached is not None:
        return cached

    html = await fetch_table_html(competition_code, ttl_seconds=ttl_seconds)
    if not html:
        out = {
            "get": "standings",
            "parameters": {"league": str(league_id), "season": str(season)},
            "errors": [f"Standings page not found for competition={competition_code}"],
            "results": 0,
            "paging": {"current": 1, "total": 1},
            "response": [],
        }
        cache_set("standings_json", key, out, ttl_seconds=120)
        return out

    soup = BeautifulSoup(html, "lxml")
    rows = _parse_table_rows(soup)

    if not rows:
        out = {
            "get": "standings",
            "parameters": {"league": str(league_id), "season": str(season)},
            "errors": [f"Could not parse standings table for competition={competition_code}"],
            "results": 0,
            "paging": {"current": 1, "total": 1},
            "response": [],
        }
        cache_set("standings_json", key, out, ttl_seconds=120)
        return out

    # ✅ Resolve club logos (based on club href from table)
    logo_map: Dict[str, Optional[str]] = {}
    for r in rows:
        href = r.get("club_href")
        if not href:
            continue
        if href in logo_map:
            continue
        logo_map[href] = await resolve_club_logo_url(href)

    out = _build_api_response(
        league_id=league_id,
        season=season,
        competition_code=competition_code,
        rows=rows,
        logo_map=logo_map
    )
    cache_set("standings_json", key, out, ttl_seconds=ttl_seconds)
    return out
