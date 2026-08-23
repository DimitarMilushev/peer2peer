package main.java.d.milushev.p2p.client.filetransfer.demo;


public enum FileTransferState
{
    IDLE,
    CONNECTED,
    FILE_CHECK,
    FILE_CONFIRMED,
    SENDING,
    FINISHED
}
