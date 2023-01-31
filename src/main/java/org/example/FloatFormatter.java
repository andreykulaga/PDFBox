package org.example;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FloatFormatter {
    public static String format(Float fl) {
        String result = new DecimalFormat("#,##,###,###.##", new DecimalFormatSymbols(Locale.ENGLISH)).format(fl);

        if (fl < 0) {
            result = result.replaceAll("-", "(").concat(")");
        }
        return result;
    }
}
