from datetime import datetime
from typing import Optional
from pydantic import BaseModel, EmailStr, constr


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