package agh.bedbooker;

import agh.bedbooker.client.ClientView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class HelloController {

    @FXML
    private VBox rootVBox;

    @FXML
    protected void onClientButtonClick() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setWidth(500);
        dialog.setTitle("Klient");
        dialog.setHeaderText("Wpisz email");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(email -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("client/client-personal-data-view.fxml"));
                Parent root = fxmlLoader.load();
                ClientView controller = fxmlLoader.getController();
                controller.setEmail(email);

                Stage stage = new Stage();
                stage.setTitle("Widok klienta");
                Scene scene = new Scene(root, 960, 640);
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    protected void onAdminButtonClick() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Admin");
        dialog.setHeaderText("Zaloguj się do panelu admina");
        dialog.setContentText("Hasło:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(password -> {
            if (password.equals("admin")) {
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("admin/admin-statistic-view.fxml"));
                    Parent root = fxmlLoader.load();


                    Stage stage = new Stage();
                    stage.setTitle("Witaj Admin!");
                    stage.initModality(Modality.APPLICATION_MODAL);
                    Scene scene = new Scene(root, 960, 640);
                    stage.setScene(scene);

                    stage.centerOnScreen();

                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Nieprawidłowe hasło", ButtonType.OK);
                alert.showAndWait();
            }
        });
    }
}
