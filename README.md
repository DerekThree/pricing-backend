# pricing-backend

## Regenerating OpenAPI code

Run the Maven resource phase to regenerate backend OpenAPI classes from a clean output folder:

```powershell
.\mvnw.cmd -q clean process-resources
```

This removes generated sources and compiled class output before generation so renamed schemas and
operations do not leave stale artifacts behind, then copies Maven resources.

## Local Postgres

From the main `pricing-app` repository, copy `deploy/local/.env.example` to
`deploy/local/.env`, set separate database passwords, then start PostgreSQL:

```powershell
docker compose --env-file deploy/local/.env -f deploy/local/compose.yaml up -d postgres
```

The example local PostgreSQL configuration uses these values:

- database: `pricingdb`
- username: `pricinguser`
- password: `POSTGRES_PASSWORD` from `deploy/local/.env`
- port: `5432`

Override them with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` if needed.

Then run the backend from this repository with its datasource password set:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = Read-Host 'Pricing database password'
.\mvnw.cmd spring-boot:run
```

The shared Compose file also starts the Backend, Mock Processor, and Dashboard when run without
the `postgres` service argument. `GET /actuator/health` reports database-aware health without
exposing internal details.
