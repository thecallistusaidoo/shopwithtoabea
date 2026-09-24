# Toabea's Closet customer website

Host this folder as the public customer storefront. `index.html` is the primary entry point; `shopzone.html` is kept as an identical compatibility entry point for older links.

This site calls the backend in `../backend/` for products, categories, orders, and Paystack payment verification: it is not a standalone static site. Set the `API_BASE` constant near the top of the `<script>` block to your backend's URL before deploying, and make sure the backend's `CORS_ALLOWED_ORIGINS` includes this site's real origin. The product catalog is empty until items are added through the admin dashboard; nothing here is seeded automatically.
