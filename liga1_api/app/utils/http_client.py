from typing import Dict, Any
import httpx

SITE_BASE = "https://www.ligaindonesiabaru.com"

DEFAULT_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    "Accept-Language": "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7",
    "Accept-Encoding": "gzip, deflate, br",
    "Cache-Control": "no-cache",
    "Pragma": "no-cache",
    "Upgrade-Insecure-Requests": "1",
    "Referer": SITE_BASE + "/",
    "Connection": "keep-alive",
}

async def fetch_html(url: str, timeout: int = 25) -> Dict[str, Any]:
    """
    Browser-like HTML fetcher (cookie warm-up).
    Return:
      { ok, status_code, final_url, text, error, headers }
    """
    async with httpx.AsyncClient(
        timeout=timeout,
        follow_redirects=True,
        headers=DEFAULT_HEADERS,
    ) as client:
        try:
            # Warm-up first page to get cookies/session (critical for /clubs/single)
            await client.get(SITE_BASE + "/")

            r = await client.get(url)

            return {
                "ok": r.status_code == 200,
                "status_code": r.status_code,
                "final_url": str(r.url),
                "text": r.text,
                "error": None if r.status_code == 200 else f"HTTP {r.status_code}",
                "headers": dict(r.headers),
            }
        except Exception as e:
            return {
                "ok": False,
                "status_code": None,
                "final_url": url,
                "text": "",
                "error": str(e),
                "headers": {},
            }
