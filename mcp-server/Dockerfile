FROM node:22-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY tsconfig.json vitest.config.ts ./
COPY src ./src
COPY tests ./tests
RUN npm run build && npm test

FROM node:22-alpine
WORKDIR /app
COPY package.json ./
RUN npm install --omit=dev
COPY --from=build /app/dist ./dist
ENV MCP_TRANSPORT=http
ENV MCP_HTTP_PORT=3100
ENV BANKING_API_URL=http://host.docker.internal:8080
EXPOSE 3100
CMD ["node", "dist/http.js"]
