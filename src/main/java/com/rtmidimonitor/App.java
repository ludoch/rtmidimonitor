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
import javafx.scene.paint.Color;
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
    private final AppSettings settings = AppSettings.getInstance();

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
        
        sidebar.addCustomContent(sidebarContent);

        // Center: Device Views and Log
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);

        ScrollPane deviceScrollPane = new ScrollPane(deviceViewsContainer);
        deviceScrollPane.setFitToWidth(true);
        
        ListView<String> logView = new ListView<>(logItems);

        splitPane.getItems().addAll(deviceScrollPane, logView);
        splitPane.setDividerPositions(0.7);

        root.setCenter(splitPane);

        Runnable updateTheme = () -> {
            Theme theme = settings.getTheme();
            String bgHex = toHex(theme.background);
            String dataHex = toHex(theme.data);
            
            deviceScrollPane.setStyle("-fx-background: " + bgHex + "; -fx-border-color: " + bgHex + ";");
            deviceViewsContainer.setStyle("-fx-background-color: " + bgHex + ";");
            logView.setStyle("-fx-control-inner-background: " + bgHex + "; -fx-text-fill: " + dataHex + ";");
            
            sidebarContent.getChildren().forEach(n -> {
                String labelHex = toHex(theme.label);
                if (n instanceof Label) n.setStyle("-fx-text-fill: " + labelHex + "; -fx-font-weight: bold;");
                if (n instanceof CheckBox) n.setStyle("-fx-text-fill: " + labelHex + ";");
            });
        };
        
        settings.themeProperty().addListener(e -> updateTheme.run());
        updateTheme.run();

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

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X", 
            (int)(color.getRed() * 255), 
            (int)(color.getGreen() * 255), 
            (int)(color.getBlue() * 255));
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
        boolean useHex = settings.getNumberFormat() == AppSettings.NumberFormat.HEX;
        
        for (byte b : raw) {
            sb.append(String.format("%02X ", b));
        }
        sb.append(" | ");

        if (msg instanceof MidiMessage.NoteOn n) {
            sb.append(String.format("Note On: Ch %d, Note %s, Vel %s", 
                n.channel() + 1, 
                getNoteName(n.note()), 
                useHex ? String.format("%02X", n.velocity()) : n.velocity()));
        } else if (msg instanceof MidiMessage.NoteOff n) {
            sb.append(String.format("Note Off: Ch %d, Note %s, Vel %s", 
                n.channel() + 1, 
                getNoteName(n.note()), 
                useHex ? String.format("%02X", n.velocity()) : n.velocity()));
        } else if (msg instanceof MidiMessage.ControlChange c) {
            sb.append(String.format("CC: Ch %d, Ctrl %s, Val %s", 
                c.channel() + 1, 
                useHex ? String.format("%02X", c.controller()) : c.controller(), 
                useHex ? String.format("%02X", c.value()) : c.value()));
        } else if (msg instanceof MidiMessage.PitchBend p) {
            sb.append(String.format("Pitch Bend: Ch %d, Val %s", 
                p.channel() + 1, 
                useHex ? String.format("%04X", p.value()) : p.value()));
        } else if (msg instanceof MidiMessage.ChannelAftertouch a) {
            sb.append(String.format("Channel Aftertouch: Ch %d, Pressure %s", 
                a.channel() + 1, 
                useHex ? String.format("%02X", a.pressure()) : a.pressure()));
        } else if (msg instanceof MidiMessage.PolyAftertouch a) {
            sb.append(String.format("Poly Aftertouch: Ch %d, Note %s, Pressure %s", 
                a.channel() + 1, 
                getNoteName(a.note()), 
                useHex ? String.format("%02X", a.pressure()) : a.pressure()));
        } else if (msg instanceof MidiMessage.Sysex s) {
            sb.append("Sysex: ").append(s.data().length).append(" bytes");
        } else {
            sb.append("Unknown message");
        }
        
        return sb.toString();
    }

    private String getNoteName(int note) {
        if (settings.getNoteFormat() == AppSettings.NoteFormat.NUMBER) {
            return settings.getNumberFormat() == AppSettings.NumberFormat.HEX ? 
                String.format("%02X", note) : String.valueOf(note);
        }
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
