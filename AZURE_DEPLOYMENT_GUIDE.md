# 🚀 Azure Deployment Guide - Quiz Service Integration

**Date**: November 20, 2025  
**Status**: Ready for Azure deployment

---

## 📋 Prerequisites Checklist

Before deploying, ensure you have:

- [x] Azure CLI installed and logged in
- [x] Access to Azure Container Registry (ACR): `acraila03478.azurecr.io`
- [x] Access to Container App Environment: `aca-env-aila`
- [x] Resource Group: `MLops_project`
- [x] All environment variables ready (Azure OpenAI keys, database connections)

---

## 🎯 Step-by-Step Deployment Process

### Step 1: Login to Azure Container Registry

```bash
# Login to your ACR
az acr login --name acraila03478

# Verify login
az acr repository list --name acraila03478 --output table
```

---

### Step 2: Build and Push Quiz Service Docker Image

```bash
# Navigate to quiz-service directory
cd /home/ammara/Documents/cloud-native-ai-learning-assistant/backend/quiz-service

# Build the Docker image
docker build -t acraila03478.azurecr.io/quiz-service:latest .

# Push to Azure Container Registry
docker push acraila03478.azurecr.io/quiz-service:latest

# Verify the push
az acr repository show-tags --name acraila03478 --repository quiz-service --output table
```

---

### Step 3: Build and Push Quiz AI Service (Python)

```bash
# Navigate to quiz-generation-service directory
cd /home/ammara/Documents/cloud-native-ai-learning-assistant/ai-services/quiz-generation-service

# Build the Docker image
docker build -t acraila03478.azurecr.io/quiz-generation-service:latest .

# Push to Azure Container Registry
docker push acraila03478.azurecr.io/quiz-generation-service:latest

# Verify
az acr repository show-tags --name acraila03478 --repository quiz-generation-service --output table
```

---

### Step 4: Deploy Quiz Service to Azure Container Apps

```bash
# Create/Update the quiz-service container app
az containerapp create \
  --name quiz-service \
  --resource-group MLops_project \
  --environment aca-env-aila \
  --image acraila03478.azurecr.io/quiz-service:latest \
  --registry-server acraila03478.azurecr.io \
  --target-port 8083 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 1.0 \
  --memory 2.0Gi \
  --env-vars \
    "DB_HOST=<your-postgres-host>" \
    "DB_PORT=5432" \
    "DB_NAME=quizdb" \
    "DB_USER=<your-db-user>" \
    "DB_PASSWORD=<your-db-password>" \
    "QUIZ_GENERATION_URL=https://quiz-generation-service.<your-aca-url>" \
    "DOCUMENT_SERVICE_URL=https://document-service.<your-aca-url>"

# Get the URL
az containerapp show \
  --name quiz-service \
  --resource-group MLops_project \
  --query properties.configuration.ingress.fqdn \
  --output tsv
```

---

### Step 5: Deploy Quiz AI Service to Azure Container Apps

```bash
# Create/Update the quiz-generation-service container app
az containerapp create \
  --name quiz-generation-service \
  --resource-group MLops_project \
  --environment aca-env-aila \
  --image acraila03478.azurecr.io/quiz-generation-service:latest \
  --registry-server acraila03478.azurecr.io \
  --target-port 8086 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 2 \
  --cpu 0.5 \
  --memory 1.0Gi \
  --env-vars \
    "AZURE_OPENAI_ENDPOINT=<your-openai-endpoint>" \
    "AZURE_OPENAI_API_KEY=<your-openai-key>" \
    "AZURE_OPENAI_DEPLOYMENT_NAME=<your-deployment-name>" \
    "AZURE_OPENAI_API_VERSION=2024-02-15-preview"

# Get the URL
az containerapp show \
  --name quiz-generation-service \
  --resource-group MLops_project \
  --query properties.configuration.ingress.fqdn \
  --output tsv
```

---

### Step 6: Update Document Service Environment Variables

Your friend's document-service is already deployed. We just need to ensure it has the correct configuration:

```bash
# Verify document-service is running and get its URL
az containerapp show \
  --name document-service \
  --resource-group MLops_project \
  --query properties.configuration.ingress.fqdn \
  --output tsv
```

