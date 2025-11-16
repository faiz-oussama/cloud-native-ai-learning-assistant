import os
import json
import logging
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import AzureOpenAI
from dotenv import load_dotenv

# --- Configuration ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)
load_dotenv()

app = FastAPI()

# --- Initialize Azure OpenAI Client ---
# This reads directly from your .env file
try:
    client = AzureOpenAI(
        api_key=os.getenv("AZURE_OPENAI_API_KEY"),
        api_version="2024-02-01",
        azure_endpoint=os.getenv("AZURE_OPENAI_ENDPOINT")
    )
    deployment_name = os.getenv("AZURE_OPENAI_DEPLOYMENT_GPT")
    logger.info(f"Azure OpenAI client initialized for deployment: {deployment_name}")
except Exception as e:
    logger.critical(f"Failed to initialize Azure OpenAI client: {e}")
    raise

# --- DTOs (to match your Java code) ---
class QuizGenerationRequest(BaseModel):
    text_content: str
    num_questions: int
    difficulty: str

class ExplanationRequest(BaseModel):
    context: str
    question: str
    wrongAnswer: str # Pydantic handles the Java camelCase

# --- System Prompts ---
QUIZ_PROMPT = """
You are an expert quiz generator.
You MUST generate a quiz in strictly valid JSON format.
The JSON must be an array of objects, where each object has:
- "question": The question string.
- "options": An array of 4 possible answers (strings).
- "correct_answer": The exact string of the correct option from the array.

You MUST return ONLY the raw JSON, with no other text, comments, or markdown.
Your response MUST start with [ and end with ].
"""

EXPLANATION_PROMPT = """
You are a helpful teaching assistant.
A student answered a question incorrectly.
Explain WHY their answer was wrong and what the correct answer is,
using the provided context. Keep it concise (2-3 sentences).
"""

# --- API Endpoints ---
@app.post("/generate-quiz")
async def generate_quiz(request: QuizGenerationRequest):
    logger.info(f"Received quiz request, difficulty: {request.difficulty}")
    try:
        response = client.chat.completions.create(
            model=deployment_name,
            messages=[
                {"role": "system", "content": QUIZ_PROMPT},
                {"role": "user", "content": f"Generate {request.num_questions} {request.difficulty} questions based on this text:\n\n{request.text_content}"}
            ],
            temperature=0.7,
            max_tokens=1000,
            # This is the correct format for gpt-4o
            response_format={"type": "json_object"} 
        )

        # --- AFTER ---
        raw_content = response.choices[0].message.content
        quiz_data = json.loads(raw_content)

        # The AI wraps the list in an object, e.g., {"questions": [...] }
        # We must extract the list and return *only* that.

        # Handle if it's {"questions": [...] }
        if isinstance(quiz_data, dict) and "questions" in quiz_data:
            return quiz_data["questions"]
        # Handle if it just returned the list correctly
        elif isinstance(quiz_data, list):
            return quiz_data
        # Handle if it just returned a single object (fallback)
        elif isinstance(quiz_data, dict):
            return [quiz_data]
        else:
            raise HTTPException(status_code=500, detail="AI returned unexpected JSON format")

    except Exception as e:
        logger.error(f"Error in generate_quiz: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/get-explanation")
async def get_explanation(request: ExplanationRequest):
    logger.info(f"Received explanation request for question: {request.question}")
    try:
        response = client.chat.completions.create(
            model=deployment_name,
            messages=[
                {"role": "system", "content": EXPLANATION_PROMPT},
                {"role": "user", "content": f"Context: {request.context}\n\nQuestion: {request.question}\n\nStudent's (wrong) answer: {request.wrongAnswer}"}
            ],
            temperature=0.5,
            max_tokens=200
        )

        explanation_text = response.choices[0].message.content
        # We must return a JSON object, just like our Java client expects
        return {"explanation": explanation_text}

    except Exception as e:
        logger.error(f"Error in get_explanation: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    # MUST match the port in your Java application.yml
    logger.info("Starting up Python 'Brain' on port 8086...")
    uvicorn.run(app, host="0.0.0.0", port=8086)