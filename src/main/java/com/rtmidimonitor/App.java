package com.rtmidimonitor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import org.rtmidijava.RtMidiIn;
import org.rtmidijava.RtMidiFactory;

import java.util.HashMap;
import java.util.Map;

public class App extends Application {

    private final TextArea logArea = new TextArea();
    private final VBox portListContainer = new VBox(5);
    private final FlowPane deviceViewsContainer = new FlowPane(10, 10);
    private final Map<String, MidiDeviceView> activeDevices = new HashMap<>();
    private final AppSettings settings = AppSettings.getInstance();

    private final CheckBox ignoreSysex = new CheckBox("Ignore Sysex");
    private final CheckBox ignoreClock = new CheckBox("Ignore Clock");
    private final CheckBox ignoreSense = new CheckBox("Ignore Active Sensing");

    private boolean simulating = false;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        // Top: MenuBar
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> exitApp());
        fileMenu.getItems().add(exitItem);
        menuBar.getMenus().add(fileMenu);
        root.setTop(menuBar);

        // Left: Sidebar (Restored Feature-Rich Sidebar)
        SidebarView sidebar = new SidebarView();
        sidebar.setOnVisualizationModeChanged(mode -> {
            activeDevices.values().forEach(v -> v.setVisualizationMode(mode));
        });
        sidebar.setOnResetAll(() -> {
            activeDevices.values().forEach(MidiDeviceView::resetState);
            Platform.runLater(() -> {
                logArea.clear();
                appendToLog("State reset.");
            });
        });
        sidebar.setOnSimulate(this::toggleSimulation);
        sidebar.setOnFreezeChanged(frozen -> {
            activeDevices.values().forEach(v -> v.setFrozen(frozen));
        });

        // Sidebar custom content (Port selection and Filters)
        VBox customSidebarContent = new VBox(10);
        Button refreshButton = new Button("Refresh Ports");
        refreshButton.setMaxWidth(Double.MAX_VALUE);
        refreshButton.setOnAction(e -> refreshPorts());

        ignoreSysex.setSelected(true);
        ignoreClock.setSelected(true);
        ignoreSense.setSelected(true);
        ignoreSysex.setOnAction(e -> updateMidiFilters());
        ignoreClock.setOnAction(e -> updateMidiFilters());
        ignoreSense.setOnAction(e -> updateMidiFilters());

        Button clearLogBtn = new Button("Clear Log");
        clearLogBtn.setMaxWidth(Double.MAX_VALUE);
        clearLogBtn.setOnAction(e -> logArea.clear());

        ScrollPane portScroll = new ScrollPane(portListContainer);
        portScroll.setPrefHeight(150);
        portScroll.setFitToWidth(true);

        customSidebarContent.getChildren().addAll(
            new Label("Available Ports:"), refreshButton, portScroll,
            new Label("Filters:"), ignoreSysex, ignoreClock, ignoreSense,
            clearLogBtn
        );
        sidebar.addCustomContent(customSidebarContent);
        root.setLeft(sidebar);

        // Center: Main area
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);

        ScrollPane deviceScrollPane = new ScrollPane(deviceViewsContainer);
        deviceScrollPane.setFitToWidth(true);
        deviceScrollPane.setFitToHeight(true);

        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: 'Monospaced';");
        
        splitPane.getItems().addAll(deviceScrollPane, logArea);
        splitPane.setDividerPositions(0.6);
        root.setCenter(splitPane);

        // Theme sync
        Runnable updateTheme = () -> {
            Theme theme = settings.getTheme();
            String bgHex = toHex(theme.background);
            String dataHex = toHex(theme.data);
            String labelHex = toHex(theme.label);
            
            deviceScrollPane.setStyle("-fx-background: " + bgHex + "; -fx-background-color: " + bgHex + ";");
            deviceViewsContainer.setStyle("-fx-background-color: " + bgHex + ";");
            logArea.setStyle("-fx-control-inner-background: " + bgHex + "; -fx-text-fill: " + dataHex + ";");
            
            customSidebarContent.getChildren().forEach(n -> {
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

        appendToLog("RtMidiMonitor Started. Use the sidebar to simulate or select ports.");
        refreshPorts();

        stage.setOnCloseRequest(e -> exitApp());
    }

    private void exitApp() {
        simulating = false;
        activeDevices.values().forEach(MidiDeviceView::close);
        Platform.exit();
        System.exit(0);
    }

    private void appendToLog(String msg) {
        Platform.runLater(() -> {
            logArea.appendText(msg + "\n");
        });
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X", 
            (int)(color.getRed() * 255), (int)(color.getGreen() * 255), (int)(color.getBlue() * 255));
    }

    private void refreshPorts() {
        portListContainer.getChildren().clear();
        java.io.File alsaSeq = new java.io.File("/dev/snd/seq");
        if (System.getProperty("os.name").toLowerCase().contains("linux") && alsaSeq.exists() && !alsaSeq.canRead()) {
            appendToLog("ALSA Permission Denied. Simulation mode only.");
            return;
        }

        RtMidiIn probe = null;
        try {
            probe = RtMidiFactory.createDefaultIn();
            int count = probe.getPortCount();
            for (int i = 0; i < count; i++) {
                String name = probe.getPortName(i);
                final int idx = i;
                CheckBox cb = new CheckBox(name);
                cb.setOnAction(e -> togglePort(name, idx, cb.isSelected()));
                portListContainer.getChildren().add(cb);
            }
        } catch (Exception e) {
            appendToLog("MIDI initialization failed: " + e.getMessage());
        } finally {
            if (probe != null) probe.closePort();
        }
    }

    private void togglePort(String name, int index, boolean selected) {
        if (selected) {
            try {
                RtMidiIn midiIn = null;
                if (index != -1) {
                    midiIn = RtMidiFactory.createDefaultIn();
                    midiIn.openPort(index, "RtMidiMonitor-" + name);
                    midiIn.ignoreTypes(ignoreSysex.isSelected(), ignoreClock.isSelected(), ignoreSense.isSelected());
                    final String finalName = name;
                    midiIn.setCallback((ts, bytes) -> {
                        MidiMessage msg = MidiParser.parse(bytes);
                        if (msg != null) {
                            appendToLog("[" + finalName + "] " + formatMessage(msg, bytes));
                        }
                    });
                }
                MidiDeviceView view = new MidiDeviceView(name, midiIn);
                activeDevices.put(name, view);
                deviceViewsContainer.getChildren().add(view);
            } catch (Exception e) {
                appendToLog("Error: " + e.getMessage());
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
        for (byte b : raw) sb.append(String.format("%02X ", b));
        sb.append("| ");
        if (msg instanceof MidiMessage.NoteOn n) sb.append("Note ON ").append(getNoteName(n.note())).append(" Vel ").append(n.velocity());
        else if (msg instanceof MidiMessage.NoteOff n) sb.append("Note OFF ").append(getNoteName(n.note()));
        else sb.append(msg.getClass().getSimpleName());
        return sb.toString();
    }

    private String getNoteName(int note) {
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return names[note % 12] + (note / 12 - 1);
    }

    private void updateMidiFilters() {
        activeDevices.values().forEach(v -> v.updateFilters(ignoreSysex.isSelected(), ignoreClock.isSelected(), ignoreSense.isSelected()));
        appendToLog("Filters updated.");
    }

    private void toggleSimulation() {
        simulating = !simulating;
        if (simulating) {
            appendToLog("Simulation started.");
            new Thread(() -> {
                while (simulating) {
                    Platform.runLater(this::runSimStep);
                    try { Thread.sleep(1000); } catch (Exception e) { break; }
                }
            }).start();
        } else {
            appendToLog("Simulation stopped.");
        }
    }

    private void runSimStep() {
        if (activeDevices.isEmpty()) togglePort("Virtual Simulation", -1, true);
        java.util.Random r = new java.util.Random();
        activeDevices.values().forEach(v -> {
            double ts = System.nanoTime() / 1_000_000_000.0;
            int ch = r.nextInt(16);
            int note = 40 + r.nextInt(40);
            
            MidiMessage m = new MidiMessage.NoteOn(ch, note, 100);
            v.handleMockMessage(m, ts);
            appendToLog("[SIM] " + formatMessage(m, new byte[]{(byte)(0x90|ch), (byte)note, 100}));
            
            new Thread(() -> {
                try { Thread.sleep(600); } catch (Exception e) {}
                Platform.runLater(() -> v.handleMockMessage(new MidiMessage.NoteOff(ch, note, 0), System.nanoTime() / 1_000_000_000.0));
            }).start();
        });
    }

    public static void main(String[] args) { launch(); }
}
