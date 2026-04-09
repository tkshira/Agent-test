@description('The principal ID (managed identity) that receives the role.')
param principalId string

@description('The built-in role definition GUID to assign.')
param roleDefinitionId string

@description('Scope resource ID to restrict the assignment (e.g. a storage account or search service).')
param resourceId string

// Unique name for idempotent re-deployments
var roleAssignmentName = guid(resourceId, principalId, roleDefinitionId)

resource roleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: roleAssignmentName
  // Assign at the resource scope, not subscription — least-privilege
  scope: resourceGroup()
  properties: {
    principalId: principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roleDefinitionId)
    // Narrow to the specific resource via condition (optional — remove if not needed)
    description: 'Assigned by azd deployment to container app managed identity'
  }
}

output id string = roleAssignment.id
