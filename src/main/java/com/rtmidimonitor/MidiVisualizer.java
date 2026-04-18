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
        settings.octaveOffsetProperty().addListener(e -> draw());
        settings.compactModeProperty().addListener(e -> draw());
        Platform.runLater(this::draw);
    }

    public void setMode(VisualizationMode mode) { this.mode = mode; draw(); }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth(); double h = getHeight();
        Theme theme = settings.getTheme();
        double now = System.nanoTime() / 1_000_000_000.0;

        gc.setFill(theme.background);
        gc.fillRect(0, 0, w, h);

        double y = 10; double xPad = 10;
        double rowH = settings.isCompactMode() ? 18 : 25;
        double fs = settings.isCompactMode() ? 10 : 12;
        double hfs = settings.isCompactMode() ? 11 : 14;

        MidiState.ClockState clock = state.getClock();
        if (!isExpired(clock.timeBpm, now) || !isExpired(clock.timeStart, now) || !isExpired(clock.timeStop, now)) {
            gc.setFill(theme.data); gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, hfs));
            gc.fillText("CLOCK", xPad, y + (rowH * 0.6));
            double cx = xPad + 70;
            if (!isExpired(clock.timeBpm, now)) { gc.setFill(theme.controller); gc.fillText(String.format("BPM %.1f", clock.bpm), cx, y + (rowH * 0.6)); cx += 100; }
            if (!isExpired(clock.timeStart, now)) { gc.setFill(theme.positive); gc.fillText("START", cx, y + (rowH * 0.6)); cx += 60; }
            if (!isExpired(clock.timeStop, now)) { gc.setFill(theme.negative); gc.fillText("STOP", cx, y + (rowH * 0.6)); }
            y += rowH; gc.setStroke(theme.separator); gc.strokeLine(xPad, y, w - xPad, y); y += 5;
        }

        double lw = settings.isCompactMode() ? 100 : 130;
        double vw = settings.isCompactMode() ? 50 : 65;
        double vizW = w - lw - vw - xPad * 4;

        for (int i = 0; i < 16; i++) {
            MidiState.Channel channel = state.getChannels()[i];
            if (isExpired(channel.lastUpdateTime, now)) continue;

            double chTop = y;
            gc.setFill(theme.data); gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, hfs));
            String chLabel = "CH " + (i + 1) + (channel.mpeManager ? " MGR" : (channel.mpeMember == MidiState.Channel.MpeMember.LOWER ? " LZ" : (channel.mpeMember == MidiState.Channel.MpeMember.UPPER ? " UZ" : "")));
            gc.fillText(chLabel, xPad, y + (rowH * 0.6));
            y += rowH;

            if (!isExpired(channel.pitchBend.current.time, now)) {
                drawItem(gc, "Pitch Bend", formatValue(channel.pitchBend.current.value), channel.pitchBend, 8192, 16383, true, theme, xPad, y, lw, vw, vizW, now, rowH, fs);
                y += rowH;
            }

            for (int j = 0; j < 128; j++) {
                if (!isExpired(channel.controlChanges[j].current.time, now)) {
                    boolean bidir = (j == 10 || j == 8 || j == 9);
                    drawItem(gc, getCcName(j), formatValue(channel.controlChanges[j].current.value), channel.controlChanges[j], 64, 127, bidir, theme, xPad, y, lw, vw, vizW, now, rowH, fs);
                    y += rowH;
                }
            }

            for (Map.Entry<Integer, MidiState.ChannelMessage> entry : channel.hrccs.entrySet()) {
                if (!isExpired(entry.getValue().current.time, now)) {
                    boolean bidir = (entry.getKey() == 10 || entry.getKey() == 8);
                    drawItem(gc, "HR " + getCcName(entry.getKey()), formatValue(entry.getValue().current.value), entry.getValue(), 8192, 16383, bidir, theme, xPad, y, lw, vw, vizW, now, rowH, fs);
                    y += rowH;
                }
            }

            for (int j = 0; j < 128; j++) {
                if (!isExpired(channel.notesOn[j].current.time, now)) {
                    drawItem(gc, "Note " + getNoteName(j), "ON " + channel.notesOn[j].current.value, channel.notesOn[j], 0, 127, false, theme, xPad, y, lw, vw, vizW, now, rowH, fs);
                    y += rowH;
                }
                if (!isExpired(channel.notesOff[j].current.time, now)) {
                    drawItem(gc, "Note " + getNoteName(j), "OFF", channel.notesOff[j], 0, 127, false, theme, xPad, y, lw, vw, vizW, now, rowH, fs);
                    y += rowH;
                }
            }

            if (channel.mpeMember != MidiState.Channel.MpeMember.NONE) {
                gc.setStroke(theme.controller); gc.setLineWidth(2);
                gc.strokeLine(xPad - 4, chTop + 2, xPad - 4, y - 2);
                gc.strokeLine(xPad - 4, chTop + 2, xPad, chTop + 2);
                gc.strokeLine(xPad - 4, y - 2, xPad, y - 2);
            }

            gc.setStroke(theme.separator); gc.setLineWidth(1);
            gc.strokeLine(xPad, y, w - xPad, y); y += 5;
        }
        if (y > h) { final double nh = y + 100; Platform.runLater(() -> setHeight(nh)); }
    }

    private String getCcName(int cc) {
        return switch (cc) {
            case 1 -> "Mod Wheel"; case 2 -> "Breath"; case 4 -> "Foot Ctrl"; case 5 -> "Porta Time"; case 7 -> "Volume";
            case 8 -> "Balance"; case 10 -> "Pan"; case 11 -> "Expression"; case 64 -> "Sustain"; case 65 -> "Porta Sw";
            case 66 -> "Sostenuto"; case 67 -> "Soft Pedal"; case 71 -> "Resonance"; case 74 -> "Cutoff";
            default -> "CC " + cc;
        };
    }

    private String formatValue(int v) {
        return settings.getNumberFormat() == AppSettings.NumberFormat.HEX ? String.format("%02X", v) : String.valueOf(v);
    }

    private boolean isExpired(double t, double now) { return t == 0 || (now - t) > EXPIRY_SEC; }

    private void drawItem(GraphicsContext gc, String lbl, String val, MidiState.ChannelMessage msg, int ctr, int max, boolean bidir, Theme theme, double x, double y, double lw, double vw, double vizW, double now, double rh, double fs) {
        double glow = Math.max(0, 1.0 - (now - msg.lastChangeTime) * 5.0); // 200ms glow
        gc.setFill(glow > 0 ? Color.WHITE : theme.label);
        gc.setFont(javafx.scene.text.Font.font("Monospaced", fs));
        gc.fillText(lbl, x, y + (rh * 0.7));

        gc.setFill(glow > 0 ? Color.WHITE : theme.data);
        gc.fillText(val, x + lw, y + (rh * 0.7));

        double vx = x + lw + vw + 10; double vy = y + (rh * 0.2); double vh = rh * 0.6;
        if (mode == VisualizationMode.BAR) {
            gc.setFill(theme.track); gc.fillRect(vx, vy, vizW, vh);
            gc.setFill(theme.controller);
            if (bidir) {
                double mid = vizW / 2.0; double v = ((double)msg.current.value - ctr) / ctr;
                gc.fillRect(vx + mid, vy, v * mid, vh);
            } else {
                double v = (double)msg.current.value / max; gc.fillRect(vx, vy, v * vizW, vh);
            }
            if (glow > 0) {
                gc.setGlobalAlpha(glow * 0.5); gc.setFill(Color.WHITE); gc.fillRect(vx, vy, vizW, vh); gc.setGlobalAlpha(1.0);
            }
        } else {
            gc.setStroke(theme.controller); gc.setLineWidth(1.5);
            double ts = 5.0; double lx = vx + vizW; double ly = vy + vh - ((double)msg.current.value / max) * vh;
            gc.beginPath(); gc.moveTo(lx, ly);
            for (MidiState.TimedValue tv : msg.history) {
                double dx = (now - tv.time) * (vizW / ts); double px = vx + vizW - dx;
                if (px < vx) break;
                double py = vy + vh - ((double)tv.value / max) * vh; gc.lineTo(px, py);
            }
            gc.stroke();
        }
    }

    private String getNoteName(int n) {
        if (settings.getNoteFormat() == AppSettings.NoteFormat.NUMBER) return formatValue(n);
        int off = switch(settings.getOctaveOffset()) { case C3 -> -2; case C4 -> -1; case C5 -> 0; };
        String[] nms = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return nms[n % 12] + (n / 12 + off);
    }
    
    public void requestRedraw() { Platform.runLater(this::draw); }
}
