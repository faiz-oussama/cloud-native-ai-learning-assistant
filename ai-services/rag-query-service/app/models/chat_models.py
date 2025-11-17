"""
Chat models for FastAPI RAG application

This module defines Pydantic models that:
1. Validate the structure of API requests
2. Provide type hints for better development experience
3. Generate automatic API documentation with FastAPI
4. Support JSON serialization/deserialization

The models focus on the core message structures needed for the chat interface.
"""
from typing import List, Optional
from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    """
    Base chat message model representing a single message in the conversation
    
    Compatible with Azure OpenAI's message format, where:
    - role: can be 'system', 'user', or 'assistant'
    - content: contains the actual message text
    """
    role: str
    content: str


class ChatRequest(BaseModel):
    """Chat completion request model for the API endpoint"""
    messages: List[ChatMessage] = Field(..., description="List of chat messages")


class QueryRequest(BaseModel):
    """Query request model for the API endpoint"""
    query: str
    user_id: str
    document_id: Optional[str] = None
    top_k: int = 5
    temperature: float = 0.7


class QueryResponse(BaseModel):
    """Query response model matching the expected format"""
    answer: str
    query: str
    model_used: str = "gpt-4"