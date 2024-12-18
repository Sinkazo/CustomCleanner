package org.example.customcleanner;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.example.customcleanner.model.FileItem;
import org.example.customcleanner.service.FileScanner;
import org.example.customcleanner.util.FileUtils;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloController {
    @FXML private VBox mainContainer;
    @FXML private Label welcomeText;
    @FXML private Button scanButton;
    @FXML private Button cleanButton;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private TableView<FileItem> fileTable;
    @FXML private Label totalSizeLabel;
    @FXML private Pane animationPane;
    @FXML private CheckBox tempCheck;
    @FXML private CheckBox prefetchCheck;
    @FXML private CheckBox downloadsCheck;
    @FXML private CheckBox recycleCheck;

    private ObservableList<FileItem> fileItems = FXCollections.observableArrayList();
    private Timeline scanAnimation;
    private List<Circle> backgroundCircles = new ArrayList<>();
    private final FileScanner fileScanner = new FileScanner();
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

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

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(10);
        mainContainer.setEffect(shadow);

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

        TableColumn<FileItem, String> typeCol = new TableColumn<>("Tipo");
        typeCol.setCellValueFactory(param -> param.getValue().typeProperty());

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
                setGraphic(empty ? null : openButton);
            }
        });

        fileTable.getColumns().addAll(selectCol, nameCol, pathCol, sizeCol, typeCol, actionCol);
        fileTable.setItems(fileItems);
        fileItems.addListener((javafx.collections.ListChangeListener<FileItem>) c -> updateTotalSize());
    }

    private void setupBackgroundAnimation() {
        animationPane.setMouseTransparent(true);

        for (int i = 0; i < 10; i++) {
            Circle circle = new Circle(5, Color.rgb(100, 200, 255, 0.2));
            backgroundCircles.add(circle);
            animationPane.getChildren().add(circle);

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(circle.layoutXProperty(), Math.random() * animationPane.getWidth()),
                            new KeyValue(circle.layoutYProperty(), Math.random() * animationPane.getHeight())
                    ),
                    new KeyFrame(Duration.seconds(10),
                            new KeyValue(circle.layoutXProperty(), Math.random() * animationPane.getWidth()),
                            new KeyValue(circle.layoutYProperty(), Math.random() * animationPane.getHeight())
                    )
            );
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();

            Timeline pulseTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(circle.radiusProperty(), 5)),
                    new KeyFrame(Duration.seconds(2), new KeyValue(circle.radiusProperty(), 10)),
                    new KeyFrame(Duration.seconds(4), new KeyValue(circle.radiusProperty(), 5))
            );
            pulseTimeline.setCycleCount(Timeline.INDEFINITE);
            pulseTimeline.play();
        }
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
        setupButtonAnimation(scanButton);
        setupButtonAnimation(cleanButton);
    }

    private void setupButtonAnimation(Button button) {
        button.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });

        button.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
            st.setToX(1);
            st.setToY(1);
            st.play();
        });
    }

    @FXML
    protected void onScanButtonClick() {
        fileItems.clear();
        scanButton.setDisable(true);
        cleanButton.setDisable(true);
        statusLabel.setText("Escaneando archivos...");
        progressBar.setProgress(0);

        List<CompletableFuture<List<FileItem>>> futures = new ArrayList<>();

        if (tempCheck.isSelected()) {
            futures.add(CompletableFuture.supplyAsync(() -> fileScanner.scanFiles("TEMP"), executorService));
        }
        if (prefetchCheck.isSelected()) {
            futures.add(CompletableFuture.supplyAsync(() -> fileScanner.scanFiles("PREFETCH"), executorService));
        }
        if (downloadsCheck.isSelected()) {
            futures.add(CompletableFuture.supplyAsync(() -> fileScanner.scanFiles("DOWNLOADS"), executorService));
        }
        if (recycleCheck.isSelected()) {
            futures.add(CompletableFuture.supplyAsync(() -> fileScanner.scanFiles("RECYCLE"), executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    List<FileItem> allFiles = new ArrayList<>();
                    futures.forEach(f -> {
                        try {
                            allFiles.addAll(f.get());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                    Platform.runLater(() -> {
                        fileItems.addAll(allFiles);
                        statusLabel.setText("Escaneo completado. Se encontraron " + allFiles.size() + " archivos.");
                        scanButton.setDisable(false);
                        cleanButton.setDisable(false);
                        progressBar.setProgress(1.0);
                        updateTotalSize();
                    });
                });
    }

    @FXML
    protected void onCleanButtonClick() {
        List<FileItem> selectedItems = new ArrayList<>();
        for (FileItem item : fileItems) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmar eliminación");
        confirmDialog.setHeaderText("¿Está seguro de que desea eliminar los archivos seleccionados?");
        confirmDialog.setContentText("Esta acción no se puede deshacer.");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                CompletableFuture.runAsync(() -> {
                    int deletedCount = 0;
                    for (FileItem item : selectedItems) {
                        File file = new File(item.getPath());
                        if (file.exists()) {
                            try {
                                if (file.delete()) {
                                    deletedCount++;
                                }
                            } catch (SecurityException e) {
                                Platform.runLater(() -> showError("No se pudo eliminar el archivo: " + file.getName()));
                            }
                        }
                    }

                    final int finalCount = deletedCount;
                    Platform.runLater(() -> {
                        statusLabel.setText("Se eliminaron " + finalCount + " archivos");
                        fileItems.removeAll(selectedItems);
                        updateTotalSize();
                    });
                }, executorService);
            }
        });
    }

    private void updateTotalSize() {
        double totalSize = fileItems.stream()
                .filter(FileItem::isSelected)
                .mapToDouble(item -> {
                    String sizeStr = item.getSize().replaceAll("[^0-9.]", "");
                    try {
                        return Double.parseDouble(sizeStr);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .sum();

        totalSizeLabel.setText("Tamaño total seleccionado: " + FileUtils.formatFileSize(totalSize));
        cleanButton.setDisable(fileItems.stream().noneMatch(FileItem::isSelected));
    }

    private void openFileLocation(File file) {
        try {
            Desktop.getDesktop().open(file.getParentFile());
        } catch (IOException e) {
            showError("No se pudo abrir la ubicación del archivo");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}