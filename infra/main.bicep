targetScope = 'subscription'

// ---------------------------------------------------------------------------
// Parameters
// ---------------------------------------------------------------------------

@minLength(1)
@maxLength(64)
@description('Name of the azd environment (used to derive resource names).')
param environmentName string

@minLength(1)
@description('Primary Azure region for all resources.')
param location string

@description('Azure AI Foundry project endpoint (set after creating the Foundry project).')
param azureAiProjectEndpoint string = ''

@description('Name of the Azure AI Search index for products.')
param azureSearchIndexName string = 'products'

// ---------------------------------------------------------------------------
// Variables
// ---------------------------------------------------------------------------

var tags = {
  'azd-env-name': environmentName
  'managed-by': 'azd'
}

var abbrs = loadJsonContent('./abbreviations.json')

// Short unique suffix derived from subscription + env + region
var resourceToken = toLower(uniqueString(subscription().id, environmentName, location))

// ---------------------------------------------------------------------------
// Resource Group
// ---------------------------------------------------------------------------

resource rg 'Microsoft.Resources/resourceGroups@2022-09-01' = {
  name: '${abbrs.resourcesResourceGroups}${environmentName}'
  location: location
  tags: tags
}

// ---------------------------------------------------------------------------
// Monitoring (Log Analytics → Application Insights)
// ---------------------------------------------------------------------------

module logAnalytics './core/monitor/loganalytics.bicep' = {
  name: 'loganalytics'
  scope: rg
  params: {
    name: '${abbrs.operationalInsightsWorkspaces}${resourceToken}'
    location: location
    tags: tags
  }
}

module appInsights './core/monitor/applicationinsights.bicep' = {
  name: 'appinsights'
  scope: rg
  params: {
    name: '${abbrs.insightsComponents}${resourceToken}'
    location: location
    tags: tags
    logAnalyticsWorkspaceId: logAnalytics.outputs.id
  }
}

// ---------------------------------------------------------------------------
// Container Registry
// ---------------------------------------------------------------------------

module containerRegistry './core/host/container-registry.bicep' = {
  name: 'registry'
  scope: rg
  params: {
    name: '${abbrs.containerRegistryRegistries}${resourceToken}'
    location: location
    tags: tags
  }
}

// ---------------------------------------------------------------------------
// Container Apps Environment
// ---------------------------------------------------------------------------

module containerAppsEnv './core/host/container-apps-environment.bicep' = {
  name: 'container-apps-env'
  scope: rg
  params: {
    name: '${abbrs.appManagedEnvironments}${resourceToken}'
    location: location
    tags: tags
    logAnalyticsWorkspaceId: logAnalytics.outputs.id
    logAnalyticsCustomerId: logAnalytics.outputs.customerId
    logAnalyticsPrimaryKey: logAnalytics.outputs.primaryKey
  }
}

// ---------------------------------------------------------------------------
// Azure AI Search — Free tier
// Note: Free tier does not support semantic search, private endpoints,
//       or multiple replicas. Upgrade to Basic/Standard for production.
// ---------------------------------------------------------------------------

module search './core/search/search-services.bicep' = {
  name: 'search'
  scope: rg
  params: {
    name: '${abbrs.searchSearchServices}${resourceToken}'
    location: location
    tags: tags
    indexName: azureSearchIndexName
  }
}

// ---------------------------------------------------------------------------
// Azure Storage — Hot tier (for purchase logs and future file storage)
// ---------------------------------------------------------------------------

module storage './core/storage/storage-account.bicep' = {
  name: 'storage'
  scope: rg
  params: {
    // Storage account names: max 24 chars, alphanumeric only, no hyphens
    name: take('${abbrs.storageStorageAccounts}${resourceToken}', 24)
    location: location
    tags: tags
  }
}

// ---------------------------------------------------------------------------
// Backend Container App (Spring Boot — port 8080)
// ---------------------------------------------------------------------------

