var request = require('supertest');
process.env['GREETING'] = 'Hello World!'

describe('loading express', function () {
  var server;
  beforeEach(function () {
    server = require('../index');
  });
  afterEach(function () {
    server.close();
  });
  it('responds to /', function testBase(done) {
    request(server)
      .get('/')
      .expect(200, done);
  });
  it('404 everything else', function testPath(done) {
    request(server)
      .get('/message')
      .expect(404, done);
  });
});

describe('checking environment variables', function () {
  var server;
  beforeEach(function () {
    server = require('../index');
  });
  afterEach(function () {
    server.close();
  });
  it('200 when environment variable exists', function testEnvVarExists(done) {
    request(server)
    .get('/greeting')
    .expect(200, done);
  });
  it('404 when environment variable not found', function testEnvVarNotSet(done) {
    request(server)
    .get('/key')
    .expect(404, done);
  });
});
