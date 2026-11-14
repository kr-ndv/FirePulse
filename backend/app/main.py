from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers.fires import router as fires_router

app = FastAPI(title="FirePulse API", version="0.1.0",
              description="NASA FIRMS observation access and harmonized fire activity services.")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=False,
                   allow_methods=["GET"], allow_headers=["*"])
app.include_router(fires_router, prefix="/api/v1")

@app.get("/health")
async def health():
    return {"status": "ok", "service": "firepulse-api"}
