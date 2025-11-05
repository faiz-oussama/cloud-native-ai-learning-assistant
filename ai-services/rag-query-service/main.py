import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from azure.core.credentials import AzureKeyCredential
from azure.search.documents import SearchClient
from azure.search.documents.models import VectorizableTextQuery
from openai import AzureOpenAI
import uvicorn
import logging
from typing import List, Optional

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="RAG Query Service", version="1.0.0")

# Environment variables
SEARCH_SERVICE_ENDPOINT = os.getenv("SEARCH_SERVICE_ENDPOINT")
SEARCH_SERVICE_KEY = os.getenv("SEARCH_SERVICE_KEY")
FOUNDRY_ENDPOINT = os.getenv("FOUNDRY_ENDPOINT")
FOUNDRY_KEY = os.getenv("FOUNDRY_KEY")
INDEX_NAME = "documents-index"

# Grounded prompt template for RAG
GROUNDED_PROMPT = """You are an AI assistant that helps users learn from the information found in the source material.
Answer the query using only the sources provided below.
Use bullets if the answer has multiple points.
If the answer is longer than 3 sentences, provide a summary.
Answer ONLY with the facts listed in the list of sources below. Cite your source when you answer the question.
If there isn't enough information below, say you don't know.
Do not generate answers that don't use the sources below.
Query: {query}
Sources:
{sources}
"""

# Initialize Azure clients
def get_openai_client():
    return AzureOpenAI(
        api_version="2024-06-01",
        azure_endpoint=FOUNDRY_ENDPOINT,
        api_key=FOUNDRY_KEY
    )

def get_search_client():
    credential = AzureKeyCredential(SEARCH_SERVICE_KEY)
    return SearchClient(
        endpoint=SEARCH_SERVICE_ENDPOINT,
        index_name=INDEX_NAME,
        credential=credential
    )

# Request/Response models
class QueryRequest(BaseModel):
    query: str
    user_id: str
    document_id: Optional[str] = None
    top_k: int = 5
    temperature: float = 0.7

class Source(BaseModel):
    chunk_id: str
    document_id: str
    title: str
    content: str
    locations: Optional[List[str]] = []
    score: float

class QueryResponse(BaseModel):
    answer: str
    sources: List[Source]
    query: str
    model_used: str

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "rag-query-service"}

@app.post("/query", response_model=QueryResponse)
async def query_documents(request: QueryRequest):
    """Query documents using RAG pattern: hybrid search + LLM chat completion"""
    try:
        logger.info(f"Received query from user {request.user_id}: {request.query}")
        
        search_client = get_search_client()
        openai_client = get_openai_client()
        
        # Build filter for user isolation
        filter_query = f"user_id eq '{request.user_id}'"
        if request.document_id:
            filter_query += f" and document_id eq '{request.document_id}'"
        
        # Hybrid search: keyword search + vector search
        # Vector query uses text-to-vector conversion built into Azure Search
        vector_query = VectorizableTextQuery(
            text=request.query,
            k_nearest_neighbors=50,
            fields="text_vector"
        )
        
        # Execute hybrid search
        search_results = search_client.search(
            search_text=request.query,
            vector_queries=[vector_query],
            filter=filter_query,
            select=["chunk_id", "document_id", "title", "chunk", "locations"],
            top=request.top_k
        )
        
        # Collect search results and format for LLM
        sources = []
        sources_list = []
        
        for result in search_results:
            source = Source(
                chunk_id=result.get("chunk_id", ""),
                document_id=result.get("document_id", ""),
                title=result.get("title", "Unknown"),
                content=result.get("chunk", ""),
                locations=result.get("locations", []),
                score=result.get("@search.score", 0.0)
            )
            sources.append(source)
            sources_list.append(result)
        
        logger.info(f"Retrieved {len(sources)} relevant chunks via hybrid search")
        
        # Handle no results
        if not sources:
            return QueryResponse(
                answer="I couldn't find any relevant information in your documents to answer this question.",
                sources=[],
                query=request.query,
                model_used="gpt-4"
            )
        
        # Format sources for the LLM prompt
        # Use unique separator to make sources distinct
        sources_formatted = "=================\n".join([
            f'TITLE: {doc.get("title", "Unknown")}, CONTENT: {doc.get("chunk", "")}, LOCATIONS: {doc.get("locations", [])}'
            for doc in sources_list
        ])
        
        # Create grounded prompt with query and sources
        prompt = GROUNDED_PROMPT.format(
            query=request.query,
            sources=sources_formatted
        )
        
        # Call Azure OpenAI chat completion
        response = openai_client.chat.completions.create(
            model="gpt-4",  # or gpt-4o, gpt-35-turbo
            messages=[
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            temperature=request.temperature,
            max_tokens=800
        )
        
        answer = response.choices[0].message.content
        
        logger.info(f"Generated grounded answer for query: {request.query}")
        
        return QueryResponse(
            answer=answer,
            sources=sources,
            query=request.query,
            model_used="gpt-4"
        )
        
    except Exception as e:
        logger.error(f"Error processing query: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/search")
async def search_documents(request: QueryRequest):
    """Hybrid search without LLM generation (retrieval only)"""
    try:
        logger.info(f"Search request from user {request.user_id}: {request.query}")
        
        search_client = get_search_client()
        
        # Build filter
        filter_query = f"user_id eq '{request.user_id}'"
        if request.document_id:
            filter_query += f" and document_id eq '{request.document_id}'"
        
        # Hybrid search with vector query
        vector_query = VectorizableTextQuery(
            text=request.query,
            k_nearest_neighbors=50,
            fields="text_vector"
        )
        
        search_results = search_client.search(
            search_text=request.query,
            vector_queries=[vector_query],
            filter=filter_query,
            select=["chunk_id", "document_id", "title", "chunk", "locations"],
            top=request.top_k
        )
        
        sources = []
        for result in search_results:
            sources.append(Source(
                chunk_id=result.get("chunk_id", ""),
                document_id=result.get("document_id", ""),
                title=result.get("title", "Unknown"),
                content=result.get("chunk", ""),
                locations=result.get("locations", []),
                score=result.get("@search.score", 0.0)
            ))
        
        return {
            "query": request.query,
            "sources": sources,
            "count": len(sources),
            "search_type": "hybrid"
        }
        
    except Exception as e:
        logger.error(f"Error searching documents: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/chat")
async def chat_completion(request: QueryRequest):
    """Direct chat completion without document search (for testing)"""
    try:
        openai_client = get_openai_client()
        
        response = openai_client.chat.completions.create(
            model="gpt-4",
            messages=[
                {
                    "role": "user",
                    "content": request.query
                }
            ],
            temperature=request.temperature,
            max_tokens=800
        )
        
        return {
            "answer": response.choices[0].message.content,
            "query": request.query,
            "model_used": "gpt-4"
        }
        
    except Exception as e:
        logger.error(f"Error in chat completion: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8085)