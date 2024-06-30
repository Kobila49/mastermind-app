package igor.kos.mastermind.util;


import igor.kos.mastermind.model.GameMove;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GameMoveUtils {

    private static final String GAME_MOVE_HISTORY_FILE_NAME = "src/main/resources/gameMoves/gameMoves.dat";
    private static List<GameMove> gameMoveList = new ArrayList<>();

    private GameMoveUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void saveNewGameMove(List<GameMove> newGameMoveList) {
        gameMoveList.addAll(newGameMoveList);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(GAME_MOVE_HISTORY_FILE_NAME))) {
            oos.writeObject(gameMoveList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static GameMove getLastGameMove() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(GAME_MOVE_HISTORY_FILE_NAME))) {
            gameMoveList = (List<GameMove>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return gameMoveList.getLast();
    }

}
