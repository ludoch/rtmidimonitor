package com.rtmidimonitor;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.Map;

public class MidiVisualizer extends Canvas {
    private final MidiState state;
    private VisualizationMode mode = VisualizationMode.BAR;
    private final AppSettings settings = AppSettings.getInstance();
    private static final double EXPIRY_SEC = 5.0;

    public MidiVisualizer(MidiState state) {
        this.state = state;
        widthProperty().addListener(e -> draw());
        heightProperty().addListener(e -> draw());
        
        settings.themeProperty().addListener(e -> draw());
        settings.numberFormatProperty().addListener(e -> draw());
        settings.noteFormatProperty().addListener(e -> draw());
        
        Platform.runLater(this::draw);
    }

    public void setMode(VisualizationMode mode) {
        this.mode = mode;
        draw();
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        Theme theme = settings.getTheme();
        double now = System.nanoTime() / 1_000_000_000.0;

        gc.setFill(theme.background);
        gc.fillRect(0, 0, w, h);

        double yOffset = 10;
        double xPadding = 10;
        
        // Clock transport
        MidiState.ClockState clock = state.getClock();
        boolean hasClock = !isExpired(clock.timeBpm, now) || !isExpired(clock.timeStart, now) || !isExpired(clock.timeStop, now) || !isExpired(clock.timeContinue, now);
        
        if (hasClock) {
            gc.setFill(theme.data);
            gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 14));
            gc.fillText("CLOCK", xPadding, yOffset + 15);
            
