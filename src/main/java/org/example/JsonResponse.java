package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;
import lombok.experimental.FieldDefaults;

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

    public HashMap<String, String> createColumnNamesForTableHead() {
        HashMap<String, String> result = new HashMap<>();
        for (Field f: cacheItem.get(0).report.fields) {
            String colName = f.field.replaceAll("_", " ").toLowerCase();
            result.put(colName, f.field.replaceAll("_", " "));
        }
        return result;
    }

    public HashMap<String, String> createHashMapOfTypes() {
        HashMap<String, String> hashMapOfTypes = new HashMap<>();

        for (Field f: cacheItem.get(0).report.fields) {
            String colName = f.field.replaceAll("_", " ").toLowerCase();
            hashMapOfTypes.put(colName, f.type);
        }
        return hashMapOfTypes;
    }

    public ArrayList<Transaction> extractTransactions(HashMap<String, Integer> textLengths, Configuration configuration) {
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

                if (value == null) {
                    value = "null";
                }

                //check length and keep the biggest one to calculate cell width latter
                int length = value.length();

                 //increase length for each capitalised letter, as capitilized letters are too wide
                String lineToCompare = value.toLowerCase();
                int howManyCapitalizedLetters = 0;
                for (int j = 0; j < length; j++) {
                    if (value.charAt(j) == lineToCompare.charAt(j)) {
                    howManyCapitalizedLetters++;
                    }
                }
                length += (howManyCapitalizedLetters/3);  


                if (length > textLengths.get(key) && length <= configuration.getMaxCharactersInTextLine()) {
                    textLengths.replace(key, length);
                }
                
        
                //check field type and fill hashmaps of transaction
                if (hashMapOfTypes.get(key).equalsIgnoreCase("number")) {
                    Double f = Double.parseDouble(value);
                    numberFields.put(key, f);
                }
                if (hashMapOfTypes.get(key).equalsIgnoreCase("Datetime")) {
                    LocalDateTime ldt = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"));
                    dateTimeFields.put(key, ldt);
                } else {
                    textFields.put(key, value);
                }
            }
            Transaction t = new Transaction(transactionNumber,numberFields,dateTimeFields,textFields);
            transactions.add(t);
            transactionNumber++;
        }
        return transactions;
    }
}


