import re
import zlib
from datetime import datetime, date
from zoneinfo import ZoneInfo
from typing import List, Optional, Dict, Any, Tuple

import httpx
from bs4 import BeautifulSoup

from app.utils.cache import cache_get, cache_set

JAKARTA_TZ = ZoneInfo("Asia/Jakarta")

FIXTURES_BASE = "https://www.ligaindonesiabaru.com/fixtures/index"
SITE_BASE = "https://www.ligaindonesiabaru.com"

# /result/detail/<comp>/<YYYY-MM-DD>/<HOME_SLUG>/<AWAY_SLUG>
DATE_IN_URL = re.compile(r"/result/detail/[^/]+/(?P<date>\d{4}-\d{2}-\d{2})/")
TEAMS_IN_URL = re.compile(
    r"/result/detail/[^/]+/\d{4}-\d{2}-\d{2}/(?P<home>[^/]+)/(?P<away>[^/]+)$"
)

TIME_IN_TEXT = re.compile(r"\b(?P<h>\d{1,2}):(?P<m>\d{2})\b")
STATUS_IN_TEXT = re.compile(r"\b(FT|HT|PST|AET|PEN)\b")

# Match detail parsing (for score, kickoff, venue/city)
SCORE_RE = re.compile(r"\b(?P<hs>\d{1,2})\s*[:\-]\s*(?P<as>\d{1,2})\b")
KICKOFF_RE = re.compile(r"Kick\s*Off\s*(?P<h>\d{1,2}):(?P<m>\d{2})\s*WIB", re.IGNORECASE)

def normalize_spaces(s: str) -> str:
    return " ".join((s or "").split()).strip()

def slug_to_name(slug: str) -> str:
    return normalize_spaces(slug.replace("_", " "))

def normalize_team_query(q: str) -> str:
    return normalize_spaces(q).replace("_", " ").lower()

def match_team(team_query: str, home_slug: str, away_slug: str, home_name: str, away_name: str) -> bool:
    tq = normalize_team_query(team_query)
    candidates = {
        home_slug.replace("_", " ").lower(),
        away_slug.replace("_", " ").lower(),
        home_name.lower(),
        away_name.lower(),
    }
    return any(tq == c or tq in c or c in tq for c in candidates)

def stable_int32_id(s: str) -> int:
    u = zlib.crc32(s.encode("utf-8")) & 0xFFFFFFFF
    return u - 0x100000000 if u >= 0x80000000 else u

def parse_yyyy_mm_dd(s: str) -> date:
    return date.fromisoformat(s)

def range_to_ts(from_yyyy_mm_dd: str, to_yyyy_mm_dd: str) -> Tuple[int, int]:
    d_from = parse_yyyy_mm_dd(from_yyyy_mm_dd)
    d_to = parse_yyyy_mm_dd(to_yyyy_mm_dd)
    if d_to < d_from:
        raise ValueError("'to' must be >= 'from'")

    from_dt = datetime(d_from.year, d_from.month, d_from.day, 0, 0, 0, tzinfo=JAKARTA_TZ)
    to_dt = datetime(d_to.year, d_to.month, d_to.day, 23, 59, 59, tzinfo=JAKARTA_TZ)
    return int(from_dt.timestamp()), int(to_dt.timestamp())

def extract_time(text: str) -> Optional[str]:
    text = normalize_spaces(text)
    m = TIME_IN_TEXT.search(text)
    if not m:
        return None
    h = int(m.group("h"))
    minute = int(m.group("m"))
    if not (0 <= h <= 23 and 0 <= minute <= 59):
        return None
    return f"{h:02d}:{minute:02d}"

def extract_status_short(text: str) -> str:
    text = normalize_spaces(text)
    m = STATUS_IN_TEXT.search(text)
    if m:
        s = m.group(1)
        return s if s in {"FT", "HT"} else "NS"
    if extract_time(text):
        return "NS"
    return "NS"

def to_unix_timestamp(date_yyyy_mm_dd: str, time_hh_mm: Optional[str]) -> int:
    hh, mm = (0, 0)
    if time_hh_mm and re.match(r"^\d{1,2}:\d{2}$", time_hh_mm):
        h_str, m_str = time_hh_mm.split(":")
        hh, mm = int(h_str), int(m_str)

    dt = datetime.fromisoformat(date_yyyy_mm_dd).replace(hour=hh, minute=mm, tzinfo=JAKARTA_TZ)
    return int(dt.timestamp())

