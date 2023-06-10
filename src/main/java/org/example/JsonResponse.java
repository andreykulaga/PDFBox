package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.example.filtration.Filtrator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


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
        Filtrator filtrator = new Filtrator(configuration.getFiltersForFields(), hashMapOfTypes);
        long transactionNumber = 1;
        
        ArrayList<Datum> data = cacheItem.get(0).report.data;
        for (Datum d: data) {
            boolean passedTheFilter = false;
            HashMap<String, Double> numberFields = new HashMap<>();
            HashMap<String, LocalDateTime> dateTimeFields = new HashMap<>();
            HashMap<String, String> textFields = new HashMap<>();
            HashMap<String, String> allFieldsAsStrings = new HashMap<>();
            for (String s: d.fieldsAndValues.keySet()) {
                //need to replace "_" because we store fields names that way
                String key = s.replaceAll("_", " ").toLowerCase();
                String value = d.fieldsAndValues.get(s);

                passedTheFilter = filtrator.applyFilter(key, value);
                if (!passedTheFilter) {
                    break;
                }


                if (value.equalsIgnoreCase("null")) {
                    value = "";
                }

                //check field type and fill hashmaps of transaction
                if (hashMapOfTypes.get(key).equalsIgnoreCase("number")) {
                    double f;
                    if (value.equalsIgnoreCase("")) {
                        f = 0;
                        allFieldsAsStrings.put(key, "");
                    } else {
                        f = Double.parseDouble(value);

                        //make the number absolute
                        if (configuration.getIsAbsoluteValueHashMap().get(key)) {
                            f = Math.abs(f);
                        }

                        allFieldsAsStrings.put(key, DoubleFormatter.format(f, key, configuration));
                    }
                    numberFields.put(key, f);
                } else if (hashMapOfTypes.get(key).equalsIgnoreCase("Datetime")) {
                    LocalDateTime ldt;
                    if (value.equalsIgnoreCase("")) {
                        ldt = LocalDateTime.of(1, 1, 1, 0,0, 0, 0);
                        allFieldsAsStrings.put(key, "");
                    } else {
                        ldt = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                        String pattern = configuration.getTextFormat().get(key);
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
                        String temp = ldt.format(dateTimeFormatter);
                        allFieldsAsStrings.put(key, temp);
                    }

                    dateTimeFields.put(key, ldt);
                } else {
                    textFields.put(key, value);
                    allFieldsAsStrings.put(key, value);
                }

                //if it is number, get a double and format it
                if (hashMapOfTypes.get(key).equalsIgnoreCase("number")) {
                    double f;
                    if (value.equalsIgnoreCase("")) {
                        f = 0;
                    } else {
                        f = Double.parseDouble(value);
                    }
                    value = DoubleFormatter.format(f, key, configuration);
                }
                //if it is date, format it according to config
                if (hashMapOfTypes.get(key).equalsIgnoreCase("Datetime")) {
                    if (!value.equalsIgnoreCase("")) {
                        value = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                                .format(DateTimeFormatter.ofPattern(configuration.getTextFormat().get(key)));
                    }
                }

                float length;
                try {
                    //check length and keep the biggest one to calculate cell width latter
                    length = PDType1Font.HELVETICA.getStringWidth(value + "  ") / 1000;


                    if (!configuration.forceFontSize) {
                        if (configuration.isWrapTextInTable() &&
                                hashMapOfTypes.get(key).equalsIgnoreCase("string") &&
                                value.length() > configuration.getMaxCharactersInTextLine()) {
                            float tempLength = PDType1Font.HELVETICA.getStringWidth(value.substring(0, configuration.getMaxCharactersInTextLine()-1) + "  ") / 1000;
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

            if (passedTheFilter) {
                Transaction t = new Transaction(transactionNumber, numberFields, dateTimeFields, textFields, allFieldsAsStrings);
                transactions.add(t);
                transactionNumber++;
            }

        }
        return transactions;
    }
}


