# 🚀 Azure Deployment & Testing Guide

## Quick Overview

You'll deploy your quiz-service and quiz-generation-service to Azure Container Apps, connect them to a PostgreSQL database, and test the full integration.

---

## Prerequisites

1. **Azure CLI installed**:
   ```bash
   # Check if installed
   az --version
   
   # If not installed:
   curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
   ```

2. **Get Azure details from your friend**:
   - Resource Group name
   - Azure Container Registry (ACR) name
   - Container Apps Environment name
   - Azure domain: `niceplant-c464d163.swedencentral.azurecontainerapps.io`

---

## Step 1: Login to Azure

```bash
# Login to Azure
az login

# Set your subscription (if you have multiple)
az account set --subscription "<subscription-id or name>"

# Verify you're logged in
az account show
```

---

## Step 2: Get Existing Resource Information

Ask your friend or check:

```bash
# List resource groups
az group list --output table

# List container registries
az acr list --output table

# List container app environments
az containerapp env list --output table
```

**Take note of**:
- Resource group: `rg-learning-assistant` (or similar)
- ACR name: `acrlearningassistant` (or similar)
- Environment: `aca-env-learning-assistant` (or similar)
- Location: `swedencentral`

---

## Step 3: Build Your JAR File Locally

Since Docker build failed, build the JAR locally first:

```bash
cd ~/Documents/cloud-native-ai-learning-assistant/backend/quiz-service

# Build the JAR
mvn clean package -DskipTests

# Verify JAR exists
ls -lh target/*.jar
```

You should see: `quiz-service-0.0.1-SNAPSHOT.jar`

---

## Step 4: Build Docker Images in Azure (No Local Docker Needed!)

Azure Container Registry can build images for you remotely:

### 4a. Login to ACR

```bash
# Replace with your friend's ACR name
ACR_NAME="acrlearningassistant"

# Login
az acr login --name $ACR_NAME
```

### 4b. Build quiz-service Image in Azure

```bash
cd ~/Documents/cloud-native-ai-learning-assistant

# Build remotely in ACR (this uploads files and builds in Azure)
az acr build \
  --registry $ACR_NAME \
  --image quiz-service:latest \
  --file backend/quiz-service/Dockerfile \
  backend/quiz-service/
```

### 4c. Build quiz-generation-service Image in Azure

```bash
az acr build \
  --registry $ACR_NAME \
  --image quiz-generation-service:latest \
  --file ai-services/quiz-generation-service/Dockerfile \
  ai-services/quiz-generation-service/
```

**This will take 5-10 minutes**. Azure is building your Docker images remotely!

---

## Step 5: Create PostgreSQL Database

### Check if PostgreSQL already exists:

```bash
# List PostgreSQL servers
az postgres flexible-server list --output table
```

### If it doesn't exist, create one:

```bash
RESOURCE_GROUP="rg-learning-assistant"  # Replace with actual name
LOCATION="swedencentral"

# Create PostgreSQL server
az postgres flexible-server create \
  --name quiz-postgres-prod \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --admin-user quizadmin \
  --admin-password 'YourSecurePassword123!' \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --version 15 \
  --public-access 0.0.0.0-255.255.255.255 \
  --yes

# Create database
az postgres flexible-server db create \
  --resource-group $RESOURCE_GROUP \
  --server-name quiz-postgres-prod \
  --database-name quizdb
```

### Get PostgreSQL connection details:

```bash
# Get server hostname
az postgres flexible-server show \
  --name quiz-postgres-prod \
  --resource-group $RESOURCE_GROUP \
  --query "fullyQualifiedDomainName" -o tsv
```

Save this - you'll need it! Example: `quiz-postgres-prod.postgres.database.azure.com`

---

## Step 6: Deploy quiz-generation-service (Python AI)

