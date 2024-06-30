package igor.kos.mastermind.chat;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatService extends Remote {
    String REMOTE_OBJECT_NAME = "igor.kos.mastermind.rmi.service";
    void sendChatMessage(String chatMessage) throws RemoteException;
    List<String> returnChatHistory() throws RemoteException;
    void clearChatHistory() throws RemoteException;
}
