package igor.kos.mastermind.util;

import igor.kos.mastermind.MastermindApp;
import igor.kos.mastermind.model.GameState;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Log4j2
public class PlayerCommunicationUtil {

    private PlayerCommunicationUtil() {
    }

    public static void playerOneSendRequest(GameState gameState) {
        try (Socket clientSocket = new Socket(MastermindApp.HOST, MastermindApp.PLAYER_TWO_SERVER_PORT)) {
            log.info("Client is connecting to {}:{}", clientSocket.getInetAddress(), clientSocket.getPort());

            sendSerializableRequest(clientSocket, gameState);
            log.info("Game state sent to Player two!");
        } catch (IOException e) {
            log.error("Error sending request to player two", e);
        }
    }

    public static void playerTwoSendRequest(GameState gameState) {
        try (Socket clientSocket = new Socket(MastermindApp.HOST, MastermindApp.PLAYER_ONE_SERVER_PORT)) {
            log.info("Client is connecting to {}:{}", clientSocket.getInetAddress(), clientSocket.getPort());

            sendSerializableRequest(clientSocket, gameState);
            log.info("Game state sent to Player one!");
        } catch (IOException e) {
            log.error("Error sending request to player one", e);
        }
    }

    private static void sendSerializableRequest(Socket client, GameState gameState) throws
            IOException {
        ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
        oos.writeObject(gameState);
    }

}
