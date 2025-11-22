# Azure Redeployment Script for Learning Assistant
# This script redeploys the infrastructure from template.json and configures the applications.

$ErrorActionPreference = "Stop"

# --- 0. Prerequisites Check ---
Write-Host "Checking for Azure PowerShell module..." -ForegroundColor Cyan
if (-not (Get-Module -ListAvailable -Name Az)) {
    Write-Host "Az module not found. Installing (this may take a few minutes)..." -ForegroundColor Yellow
    Install-Module -Name Az -Scope CurrentUser -Repository PSGallery -Force -AllowClobber
}

# --- 1. Authentication Check ---
Write-Host "Checking Azure connection..." -ForegroundColor Cyan
try {
    $context = Get-AzContext
    if ($null -eq $context) {
        Write-Host "Please login to Azure..." -ForegroundColor Yellow
        Connect-AzAccount
    }
}
catch {
    Write-Host "Please login to Azure..." -ForegroundColor Yellow
    Connect-AzAccount
}

# --- 2. Configuration ---
$resourceGroupName = Read-Host "Enter new Resource Group Name (e.g., rg-learning-assistant)"
$location = Read-Host "Enter Location (e.g., swedencentral)"
if ([string]::IsNullOrWhiteSpace($location)) { $location = "swedencentral" }

# Generate unique suffix for globally unique resources
$uniqueSuffix = -join ((97..122) | Get-Random -Count 5 | ForEach-Object {[char]$_})
$uniqueSuffix = $uniqueSuffix + (Get-Random -Minimum 1000 -Maximum 9999)

# Resource Names (must match parameters in template.json or be passed as overrides)
$acrName = "acraila$uniqueSuffix"
$keyVaultName = "kv-aila-$uniqueSuffix"
$storageAccountName = "llmopsblob$uniqueSuffix"
$searchServiceName = "llmops-search-$uniqueSuffix"
$openAiServiceName = "openai-aila-$uniqueSuffix"
$projectFoundryName = "project-foundry-$uniqueSuffix"
$cosmosDbName = "acraila-$uniqueSuffix"
$postgresServerName = "acraila-pg-$uniqueSuffix"
$logWorkspaceName = "aca-logs-aila-$uniqueSuffix"
$managedEnvName = "aca-env-aila-$uniqueSuffix"

Write-Host "Generated unique resource names:" -ForegroundColor Gray
Write-Host "  ACR: $acrName"
Write-Host "  KeyVault: $keyVaultName"
Write-Host "  Storage: $storageAccountName"

# --- 3. Resource Group Creation ---
Write-Host "Creating Resource Group '$resourceGroupName' in '$location'..." -ForegroundColor Cyan
New-AzResourceGroup -Name $resourceGroupName -Location $location -Force

# --- 4. Deploy Infrastructure ---
Write-Host "Deploying ARM Template..." -ForegroundColor Cyan
$deployment = New-AzResourceGroupDeployment -ResourceGroupName $resourceGroupName `
    -TemplateFile ".\template.json" `
    -registries_acraila03478_name $acrName `
    -vaults_kv_aila_07980_name $keyVaultName `
    -storageAccounts_llmopsblobstorage07208_name $storageAccountName `
    -searchServices_llmops_search_00762_name $searchServiceName `
    -accounts_my_quiz_openai_service_name $openAiServiceName `
    -accounts_project_foundry_name $projectFoundryName `
    -databaseAccounts_acraila_03616_name $cosmosDbName `
    -flexibleServers_acraila_01082_name $postgresServerName `
    -workspaces_aca_logs_aila_name $logWorkspaceName `
    -managedEnvironments_aca_env_aila_name $managedEnvName `
    -Verbose

if ($deployment.ProvisioningState -ne "Succeeded") {
    Write-Error "Deployment failed!"
}
Write-Host "Infrastructure deployed successfully." -ForegroundColor Green

# --- 5. Image Build & Push ---
Write-Host "Logging into ACR..." -ForegroundColor Cyan
Connect-AzContainerRegistry -Name $acrName

$services = @(
    @{ Name = "frontend"; Path = "frontend"; Image = "frontend" },
    @{ Name = "chat-service"; Path = "backend\chat-service"; Image = "chat-service" },
    @{ Name = "user-service"; Path = "backend\user-service"; Image = "user-service" },
    @{ Name = "document-service"; Path = "backend\document-service"; Image = "document-service" },
    @{ Name = "quiz-service"; Path = "backend\quiz-service"; Image = "quiz-service" },
    @{ Name = "quiz-generation-service"; Path = "ai-services\quiz-generation-service"; Image = "quiz-generation-service" },
    @{ Name = "rag-ingest-service"; Path = "ai-services\rag-ingest-service"; Image = "rag-ingest-service" },
    @{ Name = "rag-query-service"; Path = "ai-services\rag-query-service"; Image = "rag-query-service" }
)

foreach ($svc in $services) {
    $imageTag = "$($acrName).azurecr.io/$($svc.Image):latest"
    Write-Host "Building $($svc.Name)..." -ForegroundColor Cyan
    docker build -t $imageTag $svc.Path
    Write-Host "Pushing $($svc.Name)..." -ForegroundColor Cyan
    docker push $imageTag
}

# --- 6. Configure Container Apps ---
Write-Host "Configuring Container Apps..." -ForegroundColor Cyan

