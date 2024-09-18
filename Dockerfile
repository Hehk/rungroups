FROM node:18

WORKDIR /app

RUN curl -sLO https://github.com/babashka/babashka/releases/download/v1.4.192/babashka-1.4.192-linux-amd64-static.tar.gz \
    && tar -xzf babashka-1.4.192-linux-amd64-static.tar.gz \
    && mv bb /usr/local/bin/ \
    && rm babashka-1.4.192-linux-amd64-static.tar.gz

# Copy package.json and package-lock.json to leverage Docker cache
COPY package*.json ./

RUN npm install

COPY . .

RUN bb download-data
RUN bb build

EXPOSE 8788

CMD ["bb", "dev-server"]
