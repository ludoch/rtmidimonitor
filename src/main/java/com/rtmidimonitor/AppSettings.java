package com.rtmidimonitor;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class AppSettings {
    public enum NumberFormat { DECIMAL, HEX }
    public enum NoteFormat { NAME, NUMBER }

    private static final AppSettings instance = new AppSettings();

    private final ObjectProperty<NumberFormat> numberFormat = new SimpleObjectProperty<>(NumberFormat.DECIMAL);
    private final ObjectProperty<NoteFormat> noteFormat = new SimpleObjectProperty<>(NoteFormat.NAME);
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(Theme.DARK);

    private AppSettings() {}

    public static AppSettings getInstance() {
        return instance;
    }

    public ObjectProperty<NumberFormat> numberFormatProperty() { return numberFormat; }
    public NumberFormat getNumberFormat() { return numberFormat.get(); }
    public void setNumberFormat(NumberFormat format) { numberFormat.set(format); }

    public ObjectProperty<NoteFormat> noteFormatProperty() { return noteFormat; }
    public NoteFormat getNoteFormat() { return noteFormat.get(); }
    public void setNoteFormat(NoteFormat format) { noteFormat.set(format); }

    public ObjectProperty<Theme> themeProperty() { return theme; }
    public Theme getTheme() { return theme.get(); }
    public void setTheme(Theme theme) { this.theme.set(theme); }
}
