"""Small, process-local abuse controls for the public prototype.

For multi-instance production deployments, replace this limiter with a shared
Redis/API-gateway limiter. Do not trust X-Forwarded-For unless the proxy chain
is explicitly configured and trusted.
"""
import asyncio
import os
import time
from collections import deque

from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware


class InMemoryRateLimitMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, requests_per_minute: int = 120, max_clients: int = 10_000):
        super().__init__(app)
        self.limit = max(1, requests_per_minute)
        self.max_clients = max(100, max_clients)
        self._requests: dict[str, deque[float]] = {}
        self._lock = asyncio.Lock()

    async def dispatch(self, request: Request, call_next):
        if request.url.path == "/health":
            return await call_next(request)

        # Use the socket peer only. Forwarded headers are spoofable unless a
        # trusted reverse-proxy configuration is explicitly introduced.
        client_ip = request.client.host if request.client else "unknown"
        now = time.monotonic()
        async with self._lock:
            bucket = self._requests.get(client_ip)
            if bucket is None:
                if len(self._requests) >= self.max_clients:
                    # Bound memory even if an attacker rotates source IPs.
                    oldest = min(self._requests, key=lambda key: self._requests[key][-1]
                                 if self._requests[key] else 0)
                    self._requests.pop(oldest, None)
                bucket = deque()
                self._requests[client_ip] = bucket

            cutoff = now - 60.0
            while bucket and bucket[0] <= cutoff:
                bucket.popleft()
            if len(bucket) >= self.limit:
                retry_after = max(1, int(60 - (now - bucket[0])))
                return JSONResponse(
                    status_code=429,
                    content={"detail": "Rate limit exceeded. Retry shortly."},
                    headers={"Retry-After": str(retry_after)},
                )
            bucket.append(now)

        response = await call_next(request)
        response.headers.setdefault("X-Content-Type-Options", "nosniff")
        response.headers.setdefault("Cache-Control", "no-store")
        return response
