.DEFAULT_GOAL := deploy

ifneq (,$(wildcard ./.env))
    include .env
    export
    APPLICATION_NAME ?= projeto
endif

# --- infraestrutura ---
up:
	docker compose up -d --build

down:
	docker compose down --rmi local --volumes

down-all:
	docker compose down --rmi all --volumes

ps:
	docker compose ps

logs:
	docker compose logs -f webserver

bash:
	docker compose exec webserver bash

sql:
	docker compose exec db psql --username ${DB_USER} --dbname ${DB_NAME}

mails:
	docker compose exec --workdir /var/mail smtp bash

# descarrega (ou atualiza) o modelo de IA
pull-model:
	docker compose run --rm ollama-pull

# --- aplicação ---
build:
	mvn clean package

deploy: build
	docker compose cp target/${APPLICATION_NAME}.war webserver:/opt/jboss/wildfly/standalone/deployments

monitor: deploy logs

# --- testes ---
# unitários (rápidos, sem servidor)
test:
	mvn test

# integração: precisa da aplicação em execução (make up && make deploy)
it:
	mvn verify -Pit
