package org.example;

public enum TextAlign {
    LEFT,
    CENTER,
    RIGHT;

    static TextAlign fromStringToTextAlign(String text) {
        if (text.equalsIgnoreCase("LEFT")) {
            return LEFT;
        }
        if (text.equalsIgnoreCase( "RIGHT")) {
            return RIGHT;

        }
        else {
            return CENTER;
        }
    }
}
