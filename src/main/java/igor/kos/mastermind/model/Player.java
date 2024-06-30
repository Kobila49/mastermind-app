package igor.kos.mastermind.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;


@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class Player {

    private PlayerType playerType;
    private PlayerRole playerRole;
}
