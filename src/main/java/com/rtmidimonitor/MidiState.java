package com.rtmidimonitor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MidiState {
    
    public static class TimedValue {
        public double time;
        public int value;
        
        public TimedValue(double time, int value) {
            this.time = time;
            this.value = value;
        }
    }

    public static class ChannelMessage {
        public TimedValue current = new TimedValue(0, 0);
        public final ObservableList<TimedValue> history = FXCollections.observableArrayList();
        
        public void update(double time, int value) {
            if (current.time > 0) {
                history.add(0, new TimedValue(current.time, current.value));
                if (history.size() > 500) { // Limit history
                    history.remove(500, history.size());
                }
            }
            current.time = time;
            current.value = value;
        }
        
        public void reset() {
            current = new TimedValue(0, 0);
            history.clear();
        }
    }

    public static class Channel {
        public final int number;
        public double lastUpdateTime = 0;
        
        public final ChannelMessage[] notesOn = new ChannelMessage[128];
        public final ChannelMessage[] notesOff = new ChannelMessage[128];
        public final ChannelMessage[] polyPressure = new ChannelMessage[128];
        public final ChannelMessage[] controlChanges = new ChannelMessage[128];
        public final ChannelMessage programChange = new ChannelMessage();
        public final ChannelMessage channelPressure = new ChannelMessage();
        public final ChannelMessage pitchBend = new ChannelMessage();
        
        public final ObservableMap<Integer, ChannelMessage> hrccs = FXCollections.observableHashMap();
        public final ObservableMap<Integer, ChannelMessage> rpns = FXCollections.observableHashMap();
        public final ObservableMap<Integer, ChannelMessage> nrpns = FXCollections.observableHashMap();

        public Channel(int number) {
            this.number = number;
            for (int i = 0; i < 128; i++) {
                notesOn[i] = new ChannelMessage();
                notesOff[i] = new ChannelMessage();
                polyPressure[i] = new ChannelMessage();
                controlChanges[i] = new ChannelMessage();
            }
        }
        
        public void reset() {
            lastUpdateTime = 0;
            for (int i = 0; i < 128; i++) {
                notesOn[i].reset();
                notesOff[i].reset();
                polyPressure[i].reset();
                controlChanges[i].reset();
            }
            programChange.reset();
            channelPressure.reset();
            pitchBend.reset();
            hrccs.clear();
            rpns.clear();
            nrpns.clear();
        }
    }

    private final Channel[] channels = new Channel[16];
    private double lastClockTime = 0;
    private final List<Double> clockIntervals = new ArrayList<>();
    private double bpm = 0;

    public MidiState() {
        for (int i = 0; i < 16; i++) {
            channels[i] = new Channel(i);
        }
    }

    public void handleMessage(MidiMessage msg, double timeStamp) {
        if (msg instanceof MidiMessage.SystemMessage s && s.type() == 0xF8) {
            handleClock(timeStamp);
            return;
        }

        int chIdx = msg.channel();
        if (chIdx < 0 || chIdx >= 16) return;
        
        Channel channel = channels[chIdx];
        channel.lastUpdateTime = timeStamp;
        
        Platform.runLater(() -> {
            if (msg instanceof MidiMessage.NoteOn n) {
                channel.notesOn[n.note()].update(timeStamp, n.velocity());
                channel.notesOff[n.note()].current.time = 0; // Clear note off
            } else if (msg instanceof MidiMessage.NoteOff n) {
                channel.notesOff[n.note()].update(timeStamp, n.velocity());
            } else if (msg instanceof MidiMessage.ControlChange c) {
                channel.controlChanges[c.controller()].update(timeStamp, c.value());
            } else if (msg instanceof MidiMessage.PitchBend p) {
                channel.pitchBend.update(timeStamp, p.value());
            } else if (msg instanceof MidiMessage.ChannelAftertouch a) {
                channel.channelPressure.update(timeStamp, a.pressure());
            } else if (msg instanceof MidiMessage.PolyAftertouch a) {
                channel.polyPressure[a.note()].update(timeStamp, a.pressure());
            } else if (msg instanceof MidiMessage.ProgramChange p) {
                channel.programChange.update(timeStamp, p.program());
            } else if (msg instanceof MidiMessage.Rpn r) {
                channel.rpns.computeIfAbsent(r.parameter(), k -> new ChannelMessage()).update(timeStamp, r.value());
            } else if (msg instanceof MidiMessage.Nrpn n) {
                channel.nrpns.computeIfAbsent(n.parameter(), k -> new ChannelMessage()).update(timeStamp, n.value());
            }
        });
    }

    private void handleClock(double timeStamp) {
        if (lastClockTime > 0) {
            double interval = timeStamp - lastClockTime;
            if (interval > 0) {
                clockIntervals.add(interval);
                if (clockIntervals.size() > 24) {
                    clockIntervals.remove(0);
                }
                if (clockIntervals.size() == 24) {
                    double avgInterval = clockIntervals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    bpm = 60.0 / (avgInterval * 24.0);
                }
            }
        }
        lastClockTime = timeStamp;
    }

    public Channel[] getChannels() {
        return channels;
    }

    public double getBpm() {
        return bpm;
    }

    public void reset() {
        for (Channel c : channels) {
            c.reset();
        }
        clockIntervals.clear();
        lastClockTime = 0;
        bpm = 0;
    }
}
