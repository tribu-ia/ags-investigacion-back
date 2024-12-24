import logging
import os
from typing import Optional

from dotenv import load_dotenv
from fastapi import Request, FastAPI, HTTPException, Query
from fastapi.responses import JSONResponse
from starlette.middleware.cors import CORSMiddleware

from app.db_manager.elasticsearch_store import MyElasticsearchVectorStore
from app.db_manager.postgres_store import DatabaseManager
from app.models.investigador import JsonDataPayload, ElasticsearchQueryPayload, PaginatedResponse, InvestigadorPayload

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

load_dotenv()

app = FastAPI()

db_config = {
    'database': os.getenv('DB_NAME'),
    'user': os.getenv('DB_USER'),
    'password': os.getenv('DB_PASSWORD'),
    'host': os.getenv('DB_HOST'),
    'port': os.getenv('DB_PORT')
}

# Instancias globales
es_store = MyElasticsearchVectorStore()
db_manager = DatabaseManager(db_config)

@app.on_event("startup")
async def startup_event():
    """Inicializar conexiones al arrancar la aplicación"""
    try:
        await db_manager.initialize()
        logger.info("Database connection pool initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize database connection pool: {e}")
        raise

@app.on_event("shutdown")
async def shutdown_event():
    """Cerrar conexiones al detener la aplicación"""
    try:
        await db_manager.cleanup()
        logger.info("Database connections cleaned up successfully")
    except Exception as e:
        logger.error(f"Error cleaning up database connections: {e}")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "https://tribu.agentesdeia.info"],  # Origen de tu aplicación Next.js
    allow_credentials=True,
    allow_methods=["*"],  # Permite todos los métodos
    allow_headers=["*"],  # Permite todos los headers
)

@app.get("/database/metrics")
async def get_database_metrics():
    """Endpoint para monitorear el estado de las conexiones"""
    try:
        metrics = await db_manager.get_connection_metrics()
        return JSONResponse(content=metrics)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error getting database metrics: {str(e)}"
        )


@app.post("/upload-json/elasticsearch2")
async def upload_json_elasticsearch(payload: JsonDataPayload):
    try:
        json_items = payload.data[0]['json']['data']

        if not json_items:
            raise HTTPException(
                status_code=400,
                detail="No se encontraron datos válidos en el JSON"
            )
        # Procesar el JSON con Elasticsearch
        es_documents = es_store.process_json_data(json_items)

        return {
            "status": "success",
            "message": "JSON procesado correctamente",
            "elasticsearch_items": len(es_documents),
            "total_items_received": len(json_items)
        }
    except Exception as e:
        logger.error(f"Error al procesar el JSON: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error al procesar el JSON: {str(e)}")


@app.post("/upload-json/elasticsearch")
async def upload_json_elasticsearch(payload: JsonDataPayload):
    try:
        json_items = payload.data[0]['json']['data']

        if not json_items:
            raise HTTPException(
                status_code=400,
                detail="No se encontraron datos válidos en el JSON"
            )

        # Procesar el JSON con Elasticsearch
        es_documents = es_store.process_json_data(json_items)
        
        # Procesar el JSON con PostgreSQL
        pg_documents = await db_manager.process_json_data(json_items)

        skipped_items = len(json_items) - max(len(es_documents), len(pg_documents))

        return {
            "status": "success",
            "message": "JSON procesado correctamente",
            "elasticsearch_items": len(es_documents),
            "postgres_items": len(pg_documents),
            "skipped_items": skipped_items,
            "total_items_received": len(json_items)
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
        return await db_manager.get_metadata()
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
        return await db_manager.get_agents(page, page_size, category, industry, search)
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

        result = await db_manager.create_investigador(investigador_data)

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
