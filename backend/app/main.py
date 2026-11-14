from dotenv import load_dotenv
load_dotenv()

import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers.fires import router as fires_router
from app.routers.analytics import router as analytics_router
from app.security import InMemoryRateLimitMiddleware

app = FastAPI(
    title="FirePulse API",
    version="0.3.0",
    description="NASA FIRMS active-fire observations, GeoJSON and summary analytics.",
    docs_url="/docs",
    redoc_url="/redoc",
)

# Native Android clients do not need CORS. If a browser client is added,
# configure explicit origins in FIREPULSE_CORS_ORIGINS (comma-separated).
cors_origins = [
    origin.strip()
    for origin in os.getenv("FIREPULSE_CORS_ORIGINS", "").split(",")
    if origin.strip()
]
if cors_origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=cors_origins,
        allow_credentials=False,
        allow_methods=["GET"],
        allow_headers=["Accept", "Content-Type"],
    )

requests_per_minute = int(os.getenv("FIREPULSE_RATE_LIMIT_PER_MINUTE", "120"))
app.add_middleware(
    InMemoryRateLimitMiddleware,
    requests_per_minute=requests_per_minute,
)

app.include_router(fires_router, prefix="/api/v1")
app.include_router(analytics_router, prefix="/api/v1")


@app.get("/health", tags=["operations"])
async def health():
    return {"status": "ok", "service": "firepulse-api", "version": app.version}
