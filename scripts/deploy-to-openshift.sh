#!/bin/bash

# Deploy Connect Service to OpenShift using Helm charts
# This script handles the complete deployment process including Vault integration

set -e

# Configuration
NAMESPACE=${NAMESPACE:-connect-service}
RELEASE_NAME=${RELEASE_NAME:-connect-service}
CHART_PATH="./helm/connect-service"
VALUES_FILE=${VALUES_FILE:-"./helm/connect-service/values.yaml"}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if oc is installed
    if ! command -v oc &> /dev/null; then
        log_error "OpenShift CLI (oc) is not installed or not in PATH"
        exit 1
    fi
    
    # Check if helm is installed
    if ! command -v helm &> /dev/null; then
        log_error "Helm is not installed or not in PATH"
        exit 1
    fi
    
    # Check if logged in to OpenShift
    if ! oc whoami &> /dev/null; then
        log_error "Not logged in to OpenShift. Please run 'oc login' first"
        exit 1
    fi
    
    log_info "Prerequisites check passed"
}

# Create namespace if it doesn't exist
create_namespace() {
    log_info "Creating namespace: $NAMESPACE"
    
    if oc get namespace "$NAMESPACE" &> /dev/null; then
        log_info "Namespace $NAMESPACE already exists"
    else
        oc create namespace "$NAMESPACE"
        log_info "Namespace $NAMESPACE created"
    fi
}

# Add required labels and annotations
label_namespace() {
    log_info "Adding labels and annotations to namespace"
    
    oc label namespace "$NAMESPACE" \
        app.kubernetes.io/name=connect-service \
        app.kubernetes.io/part-of=adyanta-platform \
        --overwrite
    
    oc annotate namespace "$NAMESPACE" \
        openshift.io/description="Connect Service for XML/JSON processing" \
        --overwrite
}

# Create Vault service account and role binding
setup_vault_auth() {
    log_info "Setting up Vault authentication"
    
    # Create service account for Vault
    cat <<EOF | oc apply -f -
apiVersion: v1
kind: ServiceAccount
metadata:
  name: vault-auth
  namespace: $NAMESPACE
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: vault-auth
roleRef:
  apiGroups: [""]
  kind: ClusterRole
  name: system:auth-delegator
subjects:
- kind: ServiceAccount
  name: vault-auth
  namespace: $NAMESPACE
EOF
    
    log_info "Vault authentication setup completed"
}

# Deploy MongoDB if enabled
deploy_mongodb() {
    if [[ "${MONGODB_ENABLED:-true}" == "true" ]]; then
        log_info "Deploying MongoDB..."
        
        helm upgrade --install mongodb bitnami/mongodb \
            --namespace "$NAMESPACE" \
            --set auth.enabled=true \
            --set auth.rootPassword="${MONGODB_ROOT_PASSWORD:-$(openssl rand -base64 32)}" \
            --set auth.username="${MONGODB_USERNAME:-connect-service}" \
            --set auth.password="${MONGODB_PASSWORD:-$(openssl rand -base64 32)}" \
            --set auth.database="${MONGODB_DATABASE:-connect-service}" \
            --set persistence.enabled=true \
            --set persistence.size="${MONGODB_PERSISTENCE_SIZE:-10Gi}" \
            --set resources.limits.cpu=500m \
            --set resources.limits.memory=512Mi \
            --set resources.requests.cpu=250m \
            --set resources.requests.memory=256Mi \
            --wait
        
        log_info "MongoDB deployed successfully"
    else
        log_info "MongoDB deployment skipped"
    fi
}

# Deploy Vault if enabled
deploy_vault() {
    if [[ "${VAULT_ENABLED:-false}" == "true" ]]; then
        log_info "Deploying HashiCorp Vault..."
        
        helm repo add hashicorp https://helm.releases.hashicorp.com
        helm repo update
        
        helm upgrade --install vault hashicorp/vault \
            --namespace "$NAMESPACE" \
            --set server.dev.enabled=true \
            --set server.dev.devRootToken="${VAULT_DEV_TOKEN:-root}" \
            --set server.ha.enabled=false \
            --set server.standalone.enabled=true \
            --wait
        
        log_info "Vault deployed successfully"
        
        # Wait for Vault to be ready
        log_info "Waiting for Vault to be ready..."
        oc wait --for=condition=ready pod -l app.kubernetes.io/name=vault -n "$NAMESPACE" --timeout=300s
        
        # Initialize Vault with secrets
        initialize_vault_secrets
    else
        log_info "Vault deployment skipped"
    fi
}

