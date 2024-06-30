package igor.kos.mastermind.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GameMove implements Serializable {
    private Integer colorIndex;
    private Integer row;
    private Integer column;

    private GameMoveType gameMoveType;

    private LocalDateTime localDateTime;

    private Integer tries;
}
