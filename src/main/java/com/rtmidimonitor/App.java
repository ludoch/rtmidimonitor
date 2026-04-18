package com.rtmidimonitor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.rtmidijava.RtMidiIn;
import org.rtmidijava.RtMidiFactory;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.FlowPane;
import javafx.scene.control.ScrollPane;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.layout.BorderPane;

public class App extends Application {

    private final ObservableList<String> logItems = FXCollections.observableArrayList();
    private final VBox portListContainer = new VBox(5);
    private final FlowPane deviceViewsContainer = new FlowPane(10, 10);
    private final Map<String, MidiDeviceView> activeDevices = new HashMap<>();

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        // Left: Sidebar
        SidebarView sidebar = new SidebarView();
        sidebar.setOnVisualizationModeChanged(mode -> {
            activeDevices.values().forEach(v -> v.setVisualizationMode(mode));
        });
        root.setLeft(sidebar);

        // Sidebar Content: Move Port selection there
        VBox sidebarContent = new VBox(10);
        Button refreshButton = new Button("Refresh Ports");
        refreshButton.setOnAction(e -> refreshPorts());
        
        Button clearLogButton = new Button("Clear Log");
        clearLogButton.setOnAction(e -> logItems.clear());
        
        CheckBox ignoreSysex = new CheckBox("Ignore Sysex");
        ignoreSysex.setSelected(true);
        CheckBox ignoreClock = new CheckBox("Ignore Clock");
        ignoreClock.setSelected(true);
        CheckBox ignoreSense = new CheckBox("Ignore Active Sensing");
        ignoreSense.setSelected(true);

        ignoreSysex.setOnAction(e -> updateMidiFilters(ignoreSysex.isSelected(), ignoreClock.isSelected(), ignoreSense.isSelected()));
        ignoreClock.setOnAction(e -> updateMidiFilters(ignoreSysex.isSelected(), ignoreClock.isSelected(), ignoreSense.isSelected()));
        ignoreSense.setOnAction(e -> updateMidiFilters(ignoreSysex.isSelected(), ignoreClock.isSelected(), ignoreSense.isSelected()));

        ScrollPane portScrollPane = new ScrollPane(portListContainer);
        portScrollPane.setFitToWidth(true);
        portScrollPane.setPrefHeight(200);

        sidebarContent.getChildren().addAll(
            new Label("Available Ports:"), refreshButton, portScrollPane,
            new Label("Filters:"), ignoreSysex, ignoreClock, ignoreSense,
            clearLogButton
        );
        sidebarContent.getChildren().forEach(n -> {
            if (n instanceof Label) ((Label)n).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            if (n instanceof CheckBox) ((CheckBox)n).setStyle("-fx-text-fill: white;");
        });
        sidebar.addCustomContent(sidebarContent);

        // Center: Device Views and Log
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);

        ScrollPane deviceScrollPane = new ScrollPane(deviceViewsContainer);
        deviceScrollPane.setFitToWidth(true);
        deviceScrollPane.setStyle("-fx-background: #1e1e1e; -fx-border-color: #1e1e1e;");
        
        ListView<String> logView = new ListView<>(logItems);
        logView.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #00ff00;");

        splitPane.getItems().addAll(deviceScrollPane, logView);
        splitPane.setDividerPositions(0.7);

        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1400, 900);
        stage.setTitle("RtMidiMonitor");
        stage.setScene(scene);
        stage.show();

        refreshPorts();

        stage.setOnCloseRequest(e -> {
            activeDevices.values().forEach(MidiDeviceView::close);
            Platform.exit();
        });
    }

    private void refreshPorts() {
        portListContainer.getChildren().clear();
        try {
            RtMidiIn probe = RtMidiFactory.createDefaultIn();
            int count = probe.getPortCount();
            for (int i = 0; i < count; i++) {
                String name = probe.getPortName(i);
                final int portIndex = i;
                CheckBox cb = new CheckBox(name);
                cb.setOnAction(e -> togglePort(name, portIndex, cb.isSelected()));
                portListContainer.getChildren().add(cb);
            }
            // bpm estimation needs a fixed device or we aggregate? Original ShowMidi does it per device.
        } catch (Exception e) {
            logItems.add(0, "Error refreshing ports: " + e.getMessage());
        }
    }

    private void togglePort(String name, int index, boolean selected) {
        if (selected) {
            try {
                RtMidiIn midiIn = RtMidiFactory.createDefaultIn();
                midiIn.openPort(index, "RtMidiMonitor-" + name);
                MidiDeviceView view = new MidiDeviceView(name, midiIn);
                activeDevices.put(name, view);
                deviceViewsContainer.getChildren().add(view);
                
                // Also add a log callback for this device
                midiIn.setCallback((timeStamp, message) -> {
                    MidiMessage msg = MidiParser.parse(message);
                    String logLine = String.format("[%s][%f] %s", name, timeStamp, formatMessage(msg, message));
                    Platform.runLater(() -> {
                        logItems.add(0, logLine);
                        if (logItems.size() > 200) logItems.remove(200, logItems.size());
                    });
                });
                
            } catch (Exception e) {
                logItems.add(0, "Error opening port " + name + ": " + e.getMessage());
            }
        } else {
            MidiDeviceView view = activeDevices.remove(name);
            if (view != null) {
                view.close();
                deviceViewsContainer.getChildren().remove(view);
            }
        }
    }

    private String formatMessage(MidiMessage msg, byte[] raw) {
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) {
            sb.append(String.format("%02X ", b));
        }
        sb.append(" | ");

        if (msg instanceof MidiMessage.NoteOn n) {
            sb.append(String.format("Note On: Ch %d, Note %d (%s), Vel %d", n.channel() + 1, n.note(), getNoteName(n.note()), n.velocity()));
        } else if (msg instanceof MidiMessage.NoteOff n) {
            sb.append(String.format("Note Off: Ch %d, Note %d (%s), Vel %d", n.channel() + 1, n.note(), getNoteName(n.note()), n.velocity()));
        } else if (msg instanceof MidiMessage.ControlChange c) {
            sb.append(String.format("CC: Ch %d, Ctrl %d, Val %d", c.channel() + 1, c.controller(), c.value()));
        } else if (msg instanceof MidiMessage.PitchBend p) {
            sb.append(String.format("Pitch Bend: Ch %d, Val %d", p.channel() + 1, p.value()));
        } else if (msg instanceof MidiMessage.ChannelAftertouch a) {
            sb.append(String.format("Channel Aftertouch: Ch %d, Pressure %d", a.channel() + 1, a.pressure()));
        } else if (msg instanceof MidiMessage.PolyAftertouch a) {
            sb.append(String.format("Poly Aftertouch: Ch %d, Note %d (%s), Pressure %d", a.channel() + 1, a.note(), getNoteName(a.note()), a.pressure()));
        } else if (msg instanceof MidiMessage.Sysex s) {
            sb.append("Sysex: ").append(s.data().length).append(" bytes");
        } else {
            sb.append("Unknown message");
        }
        
        return sb.toString();
    }

    private String getNoteName(int note) {
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return names[note % 12] + (note / 12 - 1);
    }

    private void updateMidiFilters(boolean sysex, boolean clock, boolean sense) {
        activeDevices.values().forEach(v -> v.updateFilters(sysex, clock, sense));
    }

    public static void main(String[] args) {
        launch();
    }
}
