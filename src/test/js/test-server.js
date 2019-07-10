#!/usr/bin/env node

const http = require('http');

const range = count => [...Array(count).keys()];

const deeper = res => {
  setTimeout(
    () =>
      res.end(
        range(6)
          .map(i => `<a href="${i}/">Child ${i}</a>`)
          .join('\n')
      ),
    100
  );
};

const random = res => {
  setTimeout(
    () =>
      res.end(
        range(6)
          .map(() => Math.floor(Math.random() * 200))
          .map(i => `<a href="${i}">Random ${i}</a>`)
          .join('\n')
      ),
    Math.random() * 200 + 100
  );
};

const server = http.createServer((req, res) => {
  const parts = req.url.split('/');
  switch (parts[1]) {
    case 'random':
      return random(res);
    default:
      return deeper(res);
  }
});

server.listen(9999);
