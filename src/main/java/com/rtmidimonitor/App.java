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

public class App extends Application {

    private RtMidiIn midiIn;
    private final ObservableList<String> logItems = FXCollections.observableArrayList();
    private final ComboBox<String> portSelector = new ComboBox<>();
    private final List<String> portNames = new ArrayList<>();
    private final MidiState midiState = new MidiState();
    private MidiVisualizer visualizer;
    private final Label bpmLabel = new Label("BPM: ---");

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Top: Port Selection
        HBox topBar = new HBox(10);
        Button refreshButton = new Button("Refresh Ports");
        refreshButton.setOnAction(e -> refreshPorts());
        
        portSelector.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(portSelector, Priority.ALWAYS);
        portSelector.setOnAction(e -> openSelectedPort());

        topBar.getChildren().addAll(new Label("MIDI Port:"), portSelector, refreshButton);

        // Toolbar: Filters
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(5, 0, 5, 0));
        Button clearButton = new Button("Clear Log");
        clearButton.setOnAction(e -> {
            logItems.clear();
            midiState.reset();
        });

        CheckBox ignoreSysex = new CheckBox("Ignore Sysex");
        ignoreSysex.setSelected(true);

        CheckBox ignoreClock = new CheckBox("Ignore Clock");
        ignoreClock.setSelected(true);

        CheckBox ignoreSense = new CheckBox("Ignore Active Sensing");
        ignoreSense.setSelected(true);
        
        ignoreSysex.setOnAction(e -> updateMidiFilters(ignoreSysex.isSelected(), ignoreClock.isSelected(), ignoreSense.isSelected()));
        ignoreClock.setOnAction(e -> updateMidiFilters(ignoreSysex.isSelected(), ignoreClock.isSelected(), ignoreSense.isSelected()));
        ignoreSense.setOnAction(e -> updateMidiFilters(ignoreSysex.isSelected(), ignoreClock.isSelected(), ignoreSense.isSelected()));

        toolbar.getChildren().addAll(clearButton, ignoreSysex, ignoreClock, ignoreSense);

        // Middle: SplitPane
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        ListView<String> logView = new ListView<>(logItems);
        
        visualizer = new MidiVisualizer(midiState);
        // Make visualizer responsive
        Pane visualizerContainer = new Pane(visualizer);
        visualizer.widthProperty().bind(visualizerContainer.widthProperty());
        visualizer.heightProperty().bind(visualizerContainer.heightProperty());

        splitPane.getItems().addAll(visualizerContainer, logView);
        splitPane.setDividerPositions(0.4);

        // Bottom: Status
        Label statusLabel = new Label("Initializing...");
        HBox bottomBar = new HBox(20);
        bottomBar.getChildren().addAll(statusLabel, bpmLabel);

        root.getChildren().addAll(topBar, toolbar, splitPane, bottomBar);

        Scene scene = new Scene(root, 1024, 768);
        stage.setTitle("RtMidiMonitor");
        stage.setScene(scene);
        stage.show();

        try {
            midiIn = RtMidiFactory.createDefaultIn();
            statusLabel.setText("API: " + midiIn.getCurrentApi());
            refreshPorts();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }

        stage.setOnCloseRequest(e -> {
            if (midiIn != null) {
                midiIn.closePort();
            }
            Platform.exit();
        });
    }

    private void refreshPorts() {
        if (midiIn == null) return;
        
        portNames.clear();
        int count = midiIn.getPortCount();
        for (int i = 0; i < count; i++) {
            portNames.add(midiIn.getPortName(i));
        }
        portSelector.setItems(FXCollections.observableArrayList(portNames));
        if (!portNames.isEmpty()) {
            portSelector.getSelectionModel().select(0);
        }
    }

    private void openSelectedPort() {
        int index = portSelector.getSelectionModel().getSelectedIndex();
        if (index >= 0 && index < portNames.size()) {
            if (midiIn.isPortOpen()) {
                midiIn.closePort();
            }
            try {
                midiIn.openPort(index, "RtMidiMonitor");
                // Apply default filters (assuming they match the checkbox defaults)
                midiIn.ignoreTypes(true, true, true);
                midiIn.setCallback((timeStamp, message) -> {
                    MidiMessage msg = MidiParser.parse(message);
                    if (msg != null) {
                        midiState.handleMessage(msg, timeStamp);
                        visualizer.requestRedraw();
                        if (msg instanceof MidiMessage.SystemMessage s && s.type() == 0xF8) {
                             Platform.runLater(() -> bpmLabel.setText(String.format("BPM: %.1f", midiState.getBpm())));
                             return;
                        }
                    }
                    String logLine = String.format("[%f] %s", timeStamp, formatMessage(msg, message));
                    Platform.runLater(() -> {
                        logItems.add(0, logLine);
                        if (logItems.size() > 100) {
                            logItems.remove(100, logItems.size());
                        }
                    });
                });
            } catch (Exception e) {
                logItems.add(0, "Error opening port: " + e.getMessage());
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
        if (midiIn != null) {
            midiIn.ignoreTypes(sysex, clock, sense);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
