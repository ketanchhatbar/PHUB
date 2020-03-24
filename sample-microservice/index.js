var express = require('express');
var app = express();
var path = require('path');
const PORT = process.env.PORT || 3000
const GREETING = process.env.GREETING || 'Hello App User!'

app.use('/volume', express.static(path.join(__dirname + '/env'))); // volume test
app.get('/', function (req, res) {
	res.status(200).send('Welcome To DevOps for Microservices running on OpenShift!'); // health check
});
app.get('/greeting', function (req, res) { // testing config maps
	res.status(200).send(GREETING)
});
app.get('/key', function (req, res) { // testing secrets
	if(process.env.API_KEY_1) {
		res.status(200).send('API Key 1 set')
	} else {
		res.status(404).send('API Key not set - check OpenShift secrets')
	}
});
app.get('/text', function (req, res) { // testing secrets
	if(process.env.SECRET_TEXT) {
		res.status(200).send('Secret text set')
	} else {
		res.status(404).send('Secret text not set - check OpenShift secrets')
	}
});

var server = app.listen(PORT, function () {
  var port = server.address().port;
  console.log('Listening at port %s', PORT);
});
module.exports = server;