```bash
RESOURCE_GROUP="rg-learning-assistant"
ENV_NAME="aca-env-learning-assistant"  # Replace with actual name
ACR_NAME="acrlearningassistant"

# Deploy
az containerapp create \
  --name quiz-generation-service \
  --resource-group $RESOURCE_GROUP \
  --environment $ENV_NAME \
  --image ${ACR_NAME}.azurecr.io/quiz-generation-service:latest \
  --target-port 8086 \
  --ingress external \
  --registry-server ${ACR_NAME}.azurecr.io \
  --min-replicas 1 \
  --max-replicas 1 \
  --cpu 0.5 \
  --memory 1.0Gi \
  --env-vars \
    "AZURE_OPENAI_API_KEY=secretref:azure-openai-key" \
    "AZURE_OPENAI_ENDPOINT=secretref:azure-openai-endpoint" \
    "AZURE_OPENAI_DEPLOYMENT_GPT=secretref:azure-openai-deployment" \
    "PORT=8086"
```

**Wait, we need to add secrets first!**

### Add Azure OpenAI secrets:

```bash
# Get your Azure OpenAI details from .env file
cat ~/Documents/cloud-native-ai-learning-assistant/.env | grep AZURE_OPENAI

# Add secrets to Container App
az containerapp secret set \
  --name quiz-generation-service \
  --resource-group $RESOURCE_GROUP \
  --secrets \
    azure-openai-key="<your-api-key>" \
    azure-openai-endpoint="<your-endpoint>" \
    azure-openai-deployment="<your-deployment-name>"
```

### Get the quiz-generation-service URL:

```bash
az containerapp show \
  --name quiz-generation-service \
  --resource-group $RESOURCE_GROUP \
  --query "properties.configuration.ingress.fqdn" -o tsv
```

Save this URL! Example: `quiz-generation-service.niceplant-c464d163.swedencentral.azurecontainerapps.io`

---

## Step 7: Deploy quiz-service (Java Backend)

```bash
# Get PostgreSQL hostname from Step 5
DB_HOST="quiz-postgres-prod.postgres.database.azure.com"

# Get quiz-generation-service URL from Step 6
QUIZ_AI_URL="https://quiz-generation-service.niceplant-c464d163.swedencentral.azurecontainerapps.io"

# Deploy quiz-service
az containerapp create \
  --name quiz-service \
  --resource-group $RESOURCE_GROUP \
  --environment $ENV_NAME \
  --image ${ACR_NAME}.azurecr.io/quiz-service:latest \
  --target-port 8083 \
  --ingress external \
  --registry-server ${ACR_NAME}.azurecr.io \
  --min-replicas 1 \
  --max-replicas 2 \
  --cpu 0.5 \
  --memory 1.0Gi \
  --env-vars \
    "DB_HOST=${DB_HOST}" \
    "DB_PORT=5432" \
    "DB_NAME=quizdb" \
    "DB_USER=quizadmin" \
    "DB_PASSWORD=secretref:db-password" \
    "QUIZ_GENERATION_URL=${QUIZ_AI_URL}" \
    "SPRING_PROFILES_ACTIVE=prod"
```

### Add database password secret:

```bash
az containerapp secret set \
  --name quiz-service \
  --resource-group $RESOURCE_GROUP \
  --secrets db-password="YourSecurePassword123!"
```

### Get quiz-service URL:

```bash
az containerapp show \
  --name quiz-service \
  --resource-group $RESOURCE_GROUP \
  --query "properties.configuration.ingress.fqdn" -o tsv
```

Save this! Example: `quiz-service.niceplant-c464d163.swedencentral.azurecontainerapps.io`

---

## Step 8: Test Your Services

### Test 1: Check if services are running

```bash
# Test quiz-generation-service
curl https://quiz-generation-service.niceplant-c464d163.swedencentral.azurecontainerapps.io/docs

# Test quiz-service health
curl https://quiz-service.niceplant-c464d163.swedencentral.azurecontainerapps.io/api/quizzes/test
```

### Test 2: Generate a quiz via API

