package com.rtmidimonitor;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SidebarView extends VBox {
    private boolean expanded = true;
    private final Button toggleButton = new Button("<<");
    private final VBox content = new VBox(10);
    private VisualizationMode visualizationMode = VisualizationMode.BAR;
    private java.util.function.Consumer<VisualizationMode> onVisualizationModeChanged;
    private Runnable onResetAll;
    private final AppSettings settings = AppSettings.getInstance();

    public SidebarView() {
        setPadding(new Insets(10));
        setSpacing(10);
        updateStyle();
        setPrefWidth(200);
        
        toggleButton.setOnAction(e -> toggle());
        
        getChildren().addAll(toggleButton, content);
        
        setupContent();
        
        settings.themeProperty().addListener(e -> updateStyle());
    }

    private void updateStyle() {
        Theme theme = settings.getTheme();
        String sidebarHex = String.format("#%02X%02X%02X", 
            (int)(theme.sidebar.getRed() * 255), 
            (int)(theme.sidebar.getGreen() * 255), 
            (int)(theme.sidebar.getBlue() * 255));
        setStyle("-fx-background-color: " + sidebarHex + ";");
    }

    public void addCustomContent(javafx.scene.Node node) {
        content.getChildren().add(node);
    }

    public void setOnVisualizationModeChanged(java.util.function.Consumer<VisualizationMode> callback) {
        this.onVisualizationModeChanged = callback;
    }

    public void setOnResetAll(Runnable callback) {
        this.onResetAll = callback;
    }

    private void setupContent() {
        Button vizToggle = new Button("Mode: Bars");
        vizToggle.setOnAction(e -> {
            visualizationMode = (visualizationMode == VisualizationMode.BAR) ? VisualizationMode.GRAPH : VisualizationMode.BAR;
            vizToggle.setText("Mode: " + (visualizationMode == VisualizationMode.BAR ? "Bars" : "Graphs"));
            if (onVisualizationModeChanged != null) onVisualizationModeChanged.accept(visualizationMode);
        });

        ComboBox<Theme> themeSelector = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(Theme.DARK, Theme.LIGHT));
        themeSelector.setValue(settings.getTheme());
        themeSelector.setCellFactory(lv -> new ListCell<Theme>() {
            @Override protected void updateItem(Theme item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.name);
            }
        });
        themeSelector.setButtonCell(new ListCell<Theme>() {
            @Override protected void updateItem(Theme item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.name);
            }
        });
        themeSelector.setOnAction(e -> settings.setTheme(themeSelector.getValue()));

        ComboBox<AppSettings.NumberFormat> numFormatSelector = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(AppSettings.NumberFormat.values()));
        numFormatSelector.setValue(settings.getNumberFormat());
        numFormatSelector.setOnAction(e -> settings.setNumberFormat(numFormatSelector.getValue()));

        ComboBox<AppSettings.NoteFormat> noteFormatSelector = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(AppSettings.NoteFormat.values()));
        noteFormatSelector.setValue(settings.getNoteFormat());
        noteFormatSelector.setOnAction(e -> settings.setNoteFormat(noteFormatSelector.getValue()));

        Button resetAllBtn = new Button("Reset All");
        resetAllBtn.setOnAction(e -> {
            if (onResetAll != null) onResetAll.run();
        });

        content.getChildren().addAll(
            new Label("Visualization"),
            vizToggle,
            new Label("Theme"),
            themeSelector,
            new Label("Formatting"),
            new HBox(5, new Label("Num:"), numFormatSelector),
            new HBox(5, new Label("Note:"), noteFormatSelector),
            new Label("Global Actions"),
            resetAllBtn
        );
        
        applyLabelStyles();
        settings.themeProperty().addListener(e -> applyLabelStyles());
    }

    private void applyLabelStyles() {
        Theme theme = settings.getTheme();
        String labelHex = String.format("#%02X%02X%02X", 
            (int)(theme.label.getRed() * 255), 
            (int)(theme.label.getGreen() * 255), 
            (int)(theme.label.getBlue() * 255));
            
        content.getChildren().forEach(node -> {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: " + labelHex + "; -fx-font-weight: bold;");
            } else if (node instanceof HBox hbox) {
                hbox.getChildren().forEach(hn -> {
                    if (hn instanceof Label) hn.setStyle("-fx-text-fill: " + labelHex + ";");
                });
            }
        });
    }

    private void toggle() {
        expanded = !expanded;
        if (expanded) {
            toggleButton.setText("<<");
            setPrefWidth(200);
            content.setVisible(true);
            content.setManaged(true);
        } else {
            toggleButton.setText(">>");
            setPrefWidth(50);
            content.setVisible(false);
            content.setManaged(false);
        }
    }
}
