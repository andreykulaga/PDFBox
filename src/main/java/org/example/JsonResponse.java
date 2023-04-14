package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;


@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class JsonResponse {
    
    public Rule rule;
    
    @AllArgsConstructor
    public static class Rule{
        public String expiresAt;
        public int expirationPolicy;
    }
    
    public ArrayList<CacheItem> cacheItem;
    
    @AllArgsConstructor
    public static class CacheItem{
        public Report report;
    }

    @AllArgsConstructor
    public static class Report{
        public int reportId;
        public ArrayList<Field> fields;
        public ArrayList<Grouping> grouping;
        public ArrayList<Aggregate> aggregate;
        public ArrayList<Datum> data;
    }
    @AllArgsConstructor
    public static class Field{
        public String field;
        public String type;
    }
    @AllArgsConstructor
    public static class Grouping{
        public String field;
        public String type;
    }
    @AllArgsConstructor
    public static class Aggregate{
        public String field;
        public String aggregate;
    }
    @AllArgsConstructor
    public static class Datum{
        HashMap<String, String> fieldsAndValues;
    } 


    public ArrayList<String> createColumnNames() {
        ArrayList<String> columnNames = new ArrayList<>();
        for (Field f: cacheItem.get(0).report.fields) {
            String colName = f.field.replaceAll("_", " ").toLowerCase();
            columnNames.add(colName);
        }
        return columnNames;
    }

//    public HashMap<String, String> createColumnNamesForTableHead() {
//        HashMap<String, String> result = new HashMap<>();
//        for (Field f: cacheItem.get(0).report.fields) {
//            String colName = f.field.replaceAll("_", " ").toLowerCase();
//            result.put(colName, f.field.replaceAll("_", " "));
//        }
//        return result;
//    }

    public HashMap<String, String> createHashMapOfTypes() {
        HashMap<String, String> hashMapOfTypes = new HashMap<>();

        for (Field f: cacheItem.get(0).report.fields) {
            String colName = f.field.replaceAll("_", " ").toLowerCase();
            hashMapOfTypes.put(colName, f.type);
        }
        return hashMapOfTypes;
    }

    public ArrayList<Transaction> extractTransactions(HashMap<String, Float> textLengths, HashMap<String, Float> notStringMaxLengths, Configuration configuration) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        
        HashMap<String, String> hashMapOfTypes = createHashMapOfTypes();
        long transactionNumber = 1;
        
        ArrayList<Datum> data = cacheItem.get(0).report.data;
        for (Datum d: data) {
            HashMap<String, Double> numberFields = new HashMap<>();
            HashMap<String, LocalDateTime> dateTimeFields = new HashMap<>();
            HashMap<String, String> textFields = new HashMap<>();
            for (String s: d.fieldsAndValues.keySet()) {
                //need to replace "_" because we store fields names that way
                String key = s.replaceAll("_", " ").toLowerCase();
                String value = d.fieldsAndValues.get(s);

                //check field type and fill hashmaps of transaction
                if (hashMapOfTypes.get(key).equalsIgnoreCase("number")) {
                    double f;
                    try {
                        f = Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        f = 0;
                    }
                    numberFields.put(key, f);
                }
                if (hashMapOfTypes.get(key).equalsIgnoreCase("Datetime")) {
                    LocalDateTime ldt = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"));
                    dateTimeFields.put(key, ldt);
                } else {
                    textFields.put(key, value);
                }


                if (value == null) {
                    value = "null";
                }
                //if it is number, get a double and format it
                if (hashMapOfTypes.get(key).equalsIgnoreCase("number")) {
                    double f;
                    try {
                        f = Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        f = 0;
                    }
                    value = DoubleFormatter.format(f, key, configuration);
                }
                //if it is date, format it according to config
                if (hashMapOfTypes.get(key).equalsIgnoreCase("Datetime")) {
                    value = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"))
                            .format(DateTimeFormatter.ofPattern(configuration.getTextFormat().get(key)));
                }

                float length;
                try {
                    //check length and keep the biggest one to calculate cell width latter
                    length = PDType1Font.HELVETICA.getStringWidth(value) / 1000;


                    if (!configuration.forceFontSize) {
                        if (configuration.isWrapTextInTable() &&
                                hashMapOfTypes.get(key).equalsIgnoreCase("string") &&
                                value.length() > configuration.getMaxCharactersInTextLine()) {
                            float tempLength = PDType1Font.HELVETICA.getStringWidth(value.substring(0, configuration.getMaxCharactersInTextLine()-1)) / 1000;
                            //if it is less than is already in text lengths it means that there is already value for string with length bigger than max but more wide characters
                            if (tempLength > textLengths.get(key)) {
                                textLengths.replace(key, tempLength);
                            }
                        } else {
                            if (length > textLengths.get(key)) {
                                textLengths.replace(key, length);
                            }
                        }
                    } else {
                        if (length > textLengths.get(key)) {
                            System.out.println(textLengths.get(key));
                            System.out.println(length);
                            System.out.println();
                            textLengths.replace(key, length);
                        }
                        if (length > notStringMaxLengths.get(key)) {
                            notStringMaxLengths.replace(key, length);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            Transaction t = new Transaction(transactionNumber,numberFields,dateTimeFields,textFields);
            transactions.add(t);
            transactionNumber++;
        }
        return transactions;
    }
}