```bash
# First, get a token (you'll need user-service running)
# Or use a test token if you have one

QUIZ_URL="https://quiz-service.niceplant-c464d163.swedencentral.azurecontainerapps.io"

# Test quiz generation
curl -X POST ${QUIZ_URL}/api/quizzes \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Quiz from Azure",
    "documentText": "Python is a high-level programming language. It is known for readability and simplicity.",
    "userId": 1
  }'
```

### Test 3: View logs

```bash
# View quiz-service logs
az containerapp logs show \
  --name quiz-service \
  --resource-group $RESOURCE_GROUP \
  --follow

# View quiz-generation-service logs
az containerapp logs show \
  --name quiz-generation-service \
  --resource-group $RESOURCE_GROUP \
  --follow
```

---

## Step 9: Update Frontend to Use Azure URLs

The frontend already has the production URLs configured in `frontend/src/config/api.ts`:

```typescript
const QUIZ_SERVICE_URL = isProduction 
  ? `https://quiz-service.${PROD_BASE_DOMAIN}` 
  : 'http://localhost:8083';
```

Just make sure `PROD_BASE_DOMAIN` matches your actual Azure domain.

---

## Step 10: Deploy Frontend (Optional)

If you want to deploy the frontend too:

```bash
cd ~/Documents/cloud-native-ai-learning-assistant/frontend

# Build
npm run build

# Deploy to Azure Static Web Apps
az staticwebapp create \
  --name quiz-frontend \
  --resource-group $RESOURCE_GROUP \
  --source . \
  --location $LOCATION \
  --branch main \
  --app-location "/" \
  --output-location "dist" \
  --login-with-github
```

---

## Troubleshooting

### Check Container App Status

```bash
# Check if apps are running
az containerapp list \
  --resource-group $RESOURCE_GROUP \
  --output table

# Get detailed info
az containerapp show \
  --name quiz-service \
  --resource-group $RESOURCE_GROUP
```

### View Logs

```bash
# Stream logs in real-time
az containerapp logs show \
  --name quiz-service \
  --resource-group $RESOURCE_GROUP \
  --follow

# View recent logs
az containerapp logs show \
  --name quiz-service \
  --resource-group $RESOURCE_GROUP \
  --tail 100
```

### Common Issues

**Issue: "Image not found"**
```bash
# List images in ACR
az acr repository list --name $ACR_NAME

# Rebuild if needed
az acr build --registry $ACR_NAME --image quiz-service:latest backend/quiz-service/
```

**Issue: "Connection refused to PostgreSQL"**
```bash
# Check firewall rules
az postgres flexible-server firewall-rule list \
  --name quiz-postgres-prod \
  --resource-group $RESOURCE_GROUP

# Allow Azure services
az postgres flexible-server firewall-rule create \
  --name quiz-postgres-prod \
  --resource-group $RESOURCE_GROUP \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

**Issue: "CORS error"**
- Check that your CORS configuration includes the Azure domain
- It's already configured in AppConfig.java ✅

---

## Quick Reference Commands

```bash
# Restart a service
az containerapp update --name quiz-service --resource-group $RESOURCE_GROUP

# Scale up
az containerapp update \
  --name quiz-service \
  --resource-group $RESOURCE_GROUP \
  --min-replicas 2 \
  --max-replicas 5

# Update environment variable
az containerapp update \
  --name quiz-service \
  --resource-group $RESOURCE_GROUP \
  --set-env-vars "NEW_VAR=value"

# Delete a service (be careful!)
az containerapp delete --name quiz-service --resource-group $RESOURCE_GROUP
```

---

## Summary of What You'll Have

After following this guide:

✅ quiz-generation-service running in Azure Container Apps
✅ quiz-service running in Azure Container Apps  
✅ PostgreSQL database in Azure
✅ All services connected and talking to each other
✅ Accessible via HTTPS URLs
✅ Ready to integrate with your friend's existing services

---

## Next: Full Integration Testing

Once both services are deployed:

1. Open the frontend (wherever it's hosted)
2. Navigate to the quiz page
3. Upload a document or paste text
4. Generate a quiz
5. Take the quiz
6. Check the history
7. Verify everything persists in PostgreSQL

---

**Need help with any step? Just ask!**

