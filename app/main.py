import logging
import os
from typing import Optional, List, Dict, Any

from dotenv import load_dotenv
from fastapi import Request, FastAPI, HTTPException, Query
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from starlette.middleware.cors import CORSMiddleware

from app.db_manager.elasticsearch_store import MyElasticsearchVectorStore
from app.db_manager.postgres_store import PostgresStore

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

load_dotenv()

app = FastAPI()

# Instancias globales
es_store = MyElasticsearchVectorStore()
pg_store = PostgresStore()


app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],  # Origen de tu aplicación Next.js
    allow_credentials=True,
    allow_methods=["*"],  # Permite todos los métodos
    allow_headers=["*"],  # Permite todos los headers
)

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
    agent: str  # Este es el agent_id


@app.post("/upload-json/elasticsearch")
async def upload_json_elasticsearch(payload: JsonDataPayload):
    try:
        # Extraer directamente el array de datos del primer elemento
        json_items = payload.data[0]['json']['data']

        if not json_items:
            raise HTTPException(
                status_code=400,
                detail="No se encontraron datos válidos en el JSON"
            )

        # Procesar el JSON con Elasticsearch
        es_documents = es_store.process_json_data(json_items)

        # Procesar el JSON con PostgreSQL
        pg_documents = pg_store.process_json_data(json_items)

        return {
            "status": "success",
            "message": "JSON procesado correctamente",
            "elasticsearch_items": len(es_documents),
            "postgres_items": len(pg_documents),
            "total_items_processed": len(json_items)
        }
    except Exception as e:
        logger.error(f"Error al procesar el JSON: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error al procesar el JSON: {str(e)}")


@app.post("/query/hybrid-search")
async def process_hybrid_search(payload: ElasticsearchQueryPayload):
    try:
        if not payload.query.strip():
            raise HTTPException(status_code=400, detail="La consulta no puede estar vacía")

        results = es_store.search(
            query=payload.query,
            k=payload.k
        )

        if not results:
            return {
                "query": payload.query,
                "results": [],
                "message": "No se encontraron resultados para la consulta"
            }

        return {
            "query": payload.query,
            "results": results,
            "message": "Búsqueda híbrida exitosa"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al procesar la consulta: {str(e)}")


@app.get("/agents/metadata")
async def get_agents_metadata():
    try:
        return pg_store.get_metadata()
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error al obtener metadata: {str(e)}"
        )


@app.get("/agents", response_model=PaginatedResponse)
async def get_agents(
    page: int = Query(1, ge=1, description="Número de página"),
    page_size: int = Query(10, ge=1, le=600, description="Elementos por página"),
    category: Optional[str] = Query(None, description="Filtrar por categoría"),
    industry: Optional[str] = Query(None, description="Filtrar por industria"),
    search: Optional[str] = Query(None, description="Búsqueda por nombre o descripción")
):
    try:
        return pg_store.get_agents(page, page_size, category, industry, search)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error al obtener los agentes: {str(e)}"
        )


@app.post("/send/elasticsearch")
async def upload_json_elasticsearch(payload: JsonDataPayload):
    try:
        # Extraer directamente el array de datos del primer elemento
        json_items = payload.data[0]['json']['data']

        if not json_items:
            raise HTTPException(
                status_code=400,
                detail="No se encontraron datos válidos en el JSON"
            )

        # Procesar el JSON con Elasticsearch
        es_documents = es_store.process_json_data(json_items)

        # Procesar el JSON con PostgreSQL
        pg_documents = pg_store.process_json_data(json_items)

        return {
            "status": "success",
            "message": "JSON procesado correctamente",
            "elasticsearch_items": len(es_documents),
            "postgres_items": len(pg_documents),
            "total_items_processed": len(json_items)
        }
    except Exception as e:
        logger.error(f"Error al procesar el JSON: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error al procesar el JSON: {str(e)}")


@app.post("/investigadores")
async def create_investigador(payload: InvestigadorPayload):
    try:
        investigador_data = {
            "name": payload.name,
            "email": payload.email,
            "phone": payload.phone,
            "agent_id": payload.agent
        }

        result = pg_store.create_investigador(investigador_data)

        return {
            "status": "success",
            "message": "Investigador registrado correctamente",
            "data": result
        }
    except ValueError as ve:
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as e:
        logger.error(f"Error al crear investigador: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error al crear investigador: {str(e)}"
        )


@app.get("/health")
async def health_check():
    return {"status": "OK"}


@app.middleware("http")
async def catch_exceptions_middleware(request: Request, call_next):
    try:
        return await call_next(request)
    except Exception as e:
        logger.exception(f"Unhandled exception: {str(e)}")
        return JSONResponse(
            status_code=500,
            content={"message": "Internal server error"}
        )
