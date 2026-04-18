package com.rtmidimonitor;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.rtmidijava.RtMidiIn;

public class MidiDeviceView extends VBox {
    private final String portName;
    private final RtMidiIn midiIn;
    private final MidiState state = new MidiState();
    private final MidiVisualizer visualizer;
    private final Label bpmLabel = new Label("BPM: ---");

    public MidiDeviceView(String portName, RtMidiIn midiIn) {
        this.portName = portName;
        this.midiIn = midiIn;
        this.visualizer = new MidiVisualizer(state);
        
        setPadding(new Insets(5));
        setSpacing(5);
        setStyle("-fx-border-color: gray; -fx-border-width: 1; -fx-background-color: #2e2e2e;");
        
        Label titleLabel = new Label(portName);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        bpmLabel.setStyle("-fx-text-fill: #aaa;");
        
        visualizer.setHeight(300);
        visualizer.setWidth(250);
        
        getChildren().addAll(titleLabel, bpmLabel, visualizer);
        
        setupMidi();
    }

    private void setupMidi() {
        midiIn.setCallback((timeStamp, message) -> {
            MidiMessage msg = MidiParser.parse(message);
            if (msg != null) {
                state.handleMessage(msg, timeStamp);
                visualizer.requestRedraw();
                if (msg instanceof MidiMessage.SystemMessage s && s.type() == 0xF8) {
                    Platform.runLater(() -> bpmLabel.setText(String.format("BPM: %.1f", state.getBpm())));
                }
            }
        });
    }

    public void updateFilters(boolean sysex, boolean clock, boolean sense) {
        midiIn.ignoreTypes(sysex, clock, sense);
    }

    public void close() {
        midiIn.closePort();
    }
    
    public String getPortName() {
        return portName;
    }
}
