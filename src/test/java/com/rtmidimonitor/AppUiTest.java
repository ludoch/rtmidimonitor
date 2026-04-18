package com.rtmidimonitor;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AppUiTest {

    @Test
    public void testMidiParsing() {
        MidiState state = new MidiState();
        state.handleMessage(new MidiMessage.NoteOn(0, 60, 100), 1000.0);
        assertThat(state.getChannels()[0].notesOn[60].current.value).isEqualTo(100);
    }

    @Test
    public void test14BitParsing() {
        MidiMessage m1 = MidiParser.parse(new byte[]{(byte)0xB0, 0x07, 0x40}); 
        MidiMessage m2 = MidiParser.parse(new byte[]{(byte)0xB0, 0x27, 0x01});
        assertThat(m2).isInstanceOf(MidiMessage.HrControlChange.class);
        assertThat(((MidiMessage.HrControlChange)m2).value()).isEqualTo(8193);
    }

    @Test
    public void testStructuralIntegrity() throws Exception {
        // Source code verification to prevent regressions like missing menus
        String content = new String(Files.readAllBytes(Paths.get("src/main/java/com/rtmidimonitor/App.java")));
        assertThat(content).contains("Menu helpMenu = new Menu(\"Help\")");
        assertThat(content).contains("MenuItem guideItem = new MenuItem(\"User Guide\")");
        assertThat(content).contains("HelpView.show(stage)");
    }
}
