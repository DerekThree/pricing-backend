# pricing-backend

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
