package org.example;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {

    long numberOfTransaction;
    HashMap<String, Double> numberFields;
    HashMap<String, LocalDateTime> dateTimeFields;
    HashMap<String, String> textFields;
    HashMap<String, String> allFieldsAsStrings;

    public int compareWithBy(Transaction transactionToCompare, String columnName, String columnType) {
        if (columnType.equalsIgnoreCase("float")) {
            return numberFields.get(columnName).compareTo(transactionToCompare.getNumberFields().get(columnName));
        }
        if (columnType.equalsIgnoreCase("LocalDateTime")) {
            return dateTimeFields.get(columnName).compareTo(transactionToCompare.getDateTimeFields().get(columnName));
        }
        if (columnType.equalsIgnoreCase("String")) {
            return textFields.get(columnName).compareTo(transactionToCompare.getTextFields().get(columnName));
        } else {
            return -1;
        }
    }

    public static Transaction createTransactionFromColumnNames(ArrayList<String> columnNames, HashMap<String, String> columnNamesForTableHead) {
        Transaction transaction = new Transaction();
        HashMap<String, String> textFields = new HashMap<>();
        HashMap<String, Double> numberFields = new HashMap<>();
        HashMap<String, LocalDateTime> dateFields = new HashMap<>();
        HashMap<String, String> allFieldsAsStrings = new HashMap<>();

        for (String string: columnNames) {
            textFields.put(string, columnNamesForTableHead.get(string));
            allFieldsAsStrings.put(string, columnNamesForTableHead.get(string));
        }

        transaction.setTextFields(textFields);
        transaction.setNumberFields(numberFields);
        transaction.setDateTimeFields(dateFields);
        transaction.setNumberOfTransaction(-1);
        transaction.setAllFieldsAsStrings(allFieldsAsStrings);
        return transaction;
    }

    public boolean isFieldChanged(Transaction transactionToCompare, Configuration configuration) {
        for (String columnName: configuration.getColumnsToGroupBy()) {
//            if (!getAllValuesAsString(configuration).get(columnName).equals(transactionToCompare.getAllValuesAsString(configuration).get(columnName))) {
            if (!allFieldsAsStrings.get(columnName).equals(transactionToCompare.getAllFieldsAsStrings().get(columnName))) {
                return true;
            }
        }
        return false;
    }
    public int whatFieldIsChanged(Transaction transactionToCompare, Configuration configuration) {
        int i = 0;
        String result = configuration.getColumnsToGroupBy().get(i);
        while (allFieldsAsStrings.get(result).equals(transactionToCompare.getAllFieldsAsStrings().get(result))) {
            i++;
            result = configuration.getColumnsToGroupBy().get(i);
        }
        return i;
    }
}
