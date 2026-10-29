/** URL base da API. Em desenvolvimento aponta para o WildFly local; configurável em .env (VITE_API_URL). */
export const API_BASE_URL = (
  import.meta.env.VITE_API_URL || 'http://localhost:8080/projeto/api'
).replace(/\/$/, '')

/** Limite de upload alinhado com MAX_UPLOAD_MB do backend (por omissão 50). */
export const MAX_UPLOAD_MB = Number(import.meta.env.VITE_MAX_UPLOAD_MB || 50)
