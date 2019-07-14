#!/usr/bin/env node
const { server } = require('./server');
const { execFile } = require('child_process');

server.listen(0, () => {
  const port = server.address().port;
  console.log(`Test server started on port ${port}`);

  const start = Date.now();

  execFile(
    './bin/parker',
    [`http://localhost:${port}/0`, 1],
    { maxBuffer: 1024 * 1024 * 25 },
    (error, stdout, stderr) => {
      server.close();
      if (error) {
        throw error;
      }

      console.log(`Crawl completed in ${Date.now() - start}ms`);

      console.log(stderr);
    }
  );
});
