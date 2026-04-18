package com.rtmidimonitor;

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
        public TimedValue(double time, int value) { this.time = time; this.value = value; }
    }

    public static class ChannelMessage {
        public TimedValue current = new TimedValue(0, 0);
        public double lastChangeTime = 0;
        public final ObservableList<TimedValue> history = FXCollections.observableArrayList();
        
        public void update(double time, int value) {
            this.lastChangeTime = time;
            if (current.time > 0) {
                history.add(0, new TimedValue(current.time, current.value));
                if (history.size() > 500) history.remove(500, history.size());
            }
            current.time = time;
            current.value = value;
        }
        
        public void reset() { current = new TimedValue(0, 0); lastChangeTime = 0; history.clear(); }
    }

    public static class Channel {
        public final int number;
        public double lastUpdateTime = 0;
        public enum MpeMember { NONE, LOWER, UPPER }
        public boolean mpeManager = false;
        public MpeMember mpeMember = MpeMember.NONE;
        
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
                notesOn[i] = new ChannelMessage(); notesOff[i] = new ChannelMessage();
                polyPressure[i] = new ChannelMessage(); controlChanges[i] = new ChannelMessage();
            }
        }
        
        public void reset() {
            lastUpdateTime = 0; mpeManager = false; mpeMember = MpeMember.NONE;
            for (int i = 0; i < 128; i++) {
                notesOn[i].reset(); notesOff[i].reset();
                polyPressure[i].reset(); controlChanges[i].reset();
            }
            programChange.reset(); channelPressure.reset(); pitchBend.reset();
            hrccs.clear(); rpns.clear(); nrpns.clear();
        }
    }

    public static class SysexData {
        public double time = 0;
        public byte[] data = new byte[20];
        public int length = 0;
        public void update(double t, byte[] d) {
            this.time = t; this.length = Math.min(d.length, 20);
            System.arraycopy(d, 0, this.data, 0, this.length);
        }
        public void reset() { time = 0; length = 0; }
    }

    public static class ClockState {
        public double timeBpm = 0, timeStart = 0, timeContinue = 0, timeStop = 0, bpm = 0;
        public void reset() { timeBpm = timeStart = timeContinue = timeStop = 0; bpm = 0; }
    }

    private final Channel[] channels = new Channel[16];
    private final SysexData sysex = new SysexData();
    private final ClockState clock = new ClockState();
    private double lastClockTime = 0;
    private final List<Double> clockIntervals = new ArrayList<>();

    public MidiState() { for (int i = 0; i < 16; i++) channels[i] = new Channel(i); }

    public void handleMessage(MidiMessage msg, double timeStamp) {
        if (msg instanceof MidiMessage.SystemMessage s) {
            if (s.type() == 0xF8) { handleClock(timeStamp); return; }
            else if (s.type() == 0xFA) { clock.timeStart = timeStamp; return; }
            else if (s.type() == 0xFB) { clock.timeContinue = timeStamp; return; }
            else if (s.type() == 0xFC) { clock.timeStop = timeStamp; return; }
        }
        if (msg instanceof MidiMessage.Sysex s) { sysex.update(timeStamp, s.data()); return; }

        int chIdx = msg.channel();
        if (chIdx < 0 || chIdx >= 16) return;
        Channel channel = channels[chIdx];
        channel.lastUpdateTime = timeStamp;
        
        if (msg instanceof MidiMessage.NoteOn n) { channel.notesOn[n.note()].update(timeStamp, n.velocity()); channel.notesOff[n.note()].current.time = 0; }
        else if (msg instanceof MidiMessage.NoteOff n) { channel.notesOff[n.note()].update(timeStamp, n.velocity()); channel.notesOn[n.note()].current.time = 0; }
        else if (msg instanceof MidiMessage.ControlChange c) channel.controlChanges[c.controller()].update(timeStamp, c.value());
        else if (msg instanceof MidiMessage.HrControlChange c) channel.hrccs.computeIfAbsent(c.controller(), k -> new ChannelMessage()).update(timeStamp, c.value());
        else if (msg instanceof MidiMessage.PitchBend p) channel.pitchBend.update(timeStamp, p.value());
        else if (msg instanceof MidiMessage.ChannelAftertouch a) channel.channelPressure.update(timeStamp, a.pressure());
        else if (msg instanceof MidiMessage.PolyAftertouch a) channel.polyPressure[a.note()].update(timeStamp, a.pressure());
        else if (msg instanceof MidiMessage.ProgramChange p) channel.programChange.update(timeStamp, p.program());
        else if (msg instanceof MidiMessage.Rpn r) {
            channel.rpns.computeIfAbsent(r.parameter(), k -> new ChannelMessage()).update(timeStamp, r.value());
            if (r.parameter() == 6) handleMpeActivation(timeStamp, channel, (r.value() >> 7) & 0x7F);
        } else if (msg instanceof MidiMessage.Nrpn n) channel.nrpns.computeIfAbsent(n.parameter(), k -> new ChannelMessage()).update(timeStamp, n.value());
    }

    private void handleClock(double timeStamp) {
        if (lastClockTime > 0) {
            double interval = timeStamp - lastClockTime;
            if (interval > 0.005 && interval < 0.5) {
                clockIntervals.add(interval);
                if (clockIntervals.size() > 48) clockIntervals.remove(0);
                if (clockIntervals.size() >= 24) {
                    List<Double> sorted = new ArrayList<>(clockIntervals);
                    java.util.Collections.sort(sorted);
                    double median = sorted.get(sorted.size() / 2);
                    clock.bpm = 60.0 / (median * 24.0);
                    clock.timeBpm = timeStamp;
                }
            }
        }
        lastClockTime = timeStamp;
    }

    private void handleMpeActivation(double t, Channel channel, int range) {
        if (channel.number == 0) { 
            if (range == 0) {
                for (int i = 1; i <= 14; i++) { if (channels[i].mpeMember == Channel.MpeMember.LOWER) { channels[i].mpeMember = Channel.MpeMember.NONE; channels[i].lastUpdateTime = t; } }
                channel.mpeManager = false; channel.mpeMember = Channel.MpeMember.NONE;
            } else {
                for (int i = 1; i <= range && i <= 14; i++) { channels[i].mpeMember = Channel.MpeMember.LOWER; channels[i].lastUpdateTime = t; }
                channel.mpeManager = true; channel.mpeMember = Channel.MpeMember.LOWER;
            }
        } else if (channel.number == 15) {
            if (range == 0) {
                for (int i = 14; i >= 1; i--) { if (channels[i].mpeMember == Channel.MpeMember.UPPER) { channels[i].mpeMember = Channel.MpeMember.NONE; channels[i].lastUpdateTime = t; } }
                channel.mpeManager = false; channel.mpeMember = Channel.MpeMember.NONE;
            } else {
                for (int i = 1; i <= range && i <= 14; i++) { channels[15-i].mpeMember = Channel.MpeMember.UPPER; channels[15-i].lastUpdateTime = t; }
                channel.mpeManager = true; channel.mpeMember = Channel.MpeMember.UPPER;
            }
        }
    }

    public Channel[] getChannels() { return channels; }
    public SysexData getSysex() { return sysex; }
    public ClockState getClock() { return clock; }
    public double getBpm() { return clock.bpm; }

    public void reset() {
        for (Channel c : channels) c.reset();
        sysex.reset(); clock.reset(); clockIntervals.clear(); lastClockTime = 0;
    }
}
