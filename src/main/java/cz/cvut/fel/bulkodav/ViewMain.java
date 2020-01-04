package cz.cvut.fel.bulkodav;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ViewMain extends Application
{

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        primaryStage.setResizable(false);
        primaryStage.setTitle("Ring chat");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.jpg")));
        Parent root = FXMLLoader.load(getClass().getResource("/fxmls/connect.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/error-text-field.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
