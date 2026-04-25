.PHONY: up down restart logs ps clean build pull

CYAN  := \033[0;36m
RESET := \033[0m

## Start the full stack (build if needed)
up:
	@echo "$(CYAN)▶ Starting Box Dispatch stack…$(RESET)"
	cp -n .env.example .env 2>/dev/null || true
	docker compose up -d --build
	@echo ""
	@echo "$(CYAN)Services ready:$(RESET)"
	@echo "  App       → http://localhost:8000"
	@echo "  Swagger   → http://localhost:8000/docs"
	@echo "  Grafana   → http://localhost:3000  (admin/admin)"
	@echo "  Prometheus→ http://localhost:9090"
	@echo "  Loki      → http://localhost:3100"

## Start without rebuilding the app image
up-no-build:
	docker compose up -d

## Stop all services
down:
	docker compose down

## Stop and remove all volumes (full reset)
clean:
	docker compose down -v --remove-orphans

## Restart only the app
restart-app:
	docker compose restart app

## Follow logs from all services
logs:
	docker compose logs -f

## Follow logs from the app only
logs-app:
	docker compose logs -f app

## Show container status
ps:
	docker compose ps

## Pull latest images (won't rebuild app)
pull:
	docker compose pull prometheus grafana loki promtail postgres redis postgres-exporter redis-exporter

## Build the app image only
build:
	docker compose build app

## Open Grafana in browser 
open-grafana:
	open http://localhost:3000 2>/dev/null || xdg-open http://localhost:3000
reload-prometheus:
	curl -s -X POST http://localhost:9090/-/reload && echo "Prometheus reloaded"