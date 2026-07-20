# Stage 1: Build nextjs
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .


ARG NEXT_PUBLIC_SPRING_API_URL
ENV NEXT_PUBLIC_SPRING_API_URL=$NEXT_PUBLIC_SPRING_API_URL

RUN npm run build

# Stage 2: Install production dependencies and run app
FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm ci --only=production
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public

EXPOSE 3000
CMD ["npm", "start"]