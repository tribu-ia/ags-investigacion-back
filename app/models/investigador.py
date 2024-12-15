from datetime import datetime
from typing import Optional, List, Dict, Any

from langchain_community.storage.sql import Base
from pydantic import BaseModel, EmailStr, constr
from sqlalchemy import Column, String, Text, Integer, Boolean, DateTime, create_engine
from sqlalchemy.dialects.postgresql import JSONB


class InvestigadorBase(BaseModel):
    """Modelo base para validación de datos del investigador"""
    name: constr(min_length=2, max_length=100)  # Nombre con longitud mínima y máxima
    email: EmailStr  # Validación de email
    phone: Optional[str] = None  # Formato internacional de teléfono
    agent_id: str  # ID del agente seleccionado


class InvestigadorResponse(InvestigadorBase):
    """Modelo para la respuesta después de crear un investigador"""
    id: int
    created_at: datetime

    class Config:
        from_attributes = True  # Para permitir la conversión desde objetos SQLAlchemy


class AIAgent(Base):
    __tablename__ = 'ai_agents'

    id = Column(String, primary_key=True)
    name = Column(String)
    created_by = Column(String)
    website = Column(String)
    access = Column(String)
    pricing_model = Column(String)
    category = Column(String)
    industry = Column(String)
    short_description = Column(Text)
    long_description = Column(Text)
    key_features = Column(Text)
    use_cases = Column(Text)
    tags = Column(Text)
    logo = Column(String)
    logo_file_name = Column(String)
    image = Column(String)
    image_file_name = Column(String)
    video = Column(String)
    upvotes = Column(Integer)
    upvoters = Column(Text)
    approved = Column(Boolean)
    created_at = Column(DateTime)
    slug = Column(String)
    version = Column(Integer)
    featured = Column(Boolean)
    raw_data = Column(JSONB)  # Almacena el JSON completo original


class Investigador(Base):
    __tablename__ = 'investigador'
    id = Column(String, primary_key=True)
    name = Column(String)
    email = Column(String)
    phone = Column(String)
    agent_id = Column(String)


class ElasticsearchQueryPayload(BaseModel):
    query: str
    filters: Optional[dict] = None
    k: Optional[int] = 20


class JsonDataPayload(BaseModel):
    data: List[Dict[str, Any]]


class PaginatedResponse(BaseModel):
    items: List[Dict[str, Any]]
    total: int
    page: int
    page_size: int
    total_pages: int


class InvestigadorPayload(BaseModel):
    name: str
    email: str
    phone: Optional[str] = None
    agent: str
