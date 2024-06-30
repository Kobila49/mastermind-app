package igor.kos.mastermind.chat;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ChatServiceImpl implements ChatService {

    private final List<String> chatMessagesHistory = new ArrayList<>();

    @Override
    public void sendChatMessage(String chatMessage) throws RemoteException {
        chatMessagesHistory.add(chatMessage);
    }

    @Override
    public List<String> returnChatHistory() throws RemoteException {
        return chatMessagesHistory;
    }

    @Override
    public void clearChatHistory() throws RemoteException {
        chatMessagesHistory.clear();
    }

}
