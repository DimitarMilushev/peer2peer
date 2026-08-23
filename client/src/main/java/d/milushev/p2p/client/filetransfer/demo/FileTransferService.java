package main.java.d.milushev.p2p.client.filetransfer;


import main.java.d.milushev.p2p.client.repository.models.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.channels.Selector;


public class FileTransferService
{
    private static final Logger LOG = LogManager.getLogger(FileTransferService.class);


    public void receiveFile(String destinationPath, User user)
    {
        LOG.info("Receiving file to: {} from user: {}", destinationPath, user);
        final FileTransferDownloadHandler handler = new FileTransferDownloadHandler();

        handler.handleFileTransfer(user, destinationPath); // Assuming you have a way to get the User and Selector
    }


    public void sendFile(Selector selector)
    {
        LOG.info("Sending file to: {}", selector);
        final FileTransferUploadHandler handler = new FileTransferUploadHandler();

        handler.handleFileTransfer(selector);
    }
}
