param name string
param location string
param tags object = {}
param containerAppsEnvironmentId string
param containerRegistryName string
param targetPort int

@description('Expose the app via an external (public) ingress URL.')
param externalIngress bool = true

@description('Environment variables to inject into the container.')
param env array = []

@description('Minimum number of replicas. 0 = scale to zero (cost saving).')
param minReplicas int = 0

@description('Maximum number of replicas.')
param maxReplicas int = 3

// ---------------------------------------------------------------------------
// Container Registry reference (used to wire pull credentials)
// ---------------------------------------------------------------------------

resource registry 'Microsoft.ContainerRegistry/registries@2023-07-01' existing = {
  name: containerRegistryName
}

// ---------------------------------------------------------------------------
// Container App
// Uses a placeholder image on first deploy; azd replaces it during `azd deploy`
// ---------------------------------------------------------------------------

resource containerApp 'Microsoft.App/containerApps@2023-05-01' = {
  name: name
  location: location
  tags: tags
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    managedEnvironmentId: containerAppsEnvironmentId
    configuration: {
      ingress: {
        external: externalIngress
        targetPort: targetPort
        transport: 'auto'
        allowInsecure: false
      }
      registries: [
        {
          server: registry.properties.loginServer
          username: registry.listCredentials().username
          passwordSecretRef: 'registry-password'
        }
      ]
      secrets: [
        {
          name: 'registry-password'
          value: registry.listCredentials().passwords[0].value
        }
      ]
    }
    template: {
      containers: [
        {
          // Placeholder image — azd replaces this during `azd deploy`
          image: 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest'
          name: 'app'
          env: env
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
        }
      ]
      scale: {
        minReplicas: minReplicas
        maxReplicas: maxReplicas
        rules: [
          {
            name: 'http-scaling'
            http: {
              metadata: {
                concurrentRequests: '20'
              }
            }
          }
        ]
      }
    }
  }
}

output id string = containerApp.id
output name string = containerApp.name
output fqdn string = containerApp.properties.configuration.ingress.fqdn
output principalId string = containerApp.identity.principalId
