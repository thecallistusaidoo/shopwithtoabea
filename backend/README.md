# Toabea's Closet and More: Backend

This folder is an **IntelliJ IDEA-importable Java 21+ Spring Boot Maven project**. It replaces browser-only order and inventory storage with a MongoDB-backed API, and now also verifies real Paystack payments and issues signed Cloudinary upload credentials.

## What is implemented

The API includes product and category management, customer records, orders, inventory transactions, atomic stock reservation, protected admin endpoints, BCrypt password hashing, JWT authentication, login rate limiting, CORS configuration, actuator health checks, a one-time environment-driven admin bootstrap, real Paystack transaction verification (both a client callback endpoint and a signature-checked webhook), a configurable Paystack fee surcharge, signed Cloudinary upload credentials, and an admin-editable settings document for the WhatsApp notification number. Order creation validates each line item and decrements stock with a server-side atomic `stock >= quantity` check. If a product cannot satisfy the requested quantity, the request fails rather than creating an oversold order. Mongo transactions are enabled for MongoDB Atlas replica sets. Unpaid Paystack orders older than 30 minutes are automatically expired and their stock is restored by a scheduled job, so abandoned checkouts do not permanently lock inventory.

## IntelliJ IDEA setup

Open the `backend` folder in IntelliJ IDEA and choose **Open as Project**. IntelliJ will detect `pom.xml`. Set the project SDK and Maven runner JDK to Java 21 or newer. Copy `.env.example` to a private environment configuration, replace the placeholders, and run `ClosetBackendApplication`.

Do not commit a real `.env` file, Atlas connection string, JWT secret, Paystack secret key, Cloudinary API secret, or admin password. Configure those values in IntelliJ Run/Debug Configuration environment variables, a deployment secret manager, or the hosting platform's secret store.

## MongoDB Atlas setup

Create a production Atlas cluster, database user, network access rule, and database named `toabea_closet`. Use the Atlas SRV URI as `MONGODB_URI`. Atlas must allow the application server's outbound IP or use an appropriate private networking configuration. The application creates indexes automatically on startup. MongoDB Atlas backup and point-in-time recovery should also be enabled in the Atlas console; the included `scripts/backup-mongodb.sh` provides an additional compressed `mongodump` backup that can be run by cron or a deployment scheduler.

Install the MongoDB Database Tools on the backup host, mark the script executable, and schedule it, for example with `0 2 * * * /absolute/path/backend/scripts/backup-mongodb.sh >> /var/log/toabea-backup.log 2>&1`. Store backup archives on a separate encrypted location and periodically test restoration. A backup script is not a substitute for enabling Atlas automated backups.

## Paystack setup

Create a Paystack account, then take the **secret key** and **public key** from Settings > API Keys & Webhooks and set them as `PAYSTACK_SECRET_KEY` and `PAYSTACK_PUBLIC_KEY`. In that same dashboard page, set the webhook URL to `https://<your-api-domain>/api/payments/paystack/webhook` so Paystack can confirm payments even if the customer's browser tab closes before it calls back. `PAYSTACK_FEE_PERCENT` (default `1.95`) and `PAYSTACK_FEE_FLAT` (default `0`) control the surcharge added to the customer's total so the store receives the full product price after Paystack's cut; confirm the exact rate on your own Paystack pricing page and adjust if your account has a different negotiated rate. Every payment is re-verified server-side against Paystack's own `/transaction/verify` endpoint before an order is marked paid: the client-reported status is never trusted on its own.

## Cloudinary setup

Create a Cloudinary account, then take the cloud name, API key, and API secret from the dashboard home page and set `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET`. The API secret never leaves the server: the admin dashboard requests a short-lived signature from `GET /api/uploads/signature`, then uploads the image file directly to Cloudinary using that signature.

## Important environment variables

`MONGODB_URI` is the Atlas connection string. `JWT_SECRET` must be a random secret of at least 32 bytes; the app logs a warning at startup (and refuses to start under the `prod` profile) if it is missing, default, or too short. `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD` create the first admin only when that username does not already exist. Change or remove the bootstrap password after the first successful run. `CORS_ALLOWED_ORIGINS` must contain the real HTTPS storefront and admin origins in production. See `.env.example` for the full list, including the Paystack and Cloudinary variables above.

## Main endpoints

Public endpoints are `GET /api/products/public`, `GET /api/categories/public`, `GET /api/config/public`, `POST /api/orders`, `POST /api/auth/login`, `POST /api/payments/paystack/verify`, `POST /api/payments/paystack/webhook`, and `GET /actuator/health`. Admin JWT authentication is required for product CRUD, category CRUD, customer records, order listing/status updates, inventory history, settings, Cloudinary upload signatures, and the password-change endpoint. Send `Authorization: Bearer <token>` after login. Login attempts are rate-limited per client address (5 attempts per 15 minutes) to slow down credential-guessing.

## Production safeguards still required before launch

Configure HTTPS termination in front of this service (the app sends an HSTS header but does not terminate TLS itself), rate limiting at the edge for endpoints beyond login, structured logs, monitoring, secret rotation, a tested restore procedure, and a production deployment target. The in-memory login rate limiter resets on restart and does not share state across multiple instances; a multi-instance deployment should move it to a shared store such as Redis. The included code is a solid foundation and should not be called production-live until those deployment steps are completed.