# Initialize Vault with secrets
initialize_vault_secrets() {
    log_info "Initializing Vault with secrets..."
    
    # Get Vault pod name
    VAULT_POD=$(oc get pods -n "$NAMESPACE" -l app.kubernetes.io/name=vault -o jsonpath='{.items[0].metadata.name}')
    
    # Enable KV secrets engine
    oc exec -n "$NAMESPACE" "$VAULT_POD" -- vault secrets enable -path=secret kv-v2
    
    # Store MongoDB password
    MONGODB_PASSWORD=$(oc get secret mongodb -n "$NAMESPACE" -o jsonpath='{.data.mongodb-password}' | base64 -d)
    oc exec -n "$NAMESPACE" "$VAULT_POD" -- vault kv put secret/connect-service/mongodb mongodb_password="$MONGODB_PASSWORD"
    
    # Store OAuth2 credentials
    oc exec -n "$NAMESPACE" "$VAULT_POD" -- vault kv put secret/connect-service/oauth2 \
        apigee_client_id="${APIGEE_CLIENT_ID:-default-client-id}" \
        apigee_client_secret="${APIGEE_CLIENT_SECRET:-default-secret}" \
        fenergo_client_id="${FENERGO_CLIENT_ID:-default-fenergo-client}" \
        fenergo_client_secret="${FENERGO_CLIENT_SECRET:-default-fenergo-secret}"
    
    # Enable PKI secrets engine for certificates
    oc exec -n "$NAMESPACE" "$VAULT_POD" -- vault secrets enable -path=pki pki
    oc exec -n "$NAMESPACE" "$VAULT_POD" -- vault secrets tune -max-lease-ttl=87600h pki
    
    # Generate root CA
    oc exec -n "$NAMESPACE" "$VAULT_POD" -- vault write -field=certificate pki/root/generate/internal \
        common_name="Connect Service CA" \
        ttl=87600h > /tmp/connect-service-ca.crt
    
    # Configure PKI URLs
    oc exec -n "$NAMESPACE" "$VAULT_POD" -- vault write pki/config/urls \
        issuing_certificates="http://vault:8200/v1/pki/ca" \
        crl_distribution_points="http://vault:8200/v1/pki/crl"
    
    # Create role for certificate issuance
    oc exec -n "$NAMESPACE" "$VAULT_POD" -- vault write pki/roles/connect-service \
        allowed_domains="connect-service,connect-service.$NAMESPACE,connect-service.$NAMESPACE.svc.cluster.local" \
        allow_subdomains=true \
        max_ttl="720h"
    
    log_info "Vault secrets initialized successfully"
}

# Deploy Connect Service
deploy_connect_service() {
    log_info "Deploying Connect Service..."
    
    # Update dependencies
    helm dependency update "$CHART_PATH"
    
    # Deploy the application
    helm upgrade --install "$RELEASE_NAME" "$CHART_PATH" \
        --namespace "$NAMESPACE" \
        --values "$VALUES_FILE" \
        --set image.repository="${IMAGE_REPOSITORY:-connect-service}" \
        --set image.tag="${IMAGE_TAG:-latest}" \
        --set vault.enabled="${VAULT_ENABLED:-false}" \
        --set vault.address="${VAULT_ADDRESS:-http://vault:8200}" \
        --set mongodb.enabled="${MONGODB_ENABLED:-true}" \
        --set externalApis.xmlToJson.baseUrl="${XML_TO_JSON_API_URL:-https://api.example.com/xml-to-json}" \
        --set externalApis.fenergo.baseUrl="${FENERGO_API_URL:-https://api.fenergo.com/v1}" \
        --set oauth2.apigee.clientId="${APIGEE_CLIENT_ID:-default-client-id}" \
        --set oauth2.apigee.clientSecret="${APIGEE_CLIENT_SECRET:-default-secret}" \
        --set oauth2.apigee.tokenUri="${APIGEE_TOKEN_URI:-https://api.apigee.com/oauth/token}" \
        --set oauth2.fenergo.clientId="${FENERGO_CLIENT_ID:-default-fenergo-client}" \
        --set oauth2.fenergo.clientSecret="${FENERGO_CLIENT_SECRET:-default-fenergo-secret}" \
        --set oauth2.fenergo.tokenUri="${FENERGO_TOKEN_URI:-https://api.fenergo.com/oauth/token}" \
        --wait
    
    log_info "Connect Service deployed successfully"
}

# Verify deployment
verify_deployment() {
    log_info "Verifying deployment..."
    
    # Check if pods are running
    oc get pods -n "$NAMESPACE" -l app.kubernetes.io/name=connect-service
    
    # Wait for deployment to be ready
    oc wait --for=condition=available deployment/connect-service -n "$NAMESPACE" --timeout=300s
    
    # Check service
    oc get svc -n "$NAMESPACE" connect-service
    
    # Check ingress
    oc get ingress -n "$NAMESPACE" connect-service
    
    log_info "Deployment verification completed"
}

# Display access information
display_access_info() {
    log_info "Deployment completed successfully!"
    echo
    echo "Access Information:"
    echo "=================="
    echo "Namespace: $NAMESPACE"
    echo "Release: $RELEASE_NAME"
    echo
    
    # Get service URL
    SERVICE_URL=$(oc get route connect-service -n "$NAMESPACE" -o jsonpath='{.spec.host}' 2>/dev/null || echo "Not available")
    if [[ "$SERVICE_URL" != "Not available" ]]; then
        echo "Service URL: https://$SERVICE_URL"
        echo "API Documentation: https://$SERVICE_URL/swagger-ui.html"
        echo "Health Check: https://$SERVICE_URL/api/v1/connect/health"
    else
        echo "Service URL: Not available (check ingress configuration)"
    fi
    
    echo
    echo "Useful Commands:"
    echo "==============="
    echo "View logs: oc logs -f deployment/connect-service -n $NAMESPACE"
    echo "Port forward: oc port-forward svc/connect-service 8080:8080 -n $NAMESPACE"
    echo "Get pods: oc get pods -n $NAMESPACE"
    echo "Get services: oc get svc -n $NAMESPACE"
    echo "Get ingress: oc get ingress -n $NAMESPACE"
}

# Main execution
main() {
    log_info "Starting Connect Service deployment to OpenShift"
    echo
    
    check_prerequisites
    create_namespace
    label_namespace
    setup_vault_auth
    deploy_mongodb
    deploy_vault
    deploy_connect_service
    verify_deployment
    display_access_info
    
    log_info "Deployment completed successfully!"
}

# Run main function
main "$@"
