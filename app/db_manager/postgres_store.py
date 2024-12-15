import logging
from typing import List, Dict
from datetime import datetime
from math import ceil
import uuid
import asyncpg
import asyncio
import json

from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.exc import IntegrityError

from app.models.investigador import AIAgent, Investigador

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

Base = declarative_base()


class DatabaseManager:
    def __init__(self, config):
        self.config = config
        self.pool = None

    async def initialize(self):
        if self.pool is None:
            retry_count = 3
            last_error = None

            for attempt in range(retry_count):
                try:
                    self.pool = await asyncpg.create_pool(
                        min_size=2,
                        max_size=10,
                        command_timeout=60,
                        timeout=30,
                        **self.config
                    )

                    async with self.pool.acquire() as conn:
                        # Crear tabla de agentes si no existe
                        await conn.execute('''
                            CREATE TABLE IF NOT EXISTS ai_agents (
                                id TEXT PRIMARY KEY,
                                name TEXT NOT NULL,
                                created_by TEXT,
                                website TEXT,
                                access TEXT,
                                pricing_model TEXT,
                                category TEXT,
                                industry TEXT,
                                short_description TEXT,
                                long_description TEXT,
                                key_features JSONB,
                                use_cases JSONB,
                                tags JSONB,
                                logo TEXT,
                                logo_file_name TEXT,
                                image TEXT,
                                image_file_name TEXT,
                                video TEXT,
                                upvotes INTEGER DEFAULT 0,
                                upvoters JSONB DEFAULT '[]'::jsonb,
                                approved BOOLEAN DEFAULT false,
                                created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                slug TEXT,
                                version TEXT,
                                featured BOOLEAN DEFAULT false,
                                raw_data JSONB
                            )
                        ''')

                        # Crear tabla de investigadores si no existe
                        await conn.execute('''
                            CREATE TABLE IF NOT EXISTS investigadores (
                                id TEXT PRIMARY KEY,
                                name TEXT NOT NULL,
                                email TEXT NOT NULL UNIQUE,
                                phone TEXT,
                                created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
                            )
                        ''')

                        # Crear tabla de asignaciones si no existe
                        await conn.execute('''
                            CREATE TABLE IF NOT EXISTS agent_assignments (
                                id SERIAL PRIMARY KEY,
                                investigador_id TEXT REFERENCES investigadores(id),
                                agent_id TEXT REFERENCES ai_agents(id),
                                assigned_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                status TEXT DEFAULT 'active',
                                CONSTRAINT unique_active_assignment UNIQUE (agent_id)
                            )
                        ''')
                        print("Pool and tables created successfully")
                        return

                except (asyncpg.ConnectionDoesNotExistError, asyncpg.PostgresConnectionError) as e:
                    last_error = e
                    print(f"Connection attempt {attempt + 1} failed: {e}")
                    if self.pool:
                        await self.pool.close()
                        self.pool = None
                    if attempt < retry_count - 1:
                        await asyncio.sleep(2 ** attempt)  # Exponential backoff
                    continue
                except Exception as e:
                    print(f"Unexpected error creating pool: {e}")
                    if self.pool:
                        await self.pool.close()
                        self.pool = None
                    raise

            if last_error:
                raise last_error

    async def cleanup(self):
        """Limpia y cierra todas las conexiones del pool"""
        if self.pool:
            await self.pool.close()
            self.pool = None
            print("Connection pool cleaned up successfully")

    async def execute_query(self, query, *args):
        """Ejecuta una consulta con reintentos automáticos"""
        retry_count = 3
        last_error = None

        for attempt in range(retry_count):
            try:
                if not self.pool:
                    await self.initialize()

                async with self.pool.acquire() as conn:
                    async with conn.transaction():
                        return await conn.fetch(query, *args)

            except (asyncpg.ConnectionDoesNotExistError, asyncpg.PostgresConnectionError) as e:
                last_error = e
                print(f"Query attempt {attempt + 1} failed: {e}")
                if self.pool:
                    await self.pool.close()
                    self.pool = None
                if attempt < retry_count - 1:
                    await asyncio.sleep(2 ** attempt)  # Exponential backoff
                continue
            except Exception as e:
                print(f"Error executing query: {e}")
                raise

        if last_error:
            raise last_error

    async def get_connection_metrics(self):
        """Obtiene métricas sobre el estado de las conexiones"""
        if not self.pool:
            return {
                "pool_status": "Not initialized",
                "metrics": None
            }

        return {
            "pool_status": "Active",
            "metrics": {
                "pool_min_size": self.pool.get_min_size(),
                "pool_max_size": self.pool.get_max_size(),
                "pool_free_size": self.pool.get_free_size(),
            }
        }

    async def process_json_data(self, json_data: List[Dict]) -> List[Dict]:
        """Procesa y almacena los datos JSON en PostgreSQL usando inserción masiva"""
        processed_items = []
        agents_to_insert = []

        # Preparar todos los agentes para inserción masiva
        for item_data in json_data:
            try:
                agent = AIAgent(
                    id=str(uuid.uuid4()),
                    name=item_data.get('name', ''),
                    created_by=item_data.get('created_by', ''),
                    website=item_data.get('website', ''),
                    access=item_data.get('access', ''),
                    pricing_model=item_data.get('pricing_model', ''),
                    category=item_data.get('category', ''),
                    industry=item_data.get('industry', ''),
                    short_description=item_data.get('short_description', ''),
                    long_description=item_data.get('long_description', ''),
                    key_features=item_data.get('key_features', []),
                    use_cases=item_data.get('use_cases', []),
                    tags=item_data.get('tags', []),
                    logo=item_data.get('logo', ''),
                    logo_file_name=item_data.get('logo_file_name', ''),
                    image=item_data.get('image', ''),
                    image_file_name=item_data.get('image_file_name', ''),
                    video=item_data.get('video', ''),
                    upvotes=item_data.get('upvotes', 0),
                    upvoters=item_data.get('upvoters', []),
                    approved=item_data.get('approved', False),
                    created_at=datetime.now(),
                    slug=item_data.get('slug', ''),
                    version=str(item_data.get('version', '')),
                    featured=item_data.get('featured', False)
                )

                if agent.id:
                    # Preparar los valores para la inserción masiva
                    agents_to_insert.append((
                        agent.id, agent.name, agent.created_by, agent.website,
                        agent.access, agent.pricing_model, agent.category,
                        agent.industry, agent.short_description, agent.long_description,
                        json.dumps(agent.key_features), json.dumps(agent.use_cases),
                        json.dumps(agent.tags), agent.logo, agent.logo_file_name,
                        agent.image, agent.image_file_name, agent.video,
                        agent.upvotes, json.dumps(agent.upvoters), agent.approved,
                        agent.created_at, agent.slug, agent.version,
                        agent.featured
                    ))
                    processed_items.append(item_data)
                else:
                    print(f"Error: ID no válido para el agente {agent.name}")
            except Exception as e:
                print(f"Error al procesar {item_data.get('name', 'Unknown')}: {str(e)}")
                continue

        try:
            if agents_to_insert:
                async with self.pool.acquire() as conn:
                    # Realizar la inserción masiva
                    await conn.executemany('''
                        INSERT INTO ai_agents (
                            id, name, created_by, website, access, pricing_model,
                            category, industry, short_description, long_description,
                            key_features, use_cases, tags, logo, logo_file_name,
                            image, image_file_name, video, upvotes, upvoters,
                            approved, created_at, slug, version, featured
                        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10,
                                $11, $12, $13, $14, $15, $16, $17, $18, $19, $20,
                                $21, $22, $23, $24, $25)
                    ''', agents_to_insert)
                print(f"Se insertaron {len(agents_to_insert)} agentes exitosamente")
        except Exception as e:
            print(f"Error en la inserción masiva: {str(e)}")
            return []

        return processed_items

    async def check_agent_availability(self, agent_id: str) -> dict:
        """Verifica si un agente está disponible y obtiene información de asignación"""
        try:
            result = await self.execute_query('''
                SELECT 
                    i.name as investigador_name,
                    i.email as investigador_email,
                    aa.assigned_at
                FROM agent_assignments aa
                JOIN investigadores i ON aa.investigador_id = i.id
                WHERE aa.agent_id = $1 AND aa.status = 'active'
            ''', agent_id)

            if result:
                return {
                    "available": False,
                    "current_assignment": {
                        "investigador_name": result[0]['investigador_name'],
                        "investigador_email": result[0]['investigador_email'],
                        "assigned_at": result[0]['assigned_at']
                    }
                }
            return {"available": True}
        except Exception as e:
            print(f"Error checking agent availability: {e}")
            raise

    async def assign_agent_to_investigador(self, investigador_id: str, agent_id: str) -> bool:
        """Asigna un agente a un investigador si está disponible"""
        try:
            # Verificar disponibilidad
            availability = await self.check_agent_availability(agent_id)
            if not availability["available"]:
                return False

            # Crear asignación
            await self.execute_query('''
                INSERT INTO agent_assignments (investigador_id, agent_id, status)
                VALUES ($1, $2, 'active')
            ''', investigador_id, agent_id)
            return True
        except Exception as e:
            print(f"Error assigning agent: {e}")
            raise

    async def get_agents(self, page: int, page_size: int, category: str = None,
                         industry: str = None, search: str = None) -> Dict:
        """Obtiene los agentes con paginación, filtros e información de asignación"""
        try:
            # Construir la query base con información de asignación
            query = """
                SELECT 
                    a.*,
                    CASE 
                        WHEN aa.id IS NOT NULL THEN true 
                        ELSE false 
                    END as is_assigned,
                    i.name as assigned_to_name,
                    i.email as assigned_to_email,
                    aa.assigned_at
                FROM ai_agents a
                LEFT JOIN agent_assignments aa ON a.id = aa.agent_id AND aa.status = 'active'
                LEFT JOIN investigadores i ON aa.investigador_id = i.id
            """

            # Aplicar filtros
            filters = []
            if category:
                filters.append(f"a.category = '{category}'")
            if industry:
                filters.append(f"a.industry = '{industry}'")
            if search:
                search_filter = f"%{search}%"
                filters.append(f"(a.name ILIKE '{search_filter}' OR a.short_description ILIKE '{search_filter}')")

            if filters:
                query += " WHERE " + " AND ".join(filters)

            # Obtener total de registros
            count_query = f"SELECT COUNT(*) FROM ({query}) as subquery"
            total_count = await self.execute_query(count_query)
            total_items = total_count[0]['count']

            # Agregar paginación
            query += f" ORDER BY a.created_at DESC LIMIT {page_size} OFFSET {(page - 1) * page_size}"

            # Ejecutar query final
            results = await self.execute_query(query)

            # Formatear resultados
            items = []
            for row in results:
                agent_dict = {
                    "id": row['id'],
                    "name": row['name'],
                    "category": row['category'],
                    "industry": row['industry'],
                    "shortDescription": row['short_description'],
                    "longDescription": row['long_description'],
                    "website": row['website'],
                    "access": row['access'],
                    "pricingModel": row['pricing_model'],
                    "keyFeatures": row['key_features'],
                    "useCases": row['use_cases'],
                    "tags": row['tags'],
                    "logo": row['logo'],
                    "image": row['image'],
                    "video": row['video'],
                    "upvotes": row['upvotes'],
                    "createdAt": row['created_at'],
                    "featured": row['featured'],
                    "is_assigned": row['is_assigned'],
                    "assignment_info": {
                        "assigned_to": row['assigned_to_name'],
                        "assigned_email": row['assigned_to_email'],
                        "assigned_at": row['assigned_at']
                    } if row['is_assigned'] else None
                }
                items.append(agent_dict)

            return {
                "items": items,
                "total": total_items,
                "page": page,
                "page_size": page_size,
                "total_pages": ceil(total_items / page_size)
            }

        except Exception as e:
            print(f"Error getting agents: {e}")
            raise

    async def get_metadata(self) -> Dict:
        """Obtiene las categorías y industrias únicas"""
        try:
            # Obtener categorías únicas
            categories = await self.execute_query("SELECT DISTINCT category FROM ai_agents WHERE category != ''")

            # Obtener industrias únicas
            industries = await self.execute_query("SELECT DISTINCT industry FROM ai_agents WHERE industry != ''")

            return {
                "categories": [cat[0] for cat in categories if cat[0]],
                "industries": [ind[0] for ind in industries if ind[0]]
            }

        except Exception as e:
            logger.error(f"Error al obtener metadata: {str(e)}")
            raise

    async def create_investigador(self, investigador_data: Dict) -> Dict:
        """Crea un nuevo investigador y asigna el agente si está disponible"""
        try:
            # Verificar que el agente existe y está disponible
            agent_id = investigador_data['agent_id']
            availability = await self.check_agent_availability(agent_id)

            if not availability["available"]:
                raise ValueError(
                    f"El agente ya está asignado a {availability['current_assignment']['investigador_name']} "
                    f"({availability['current_assignment']['investigador_email']})"
                )

            # Crear investigador
            investigador_id = str(uuid.uuid4())
            await self.execute_query("""
                INSERT INTO investigadores (id, name, email, phone)
                VALUES ($1, $2, $3, $4)
            """, investigador_id, investigador_data['name'],
                                     investigador_data['email'], investigador_data.get('phone', ''))

            # Asignar agente
            await self.assign_agent_to_investigador(investigador_id, agent_id)

            return {
                "id": investigador_id,
                "name": investigador_data['name'],
                "email": investigador_data['email'],
                "phone": investigador_data.get('phone', ''),
                "agent_id": agent_id,
                "status": "assigned"
            }

        except ValueError as ve:
            raise ve
        except Exception as e:
            print(f"Error creating investigador: {e}")
            raise
