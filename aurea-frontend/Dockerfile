# Etapa 1: compilación de la SPA con Vite
FROM node:22-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# La API se sirve en el mismo origen: nginx reenvía /api al backend.
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# Etapa 2: archivos estáticos servidos por nginx
# Rama estable de nginx sobre Alpine; apk upgrade aplica los parches de seguridad publicados después de la imagen.
FROM nginx:stable-alpine
RUN apk upgrade --no-cache
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
