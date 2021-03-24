const express = require('express');
const app = express();
const server = require('http').Server(app);
const ROOT = '/index.html';
const PORT = 9000;


// serve root folder
app.get('/',function (req,res) {
    res.sendFile(__dirname + ROOT);
});

// serve everything from local
app.use(express.static('.'));


server.listen(PORT, function() {
    console.log("Server is now listening at the PORT: " + PORT);
});
