# Design Document: RtMidiMonitor Port

## Goal
Port the `ShowMidi` tool to a modern Java 25 + JavaFX 25 environment, using the `rtmidijava` library for MIDI access.

## Architecture

### 1. MIDI Layer (`rtmidijava`)
- Utilize `rtmidijava` for cross-platform MIDI input/output.
- Handle device discovery and connection lifecycle.

### 2. Core Logic (Port from ShowMidi)
- Message parsing: Convert raw MIDI bytes into human-readable events.
- State management: Keep track of active notes, controller values, etc.

### 3. UI Layer (JavaFX)
- Main window with a list of MIDI devices.
- Real-time display area for MIDI events.
- Visualizers for Note On/Off, CC changes, Pitch Bend, etc.

## Porting Steps

1.  **Analyze ShowMidi Source**: Identify the core message processing logic and UI components.
2.  **Integrate `rtmidijava`**:
    - Configure Maven to pull `rtmidijava` from GitHub Packages. Note: You may need to configure your `settings.xml` with a GitHub personal access token to access the repository.
    - Implement a basic MIDI listener that logs events to the console.
3.  **Implement Message Parser**:
    - Translate ShowMidi's message handling logic into Java.
4.  **Build JavaFX UI Skeleton**:
    - Create a responsive layout for device selection and monitoring.
5.  **Connect UI to MIDI Events**:
    - Use JavaFX properties and observable lists to update the UI in real-time.
6.  **Refinement**:
    - Add filtering capabilities.
    - Improve visual representation (e.g., piano roll or status bars).

## Testing Plan

1.  **Unit Testing**:
    - Test the message parser with various MIDI byte sequences.
    - Test state management logic (e.g., ensuring notes are cleared on Note Off).
2.  **Integration Testing**:
    - Verify MIDI device discovery works on different OS platforms.
    - Verify message reception from physical/virtual MIDI controllers.
3.  **UI Testing**:
    - Ensure the UI remains responsive even during high MIDI traffic.
    - Check layout on different screen resolutions.
4.  **User Acceptance**:
    - Compare functionality with the original ShowMidi tool.