# Get Secrets / Connection Strings
$cosmosKeys = Get-AzCosmosDBAccountKey -ResourceGroupName $resourceGroupName -Name $cosmosDbName
$cosmosKey = $cosmosKeys.PrimaryMasterKey
$cosmosUri = "mongodb://$($cosmosDbName):$($cosmosKey)@$($cosmosDbName).mongo.cosmos.azure.com:10255/?ssl=true&replicaSet=globaldb&retrywrites=false&maxIdleTimeMS=120000&appName=@$($cosmosDbName)@"

# Get ACR Credentials for the apps
$acrCreds = Get-AzContainerRegistryCredential -ResourceGroupName $resourceGroupName -Name $acrName
$acrPassword = $acrCreds.Passwords[0].Value

# Function to update a container app
function Update-ContainerApp {
    param (
        [string]$AppName,
        [string]$Image,
        [hashtable]$EnvVars
    )
    Write-Host "Updating $AppName..." -ForegroundColor Gray
    
    $containerApp = Get-AzContainerApp -ResourceGroupName $resourceGroupName -Name $AppName
    
    # Update Registry Credentials
    $containerApp.Configuration.Registries = @(
        @{
            Server = "$acrName.azurecr.io"
            Username = $acrName
            PasswordSecretRef = "acr-password"
        }
    )
    
    # Add/Update Secret for ACR Password
    $secretFound = $false
    if ($null -eq $containerApp.Configuration.Secrets) { $containerApp.Configuration.Secrets = @() }
    foreach ($secret in $containerApp.Configuration.Secrets) {
        if ($secret.Name -eq "acr-password") {
            $secret.Value = $acrPassword
            $secretFound = $true
        }
    }
    if (-not $secretFound) {
        $containerApp.Configuration.Secrets += @{ Name = "acr-password"; Value = $acrPassword }
    }

    # Update Container Image and Env Vars
    $container = $containerApp.Template.Containers[0]
    $container.Image = $Image
    
    # Convert Hashtable to Env Var Array
    $envList = @()
    foreach ($key in $EnvVars.Keys) {
        $envList += @{ Name = $key; Value = $EnvVars[$key] }
    }
    $container.Env = $envList

    # Update the app
    Update-AzContainerApp -ResourceGroupName $resourceGroupName -Name $AppName -ContainerApp $containerApp
}

# --- Update Individual Apps ---

# 1. Frontend
# Need to get Backend URL first. Assuming chat-service is the gateway/backend.
$chatApp = Get-AzContainerApp -ResourceGroupName $resourceGroupName -Name "chat-service"
$backendUrl = "https://$($chatApp.Configuration.Ingress.Fqdn)"

Update-ContainerApp -AppName "frontend" `
    -Image "$acrName.azurecr.io/frontend:latest" `
    -EnvVars @{ "BACKEND_URL" = $backendUrl }

# 2. Chat Service
Update-ContainerApp -AppName "chat-service" `
    -Image "$acrName.azurecr.io/chat-service:latest" `
    -EnvVars @{ 
        "COSMOS_URI" = $cosmosUri;
        "COSMOS_DB_NAME" = "chat_db";
        "DOCUMENT_SERVICE_URL" = "http://document-service"; # Internal Dapr/Service Discovery if enabled, else FQDN
        "RAG_QUERY_URL" = "http://rag-query-service"
    }

# Note: For internal communication in ACA, we might need the FQDNs if Dapr isn't explicitly set up in the template.
# The template shows "daprConfiguration": {}, so Dapr might not be fully configured or sidecars enabled.
# Let's fetch FQDNs for dependencies to be safe.

$docApp = Get-AzContainerApp -ResourceGroupName $resourceGroupName -Name "document-service"
$docUrl = "https://$($docApp.Configuration.Ingress.Fqdn)"

$ragQueryApp = Get-AzContainerApp -ResourceGroupName $resourceGroupName -Name "rag-query-service"
$ragQueryUrl = "https://$($ragQueryApp.Configuration.Ingress.Fqdn)"

$userApp = Get-AzContainerApp -ResourceGroupName $resourceGroupName -Name "user-service"
$userUrl = "https://$($userApp.Configuration.Ingress.Fqdn)"

# Re-update Chat Service with real URLs
Update-ContainerApp -AppName "chat-service" `
    -Image "$acrName.azurecr.io/chat-service:latest" `
    -EnvVars @{ 
        "COSMOS_URI" = $cosmosUri;
        "COSMOS_DB_NAME" = "chat_db";
        "DOCUMENT_SERVICE_URL" = $docUrl;
        "RAG_QUERY_URL" = $ragQueryUrl
    }

# 3. User Service
Update-ContainerApp -AppName "user-service" `
    -Image "$acrName.azurecr.io/user-service:latest" `
    -EnvVars @{ 
        "COSMOS_URI" = $cosmosUri;
        "COSMOS_DB_NAME" = "user_db"
    }

# 4. Document Service
Update-ContainerApp -AppName "document-service" `
    -Image "$acrName.azurecr.io/document-service:latest" `
    -EnvVars @{ 
        "COSMOS_URI" = $cosmosUri;
        "COSMOS_DB_NAME" = "document_db"
    }

# 5. Quiz Service
Update-ContainerApp -AppName "quiz-service" `
    -Image "$acrName.azurecr.io/quiz-service:latest" `
    -EnvVars @{ 
        "COSMOS_URI" = $cosmosUri;
        "COSMOS_DB_NAME" = "quiz_db"
    }

# ... Add other services as needed ...

Write-Host "Redeployment Complete!" -ForegroundColor Green
Write-Host "Frontend URL: https://$($frontendApp.Configuration.Ingress.Fqdn)" -ForegroundColor Green
