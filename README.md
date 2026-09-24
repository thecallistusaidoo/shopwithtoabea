# Toabea's Closet and More

A small Ghana clothing resale storefront with a customer-facing site, a protected admin dashboard, and a Spring Boot and MongoDB backend that both talk to.

## Project layout

| Folder | Entry point | Purpose |
|---|---|---|
| `actual-site/` | `index.html` (`shopzone.html` is an identical compatibility copy) | Public customer storefront |
| `admin-dashboard/` | `index.html` | Store administration dashboard |
| `backend/` | `ClosetBackendApplication` | Java 21+ Spring Boot API backed by MongoDB |

Deploy `actual-site/` and `admin-dashboard/` as separate static hosts or subdomains, and deploy `backend/` as a server process. Keep the admin host private or protected with hosting-platform access controls.

## How the pieces fit together

Both HTML files call the backend over HTTP: the storefront reads products and categories, creates orders, and confirms Paystack payments; the admin dashboard authenticates with a JWT and manages products, categories, orders, customers, inventory history, and store settings. Product images are uploaded straight from the admin browser to Cloudinary using a short-lived signature issued by the backend, so the Cloudinary API secret never leaves the server. Read `backend/README.md` before running the API: it explains the MongoDB Atlas, Paystack, and Cloudinary setup steps and every required environment variable.

Both HTML files have a single `API_BASE` constant near the top of their `<script>` block, defaulted to `http://localhost:8080`. Update it, and the backend's `CORS_ALLOWED_ORIGINS` environment variable, before deploying anywhere other than localhost.

## Running it locally

1. Start the backend (see `backend/README.md`) with a `.env` pointing at your own MongoDB Atlas cluster, Paystack keys, and Cloudinary credentials.
2. Serve `actual-site/` and `admin-dashboard/` with a local static server on one of the backend's default allowed origins, for example VS Code's Live Server on port 5500. Opening the HTML files directly from disk (a `file://` URL) will fail CORS checks against the API, since browsers do not send a matching origin for local files.
3. Sign in to the admin dashboard with the bootstrap admin account created from `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD`, then add a category and a product before expecting the storefront to show anything: the catalog starts empty and is never auto-seeded with demo data.

## Payments

Checkout offers Paystack (card, mobile money, and bank transfer through the Paystack popup) and Cash on Pickup. For Paystack orders, the backend adds its own processing fee on top of the item subtotal (configurable, defaulting to Ghana's published 1.95% rate) so the store receives the full listed price after Paystack takes its cut; the fee is shown to the customer as its own line item before they pay. Every payment is re-verified against Paystack's own API on the server before an order is marked paid, so the client's reported status is never trusted on its own.

## Security notes

Admin accounts are created only through the backend's bootstrap environment variables, never through a form in the browser. Passwords are hashed with BCrypt, sessions are stateless JWTs, login attempts are rate-limited, and product, order, and customer management endpoints all require an authenticated admin. See `backend/README.md` for the full list of production safeguards still worth adding (HTTPS termination, edge rate limiting, monitoring, and secret rotation).
