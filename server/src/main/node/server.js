var PORT = 9999;
var HOST = '255.255.255.255';

var dgram = require('dgram');
var message = new Buffer('My KungFu is Good!');

var client = dgram.createSocket('udp4');


client.send(message, 0, message.length, PORT, HOST, function(err, bytes) {
    client.setBroadcast(true);
    if (err) {
        console.log(err)
    }
    console.log('UDP message sent to ' + HOST +':'+ PORT);
    client.close();
});
