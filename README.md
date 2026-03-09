# 🔐 Auth Service

A production-ready Authentication & Authorization Service built with:

-   Spring Boot
-   Maven
-   HTTPS (TLS)
-   JWT (JSON Web Tokens)
-   OAuth2
-   Docker
-   Kubernetes

------------------------------------------------------------------------

# 🚀 Features

## Core Features

-   User registration & login
-   JWT access & refresh tokens
-   OAuth2 (Authorization Code + Client Credentials)
-   Role-based access control (RBAC)
-   Password hashing (BCrypt)
-   HTTPS enforcement
-   Session Management – Invalidate tokens on logout or password change.
-   Token validation endpoint
-   Token revocation / blacklist
-   Secure cookie support (optional)

## Security Features

-   Spring Security integration
-   CSRF protection (configurable)
-   CORS configuration
-   Rate limiting
-   Account lockout after failed attempts
-   Email verification (optional)
-   Password reset flow
-   Multi-Factor Authentication (MFA) – Optional TOTP-based 2FA.
-   Account Lockout – Temporary lock after multiple failed login attempts.
-   Audit logging
-   Refresh Token Rotation – Enhanced security by rotating refresh tokens on use.

## Observability

-   Spring Boot Actuator
-   Health checks
-   Prometheus metrics
-   Structured logging (JSON)

------------------------------------------------------------------------

# 🏗 Architecture

Client → HTTPS → Auth Service → Database\
                                  ↓\
                                  JWT Issued

-   Stateless JWT authentication
-   Access tokens (short-lived)
-   Refresh tokens (secure storage)

------------------------------------------------------------------------

# 🧱 Tech Stack

  Component       Technology
  --------------- -----------------------------
  Framework       Spring Boot
  Build Tool      Maven
  Security        Spring Security
  Token           JWT (RSA256)
  OAuth           Spring Authorization Server
  Database        PostgreSQL
  Container       Docker
  Orchestration   Kubernetes

------------------------------------------------------------------------

# 📦 Docker

## Build

mvn clean package\
docker build -t auth-service:latest .

## Run

docker run -p 8443:8443 auth-service

------------------------------------------------------------------------

# ☸ Kubernetes

## Deployment (example)

-   3 replicas
-   Liveness & readiness probes
-   Config via ConfigMap
-   Secrets via Kubernetes Secrets

## Service

-   Type: ClusterIP

## Ingress

-   TLS enabled
-   Domain-based routing

------------------------------------------------------------------------

# 🔐 JWT Strategy

-   Access Token: 15 minutes
-   Refresh Token: 7 days
-   Signed using RSA256
-   JWKS endpoint available

------------------------------------------------------------------------

# 📊 Health Endpoints

-   /actuator/health
-   /actuator/metrics

------------------------------------------------------------------------

# 🔑 Environment Variables

  Variable          Description
  ----------------- ---------------------
  DB_URL            Database connection
  DB_USER           Database user
  DB_PASSWORD       Database password
  JWT_PRIVATE_KEY   RSA private key
  JWT_PUBLIC_KEY    RSA public key

------------------------------------------------------------------------

# 🛡 Production Recommendations

-   Use real TLS certificates (Let's Encrypt)
-   Store secrets in Kubernetes Secrets
-   Enable rate limiting
-   Use Redis for token blacklist
-   Rotate signing keys periodically

------------------------------------------------------------------------

# 🧪 Maven Build

mvn clean install

------------------------------------------------------------------------