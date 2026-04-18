package com.rtmidimonitor;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.rtmidijava.RtMidiIn;

public class MidiDeviceView extends VBox {
    private final String portName;
    private final RtMidiIn midiIn;
    private final MidiState state = new MidiState();
    private final MidiVisualizer visualizer;
    private final Label bpmLabel = new Label("BPM: ---");
    private final AppSettings settings = AppSettings.getInstance();
    private final Label titleLabel = new Label();
    private boolean frozen = false;

    public MidiDeviceView(String portName, RtMidiIn midiIn) {
        this.portName = portName;
        this.midiIn = midiIn;
        this.visualizer = new MidiVisualizer(state);
        
        setPadding(new Insets(10));
        setSpacing(10);
        
        titleLabel.setText(portName);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        ScrollPane scrollPane = new ScrollPane(visualizer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        visualizer.widthProperty().bind(scrollPane.widthProperty().subtract(20));
        
        getChildren().addAll(titleLabel, bpmLabel, scrollPane);
        
        setupMidi();
        
        updateStyle();
        settings.themeProperty().addListener(e -> updateStyle());
    }

    private void updateStyle() {
        Theme theme = settings.getTheme();
        String bgHex = toHex(theme.background);
        String labelHex = toHex(theme.label);
        String dataHex = toHex(theme.data);
        
        setStyle("-fx-border-color: " + labelHex + "; -fx-border-width: 1; -fx-background-color: " + bgHex + ";");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + dataHex + ";");
        bpmLabel.setStyle("-fx-text-fill: " + labelHex + ";");
    }

    private String toHex(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X", 
            (int)(color.getRed() * 255), 
            (int)(color.getGreen() * 255), 
            (int)(color.getBlue() * 255));
    }

    private void setupMidi() {
        if (midiIn == null) return;

        midiIn.setCallback((timeStamp, message) -> {
            if (frozen) return;
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
        if (midiIn != null) midiIn.ignoreTypes(sysex, clock, sense);
    }

    public void setVisualizationMode(VisualizationMode mode) {
        visualizer.setMode(mode);
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public void resetState() {
        state.reset();
        visualizer.requestRedraw();
    }

    public void handleMockMessage(MidiMessage msg, double timeStamp) {
        if (frozen) return;
        state.handleMessage(msg, timeStamp);
        visualizer.requestRedraw();
    }

    public void close() {
        if (midiIn != null) midiIn.closePort();
    }
    
    public String getPortName() {
        return portName;
    }
}
