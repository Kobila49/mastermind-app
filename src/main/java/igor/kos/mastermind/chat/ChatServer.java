package igor.kos.mastermind.chat;


import igor.kos.mastermind.exception.ChatServerException;
import igor.kos.mastermind.jndi.ConfigurationReader;
import igor.kos.mastermind.model.ConfigurationKey;
import lombok.extern.log4j.Log4j2;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

@Log4j2
public class ChatServer {
    private static final int RANDOM_PORT_HINT = 0;

    public static void main(String[] args) {
        try {
            int rmiPort = Integer.parseInt(ConfigurationReader.getValue(ConfigurationKey.RMI_PORT));
            Registry registry = LocateRegistry.createRegistry(rmiPort);
            ChatService remoteService = new ChatServiceImpl();
            ChatService skeleton = (ChatService) UnicastRemoteObject.exportObject(remoteService, RANDOM_PORT_HINT);
            registry.rebind(ChatService.REMOTE_OBJECT_NAME, skeleton);
            log.info("Chat service ready!");
        } catch (RemoteException e) {
            throw new ChatServerException("Error starting chat server", e);
        }
    }

}
