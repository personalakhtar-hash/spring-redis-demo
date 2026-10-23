# Spring Boot + shared PostgreSQL + Redis

Two instances of the same product API run at `http://localhost:8081` and `http://localhost:8082`. Both connect to one PostgreSQL database on port **5433** and one Redis server on **6380**. PostgreSQL holds durable data; Redis caches individual product reads for ten minutes.

## Requirements and start (macOS Apple Silicon)

```bash
brew install openjdk@21 maven postgresql@17 redis
cd spring-redis-demo
./scripts/start.sh
python3 scripts/smoke-test.py
```

The start script initializes a project-local PostgreSQL data directory, creates the database, starts Redis, builds the JAR, and starts both instances. No global Homebrew services are enabled. Start is safe to repeat while the instances are running. Ports 5433, 6380, 8081, and 8082 must be available before the first start. On another platform, install Java 21/Maven/PostgreSQL/Redis and adjust `scripts/env.sh` paths and the Redis CLI path in the smoke test.

Stop the project processes (data is retained):

```bash
./scripts/stop.sh
```

Logs, process IDs, and local data are under `.runtime/`; build output is in `.runtime/build.log`. Do not delete `.runtime/` while processes are running.

## Try the API

```bash
# Create through instance 1; note the returned id.
curl -i -X POST http://localhost:8081/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Keyboard","price":49.99}'

# Substitute the actual id for 1. First read loads PostgreSQL and fills Redis.
curl http://localhost:8081/api/products/1
# This instance uses the same Redis entry.
curl http://localhost:8082/api/products/1

# Update through instance 2; the shared cache entry is evicted after commit.
curl -X PUT http://localhost:8082/api/products/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mechanical keyboard","price":79.99}'
curl http://localhost:8081/api/products/1

curl http://localhost:8082/api/products
curl http://localhost:8081/api/instance
curl http://localhost:8082/actuator/health
curl -X DELETE http://localhost:8082/api/products/1
```

`POST` returns 201, `DELETE` returns 204, missing products return 404, and invalid requests return 400. Lists always query the database. Single-product GETs use `@Cacheable`; PUT and DELETE evict the shared entry with `@CacheEvict`. Cache operations are transaction-aware so an update evicts only after a successful database commit. Cache misses are logged in each instance's log.

Inspect the cache and database:

```bash
/opt/homebrew/bin/redis-cli -p 6380 --scan --pattern 'products::*'
/opt/homebrew/bin/redis-cli -p 6380 TTL products::1
source scripts/env.sh
psql -c 'SELECT * FROM products;'
```

The smoke test creates and deletes its own product. It verifies a Redis entry is populated, the other instance reads it without a database-load log, the TTL is set, updates/deletes evict it, both instances observe updates, and validation and missing-product handling work.

## Additional instances and configuration

```bash
source scripts/env.sh
java -jar target/spring-redis-demo-0.0.1-SNAPSHOT.jar --server.port=8083
```

All instances share these defaults; override with environment variables when needed:

| Variable | Default |
|---|---|
| `PORT` | `8081` |
| `DB_URL` | `jdbc:postgresql://localhost:5433/cache_demo` |
| `DB_USER` | `cache_demo` |
| `DB_PASSWORD` | `local_demo_password` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6380` |

Flyway owns schema creation; Hibernate validates it on startup. Redis uses Spring's default Java serialization for the serializable product DTO, so use a dedicated, trusted Redis instance. This is a local demonstration: services bind to loopback and use demo credentials; the API has no authentication. Redis failures fail cached operations. Cache-aside invalidation is not a strict consistency guarantee during simultaneous reads and writes; stale entries from a race expire within the TTL. All database writers must participate in invalidation or accept that delay.

Reference: [Spring Data Redis cache documentation](https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html).
