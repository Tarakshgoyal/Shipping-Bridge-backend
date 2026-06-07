# Shipping Bridge Service

Spring Boot service that bridges an e-commerce platform with a logistics provider. It exposes simplified APIs for shipping rate calculation, order creation, order lookup, and tracking synchronization.

## Endpoints

- `POST /api/shipping/calculate`
- `POST /api/orders`
- `GET /api/orders/{id}`
- `GET /api/orders/{id}/tracking`

OpenAPI documentation is available at `/v3/docs`. Swagger UI is available at `/swagger-ui.html`.

## Authentication

Register a user with `POST /api/auth/register`:

```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123"
}
```

The service sends a verification email through Gmail SMTP from `taraksh9a33@gmail.com`. Configure:

```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=taraksh9a33@gmail.com
SMTP_PASS=<gmail-app-password>
EMAIL_FROM=taraksh9a33@gmail.com
EMAIL_VERIFICATION_BASE_URL=http://localhost:8080/api/auth/verify
```

For Docker Compose, keep the real app password in a local `.env` file:

```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=taraksh9a33@gmail.com
SMTP_PASS=your_gmail_app_password
```

After the user clicks the verification link, call protected `/api/**` endpoints with HTTP Basic auth using the verified username and password.

## Run Locally

```bash
docker compose up --build
```

By default the app uses the deterministic `mock` provider. To call Shiprocket, set:

```bash
LOGISTICS_PROVIDER=shiprocket
SHIPROCKET_TOKEN=<token>
SHIPROCKET_BASE_URL=https://apiv2.shiprocket.in/v1/external
```

## Database

The default runtime database is PostgreSQL:

```properties
DB_URL=jdbc:postgresql://localhost:5432/shipping_bridge
DB_USERNAME=shipping
DB_PASSWORD=shipping
```

On Render, attach a PostgreSQL database and expose its internal connection string as `DATABASE_URL`. The app converts Render's `postgres://...` URL into the JDBC URL Spring Boot needs at startup.

Neon works the same way. Put the Neon connection string in `.env` as `DATABASE_URL`; the app will convert `postgresql://...` into a JDBC URL and normalize Neon's `channel_binding` parameter for the PostgreSQL JDBC driver.

For Render deployments, configure these environment variables on the Render service itself:

```bash
DATABASE_URL=postgresql://...
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=taraksh9a33@gmail.com
SMTP_PASS=<gmail-app-password>
EMAIL_VERIFICATION_BASE_URL=https://<your-backend-host>/api/auth/verify
```

Render does not read your local `.env` file unless you add those values in the Render dashboard.

## Caching

The service uses Spring caching with an in-memory cache manager. Repeated calls for the same shipping-rate request or order lookup are served from cache. Order creation and tracking refresh update the cached order data so later reads do not need to hit the database for unchanged details.
