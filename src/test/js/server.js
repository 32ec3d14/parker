const { createServer } = require('http');
const { readFileSync } = require('fs');

const data = JSON.parse(readFileSync(__dirname + '/links.json'));

let total = 0;

const server = createServer((req, res) => {
  if (total++ % 100 == 0) process.stdout.write('.');

  const path = req.url.substring(1);
  const { timing, links, error } = data[path];

  if (error) {
    res.statusCode = 500;
    res.end('An error occurred');
  } else {
    setTimeout(
      () => res.end(links.map(i => `<a href="${i}">${i}</a>`).join('\n')),
      timing
    );
  }
});

module.exports.server = server;

if (require.main === module) {
  server.listen(0, () => console.log(`listening on ${server.address().port}`));
}
