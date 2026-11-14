from dotenv import load_dotenv
load_dotenv()

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers.fires import router as fires_router
from app.routers.analytics import router as analytics_router

app = FastAPI(
    title="FirePulse API",
    version="0.2.0",
    description="NASA FIRMS active-fire observations, GeoJSON and summary analytics.",
    docs_url="/docs",
    redoc_url="/redoc",
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["GET"],
    allow_headers=["*"],
)
app.include_router(fires_router, prefix="/api/v1")
app.include_router(analytics_router, prefix="/api/v1")

@app.get("/health", tags=["operations"])
async def health():
    return {"status": "ok", "service": "firepulse-api", "version": app.version}