module backend './core/host/container-app.bicep' = {
  name: 'backend'
  scope: rg
  params: {
    name: '${abbrs.appContainerApps}backend-${resourceToken}'
    location: location
    tags: union(tags, { 'azd-service-name': 'backend' })
    containerAppsEnvironmentId: containerAppsEnv.outputs.id
    containerRegistryName: containerRegistry.outputs.name
    targetPort: 8080
    externalIngress: true
    env: [
      { name: 'AZURE_AI_PROJECT_ENDPOINT',             value: azureAiProjectEndpoint }
      { name: 'AZURE_SEARCH_ENDPOINT',                 value: search.outputs.endpoint }
      { name: 'AZURE_SEARCH_INDEX_NAME',               value: azureSearchIndexName }
      { name: 'AZURE_SEARCH_CONNECTION_ID',            value: '' } // set after Foundry connection is created
      { name: 'AZURE_AGENT_MODEL',                     value: 'gpt-4o' }
      { name: 'APPLICATIONINSIGHTS_CONNECTION_STRING', value: appInsights.outputs.connectionString }
      { name: 'AZURE_STORAGE_ACCOUNT_NAME',            value: storage.outputs.name }
      { name: 'MCP_LOG_PATH',                          value: '/tmp/logs/purchases.log' }
    ]
  }
}

// ---------------------------------------------------------------------------
// Frontend Container App (React / nginx — port 80)
// ---------------------------------------------------------------------------

module frontend './core/host/container-app.bicep' = {
  name: 'frontend'
  scope: rg
  params: {
    name: '${abbrs.appContainerApps}frontend-${resourceToken}'
    location: location
    tags: union(tags, { 'azd-service-name': 'frontend' })
    containerAppsEnvironmentId: containerAppsEnv.outputs.id
    containerRegistryName: containerRegistry.outputs.name
    targetPort: 80
    externalIngress: true
    env: [
      { name: 'REACT_APP_API_URL', value: 'https://${backend.outputs.fqdn}' }
    ]
  }
}

// ---------------------------------------------------------------------------
// RBAC: grant backend managed identity access to Search and Storage
// ---------------------------------------------------------------------------

module searchRoleAssignment './core/security/role-assignment.bicep' = {
  name: 'search-role'
  scope: rg
  params: {
    principalId: backend.outputs.principalId
    // Search Index Data Contributor
    roleDefinitionId: '8ebe5a00-799e-43f5-93ac-243d3dce84a7'
    resourceId: search.outputs.id
  }
}

module storageRoleAssignment './core/security/role-assignment.bicep' = {
  name: 'storage-role'
  scope: rg
  params: {
    principalId: backend.outputs.principalId
    // Storage Blob Data Contributor
    roleDefinitionId: 'ba92f5b4-2d11-453d-a403-e96b0029c9fe'
    resourceId: storage.outputs.id
  }
}

// ---------------------------------------------------------------------------
// Outputs — azd reads these to wire up env vars and service URLs
// ---------------------------------------------------------------------------

output AZURE_LOCATION string = location
output AZURE_TENANT_ID string = tenant().tenantId
output AZURE_RESOURCE_GROUP string = rg.name

output AZURE_CONTAINER_REGISTRY_ENDPOINT string = containerRegistry.outputs.loginServer
output AZURE_CONTAINER_REGISTRY_NAME string = containerRegistry.outputs.name

output AZURE_SEARCH_ENDPOINT string = search.outputs.endpoint
output AZURE_SEARCH_INDEX_NAME string = azureSearchIndexName

output AZURE_STORAGE_ACCOUNT_NAME string = storage.outputs.name

output APPLICATIONINSIGHTS_CONNECTION_STRING string = appInsights.outputs.connectionString

output SERVICE_BACKEND_URI string = 'https://${backend.outputs.fqdn}'
output SERVICE_FRONTEND_URI string = 'https://${frontend.outputs.fqdn}'