            double clockX = xPadding + 70;
            if (!isExpired(clock.timeBpm, now)) {
                gc.setFill(theme.controller);
                gc.fillText(String.format("BPM %.1f", clock.bpm), clockX, yOffset + 15);
                clockX += 110;
            }
            if (!isExpired(clock.timeStart, now)) {
                gc.setFill(theme.positive);
                gc.fillText("START", clockX, yOffset + 15);
                clockX += 70;
            }
            if (!isExpired(clock.timeContinue, now)) {
                gc.setFill(theme.positive);
                gc.fillText("CONT", clockX, yOffset + 15);
                clockX += 70;
            }
            if (!isExpired(clock.timeStop, now)) {
                gc.setFill(theme.negative);
                gc.fillText("STOP", clockX, yOffset + 15);
            }
            yOffset += 25;
            gc.setStroke(theme.separator);
            gc.strokeLine(xPadding, yOffset, w - xPadding, yOffset);
            yOffset += 10;
        }

        // Sysex
        MidiState.SysexData sysexData = state.getSysex();
        if (!isExpired(sysexData.time, now)) {
            gc.setFill(theme.data);
            gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 14));
            gc.fillText("SYSEX", xPadding, yOffset + 15);
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sysexData.length; i++) {
                sb.append(String.format("%02X ", sysexData.data[i]));
            }
            gc.setFill(theme.label);
            gc.setFont(javafx.scene.text.Font.font("Monospaced", 12));
            gc.fillText(sb.toString(), xPadding + 70, yOffset + 15);
            
            yOffset += 25;
            gc.setStroke(theme.separator);
            gc.strokeLine(xPadding, yOffset, w - xPadding, yOffset);
            yOffset += 10;
        }

        double labelWidth = 80;
        double valueWidth = 60;
        double vizWidth = w - labelWidth - valueWidth - xPadding * 4;

        for (int i = 0; i < 16; i++) {
            MidiState.Channel channel = state.getChannels()[i];
            if (isExpired(channel.lastUpdateTime, now)) continue;

            // Channel Header
            gc.setFill(theme.data);
            gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 14));
            String chLabel = "CH " + (i + 1);
            if (channel.mpeManager) chLabel += " MGR";
            else if (channel.mpeMember == MidiState.Channel.MpeMember.LOWER) chLabel += " LZ";
            else if (channel.mpeMember == MidiState.Channel.MpeMember.UPPER) chLabel += " UZ";
            
            gc.fillText(chLabel, xPadding, yOffset + 15);
            yOffset += 25;

            // Pitch Bend
            if (!isExpired(channel.pitchBend.current.time, now)) {
                drawItem(gc, "PB", formatValue(channel.pitchBend.current.value), channel.pitchBend, 0x2000, 16383, true, theme, xPadding, yOffset, labelWidth, valueWidth, vizWidth, now);
                yOffset += 25;
            }

            // Control Changes
            for (int j = 0; j < 128; j++) {
                MidiState.ChannelMessage cc = channel.controlChanges[j];
                if (!isExpired(cc.current.time, now)) {
                    drawItem(gc, "CC " + j, formatValue(cc.current.value), cc, 0, 127, false, theme, xPadding, yOffset, labelWidth, valueWidth, vizWidth, now);
                    yOffset += 25;
                }
            }

            // RPNs
            for (Map.Entry<Integer, MidiState.ChannelMessage> entry : channel.rpns.entrySet()) {
                if (!isExpired(entry.getValue().current.time, now)) {
                    drawItem(gc, getRpnName(entry.getKey()), formatValue(entry.getValue().current.value), entry.getValue(), 0x2000, 16383, true, theme, xPadding, yOffset, labelWidth, valueWidth, vizWidth, now);
                    yOffset += 25;
                }
            }

            // NRPNs
            for (Map.Entry<Integer, MidiState.ChannelMessage> entry : channel.nrpns.entrySet()) {
                if (!isExpired(entry.getValue().current.time, now)) {
                    drawItem(gc, "NRPN " + entry.getKey(), formatValue(entry.getValue().current.value), entry.getValue(), 0x2000, 16383, true, theme, xPadding, yOffset, labelWidth, valueWidth, vizWidth, now);
                    yOffset += 25;
                }
            }

            // Notes
            for (int j = 0; j < 128; j++) {
                MidiState.ChannelMessage noteOn = channel.notesOn[j];
                if (!isExpired(noteOn.current.time, now)) {
                    drawItem(gc, getNoteName(j), "ON " + noteOn.current.value, noteOn, 0, 127, false, theme, xPadding, yOffset, labelWidth, valueWidth, vizWidth, now);
                    yOffset += 25;
                }
                MidiState.ChannelMessage noteOff = channel.notesOff[j];
                if (!isExpired(noteOff.current.time, now)) {
                    drawItem(gc, getNoteName(j), "OFF", noteOff, 0, 127, false, theme, xPadding, yOffset, labelWidth, valueWidth, vizWidth, now);
                    yOffset += 25;
                }
            }

            // Separator
            gc.setStroke(theme.separator);
            gc.setLineWidth(1);
            gc.strokeLine(xPadding, yOffset, w - xPadding, yOffset);
            yOffset += 10;
        }
        
        // Dynamic resize if content exceeds current height
        if (yOffset > h) {
            final double newH = yOffset + 100;
            Platform.runLater(() -> setHeight(newH));
        }
    }

    private String formatValue(int value) {
        if (settings.getNumberFormat() == AppSettings.NumberFormat.HEX) {
            return String.format("%X", value);
        }
        return String.valueOf(value);
    }

    private boolean isExpired(double time, double now) {
        return time == 0 || (now - time) > EXPIRY_SEC;
    }

    private void drawItem(GraphicsContext gc, String label, String value, MidiState.ChannelMessage msg, int center, int max, boolean bidirectional, Theme theme, double x, double y, double lw, double vw, double vizW, double now) {
        gc.setFill(theme.label);
        gc.setFont(javafx.scene.text.Font.font("Monospaced", 12));
        gc.fillText(label, x, y + 15);

        gc.setFill(theme.data);
        gc.fillText(value, x + lw, y + 15);

        double vizX = x + lw + vw + 10;
        double vizY = y + 5;
        double vizH = 15;

        if (mode == VisualizationMode.BAR) {
            gc.setFill(theme.track);
            gc.fillRect(vizX, vizY, vizW, vizH);

            gc.setFill(theme.controller);
            if (bidirectional) {
                double mid = vizW / 2.0;
                double val = ((double)msg.current.value - center) / (max - center);
                gc.fillRect(vizX + mid, vizY, val * mid, vizH);
            } else {
                double val = (double)msg.current.value / max;
                gc.fillRect(vizX, vizY, val * vizW, vizH);
            }
        } else {
            // Graph Mode
            gc.setStroke(theme.controller);
            gc.setLineWidth(2);
            double timeScale = 5.0; // 5 seconds
            
            double lastX = vizX + vizW;
            double lastY = vizY + vizH - ((double)msg.current.value / max) * vizH;
            
            gc.beginPath();
            gc.moveTo(lastX, lastY);
            for (MidiState.TimedValue tv : msg.history) {
                double dx = (now - tv.time) * (vizW / timeScale);
                double px = vizX + vizW - dx;
                if (px < vizX) break;
                double py = vizY + vizH - ((double)tv.value / max) * vizH;
                gc.lineTo(px, py);
            }
            gc.stroke();
        }
    }
    
    public void requestRedraw() {
        Platform.runLater(this::draw);
    }

    private String getNoteName(int note) {
        if (settings.getNoteFormat() == AppSettings.NoteFormat.NUMBER) {
            return formatValue(note);
        }
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return names[note % 12] + (note / 12 - 1);
    }

    private String getRpnName(int param) {
        return switch (param) {
            case 0 -> "PB SNS";
            case 1 -> "FTUN";
            case 2 -> "CTUN";
            case 3 -> "TUN PC";
            case 4 -> "TUN BS";
            case 6 -> "MPE";
            default -> "RPN " + param;
        };
    }
}
