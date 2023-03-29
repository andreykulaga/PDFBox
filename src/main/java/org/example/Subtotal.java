package org.example;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Subtotal {

    HashMap<String, Double> numberFields;
//    HashMap<String, LocalDateTime> dateTimeFields;
   HashMap<String, String> textFields;

    public Subtotal(Transaction transaction) {
        HashMap<String, Double> newNumberFields = new HashMap<>();
        for (String string: transaction.getNumberFields().keySet()) {
            newNumberFields.put(string, (double) 0);
        }
        this.numberFields = newNumberFields;


    }

    public void addToSubtotal(Transaction transaction) {
        for (String string: transaction.getNumberFields().keySet()) {
            Double dbl = numberFields.get(string) + transaction.getNumberFields().get(string);
            numberFields.put(string, dbl);
        }
    }
}


