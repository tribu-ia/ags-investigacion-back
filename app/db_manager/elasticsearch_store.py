import os
from typing import List, Dict

from langchain_core.documents import Document
from langchain_elasticsearch import ElasticsearchStore
from langchain_openai import OpenAIEmbeddings


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

    def process_json_data(self, json_data: List[Dict]) -> List[Document]:
        """Procesa datos JSON y los indexa en Elasticsearch con embeddings."""
        documents = []
        for item_data in json_data:
            content = f"""
            Name: {item_data.get('name', '')}
            Description: {item_data.get('shortDescription', '')} {item_data.get('longDescription', '')}
            Category: {item_data.get('category', '')}
            Industry: {item_data.get('industry', '')}
            Key Features: {item_data.get('keyFeatures', '')}
            Use Cases: {item_data.get('useCases', '')}
            Tags: {item_data.get('tags', '')}
            """.strip()

            doc = Document(
                page_content=content,
                metadata={
                    "id": item_data.get('id'),
                    "name": item_data.get('name'),
                    "website": item_data.get('website'),
                    "category": item_data.get('category'),
                    "industry": item_data.get('industry')
                }
            )
            documents.append(doc)

        # Añadir todos los documentos al vector store
        self.vector_store.add_documents(documents)
        return documents

    def search(self, query: str, k: int = 5):
        """Realiza una búsqueda de similitud en el vector store."""
        results = self.vector_store.similarity_search_with_relevance_scores(query, k=k)
        # 'results' es una lista de Document con contenido y metadatos.
        return results
