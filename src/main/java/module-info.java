module src.drimjavafxproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.xml.crypto;

    // ✅ exported packages (so other modules can use them)
    exports files;
    exports files.Classes;
    exports files.Controllers;
    exports files.Server;     // ✅ only once

    // ✅ FXMLLoader reflection (controllers)
    opens files to javafx.fxml;
    opens files.Controllers to javafx.fxml;

    // ✅ JavaFX PropertyValueFactory reflection (models in TableView)
    opens files.Classes to javafx.base;

    // ✅ only needed if you ever load server classes via FXML (usually not)
    // opens files.Server to javafx.fxml;
}
