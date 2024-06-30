package igor.kos.mastermind.thread;


import igor.kos.mastermind.model.GameMove;
import igor.kos.mastermind.util.GameMoveUtils;

import java.util.List;

public abstract class GameMoveThread {

    private static boolean gameMoveFileAccessInProgress = false;

    protected synchronized void saveNewGameMoveToFile(List<GameMove> gameMoveList) {
        while (gameMoveFileAccessInProgress) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        gameMoveFileAccessInProgress = true;

        GameMoveUtils.saveNewGameMove(gameMoveList);

        gameMoveFileAccessInProgress = false;

        notifyAll();
    }

    protected synchronized GameMove getLastGameMoveFromFile() {
        while (gameMoveFileAccessInProgress) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        gameMoveFileAccessInProgress = true;

        GameMove lastGameMove = GameMoveUtils.getLastGameMove();

        gameMoveFileAccessInProgress = false;

        notifyAll();

        return lastGameMove;
    }

}
