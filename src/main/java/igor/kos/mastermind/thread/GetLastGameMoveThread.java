package igor.kos.mastermind.thread;

import igor.kos.mastermind.model.GameMove;
import igor.kos.mastermind.model.GameMoveType;
import javafx.scene.control.Label;

public class GetLastGameMoveThread extends GameMoveThread implements Runnable {

    private final Label label;

    public GetLastGameMoveThread(Label label) {
        this.label = label;
    }

    @Override
    public void run() {
        GameMove lastGameMove = getLastGameMoveFromFile();

        if (lastGameMove.getGameMoveType().equals(GameMoveType.COLOR_CHANGE)) {
            label.setText("Last game move: "
                    + lastGameMove.getGameMoveType() + "; (row:"
                    + lastGameMove.getRow() + ", column:"
                    + lastGameMove.getColumn() + ", colorIndex: "
                    + lastGameMove.getColorIndex() + ") "
                    + lastGameMove.getLocalDateTime());
        } else {
            label.setText("Last game move: "
                    + lastGameMove.getGameMoveType() + ";"
                    + lastGameMove.getLocalDateTime());
        }

    }
}
