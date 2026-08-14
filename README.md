# pricing-backend

## Regenerating OpenAPI code

Run the Maven resource phase to regenerate backend OpenAPI classes from a clean output folder:

```powershell
.\mvnw.cmd -q clean process-resources
```

This removes generated sources and compiled class output before generation so renamed schemas and
operations do not leave stale artifacts behind, then copies Maven resources.

## Local Postgres

Create a local `.env` file from `.env.example`, then start PostgreSQL:

```powershell
docker compose up -d
```

The example local PostgreSQL configuration uses these values:

- database: `pricingdb`
- username: `pricinguser`
- password: `pricingpass`
- port: `5432`

Override them with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` if needed.

Then run the backend:

```powershell
./mvnw spring-boot:run
```
