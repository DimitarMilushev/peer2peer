# File Transfer Server v1
This mini-server will have the following features:
- Listen on a specific port for incoming file transfer requests.
- Handle a single file transfer at a time.
- Save the received file to a specified directory.

## Listening
- Simple nio server that listens on a specific port for incoming connections.
- It accepts a connection and reads the incoming data stream.
