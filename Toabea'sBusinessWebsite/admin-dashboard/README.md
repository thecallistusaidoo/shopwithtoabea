# Toabea admin dashboard

Host this folder separately from the customer website, preferably on a private admin subdomain or behind host-level access controls. `index.html` is the dashboard entry point.

The dashboard authenticates against the backend in `../backend/` with a JWT: it has no login form for creating an account. The first admin account is created by the backend from the `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD` environment variables described in `backend/README.md`. Set the `API_BASE` constant near the top of the `<script>` block to your backend's URL before deploying, and make sure the backend's `CORS_ALLOWED_ORIGINS` includes this dashboard's real origin. Product photos are uploaded directly to Cloudinary using a signature issued by the backend; no image data passes through browser storage.
