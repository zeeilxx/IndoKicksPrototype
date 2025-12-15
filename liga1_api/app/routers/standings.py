from fastapi import APIRouter, Query
from typing import Dict, Any
import httpx

from app.utils.cache import cache_get, cache_set
from app.scraping.standings_scraper import scrape_standings

router = APIRouter()

SITE_BASE = "https://www.ligaindonesiabaru.com"
TABLE_URL = f"{SITE_BASE}/table/index"

def season_candidates(season: int) -> list[str]:
    suffix = f"{season}-{(season + 1) % 100:02d}"
    # sesuai pattern yg kamu pakai sebelumnya
    if season >= 2025:
        return [f"BRI_SUPER_LEAGUE_{suffix}"]
    return [f"BRI_LIGA_1_{suffix}"]

async def resolve_competition_code_for_table(season: int) -> str:
    # cache 1 hari biar gak probe terus
    key = f"comp_table:{season}"
    cached = cache_get("comp_resolve", key)
    if cached:
        return cached

    cands = season_candidates(season)
    async with httpx.AsyncClient(timeout=15, follow_redirects=True) as client:
        for code in cands:
            test_url = f"{TABLE_URL}/{code}"
            r = await client.get(test_url, headers={"User-Agent": "bri-liga-scraper/3.0"})
            if r.status_code == 200:
                cache_set("comp_resolve", key, code, ttl_seconds=86400)
                return code

    # fallback (still cache)
    cache_set("comp_resolve", key, cands[0], ttl_seconds=86400)
    return cands[0]

@router.get("/standings")
async def get_standings(
    league: int = Query(274),
    season: int = Query(...),
) -> Dict[str, Any]:
    competition_code = await resolve_competition_code_for_table(season)
    return await scrape_standings(
        competition_code=competition_code,
        league_id=league,
        season=season,
        ttl_seconds=600,  # ✅ 10 menit cache
    )
