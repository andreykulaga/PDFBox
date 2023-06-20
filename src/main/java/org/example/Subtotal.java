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

    public Subtotal(HashMap<String, String> hashMapOfTypes) {
        HashMap<String, Double> newNumberFields = new HashMap<>();
        for (String string: hashMapOfTypes.keySet()) {
            if (hashMapOfTypes.get(string).equalsIgnoreCase("number")) {
                newNumberFields.put(string, (double) 0);
            }
        }
        this.numberFields = newNumberFields;


    }

    public void addToSubtotal(Transaction transaction) {
        for (String string: numberFields.keySet()) {
            Double dbl = numberFields.get(string) + transaction.getNumberFields().get(string);
            numberFields.put(string, dbl);
        }
    }
}


