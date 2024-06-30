package igor.kos.mastermind.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class GameState implements Serializable {

  public static final String SAVE_GAME_FILE_NAME = "src/main/resources/saveGame/gameSave.dat";

  private PlayerRole activePlayer;
  private Map<Integer, Integer[]> guessMap;
  private Integer[] solution;
  private int tries;
  private GameMove lastMove;
  private List<GameMove> gameMoves;
  private int currentRow;
    
}
