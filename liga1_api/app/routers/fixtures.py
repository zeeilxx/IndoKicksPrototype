from fastapi import APIRouter, Query, HTTPException
from typing import Optional, Dict, Any

import httpx

from app.scraping.fixtures_scraper import scrape_fixtures_period

router = APIRouter()

FIXTURES_BASE = "https://www.ligaindonesiabaru.com/fixtures/index"

def season_candidates(season: int) -> list[str]:
    suffix = f"{season}-{(season + 1) % 100:02d}"
    if season >= 2025:
        return [f"BRI_SUPER_LEAGUE_{suffix}", f"bri%20super%20league%20{suffix}"]
    return [f"BRI_LIGA_1_{suffix}", f"bri%20liga%201%20{suffix}"]

async def resolve_competition_code(season: int) -> str:
    async with httpx.AsyncClient(timeout=15, follow_redirects=True) as client:
        for code in season_candidates(season):
            test_url = f"{FIXTURES_BASE}/{code}/1"
            r = await client.get(test_url, headers={"User-Agent": "bri-liga-scraper/2.0"})
            if r.status_code == 200:
                return code
    return season_candidates(season)[0]

@router.get("/fixtures")
async def get_fixtures(
    league: int = Query(274),
    season: int = Query(...),
    from_date: str = Query(..., alias="from", description="YYYY-MM-DD"),
    to_date: str = Query(..., alias="to", description="YYYY-MM-DD"),
    team: Optional[str] = Query(None, description="Team name or slug (e.g. persija or PERSIJA_JAKARTA)"),
) -> Dict[str, Any]:
    competition = await resolve_competition_code(season)

    try:
        items = await scrape_fixtures_period(
            competition_code=competition,
            season=season,
            from_date=from_date,
            to_date=to_date,
            team=team,
            max_week=38,
            ttl_seconds=600,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    params = {"league": str(league), "season": str(season), "from": from_date, "to": to_date}
    if team:
        params["team"] = team

    return {
        "get": "fixtures",
        "parameters": params,
        "errors": [] if items else [f"No fixtures found for competition={competition} in given period."],
        "results": len(items),
        "paging": {"current": 1, "total": 1},
        "response": items,
    }
