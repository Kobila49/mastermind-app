package igor.kos.mastermind.thread;


import igor.kos.mastermind.model.GameMove;

import java.util.List;

public class SaveNewGameMoveThread extends GameMoveThread implements Runnable {

    private final List<GameMove> gameMoveList;

    public SaveNewGameMoveThread(List<GameMove> gameMoveList) {
        this.gameMoveList = gameMoveList;
    }

    @Override
    public void run() {
        saveNewGameMoveToFile(gameMoveList);
    }
}
