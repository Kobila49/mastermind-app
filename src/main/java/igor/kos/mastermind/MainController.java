package igor.kos.mastermind;

import igor.kos.mastermind.chat.ChatService;
import igor.kos.mastermind.exception.ChatServerException;
import igor.kos.mastermind.jndi.ConfigurationReader;
import igor.kos.mastermind.model.*;
import igor.kos.mastermind.thread.GetLastGameMoveThread;
import igor.kos.mastermind.thread.SaveNewGameMoveThread;
import igor.kos.mastermind.util.XmlUtils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static igor.kos.mastermind.MastermindApp.player;
import static igor.kos.mastermind.util.DeepCopy.deepCopyArray;
import static igor.kos.mastermind.util.DeepCopy.deepCopyMap;
import static igor.kos.mastermind.util.PlayerCommunicationUtil.playerOneSendRequest;
import static igor.kos.mastermind.util.PlayerCommunicationUtil.playerTwoSendRequest;
import static igor.kos.mastermind.util.XmlUtils.saveGameMovesToXml;

@Log4j2
public class MainController {

    private static final int TRY_LIMIT = 10;
    private static final String ERROR = "Error";
    @Getter
    private static MainController instance;
    private final Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.PURPLE};
    private Integer[] colorIndices = {null, null, null, null};
    private final Map<Integer, Integer[]> guessMap = new HashMap<>();
    private final Map<Integer, List<Circle>> guessCircles = new HashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    List<Circle> solutionCircles = new ArrayList<>();
    private Integer[] solution = {null, null, null, null};

    private ChatService stub;
    int currentRow = 2;
    int tries = 0;
    int lastColorIndex;

    List<GameMove> gameMoves = new ArrayList<>();

    @FXML
    GridPane guessGrid;
    @FXML
    Button checkCodeButton;
    @FXML
    Button saveCodeButton;
    @FXML
    TextArea chatTextArea;
    @FXML
    TextField chatMessageTextField;
    @FXML
    AnchorPane chatAnchor;
    @FXML
    private Label lastGameMoveLabel;

    public MainController() {
        instance = this;
    }

    @FXML
    private void initialize() {
        setupGame();
        setupChat();
        setupGameMoveRefresh();
    }

    @FXML
    protected void checkCombination() {
        if (player.getPlayerRole().equals(PlayerRole.CODE_MAKER)) {
            showAlert(Alert.AlertType.ERROR, "You can't check the combination", ERROR);
            return;
        }

        if (Arrays.stream(colorIndices).anyMatch(Objects::isNull)) {
            showAlert(Alert.AlertType.ERROR, "Please fill all circles", ERROR);
            return;
        }

        final var solutionList = Arrays.asList(solution);

        disableRowCircles(currentRow);

        tries++;
        GameMove gameMove = new GameMove();
        gameMove.setGameMoveType(GameMoveType.CHECK_SOLUTION);
        gameMove.setTries(tries);
        gameMove.setLocalDateTime(LocalDateTime.now());
        gameMoves.add(gameMove);
        updateGameState(gameMove);

        if (tries == TRY_LIMIT) {
            showAlert(Alert.AlertType.ERROR, "You lost!", "Game over");
            return;
        }

        final var result = cipherSuccessfullyBroken(solutionList);

        if (result) {
            return;
        }

        addChildren();
        resetColorIndices();
    }

    @FXML
    protected void saveCode() {
        if (Arrays.stream(colorIndices).anyMatch(Objects::isNull)) {
            showAlert(Alert.AlertType.ERROR, "Please fill all circles", ERROR);
            return;
        }
        solution = colorIndices.clone();
        showAlert(Alert.AlertType.INFORMATION, "Code saved", "Success");
        GameMove gameMove = new GameMove();
        gameMove.setTries(tries);
        gameMove.setGameMoveType(GameMoveType.CODE_SAVE);
        gameMove.setLocalDateTime(LocalDateTime.now());
        gameMoves.add(gameMove);
        updateGameState(gameMove);
        addChildren();
        this.disableSolutionCircles();
        this.saveCodeButton.setDisable(true);
    }

    @FXML
    protected void handleColorChangeBreaker(MouseEvent event) {
        int index = changeColor(event);
        saveGameMoveAndUpdateGameState(index, currentRow);
    }

    @FXML
    protected void handleColorChangeMaker(MouseEvent event) {
        int index = changeColor(event);
        saveGameMoveAndUpdateGameState(index, 1);
    }

    @FXML
    protected void resetAll() {
        this.saveCodeButton.setDisable(false);
        resetValues();
        if (player.getPlayerType().equals(PlayerType.SINGLE_PLAYER)) {
            randomizeSolution();
        }
        initCodeSettingLabel();
        if (player.getPlayerType().equals(PlayerType.SINGLE_PLAYER)) {
            addChildren();
        }
    }

    private void setupGame() {
        initCodeSettingLabel();
        saveCodeButtonVisibility();

        if (player.getPlayerType().equals(PlayerType.SINGLE_PLAYER)) {
            randomizeSolution();
            addChildren();
            chatAnchor.setDisable(true);
        }
    }

    private void setupChat() {
        if (player.getPlayerType().equals(PlayerType.SINGLE_PLAYER)) {
            return;
        }
        try {
            setupChatService();
            startChatRefresh();
        } catch (RemoteException | NotBoundException e) {
            throw new ChatServerException("Error connecting to chat server", e);
        }
    }

    private void setupChatService() throws RemoteException, NotBoundException {
        String rmiPort = ConfigurationReader.getValue(ConfigurationKey.RMI_PORT);
        String serverName = ConfigurationReader.getValue(ConfigurationKey.RMI_HOST);
        Registry registry = LocateRegistry.getRegistry(serverName, Integer.parseInt(rmiPort));
        stub = (ChatService) registry.lookup(ChatService.REMOTE_OBJECT_NAME);
        stub.clearChatHistory();
    }

    private void startChatRefresh() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshChatTextArea()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.playFromStart();
    }

    private void setupGameMoveRefresh() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> Platform.runLater(new GetLastGameMoveThread(lastGameMoveLabel))));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.playFromStart();
    }

    private void saveGameMoveAndUpdateGameState(int index, int row) {
        GameMove gameMove = new GameMove();
        gameMove.setGameMoveType(GameMoveType.COLOR_CHANGE);
        gameMove.setColumn(index);
        gameMove.setRow(row);
        gameMove.setTries(tries);
        gameMove.setColorIndex(lastColorIndex);
        gameMove.setLocalDateTime(LocalDateTime.now());
        gameMoves.add(gameMove);

        updateGameState(gameMove);
    }

    private void updateGameState(GameMove gameMove) {
        GameState gameState = getGameState(gameMove);
        saveGameMovesToXml(gameMoves);

        SaveNewGameMoveThread saveNewGameMoveThread = new SaveNewGameMoveThread(gameMoves);
        Thread starter = new Thread(saveNewGameMoveThread);
        starter.start();

        if (player.getPlayerType().equals(PlayerType.PLAYER_ONE)) {
            playerOneSendRequest(gameState);
        } else if (player.getPlayerType().equals(PlayerType.PLAYER_TWO)) {
            playerTwoSendRequest(gameState);
        }
    }

    private GameState getGameState(GameMove gameMove) {
        GameState gameState = new GameState();
        gameState.setActivePlayer(player.getPlayerRole());
        gameState.setGuessMap(deepCopyMap(guessMap));
        gameState.setSolution(deepCopyArray(solution));
        gameState.setTries(tries);
        gameState.setCurrentRow(currentRow);
        gameState.setLastMove(gameMove);
        gameState.setGameMoves(gameMoves);
        return gameState;
    }

    public void saveGame() {
        if (!player.getPlayerType().equals(PlayerType.SINGLE_PLAYER)) {
            showAlert(Alert.AlertType.ERROR, "Only available in single player mode", ERROR);
            return;
        }
        GameState gameStateToSave = getGameState(gameMoves.getLast());

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(GameState.SAVE_GAME_FILE_NAME))) {
            oos.writeObject(gameStateToSave);
            showAlert(Alert.AlertType.INFORMATION, "Game was successfully saved!", "Game Saved");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void loadGame() {
        if (!player.getPlayerType().equals(PlayerType.SINGLE_PLAYER)) {
            showAlert(Alert.AlertType.ERROR, "Only available in single player mode", ERROR);
            return;
        }
        resetValues();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(GameState.SAVE_GAME_FILE_NAME))) {
            GameState gameStateToLoad = (GameState) ois.readObject();
            populateFromGameState(gameStateToLoad, true);
            saveGameMovesToXml(gameMoves);
            showAlert(Alert.AlertType.INFORMATION, "Game was successfully loaded!", "Game Loaded");
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private void resetValues() {
        guessGrid.getChildren().clear();
        guessCircles.clear();
        guessMap.clear();
        solutionCircles.clear();
        gameMoves.clear();
        currentRow = 2;
        tries = 0;
        solution = new Integer[]{null, null, null, null};
        resetColorIndices();
    }

    public static void updateFromGameState(GameState gameState) {
        Platform.runLater(() -> {
            MainController controller = getInstance();
            if (controller != null) {
                controller.populateFromGameState(gameState, false);
            }
        });
    }

    private void populateFromGameState(GameState gameState, boolean loadedGame) {
        resetValues();
        updateGameStateFields(gameState);

        if (gameState.getTries() > TRY_LIMIT) {
            showAlert(Alert.AlertType.INFORMATION, "You won!", "Congratulations");
            return;
        }

        setCodeLabel();
        if (!isSolutionAllNull()) {
            createSolutionCircles();
        }

        guessAndFeedbackLabel();
        if (currentRow > 2) {
            boolean result = populatePreviousGuesses(gameState, loadedGame);
            if (result) {
                return;
            }
        }

        if (!loadedGame && gameState.getLastMove() != null && !gameState.getLastMove().getGameMoveType().equals(GameMoveType.COLOR_CHANGE)) {
            addChildren();
        }
    }

    private void updateGameStateFields(GameState gameState) {
        guessMap.putAll(gameState.getGuessMap());
        solution = gameState.getSolution();
        tries = gameState.getTries();
        currentRow = gameState.getCurrentRow();
        gameMoves.addAll(gameState.getGameMoves());
    }

    private boolean isSolutionAllNull() {
        return Arrays.stream(solution).allMatch(Objects::isNull);
    }

    private void createSolutionCircles() {
        for (int i = 0; i < 4; i++) {
            Circle circle = new Circle(20);
            circle.setId(String.valueOf(i));
            circle.setFill(colors[solution[i]]);
            solutionCircles.add(circle);
            guessGrid.add(circle, i, 1);
            circle.setDisable(true);
            if (player.getPlayerRole().equals(PlayerRole.CODE_BREAKER)) {
                circle.setVisible(false);
            }
        }
    }

    private boolean populatePreviousGuesses(GameState gameState, boolean loadedGame) {
        boolean gameOver = false;
        for (int i = 3; i <= currentRow; i++) {
            populateChildren(i);
            if (gameState.getLastMove().getGameMoveType().equals(GameMoveType.CHECK_SOLUTION) || loadedGame) {
                gameOver = createFeedbackGrid(i);
            }
        }
        return gameOver;
    }

    private boolean createFeedbackGrid(int rowIndex) {

        if (!guessMap.containsKey(rowIndex)) {
            return false;
        }
        GridPane feedbackGrid = new GridPane();
        feedbackGrid.setHgap(5);
        feedbackGrid.setVgap(5);

        int feedbackIndex = 0;
        int score = 0;
        for (int j = 0; j < 4; j++) {
            if (Objects.equals(guessMap.get(rowIndex)[j], solution[j])) {
                addFeedbackCircle(feedbackGrid, feedbackIndex++, Color.BLACK);
                score++;
            }
        }

        for (int j = 0; j < 4; j++) {
            if (!Objects.equals(guessMap.get(rowIndex)[j], solution[j]) && Arrays.asList(solution).contains(guessMap.get(rowIndex)[j])) {
                addFeedbackCircle(feedbackGrid, feedbackIndex++, Color.WHITE);
            }
        }

        guessGrid.add(feedbackGrid, 4, rowIndex);
        if (score == 4) {
            showSolution();
            showAlert(Alert.AlertType.INFORMATION, "Your code was broken, you lost!", "Unlucky");
        }
        return score == 4;
    }

    private void addFeedbackCircle(GridPane feedbackGrid, int index, Color color) {
        int row = index / 2;
        int column = index % 2;
        feedbackGrid.add(new Circle(10, color), column, row);
    }

    private void populateChildren(int i) {
        var circleArrayList = new ArrayList<Circle>();
        if (!guessMap.containsKey(i)) {
            circleArrayList = getChildrenCircles();
            this.guessCircles.put(i, circleArrayList);
            return;
        }
        for (int j = 0; j < 4; j++) {
            Circle circle = new Circle(20);
            circle.setFill(guessMap.get(i)[j] != null ? colors[guessMap.get(i)[j]] : Color.WHITE);
            circle.setId(i + "_" + j);
            circle.setOnMouseClicked(this::handleColorChangeBreaker);
            if (guessMap.get(i).length == 4 && i < currentRow) {
                circle.setDisable(true);
            }

            if (player.getPlayerRole().equals(PlayerRole.CODE_MAKER)) {
                circle.setDisable(true);
            }
            guessGrid.add(circle, j, i);
            circleArrayList.add(circle);
        }
        this.guessCircles.put(i, circleArrayList);
    }

    private int changeColor(MouseEvent event) {
        Circle circle = (Circle) event.getSource();
        int index = Integer.parseInt(circle.getId().split("_")[1]);
        colorIndices[index] = colorIndices[index] != null ? (colorIndices[index] + 1) % colors.length : 0;
        lastColorIndex = colorIndices[index];
        circle.setFill(colors[colorIndices[index]]);
        this.guessMap.put(currentRow, new Integer[]{colorIndices[0], colorIndices[1], colorIndices[2], colorIndices[3]});
        return index;
    }

    private void disableSolutionCircles() {
        for (Circle circle : solutionCircles) {
            circle.setDisable(true);
        }
    }

    private void disableRowCircles(int index) {
        for (Circle circle : guessCircles.get(index)) {
            circle.setDisable(true);
        }
    }

    private void resetColorIndices() {
        Arrays.fill(colorIndices, null);
    }

    private void showSolution() {
        solutionCircles.forEach(circle -> {
            circle.setVisible(true);
            circle.setDisable(true);
        });
    }

    private boolean cipherSuccessfullyBroken(List<Integer> solutionList) {
        GridPane feedbackGrid = new GridPane();
        feedbackGrid.setHgap(5);
        feedbackGrid.setVgap(5);
        int score = 0;
        int feedbackIndex = 0;

        for (int i = 0; i < 4; i++) {
            if (Objects.equals(colorIndices[i], solution[i])) {
                int row = feedbackIndex / 2;
                int column = feedbackIndex % 2;
                feedbackGrid.add(new Circle(10, Color.BLACK), column, row);
                score++;
                feedbackIndex++;
            }
        }

        for (int i = 0; i < 4; i++) {
            if (!Objects.equals(colorIndices[i], solution[i]) && solutionList.contains(colorIndices[i])) {
                int row = feedbackIndex / 2;
                int column = feedbackIndex % 2;
                feedbackGrid.add(new Circle(10, Color.WHITE), column, row);
                feedbackIndex++;
            }
        }
        guessGrid.add(feedbackGrid, 4, currentRow);
        if (score == 4) {
            showSolution();
            showAlert(Alert.AlertType.INFORMATION, "You won!", "Congratulations");
            return true;
        } else {
            return false;
        }
    }

    private void initCodeSettingLabel() {
        setCodeLabel();
        for (int i = 0; i < 4; i++) {
            Circle circle = new Circle(20);
            circle.setFill(Color.WHITE);
            circle.setId("1_" + i);
            circle.setOnMouseClicked(this::handleColorChangeMaker);
            if (player.getPlayerType().equals(PlayerType.SINGLE_PLAYER) || player.getPlayerRole().equals(PlayerRole.CODE_BREAKER)) {
                circle.setVisible(false);
                circle.setManaged(false);
            }
            solutionCircles.add(circle);
            guessGrid.add(circle, i, 1);
        }
        guessAndFeedbackLabel();
    }

    private void guessAndFeedbackLabel() {
        for (int i = 0; i < 4; i++) {
            Label guess = new Label("Guess");
            guess.setTextFill(Color.WHITE);
            guessGrid.add(guess, i, 2);
        }
        Label feedback = new Label("Feedback");
        feedback.setTextFill(Color.WHITE);
        guessGrid.add(feedback, 4, 2);
    }

    private void setCodeLabel() {
        final var label = new Label("Set the code");
        label.setTextFill(Color.WHITE);
        label.setPrefHeight(50);
        if (player.getPlayerType().equals(PlayerType.SINGLE_PLAYER) || player.getPlayerRole().equals(PlayerRole.CODE_BREAKER)) {
            label.setVisible(false);
            label.setManaged(false);
        }
        guessGrid.add(label, 4, 0);
    }

    private void addChildren() {
        currentRow++;
        final var circleArrayList = getChildrenCircles();
        this.guessCircles.put(currentRow, circleArrayList);
    }

    private ArrayList<Circle> getChildrenCircles() {
        final var circleArrayList = new ArrayList<Circle>();
        for (int i = 0; i < 4; i++) {
            Circle circle = new Circle(20);
            circle.setFill(Color.WHITE);
            circle.setId(currentRow + "_" + i);
            circle.setOnMouseClicked(this::handleColorChangeBreaker);
            guessGrid.add(circle, i, currentRow);
            if (player.getPlayerRole().equals(PlayerRole.CODE_MAKER)) {
                circle.setDisable(true);
            }
            circleArrayList.add(circle);
        }
        return circleArrayList;
    }

    private void randomizeSolution() {
        for (int i = 0; i < 4; i++) {
            solution[i] = ThreadLocalRandom.current().nextInt(6);
            Circle circle = new Circle(20);
            circle.setFill(colors[solution[i]]);
            solutionCircles.add(circle);
            lastColorIndex = solution[i];
            saveGameMoveAndUpdateGameState(i, 1);
            guessGrid.add(circle, i, 1);
            circle.setDisable(true);
            circle.setVisible(false);
            circle.setVisible(false);
        }

        GameMove gameMove = new GameMove();
        gameMove.setTries(tries);
        gameMove.setGameMoveType(GameMoveType.CODE_SAVE);
        gameMove.setLocalDateTime(LocalDateTime.now());
        gameMoves.add(gameMove);
        updateGameState(gameMove);
        log.info("Solution: {}", Arrays.toString(solution));
    }

    private void refreshChatTextArea() {
        List<String> chatHistory;
        try {
            chatHistory = stub.returnChatHistory();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        StringBuilder sb = new StringBuilder();

        for (String message : chatHistory) {
            sb.append(message);
            sb.append("\n");
        }

        chatTextArea.setText(sb.toString());
    }

    public void sendChatMessage() {
        String chatMessage = chatMessageTextField.getText();
        String playerName = player.getPlayerType().name();
        LocalDateTime now = LocalDateTime.now();

        try {
            stub.sendChatMessage(playerName + "(" + now.format(formatter) + "):" + chatMessage);
            refreshChatTextArea();
            chatMessageTextField.clear();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveCodeButtonVisibility() {
        if (player.getPlayerRole().equals(PlayerRole.CODE_MAKER)) {
            saveCodeButton.setVisible(true);
            saveCodeButton.setManaged(true);
            checkCodeButton.setVisible(false);
            checkCodeButton.setManaged(false);
        } else {
            saveCodeButton.setVisible(false);
            saveCodeButton.setManaged(false);
            checkCodeButton.setVisible(true);
            checkCodeButton.setManaged(true);
        }
    }

    public void showAlert(Alert.AlertType alertType, String message, String title) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(message);

        Stage primaryStage = MastermindApp.getPrimaryStage();
        alert.initOwner(primaryStage);
        alert.initModality(Modality.APPLICATION_MODAL);

        alert.setOnShown(event -> {
            Window window = alert.getDialogPane().getScene().getWindow();
            window.setX(primaryStage.getX() + primaryStage.getWidth() / 2 - window.getWidth() / 2);
            window.setY(primaryStage.getY() + primaryStage.getHeight() / 2 - window.getHeight() / 2);
        });
        alert.showAndWait();
    }

    public void replayGame() {
        if (!player.getPlayerType().equals(PlayerType.SINGLE_PLAYER)) {
            showAlert(Alert.AlertType.ERROR, "Only available in single player mode", ERROR);
            return;
        }
        List<GameMove> gameMovesList = XmlUtils.readGameMovesFromXml();
        AtomicInteger moveIndex = new AtomicInteger(0);
        resetValues();
        setCodeLabel();
        guessAndFeedbackLabel();
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
            if (moveIndex.get() < gameMovesList.size()) {
                GameMove gameMove = gameMovesList.get(moveIndex.getAndIncrement());
                processGameMove(gameMove);
            }
        }));
        timeline.setCycleCount(gameMovesList.size());
        timeline.playFromStart();
    }

    private void processGameMove(GameMove gameMove) {
        switch (gameMove.getGameMoveType()) {
            case COLOR_CHANGE:
                processColorChange(gameMove);
                break;
            case CODE_SAVE:
                processCodeSave();
                break;
            case CHECK_SOLUTION:
                processCheckSolution(gameMove);
                break;
            default:
                throw new IllegalArgumentException("Unknown game move type: " + gameMove.getGameMoveType());
        }
    }

    private void processColorChange(GameMove gameMove) {
        Circle circle = findOrCreateCircle(gameMove.getRow(), gameMove.getColumn());
        circle.setFill(colors[gameMove.getColorIndex()]);
        guessMap.putIfAbsent(gameMove.getRow(), new Integer[4]);
        guessMap.get(gameMove.getRow())[gameMove.getColumn()] = gameMove.getColorIndex();
    }

    private void processCodeSave() {
        if (guessMap.containsKey(1)) {
            solution = Arrays.copyOf(guessMap.get(1), 4);
        }
        currentRow++;
    }

    private void processCheckSolution(GameMove gameMove) {
        if (guessMap.containsKey(currentRow)) {
            colorIndices = Arrays.copyOf(guessMap.get(1), 4);
        }
        if (gameMove.getTries() == TRY_LIMIT) {
            showAlert(Alert.AlertType.ERROR, "You lost!", "Game over");
            return;
        }
        if (cipherSuccessfullyBroken(Arrays.asList(solution))) {
            return;
        }
        addChildren();
    }

    private Circle findOrCreateCircle(int row, int column) {
        Circle circle = (Circle) guessGrid.lookup("#" + row + "_" + column);
        if (circle == null) {
            circle = new Circle(20);
            circle.setId(row + "_" + column);
            if (row == 1) {
                circle.setVisible(false);
            }
            guessGrid.add(circle, column, row);
        }
        return circle;
    }
}
