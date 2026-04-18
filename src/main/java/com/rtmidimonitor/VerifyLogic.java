package com.rtmidimonitor;

public class VerifyLogic {
    public static void main(String[] args) {
        System.out.println("Starting Logic Verification...");
        
        try {
            // 1. Test Parser
            byte[] noteOn = {(byte)0x90, 60, 100};
            MidiMessage msg = MidiParser.parse(noteOn);
            System.out.println("Parsed: " + msg);

            // 2. Test State (Per-Channel)
            MidiState state = new MidiState();
            state.handleMessage(msg, 1.0);
            
            MidiState.Channel ch = state.getChannels()[0];
            if (ch.notesOn[60].current.value == 100 && ch.notesOn[60].current.time == 1.0) {
                System.out.println("State Update Successful.");
            } else {
                throw new RuntimeException("State Update Failed.");
            }

            // 3. Test NoteOff
            byte[] noteOff = {(byte)0x80, 60, 0};
            MidiMessage offMsg = MidiParser.parse(noteOff);
            state.handleMessage(offMsg, 2.0);
            if (ch.notesOn[60].current.time == 0) {
                System.out.println("NoteOff Clearing Successful.");
            } else {
                throw new RuntimeException("NoteOff Clearing Failed.");
            }

            System.out.println("Logic Verification Successful.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
