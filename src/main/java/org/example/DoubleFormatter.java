package org.example;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class DoubleFormatter {
    public static String format(Double dbl, String columnName, Configuration configuration) {

        String result = new DecimalFormat(configuration.getTextFormat().get(columnName), new DecimalFormatSymbols(Locale.ENGLISH)).format(dbl);
        if (dbl < 0 && configuration.getNegativeAsParenthesesHashMap().get(columnName)) {
            result = result.replaceAll("-", "(").concat(")");
        }
        return result;
    }
}
