from typing import Optional
from urllib.parse import urljoin

import httpx
from bs4 import BeautifulSoup

from app.utils.cache import cache_get, cache_set

SITE_BASE = "https://www.ligaindonesiabaru.com"


def _norm(s: str) -> str:
    return " ".join((s or "").split()).strip()


from typing import Optional
from bs4 import BeautifulSoup

def _pick_club_logo_src(soup: BeautifulSoup) -> Optional[str]:
    """
    Pick CLUB crest image from club page.
    Priority:
      1) Hero header logo (section-title-team)
      2) Known logo containers
      3) Fallback scoring
    """

    def ok(src: str) -> bool:
        low = (src or "").lower()
        if not low:
            return False
        # hard rejects
        if "competition-logo" in low:
            return False
        if "flagcdn" in low or "/flag" in low or "flags/" in low:
            return False
        if any(x in low for x in ["facebook", "instagram", "twitter", "youtube", "tiktok"]):
            return False
        # reject common non-logo images
        if any(x in low for x in ["lineup", "starting", "startingxi", "starting_xi", "susunan", "poster", "banner"]):
            return False
        return True

    # ✅ 1) BEST: crest in the hero header (your screenshot)
    hero_candidates = [
        ".section-title-team .col-md-3 img",
        ".section-title-team img",
    ]
    for sel in hero_candidates:
        img = soup.select_one(sel)
        if img and img.get("src"):
            src = img.get("src", "").strip()
            if ok(src):
                return src

    # ✅ 2) Other common containers
    selectors = [
        ".club-logo img",
        "img.club-logo",
        ".club__logo img",
        ".team-logo img",
        ".clubLogo img",
        ".club-header img",
        ".clubHeader img",
    ]
    for sel in selectors:
        img = soup.select_one(sel)
        if img and img.get("src"):
            src = img.get("src", "").strip()
            if ok(src):
                return src

    # ✅ 3) Fallback scoring (prefer site assets + logo hints)
    best_src = None
    best_score = -10**9

    for img in soup.find_all("img"):
        src = (img.get("src") or "").strip()
        if not ok(src):
            continue

        low = src.lower()
        alt = (img.get("alt") or "").lower()
        cls = " ".join(img.get("class") or []).lower()

        score = 0
        if "assets.ligaindonesiabaru.com" in low: score += 10
        if "ligaindonesiabaru.com/uploads/" in low: score += 8
        if "uploads/images" in low: score += 6

        if "club" in low: score += 6
        if "crest" in low: score += 6
        if "logo" in low: score += 2
        if "club" in alt or "logo" in alt: score += 2
        if "club" in cls or "logo" in cls: score += 1

        # small penalties
        if "icon" in low: score -= 4
        if "banner" in low or "sponsor" in low or "ads" in low: score -= 8

        if score > best_score:
            best_score = score
            best_src = src

    return best_src



async def resolve_club_logo_url(
    club_href_or_url: str,
    *,
    ttl_seconds: int = 7 * 86400,  # cache 7 days
) -> Optional[str]:
    """
    Input: "/clubs/single/..." OR full URL.
    Output: absolute logo URL (string) or None.
    """

    club_href_or_url = _norm(club_href_or_url)
    if not club_href_or_url:
        return None

    cache_key = f"club_logo:{club_href_or_url}"
    cached = cache_get("club_logo", cache_key)
    if cached is not None:
        return cached or None

    club_url = (
        club_href_or_url
        if club_href_or_url.startswith("http")
        else urljoin(SITE_BASE, club_href_or_url)
    )

    try:
        async with httpx.AsyncClient(
            timeout=15,
            follow_redirects=True,
            headers={"User-Agent": "bri-liga-scraper/3.0"},
        ) as client:
            r = await client.get(club_url)
            if r.status_code != 200:
                cache_set("club_logo", cache_key, "", ttl_seconds=ttl_seconds)
                return None

        soup = BeautifulSoup(r.text, "lxml")
        src = _pick_club_logo_src(soup)
        if not src:
            cache_set("club_logo", cache_key, "", ttl_seconds=ttl_seconds)
            return None

        logo_url = src if src.startswith("http") else urljoin(SITE_BASE, src)

        if logo_url:
            low = logo_url.lower()
            if "competition-logo" in low or "flagcdn" in low or "/flag" in low or "flags/" in low:
                return None


        # final safety
        if "competition-logo" in logo_url:
            cache_set("club_logo", cache_key, "", ttl_seconds=ttl_seconds)
            return None

        cache_set("club_logo", cache_key, logo_url, ttl_seconds=ttl_seconds)
        return logo_url

    except Exception:
        cache_set("club_logo", cache_key, "", ttl_seconds=ttl_seconds)
        return None
