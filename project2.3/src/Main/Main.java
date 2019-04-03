package Main;

import Controller.SocketController;
import Model.Config;
import View.StartView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	
	private static Stage primaryReference = null;
	public enum SceneType { START, GAME }
	
	@Override
	public void start(Stage primaryStage) throws Exception {		
		StartView menu = new StartView(primaryStage);
		Scene scene = new Scene(menu);
		
		primaryStage.setTitle(Config.APP_NAME); // Set the stage title
		primaryStage.setScene(scene); // Place the scene in the stage
		primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
		primaryStage.show(); // Display the stage
		
		primaryReference = primaryStage;
		
		SocketController.getInstance(true);
	}
	
	public static void switchScene(SceneType scenetype) {
		// TODO: switch from scene
		switch (scenetype) {
		case START:
			System.out.println("----> start view");
			StartView start = new StartView(primaryReference);
			primaryReference.getScene().setRoot(start);
			break;
		case GAME:
			System.out.println("----> game view");

			break;
		}
	}

	public static void main(String[] args) throws Exception {
		launch(args);
	}
	
	public static void QuitApp() {
		System.out.println("Quiting application. Bye...");
		Platform.exit();
		System.exit(0);
	}
}
