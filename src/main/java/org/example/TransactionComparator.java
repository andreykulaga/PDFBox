package org.example;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

@AllArgsConstructor
public class TransactionComparator implements Comparator<Transaction> {

    ArrayList<String> columnsToGroupBy;
    HashMap<String, String> hashMapOfTypes;

    HashMap<String, String> typesOfColumnsToGroupBy;

    public TransactionComparator(ArrayList<String> columnsToGroupBy, HashMap<String, String> hashMapOfTypes) {
        this.columnsToGroupBy = columnsToGroupBy;
        this.hashMapOfTypes = hashMapOfTypes;
        this.typesOfColumnsToGroupBy = new HashMap<>();


        for (String column: columnsToGroupBy) {
            typesOfColumnsToGroupBy.put(column, hashMapOfTypes.get(column));
        }
    }

    @Override
    public int compare(Transaction transaction1, Transaction transaction2) {
        int i = 0;
        int result = transaction1.compareWithBy(transaction2, columnsToGroupBy.get(i), typesOfColumnsToGroupBy.get(columnsToGroupBy.get(i)));
        while (result == 0 && i < columnsToGroupBy.size()-1) {
            i++;
            result = transaction1.compareWithBy(transaction2, columnsToGroupBy.get(i), typesOfColumnsToGroupBy.get(columnsToGroupBy.get(i)));
        }
        return result;
    }
}
