package org.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;

public class TransactionParser {


    public static HashMap<String, String> defineFieldsTypes(String string, ArrayList<String> fieldsNames) {
        String[] array = string.split("\\|");
        HashMap<String, String> hashMapOfTypes = new HashMap<>();

        if (fieldsNames.size() != array.length) {
            throw new RuntimeException("the first line with data has different quantity of fields from the line with column names");
        }

        for (int i = 0; i < fieldsNames.size(); i++) {
            try {
                Float.parseFloat(array[i]);
                hashMapOfTypes.put(fieldsNames.get(i), "float");
//                System.out.println("value number " + (i+1) + " is a float");
            } catch (IllegalArgumentException ignored) {
            }
            try {
                LocalDateTime.parse(array[i], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"));
                hashMapOfTypes.put(fieldsNames.get(i), "LocalDateTime");
//                System.out.println("value number " + (i+1) + " is a LocalDateTime");
            } catch (DateTimeParseException ignored) {
            }
            if (!hashMapOfTypes.containsKey(fieldsNames.get(i))) {
                hashMapOfTypes.put(fieldsNames.get(i), "String");
//                System.out.println("value number " + (i+1) + " is a String");
            }
        }

        return hashMapOfTypes;
    }


    public static Transaction parseTextLineIntoTransaction(Long numberOfString, String string, HashMap<String, String> hashMapOfTypes, ArrayList<String> columnNames, ArrayList<String> whatColumnsToHide) throws DateTimeParseException, ArrayIndexOutOfBoundsException {
        String[] array = string.split("\\|");

        HashMap<String, Float> numberFields = new HashMap<>();
        HashMap<String, LocalDateTime> dateTimeFields = new HashMap<>();
        HashMap<String, String> textFields = new HashMap<>();

        for (int i = 0; i < hashMapOfTypes.size(); i++) {
            String columnName = columnNames.get(i);
            //if column name need to hide, do not add it to transaction
            if (!whatColumnsToHide.contains(columnName)) {
                String type = hashMapOfTypes.get(columnName);
                if (type.equalsIgnoreCase("float")) {
                    float f = Float.parseFloat(array[i]);
                    numberFields.put(columnName, f);
                }
                if (type.equalsIgnoreCase("LocalDateTime")) {
                    LocalDateTime ldt = LocalDateTime.parse(array[i], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"));
                    dateTimeFields.put(columnName, ldt);
                }
                else {
                    textFields.put(columnName, array[i]);
                }
            }
        }
        return new Transaction(numberOfString, numberFields, dateTimeFields, textFields);

    }
}
