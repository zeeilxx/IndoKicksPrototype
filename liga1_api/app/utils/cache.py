import time
from typing import Any, Optional, Dict, Tuple

# namespace -> key -> (expires_at, value)
_STORE: Dict[str, Dict[str, Tuple[float, Any]]] = {}

def cache_get(namespace: str, key: str) -> Optional[Any]:
    ns = _STORE.get(namespace)
    if not ns:
        return None
    item = ns.get(key)
    if not item:
        return None
    expires_at, value = item
    if expires_at and time.time() > expires_at:
        ns.pop(key, None)
        return None
    return value

def cache_set(namespace: str, key: str, value: Any, ttl_seconds: int = 300) -> None:
    ns = _STORE.setdefault(namespace, {})
    expires_at = time.time() + ttl_seconds if ttl_seconds else 0.0
    ns[key] = (expires_at, value)
