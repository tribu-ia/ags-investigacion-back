import logging
from typing import List, Dict, Optional
from datetime import datetime
from math import ceil
import uuid
import asyncpg
import asyncio
import json
import os
import aiohttp

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
                        # Crear tabla de agentes
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
                                updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                slug TEXT,
                                version TEXT,
                                featured BOOLEAN DEFAULT false,
                                UNIQUE(name),
                                UNIQUE(slug)
                            )
                        ''')

                        # Crear tabla de investigadores con nuevos campos
                        await conn.execute('''
                            CREATE TABLE IF NOT EXISTS investigadores (
                                id TEXT PRIMARY KEY,
                                name TEXT NOT NULL,
                                email TEXT NOT NULL UNIQUE,
                                phone TEXT,
                                github_username TEXT,
                                avatar_url TEXT,
                                repository_url TEXT,
                                linkedin_profile TEXT,
                                created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
                            )
                        ''')

                        # Crear tabla de asignaciones con restricción única compuesta
                        await conn.execute('''
                            CREATE TABLE IF NOT EXISTS agent_assignments (
                                id SERIAL PRIMARY KEY,
                                investigador_id TEXT REFERENCES investigadores(id),
                                agent_id TEXT REFERENCES ai_agents(id),
                                assigned_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                status TEXT DEFAULT 'active',
                                CONSTRAINT unique_active_assignment UNIQUE (agent_id),
                                CONSTRAINT unique_assignment_pair UNIQUE (agent_id, investigador_id)
                            )
                        ''')

                        # Crear tabla de documentación
                        await conn.execute('''
                            CREATE TABLE IF NOT EXISTS agent_documentation (
                                id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
                                agent_id TEXT,
                                investigador_id TEXT,
                                documentation_date TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                status TEXT DEFAULT 'completed',
                                findings TEXT,
                                recommendations TEXT,
                                research_summary TEXT,
                                research_data JSONB,
                                FOREIGN KEY (agent_id, investigador_id) 
                                    REFERENCES agent_assignments(agent_id, investigador_id)
                            )
                        ''')

                        print("Pool and tables created successfully")
                        return

                except Exception as e:
                    last_error = e
                    print(f"Connection attempt {attempt + 1} failed: {e}")
                    if self.pool:
                        await self.pool.close()
                        self.pool = None
                    if attempt < retry_count - 1:
                        await asyncio.sleep(2 ** attempt)
                    continue

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

    async def process_json_data(self, json_items):
        valid_documents = []
        
        logger.info(f"Procesando {len(json_items)} documentos")
        
        for item in json_items:
            try:
                # Validar que los campos requeridos no estén vacíos
                if not self._is_valid_document(item):
                    logger.warning(f"Documento inválido o vacío encontrado, saltando: {item.get('name', 'Unknown')}")
                    continue
                    
                # Convertir campos que pueden ser None a valores por defecto apropiados
                document = {
                    'id': item.get('id', str(uuid.uuid4())),
                    'name': item.get('name'),
                    'created_by': item.get('createdBy', ''),
                    'website': item.get('website', ''),
                    'access': item.get('access', ''),
                    'pricing_model': item.get('pricingModel', ''),
                    'category': item.get('category', ''),
                    'industry': item.get('industry', ''),
                    'short_description': item.get('shortDescription', ''),
                    'long_description': item.get('longDescription', ''),
                    'key_features': item.get('keyFeatures', []),
                    'use_cases': item.get('useCases', []),
                    'tags': item.get('tags', []),
                    'logo': item.get('logo', ''),
                    'logo_file_name': item.get('logoFileName', ''),
                    'image': item.get('image', ''),
                    'image_file_name': item.get('imageFileName', ''),
                    'video': item.get('video', ''),
                    'upvotes': item.get('upvotes', 0),
                    'upvoters': item.get('upvoters', []),
                    'approved': item.get('approved', False),
                    'created_at': datetime.now(),
                    'slug': item.get('slug', ''),
                    'version': str(item.get('version', '')),
                    'featured': item.get('featured', False)
                }
                
                # Asegurar que los campos JSONB sean listas o diccionarios válidos
                if isinstance(document['key_features'], str):
                    document['key_features'] = [x.strip() for x in document['key_features'].split(',')]
                if isinstance(document['use_cases'], str):
                    document['use_cases'] = [x.strip() for x in document['use_cases'].split(',')]
                if isinstance(document['tags'], str):
                    document['tags'] = [x.strip() for x in document['tags'].split(',')]
                if document['tags'] is None:
                    document['tags'] = []
                if isinstance(document['upvoters'], str):
                    document['upvoters'] = [document['upvoters']]
                
                valid_documents.append(document)
                logger.debug(f"Documento procesado exitosamente: {document['name']}")
                
            except Exception as e:
                logger.error(f"Error procesando documento {item.get('name', 'Unknown')}: {str(e)}")
                continue

        logger.info(f"Se procesaron {len(valid_documents)} documentos válidos de {len(json_items)} totales")

        if not valid_documents:
            return []

        try:
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
                    ON CONFLICT (name) DO UPDATE SET
                        created_by = EXCLUDED.created_by,
                        website = EXCLUDED.website,
                        access = EXCLUDED.access,
                        pricing_model = EXCLUDED.pricing_model,
                        category = EXCLUDED.category,
                        industry = EXCLUDED.industry,
                        short_description = EXCLUDED.short_description,
                        long_description = EXCLUDED.long_description,
                        key_features = EXCLUDED.key_features,
                        use_cases = EXCLUDED.use_cases,
                        tags = EXCLUDED.tags,
                        logo = EXCLUDED.logo,
                        logo_file_name = EXCLUDED.logo_file_name,
                        image = EXCLUDED.image,
                        image_file_name = EXCLUDED.image_file_name,
                        video = EXCLUDED.video,
                        upvotes = EXCLUDED.upvotes,
                        upvoters = EXCLUDED.upvoters,
                        approved = EXCLUDED.approved,
                        slug = EXCLUDED.slug,
                        version = EXCLUDED.version,
                        featured = EXCLUDED.featured,
                        updated_at = CURRENT_TIMESTAMP
                ''', [(
                    doc['id'], doc['name'], doc['created_by'], doc['website'],
                    doc['access'], doc['pricing_model'], doc['category'],
                    doc['industry'], doc['short_description'], doc['long_description'],
                    json.dumps(doc['key_features']), json.dumps(doc['use_cases']),
                    json.dumps(doc['tags']), doc['logo'], doc['logo_file_name'],
                    doc['image'], doc['image_file_name'], doc['video'],
                    doc['upvotes'], json.dumps(doc['upvoters']), doc['approved'],
                    doc['created_at'], doc['slug'], doc['version'],
                    doc['featured']
                ) for doc in valid_documents])
                
                return valid_documents
                
        except Exception as e:
            logger.error(f"Error en la inserción masiva: {str(e)}")
            raise

    def _is_valid_document(self, item):
        """Validar campos requeridos para ambas bases de datos"""
        required_fields = ['name', 'category', 'industry', 'shortDescription']
        
        for field in required_fields:
            if not item.get(field) or str(item.get(field)).strip() == '':
                return False
        return True

    async def check_agent_availability(self, agent_id: str) -> dict:
        """Verifica si un agente está disponible y obtiene información de asignación"""
        try:
            result = await self.execute_query('''
                SELECT 
                    i.name as investigador_name,
                    i.email as investigador_email,
                    aa.assigned_at,
                    aa.status
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
                        "assigned_at": result[0]['assigned_at'],
                        "status": result[0]['status']
                    }
                }
            
            # Verificar si el agente existe
            agent = await self.execute_query('SELECT id FROM ai_agents WHERE id = $1', agent_id)
            if not agent:
                raise ValueError(f"El agente seleccionado no existe")
                
            return {
                "available": True,
                "message": "Agente disponible para asignación"
            }
        except ValueError as ve:
            raise ve
        except Exception as e:
            logger.error(f"Error verificando disponibilidad del agente: {str(e)}")
            raise ValueError("Error al verificar la disponibilidad del agente. Por favor, inténtelo nuevamente.")

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
            # Query simplificada
            query = """
                SELECT 
                    a.*,
                    aa.status as assignment_status,
                    aa.assigned_at,
                    i.name as assigned_to_name,
                    i.email as assigned_to_email
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
                # Si hay status de asignación, está asignado
                is_assigned = row['assignment_status'] == 'active'
                
                agent_dict = {
                    "id": row['id'],
                    "name": row['name'],
                    "createdBy": row['created_by'],
                    "website": row['website'],
                    "access": row['access'],
                    "pricingModel": row['pricing_model'],
                    "category": row['category'],
                    "industry": row['industry'],
                    "shortDescription": row['short_description'],
                    "longDescription": row['long_description'],
                    "keyFeatures": row['key_features'],
                    "useCases": row['use_cases'],
                    "tags": row['tags'],
                    "logo": row['logo'],
                    "logoFileName": row['logo_file_name'],
                    "image": row['image'],
                    "imageFileName": row['image_file_name'],
                    "video": row['video'],
                    "upvotes": row['upvotes'],
                    "upvoters": row['upvoters'],
                    "approved": row['approved'],
                    "createdAt": row['created_at'],
                    "updatedAt": row['updated_at'],
                    "slug": row['slug'],
                    "version": row['version'],
                    "featured": row['featured'],
                    "isAssigned": is_assigned,
                    "assignmentInfo": {
                        "assignedTo": row['assigned_to_name'],
                        "assignedEmail": row['assigned_to_email'],
                        "assignedAt": row['assigned_at']
                    } if is_assigned else None
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

    async def fetch_github_data(self, github_username: str) -> Optional[Dict]:
        """Obtiene datos del usuario desde GitHub"""
        github_token = os.getenv('GITHUB_TOKEN')
        headers = {
            'Authorization': f'token {github_token}',
            'Accept': 'application/vnd.github+json',
            'X-GitHub-Api-Version': '2022-11-28'
        }
        
        async with aiohttp.ClientSession() as session:
            try:
                async with session.get(
                    f'https://api.github.com/users/{github_username}',
                    headers=headers
                ) as response:
                    if response.status == 200:
                        data = await response.json()
                        return {
                            'avatar_url': data.get('avatar_url'),
                            'repository_url': data.get('html_url')
                        }
                    elif response.status == 404:
                        return None
                    else:
                        logger.error(f"Error consultando GitHub API: {response.status}")
                        return None
            except Exception as e:
                logger.error(f"Error en la conexión con GitHub: {str(e)}")
                return None

    async def create_investigador(self, investigador_data: Dict) -> Dict:
        """Crea un nuevo investigador y asigna el agente si está disponible"""
        try:
            # Validar datos requeridos
            required_fields = ['name', 'email', 'agent_id', 'github_username']
            for field in required_fields:
                if not investigador_data.get(field):
                    return {
                        "success": False,
                        "message": f"El campo {field} es requerido",
                        "error_type": "validation_error",
                        "error_code": "MISSING_FIELD",
                        "field": field
                    }

            # Verificar si el investigador ya existe
            existing_investigador = await self.execute_query(
                "SELECT email FROM investigadores WHERE email = $1",
                investigador_data['email']
            )
            
            if existing_investigador:
                return {
                    "success": False,
                    "message": "Ya existe una cuenta registrada con este correo electrónico",
                    "error_type": "validation_error",
                    "error_code": "EMAIL_EXISTS",
                    "field": "email"
                }

            # Obtener datos de GitHub
            github_data = await self.fetch_github_data(investigador_data['github_username'])
            if not github_data:
                return {
                    "success": False,
                    "message": "No se pudo verificar el usuario de GitHub. Por favor, verifica que el nombre de usuario sea correcto.",
                    "error_type": "validation_error",
                    "error_code": "INVALID_GITHUB_USER",
                    "field": "github_username"
                }

            # Verificar que el agente existe y está disponible
            agent_id = investigador_data['agent_id']
            availability = await self.check_agent_availability(agent_id)

            if not availability["available"]:
                assignment = availability['current_assignment']
                return {
                    "success": False,
                    "message": f"El agente ya está asignado a {assignment['investigador_name']}",
                    "error_type": "validation_error",
                    "error_code": "AGENT_ASSIGNED",
                    "field": "agent_id",
                    "current_assignment": {
                        "investigador_name": assignment['investigador_name'],
                        "investigador_email": assignment['investigador_email'],
                        "assigned_at": assignment['assigned_at']
                    }
                }

            try:
                # Crear investigador con datos de GitHub
                investigador_id = str(uuid.uuid4())
                await self.execute_query("""
                    INSERT INTO investigadores (
                        id, name, email, phone, github_username,
                        avatar_url, repository_url, linkedin_profile
                    )
                    VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
                """, 
                    investigador_id,
                    investigador_data['name'],
                    investigador_data['email'],
                    investigador_data.get('phone', ''),
                    investigador_data['github_username'],
                    github_data['avatar_url'],
                    github_data['repository_url'],
                    investigador_data.get('linkedin_profile', '')
                )
            except asyncpg.UniqueViolationError:
                return {
                    "success": False,
                    "message": "Ya existe una cuenta registrada con este correo electrónico",
                    "error_type": "validation_error",
                    "error_code": "EMAIL_EXISTS",
                    "field": "email"
                }

            # Asignar agente
            assignment_success = await self.assign_agent_to_investigador(investigador_id, agent_id)
            
            if not assignment_success:
                # Si falla la asignación, eliminar el investigador creado
                await self.execute_query(
                    "DELETE FROM investigadores WHERE id = $1", 
                    investigador_id
                )
                return {
                    "success": False,
                    "message": "No se pudo asignar el agente seleccionado",
                    "error_type": "assignment_error",
                    "error_code": "ASSIGNMENT_FAILED",
                    "field": "agent_id"
                }

            return {
                "success": True,
                "message": "¡Registro exitoso! Se ha creado tu cuenta y asignado el agente",
                "error_type": None,
                "error_code": None,
                "data": {
                    "id": investigador_id,
                    "name": investigador_data['name'],
                    "email": investigador_data['email'],
                    "phone": investigador_data.get('phone', ''),
                    "github_username": investigador_data['github_username'],
                    "avatar_url": github_data['avatar_url'],
                    "repository_url": github_data['repository_url'],
                    "linkedin_profile": investigador_data.get('linkedin_profile', ''),
                    "agent_id": agent_id,
                    "status": "assigned"
                }
            }

        except Exception as e:
            logger.error(f"Error inesperado creando investigador: {str(e)}")
            return {
                "success": False,
                "message": "Ha ocurrido un error inesperado. Por favor, inténtelo más tarde",
                "error_type": "server_error",
                "error_code": "INTERNAL_ERROR"
            }

    async def get_stats(self) -> Dict:
        """Obtiene estadísticas de agentes e investigadores"""
        try:
            stats = await self.execute_query("""
                WITH stats AS (
                    SELECT 
                        (SELECT COUNT(*) FROM ai_agents) as total_agents,
                        (SELECT COUNT(*) FROM agent_documentation) as documented_agents,
                        (SELECT COUNT(*) FROM agent_assignments WHERE status = 'active') as active_investigators
                )
                SELECT 
                    total_agents,
                    documented_agents,
                    active_investigators
                FROM stats
            """)

            if stats:
                return {
                    "total_agents": stats[0]['total_agents'],
                    "documented_agents": stats[0]['documented_agents'],
                    "active_investigators": stats[0]['active_investigators']
                }
            return {
                "total_agents": 0,
                "documented_agents": 0,
                "active_investigators": 0
            }
        except Exception as e:
            logger.error(f"Error obteniendo estadísticas: {str(e)}")
            raise

    async def complete_agent_documentation(self, documentation_data: Dict) -> Dict:
        """Registra la documentación completada de un agente"""
        try:
            # Verificar que existe la asignación activa
            assignment = await self.execute_query("""
                SELECT investigador_id, agent_id 
                FROM agent_assignments 
                WHERE agent_id = $1 AND investigador_id = $2 AND status = 'active'
            """, documentation_data['agent_id'], documentation_data['investigador_id'])

            if not assignment:
                return {
                    "success": False,
                    "message": "No se encontró una asignación activa para este agente e investigador",
                    "error_type": "validation_error",
                    "error_code": "NO_ACTIVE_ASSIGNMENT"
                }

            # Insertar documentación
            await self.execute_query("""
                INSERT INTO agent_documentation (
                    agent_id, investigador_id, findings, recommendations, 
                    research_summary, research_data
                ) VALUES ($1, $2, $3, $4, $5, $6)
            """, 
                documentation_data['agent_id'],
                documentation_data['investigador_id'],
                documentation_data.get('findings', ''),
                documentation_data.get('recommendations', ''),
                documentation_data.get('research_summary', ''),
                json.dumps(documentation_data.get('research_data', {}))
            )

            # Actualizar estado de la asignación
            await self.execute_query("""
                UPDATE agent_assignments 
                SET status = 'completed' 
                WHERE agent_id = $1 AND investigador_id = $2
            """, documentation_data['agent_id'], documentation_data['investigador_id'])

            return {
                "success": True,
                "message": "Documentación del agente registrada exitosamente",
                "data": {
                    "agent_id": documentation_data['agent_id'],
                    "investigador_id": documentation_data['investigador_id'],
                    "status": "completed"
                }
            }

        except Exception as e:
            logger.error(f"Error registrando documentación: {str(e)}")
            return {
                "success": False,
                "message": "Error al registrar la documentación del agente",
                "error_type": "server_error",
                "error_code": "DOCUMENTATION_ERROR"
            }
