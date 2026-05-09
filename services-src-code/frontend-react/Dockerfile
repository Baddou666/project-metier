FROM node:22-alpine AS base

WORKDIR /app

COPY package.json package-lock.json ./

FROM base AS development

ENV NODE_ENV=development

RUN npm ci

COPY . .

EXPOSE 3000

CMD ["npm", "run", "dev"]

FROM base AS build

ARG API_URL=""
ENV API_URL=${API_URL}
ENV NODE_ENV=production

RUN npm ci

COPY . .
RUN npm run build

FROM base AS production

ENV NODE_ENV=production

RUN npm ci --omit=dev && npm cache clean --force

COPY --from=build /app/dist ./dist

EXPOSE 3000

CMD ["npm", "run", "start"]
