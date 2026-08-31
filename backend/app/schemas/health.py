from typing import Literal

from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: Literal["ok"]
    service: str
    environment: str
    version: str


class DatabaseHealthResponse(BaseModel):
    status: Literal["ok"]
    database: Literal["connected"]


class RedisHealthResponse(BaseModel):
    status: Literal["ok"]
    redis: Literal["connected"]


class ReadinessResponse(BaseModel):
    status: Literal["ready"]
    backend: Literal["connected"]
    database: Literal["connected"]
    redis: Literal["connected"]
