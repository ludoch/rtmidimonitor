package com.rtmidimonitor;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.Window;

public class HelpView {

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.setTitle("User Guide");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        content.getChildren().addAll(
            createSection("Interactivity", "All checkboxes and buttons in the sidebar are directly interactive. If a checkbox doesn't respond, ensure the window is focused."),
            createSection("Simulation", "Click 'Simulate MIDI' to generate a continuous stream of Note, CC, and PB data for testing."),
            createSection("Shortcuts", "[SPACE] Freeze UI\n[DELETE] Clear Log\n[R] Reset state\n[F] Fullscreen"),
            createSection("Filters", "Toggle filters to ignore Sysex or Clock noise in the log.")
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true); scroll.setPrefHeight(400);

        Button closeBtn = new Button("Close");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> stage.close());

        VBox root = new VBox(scroll, closeBtn);
        VBox.setMargin(closeBtn, new Insets(10));

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private static VBox createSection(String title, String body) {
        Label head = new Label(title); head.setFont(Font.font("System", FontWeight.BOLD, 14));
        Text text = new Text(body); text.setFont(Font.font("Monospaced", 12));
        return new VBox(5, head, new TextFlow(text));
    }
}
