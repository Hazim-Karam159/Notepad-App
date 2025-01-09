import javafx.application.Application;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;

import java.io.*;
import java.util.Optional;
import java.util.Timer;
import      java.util.TimerTask;

public class NotepadApp extends Application {

    private boolean isModified = false;
    private Label statusBar;
    private int fontSize = 14;
    private boolean isDarkMode = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
	 primaryStage.getIcons().add(new Image("icon.png"));
        BorderPane root = new BorderPane();

        TextArea textArea = new TextArea();
        textArea.getStyleClass().add("text-area");
        root.setCenter(textArea);

        statusBar = new Label("Line: 1, Column: 1 | Words: 0, Characters: 0");
        HBox statusBox = new HBox(statusBar);
        root.setBottom(statusBox);

        Menu fileMenu = new Menu("File");
        MenuItem newFile = new MenuItem("New");
        newFile.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
        MenuItem openFile = new MenuItem("Open");
        openFile.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        MenuItem saveFile = new MenuItem("Save");
        saveFile.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        MenuItem exitApp = new MenuItem("Exit");
        exitApp.setAccelerator(KeyCombination.keyCombination("Ctrl+Q"));

        newFile.setOnAction(e -> {
            if (confirmExit(textArea)) {
                textArea.clear();
                isModified = false;
            }
        });
        openFile.setOnAction(e -> {
            if (confirmExit(textArea)) {
                openFile(textArea);
                isModified = false;
            }
        });
        saveFile.setOnAction(e -> saveFile(textArea));

        exitApp.setOnAction(e -> {
            if (confirmExit(textArea)) {
                System.exit(0);
            }
        });

        Menu editMenu = new Menu("Edit");
        MenuItem undoText = new MenuItem("Undo");
        undoText.setAccelerator(KeyCombination.keyCombination("Ctrl+Z"));
        MenuItem cutText = new MenuItem("Cut");
        cutText.setAccelerator(KeyCombination.keyCombination("Ctrl+X"));
        MenuItem copyText = new MenuItem("Copy");
        copyText.setAccelerator(KeyCombination.keyCombination("Ctrl+C"));
        MenuItem pasteText = new MenuItem("Paste");
        pasteText.setAccelerator(KeyCombination.keyCombination("Ctrl+V"));
        MenuItem deleteText = new MenuItem("Delete");
        MenuItem selectAllText = new MenuItem("Select All");
        selectAllText.setAccelerator(KeyCombination.keyCombination("Ctrl+A"));

        undoText.setOnAction(e -> textArea.undo());
        cutText.setOnAction(e -> textArea.cut());
        copyText.setOnAction(e -> textArea.copy());
        pasteText.setOnAction(e -> textArea.paste());
        deleteText.setOnAction(e -> textArea.deleteText(textArea.getSelection()));
        selectAllText.setOnAction(e -> textArea.selectAll());

        Menu viewMenu = new Menu("View");
        MenuItem increaseFont = new MenuItem("Increase Font");
        MenuItem decreaseFont = new MenuItem("Decrease Font");

        increaseFont.setOnAction(e -> {
            fontSize += 4;
            textArea.setStyle("-fx-font-size: " + fontSize + "px;");
        });

        decreaseFont.setOnAction(e -> {
            fontSize -= 2;
            textArea.setStyle("-fx-font-size: " + fontSize + "px;");
        });

        viewMenu.getItems().addAll(increaseFont, decreaseFont);

        Menu searchMenu = new Menu("Search");
        MenuItem findReplace = new MenuItem("Find and Replace");

        findReplace.setOnAction(e -> findAndReplace(textArea));
        searchMenu.getItems().add(findReplace);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutMenu = new MenuItem("About");

        aboutMenu.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutMenu);

        MenuBar menuBar = new MenuBar();
        fileMenu.getItems().addAll(newFile, openFile, saveFile, exitApp);
        editMenu.getItems().addAll(undoText, cutText, copyText, pasteText, deleteText, selectAllText);
        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, searchMenu, helpMenu);
        root.setTop(menuBar);

        Button toggleThemeButton = new Button("Switch to Dark Mode");
        toggleThemeButton.setOnAction(e -> {
            if (isDarkMode) {
                root.getStylesheets().remove("dark-theme.css");
                toggleThemeButton.setText("Switch to Dark Mode");
                isDarkMode = false;
            } else {
                root.getStylesheets().add("dark-theme.css");
                toggleThemeButton.setText("Switch to Light Mode");
                isDarkMode = true;
            }
        });

        VBox themeBox = new VBox(toggleThemeButton);
        themeBox.setSpacing(10);
        root.setLeft(themeBox);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add("styles.css");
        primaryStage.setScene(scene);
        primaryStage.setTitle("🎉 Notepad Application 🎉");
        primaryStage.show();

        textArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> updateStatusBar(textArea));
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            updateStatusBar(textArea);
            isModified = true;
        });

        startAutoSaveReminder(textArea);
    }

    private void updateStatusBar(TextArea textArea) {
        int caret = textArea.getCaretPosition();
        String[] lines = textArea.getText().substring(0, caret).split("\n");
        int line = lines.length;
        int column = lines[lines.length - 1].length() + 1;

        String text = textArea.getText();
        int wordCount = text.isBlank() ? 0 : text.split("\\s+").length;
        int charCount = text.length();

        statusBar.setText("Line: " + line + ", Column: " + column + " | Words: " + wordCount + ", Characters: " + charCount);
    }

    private void findAndReplace(TextArea textArea) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Find and Replace");

        TextField findField = new TextField();
        TextField replaceField = new TextField();
        findField.setPromptText("Find");
        replaceField.setPromptText("Replace");

        dialog.getDialogPane().setContent(new VBox(10, findField, replaceField));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String text = textArea.getText();
                textArea.setText(text.replace(findField.getText(), replaceField.getText()));
            }
        });
    }

    private void startAutoSaveReminder(TextArea textArea) {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isModified) {
                    System.out.println("Reminder: You have unsaved changes!");
                }
            }
        }, 300000, 300000);
    }

    private boolean confirmExit(TextArea textArea) {
        if (isModified) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes.");
            alert.setContentText("Do you want to save them before exiting?");

            ButtonType saveButton = new ButtonType("Save");
            ButtonType discardButton = new ButtonType("Don't Save");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == saveButton) {
                saveFile(textArea);
                return true;
            } else if (result.get() == discardButton) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private void openFile(TextArea textArea) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                textArea.clear();
                String line;
                while ((line = reader.readLine()) != null) {
                    textArea.appendText(line + "\n");
                }
                isModified = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFile(TextArea textArea) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textArea.getText());
                isModified = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Notepad");
        alert.setHeaderText(null);
        alert.setContentText(""" 
		Hello LinkedIn Connections 🙌
                This is Notepad Application v1.0
                Developed with JavaFX ZoOoMA Team
                Enjoy editing your text files!
                """);
        alert.showAndWait();
    }
}


// javac --module-path C:\DevTools\openjfx-23.0.1_windows-x64_bin-sdk\javafx-sdk-23.0.1\lib --add-modules javafx.controls NotepadApp.java

// java --module-path C:\DevTools\openjfx-23.0.1_windows-x64_bin-sdk\javafx-sdk-23.0.1\lib --add-modules javafx.controls NotepadApp