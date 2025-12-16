# app/routers/players.py
from typing import Any, Dict

from fastapi import APIRouter, HTTPException, Query

from app.scraping.player_detail_scraper import scrape_player_detail, stable_int32_id

router = APIRouter(tags=["players"])


@router.get("/players/{player_slug}")
async def players_detail(
    player_slug: str,
    league: int = Query(274),
    season: int = Query(..., ge=2000, le=2100),
    token: str = Query(..., min_length=3),
) -> Dict[str, Any]:
    if league != 274:
        raise HTTPException(status_code=400, detail="Only league=274 supported for now.")

    detail = await scrape_player_detail(player_slug=player_slug, season=season, token=token)
    if not detail.get("ok"):
        return {
            "get": "players",
            "parameters": {"league": str(league), "season": str(season), "id": player_slug},
            "errors": [detail.get("error", "Unknown error")],
            "results": 0,
            "paging": {"current": 1, "total": 1},
            "response": [],
            "_source": detail.get("_source"),
        }

    prof = detail["profile"]
    stats = detail["key_stats"]

    # bentuk mirip API-football style
    response_obj = {
        "player": {
            "id": stable_int32_id(f"PLAYER:{detail['season']}:{detail['player_slug']}:{detail['token']}"),
            "name": (prof.get("full_name") or detail["player_slug"].replace("_", " ").title()),
            "age": prof.get("age"),
            "nationality": prof.get("nationality"),
            "photo": prof.get("photo"),
        },
        "statistics": [
            {
                "team": {
                    "name": prof.get("club"),
                },
                "games": {
                    "number": prof.get("number"),
                    "position": prof.get("position"),
                    "appearences": stats.get("appearances"),
                },
                "goals": {
                    "total": stats.get("goals"),
                    "assists": stats.get("assists"),
                },
                "shots": {
                    "total": stats.get("shots"),
                    "on": stats.get("shots_on_target"),
                },
                "passes": stats.get("passes"),  # {success,total}
                "defence": {
                    "tackles": stats.get("tackles"),
                    "interceptions": stats.get("interceptions"),
                    "clearances": stats.get("clearances"),
                },
                "cards": {
                    "yellow": stats.get("yellow_cards"),
                    "red": stats.get("red_cards"),
                },
                "discipline": {
                    "fouls": stats.get("fouls"),
                    "offsides": stats.get("offsides"),
                },
                "history": {
                    "club_history": detail.get("club_history", []),
                    "match_history": detail.get("match_history", []),
                },
                "_source": detail.get("_source", {}),
            }
        ],
    }

    return {
        "get": "players",
        "parameters": {"league": str(league), "season": str(season), "id": player_slug},
        "errors": [],
        "results": 1,
        "paging": {"current": 1, "total": 1},
        "response": [response_obj],
    }
