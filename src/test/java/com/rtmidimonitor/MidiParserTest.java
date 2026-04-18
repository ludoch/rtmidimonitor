package com.rtmidimonitor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MidiParserTest {
    @Test
    public void testNoteOn() {
        byte[] data = {(byte) 0x90, 0x3C, 0x40};
        MidiMessage msg = MidiParser.parse(data);
        assertTrue(msg instanceof MidiMessage.NoteOn);
        MidiMessage.NoteOn n = (MidiMessage.NoteOn) msg;
        assertEquals(0, n.channel());
        assertEquals(60, n.note());
        assertEquals(64, n.velocity());
    }

    @Test
    public void testNoteOff() {
        byte[] data = {(byte) 0x81, 0x40, 0x00};
        MidiMessage msg = MidiParser.parse(data);
        assertTrue(msg instanceof MidiMessage.NoteOff);
        MidiMessage.NoteOff n = (MidiMessage.NoteOff) msg;
        assertEquals(1, n.channel());
        assertEquals(64, n.note());
        assertEquals(0, n.velocity());
    }

    @Test
    public void testControlChange() {
        byte[] data = {(byte) 0xB2, 0x07, 0x7F};
        MidiMessage msg = MidiParser.parse(data);
        assertTrue(msg instanceof MidiMessage.ControlChange);
        MidiMessage.ControlChange c = (MidiMessage.ControlChange) msg;
        assertEquals(2, c.channel());
        assertEquals(7, c.controller());
        assertEquals(127, c.value());
    }

    @Test
    public void testRpn() {
        // RPN 00:00 (Pitch Bend Sensitivity)
        MidiParser.parse(new byte[]{(byte) 0xB0, 101, 0});
        MidiParser.parse(new byte[]{(byte) 0xB0, 100, 0});
        // Data Entry 2:0
        MidiParser.parse(new byte[]{(byte) 0xB0, 6, 2});
        MidiMessage msg = MidiParser.parse(new byte[]{(byte) 0xB0, 38, 0});
        
        assertTrue(msg instanceof MidiMessage.Rpn);
        MidiMessage.Rpn r = (MidiMessage.Rpn) msg;
        assertEquals(0, r.channel());
        assertEquals(0, r.parameter());
        assertEquals(256, r.value()); // (2 << 7) | 0
    }
}
