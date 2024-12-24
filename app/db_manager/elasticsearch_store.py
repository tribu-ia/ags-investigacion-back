import os
from typing import List, Dict
import json
import uuid
from datetime import datetime
import logging

from langchain_core.documents import Document
from langchain_elasticsearch import ElasticsearchStore
from langchain_openai import OpenAIEmbeddings

logger = logging.getLogger(__name__)


class MyElasticsearchVectorStore:
    def __init__(self):
        # Configurar la URL de Elasticsearch desde variable de entorno o por defecto
        self.es_url = os.getenv('ES_URL', 'http://localhost:9200')

        # Configurar las embeddings
        self.embeddings = OpenAIEmbeddings(model="text-embedding-3-small")

        # Nombre del índice
        self.index_name = "documents"

        # Inicializar el ElasticsearchStore
        # Este se encargará de crear el índice y mapearlo según se requiera.
        self.vector_store = ElasticsearchStore(
            es_url=self.es_url,
            index_name=self.index_name,
            embedding=self.embeddings
        )

    def process_json_data(self, json_items):
        valid_documents = []
        
        for item in json_items:
            try:
                # Validar que los campos requeridos no estén vacíos
                if not self._is_valid_document(item):
                    logger.warning(f"Documento inválido o vacío encontrado, saltando: {item}")
                    continue
                
                try:
                    # Verificar si ya existe un documento con el mismo nombre o slug
                    existing_doc = self.vector_store.client.search(
                        index=self.index_name,
                        body={
                            "query": {
                                "bool": {
                                    "should": [
                                        {"term": {"metadata.name.keyword": item.get('name', '')}},
                                        {"term": {"metadata.slug.keyword": item.get('slug', '')}}
                                    ]
                                }
                            }
                        }
                    )
                    
                    if existing_doc['hits']['total']['value'] > 0:
                        logger.warning(f"Documento con nombre o slug duplicado encontrado: {item['name']}")
                        continue
                    
                except Exception as es_error:
                    # Si el índice no existe, continuamos con la inserción
                    if 'index_not_found_exception' in str(es_error):
                        logger.info("Índice no encontrado, se creará con el primer documento")
                    else:
                        raise es_error

                # Crear el contenido para el embedding
                content = f"""
                Name: {item.get('name', '')}
                Short Description: {item.get('shortDescription', '')}
                Long Description: {item.get('longDescription', '')}
                Category: {item.get('category', '')}
                Industry: {item.get('industry', '')}
                Key Features: {item.get('keyFeatures', '')}
                Use Cases: {item.get('useCases', '')}
                Tags: {item.get('tags', '')}
                """.strip()

                # Crear documento con todos los campos de PostgreSQL
                document = Document(
                    page_content=content,
                    metadata={
                        'id': item.get('id', str(uuid.uuid4())),
                        'name': item.get('name'),
                        'createdBy': item.get('createdBy'),
                        'website': item.get('website'),
                        'access': item.get('access'),
                        'pricingModel': item.get('pricingModel'),
                        'category': item.get('category'),
                        'industry': item.get('industry'),
                        'shortDescription': item.get('shortDescription'),
                        'longDescription': item.get('longDescription'),
                        'keyFeatures': item.get('keyFeatures'),
                        'useCases': item.get('useCases'),
                        'tags': item.get('tags'),
                        'logo': item.get('logo'),
                        'logoFileName': item.get('logoFileName'),
                        'image': item.get('image'),
                        'imageFileName': item.get('imageFileName'),
                        'video': item.get('video'),
                        'upvotes': item.get('upvotes', 0),
                        'upvoters': item.get('upvoters', []),
                        'approved': item.get('approved', False),
                        'createdAt': str(datetime.now()),
                        'slug': item.get('slug'),
                        'version': item.get('version'),
                        'featured': item.get('featured', False)
                    }
                )
                valid_documents.append(document)
                
            except Exception as e:
                logger.error(f"Error procesando documento: {e}")
                continue
        
        if valid_documents:
            self.vector_store.add_documents(valid_documents)
            return valid_documents
        return []

    def _is_valid_document(self, item):
        """Validar campos requeridos para ambas bases de datos"""
        required_fields = ['name', 'category', 'industry', 'shortDescription']
        
        for field in required_fields:
            if not item.get(field) or str(item.get(field)).strip() == '':
                return False
        return True

    def search(self, query: str, k: int = 5):
        """Realiza una búsqueda de similitud en el vector store."""
        results = self.vector_store.similarity_search_with_relevance_scores(query, k=k)
        # 'results' es una lista de Document con contenido y metadatos.
        return results
