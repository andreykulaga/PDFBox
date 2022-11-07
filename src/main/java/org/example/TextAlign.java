package org.example;

public enum TextAlign {
    LEFT,
    CENTER,
    RIGHT;

    static TextAlign fromStringToTextAlign(String text) {
        if (text.equals(LEFT)) {
            return LEFT;
        }
        if (text.equals(RIGHT)) {
            return RIGHT;

        }
        else {
            return CENTER;
        }
    }
}
