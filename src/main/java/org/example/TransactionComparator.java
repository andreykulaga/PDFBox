package org.example;

import lombok.AllArgsConstructor;

import java.util.Comparator;

@AllArgsConstructor
public class TransactionComparator implements Comparator<Transaction> {

    ColumnName[] columnsToGroupBy;
    @Override
    public int compare(Transaction transaction1, Transaction transaction2) {
        int i = 0;
        int result = transaction1.compareWithBy(transaction2, columnsToGroupBy[i]);
        while (result == 0 && i < columnsToGroupBy.length-1) {
            i++;
            result = transaction1.compareWithBy(transaction2, columnsToGroupBy[i]);
        }
        return result;
    }
}
