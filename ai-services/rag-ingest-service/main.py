import os
import json
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from azure.core.credentials import AzureKeyCredential
from azure.search.documents import SearchClient
from azure.search.documents.indexes import SearchIndexClient, SearchIndexerClient
from azure.search.documents.indexes.models import (
    SearchIndex,
    SimpleField,
    SearchableField,
    SearchFieldDataType,
    VectorSearch,
    VectorSearchProfile,
    HnswAlgorithmConfiguration,
    AzureOpenAIVectorizer,
    AzureOpenAIVectorizerParameters,
    SearchIndexerDataSourceConnection,
    SearchIndexerDataContainer,
    SearchIndexer,
    SearchIndexerSkillset,
    SplitSkill,
    AzureOpenAIEmbeddingSkill,
    EntityRecognitionSkill,
    InputFieldMappingEntry,
    OutputFieldMappingEntry,
    SearchIndexerIndexProjection,
    SearchIndexerIndexProjectionSelector,
    SearchIndexerIndexProjectionsParameters,
    IndexProjectionMode,
    CognitiveServicesAccountKey,
    FieldMapping
)
import uvicorn
import logging
from typing import Optional

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="RAG Ingest Service", version="1.0.0")

# Environment variables
SERVICE_BUS_CONNECTION = os.getenv("SERVICE_BUS_CONNECTION")
AZURE_STORAGE_CONNECTION = os.getenv("AZURE_STORAGE_CONNECTION")
SEARCH_SERVICE_ENDPOINT = os.getenv("SEARCH_SERVICE_ENDPOINT")
SEARCH_SERVICE_KEY = os.getenv("SEARCH_SERVICE_KEY")
FOUNDRY_ENDPOINT = os.getenv("FOUNDRY_ENDPOINT")
FOUNDRY_KEY = os.getenv("FOUNDRY_KEY")
AZURE_AI_SERVICES_KEY = os.getenv("AZURE_AI_SERVICES_KEY", FOUNDRY_KEY)  # For entity recognition
INDEX_NAME = "documents-index"
DATA_SOURCE_NAME = "documents-datasource"
SKILLSET_NAME = "documents-skillset"
INDEXER_NAME = "documents-indexer"

# Initialize Azure clients
def get_search_index_client():
    credential = AzureKeyCredential(SEARCH_SERVICE_KEY)
    return SearchIndexClient(endpoint=SEARCH_SERVICE_ENDPOINT, credential=credential)

def get_search_indexer_client():
    credential = AzureKeyCredential(SEARCH_SERVICE_KEY)
    return SearchIndexerClient(endpoint=SEARCH_SERVICE_ENDPOINT, credential=credential)

def get_search_client():
    credential = AzureKeyCredential(SEARCH_SERVICE_KEY)
    return SearchClient(endpoint=SEARCH_SERVICE_ENDPOINT, index_name=INDEX_NAME, credential=credential)

# Request models
class DocumentIngestRequest(BaseModel):
    document_id: str
    user_id: str
    container_name: str = "documents"
    trigger_indexer: bool = True

