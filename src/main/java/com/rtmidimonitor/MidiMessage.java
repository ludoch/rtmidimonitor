package com.rtmidimonitor;

public sealed interface MidiMessage {
    int channel();

    record NoteOn(int channel, int note, int velocity) implements MidiMessage {}
    record NoteOff(int channel, int note, int velocity) implements MidiMessage {}
    record ControlChange(int channel, int controller, int value) implements MidiMessage {}
    record PitchBend(int channel, int value) implements MidiMessage {}
    record ProgramChange(int channel, int program) implements MidiMessage {}
    record PolyAftertouch(int channel, int note, int pressure) implements MidiMessage {}
    record ChannelAftertouch(int channel, int pressure) implements MidiMessage {}
    record Rpn(int channel, int parameter, int value) implements MidiMessage {}
    record Nrpn(int channel, int parameter, int value) implements MidiMessage {}
    record Sysex(byte[] data) implements MidiMessage {
        @Override public int channel() { return -1; }
    }
    record SystemMessage(int type, byte[] data) implements MidiMessage {
        @Override public int channel() { return -1; }
    }
    record Unknown(byte[] data) implements MidiMessage {
        @Override public int channel() { return -1; }
    }
}
