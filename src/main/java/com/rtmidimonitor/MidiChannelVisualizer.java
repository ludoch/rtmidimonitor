package com.rtmidimonitor;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.Map;

public class MidiChannelVisualizer extends Canvas {
    private final MidiState.Channel channel;
    private final MidiState state;
    private final AppSettings settings = AppSettings.getInstance();
    private VisualizationMode mode = VisualizationMode.BAR;
    private static final double EXPIRY_SEC = 5.0;

    public MidiChannelVisualizer(MidiState state, MidiState.Channel channel) {
        this.state = state;
        this.channel = channel;
        setHeight(260); 
        setWidth(320);
        
        settings.themeProperty().addListener(e -> draw());
        settings.compactModeProperty().addListener(e -> draw());
        Platform.runLater(this::draw);
    }

    public void setMode(VisualizationMode mode) {
        this.mode = mode;
        draw();
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth(); double h = getHeight();
        Theme theme = settings.getTheme();
        double now = System.nanoTime() / 1_000_000_000.0;

        gc.setFill(theme.background);
        gc.fillRect(0, 0, w, h);
        
        gc.setStroke(theme.separator);
        gc.setLineWidth(1);
        gc.strokeRect(1, 1, w-2, h-2);

        double y = 5; double xPad = 10;
        double rowH = settings.isCompactMode() ? 16 : 22;
        double fs = settings.isCompactMode() ? 10 : 12;

        gc.setFill(theme.data);
        gc.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, fs + 2));
        String chLabel = "CH " + (channel.number + 1) + (channel.mpeManager ? " MGR" : (channel.mpeMember != MidiState.Channel.MpeMember.NONE ? " MBR" : ""));
        gc.fillText(chLabel, xPad, y + rowH);
        y += rowH + 5;

        double lw = 90; double vw = 50;
        double vizW = w - lw - vw - xPad * 3;

        if (!isExpired(channel.pitchBend.current.time, now)) {
            drawItem(gc, "PitchBend", formatValue(channel.pitchBend.current.value), channel.pitchBend, 8192, 16383, true, theme, xPad, y, lw, vw, vizW, now, rowH, fs);
            y += rowH;
        }

        int count = 0;
        for (int j = 0; j < 128 && count < 6; j++) {
            MidiState.ChannelMessage cc = channel.controlChanges[j];
            if (!isExpired(cc.current.time, now)) {
                boolean bidir = (j == 10 || j == 8 || j == 9);
                drawItem(gc, getCcName(j), formatValue(cc.current.value), cc, 64, 127, bidir, theme, xPad, y, lw, vw, vizW, now, rowH, fs);
                y += rowH; count++;
            }
        }

        for (int j = 0; j < 128 && count < 9; j++) {
            if (!isExpired(channel.notesOn[j].current.time, now)) {
                drawItem(gc, "Note " + getNoteName(j), "ON", channel.notesOn[j], 0, 127, false, theme, xPad, y, lw, vw, vizW, now, rowH, fs);
                y += rowH; count++;
            }
        }
    }

    private void drawItem(GraphicsContext gc, String lbl, String val, MidiState.ChannelMessage msg, int ctr, int max, boolean bidir, Theme theme, double x, double y, double lw, double vw, double vizW, double now, double rh, double fs) {
        double glow = Math.max(0, 1.0 - (now - msg.lastChangeTime) * 5.0);
        gc.setFill(glow > 0 ? Color.WHITE : theme.label);
        gc.setFont(javafx.scene.text.Font.font("Monospaced", fs));
        gc.fillText(lbl, x, y + (rh * 0.7));
        gc.setFill(theme.data);
        gc.fillText(val, x + lw, y + (rh * 0.7));

        double vx = x + lw + vw + 5; double vy = y + (rh * 0.2); double vh = rh * 0.6;

        if (mode == VisualizationMode.BAR) {
            gc.setFill(theme.track); gc.fillRect(vx, vy, vizW, vh);
            gc.setFill(theme.controller);
            if (bidir) {
                double mid = vizW / 2.0; double v = ((double)msg.current.value - ctr) / ctr;
                gc.fillRect(vx + mid, vy, v * mid, vh);
            } else {
                double v = (double)msg.current.value / max; gc.fillRect(vx, vy, v * vizW, vh);
            }
            if (glow > 0) { gc.setGlobalAlpha(glow * 0.4); gc.setFill(Color.WHITE); gc.fillRect(vx, vy, vizW, vh); gc.setGlobalAlpha(1.0); }
        } else {
            gc.setStroke(theme.controller); gc.setLineWidth(1.5);
            double timeScale = 5.0; double lx = vx + vizW; double ly = vy + vh - ((double)msg.current.value / max) * vh;
            gc.beginPath(); gc.moveTo(lx, ly);
            for (MidiState.TimedValue tv : msg.history) {
                double dx = (now - tv.time) * (vizW / timeScale); double px = vx + vizW - dx;
                if (px < vx) break;
                double py = vy + vh - ((double)tv.value / max) * vh; gc.lineTo(px, py);
            }
            gc.stroke();
        }
    }

    private String getCcName(int cc) {
        return switch (cc) { case 1 -> "ModWheel"; case 7 -> "Volume"; case 10 -> "Pan"; case 64 -> "Sustain"; case 74 -> "Cutoff"; default -> "CC" + cc; };
    }
    private String formatValue(int v) { return settings.getNumberFormat() == AppSettings.NumberFormat.HEX ? String.format("%02X", v) : String.valueOf(v); }
    private boolean isExpired(double t, double now) { return t == 0 || (now - t) > EXPIRY_SEC; }
    private String getNoteName(int n) {
        int off = switch(settings.getOctaveOffset()) { case C3 -> -2; case C4 -> -1; case C5 -> 0; };
        String[] nms = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return nms[n % 12] + (n / 12 + off);
    }
    public void requestRedraw() { Platform.runLater(this::draw); }
}
