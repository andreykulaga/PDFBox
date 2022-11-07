package org.example;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {
    String investmentName;
    String transactionTypeName;
    String contractSettlement;
    String currencyCode;
    LocalDateTime tradeDate;
//    String tradeDate;
    float unitPriceLocalAmount;
//    String unitPriceLocalAmount;
    String assetTypeName;
    float netLocalAmount;
//    String netLocalAmount;
    String tradeType;
    String transactionEvent;
    float taxLocalAmount;
//    String taxLocalAmount;
//    String createDateTime;
    LocalDateTime createDateTime;
    String[] fieldsToCheck;

    public Transaction(String[] array, Configuration configuration) {
        this.investmentName = array[0];
        this.transactionTypeName = array[1];
        this.contractSettlement = array[2];
        this.currencyCode = array[3];
        this.tradeDate = LocalDateTime.parse(array[4], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"));
        this.unitPriceLocalAmount = Float.parseFloat(array[5]);
        this.assetTypeName = array[6];
        this.netLocalAmount = Float.parseFloat(array[7]);
        this.tradeType = array[8];
        this.transactionEvent = array[9];
        this.taxLocalAmount = Float.parseFloat(array[10]);
        this.createDateTime = LocalDateTime.parse(array[11], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"));
        if (configuration.getColumnsToGroupBy() != null) {
            this.fieldsToCheck = new String[configuration.getColumnsToGroupBy().length];
        } else {
            this.fieldsToCheck = new String[0];
        }
    }

    public Transaction(float a, float b, float c) {
        this.investmentName = "";
        this.transactionTypeName = "";
        this.contractSettlement = "";
        this.currencyCode = "";
        this.tradeDate = LocalDateTime.of(01, 01, 01, 01, 01);
        this.unitPriceLocalAmount = a;
        this.assetTypeName = "";
        this.netLocalAmount = b;
        this.tradeType = "";
        this.transactionEvent = "";
        this.taxLocalAmount = c;
        this.createDateTime = LocalDateTime.of(01, 01, 01, 01, 01);
        this.fieldsToCheck = new String[1];
    }

    public void setFieldsToCheck(Configuration configuration) {
        int k =0;
        for (ColumnName columnName: configuration.getColumnsToGroupBy()) {
            fieldsToCheck[k] = getValue(columnName);
            k++;
        }
    }

    public boolean isFieldChanged(Transaction transactionToCompare, Configuration configuration) {
        for (ColumnName columnName: configuration.getColumnsToGroupBy()) {
            if (!getValue(columnName).equals(transactionToCompare.getValue(columnName))) {
                return true;
            }
        }
        return false;
    }
    public int whatFieldIsChanged(Transaction transactionToCompare, Configuration configuration) {
        int i = 0;
        ColumnName result = configuration.getColumnsToGroupBy()[i];
        while (getValue(result).equals(transactionToCompare.getValue(result))) {
            i++;
            result = configuration.getColumnsToGroupBy()[i];
        }
        return i;
    }


    public int compareWithBy(Transaction transactionToCompare, ColumnName columnName) {
        if (columnName == ColumnName.Investment_Name) {
            return investmentName.compareTo(transactionToCompare.getInvestmentName());
        }
        if (columnName == ColumnName.Transaction_Type_Name) {
            return transactionTypeName.compareTo(transactionToCompare.getTransactionTypeName());
        }
        if (columnName == ColumnName.Contract_Settlement) {
            return contractSettlement.compareTo(transactionToCompare.getContractSettlement());
        }
        if (columnName == ColumnName.Currency_Cd) {
            return currencyCode.compareTo(transactionToCompare.getCurrencyCode());
        }
        if (columnName == ColumnName.Trade_Dt) {
            return tradeDate.compareTo(transactionToCompare.getTradeDate());
        }
        if (columnName == ColumnName.Unit_Price_Local_Amt) {
            return Float.compare(unitPriceLocalAmount, transactionToCompare.getUnitPriceLocalAmount());
        }
        if (columnName == ColumnName.Asset_Type_Nm) {
            return assetTypeName.compareTo(transactionToCompare.getAssetTypeName());
        }
        if (columnName == ColumnName.Net_Local_Amt) {
            return Float.compare(netLocalAmount, transactionToCompare.getNetLocalAmount());
        }
        if (columnName == ColumnName.Trade_Type) {
            return tradeType.compareTo(transactionToCompare.getTradeType());
        }
        if (columnName == ColumnName.Transaction_Event) {
            return transactionEvent.compareTo(transactionToCompare.getTransactionEvent());
        }
        if (columnName == ColumnName.Tax_Local_Amt) {
            return Float.compare(taxLocalAmount, transactionToCompare.getTaxLocalAmount());
        }
        if (columnName == ColumnName.Create_Date){
            return createDateTime.compareTo(transactionToCompare.getCreateDateTime());
        } else {
            return -1;
        }
    }

    public String getValue(ColumnName columnName) {
       if (columnName == ColumnName.Investment_Name) {
           return investmentName;
       }
       if (columnName == ColumnName.Transaction_Type_Name) {
           return transactionTypeName;
       }
       if (columnName == ColumnName.Contract_Settlement) {
           return contractSettlement;
       }
       if (columnName == ColumnName.Currency_Cd) {
           return currencyCode;
       }
       if (columnName == ColumnName.Trade_Dt) {
           return tradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"));
       }
       if (columnName == ColumnName.Unit_Price_Local_Amt) {
           return String.valueOf(unitPriceLocalAmount);
       }
       if (columnName == ColumnName.Asset_Type_Nm) {
           return assetTypeName;
       }
       if (columnName == ColumnName.Net_Local_Amt) {
           return String.valueOf(netLocalAmount);
       }
       if (columnName == ColumnName.Trade_Type) {
           return tradeType;
       }
       if (columnName == ColumnName.Transaction_Event) {
           return transactionEvent;
       }
       if (columnName == ColumnName.Tax_Local_Amt) {
            return String.valueOf(taxLocalAmount);
       } else {
           return createDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a"));
       }
    }

//    public String columnNameToFieldName(ColumnName columnName) {
//       if (columnName == ColumnName.Investment_Name) {
//           return "investmentName";
//       }
//       if (columnName == ColumnName.Transaction_Type_Name) {
//           return "transactionTypeName";
//       }
//       if (columnName == ColumnName.Contract_Settlement) {
//           return "contractSettlement";
//       }
//       if (columnName == ColumnName.Currency_Cd) {
//           return "currencyCode";
//       }
//       if (columnName == ColumnName.Trade_Dt) {
//           return "tradeDate";
//       }
//       if (columnName == ColumnName.Unit_Price_Local_Amt) {
//           return "unitPriceLocalAmount";
//       }
//       if (columnName == ColumnName.Asset_Type_Nm) {
//           return "assetTypeName";
//       }
//       if (columnName == ColumnName.Net_Local_Amt) {
//           return "netLocalAmount";
//       }
//       if (columnName == ColumnName.Trade_Type) {
//           return "tradeType";
//       }
//       if (columnName == ColumnName.Transaction_Event) {
//           return "transactionEvent";
//       }
//       if (columnName == ColumnName.Tax_Local_Amt) {
//            return "taxLocalAmount";
//       } else {
//           return "createDateTime";
//       }
//    }


//    public String[] getFields() {
//        String[] array = {investmentName, transactionTypeName, contractSettlement, currencyCode,
//                String.valueOf(tradeDate), String.valueOf(unitPriceLocalAmount), assetTypeName,
//                String.valueOf(netLocalAmount), tradeType, transactionEvent, String.valueOf(taxLocalAmount),
//                String.valueOf(createDateTime)};
//        return array;
//    }
}