---

### Step 7: Update Frontend Environment Variables

After deploying backend services, update the frontend to point to the new Azure URLs:

```bash
# Navigate to frontend directory
cd /home/ammara/Documents/cloud-native-ai-learning-assistant/frontend

# Create .env.production file with Azure URLs
cat > .env.production << 'EOF'
VITE_API_BASE_URL=https://<your-frontend-url>
VITE_USER_SERVICE_URL=https://user-service.<your-aca-url>
VITE_DOCUMENT_SERVICE_URL=https://document-service.<your-aca-url>
VITE_CHAT_SERVICE_URL=https://chat-service.<your-aca-url>
VITE_QUIZ_SERVICE_URL=https://quiz-service.<your-aca-url>
EOF

# Build frontend with production config
npm run build

# Build and push frontend Docker image
docker build -t acraila03478.azurecr.io/frontend:latest .
docker push acraila03478.azurecr.io/frontend:latest

# Update frontend container app
az containerapp update \
  --name frontend \
  --resource-group MLops_project \
  --image acraila03478.azurecr.io/frontend:latest
```

---

## 🔧 Quick Deployment Script

Save this as `deploy-quiz-to-azure.sh`:

```bash
#!/bin/bash

set -e

echo "🚀 Starting Azure Deployment for Quiz Services..."

# Configuration
ACR_NAME="acraila03478"
RESOURCE_GROUP="MLops_project"
ENVIRONMENT="aca-env-aila"

# Login to ACR
echo "🔐 Logging into Azure Container Registry..."
az acr login --name $ACR_NAME

# Build and Push Quiz Service
echo "🏗️  Building Quiz Service..."
cd backend/quiz-service
docker build -t $ACR_NAME.azurecr.io/quiz-service:latest .
docker push $ACR_NAME.azurecr.io/quiz-service:latest

# Build and Push Quiz AI Service
echo "🏗️  Building Quiz AI Service..."
cd ../../ai-services/quiz-generation-service
docker build -t $ACR_NAME.azurecr.io/quiz-generation-service:latest .
docker push $ACR_NAME.azurecr.io/quiz-generation-service:latest

cd ../..

echo "✅ Images pushed successfully!"
echo ""
echo "📝 Next steps:"
echo "1. Update container apps with new images"
echo "2. Configure environment variables"
echo "3. Test the endpoints"
echo ""
echo "🌐 Get service URLs:"
echo "   az containerapp list --resource-group $RESOURCE_GROUP --query '[].{Name:name, URL:properties.configuration.ingress.fqdn}' --output table"
```

---

## 🧪 Testing on Azure

### 1. Get All Service URLs

```bash
az containerapp list \
  --resource-group MLops_project \
  --query '[].{Name:name, URL:properties.configuration.ingress.fqdn}' \
  --output table
```

### 2. Test Quiz Service Health

```bash
QUIZ_SERVICE_URL=$(az containerapp show --name quiz-service --resource-group MLops_project --query properties.configuration.ingress.fqdn -o tsv)

curl https://$QUIZ_SERVICE_URL/api/quizzes/test
```

### 3. Test Document Service Text Endpoint

```bash
DOCUMENT_SERVICE_URL=$(az containerapp show --name document-service --resource-group MLops_project --query properties.configuration.ingress.fqdn -o tsv)

# First, get a document ID from your system
# Then test:
curl https://$DOCUMENT_SERVICE_URL/api/documents/<DOCUMENT_ID>/text
```

### 4. Test End-to-End Quiz Creation

```bash
# Get your auth token first from the frontend
TOKEN="<your-jwt-token>"

# Upload a document
curl -X POST "https://$DOCUMENT_SERVICE_URL/api/documents/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test-document.txt" \
  -F "userId=1"

# Note the documentId from response

# Create a quiz using the documentId
curl -X POST "https://$QUIZ_SERVICE_URL/api/quizzes" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Quiz from Azure",
    "documentId": "<DOCUMENT_ID>",
    "userId": 1
  }'
```

---

## 📊 Environment Variables Reference

