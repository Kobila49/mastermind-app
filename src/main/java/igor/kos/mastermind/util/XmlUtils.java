package igor.kos.mastermind.util;

import igor.kos.mastermind.model.GameMove;
import igor.kos.mastermind.model.GameMoveType;
import lombok.extern.log4j.Log4j2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class XmlUtils {

    private static final String GAME_MOVES_XML_FILE_NAME = "src/main/resources/xml/gameMoves.xml";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private XmlUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void saveGameMovesToXml(List<GameMove> gameMoves) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("GameMoves");
            doc.appendChild(rootElement);

            for (GameMove gameMove : gameMoves) {
                Element gameMoveElement = doc.createElement("GameMove");

                Element colorIndexElement = doc.createElement("ColorIndex");
                colorIndexElement.setTextContent(gameMove.getColorIndex() != null ? gameMove.getColorIndex().toString() : null);
                gameMoveElement.appendChild(colorIndexElement);

                Element rowElement = doc.createElement("Row");
                rowElement.setTextContent(gameMove.getRow() != null ? gameMove.getRow().toString() : null);
                gameMoveElement.appendChild(rowElement);

                Element columnElement = doc.createElement("Column");
                columnElement.setTextContent(gameMove.getColumn() != null ? gameMove.getColumn().toString() : null);
                gameMoveElement.appendChild(columnElement);

                Element gameMoveTypeElement = doc.createElement("GameMoveType");
                gameMoveTypeElement.setTextContent(gameMove.getGameMoveType().toString());
                gameMoveElement.appendChild(gameMoveTypeElement);

                Element triesElement = doc.createElement("Tries");
                triesElement.setTextContent(gameMove.getTries().toString());
                gameMoveElement.appendChild(triesElement);

                Element localDateTimeElement = doc.createElement("LocalDateTime");
                localDateTimeElement.setTextContent(gameMove.getLocalDateTime().format(dateTimeFormatter));
                gameMoveElement.appendChild(localDateTimeElement);

                rootElement.appendChild(gameMoveElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);

            // Ensure the directories exist
            Path outputPath = Paths.get(GAME_MOVES_XML_FILE_NAME);
            if (!Files.exists(outputPath.getParent())) {
                Files.createDirectories(outputPath.getParent());
            }

            // Write the document to the file
            try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                StreamResult result = new StreamResult(writer);
                transformer.transform(source, result);
            }
        } catch (ParserConfigurationException | IOException | TransformerException e) {
            throw new RuntimeException("Error saving game moves to XML", e);
        }
    }

    public static List<GameMove> readGameMovesFromXml() {
        List<GameMove> gameMoves = new ArrayList<>();

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom;

            // Read from the file in the resources directory
            Path filePath = Paths.get(GAME_MOVES_XML_FILE_NAME);
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("File not found: " + GAME_MOVES_XML_FILE_NAME);
            }

            try (InputStream inputStream = Files.newInputStream(filePath)) {
                dom = db.parse(inputStream);
            }

            Element gameMoveElement = dom.getDocumentElement();
            NodeList nl = gameMoveElement.getChildNodes();
            int length = nl.getLength();
            for (int i = 0; i < length; i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) nl.item(i);
                    if (el.getNodeName().contains("GameMove")) {
                        Integer colorIndex = parseInteger(el, "ColorIndex");
                        Integer column = parseInteger(el, "Column");
                        Integer row = parseInteger(el, "Row");
                        GameMoveType gameMoveType = GameMoveType.valueOf(el.getElementsByTagName("GameMoveType").item(0).getTextContent());
                        Integer tries = parseInteger(el, "Tries");
                        LocalDateTime localDateTime = LocalDateTime.parse(
                                el.getElementsByTagName("LocalDateTime").item(0).getTextContent(), dateTimeFormatter);

                        GameMove gameMove = new GameMove();
                        gameMove.setColorIndex(colorIndex);
                        gameMove.setColumn(column);
                        gameMove.setRow(row);
                        gameMove.setGameMoveType(gameMoveType);
                        gameMove.setTries(tries);
                        gameMove.setLocalDateTime(localDateTime);

                        gameMoves.add(gameMove);
                    }
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException ex) {
            throw new RuntimeException("Error reading game moves from XML", ex);
        }

        return gameMoves;
    }

    private static Integer parseInteger(Element element, String tagName) {
        String textContent = element.getElementsByTagName(tagName).item(0).getTextContent();
        return textContent.isEmpty() ? null : Integer.parseInt(textContent);
    }

}
