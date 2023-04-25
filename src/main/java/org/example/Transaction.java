package org.example;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

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

    public HashMap<String, String> getAllValuesAsString(Configuration configuration) {
        HashMap<String, String> result = new HashMap<>(textFields);

        for (String st: numberFields.keySet()) {

            double dbl = numberFields.get(st);
            String doubleAsString = DoubleFormatter.format(dbl, st, configuration);

            result.put(st, doubleAsString);
        }
        for (String st: dateTimeFields.keySet()) {
            if (dateTimeFields.get(st).isEqual(LocalDateTime.of(1, 1, 1, 0,0, 0, 0))) {
               result.put(st, "");
            } else {
                result.put(st, dateTimeFields.get(st).format(DateTimeFormatter.ofPattern(configuration.getTextFormat().get(st))));
            }
        }
        
      return result;
    }

    // public  Transaction createTransactionWithEmptyTextAndDateFieldsAndZeroNumbers() {
    //     HashMap<String, Double> newNumberFields = numberFields;
    //     newNumberFields.replaceAll((s, v) -> (double) 0);
    //     Transaction newTransaction = new Transaction();
    //     newTransaction.setNumberFields(newNumberFields);
    //     return newTransaction;
    // }

    public static Transaction createTransactionFromColumnNames(ArrayList<String> columnNames, HashMap<String, String> columnNamesForTableHead) {
        Transaction transaction = new Transaction();
        HashMap<String, String> textFields = new HashMap<>();
        HashMap<String, Double> numberFields = new HashMap<>();
        HashMap<String, LocalDateTime> dateFields = new HashMap<>();

        for (String string: columnNames) {
            textFields.put(string, columnNamesForTableHead.get(string));
        }

        transaction.setTextFields(textFields);
        transaction.setNumberFields(numberFields);
        transaction.setDateTimeFields(dateFields);
        transaction.setNumberOfTransaction(-1);
        return transaction;
    }







//    public void setFieldsToCheck(Configuration configuration) {
//        int k =0;
//        for (ColumnName columnName: configuration.getColumnsToGroupBy()) {
//            fieldsToCheck[k] = getValue(columnName);
//            k++;
//        }
//    }
//
    public boolean isFieldChanged(Transaction transactionToCompare, Configuration configuration) {
        for (String columnName: configuration.getColumnsToGroupBy()) {
            if (!getAllValuesAsString(configuration).get(columnName).equals(transactionToCompare.getAllValuesAsString(configuration).get(columnName))) {
                return true;
            }
        }
        return false;
    }
    public int whatFieldIsChanged(Transaction transactionToCompare, Configuration configuration) {
        int i = 0;
        String result = configuration.getColumnsToGroupBy().get(i);
        while (getAllValuesAsString(configuration).get(result).equals(transactionToCompare.getAllValuesAsString(configuration).get(result))) {
            i++;
            result = configuration.getColumnsToGroupBy().get(i);
        }
        return i;
    }
//
//

//
//    public String getValue(ColumnName columnName) {
//       if (columnName == ColumnName.Investment_Name) {
//           return investmentName;
//       }
//       if (columnName == ColumnName.Transaction_Type_Name) {
//           return transactionTypeName;
//       }
//       if (columnName == ColumnName.Contract_Settlement) {
//           return contractSettlement;
//       }
//       if (columnName == ColumnName.Currency_Cd) {
//           return currencyCode;
//       }
//       if (columnName == ColumnName.Trade_Dt) {
//           return tradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"));
//       }
//       if (columnName == ColumnName.Unit_Price_Local_Amt) {
//           return String.valueOf(unitPriceLocalAmount);
//       }
//       if (columnName == ColumnName.Asset_Type_Nm) {
//           return assetTypeName;
//       }
//       if (columnName == ColumnName.Net_Local_Amt) {
//           return String.valueOf(netLocalAmount);
//       }
//       if (columnName == ColumnName.Trade_Type) {
//           return tradeType;
//       }
//       if (columnName == ColumnName.Transaction_Event) {
//           return transactionEvent;
//       }
//       if (columnName == ColumnName.Tax_Local_Amt) {
//            return String.valueOf(taxLocalAmount);
//       } else {
//           return createDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"));
//       }
//    }
//
////    public String columnNameToFieldName(ColumnName columnName) {
////       if (columnName == ColumnName.Investment_Name) {
////           return "investmentName";
////       }
////       if (columnName == ColumnName.Transaction_Type_Name) {
////           return "transactionTypeName";
////       }
////       if (columnName == ColumnName.Contract_Settlement) {
////           return "contractSettlement";
////       }
////       if (columnName == ColumnName.Currency_Cd) {
////           return "currencyCode";
////       }
////       if (columnName == ColumnName.Trade_Dt) {
////           return "tradeDate";
////       }
////       if (columnName == ColumnName.Unit_Price_Local_Amt) {
////           return "unitPriceLocalAmount";
////       }
////       if (columnName == ColumnName.Asset_Type_Nm) {
////           return "assetTypeName";
////       }
////       if (columnName == ColumnName.Net_Local_Amt) {
////           return "netLocalAmount";
////       }
////       if (columnName == ColumnName.Trade_Type) {
////           return "tradeType";
////       }
////       if (columnName == ColumnName.Transaction_Event) {
////           return "transactionEvent";
////       }
////       if (columnName == ColumnName.Tax_Local_Amt) {
////            return "taxLocalAmount";
////       } else {
////           return "createDateTime";
////       }
////    }
//
//
////    public String[] getFields() {
////        String[] array = {investmentName, transactionTypeName, contractSettlement, currencyCode,
////                String.valueOf(tradeDate), String.valueOf(unitPriceLocalAmount), assetTypeName,
////                String.valueOf(netLocalAmount), tradeType, transactionEvent, String.valueOf(taxLocalAmount),
////                String.valueOf(createDateTime)};
////        return array;
////    }
}
