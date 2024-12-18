package org.example.customcleanner;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class HelloController {
    @FXML
    private VBox mainContainer;
    @FXML
    private Label welcomeText;
    @FXML
    private Button scanButton;
    @FXML
    private Button cleanButton;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TableView<FileItem> fileTable;
    @FXML
    private Label totalSizeLabel;
    @FXML
    private Pane animationPane;

    private ObservableList<FileItem> fileItems = FXCollections.observableArrayList();
    private Timeline scanAnimation;
    private List<Circle> backgroundCircles = new ArrayList<>();

    @FXML
    public void initialize() {
        setupUI();
        setupTable();
        setupAnimations();
        setupBackgroundAnimation();
    }

    private void setupUI() {
        welcomeText.setText("Sistema Avanzado de Limpieza");
        cleanButton.setDisable(true);
        progressBar.setProgress(0);
        statusLabel.setText("Listo para iniciar");

        // Aplicar efectos visuales
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(10);
        mainContainer.setEffect(shadow);

        // Estilo para los botones
        String buttonStyle = "-fx-background-radius: 20; -fx-padding: 10 20;";
        scanButton.setStyle(scanButton.getStyle() + buttonStyle);
        cleanButton.setStyle(cleanButton.getStyle() + buttonStyle);
    }

    private void setupTable() {
        TableColumn<FileItem, Boolean> selectCol = new TableColumn<>("Seleccionar");
        selectCol.setCellValueFactory(param -> param.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));

        TableColumn<FileItem, String> nameCol = new TableColumn<>("Nombre");
        nameCol.setCellValueFactory(param -> param.getValue().nameProperty());

        TableColumn<FileItem, String> pathCol = new TableColumn<>("Ubicación");
        pathCol.setCellValueFactory(param -> param.getValue().pathProperty());

        TableColumn<FileItem, String> sizeCol = new TableColumn<>("Tamaño");
        sizeCol.setCellValueFactory(param -> param.getValue().sizeProperty());

        TableColumn<FileItem, Void> actionCol = new TableColumn<>("Acciones");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button openButton = new Button("Abrir ubicación");

            {
                openButton.setOnAction(event -> {
                    FileItem item = getTableRow().getItem();
                    if (item != null) {
                        openFileLocation(new File(item.getPath()));
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(openButton);
                }
            }
        });

        fileTable.getColumns().addAll(selectCol, nameCol, pathCol, sizeCol, actionCol);
        fileTable.setItems(fileItems);

        // Actualizar el total cuando cambia la selección
        fileItems.addListener((javafx.collections.ListChangeListener<FileItem>) c -> updateTotalSize());
    }

    private void setupBackgroundAnimation() {
        animationPane.setMouseTransparent(true);

        for (int i = 0; i < 10; i++) {
            Circle circle = new Circle(5, Color.rgb(100, 200, 255, 0.2));
            backgroundCircles.add(circle);
            animationPane.getChildren().add(circle);

            // Animación de movimiento
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(circle.layoutXProperty(), Math.random() * animationPane.getWidth())),
                    new KeyFrame(Duration.ZERO, new KeyValue(circle.layoutYProperty(), Math.random() * animationPane.getHeight())),
                    new KeyFrame(Duration.seconds(10), new KeyValue(circle.layoutXProperty(), Math.random() * animationPane.getWidth())),
                    new KeyFrame(Duration.seconds(10), new KeyValue(circle.layoutYProperty(), Math.random() * animationPane.getHeight()))
            );
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();

            // Animación de tamaño
            Timeline pulseTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(circle.radiusProperty(), 5)),
                    new KeyFrame(Duration.seconds(2), new KeyValue(circle.radiusProperty(), 10)),
                    new KeyFrame(Duration.seconds(4), new KeyValue(circle.radiusProperty(), 5))
            );
            pulseTimeline.setCycleCount(Timeline.INDEFINITE);
            pulseTimeline.play();
        }
    }

    @FXML
    protected void onScanButtonClick() {
        fileItems.clear();
        scanButton.setDisable(true);
        cleanButton.setDisable(true);
        statusLabel.setText("Escaneando archivos...");
        progressBar.setProgress(0);

        Thread scanThread = new Thread(this::scanFiles);
        scanThread.start();
    }

    private void scanFiles() {
        List<File> files = findJunkFiles();
        double totalSize = 0;

        for (File file : files) {
            FileItem item = new FileItem(
                    file.getName(),
                    file.getAbsolutePath(),
                    formatFileSize(file.length()),
                    true
            );
            Platform.runLater(() -> fileItems.add(item));
            totalSize += file.length();
        }

        final double finalTotalSize = totalSize;
        Platform.runLater(() -> {
            statusLabel.setText("Escaneo completado. Se encontraron " + files.size() + " archivos.");
            totalSizeLabel.setText("Tamaño total: " + formatFileSize(finalTotalSize));
            scanButton.setDisable(false);
            cleanButton.setDisable(false);
            progressBar.setProgress(1.0);
        });
    }

    @FXML
    protected void onCleanButtonClick() {
        List<File> filesToDelete = new ArrayList<>();
        for (FileItem item : fileItems) {
            if (item.isSelected()) {
                filesToDelete.add(new File(item.getPath()));
            }
        }

        Thread cleanThread = new Thread(() -> {
            int deletedCount = 0;
            for (File file : filesToDelete) {
                if (file.delete()) {
                    deletedCount++;
                }
            }

            final int finalCount = deletedCount;
            Platform.runLater(() -> {
                statusLabel.setText("Se eliminaron " + finalCount + " archivos");
                fileItems.removeIf(FileItem::isSelected);
                updateTotalSize();
            });
        });
        cleanThread.start();
    }

    private void updateTotalSize() {
        double totalSize = 0;
        for (FileItem item : fileItems) {
            if (item.isSelected()) {
                String sizeStr = item.getSize().replaceAll("[^0-9.]", "");
                try {
                    totalSize += Double.parseDouble(sizeStr);
                } catch (NumberFormatException e) {
                    // Ignorar errores de parseo
                }
            }
        }
        totalSizeLabel.setText("Tamaño total seleccionado: " + formatFileSize(totalSize));
    }

    private void openFileLocation(File file) {
        try {
            Desktop.getDesktop().open(file.getParentFile());
        } catch (IOException e) {
            showError("No se pudo abrir la ubicación del archivo");
        }
    }

    private String formatFileSize(double size) {
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(size) + " " + units[unitIndex];
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupAnimations() {
        scanAnimation = new Timeline(
                new KeyFrame(Duration.seconds(0.05), e -> {
                    if (progressBar != null) {
                        double progress = progressBar.getProgress();
                        if (progress < 1.0) {
                            progressBar.setProgress(progress + 0.01);
                        } else {
                            scanAnimation.stop();
                        }
                    }
                })
        );
        scanAnimation.setCycleCount(Timeline.INDEFINITE);

        // Animación para los botones
        scanButton.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), scanButton);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });

        scanButton.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), scanButton);
            st.setToX(1);
            st.setToY(1);
            st.play();
        });

        cleanButton.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), cleanButton);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });

        cleanButton.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), cleanButton);
            st.setToX(1);
            st.setToY(1);
            st.play();
        });
    }

    private List<File> findJunkFiles() {
        List<File> junkFiles = new ArrayList<>();

        // Directorios comunes donde buscar archivos temporales
        String[] locations = {
                System.getProperty("java.io.tmpdir"),
                System.getProperty("user.home") + "/AppData/Local/Temp",
                System.getProperty("user.home") + "/Downloads",
                System.getProperty("user.home") + "/Documents",
                "C:/Windows/Temp"
        };

        for (String location : locations) {
            File directory = new File(location);
            if (directory.exists()) {
                searchJunkFilesInDirectory(directory, junkFiles);
            }
        }

        return junkFiles;
    }

    private void searchJunkFilesInDirectory(File directory, List<File> junkFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isJunkFile(file)) {
                    junkFiles.add(file);
                }
            }
        }
    }

    private boolean isJunkFile(File file) {
        String name = file.getName().toLowerCase();
        // Extensiones comunes de archivos temporales o de respaldo
        String[] junkExtensions = {
                ".tmp", ".temp", ".log", ".old", ".bak",
                ".chk", ".gid", ".prv", ".~mp", ".dmp",
                ".part", ".crdownload", ".download"
        };

        for (String ext : junkExtensions) {
            if (name.endsWith(ext)) {
                return true;
            }
        }

        // Patrones comunes de archivos temporales
        return name.startsWith("~$") || // Archivos temporales de Office
                name.matches("^[~].*") || // Archivos que empiezan con ~
                name.matches(".*[.][0-9]+$") || // Archivos que terminan en números
                name.matches("^[0-9a-f]{8}$"); // Archivos con nombres hexadecimales
    }
}