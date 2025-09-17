# Advanced MongoDB SSL/TLS Configuration

This document explains how the Connect Service connects to MongoDB with TLS/SSL using secrets from HashiCorp Vault and values from the Helm chart.

## Why this configuration?

Production clusters often require:
- Encrypted connections to MongoDB (TLS/SSL)
- Credentials injected at runtime (e.g., Vault) rather than baked in images
- Separate keystore/truststore to present a client certificate and trust the server CA

To support this, the service includes:
- `AdvancedMongoProperties`: typed config bound to `mongodb.*` properties
- `AdvancedMongoConfig`: builds a Reactive MongoClient with SSLContext from keystore/truststore
- Helm env vars to pass keystore/truststore paths and passwords (populated by Vault)
- `application-openshift.yml` to map env vars to config properties

## Configuration Properties (`application-openshift.yml`)

```yaml
mongodb:
  uri: ${MONGODB_URI:mongodb://connect-service@mongodb:27017/connect-service?authSource=admin}
  database: ${MONGODB_DATABASE:connect-service}
  password: ${MONGODB_PASSWORD:}
  ssl:
    enabled: ${MONGODB_SSL_ENABLED:false}
    keyStorePath: ${MONGODB_KEYSTORE_PATH:}
    keyStorePassword: ${MONGODB_KEYSTORE_PASSWORD:}
    keyStoreType: ${MONGODB_KEYSTORE_TYPE:PKCS12}
    trustStorePath: ${MONGODB_TRUSTSTORE_PATH:}
    trustStorePassword: ${MONGODB_TRUSTSTORE_PASSWORD:}
    trustStoreType: ${MONGODB_TRUSTSTORE_TYPE:PKCS12}
    disableHostNameVerification: ${MONGODB_SSL_DISABLE_HOSTNAME_VERIFICATION:false}
```

Notes:
- The `uri` contains the username only; the password comes from `mongodb.password` and is merged into the URI by `AdvancedMongoConfig` at runtime.
- Set `ssl.enabled=true` to activate TLS and supply keystore/truststore details.

## Helm Values and Env Vars

In `helm/connect-service/templates/deployment.yaml`, the following env vars are set from values and Vault secrets:

```yaml
- name: MONGODB_URI
  value: "mongodb://<user>@<host>:27017/<db>?authSource=admin"
- name: MONGODB_DATABASE
  value: "<db>"
- name: MONGODB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: <release>-secrets
      key: mongodb-password
- name: MONGODB_SSL_ENABLED
  value: "{{ .Values.mongodb.ssl.enabled }}"
- name: MONGODB_KEYSTORE_PATH
  value: "/vault/secrets/mongo-client-keystore.p12"
- name: MONGODB_KEYSTORE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: <release>-secrets
      key: mongodb-keystore-password
- name: MONGODB_TRUSTSTORE_PATH
  value: "/vault/secrets/mongo-truststore.p12"
- name: MONGODB_TRUSTSTORE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: <release>-secrets
      key: mongodb-truststore-password
```

Vault is expected to write the keystore/truststore files and passwords into the mounted `/vault/secrets` directory via the `vault-init` init container.

## Code Overview

### `AdvancedMongoProperties`
- Binds to `mongodb.*` properties
- Holds URI, database, password
- Nested `ssl` properties for keystore/truststore configuration

### `AdvancedMongoConfig`
- Activated when `mongodb.ssl.enabled=true`
- Builds `SSLContext` from keystore/truststore files and config
- Creates a `MongoClientSettings` with `.applyToSslSettings()` and optional hostname verification disable (dev only)
- Injects password into URI if missing, so credentials remain separated

## How to Enable SSL

1) Ensure Vault provides:
- `/vault/secrets/mongo-client-keystore.p12` (client cert+key; optional if server is mutual TLS)
- `/vault/secrets/mongo-truststore.p12` (CA chain to trust the Mongo server cert)
- Passwords for both stores in the Secrets created by the chart (`mongodb-keystore-password`, `mongodb-truststore-password`)

2) Set Helm values:

```bash
--set mongodb.ssl.enabled=true \
--set mongodb.ssl.keyStoreType=PKCS12 \
--set mongodb.ssl.trustStoreType=PKCS12
```

3) Provide the Mongo user and DB via values or env (username is in `MONGODB_URI`, password in `MONGODB_PASSWORD`).

## Verifying the Connection

- Check logs for: "Configuring Reactive MongoClient with SSL"
- If connection fails, verify:
  - Keystore/truststore paths exist inside the pod
  - Passwords are correct
  - Server certificate chain matches the truststore
  - Hostname verification setting aligns with server certificate CN/SANs

## Security Recommendations

- Do not disable hostname verification in production
- Keep keystore/truststore in-memory volumes only (no persistent storage)
- Use short-lived certs and rotate regularly
- Limit Vault policy to only needed paths



