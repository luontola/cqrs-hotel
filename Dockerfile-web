FROM node:11 AS builder

# working directory
RUN mkdir -p /project && \
    chown node /project
WORKDIR /project
USER node

# cache node_modules
COPY --chown=node package.json yarn.lock /project/
RUN yarn install --frozen-lockfile --non-interactive --no-progress && \
    yarn cache clean

# do the build
COPY --chown=node .babelrc webpack.config.js /project/
COPY --chown=node src/main/web /project/src/main/web/
COPY --chown=node src/main/css /project/src/main/css/
COPY --chown=node src/main/js /project/src/main/js/
RUN yarn run test && \
    yarn run build --no-progress --no-colors

# ------------------------------------------------------------

FROM nginx:1.15-alpine

COPY etc/web/nginx-default.conf /etc/nginx/conf.d/default.conf

COPY src/main/web /usr/share/nginx/html
COPY --from=builder /project/target/webpack /usr/share/nginx/html
