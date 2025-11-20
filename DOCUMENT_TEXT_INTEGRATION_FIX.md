# 🔍 Document Text Flow Analysis & Fix

## Current Problem

Your quiz-service accepts `documentText` as a String in `CreateQuizRequest`, but the document-service doesn't currently extract and store the full text from PDFs/images.

### What Happens Now:
1. User uploads PDF/image → **document-service**
2. document-service sends file → **rag-ingest-service** (uses Docling + EasyOCR)
3. rag-ingest-service processes, chunks, and stores in Azure AI Search
4. **BUT**: The extracted text is NOT sent back to document-service
5. document-service.extractedText field remains **NULL**

### What You Need:
- quiz-service needs the full extracted text from the document
- Currently: `CreateQuizRequest` has `documentText` (String)
- Problem: Where does this text come from if the document is a PDF/image?

---

## Solution: Two Options

### Option A: Change Quiz-Service to Accept documentId (RECOMMENDED)

Instead of passing raw text, pass the documentId and let quiz-service fetch the text from document-service.

#### Changes Needed:

**1. Update CreateQuizRequest:**
```java
// backend/quiz-service/src/main/java/com/learningassistant/quiz/dto/CreateQuizRequest.java
package com.learningassistant.quiz.dto;

public record CreateQuizRequest(
        String title,
        Long documentId,  // Changed from documentText
        Long userId
) {
}
```

**2. Create DocumentServiceClient in quiz-service:**
```java
// backend/quiz-service/src/main/java/com/learningassistant/quiz/client/DocumentServiceClient.java
package com.learningassistant.quiz.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class DocumentServiceClient {
    
    private final RestTemplate restTemplate;
    private final String documentServiceUrl;
    
    public DocumentServiceClient(RestTemplate restTemplate,
                                @Value("${services.document-service.url}") String documentServiceUrl) {
        this.restTemplate = restTemplate;
        this.documentServiceUrl = documentServiceUrl;
    }
    
    public String getDocumentText(Long documentId) {
        String url = documentServiceUrl + "/api/documents/" + documentId;
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        
        if (response != null && response.containsKey("extractedText")) {
            return (String) response.get("extractedText");
        }
        
        throw new RuntimeException("Document text not found for documentId: " + documentId);
    }
}
```

**3. Update QuizService:**
```java
// backend/quiz-service/src/main/java/com/learningassistant/quiz/service/QuizService.java
@Autowired
private DocumentServiceClient documentServiceClient;

public Quiz createAndSaveQuiz(CreateQuizRequest request) {
    // Fetch document text from document-service
    String documentText = documentServiceClient.getDocumentText(request.documentId());
    
    // Rest of your existing logic...
    List<QuizSubmission> history = submissionRepository.findByUserId(request.userId());
    String difficulty = determineDifficulty(history);
    
    QuizGenerationRequest aiRequest = new QuizGenerationRequest(
        documentText,  // Now fetched from document-service
        5, 
        difficulty
    );
    // ... rest remains the same
}
```

**4. Add to application.yml:**
```yaml
services:
  quiz-generation:
    url: ${QUIZ_GENERATION_URL:http://localhost:8086}
  document-service:
    url: ${DOCUMENT_SERVICE_URL:http://localhost:8081}
```

---

### Option B: Fix RAG Ingest to Return Full Text (ALTERNATIVE)

Modify rag-ingest-service to return the extracted text back to document-service.

#### Changes Needed:

**1. Update rag-ingest-service response:**
```python
# ai-services/rag-ingest-service/main.py
@app.post("/ingest-docling")
async def ingest_document_docling(request: DocumentIngestRequest):
    # ... existing processing ...
    
    # Before returning, compile full text
    full_text = " ".join([chunk["content"] for chunk in all_chunks])
    
    return {
        "message": "Document ingested successfully",
        "document_id": request.document_id,
        "chunks_created": len(upload_docs),
        "extracted_text": full_text  # NEW: Return full text
    }
```

**2. Update document-service to save extracted text:**
```java
// backend/document-service/src/main/java/com/learningassistant/document/service/DocumentService.java
Map<String, Object> ragResponse = ragIngestClient.triggerDocumentIngestion(...);

if (ragResponse != null && ragResponse.containsKey("extracted_text")) {
    document.setExtractedText((String) ragResponse.get("extracted_text"));
    document.setProcessingStatus(ProcessingStatus.COMPLETED);
    documentRepository.save(document);
}
```

---

## Recommendation: Use Option A

**Why?**
1. ✅ Clean separation of concerns
2. ✅ No changes to your friend's rag-ingest-service
3. ✅ quiz-service fetches what it needs when it needs it
4. ✅ Reuses existing document-service API
5. ✅ Easier to test

**Option B requires:**
- ❌ Modifying rag-ingest-service (your friend's code)
- ❌ Large text in HTTP responses (can be slow)
- ❌ Storing duplicate data in document-service DB

---

## Current Status & Next Steps

### What Works Now:
- ✅ rag-ingest processes PDFs/images with Docling + EasyOCR
- ✅ Text is extracted and chunked
- ✅ Chunks are stored in Azure AI Search

### What's Missing:
- ❌ Extracted text not available to quiz-service
- ❌ quiz-service expects raw text string

### Immediate Action Required:

**Before deploying to Azure**, you need to:

1. **Decide**: Option A (documentId) or Option B (return text)?
2. **Implement** the chosen option
3. **Test** locally with a PDF upload
4. **Verify** quiz generation works with extracted text

---

## Test Plan

### With Option A (documentId approach):

```bash
# 1. Upload a PDF via document-service
curl -X POST http://localhost:8081/api/documents/upload \
  -F "file=@sample.pdf" \
  -F "userId=1"

# Response: { "documentId": "abc123", ... }

# 2. Wait for RAG processing to complete
curl http://localhost:8081/api/documents/abc123/status

# 3. Create quiz using documentId
curl -X POST http://localhost:8083/api/quizzes \
  -H "Content-Type: application/json" \
  -d '{
    "title": "PDF Quiz",
    "documentId": "abc123",
    "userId": 1
  }'
```

---

## Decision Time

**Which option do you want to implement?**

- **Option A**: Change quiz-service to accept `documentId` and fetch text from document-service
- **Option B**: Modify rag-ingest to return full extracted text

I recommend **Option A** because it doesn't touch your friend's working code.

Want me to implement Option A for you right now?

