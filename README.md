# Plataforma de Publicações Científicas

Aplicação web para submeter, organizar e consultar publicações científicas (artigos, teses, relatórios e datasets). Cada publicação em PDF recebe um **resumo estruturado gerado por IA** (Ollama, executado localmente), que pode ser editado ou regenerado.

Projeto desenvolvido no âmbito do curso de Engenharia Informática (ESTG – Politécnico de Leiria).

## Funcionalidades

- **Autenticação com JWT**, recuperação e reposição de password por e-mail
- **Três perfis** de utilizador: colaborador, responsável e administrador
- **Publicações**: upload de PDF/ZIP, pesquisa, tags, avaliações (ratings), comentários e descarga
- **Moderação**: publicações e comentários podem ser ocultados e tornados visíveis de novo
- **Resumos por IA**: geração automática a partir do texto do PDF, com estados (gerado, editado, erro) e opção "Gerar de novo"
- **Subscrição de tags** com notificações por e-mail
- **Histórico de ações** e estatísticas por utilizador e por publicação
- **Gestão de utilizadores** (criar, alterar perfil, suspender, apagar)

## Destaques técnicos

- **Pipeline de IA resiliente**: *circuit breaker* (abre após falhas consecutivas), limitador de concorrência com fila e limites de páginas/tempo na leitura de PDFs. Se o Ollama estiver em baixo, a aplicação continua a funcionar e os resumos ficam em estado de erro, podendo ser regenerados mais tarde.
- **Jakarta EE 9.1** com EJBs (incluindo processamento assíncrono), JPA/Hibernate, JAX-RS (RESTEasy) e Bean Validation
- **Segurança**: JWT (jjwt), validação de roles, configuração por variáveis de ambiente
- **Ambiente totalmente containerizado** com Docker Compose (BD, SMTP de desenvolvimento, IA e servidor aplicacional)
- **Testes**: unitários (JUnit 5), de integração com REST Assured (perfil Maven `it`) e do frontend com Vitest
- **Seed idempotente** que cobre todos os cenários (roles, utilizador suspenso, tag oculta, publicação oculta, comentário oculto, ratings e subscrições)

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 17, Jakarta EE 9.1, WildFly 27, Hibernate 6, RESTEasy, jjwt, Apache PDFBox |
| Base de dados | PostgreSQL 16 |
| IA | Ollama (modelo `llama3.2` por omissão) |
| Frontend | Vue 3, Vite, Pinia, Vue Router, Tailwind CSS 4, Reka UI, Axios |
| Infraestrutura | Docker Compose, FakeSMTP |
| Testes | JUnit 5, REST Assured, Vitest, Vue Test Utils |

## Estrutura do repositório

```
.
├── backend/     # API REST (Jakarta EE) + docker-compose + Makefile
└── frontend/    # Interface web (Vue 3)
```

## Como executar

### Pré-requisitos

- Docker e Docker Compose
- JDK 17 e Maven
- Node.js `^20.19.0` ou `>=22.12.0`
- `make` (opcional, mas simplifica os comandos)

### 1. Backend

```bash
cd backend
cp .env.example .env        # depois edite o .env e defina valores próprios (em especial JWT_SECRET)
make up                     # levanta BD, SMTP, Ollama e WildFly
make pull-model             # descarrega o modelo de IA (só na primeira vez)
make deploy                 # compila e instala o .war no WildFly
```

A API fica disponível em `http://localhost:8080`. Na primeira arrancada, a base de dados é populada com os dados de demonstração abaixo.

Outros comandos úteis: `make logs`, `make ps`, `make sql`, `make mails` (ver e-mails enviados), `make down`.

### 2. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

A interface fica disponível em `http://localhost:5173`.

### 3. Testes

```bash
# Backend
cd backend
make test                   # unitários (sem servidor)
make it                     # integração (requer: make up && make deploy)

# Frontend
cd frontend
npm run test:run
```

## Contas de demonstração

Criadas automaticamente pelo seed na primeira execução.

> ⚠️ **Apenas para desenvolvimento e demonstração.** Estas credenciais são públicas. Nunca as use num ambiente real e altere/remova estas contas antes de qualquer deploy.

| Username | Password | Perfil | Estado |
|---|---|---|---|
| `administrador` | `administrador` | Administrador | Ativo |
| `responsavel` | `responsavel` | Responsável | Ativo |
| `colaborador` | `colaborador` | Colaborador | Ativo |
| `colaborador2` | `colaborador2` | Colaborador | Ativo |
| `suspenso` | `suspenso1` | Colaborador | Suspenso (não consegue entrar) |

## Configuração

Todas as definições sensíveis vêm de variáveis de ambiente (ficheiro `backend/.env`, que **não** é versionado). O ficheiro `backend/.env.example` indica as variáveis necessárias.

Principais opções (com valores por omissão):

| Variável | Descrição | Omissão |
|---|---|---|
| `JWT_SECRET` | Segredo para assinar os tokens (obrigatório) | — |
| `OLLAMA_MODEL` | Modelo de IA usado nos resumos | `llama3.2` |
| `MAX_UPLOAD_MB` | Tamanho máximo de upload | `50` |
| `AI_MAX_CONCURRENT` / `AI_MAX_QUEUE` | Resumos em simultâneo / tamanho da fila | `1` / `20` |
| `AI_BREAKER_FAILURES` / `AI_BREAKER_OPEN_SECONDS` | Falhas para abrir o *circuit breaker* / tempo aberto | `3` / `60` |
| `PDF_MAX_PAGES` / `PDF_TIMEOUT_SECONDS` | Limites de leitura de PDF | `150` / `60` |
| `CORS_ORIGIN` / `APP_BASE_URL` | Endereço do frontend | `http://localhost:5173` |
