package igor.kos.mastermind.thread;


import igor.kos.mastermind.MainController;
import igor.kos.mastermind.MastermindApp;
import igor.kos.mastermind.model.GameState;
import javafx.application.Platform;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

@Log4j2
public class PlayerOneServerThread implements Runnable {
    @Override
    public void run() {
        playerOneAcceptRequests();
    }

    private static void playerOneAcceptRequests() {
        try (ServerSocket serverSocket = new ServerSocket(MastermindApp.PLAYER_ONE_SERVER_PORT)){
            log.info("Server listening on port: {}", serverSocket.getLocalPort());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("Client connected from port: {}", clientSocket.getPort());
                Platform.runLater(() ->  processSerializableClient(clientSocket));
            }
        }  catch (IOException e) {
            log.error("Error accepting client connection", e);
        }
    }

    private static void processSerializableClient(Socket clientSocket) {
        try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())){
            GameState gameState = (GameState)ois.readObject();
            log.info("Player one received the game state!");
            MainController.updateFromGameState(gameState);
//            oos.writeObject("Player one received the game state - confirmation!");
        } catch (IOException | ClassNotFoundException e) {
            log.warn("Error processing client request", e);
        }
    }
}
