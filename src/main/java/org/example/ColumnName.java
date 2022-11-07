package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ColumnName {
    Investment_Name,
    Transaction_Type_Name,
    Contract_Settlement,
    Currency_Cd,
    Trade_Dt,
    Unit_Price_Local_Amt,
    Asset_Type_Nm,
    Net_Local_Amt,
    Trade_Type,
    Transaction_Event,
    Tax_Local_Amt,
    Create_Date;

    static ColumnName fromString(String string) {
        if (string.equals("Investment_Name")) {
            return Investment_Name;
        }
        if (string.equals("Transaction_Type_Name")) {
            return Transaction_Type_Name;
        }
        if (string.equals("Contract_Settlement")) {
            return Contract_Settlement;
        }
        if (string.equals("Currency_Cd")) {
            return Currency_Cd;
        }
        if (string.equals("Trade_Dt")) {
            return Trade_Dt;
        }
        if (string.equals("Unit_Price_Local_Amt")) {
            return Unit_Price_Local_Amt;
        }
        if (string.equals("Asset_Type_Nm")) {
            return Asset_Type_Nm;
        }
        if (string.equals("Net_Local_Amt")) {
            return Net_Local_Amt;
        }
        if (string.equals("Trade_Type")) {
            return Trade_Type;
        }
        if (string.equals("Transaction_Event")) {
            return Transaction_Event;
        }
        if (string.equals("Tax_Local_Amt")) {
            return Tax_Local_Amt;
        }
        else {
            return Create_Date;
        }
    }
}