def ts_to_iso(ts: int) -> str:
    return datetime.fromtimestamp(ts, tz=JAKARTA_TZ).isoformat()

def status_long(short: str) -> str:
    if short == "FT":
        return "Match Finished"
    if short == "HT":
        return "Match In Progress"
    return "Not Started"

# ---------- Cached HTML fetchers ----------

async def fetch_week_html(competition_code: str, week: int, ttl_seconds: int = 600) -> Optional[str]:
    key = f"{competition_code}:week:{week}"
    cached = cache_get("fixtures_html", key)
    if cached is not None:
        return cached

    url = f"{FIXTURES_BASE}/{competition_code}/{week}"
    async with httpx.AsyncClient(
        timeout=20,
        headers={"User-Agent": "bri-liga-scraper/2.0"},
        follow_redirects=True,
    ) as client:
        r = await client.get(url)
        if r.status_code == 404:
            cache_set("fixtures_html", key, None, ttl_seconds=180)
            return None
        r.raise_for_status()

    cache_set("fixtures_html", key, r.text, ttl_seconds=ttl_seconds)
    return r.text

async def fetch_match_html(match_url: str, ttl_seconds: int = 900) -> Optional[str]:
    key = f"match:{match_url}"
    cached = cache_get("match_html", key)
    if cached is not None:
        return cached

    async with httpx.AsyncClient(
        timeout=20,
        headers={"User-Agent": "bri-liga-scraper/2.0"},
        follow_redirects=True,
    ) as client:
        r = await client.get(match_url)
        if r.status_code == 404:
            cache_set("match_html", key, None, ttl_seconds=180)
            return None
        r.raise_for_status()

    cache_set("match_html", key, r.text, ttl_seconds=ttl_seconds)
    return r.text

# ---------- Match detail parsing (ONLY what you need) ----------

def parse_venue_city(soup: BeautifulSoup) -> Tuple[Optional[str], Optional[str]]:
    # find something like "Stadion Gelora Bung Tomo , Surabaya"
    candidates = []
    for t in soup.stripped_strings:
        tt = normalize_spaces(t)
        if "stadion" in tt.lower() and "," in tt:
            candidates.append(tt)

    if not candidates:
        return None, None

    line = max(candidates, key=len)
    parts = [p.strip() for p in line.split(",") if p.strip()]
    venue = parts[0] if parts else None
    city = parts[1] if len(parts) > 1 else None
    return venue, city

def parse_score_kickoff_status(soup: BeautifulSoup) -> Tuple[Optional[int], Optional[int], Optional[str], str]:
    text = soup.get_text(" ", strip=True)

    # status
    st = "NS"
    sm = STATUS_IN_TEXT.search(text)
    if sm:
        s = sm.group(1)
        st = s if s in {"FT", "HT"} else "NS"

    # score
    hs = aws = None
    m = SCORE_RE.search(text)
    if m:
        hs = int(m.group("hs"))
        aws = int(m.group("as"))

    # kickoff time
    kickoff = None
    km = KICKOFF_RE.search(text)
    if km:
        kickoff = f"{int(km.group('h')):02d}:{int(km.group('m')):02d}"

    return hs, aws, kickoff, st

async def enrich_from_match_detail(match_url: str, fallback_date: str) -> Dict[str, Any]:
    """
    Returns ONLY fields you requested from match detail:
      - date/timestamp (based on fallback_date + kickoff)
      - venue/city
      - status
      - score (home/away)
      - kickoff_wib
    """
    html = await fetch_match_html(match_url)
    if not html:
        # fallback minimal
        return {
            "kickoff_wib": None,
            "venue": {"name": None, "city": None},
            "status": "NS",
            "score": {"home": None, "away": None},
            "timestamp": to_unix_timestamp(fallback_date, None),
        }

    soup = BeautifulSoup(html, "lxml")
    venue, city = parse_venue_city(soup)
    hs, aws, kickoff, st = parse_score_kickoff_status(soup)

    ts = to_unix_timestamp(fallback_date, kickoff)
    return {
        "kickoff_wib": kickoff,
        "venue": {"name": venue, "city": city},
        "status": st,
        "score": {"home": hs, "away": aws},
        "timestamp": ts,
    }

# ---------- Main scraping ----------

