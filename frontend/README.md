# Frontend - Plataforma de Gestão de Publicações Científicas

Vue 3 + Vite + Pinia + Vue Router, Tailwind CSS v4 e componentes no estilo shadcn (Reka UI).
Fontes (Newsreader e Hanken Grotesk) servidas localmente: o sistema não depende de nenhum serviço externo.

## Correr

```bash
npm install
npm run dev        # http://localhost:5173 (porta fixa: tem de coincidir com CORS_ORIGIN do backend)
```

O backend tem de estar a correr (`make up && make deploy` na pasta `backend/`).
A URL da API é `http://localhost:8080/projeto/api`; para a mudar copie `.env.example` para `.env`.

Utilizadores de demonstração (password = username): `colaborador`, `responsavel`, `administrador`.

## Scripts

| Comando | O que faz |
|---|---|
| `npm run dev` | servidor de desenvolvimento |
| `npm run build` | build de produção em `dist/` |
| `npm run test:run` | testes unitários e de componentes (Vitest + Vue Test Utils) |
| `npm run lint` | ESLint |
| `npm run format` | Prettier |

## Estrutura

```
src/
  lib/           http (axios + token), erros, formatação, cor das tags, download   <- lógica pura, testada
  stores/        Pinia: api (todas as chamadas REST), auth, publicacao, tag, user, historico
  router/        rotas, guards por sessão e por role (carregamento lazy das páginas)
  components/    app (shell, sidebar), common (diálogos, avaliação...), publicacoes, ui (base)
  pages/         auth, publicacoes, tags, historico, users, profile
```

## Decisões

- **Sessão**: o JWT fica em `localStorage` (sobrevive ao F5). Um 401 da API termina a sessão e volta ao login.
  Compromisso: `localStorage` é legível por scripts em caso de XSS; os resumos de IA são escapados antes de
  serem formatados (`lib/markdownLite.js`) e não há HTML de terceiros.
- **Pesquisa no servidor** com estado no URL (`/publicacoes?q=...&tag=...&ordem=rating:desc`): partilhável e funciona com "voltar".
- **O que o utilizador não pode ver nem sequer chega ao browser**: comentários e tags ocultos são filtrados
  pelo backend; o frontend só esconde botões por conveniência (a autorização é do servidor).
- **Cor só nos dados**: a interface é monocromática; as tags têm sempre a mesma cor (hash do nome) e formam a
  "lombada" de cada publicação na lista.
- **Resumo de IA**: mostra o estado (a gerar, gerado, corrigido, falhou), atualiza sozinho enquanto está pendente
  e está sempre identificado como gerado por IA.
- Acessibilidade: `<select>` nativos, diálogos com foco preso (Reka UI), `aria-live` nos resultados,
  ligação "saltar para o conteúdo", `prefers-reduced-motion` respeitado, modo claro/escuro/sistema.
