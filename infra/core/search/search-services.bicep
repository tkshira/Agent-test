param name string
param location string
param tags object = {}
param indexName string = 'products'

// ---------------------------------------------------------------------------
// Azure AI Search — Free tier
//
// Free tier limits:
//   - 1 service per subscription per region
//   - 3 indexes, 50 MB storage, 10,000 documents
//   - No SLA, no semantic search, no private endpoints
//
// To upgrade: change sku.name to 'basic' or 'standard' and set
//   semanticSearch: 'free' (basic) or 'standard' (standard tier)
// ---------------------------------------------------------------------------

resource search 'Microsoft.Search/searchServices@2023-11-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: 'free'
  }
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    replicaCount: 1
    partitionCount: 1
    hostingMode: 'default'
    publicNetworkAccess: 'enabled'
    // semanticSearch not available on free tier — uncomment when upgrading:
    // semanticSearch: 'free'
    authOptions: {
      // Allow both API key and RBAC (AAD) authentication
      aadOrApiKey: {
        aadAuthFailureMode: 'http403'
      }
    }
  }
}

output id string = search.id
output name string = search.name
output endpoint string = 'https://${search.name}.search.windows.net'
output principalId string = search.identity.principalId
