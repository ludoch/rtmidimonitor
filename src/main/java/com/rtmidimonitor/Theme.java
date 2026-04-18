package com.rtmidimonitor;

import javafx.scene.paint.Color;

public class Theme {
    public final String name;
    public final Color background;
    public final Color sidebar;
    public final Color separator;
    public final Color track;
    public final Color label;
    public final Color data;
    public final Color positive;
    public final Color negative;
    public final Color controller;

    public Theme(String name, Color background, Color sidebar, Color separator, Color track, Color label, Color data, Color positive, Color negative, Color controller) {
        this.name = name;
        this.background = background;
        this.sidebar = sidebar;
        this.separator = separator;
        this.track = track;
        this.label = label;
        this.data = data;
        this.positive = positive;
        this.negative = negative;
        this.controller = controller;
    }

    public static final Theme DARK = new Theme(
        "Dark",
        Color.web("#29272B"),
        Color.web("#201E21"),
        Color.web("#66606B"),
        Color.web("#201E21"),
        Color.web("#66606B"),
        Color.web("#FFFFFF"),
        Color.web("#66ADF3"),
        Color.web("#D8414E"),
        Color.web("#FFAB2B")
    );

    public static final Theme LIGHT = new Theme(
        "Light",
        Color.web("#F2F2F2"),
        Color.web("#FFFFFF"),
        Color.web("#A0A0A0"),
        Color.web("#FFFFFF"),
        Color.web("#A0A0A0"),
        Color.web("#222222"),
        Color.web("#66ADF3"),
        Color.web("#D8414E"),
        Color.web("#FFAB2B")
    );
}