# Initialize search index, skillset, and indexer on startup
@app.on_event("startup")
async def startup_event():
    """Create search index, skillset, data source, and indexer if they don't exist"""
    try:
        index_client = get_search_index_client()
        indexer_client = get_search_indexer_client()
        
        # 1. Create Search Index
        logger.info(f"Creating search index: {INDEX_NAME}")
        fields = [
            SimpleField(name="parent_id", type=SearchFieldDataType.String),
            SimpleField(name="chunk_id", type=SearchFieldDataType.String, key=True, filterable=True),
            SimpleField(name="document_id", type=SearchFieldDataType.String, filterable=True),
            SimpleField(name="user_id", type=SearchFieldDataType.String, filterable=True),
            SearchableField(name="title", type=SearchFieldDataType.String),
            SearchableField(name="chunk", type=SearchFieldDataType.String),
            SearchableField(
                name="locations",
                type=SearchFieldDataType.Collection(SearchFieldDataType.String),
                filterable=True
            ),
            SearchableField(
                name="text_vector",
                type=SearchFieldDataType.Collection(SearchFieldDataType.Single),
                vector_search_dimensions=1024,
                vector_search_profile_name="myHnswProfile"
            ),
        ]
        
        vector_search = VectorSearch(
            algorithms=[HnswAlgorithmConfiguration(name="myHnsw")],
            profiles=[VectorSearchProfile(
                name="myHnswProfile",
                algorithm_configuration_name="myHnsw",
                vectorizer_name="myOpenAI"
            )],
            vectorizers=[AzureOpenAIVectorizer(
                vectorizer_name="myOpenAI",
                kind="azureOpenAI",
                parameters=AzureOpenAIVectorizerParameters(
                    resource_url=FOUNDRY_ENDPOINT,
                    deployment_name="text-embedding-3-large",
                    model_name="text-embedding-3-large",
                    api_key=FOUNDRY_KEY
                )
            )]
        )
        
        index = SearchIndex(name=INDEX_NAME, fields=fields, vector_search=vector_search)
        index_client.create_or_update_index(index)
        logger.info(f"Search index '{INDEX_NAME}' created/updated")
        
        # 2. Create Data Source Connection
        logger.info(f"Creating data source: {DATA_SOURCE_NAME}")
        container = SearchIndexerDataContainer(name="documents")
        data_source = SearchIndexerDataSourceConnection(
            name=DATA_SOURCE_NAME,
            type="azureblob",
            connection_string=AZURE_STORAGE_CONNECTION,
            container=container
        )
        indexer_client.create_or_update_data_source_connection(data_source)
        logger.info(f"Data source '{DATA_SOURCE_NAME}' created/updated")
        
        # 3. Create Skillset
        logger.info(f"Creating skillset: {SKILLSET_NAME}")
        
        # Split skill for chunking
        split_skill = SplitSkill(
            description="Split skill to chunk documents",
            text_split_mode="pages",
            context="/document",
            maximum_page_length=2000,
            page_overlap_length=500,
            inputs=[InputFieldMappingEntry(name="text", source="/document/content")],
            outputs=[OutputFieldMappingEntry(name="textItems", target_name="pages")]
        )
        
        # Embedding skill
        embedding_skill = AzureOpenAIEmbeddingSkill(
            description="Generate embeddings via Azure OpenAI",
            context="/document/pages/*",
            resource_url=FOUNDRY_ENDPOINT,
            deployment_name="text-embedding-3-large",
            model_name="text-embedding-3-large",
            dimensions=1024,
            api_key=FOUNDRY_KEY,
            inputs=[InputFieldMappingEntry(name="text", source="/document/pages/*")],
            outputs=[OutputFieldMappingEntry(name="embedding", target_name="text_vector")]
        )
        
        # Entity recognition skill for locations
        entity_skill = EntityRecognitionSkill(
            description="Recognize entities in text",
            context="/document/pages/*",
            categories=["Location"],
            default_language_code="en",
            inputs=[InputFieldMappingEntry(name="text", source="/document/pages/*")],
            outputs=[OutputFieldMappingEntry(name="locations", target_name="locations")]
        )
        
        # Index projections for chunked data
        index_projections = SearchIndexerIndexProjection(
            selectors=[SearchIndexerIndexProjectionSelector(
                target_index_name=INDEX_NAME,
                parent_key_field_name="parent_id",
                source_context="/document/pages/*",
                mappings=[
                    InputFieldMappingEntry(name="chunk", source="/document/pages/*"),
                    InputFieldMappingEntry(name="text_vector", source="/document/pages/*/text_vector"),
                    InputFieldMappingEntry(name="locations", source="/document/pages/*/locations"),
                    InputFieldMappingEntry(name="title", source="/document/metadata_storage_name"),
                    InputFieldMappingEntry(name="document_id", source="/document/metadata_storage_name"),
                ]
            )],
            parameters=SearchIndexerIndexProjectionsParameters(
                projection_mode=IndexProjectionMode.SKIP_INDEXING_PARENT_DOCUMENTS
            )
        )
        
        cognitive_services = CognitiveServicesAccountKey(key=AZURE_AI_SERVICES_KEY)
        
        skillset = SearchIndexerSkillset(
            name=SKILLSET_NAME,
            description="Skillset to chunk documents and generate embeddings",
            skills=[split_skill, embedding_skill, entity_skill],
            index_projection=index_projections,
            cognitive_services_account=cognitive_services
        )
        
        indexer_client.create_or_update_skillset(skillset)
        logger.info(f"Skillset '{SKILLSET_NAME}' created/updated")
        
        # 4. Create Indexer
        logger.info(f"Creating indexer: {INDEXER_NAME}")
        indexer = SearchIndexer(
            name=INDEXER_NAME,
            description="Indexer to process documents and generate embeddings",
            skillset_name=SKILLSET_NAME,
            target_index_name=INDEX_NAME,
            data_source_name=DATA_SOURCE_NAME,
            field_mappings=[
                FieldMapping(source_field_name="metadata_storage_path", target_field_name="document_id")
            ]
        )
        
        indexer_client.create_or_update_indexer(indexer)
        logger.info(f"Indexer '{INDEXER_NAME}' created/updated")
        logger.info("RAG pipeline initialization complete!")
        
    except Exception as e:
        logger.error(f"Error initializing RAG pipeline: {str(e)}")
        raise

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "rag-ingest-service"}

