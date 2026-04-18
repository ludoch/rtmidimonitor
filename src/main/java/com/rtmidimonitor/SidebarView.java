package com.rtmidimonitor;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

public class SidebarView extends VBox {
    private boolean expanded = true;
    private final Button toggleButton = new Button("<<");
    private final VBox innerContent = new VBox(10);
    
    private VisualizationMode visualizationMode = VisualizationMode.BAR;
    private java.util.function.Consumer<VisualizationMode> onVisualizationModeChanged;
    private Runnable onResetAll;
    private Runnable onSimulate;
    private java.util.function.Consumer<Boolean> onFreezeChanged;
    private boolean frozen = false;
    private final AppSettings settings = AppSettings.getInstance();

    public SidebarView() {
        setPadding(new Insets(10));
        setSpacing(10);
        updateStyle();
        setMinWidth(250);
        setPrefWidth(250);
        
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        toggleButton.setOnAction(e -> toggle());
        
        getChildren().addAll(toggleButton, innerContent);
        VBox.setVgrow(innerContent, javafx.scene.layout.Priority.ALWAYS);
        
        setupContent();
        
        settings.themeProperty().addListener(e -> updateStyle());
    }

    private void updateStyle() {
        Theme theme = settings.getTheme();
        String sidebarHex = toHex(theme.sidebar);
        setStyle("-fx-background-color: " + sidebarHex + ";");
    }

    private String toHex(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X", 
            (int)(color.getRed() * 255), 
            (int)(color.getGreen() * 255), 
            (int)(color.getBlue() * 255));
    }

    public void addCustomContent(javafx.scene.Node node) {
        innerContent.getChildren().add(node);
    }

    public void setOnVisualizationModeChanged(java.util.function.Consumer<VisualizationMode> callback) {
        this.onVisualizationModeChanged = callback;
    }

    public void setOnResetAll(Runnable callback) {
        this.onResetAll = callback;
    }

    public void setOnSimulate(Runnable callback) {
        this.onSimulate = callback;
    }

    public void setOnFreezeChanged(java.util.function.Consumer<Boolean> callback) {
        this.onFreezeChanged = callback;
    }

    private void setupContent() {
        Button vizToggle = new Button("Mode: Bars");
        vizToggle.setMaxWidth(Double.MAX_VALUE);
        vizToggle.setOnAction(e -> {
            visualizationMode = (visualizationMode == VisualizationMode.BAR) ? VisualizationMode.GRAPH : VisualizationMode.BAR;
            vizToggle.setText("Mode: " + (visualizationMode == VisualizationMode.BAR ? "Bars" : "Graphs"));
            if (onVisualizationModeChanged != null) onVisualizationModeChanged.accept(visualizationMode);
        });

        Button freezeBtn = new Button("Freeze: OFF");
        freezeBtn.setMaxWidth(Double.MAX_VALUE);
        freezeBtn.setOnAction(e -> {
            frozen = !frozen;
            freezeBtn.setText("Freeze: " + (frozen ? "ON" : "OFF"));
            if (onFreezeChanged != null) onFreezeChanged.accept(frozen);
        });

        ComboBox<Theme> themeSelector = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(Theme.DARK, Theme.LIGHT));
        themeSelector.setMaxWidth(Double.MAX_VALUE);
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
        resetAllBtn.setMaxWidth(Double.MAX_VALUE);
        resetAllBtn.setOnAction(e -> {
            if (onResetAll != null) onResetAll.run();
        });

        Button simulateBtn = new Button("Simulate MIDI");
        simulateBtn.setMaxWidth(Double.MAX_VALUE);
        simulateBtn.setOnAction(e -> {
            if (onSimulate != null) onSimulate.run();
        });

        innerContent.getChildren().addAll(
            new Label("Visualization"),
            vizToggle,
            freezeBtn,
            new Label("Theme"),
            themeSelector,
            new Label("Formatting"),
            new HBox(5, new Label("Num:"), numFormatSelector),
            new HBox(5, new Label("Note:"), noteFormatSelector),
            new Label("Global Actions"),
            resetAllBtn,
            simulateBtn
        );
        
        applyLabelStyles();
        settings.themeProperty().addListener(e -> applyLabelStyles());
    }

    private void applyLabelStyles() {
        Theme theme = settings.getTheme();
        String labelHex = toHex(theme.label);
            
        innerContent.getChildren().forEach(node -> {
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
            setMinWidth(250);
            setPrefWidth(250);
            innerContent.setVisible(true);
            innerContent.setManaged(true);
        } else {
            toggleButton.setText(">>");
            setMinWidth(50);
            setPrefWidth(50);
            innerContent.setVisible(false);
            innerContent.setManaged(false);
        }
    }
}
