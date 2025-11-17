
import os
import logging

logger = logging.getLogger(__name__)


class AppSettings:
    
    def __init__(self):
        # Azure OpenAI Settings
        self.azure_openai_endpoint = os.getenv("FOUNDRY_ENDPOINT")
        self.azure_openai_gpt_deployment = os.getenv("AZURE_OPENAI_CHAT_MODEL")
        self.azure_openai_embedding_deployment = os.getenv("AZURE_OPENAI_EMBEDDINGS", "text-embedding-3-small")
        self.azure_openai_api_version = os.getenv("AZURE_OPENAI_API_VERSION", "2024-10-21")
        
        # Azure AI Search Settings
        self.azure_search_service_url = os.getenv("SEARCH_SERVICE_ENDPOINT")
        self.azure_search_index_name = os.getenv("AZURE_SEARCH_INDEX_NAME", "docling-rag-sample")
        
        # Other settings
        self.system_prompt = os.getenv(
            "SYSTEM_PROMPT",
            """You are an AI assistant that helps people find information. **Generate Response to User Query**
                **Step 1: Parse Context Information**
                Extract and utilize relevant knowledge from the provided context within `<context></context>` XML tags.
                **Step 2: Analyze User Query**
                Carefully read and comprehend the user's query, pinpointing the key concepts, entities, and intent behind the question.
                **Step 3: Determine Response**
                If the answer to the user's query can be directly inferred from the context information, provide a concise and accurate response in the same language as the user's query.
                **Step 4: Handle Uncertainty**
                If the answer is not clear, ask the user for clarification to ensure an accurate response.
                **Step 5: Avoid Context Attribution**
                When formulating your response, do not indicate that the information was derived from the context.
                **Step 6: Respond in User's Language**
                Maintain consistency by ensuring the response is in the same language as the user's query.
                **Step 7: Provide Response**
                Generate a clear, concise, and informative response to the user's query, adhering to the guidelines outlined above.
                User Query: [query]
                <context>
                [context]
                </context>"""
        )
        
        # Optional port setting
        self.port = int(os.getenv("PORT", 8085))
        
        # Validate required settings
        self._validate_required_settings()
    
    def _validate_required_settings(self):
        """Validate that all required environment variables are set"""
        required_vars = {
            "FOUNDRY_ENDPOINT": self.azure_openai_endpoint,
            "AZURE_OPENAI_CHAT_MODEL": self.azure_openai_gpt_deployment,
            "SEARCH_SERVICE_ENDPOINT": self.azure_search_service_url
        }
        
        missing_vars = [name for name, value in required_vars.items() if not value]
        if missing_vars:
            raise ValueError(f"Missing required environment variables: {', '.join(missing_vars)}")
    
    @property
    def openai(self):
        """Return OpenAI settings as a dictionary"""
        return {
            "endpoint": self.azure_openai_endpoint,
            "gpt_deployment": self.azure_openai_gpt_deployment,
            "embedding_deployment": self.azure_openai_embedding_deployment
        }
    
    @property
    def search(self):
        """Return Search settings as a dictionary"""
        return {
            "url": self.azure_search_service_url,
            "index_name": self.azure_search_index_name
        }


# Create settings instance - environment variables will be loaded automatically
# This creates a singleton instance that can be imported throughout the app
settings = AppSettings()