async def scrape_fixtures_period(
    competition_code: str,
    season: int,
    from_date: str,
    to_date: str,
    team: Optional[str] = None,
    max_week: int = 38,
    ttl_seconds: int = 600,
) -> List[Dict[str, Any]]:
    # cache final result per parameter set
    result_key = f"{competition_code}:{season}:{from_date}:{to_date}:{team}:{max_week}"
    cached = cache_get("fixtures_period", result_key)
    if cached is not None:
        return cached

    ts_min, ts_max = range_to_ts(from_date, to_date)

    all_items: List[Dict[str, Any]] = []

    for week in range(1, max_week + 1):
        html = await fetch_week_html(competition_code, week)
        if html is None:
            continue

        soup = BeautifulSoup(html, "lxml")
        anchors = soup.select('a[href^="/result/detail/"], a[href*="/result/detail/"]')

        for a in anchors:
            href = a.get("href") or ""
            if "/result/detail/" not in href:
                continue

            match_url = href if href.startswith("http") else f"{SITE_BASE}{href}"

            dm = DATE_IN_URL.search(match_url)
            if not dm:
                continue
            match_date = dm.group("date")

            tm = TEAMS_IN_URL.search(match_url)
            if not tm:
                continue
            home_slug, away_slug = tm.group("home"), tm.group("away")
            home_name, away_name = slug_to_name(home_slug), slug_to_name(away_slug)

            # Optional team filter
            if team and not match_team(team, home_slug, away_slug, home_name, away_name):
                continue

            # Initial (from week page)
            a_text = a.get_text(" ", strip=True)
            kickoff_week = extract_time(a_text)          # might be None
            st_week = extract_status_short(a_text)       # NS/FT/HT

            ts_week = to_unix_timestamp(match_date, kickoff_week)

            # Period filter based on (date + time if any)
            # If no kickoff, it becomes 00:00 — still OK for date-range filtering.
            if ts_week < ts_min or ts_week > ts_max:
                continue

            # Enrich from match detail ONLY to get your required fields (score/kickoff/venue/status)
            detail = await enrich_from_match_detail(match_url, fallback_date=match_date)

            # final timestamp/iso uses detail kickoff if exists
            ts_final = detail["timestamp"]
            if ts_final < ts_min or ts_final > ts_max:
                # if kickoff shifts it outside range, skip
                continue

            fixture_id = stable_int32_id(match_url)
            home_id = stable_int32_id(f"TEAM:{home_slug}")
            away_id = stable_int32_id(f"TEAM:{away_slug}")

            goals_home = detail["score"]["home"]
            goals_away = detail["score"]["away"]
            st_final = detail["status"] or st_week

            item = {
                "fixture": {
                    "id": fixture_id,
                    "referee": None,
                    "timezone": "Asia/Jakarta",
                    "date": ts_to_iso(ts_final),
                    "timestamp": ts_final,
                    "periods": {"first": None, "second": None},
                    "venue": {
                        "id": None,
                        "name": detail["venue"]["name"],
                        "city": detail["venue"]["city"],
                    },
                    "status": {
                        "long": status_long(st_final),
                        "short": st_final,
                        "elapsed": 90 if st_final == "FT" else (45 if st_final == "HT" else None),
                    },
                },
                "league": {
                    "id": 274,
                    "name": "Liga 1",
                    "country": "Indonesia",
                    "logo": None,
                    "flag": None,
                    "season": season,
                    "round": f"Regular Season - {week}",
                },
                "teams": {
                    "home": {"id": home_id, "name": home_name, "logo": None, "winner": None},
                    "away": {"id": away_id, "name": away_name, "logo": None, "winner": None},
                },
                "goals": {"home": goals_home, "away": goals_away},
                "score": {
                    "halftime": {"home": None, "away": None},
                    "fulltime": {"home": goals_home, "away": goals_away},
                    "extratime": {"home": None, "away": None},
                    "penalty": {"home": None, "away": None},
                },
                "events": None,
                "lineups": None,
                "statistics": None,
                "players": None,
                "_source": {
                    "week_url": f"{FIXTURES_BASE}/{competition_code}/{week}",
                    "match_url": match_url,              # ✅ use this for "click detail → open web"
                    "kickoff_wib": detail["kickoff_wib"] # ✅ requested
                },
            }

            all_items.append(item)

    # dedupe + sort
    unique: Dict[int, Dict[str, Any]] = {it["fixture"]["id"]: it for it in all_items}
    out = list(unique.values())
    out.sort(key=lambda x: x["fixture"]["timestamp"])

    cache_set("fixtures_period", result_key, out, ttl_seconds=ttl_seconds)
    return out
