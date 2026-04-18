# RtMidiMonitor

A JavaFX port of the ShowMidi tool, utilizing the `rtmidijava` portable MIDI library.

## Prerequisites

- Java 25
- Maven

## Build and Run

To build the project:

```bash
mvn clean install
```

To run the JavaFX application:

```bash
mvn javafx:run
```

## Features

- **Real-time MIDI monitoring**: Detailed log with hex bytes and human-readable descriptions.
- **Visualizer**: High-performance canvas showing active notes, CC values, and Pitch Bend.
- **BPM Estimation**: Automatic calculation of BPM from incoming MIDI Clock messages.
- **Filtering**: Easily ignore Sysex, Clock, or Active Sensing messages to declutter the log.
- **Port Management**: Dynamic discovery and selection of MIDI input ports.
- **Cross-platform**: Pure Java implementation using `rtmidijava` and JavaFX, no native binaries required.
