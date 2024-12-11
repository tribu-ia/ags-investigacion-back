import logging
from typing import List, Dict, Any
import os
from datetime import datetime
from math import ceil
import uuid

from sqlalchemy import create_engine, Column, String, Boolean, Integer, DateTime, Text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, scoped_session
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.exc import IntegrityError


logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

Base = declarative_base()

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

class PostgresStore:
    _instance = None
    _engine = None
    _session_factory = None
    
    def __new__(cls, db_url=None):
        if cls._instance is None:
            cls._instance = super(PostgresStore, cls).__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self, db_url=None):
        if self._initialized:
            return
            
        if not db_url:
            db_url = os.getenv('DATABASE_URL')
            if not db_url:
                raise ValueError("DATABASE_URL environment variable is not set")

        # Crear la conexión a la BD solo si no existe
        if not self._engine:
            self._engine = create_engine(db_url, pool_size=5, max_overflow=10, pool_pre_ping=True)
            Base.metadata.create_all(self._engine)
            
            # Crear session factory con scope
            self._session_factory = scoped_session(sessionmaker(bind=self._engine))
        
        self._initialized = True

    @property
    def session(self):
        """Obtener una sesión del scope actual"""
        return self._session_factory()

    def cleanup_session(self):
        """Limpiar la sesión actual"""
        self._session_factory.remove()

    def process_json_data(self, json_data: List[Dict]) -> List[Dict]:
        """Procesa y almacena los datos JSON en PostgreSQL"""
        processed_items = []

        for item_data in json_data:
            try:

                # Intentamos parsear la fecha
                created_at_str = item_data.get('createdAt')
                created_at = None
                if created_at_str:
                    created_at_str = created_at_str.replace('Z', '+00:00')
                    created_at = datetime.fromisoformat(created_at_str)

                existing_agent = self.session.query(AIAgent).filter_by(id=item_data.get('id')).first()

                if not existing_agent:
                    agent = AIAgent(
                        id=item_data.get('id'),
                        name=item_data.get('name', ''),
                        created_by=item_data.get('createdBy', ''),
                        website=item_data.get('website', ''),
                        access=item_data.get('access', ''),
                        pricing_model=item_data.get('pricingModel', ''),
                        category=item_data.get('category', ''),
                        industry=item_data.get('industry', ''),
                        short_description=item_data.get('shortDescription', ''),
                        long_description=item_data.get('longDescription', ''),
                        key_features=item_data.get('keyFeatures', ''),
                        use_cases=item_data.get('useCases', ''),
                        tags=item_data.get('tags', ''),
                        logo=item_data.get('logo', ''),
                        logo_file_name=item_data.get('logoFileName', ''),
                        image=item_data.get('image', ''),
                        image_file_name=item_data.get('imageFileName', ''),
                        video=item_data.get('video', ''),
                        upvotes=item_data.get('upvotes', 0),
                        upvoters=item_data.get('upvoters', ''),
                        approved=item_data.get('approved', False),
                        created_at=created_at,
                        slug=item_data.get('slug', ''),
                        version=item_data.get('version', 1),
                        featured=item_data.get('featured', False),
                        raw_data=item_data if isinstance(item_data, dict) else {}
                    )
                    
                    if agent.id:
                        self.session.add(agent)
                        processed_items.append(item_data)
                        logger.debug(f"Nuevo agente agregado: {agent.name}")
                    else:
                        logger.warning(f"Saltando registro sin ID: {agent.name}")

                else:
                    logger.debug(f"Agente ya existe: {item_data.get('name')} - no se actualiza.")

            except IntegrityError as e:
                self.session.rollback()
                logger.error(f"Error de integridad al procesar {item_data.get('name', 'unknown')}: {str(e)}")
            except Exception as e:
                self.session.rollback()
                logger.error(f"Error al procesar {item_data.get('name', 'unknown')}: {str(e)}")

        try:
            self.session.commit()
            logger.debug(f"Procesados {len(processed_items)} nuevos registros")
        except Exception as e:
            self.session.rollback()
            logger.error(f"Error al hacer commit: {str(e)}")
            raise

        return processed_items

    def __del__(self):
        """Asegurar que se limpien las sesiones al destruir la instancia"""
        if hasattr(self, '_session_factory'):
            self._session_factory.remove()

    def get_agents(self, page: int, page_size: int, category: str = None, 
                  industry: str = None, search: str = None) -> Dict:
        """Obtiene los agentes con paginación y filtros"""
        try:
            # Construir la query base
            query = self.session.query(AIAgent)

            # Aplicar filtros si existen
            if category:
                query = query.filter(AIAgent.category == category)
            if industry:
                query = query.filter(AIAgent.industry == industry)
            if search:
                search_filter = f"%{search}%"
                query = query.filter(
                    (AIAgent.name.ilike(search_filter)) |
                    (AIAgent.short_description.ilike(search_filter)) |
                    (AIAgent.long_description.ilike(search_filter))
                )

            # Obtener total de registros
            total_items = query.count()

            # Calcular paginación
            total_pages = ceil(total_items / page_size)
            offset = (page - 1) * page_size

            # Obtener registros paginados
            agents = query.order_by(AIAgent.created_at.desc())\
                         .offset(offset)\
                         .limit(page_size)\
                         .all()

            # Convertir a diccionario
            items = []
            for agent in agents:
                agent_dict = {
                    "id": agent.id,
                    "name": agent.name,
                    "category": agent.category,
                    "industry": agent.industry,
                    "shortDescription": agent.short_description,
                    "longDescription": agent.long_description,
                    "website": agent.website,
                    "access": agent.access,
                    "pricingModel": agent.pricing_model,
                    "keyFeatures": agent.key_features,
                    "useCases": agent.use_cases,
                    "tags": agent.tags,
                    "logo": agent.logo,
                    "image": agent.image,
                    "video": agent.video,
                    "upvotes": agent.upvotes,
                    "createdAt": agent.created_at,
                    "featured": agent.featured
                }
                items.append(agent_dict)

            return {
                "items": items,
                "total": total_items,
                "page": page,
                "page_size": page_size,
                "total_pages": total_pages
            }

        except Exception as e:
            logger.error(f"Error al obtener agentes: {str(e)}")
            raise

    def get_metadata(self) -> Dict:
        """Obtiene las categorías y industrias únicas"""
        try:
            # Obtener categorías únicas
            categories = self.session.query(AIAgent.category)\
                                   .distinct()\
                                   .filter(AIAgent.category != '')\
                                   .all()
            
            # Obtener industrias únicas
            industries = self.session.query(AIAgent.industry)\
                                   .distinct()\
                                   .filter(AIAgent.industry != '')\
                                   .all()

            return {
                "categories": [cat[0] for cat in categories if cat[0]],
                "industries": [ind[0] for ind in industries if ind[0]]
            }

        except Exception as e:
            logger.error(f"Error al obtener metadata: {str(e)}")
            raise

    def create_investigador(self, investigador_data: Dict) -> Dict:
        """Crea un nuevo registro de investigador usando el patrón singleton"""
        try:
            # Obtener una sesión del scope actual
            session = self.session

            # Verificar que el agente existe
            agent = session.query(AIAgent).filter_by(id=investigador_data['agent_id']).first()
            if not agent:
                raise ValueError(f"No se encontró el agente con ID: {investigador_data['agent_id']}")

            # Crear nuevo investigador
            investigador = Investigador(
                id=str(uuid.uuid4()),
                name=investigador_data['name'],
                email=investigador_data['email'],
                phone=investigador_data.get('phone', ''),
                agent_id=investigador_data['agent_id']
            )

            try:
                session.add(investigador)
                session.commit()
                logger.debug(f"Investigador creado: {investigador.name}")

                return investigador

            except IntegrityError as e:
                session.rollback()
                logger.error(f"Error de integridad al crear investigador: {str(e)}")
                raise ValueError("Error de integridad en los datos del investigador")
            
            except Exception as e:
                session.rollback()
                logger.error(f"Error al crear investigador: {str(e)}")
                raise

        except Exception as e:
            logger.error(f"Error en create_investigador: {str(e)}")
            raise

        finally:
            # Limpiar la sesión después de usarla
            self.cleanup_session()
