package com.rtmidimonitor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.rtmidijava.RtMidiIn;
import org.rtmidijava.RtMidiFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class App extends Application {

    private final TextArea logArea = new TextArea();
    private final VBox devicesBox = new VBox(20);
    private final Map<String, MidiDeviceView> activeDevices = new HashMap<>();
    private final AppSettings settings = AppSettings.getInstance();
    private final VBox portListContainer = new VBox(5);

    private final CheckBox ignoreSysex = new CheckBox("Ignore Sysex");
    private final CheckBox ignoreClock = new CheckBox("Ignore Clock");
    private final CheckBox compactMode = new CheckBox("Compact Mode");
    
    private boolean simulating = false;
    private Button simulateBtn;
    private VisualizationMode currentMode = VisualizationMode.BAR;

    @Override
    public void start(Stage stage) {
        System.out.println("[CORE] Final UI Alignment...");
        loadSettings();

        BorderPane root = new BorderPane();

        // 1. Menu Bar
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit"); exitItem.setOnAction(e -> exitApp());
        fileMenu.getItems().add(exitItem);
        Menu helpMenu = new Menu("Help");
        MenuItem guideItem = new MenuItem("User Guide"); guideItem.setOnAction(e -> HelpView.show(stage));
        helpMenu.getItems().add(guideItem);
        menuBar.getMenus().addAll(fileMenu, helpMenu);
        root.setTop(menuBar);

        // 2. Sidebar
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(15));
        sidebar.setMinWidth(220);
        sidebar.setStyle("-fx-background-color: #2b2b2b; -fx-border-color: #444; -fx-border-width: 0 1 0 0;");

        Button modeBtn = new Button("Mode: BARS"); modeBtn.setMaxWidth(Double.MAX_VALUE);
        modeBtn.setOnAction(e -> {
            currentMode = (currentMode == VisualizationMode.BAR) ? VisualizationMode.GRAPH : VisualizationMode.BAR;
            modeBtn.setText("Mode: " + currentMode.name());
            activeDevices.values().forEach(v -> v.setVisualizationMode(currentMode));
        });

        compactMode.setStyle("-fx-text-fill: white;");
        compactMode.setSelected(settings.isCompactMode());
        compactMode.setOnAction(e -> settings.setCompactMode(compactMode.isSelected()));

        Button refreshBtn = new Button("Refresh Ports"); refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setOnAction(e -> refreshPorts());

        ignoreSysex.setSelected(true); ignoreClock.setSelected(true);
        ignoreSysex.setStyle("-fx-text-fill: white;"); ignoreClock.setStyle("-fx-text-fill: white;");
        ignoreSysex.setOnAction(e -> updateMidiFilters());
        ignoreClock.setOnAction(e -> updateMidiFilters());

        simulateBtn = new Button("Simulate MIDI"); simulateBtn.setMaxWidth(Double.MAX_VALUE);
        simulateBtn.setOnAction(e -> toggleSimulation());

        Button clearBtn = new Button("Clear Log"); clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> logArea.clear());

        sidebar.getChildren().addAll(
            new Label("VISUALS") {{ setStyle("-fx-text-fill: #888;"); }},
            modeBtn, compactMode, new Separator(),
            new Label("PORTS") {{ setStyle("-fx-text-fill: #888;"); }},
            refreshBtn, portListContainer, new Separator(),
            new Label("FILTERS") {{ setStyle("-fx-text-fill: #888;"); }},
            ignoreSysex, ignoreClock, new Separator(),
            simulateBtn, clearBtn
        );
        root.setLeft(sidebar);

        // 3. Center Content (The critical scrolling area)
        devicesBox.setStyle("-fx-background-color: #111;");
        devicesBox.setPadding(new Insets(10));
        
        ScrollPane deviceScroll = new ScrollPane(devicesBox);
        deviceScroll.setFitToWidth(true);
        deviceScroll.setFitToHeight(false); 
        deviceScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS); // Force for testing
        deviceScroll.setStyle("-fx-background: #111; -fx-background-color: #111; -fx-border-color: transparent;");

        logArea.setEditable(false);
        logArea.setStyle("-fx-control-inner-background: #000; -fx-text-fill: #0f0; -fx-font-family: 'Monospaced';");
        
        SplitPane mainSplit = new SplitPane(deviceScroll, logArea);
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.setDividerPositions(0.7);

        root.setCenter(mainSplit);

        Scene scene = new Scene(root, 1400, 900);
        stage.setTitle("RtMidiMonitor Pro");
        stage.setScene(scene);
        stage.setResizable(true); 
        stage.show();

        refreshPorts();
        stage.setOnCloseRequest(e -> exitApp());
    }

    private void exitApp() {
        saveSettings();
        simulating = false;
        activeDevices.values().forEach(MidiDeviceView::close);
        Platform.exit();
        System.exit(0);
    }

    private void refreshPorts() {
        portListContainer.getChildren().clear();
        new Thread(() -> {
            RtMidiIn probe = null;
            try {
                probe = RtMidiFactory.createDefaultIn();
                for (int i = 0; i < probe.getPortCount(); i++) {
                    String name = probe.getPortName(i);
                    int idx = i;
                    Platform.runLater(() -> {
                        CheckBox cb = new CheckBox(name); cb.setStyle("-fx-text-fill: #0f0;");
                        cb.setOnAction(e -> togglePort(name, idx, cb.isSelected()));
                        portListContainer.getChildren().add(cb);
                    });
                }
            } catch (Exception e) {}
            finally { if (probe != null) probe.closePort(); }
        }).start();
    }

    private void togglePort(String name, int index, boolean selected) {
        if (selected) {
            try {
                RtMidiIn in = (index == -1) ? null : RtMidiFactory.createDefaultIn();
                if (in != null) {
                    in.openPort(index, "Monitor-" + name);
                    in.ignoreTypes(ignoreSysex.isSelected(), ignoreClock.isSelected(), true);
                    in.setCallback((ts, bytes) -> {
                        MidiMessage msg = MidiParser.parse(bytes);
                        if (msg != null) Platform.runLater(() -> logArea.appendText("[" + name + "] " + msg.getClass().getSimpleName() + "\n"));
                    });
                }
                MidiDeviceView view = new MidiDeviceView(name, in);
                view.setVisualizationMode(currentMode);
                activeDevices.put(name, view);
                devicesBox.getChildren().add(view);
            } catch (Exception e) {}
        } else {
            MidiDeviceView view = activeDevices.remove(name);
            if (view != null) { view.close(); devicesBox.getChildren().remove(view); }
        }
    }

    private void toggleSimulation() {
        simulating = !simulating;
        simulateBtn.setText(simulating ? "Stop Simulation" : "Simulate MIDI");
        if (simulating) {
            Thread t = new Thread(() -> {
                while (simulating) {
                    Platform.runLater(this::runSimBurst);
                    try { Thread.sleep(1000); } catch (Exception e) { break; }
                }
            });
            t.setDaemon(true); t.start();
        }
    }

    private void runSimBurst() {
        if (activeDevices.isEmpty()) togglePort("Simulation-In", -1, true);
        java.util.Random r = new java.util.Random();
        double ts = System.nanoTime() / 1_000_000_000.0;
        activeDevices.values().forEach(v -> {
            for (int i = 0; i < 14; i++) {
                int ch = i; int note = 40 + r.nextInt(40);
                v.handleMockMessage(new MidiMessage.NoteOn(ch, note, 100), ts);
                new Thread(() -> { try { Thread.sleep(400); } catch (Exception e) {} Platform.runLater(() -> v.handleMockMessage(new MidiMessage.NoteOff(ch, note, 0), System.nanoTime()/1_000_000_000.0)); }).start();
            }
        });
    }

    private void updateMidiFilters() {
        activeDevices.values().forEach(v -> v.updateFilters(ignoreSysex.isSelected(), ignoreClock.isSelected(), true));
    }

    private void loadSettings() { try { Preferences p = Preferences.userNodeForPackage(App.class); settings.setCompactMode(p.getBoolean("compact", false)); } catch (Exception e) {} }
    private void saveSettings() { try { Preferences p = Preferences.userNodeForPackage(App.class); p.putBoolean("compact", settings.isCompactMode()); } catch (Exception e) {} }

    public static void main(String[] args) { launch(); }
}
