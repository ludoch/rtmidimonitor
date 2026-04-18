package com.rtmidimonitor;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SidebarView extends VBox {
    private boolean expanded = true;
    private final Button toggleButton = new Button("<<");
    private final VBox content = new VBox(10);
    private VisualizationMode visualizationMode = VisualizationMode.BAR;
    private java.util.function.Consumer<VisualizationMode> onVisualizationModeChanged;

    public SidebarView() {
        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-background-color: #3c3f41;");
        setPrefWidth(200);
        
        toggleButton.setOnAction(e -> toggle());
        
        getChildren().addAll(toggleButton, content);
        
        setupContent();
    }

    public void addCustomContent(javafx.scene.Node node) {
        content.getChildren().add(node);
    }

    public void setOnVisualizationModeChanged(java.util.function.Consumer<VisualizationMode> callback) {
        this.onVisualizationModeChanged = callback;
    }

    private void setupContent() {
        Button vizToggle = new Button("Mode: Bars");
        vizToggle.setOnAction(e -> {
            visualizationMode = (visualizationMode == VisualizationMode.BAR) ? VisualizationMode.GRAPH : VisualizationMode.BAR;
            vizToggle.setText("Mode: " + (visualizationMode == VisualizationMode.BAR ? "Bars" : "Graphs"));
            if (onVisualizationModeChanged != null) onVisualizationModeChanged.accept(visualizationMode);
        });

        content.getChildren().addAll(
            new Label("Global Controls"),
            vizToggle,
            new Button("Reset All"),
            new Button("Settings"),
            new Button("About")
        );
        content.getChildren().forEach(node -> {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
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
