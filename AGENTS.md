# RtMidiMonitor Project Manifest (To Avoid Regressions)

## 1. Core MIDI Logic (Headless & Testable)
- **14-bit High-Res CC**: Parser must pair MSB (0-31) and LSB (32-63) into single 14-bit values (0-16383).
- **RPN/NRPN State Machine**: Full tracking of parameter selection and data entry, including "Null Reset" (127/127).
- **BPM Median Smoothing**: Clock BPM must be calculated using a median filter over a 48-pulse buffer to prevent jitter.
- **MPE Logic**: Detection of Manager/Member roles via RPN 6; state must propagate to all member channels.
- **Unified Timing**: All expiry and graph logic must use `System.nanoTime() / 1_000_000_000.0`.

## 2. UI Features (JavaFX)
- **Multi-Column Grid**: Channels must be rendered as individual "Cards" (approx 320x260) inside a `FlowPane` that wraps horizontally.
- **Draggable Resizing**: Main layout must use a vertical `SplitPane` allowing the user to drag and resize the Console Log height.
- **Vertical Scrolling**: The main `ScrollPane` must have `setFitToHeight(false)` to allow scrolling through unlimited channels.
- **Activity Glow**: Labels/Bars must flash (Global Alpha or White overlay) for 200ms upon data arrival.
- **Visualization Modes**:
    - **BAR**: Real-time levels with bidirectional (center-zero) support for Pan, Balance, and Pitch Bend.
    - **GRAPH**: 5-second scrolling historical view of data points.
- **Compact Mode**: Toggleable high-density layout with smaller fonts and row heights.

## 3. Critical Safety & Stability (Mandatory)
- **Native Bypass**: Index `-1` is reserved for "Virtual-Sim". Logic must NEVER call `openPort(-1)` on the native RtMidi library (causes hangs).
- **Daemon Threads**: All background threads (Simulation, Hardware Probing) must be `setDaemon(true)`.
- **Hard Exit**: `System.exit(0)` must be called on window close to release native MIDI hooks.
- **Thread Safety**: Every UI update from MIDI callbacks must be wrapped in `Platform.runLater`.

## 4. User Interaction
- **Persistent Settings**: Compact Mode, Octave Offset, and Formats must persist via `java.util.prefs`.
- **Keyboard Shortcuts**:
    - `SPACE`: Toggle Freeze.
    - `DELETE`: Clear Log.
    - `R`: Reset State.
    - `F`: Toggle Fullscreen.
- **Help Menu**: Non-modal "User Guide" window that does not block the main application.
