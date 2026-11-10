# Production Dockerfile for Raviranjan.OS Frontend & Admin CMS
FROM node:18-alpine

WORKDIR /app

# Copy configuration, server, and static public assets
COPY package.json ./
COPY server.js ./
COPY public ./public

# Expose port 3000
EXPOSE 3000
ENV PORT=3000
ENV NODE_ENV=production

# Run lightweight, zero-dependency Node.js server with reverse-proxy
CMD ["node", "server.js"]
