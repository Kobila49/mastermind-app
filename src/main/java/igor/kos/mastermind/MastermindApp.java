package igor.kos.mastermind;

import igor.kos.mastermind.exception.WrongPlayerNameException;
import igor.kos.mastermind.model.Player;
import igor.kos.mastermind.model.PlayerRole;
import igor.kos.mastermind.model.PlayerType;
import igor.kos.mastermind.thread.PlayerOneServerThread;
import igor.kos.mastermind.thread.PlayerTwoServerThread;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MastermindApp extends Application {
    public static Player player;

    public static final int PLAYER_TWO_SERVER_PORT = 1989;
    public static final int PLAYER_ONE_SERVER_PORT = 1990;

    public static final String HOST = "localhost";

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MastermindApp.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1600, 1200);
        stage.setTitle(player.getPlayerType().name());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        String firstArgument = args[0];

        if (PlayerType.valueOf(firstArgument).equals(PlayerType.PLAYER_ONE)) {
            player = new Player(PlayerType.PLAYER_ONE, PlayerRole.CODE_MAKER);
            Thread serverStater = new Thread(new PlayerOneServerThread());
            serverStater.start();
        } else if (PlayerType.valueOf(firstArgument).equals(PlayerType.PLAYER_TWO)) {
            player = new Player(PlayerType.PLAYER_TWO, PlayerRole.CODE_BREAKER);
            Thread serverStater = new Thread(new PlayerTwoServerThread());
            serverStater.start();
        } else if (PlayerType.valueOf(firstArgument).equals(PlayerType.SINGLE_PLAYER)) {
            player = new Player(PlayerType.SINGLE_PLAYER, PlayerRole.CODE_BREAKER);
        } else {
            throw new WrongPlayerNameException("The game was started with the player name: "
                    + firstArgument + ", but only PLAYER_ONE and PLAYER_TWO are supported.");
        }

        launch();
    }
}
