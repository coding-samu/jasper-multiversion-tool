package it.jmt.launcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class LauncherMain extends Application {

    private Label globalStatusLabel;
    private ComboBox<String> versionCombo;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Jasper Multi-Version Utility");

        globalStatusLabel = new Label("Seleziona un'operazione dalle schede.");
        globalStatusLabel.setStyle("-fx-text-fill: #444; -fx-font-weight: bold;");

        versionCombo = new ComboBox<>(FXCollections.observableArrayList("6.1.1", "6.21.3"));
        versionCombo.getSelectionModel().selectFirst();

        HBox versionBox = new HBox(10, new Label("Versione JasperReports:"), versionCombo);
        versionBox.setAlignment(Pos.CENTER_LEFT);
        versionBox.setPadding(new Insets(0, 0, 10, 0));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabDecompile = new Tab("Jasper → JRXML", createDecompileView(stage));
        Tab tabCompile = new Tab("JRXML → Jasper", createCompileView(stage));
        tabPane.getTabs().addAll(tabDecompile, tabCompile);

        VBox root = new VBox(15, versionBox, tabPane, globalStatusLabel);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 14px;");

        Scene scene = new Scene(root, 600, 350);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createDecompileView(Stage stage) {
        TextField sourceField = new TextField();
        TextField destDirField = new TextField();
        TextField fileNameField = new TextField("report_estratto");

        Button browseSourceBtn = new Button("Sfoglia...");
        Button browseDirBtn = new Button("Sfoglia...");
        Button actionButton = new Button("Estrai JRXML");

        browseSourceBtn.setOnAction(e -> {
            File selected = chooseFile(stage, "Seleziona file .jasper", "*.jasper");
            if (selected != null) sourceField.setText(selected.getAbsolutePath());
        });

        browseDirBtn.setOnAction(e -> {
            File selectedDir = chooseDirectory(stage);
            if (selectedDir != null) destDirField.setText(selectedDir.getAbsolutePath());
        });

        actionButton.setStyle("-fx-background-color: #0078D7; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
        actionButton.setOnAction(e -> {
            if (validateInputs(sourceField, destDirField, fileNameField)) {
                String destPath = destDirField.getText() + File.separator + fileNameField.getText() + ".jrxml";
                executeWorker("decompile", sourceField.getText(), destPath);
            }
        });

        return buildGrid(sourceField, browseSourceBtn, destDirField, browseDirBtn, fileNameField, actionButton, "File .jasper:");
    }

    private VBox createCompileView(Stage stage) {
        TextField sourceField = new TextField();
        TextField destDirField = new TextField();
        TextField fileNameField = new TextField("report_compilato");

        Button browseSourceBtn = new Button("Sfoglia...");
        Button browseDirBtn = new Button("Sfoglia...");
        Button actionButton = new Button("Compila Jasper");

        browseSourceBtn.setOnAction(e -> {
            File selected = chooseFile(stage, "Seleziona file .jrxml", "*.jrxml");
            if (selected != null) sourceField.setText(selected.getAbsolutePath());
        });

        browseDirBtn.setOnAction(e -> {
            File selectedDir = chooseDirectory(stage);
            if (selectedDir != null) destDirField.setText(selectedDir.getAbsolutePath());
        });

        actionButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
        actionButton.setOnAction(e -> {
            if (validateInputs(sourceField, destDirField, fileNameField)) {
                String destPath = destDirField.getText() + File.separator + fileNameField.getText() + ".jasper";
                executeWorker("compile", sourceField.getText(), destPath);
            }
        });

        return buildGrid(sourceField, browseSourceBtn, destDirField, browseDirBtn, fileNameField, actionButton, "File .jrxml:");
    }

    private void executeWorker(String action, String sourcePath, String destPath) {
        String selectedVersion = versionCombo.getValue();
        globalStatusLabel.setText("⏳ Operazione in corso con versione " + selectedVersion + "...");
        globalStatusLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");

        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

        String jarName = selectedVersion.equals("6.1.1") ? "worker-jr-611-fat.jar" : "worker-jr-621-fat.jar";

        String workerPath = Paths.get("workers", jarName).toString();

        File workerFile = new File(workerPath);
        if (!workerFile.exists()) {
            globalStatusLabel.setText("❌ Errore: File worker non trovato in: " + workerFile.getAbsolutePath());
            globalStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return;
        }

        ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", workerFile.getAbsolutePath(), action, sourcePath, destPath);
        builder.redirectErrorStream(true);

        new Thread(() -> {
            try {
                Process process = builder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                boolean success = false;
                StringBuilder errorLog = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    if (line.contains("SUCCESS")) {
                        success = true;
                    } else if (line.contains("ERROR") || line.contains("FATAL") || line.contains("Exception")) {
                        errorLog.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                final boolean finalSuccess = success && exitCode == 0;
                final String finalError = errorLog.toString();

                Platform.runLater(() -> {
                    if (finalSuccess) {
                        globalStatusLabel.setText("✅ Operazione completata con successo!");
                        globalStatusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else {
                        globalStatusLabel.setText("❌ Errore (Exit Code " + exitCode + ")");
                        globalStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        showAlert(Alert.AlertType.ERROR, "Errore di esecuzione", "Dettagli:\n" + finalError);
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    globalStatusLabel.setText("❌ Impossibile lanciare il processo.");
                    globalStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    showAlert(Alert.AlertType.ERROR, "Errore", ex.getMessage());
                });
            }
        }).start();
    }

    private VBox buildGrid(TextField sourceField, Button srcBtn, TextField dirField, Button dirBtn, TextField nameField, Button actionBtn, String sourceLabel) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.add(new Label(sourceLabel), 0, 0);
        grid.add(sourceField, 1, 0);
        grid.add(srcBtn, 2, 0);
        grid.add(new Label("Cartella destinazione:"), 0, 1);
        grid.add(dirField, 1, 1);
        grid.add(dirBtn, 2, 1);
        grid.add(new Label("Nome file output:"), 0, 2);
        grid.add(nameField, 1, 2);

        VBox box = new VBox(20, grid, actionBtn);
        box.setPadding(new Insets(20, 0, 0, 0));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private File chooseFile(Stage stage, String title, String ext) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("File Jasper", ext));
        return chooser.showOpenDialog(stage);
    }

    private File chooseDirectory(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleziona cartella di destinazione");
        return chooser.showDialog(stage);
    }

    private boolean validateInputs(TextField... fields) {
        for (TextField f : fields) {
            if (f.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Errore", "Compila tutti i campi prima di procedere.");
                return false;
            }
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}
