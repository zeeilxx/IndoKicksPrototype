import re
import zlib
from typing import Any, Dict, List

from fastapi import APIRouter, Query, HTTPException
from bs4 import BeautifulSoup

from app.utils.cache import cache_get, cache_set
from app.utils.http_client import fetch_html
from app.scraping.teams_detail_scraper import scrape_team_detail
from app.scraping.team_squad_scraper import scrape_team_squad
from app.scraping.club_logo_scraper import resolve_club_logo_url


router = APIRouter(tags=["teams"])

SITE_BASE = "https://www.ligaindonesiabaru.com"
TABLE_URL = f"{SITE_BASE}/table/index"

def norm(s: str) -> str:
    return " ".join((s or "").split()).strip()

def stable_id(s: str) -> int:
    u = zlib.crc32(s.encode()) & 0xFFFFFFFF
    return u - 0x100000000 if u >= 0x80000000 else u

def season_to_range(season: int) -> str:
    return f"{season}-{(season + 1) % 100:02d}"

def parse_teams(html: str) -> List[Dict[str, Any]]:
    soup = BeautifulSoup(html, "lxml")
    teams = []

    for tr in soup.select("table tr"):
        cols = [norm(td.text) for td in tr.find_all("td")]
        if len(cols) < 2 or not cols[0].isdigit():
            continue

        name = cols[1]
        slug = re.sub(r"[^A-Z0-9]+", "_", name.upper()).strip("_")

        teams.append({
            "team": {
                "id": stable_id(slug),
                "name": name,
                "slug": slug,
                "country": "Indonesia",
                "founded": None,
                "national": False,
                "logo": None,
            }
        })
    return teams

@router.get("/teams")
async def teams_list(league: int = 274, season: int = Query(...)):
    if league != 274:
        raise HTTPException(400, "Only Liga 1 supported")

    comp = f"BRI_LIGA_1_{season_to_range(season)}"
    url = f"{TABLE_URL}/{comp}"
    resp = await fetch_html(url)

    if not resp["ok"]:
        return {"get": "teams", "results": 0, "response": []}

    return {
        "get": "teams",
        "parameters": {"league": league, "season": season},
        "results": len(parse_teams(resp["text"])),
        "response": parse_teams(resp["text"]),
    }

@router.get("/teams/{team_slug}")
async def team_detail(team_slug: str, league: int = 274, season: int = Query(...), include_players: bool = True):
    if league != 274:
        raise HTTPException(400, "Only Liga 1 supported")

    detail = await scrape_team_detail(team_slug, season)
    if not detail["ok"]:
        return {"get": "teams", "errors": [detail["error"]], "response": []}

    players = []
    if include_players:
        squad = await scrape_team_squad(team_slug.upper(), season)
        players = squad.get("players", [])

    meta = detail["meta"]
    slug = team_slug.upper()

    # ✅ Get club page URL from your team detail scraper source info
    src = detail.get("_source", {}) or {}
    club_page_url = src.get("final_url") or src.get("club_url")  # absolute URL expected

    # ✅ Resolve logo from the club page (uses your safe filters + caching)
    logo_url = await resolve_club_logo_url(club_page_url or "")

    return {
        "get": "teams",
        "parameters": {"league": league, "season": season, "id": slug},
        "results": 1,
        "response": [{
            "team": {
                "id": stable_id(slug),
                "name": meta.get("team_name") or slug,
                "slug": slug,
                "country": "Indonesia",
                "founded": meta.get("founded"),
                "national": False,
                "logo": logo_url,  
            },
            "coach": {"name": meta.get("coach")},
            "venue": {"name": meta.get("stadium"), "city": meta.get("location")},
            "players": players,
            "_source": {"club_url": club_page_url},
        }]
    }