### Quiz Service (Java)
```bash
DB_HOST=<your-azure-postgres-host>
DB_PORT=5432
DB_NAME=quizdb
DB_USER=<username>
DB_PASSWORD=<password>
QUIZ_GENERATION_URL=https://quiz-generation-service.<your-url>
DOCUMENT_SERVICE_URL=https://document-service.<your-url>
```

### Quiz AI Service (Python)
```bash
AZURE_OPENAI_ENDPOINT=<your-endpoint>
AZURE_OPENAI_API_KEY=<your-key>
AZURE_OPENAI_DEPLOYMENT_NAME=<deployment-name>
AZURE_OPENAI_API_VERSION=2024-02-15-preview
```

### Frontend
```bash
VITE_QUIZ_SERVICE_URL=https://quiz-service.<your-url>
VITE_DOCUMENT_SERVICE_URL=https://document-service.<your-url>
VITE_USER_SERVICE_URL=https://user-service.<your-url>
VITE_CHAT_SERVICE_URL=https://chat-service.<your-url>
```

---

## 🔍 Monitoring and Debugging

### View Container Logs

```bash
# Quiz Service logs
az containerapp logs show \
  --name quiz-service \
  --resource-group MLops_project \
  --follow

# Quiz AI Service logs
az containerapp logs show \
  --name quiz-generation-service \
  --resource-group MLops_project \
  --follow
```

### Check Container App Status

```bash
az containerapp show \
  --name quiz-service \
  --resource-group MLops_project \
  --query 'properties.{Status:runningStatus, URL:configuration.ingress.fqdn, Replicas:template.scale}'
```

### Update Environment Variables Without Redeployment

```bash
az containerapp update \
  --name quiz-service \
  --resource-group MLops_project \
  --set-env-vars "DOCUMENT_SERVICE_URL=https://new-url"
```

---

## ⚠️ Common Issues and Solutions

### Issue 1: "Cannot connect to document-service"
**Solution**: Ensure DOCUMENT_SERVICE_URL uses the internal Azure URL, not localhost

### Issue 2: "Database connection refused"
**Solution**: Check PostgreSQL firewall rules allow Azure services

### Issue 3: "CORS errors in browser"
**Solution**: Verify CORS configuration in quiz-service includes the frontend URL

### Issue 4: "Quiz generation timeout"
**Solution**: Increase quiz-generation-service CPU/memory or timeout settings

---

## 🎯 Deployment Checklist

- [ ] ACR login successful
- [ ] Quiz-service Docker image built and pushed
- [ ] Quiz-generation-service Docker image built and pushed
- [ ] Container app created/updated for quiz-service
- [ ] Container app created/updated for quiz-generation-service
- [ ] Environment variables configured (database, OpenAI, service URLs)
- [ ] Service URLs obtained and documented
- [ ] Health check endpoints tested
- [ ] Document text endpoint tested
- [ ] End-to-end quiz creation tested
- [ ] Frontend updated with Azure URLs
- [ ] CORS configured correctly
- [ ] Logs monitored for errors

---

## 📚 Useful Azure CLI Commands

```bash
# List all container apps
az containerapp list --resource-group MLops_project --output table

# Get specific app details
az containerapp show --name quiz-service --resource-group MLops_project

# Scale a service
az containerapp update --name quiz-service --resource-group MLops_project --min-replicas 2 --max-replicas 5

# Restart a service
az containerapp revision restart --name quiz-service --resource-group MLops_project

# Delete a service
az containerapp delete --name quiz-service --resource-group MLops_project --yes
```

---

## 🎉 Success Criteria

Your deployment is successful when:

1. ✅ All container apps show "Running" status
2. ✅ Health endpoints return 200 OK
3. ✅ Document text endpoint returns extracted text
4. ✅ Quiz creation works with documentId
5. ✅ Frontend can access all backend services
6. ✅ No CORS errors in browser console
7. ✅ Logs show successful service communication

---

**Next Steps After Deployment:**
1. Share the frontend URL with stakeholders
2. Monitor performance and errors
3. Set up Azure Application Insights (optional)
4. Configure automatic scaling based on load
5. Set up CI/CD pipeline for automated deployments

