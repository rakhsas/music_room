.PHONY: install up deps backend web dev test test-backend test-web down logs app-build app-test

# Local setup: Postgres + Redis + Adminer run in Docker (docker-compose.yml); the NestJS
# backend and the Next.js web app run on the host. Every dependency is pinned in the
# package-lock.json files, so `make install` on a fresh clone is all it takes (subject IV.1).

# One .env for everything, at the repo root (template: .env.example). Its DATABASE_URL /
# REDIS_URL use the Docker names (db, redis); a backend running on the host reaches them
# through the published ports instead, so these override the file for host runs.
HOST_ENV = PORT=8000 DATABASE_URL=postgresql://postgres:postgres123@localhost:5433/musicroom REDIS_URL=redis://localhost:6379/1

# Fresh clone: install every dependency and create .env from the template.
install: .env
	cd Backend-Nest && npm ci
	cd web && npm ci

.env:
	cp .env.example .env

# Postgres (localhost:5433), Redis (localhost:6379), Adminer (localhost:8081).
deps:
	docker compose up -d db redis adminer

# Backend on http://localhost:8000 (Swagger: /swagger), hot reload.
backend: deps
	cd Backend-Nest && $(HOST_ENV) npm run start:dev

# Web app on http://localhost:3000.
web:
	cd web && npm run dev

# Backend + web together in one terminal (Ctrl+C stops both).
dev: deps
	(cd Backend-Nest && $(HOST_ENV) npm run start:dev) & (cd web && npm run dev) & wait

# Everything that runs without a device: backend unit + e2e (uses a separate
# musicroom_test database, created automatically) and web unit tests.
test: test-backend test-web

test-backend: deps
	cd Backend-Nest && npm test

test-web:
	cd web && npm test

# Whole stack in Docker (db, redis, adminer, backend :8000, web :3000) - no Node needed on the host.
# Ports taken (e.g. by `make dev`)? API_PORT=18000 WEB_PORT=13000 make up
up: .env
	docker compose up -d --build --wait

down:
	docker compose down

logs:
	docker compose logs -f

# Android (needs a JDK + Android SDK, and App/app/google-services.json).
app-build:
	cd App && ./gradlew assembleDebug

app-test:
	cd App && ./gradlew testDebugUnitTest
