package mx.edu.utch.proyectofinal1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class    App extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        Database.init();

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/mx/edu/utch/proyectofinal1/GameView.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Simón dice");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
