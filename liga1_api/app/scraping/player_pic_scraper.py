# app/scraping/player_pic_scraper.py
import re
from typing import Optional
from urllib.parse import urljoin

from bs4 import BeautifulSoup

SITE_BASE = "https://www.ligaindonesiabaru.com"

# matches: url("...") or url('...') or url(...)
RE_CSS_URL = re.compile(r"url\(\s*['\"]?([^'\")]+)['\"]?\s*\)", re.IGNORECASE)

def _norm(s: str) -> str:
    return " ".join((s or "").split()).strip()

def _abs(url: str, base_url: str) -> str:
    return url if url.startswith("http") else urljoin(base_url or SITE_BASE, url)

def _looks_like_player_photo(u: str) -> bool:
    low = (u or "").lower()
    if not low:
        return False
    # allow common player photo paths
    if "/uploads/images/player/" in low:
        return True
    # sometimes could be under uploads/images/players/ etc.
    if "uploads/images" in low and "player" in low:
        return True
    return False

def extract_player_photo_url(soup: BeautifulSoup, base_url: str = SITE_BASE) -> Optional[str]:
    """
    Extract player photo URL from player page DOM.
    Primary source: inline CSS background/background-image.
    Fallback: <img src="...">
    """

    hero_selectors = [
        ".section-title .col-md-3 [style*='background']",
        ".section-title .col-md-3 [style*='background-image']",
        ".section-title [style*='uploads/images/player']",
        ".section-title [style*='background']",
    ]
    for sel in hero_selectors:
        for el in soup.select(sel):
            style = el.get("style") or ""
            m = RE_CSS_URL.search(style)
            if m:
                url = _abs(_norm(m.group(1)), base_url)
                if _looks_like_player_photo(url):
                    return url

    for el in soup.find_all(style=True):
        style = el.get("style") or ""
        if "url(" not in style.lower():
            continue
        m = RE_CSS_URL.search(style)
        if not m:
            continue
        url = _abs(_norm(m.group(1)), base_url)
        if _looks_like_player_photo(url):
            return url

    for img in soup.find_all("img"):
        src = _norm(img.get("src") or "")
        if not src:
            continue
        url = _abs(src, base_url)
        if _looks_like_player_photo(url):
            return url

    return None
