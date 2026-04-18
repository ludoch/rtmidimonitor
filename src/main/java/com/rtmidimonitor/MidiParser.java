package com.rtmidimonitor;

public class MidiParser {
    private static final int[] lastRpnMsb = new int[16];
    private static final int[] lastRpnLsb = new int[16];
    private static final int[] lastNrpnMsb = new int[16];
    private static final int[] lastNrpnLsb = new int[16];
    private static final int[] lastDataMsb = new int[16];

    static {
        for (int i = 0; i < 16; i++) {
            lastRpnMsb[i] = 127;
            lastRpnLsb[i] = 127;
            lastNrpnMsb[i] = 127;
            lastNrpnLsb[i] = 127;
            lastDataMsb[i] = -1;
        }
    }

    public static MidiMessage parse(byte[] data) {
        if (data == null || data.length == 0) return null;

        int status = data[0] & 0xFF;
        if (status >= 0xF0) {
            if (status == 0xF0) return new MidiMessage.Sysex(data);
            if (status == 0xF8) return new MidiMessage.SystemMessage(0xF8, data);
            if (status == 0xFA) return new MidiMessage.SystemMessage(0xFA, data);
            if (status == 0xFB) return new MidiMessage.SystemMessage(0xFB, data);
            if (status == 0xFC) return new MidiMessage.SystemMessage(0xFC, data);
            return new MidiMessage.SystemMessage(status, data);
        }

        int command = status & 0xF0;
        int channel = status & 0x0F;

        switch (command) {
            case 0x80:
                if (data.length >= 3) return new MidiMessage.NoteOff(channel, data[1] & 0x7F, data[2] & 0x7F);
                break;
            case 0x90:
                if (data.length >= 3) {
                    int velocity = data[2] & 0x7F;
                    if (velocity == 0) return new MidiMessage.NoteOff(channel, data[1] & 0x7F, 0);
                    return new MidiMessage.NoteOn(channel, data[1] & 0x7F, velocity);
                }
                break;
            case 0xA0:
                if (data.length >= 3) return new MidiMessage.PolyAftertouch(channel, data[1] & 0x7F, data[2] & 0x7F);
                break;
            case 0xB0:
                if (data.length >= 3) {
                    int controller = data[1] & 0x7F;
                    int value = data[2] & 0x7F;

                    // RPN / NRPN state machine
                    if (controller == 101) lastRpnMsb[channel] = value;
                    else if (controller == 100) lastRpnLsb[channel] = value;
                    else if (controller == 99) lastNrpnMsb[channel] = value;
                    else if (controller == 98) lastNrpnLsb[channel] = value;
                    else if (controller == 6) { // Data Entry MSB
                        lastDataMsb[channel] = value;
                    }
                    else if (controller == 38) { // Data Entry LSB
                        int fullValue = (lastDataMsb[channel] << 7) | value;
                        if (lastRpnMsb[channel] != 127 || lastRpnLsb[channel] != 127) {
                            return new MidiMessage.Rpn(channel, (lastRpnMsb[channel] << 7) | lastRpnLsb[channel], fullValue);
                        } else if (lastNrpnMsb[channel] != 127 || lastNrpnLsb[channel] != 127) {
                            return new MidiMessage.Nrpn(channel, (lastNrpnMsb[channel] << 7) | lastNrpnLsb[channel], fullValue);
                        }
                    }

                    return new MidiMessage.ControlChange(channel, controller, value);
                }
                break;
            case 0xC0:
                if (data.length >= 2) return new MidiMessage.ProgramChange(channel, data[1] & 0x7F);
                break;
            case 0xD0:
                if (data.length >= 2) return new MidiMessage.ChannelAftertouch(channel, data[1] & 0x7F);
                break;
            case 0xE0:
                if (data.length >= 3) {
                    int value = (data[1] & 0x7F) | ((data[2] & 0x7F) << 7);
                    return new MidiMessage.PitchBend(channel, value);
                }
                break;
        }

        return new MidiMessage.Unknown(data);
    }
}
