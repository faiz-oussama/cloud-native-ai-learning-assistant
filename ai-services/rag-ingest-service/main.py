import os
import json
import httpx
import requests
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from azure.core.credentials import AzureKeyCredential
from azure.search.documents import SearchClient
from azure.search.documents.indexes import SearchIndexClient
from azure.search.documents.indexes.models import (
    SearchIndex,
    SimpleField,
    SearchableField,
    SearchField,
    SearchFieldDataType,
    VectorSearch,
    VectorSearchProfile,
    HnswAlgorithmConfiguration,
    AzureOpenAIVectorizer,
    AzureOpenAIVectorizerParameters
)
import asyncio
import logging
from typing import Optional
from contextlib import asynccontextmanager
from docling.document_converter import DocumentConverter
from docling.chunking import HierarchicalChunker
from openai import AzureOpenAI
from rich.console import Console
import uvicorn
from dotenv import load_dotenv
import uuid

# Import EasyOCR
import easyocr

load_dotenv()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
console = Console()

# Initialize EasyOCR reader once at startup
ocr_reader = None

# Cache for indexer status
indexer_status_cache = {
    "data": None,
    "timestamp": 0,
    "ttl": 30  # Cache for 30 seconds
}

# Environment variables (must be loaded before app initialization)
SEARCH_SERVICE_ENDPOINT = os.getenv("SEARCH_SERVICE_ENDPOINT")
SEARCH_SERVICE_KEY = os.getenv("SEARCH_SERVICE_KEY")
FOUNDRY_ENDPOINT = os.getenv("FOUNDRY_ENDPOINT")
FOUNDRY_KEY = os.getenv("FOUNDRY_KEY")
AZURE_OPENAI_API_VERSION = os.getenv("AZURE_OPENAI_API_VERSION", "2024-10-21")
AZURE_OPENAI_CHAT_MODEL = os.getenv("AZURE_OPENAI_CHAT_MODEL", "gpt-4o")
AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT = os.getenv("AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT", "text-embedding-3-small")
AZURE_OPENAI_EMBEDDINGS = os.getenv("AZURE_OPENAI_EMBEDDINGS", "text-embedding-3-small")
AZURE_SEARCH_INDEX_NAME = os.getenv("AZURE_SEARCH_INDEX_NAME", "docling-rag-sample")
DOCUMENT_SERVICE_URL = os.getenv("DOCUMENT_SERVICE_URL", "https://document-service.niceplant-c464d163.swedencentral.azurecontainerapps.io")

# Vector dimensions for embedding model
VECTOR_DIM = 1536  # text-embedding-3-small dimension

# Initialize Azure clients
def get_search_index_client():
    credential = AzureKeyCredential(SEARCH_SERVICE_KEY)
    return SearchIndexClient(endpoint=SEARCH_SERVICE_ENDPOINT, credential=credential)

def get_search_client():
    credential = AzureKeyCredential(SEARCH_SERVICE_KEY)
    return SearchClient(endpoint=SEARCH_SERVICE_ENDPOINT, index_name=AZURE_SEARCH_INDEX_NAME, credential=credential)

def get_openai_client():
    return AzureOpenAI(
        api_version=AZURE_OPENAI_API_VERSION,
        azure_endpoint=FOUNDRY_ENDPOINT,
        api_key=FOUNDRY_KEY
    )

# Initialize search index on startup
async def initialize_search_index():
    """Create search index with vector search configuration"""
    try:
        index_client = get_search_index_client()
        
        logger.info(f"Checking for existing search index: {AZURE_SEARCH_INDEX_NAME}")
        try:
            existing_index = index_client.get_index(AZURE_SEARCH_INDEX_NAME)
            logger.info(f"Search index '{AZURE_SEARCH_INDEX_NAME}' already exists, skipping creation")
            return
        except Exception as e:
            logger.info(f"Index does not exist, will create it: {str(e)}")
        
        logger.info("Creating new search index...")
        # Define fields with proper properties to avoid serialization warnings
        fields = [
            SimpleField(name="chunk_id", type=SearchFieldDataType.String, key=True, filterable=True),
            SimpleField(name="document_id", type=SearchFieldDataType.String, filterable=True),
            SimpleField(name="user_id", type=SearchFieldDataType.String, filterable=True),
            SearchableField(name="title", type=SearchFieldDataType.String),
            SearchableField(name="content", type=SearchFieldDataType.String),
            SearchField(
                name="content_vector",
                type=SearchFieldDataType.Collection(SearchFieldDataType.Single),
                searchable=True,
                vector_search_dimensions=VECTOR_DIM,
                vector_search_profile_name="default"
            ),
        ]
        
        # Define vector search configuration
        vector_search = VectorSearch(
            algorithms=[
                HnswAlgorithmConfiguration(
                    name="default",
                    parameters={
                        "metric": "cosine"
                    }
                )
            ],
            profiles=[
                VectorSearchProfile(
                    name="default",
                    algorithm_configuration_name="default"
                )
            ]
        )
        
        # Create index with proper configuration
        index = SearchIndex(
            name=AZURE_SEARCH_INDEX_NAME,
            fields=fields,
            vector_search=vector_search
        )
        
        result = index_client.create_or_update_index(index)
        logger.info(f"Search index '{AZURE_SEARCH_INDEX_NAME}' created successfully")
        logger.info(f"Index creation result: {result.name}")
        
    except Exception as e:
        logger.error(f"Error initializing search index: {str(e)}", exc_info=True)
        raise

