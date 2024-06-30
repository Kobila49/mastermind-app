module igor.kos.mastermind {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires org.kordamp.bootstrapfx.core;
    requires java.rmi;
    requires java.naming;
    requires java.xml;
    requires org.apache.logging.log4j;
    opens igor.kos.mastermind to javafx.fxml;
    exports igor.kos.mastermind;
    opens igor.kos.mastermind.chat to java.rmi;
    exports igor.kos.mastermind.model;
    opens igor.kos.mastermind.model to javafx.fxml;
}
