package com.rtmidimonitor;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import org.rtmidijava.RtMidiIn;
import java.util.HashMap;
import java.util.Map;

public class MidiDeviceView extends VBox {
    private final String portName;
    private final RtMidiIn midiIn;
    private final MidiState state = new MidiState();
    private final Map<Integer, MidiChannelVisualizer> channelViews = new HashMap<>();
    private final FlowPane channelGrid = new FlowPane(10, 10);
    private final Label bpmLabel = new Label("BPM: ---");
    private final AppSettings settings = AppSettings.getInstance();
    private boolean frozen = false;
    private VisualizationMode currentMode = VisualizationMode.BAR;

    public MidiDeviceView(String portName, RtMidiIn midiIn) {
        this.portName = portName;
        this.midiIn = midiIn;
        setPadding(new Insets(10)); 
        setSpacing(5);
        
        Label title = new Label("DEVICE: " + portName);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f0;");
        
        channelGrid.setPadding(new Insets(10));
        // Force wrapping logic
        widthProperty().addListener((obs, oldVal, newVal) -> {
            channelGrid.setPrefWrapLength(newVal.doubleValue() - 40);
        });
        
        getChildren().addAll(title, bpmLabel, channelGrid);
        
        setupMidi();
        updateStyle();
        settings.themeProperty().addListener(e -> updateStyle());
    }

    private void updateStyle() {
        Theme theme = settings.getTheme();
        setStyle("-fx-border-color: " + toHex(theme.label) + "; -fx-border-width: 1; -fx-background-color: " + toHex(theme.background) + ";");
        bpmLabel.setStyle("-fx-text-fill: " + toHex(theme.label) + ";");
    }

    private String toHex(javafx.scene.paint.Color c) { 
        return String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255)); 
    }

    private void setupMidi() {
        if (midiIn == null) return;
        midiIn.setCallback((ts, bytes) -> {
            if (frozen) return;
            MidiMessage msg = MidiParser.parse(bytes);
            if (msg != null) handleMessageInternal(msg, ts);
        });
    }

    private void handleMessageInternal(MidiMessage msg, double ts) {
        state.handleMessage(msg, ts);
        int chIdx = msg.channel();
        if (chIdx >= 0 && chIdx < 16) {
            Platform.runLater(() -> {
                MidiChannelVisualizer v = channelViews.get(chIdx);
                if (v == null) {
                    v = new MidiChannelVisualizer(state, state.getChannels()[chIdx]);
                    v.setMode(currentMode);
                    channelViews.put(chIdx, v);
                    channelGrid.getChildren().add(v);
                    System.out.println("[GRID] Added Card for CH " + (chIdx + 1));
                }
                v.requestRedraw();
                if (msg instanceof MidiMessage.SystemMessage s && s.type() == 0xF8) {
                    bpmLabel.setText(String.format("BPM: %.1f", state.getClock().bpm));
                }
            });
        }
    }

    public void handleMockMessage(MidiMessage msg, double ts) {
        if (frozen) return;
        handleMessageInternal(msg, ts);
    }

    public void updateFilters(boolean sysex, boolean clock, boolean sense) { if (midiIn != null) midiIn.ignoreTypes(sysex, clock, sense); }
    public void setVisualizationMode(VisualizationMode mode) { 
        this.currentMode = mode;
        channelViews.values().forEach(v -> v.setMode(mode)); 
    }
    public void setFrozen(boolean f) { this.frozen = f; }
    public void resetState() { state.reset(); channelGrid.getChildren().clear(); channelViews.clear(); }
    public void close() { if (midiIn != null) midiIn.closePort(); }
}
