FROM node:latest

WORKDIR /usr/src/app

RUN mkdir -p /usr/src/app/env

COPY package.json ./

RUN npm install

COPY . .

EXPOSE 3000

ENTRYPOINT ["npm", "start"]
