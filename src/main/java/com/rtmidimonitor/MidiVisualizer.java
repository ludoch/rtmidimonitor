package com.rtmidimonitor;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class MidiVisualizer extends Canvas {
    private final MidiState state;
    private VisualizationMode mode = VisualizationMode.BAR;

    public MidiVisualizer(MidiState state) {
        this.state = state;
        widthProperty().addListener(e -> draw());
        heightProperty().addListener(e -> draw());
    }

    public void setMode(VisualizationMode mode) {
        this.mode = mode;
        draw();
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
        if (mode == VisualizationMode.BAR) {
            drawChannelBars(gc, ch, y, w, h);
        } else {
            drawChannelGraph(gc, ch, y, w, h);
        }
    }

    private void drawChannelBars(GraphicsContext gc, MidiState.Channel ch, double y, double w, double h) {
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

        // Draw CCs
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

        gc.setFill(Color.WHITE);
        gc.fillText("Ch " + (ch.number + 1), 5, y + 12);
    }

    private void drawChannelGraph(GraphicsContext gc, MidiState.Channel ch, double y, double w, double h) {
        MidiState.ChannelMessage latest = null;
        double latestTime = -1;
        
        if (ch.pitchBend.current.time > latestTime) {
            latest = ch.pitchBend;
            latestTime = ch.pitchBend.current.time;
        }
        for (int i = 0; i < 128; i++) {
            if (ch.controlChanges[i].current.time > latestTime) {
                latest = ch.controlChanges[i];
                latestTime = ch.controlChanges[i].current.time;
            }
        }

        if (latest != null && !latest.history.isEmpty()) {
            gc.setStroke(Color.ORANGE);
            gc.beginPath();
            double timeScale = 10.0;
            double currentTime = latest.current.time;
            
            double x = w;
            double valY = y + h - (latest.current.value / 127.0) * h;
            if (latest == ch.pitchBend) valY = y + h/2.0 - ((latest.current.value - 8192) / 8192.0) * (h/2.0);
            
            gc.moveTo(x, valY);
            
            for (MidiState.TimedValue tv : latest.history) {
                double dx = (currentTime - tv.time) * (w / timeScale);
                x = w - dx;
                if (x < 0) break;
                valY = y + h - (tv.value / 127.0) * h;
                if (latest == ch.pitchBend) valY = y + h/2.0 - ((tv.value - 8192) / 8192.0) * (h/2.0);
                gc.lineTo(x, valY);
            }
            gc.stroke();
        }

        gc.setFill(Color.WHITE);
        gc.fillText("Ch " + (ch.number + 1) + " (Graph)", 5, y + 12);
    }
    
    public void requestRedraw() {
        Platform.runLater(this::draw);
    }
}
