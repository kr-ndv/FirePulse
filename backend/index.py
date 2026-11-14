"""Vercel entrypoint for the FirePulse FastAPI application.

Set the Vercel project's Root Directory to `backend`.
"""

from app.main import app

__all__ = ["app"]
