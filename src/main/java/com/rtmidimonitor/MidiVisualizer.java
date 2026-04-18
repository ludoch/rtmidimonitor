package com.rtmidimonitor;

import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class MidiVisualizer extends Canvas {
    private final MidiState state;

    public MidiVisualizer(MidiState state) {
        this.state = state;
        widthProperty().addListener(e -> draw());
        heightProperty().addListener(e -> draw());
        
        // In a more complex version, we would listen to all channels
        // For now, let's just trigger a redraw on any update
        // (Simplified for this phase)
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.setFill(Color.web("#1e1e1e"));
        gc.fillRect(0, 0, w, h);

        double channelHeight = h / 16.0;

        for (int i = 0; i < 16; i++) {
            MidiState.Channel channel = state.getChannels()[i];
            drawChannel(gc, channel, i * channelHeight, w, channelHeight);
        }
    }

    private void drawChannel(GraphicsContext gc, MidiState.Channel ch, double y, double w, double h) {
        // Grid lines for octaves
        gc.setStroke(Color.web("#333333"));
        double noteWidth = w / 128.0;
        for (int i = 0; i < 128; i += 12) {
            double x = i * noteWidth;
            gc.strokeLine(x, y, x, y + h);
        }

        // Draw active notes
        gc.setFill(Color.LIME.deriveColor(0, 1, 1, 0.8));
        for (int i = 0; i < 128; i++) {
            MidiState.ChannelMessage msg = ch.notesOn[i];
            if (msg.current.time > 0) {
                double x = i * noteWidth;
                double barHeight = (msg.current.value / 127.0) * h;
                gc.fillRect(x, y + h - barHeight, noteWidth - 1, barHeight);
            }
        }

        // Draw CCs (simplified: just latest active CC)
        gc.setFill(Color.ORANGE.deriveColor(0, 1, 1, 0.4));
        for (int i = 0; i < 128; i++) {
            MidiState.ChannelMessage msg = ch.controlChanges[i];
            if (msg.current.time > 0) {
                double x = i * noteWidth;
                double barHeight = (msg.current.value / 127.0) * h;
                gc.fillRect(x, y, noteWidth - 1, barHeight);
            }
        }

        // Draw Pitch Bend
        if (ch.pitchBend.current.time > 0) {
            gc.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.5));
            double pbVal = (ch.pitchBend.current.value - 8192) / 8192.0;
            double centerX = w / 2.0;
            gc.fillRect(centerX, y, pbVal * (w / 2.0), h);
        }

        // Channel label
        gc.setFill(Color.WHITE);
        gc.fillText("Ch " + (ch.number + 1), 5, y + 12);
    }
    
    // Manual redraw request for when state changes
    public void requestRedraw() {
        Platform.runLater(this::draw);
    }
}
