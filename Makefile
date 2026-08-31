.PHONY: dev down logs test verify backend-test frontend-check android-check

dev:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f --tail=200

test:
	docker compose -f compose.test.yaml up --build --abort-on-container-exit --exit-code-from backend

verify:
	python scripts/verify_sprint1.py

backend-test:
	cd backend && pytest -q -m "not integration"

frontend-check:
	cd frontend && npm run lint && npm run build

android-check:
	cd mobile && gradle test assembleDebug