# Initialize EasyOCR reader
def initialize_ocr_reader():
    """Initialize EasyOCR reader once at startup"""
    global ocr_reader
    if ocr_reader is None:
        logger.info("Initializing EasyOCR reader...")
        try:
            ocr_reader = easyocr.Reader(['en'], download_enabled=False)  # Disable download since we preloaded
            logger.info("EasyOCR reader initialized successfully")
        except Exception as e:
            logger.error(f"Error initializing EasyOCR reader: {str(e)}")
            # Fallback to allowing downloads if preloaded models fail
            try:
                ocr_reader = easyocr.Reader(['en'], download_enabled=True)
                logger.info("EasyOCR reader initialized with download fallback")
            except Exception as e2:
                logger.error(f"Fallback initialization also failed: {str(e2)}")
                raise

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Handle startup and shutdown events using lifespan context manager"""
    # Startup
    logger.info("RAG Ingest Service starting up...")
    try:
        await initialize_search_index()
        initialize_ocr_reader()  # Initialize OCR reader at startup
    except Exception as e:
        logger.error(f"Error during startup initialization: {str(e)}", exc_info=True)
    yield
    # Shutdown
    logger.info("RAG Ingest Service shutting down")

app = FastAPI(title="RAG Ingest Service with Docling", version="1.0.0", lifespan=lifespan)

# CORS Configuration - Allow all requests for production testing
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["*"]
)

# Request models
class DocumentIngestRequest(BaseModel):
    document_id: str
    user_id: str
    document_url: str  # URL to the document (PDF, etc.)
    document_title: Optional[str] = None

def embed_text(text: str) -> list:
    """Generate embeddings using Azure OpenAI"""
    try:
        openai_client = get_openai_client()
        response = openai_client.embeddings.create(
            input=text,
            model=AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT
        )
        return response.data[0].embedding
    except Exception as e:
        logger.error(f"Error generating embedding: {str(e)}")
        raise

def embed_texts(texts: list) -> list:
    """Generate embeddings for multiple texts using Azure OpenAI batch API"""
    try:
        openai_client = get_openai_client()
        response = openai_client.embeddings.create(
            input=texts,
            model=AZURE_OPENAI_EMBEDDINGS_DEPLOYMENT
        )
        return [data.embedding for data in response.data]
    except Exception as e:
        logger.error(f"Error generating embeddings: {str(e)}")
        raise

async def notify_document_service_completion(document_id: str, max_retries: int = 5):
    """Notify document-service of completion - disabled for background processing"""
    logger.info(f"[INFO] Document {document_id} processing completed - no callback to document service")
    return True

async def notify_document_service_failure(document_id: str, max_retries: int = 3):
    """Notify document-service of failure - disabled for background processing"""
    logger.info(f"[INFO] Document {document_id} processing failed - no callback to document service")
    return True

@app.post("/ingest-docling")
async def ingest_document_docling(request: DocumentIngestRequest):
    """
    Ingest and process a document using Docling:
    1. Parse PDF/document with Docling
    2. Chunk using HierarchicalChunker
    3. Generate embeddings using Azure OpenAI
    4. Upload to Azure AI Search
    5. No callback - process independently
    """
    try:
        logger.info(f"Starting Docling ingestion for document: {request.document_id}, user: {request.user_id}")
        console.print(f"[bold yellow]Processing document: {request.document_url}[/bold yellow]")
        
        # Step 1: Parse document with Docling or handle text files
        if request.document_url.lower().endswith('.txt'):
            # Handle text files directly
            logger.info(f"Processing text file directly: {request.document_url}")
            response = requests.get(request.document_url, timeout=30)
            response.raise_for_status()
            text_content = response.text
            
            # Create a simple document structure for chunking
            from docling.datamodel.base import Document
            from docling.datamodel.document import InputDocument
            from docling_core.types.doc.document import DoclingDocument
            
            # Create a mock result structure for text files
            class MockResult:
                def __init__(self, text):
                    self.document = DoclingDocument()
                    # Add the text content to the document
                    self.document.text = text
            
            result = MockResult(text_content)
            console.print("[green]✓ Text file processed successfully[/green]")
        else:
            # Handle other document types with Docling
            converter = DocumentConverter()
            logger.info(f"Parsing document from URL: {request.document_url}")
            result = converter.convert(request.document_url)
            console.print("[green]✓ Document parsed successfully[/green]")
        
        # Step 2: Chunk the document
        chunker = HierarchicalChunker()
        doc_chunks = list(chunker.chunk(result.document))
        logger.info(f"Document chunked into {len(doc_chunks)} chunks")
        
        all_chunks = []
        for idx, chunk in enumerate(doc_chunks):
            chunk_id = f"{request.document_id}_chunk_{idx}"
            chunk_text = chunk.text
            all_chunks.append({
                "chunk_id": chunk_id,
                "content": chunk_text,
                "document_id": request.document_id,
                "user_id": request.user_id,
                "title": request.document_title or "Untitled"
            })
        
        logger.info(f"Created {len(all_chunks)} chunks for indexing")
        
        # Step 3: Generate embeddings and prepare documents for upload
        console.print("[bold yellow]Generating embeddings...[/bold yellow]")
        upload_docs = []
        
        # Batch embedding generation for better performance
        BATCH_SIZE = 10  # Azure OpenAI has limits on batch size
        
        for i in range(0, len(all_chunks), BATCH_SIZE):
            batch_chunks = all_chunks[i:i + BATCH_SIZE]
            batch_texts = [chunk["content"] for chunk in batch_chunks]
            
            try:
                # Generate embeddings for the batch
                embedding_vectors = embed_texts(batch_texts)
                
                # Add documents with embeddings to upload list
                for j, chunk in enumerate(batch_chunks):
                    upload_docs.append({
                        "chunk_id": chunk["chunk_id"],
                        "content": chunk["content"],
                        "content_vector": embedding_vectors[j],
                        "document_id": chunk["document_id"],
                        "user_id": chunk["user_id"],
                        "title": chunk["title"]
                    })
            except Exception as e:
                logger.error(f"Error embedding batch {i//BATCH_SIZE}: {str(e)}")
                # Fallback to individual embedding generation for failed batch
                for chunk in batch_chunks:
                    try:
                        embedding_vector = embed_text(chunk["content"])
                        upload_docs.append({
                            "chunk_id": chunk["chunk_id"],
                            "content": chunk["content"],
                            "content_vector": embedding_vector,
                            "document_id": chunk["document_id"],
                            "user_id": chunk["user_id"],
                            "title": chunk["title"]
                        })
                    except Exception as inner_e:
                        logger.error(f"Error embedding chunk {chunk['chunk_id']}: {str(inner_e)}")
                        continue
        
        console.print(f"[green]✓ Generated embeddings for {len(upload_docs)} chunks[/green]")
        
        # Step 4: Upload to Azure AI Search in batches
        search_client = get_search_client()
        BATCH_SIZE = 50
        
        console.print("[bold yellow]Uploading to Azure AI Search...[/bold yellow]")
        for i in range(0, len(upload_docs), BATCH_SIZE):
            subset = upload_docs[i : i + BATCH_SIZE]
            resp = search_client.upload_documents(documents=subset)
            all_succeeded = all(r.succeeded for r in resp)
            logger.info(f"Uploaded batch {i} -> {i + len(subset)}; all_succeeded: {all_succeeded}")
        
        console.print(f"[green]✓ All {len(upload_docs)} chunks uploaded to Azure AI Search[/green]")
        
        # Step 5: No callback - just log completion
        logger.info(f"[SUCCESS] Document {request.document_id} processed and indexed successfully - no callback")
        
        return {
            "status": "success",
            "message": "Document ingested successfully with Docling",
            "document_id": request.document_id,
            "chunks_created": len(all_chunks),
            "chunks_indexed": len(upload_docs)
        }
        
    except Exception as e:
        logger.error(f"[ERROR] Error in Docling ingestion: {str(e)}", exc_info=True)
        # No callback on failure
        logger.info(f"[INFO] Document {request.document_id} processing failed - no callback")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "rag-ingest-service", "rag_type": "Docling + Azure AI Search"}

@app.get("/indexer/status")
async def get_indexer_status():
    """Check RAG service status with caching"""
    import time
    current_time = time.time()
    
    # Check if we have cached data that's still valid
    if (indexer_status_cache["data"] is not None and 
        current_time - indexer_status_cache["timestamp"] < indexer_status_cache["ttl"]):
        logger.info("Returning cached indexer status")
        return indexer_status_cache["data"]
    
    try:
        search_client = get_search_client()
        index_stats = search_client.get_search_statistics()
        
        result = {
            "status": "operational",
            "index_name": AZURE_SEARCH_INDEX_NAME,
            "document_count": index_stats.document_count,
            "storage_size_bytes": index_stats.storage_size
        }
        
        # Update cache
        indexer_status_cache["data"] = result
        indexer_status_cache["timestamp"] = current_time
        
        logger.info("Updated indexer status cache")
        return result
        
    except Exception as e:
        logger.error(f"Error getting indexer status: {str(e)}")
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