@app.post("/ingest")
async def trigger_indexing(request: DocumentIngestRequest):
    """Trigger indexer to process documents from blob storage"""
    try:
        logger.info(f"Triggering indexer for user: {request.user_id}")
        
        indexer_client = get_search_indexer_client()
        
        if request.trigger_indexer:
            # Run the indexer
            indexer_client.run_indexer(INDEXER_NAME)
            logger.info(f"Indexer '{INDEXER_NAME}' triggered successfully")
            
            return {
                "status": "success",
                "message": "Indexer triggered. Documents will be processed automatically.",
                "indexer_name": INDEXER_NAME
            }
        else:
            return {
                "status": "success",
                "message": "Indexer not triggered. Documents will be processed on next scheduled run."
            }
        
    except Exception as e:
        logger.error(f"Error triggering indexer: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/indexer/status")
async def get_indexer_status():
    """Get the current status of the indexer"""
    try:
        indexer_client = get_search_indexer_client()
        status = indexer_client.get_indexer_status(INDEXER_NAME)
        
        return {
            "indexer_name": INDEXER_NAME,
            "status": status.status,
            "last_result": {
                "status": status.last_result.status if status.last_result else None,
                "error_message": status.last_result.error_message if status.last_result else None,
                "items_processed": status.last_result.items_processed if status.last_result else 0,
                "items_failed": status.last_result.items_failed if status.last_result else 0
            } if status.last_result else None
        }
        
    except Exception as e:
        logger.error(f"Error getting indexer status: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/indexer/reset")
async def reset_indexer():
    """Reset the indexer to reprocess all documents"""
    try:
        indexer_client = get_search_indexer_client()
        indexer_client.reset_indexer(INDEXER_NAME)
        
        logger.info(f"Indexer '{INDEXER_NAME}' reset successfully")
        
        return {
            "status": "success",
            "message": f"Indexer '{INDEXER_NAME}' has been reset"
        }
        
    except Exception as e:
        logger.error(f"Error resetting indexer: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.delete("/document/{document_id}")
async def delete_document(document_id: str, user_id: str):
    """Delete all chunks for a specific document"""
    try:
        search_client = get_search_client()
        
        # Search for all chunks of this document
        results = search_client.search(
            search_text="*",
            filter=f"document_id eq '{document_id}' and user_id eq '{user_id}'",
            select=["chunk_id"]
        )
        
        chunk_ids = [doc["chunk_id"] for doc in results]
        
        if chunk_ids:
            documents_to_delete = [{"chunk_id": cid} for cid in chunk_ids]
            search_client.delete_documents(documents=documents_to_delete)
            
            logger.info(f"Deleted {len(chunk_ids)} chunks for document {document_id}")
            
            return {
                "status": "success",
                "chunks_deleted": len(chunk_ids)
            }
        else:
            return {
                "status": "success",
                "chunks_deleted": 0,
                "message": "No chunks found for this document"
            }
            
    except Exception as e:
        logger.error(f"Error deleting document: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8084)
