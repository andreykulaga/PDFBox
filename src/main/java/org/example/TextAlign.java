package org.example;

public enum TextAlign {
    LEFT,
    CENTER,
    RIGHT,
    TOP,
    BOTTOM;


    static TextAlign fromStringToTextAlign(String text) {
        if (text.equalsIgnoreCase("LEFT")) {
            return LEFT;
        }
        if (text.equalsIgnoreCase( "RIGHT")) {
            return RIGHT;

        }
        if (text.equalsIgnoreCase("TOP")) {
            return TOP;
        }
        if (text.equalsIgnoreCase( "BOTTOM")) {
            return BOTTOM;

        }
        else {
            return CENTER;
        }
    }
}
