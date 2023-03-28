package org.example;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FloatFormatter {
    public static String format(Float fl, String columnName, Configuration configuration) {

        String result = new DecimalFormat(configuration.getTextFormat().get(columnName), new DecimalFormatSymbols(Locale.ENGLISH)).format(fl);
        if (fl < 0 && configuration.getNegativeAsParenthesesHashMap().get(columnName)) {
            result = result.replaceAll("-", "(").concat(")");
        }
        return result;
    }
}
