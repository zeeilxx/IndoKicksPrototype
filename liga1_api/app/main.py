from fastapi import FastAPI
from app.routers import fixtures, standings, teams, players

app = FastAPI(title="BRI Liga 1 Scraping API", version="0.1.0")

app.include_router(fixtures.router, prefix="", tags=["fixtures"])
app.include_router(standings.router, prefix="", tags=["standings"])
app.include_router(teams.router, prefix="", tags=["teams"])
app.include_router(players.router, prefix="", tags=["players"])

@app.get("/health")
def health():
    return {"status": "ok"}
