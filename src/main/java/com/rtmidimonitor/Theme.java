package com.rtmidimonitor;

import javafx.scene.paint.Color;

public class Theme {
    public final String name;
    public final Color background;
    public final Color sidebar;
    public final Color label;
    public final Color data;
    public final Color track;
    public final Color controller;
    public final Color positive;
    public final Color negative;
    public final Color separator;

    public Theme(String name, Color bg, Color side, Color lb, Color dt, Color tr, Color ctrl, Color pos, Color neg, Color sep) {
        this.name = name;
        this.background = bg;
        this.sidebar = side;
        this.label = lb;
        this.data = dt;
        this.track = tr;
        this.controller = ctrl;
        this.positive = pos;
        this.negative = neg;
        this.separator = sep;
    }

    public static final Theme DARK = new Theme(
        "Dark",
        Color.web("#1e1e1e"), Color.web("#2d2d2d"),
        Color.web("#aaaaaa"), Color.web("#ffffff"),
        Color.web("#333333"), Color.web("#00ff00"),
        Color.web("#00ff00"), Color.web("#ff0000"),
        Color.web("#444444")
    );

    public static final Theme LIGHT = new Theme(
        "Light",
        Color.web("#f5f5f5"), Color.web("#e0e0e0"),
        Color.web("#333333"), Color.web("#000000"),
        Color.web("#cccccc"), Color.web("#0078d7"),
        Color.web("#28a745"), Color.web("#dc3545"),
        Color.web("#dddddd")
    );
    
    public static Theme random() {
        java.util.Random r = new java.util.Random();
        Color bg = Color.rgb(r.nextInt(50), r.nextInt(50), r.nextInt(50));
        return new Theme(
            "Random",
            bg, bg.deriveColor(0, 1, 1.2, 1),
            Color.hsb(r.nextInt(360), 0.2, 0.8),
            Color.WHITE,
            bg.deriveColor(0, 1, 1.5, 1),
            Color.hsb(r.nextInt(360), 0.8, 0.9),
            Color.LIME, Color.RED,
            bg.deriveColor(0, 1, 2.0, 1)
        );
    }
}
