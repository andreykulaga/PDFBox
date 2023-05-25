package org.example.filtration;

import lombok.Getter;
import lombok.Setter;


import java.util.ArrayList;
import java.util.HashMap;


@Setter
@Getter
public class Filtrator {
    HashMap<String, ArrayList<Filter>> filtersForFields;
    HashMap<String, String> hashMapOfTypes;

    public Filtrator(HashMap<String, ArrayList<Filter>> filtersForFields, HashMap<String, String> hashMapOfTypes) {
        this.filtersForFields = filtersForFields;
        this.hashMapOfTypes = hashMapOfTypes;

        //check that filters are provided according to types
        for (String columnName: filtersForFields.keySet()) {
            String type = hashMapOfTypes.get(columnName);
            for (Filter filter: filtersForFields.get(columnName)) {
                if (type.equalsIgnoreCase("number")) {
                    if (!(filter.getOperator().equals(Filter.Operator.eq) ||
                            filter.getOperator().equals(Filter.Operator.neq) ||
                            filter.getOperator().equals(Filter.Operator.gt) ||
                            filter.getOperator().equals(Filter.Operator.gte) ||
                            filter.getOperator().equals(Filter.Operator.lt) ||
                            filter.getOperator().equals(Filter.Operator.lte) ||
                            filter.getOperator().equals(Filter.Operator.isnull) ||
                            filter.getOperator().equals(Filter.Operator.isnotnull)
                    )) {
                        throw new RuntimeException("Operator \"" + filter.getOperator() + "\" for field \"" + columnName + "\" could not be applied to type of this type of data: " + type);
                    }
                } else if (type.equalsIgnoreCase("Datetime")) {
                    if (!(filter.getOperator().equals(Filter.Operator.eq) ||
                            filter.getOperator().equals(Filter.Operator.neq)
                    )) {
                        throw new RuntimeException("Operator \"" + filter.getOperator() + "\" for field \"" + columnName + "\" could not be applied to type of this type of data: " + type);
                    }
                } else if (type.equalsIgnoreCase("string")) {
                    if (!(filter.getOperator().equals(Filter.Operator.eq) ||
                            filter.getOperator().equals(Filter.Operator.neq) ||
                            filter.getOperator().equals(Filter.Operator.contains) ||
                            filter.getOperator().equals(Filter.Operator.doesnotcontain) ||
                            filter.getOperator().equals(Filter.Operator.beginswith) ||
                            filter.getOperator().equals(Filter.Operator.endswith) ||
                            filter.getOperator().equals(Filter.Operator.isempty) ||
                            filter.getOperator().equals(Filter.Operator.isnotempty)
                    )) {
                        throw new RuntimeException("Operator \"" + filter.getOperator() + "\" for field \"" + columnName + "\" could not be applied to type of this type of data: " + type);
                    }
                }
            }
        }

    }

    public boolean applyFilter(String columnName, String string) {
        if (!filtersForFields.containsKey(columnName)) {
            return true;
        } else {
            if (hashMapOfTypes.get(columnName).equalsIgnoreCase("number")) {
                return applyFilterToNumber(columnName, string);
            } else if (hashMapOfTypes.get(columnName). equalsIgnoreCase("Datetime")) {
                return applyFilterToDatetime(columnName, string);
            } else {
                return applyFilterToString(columnName, string);
            }
        }
    }
    private boolean applyFilterToString(String columnName, String string) {

        for (Filter filter: filtersForFields.get(columnName)) {
            if (filter.getOperator().equals(Filter.Operator.eq)) {

                if (string.equals("null")  && filter.getValue().equals("")) {
                    return true;
                } else if (filter.getValue().equalsIgnoreCase(string)) {
                    return true;
                }
            } else if (filter.getOperator().equals(Filter.Operator.neq)) {
                if (string.equals("null") && filter.getValue().equals("")) {
                    return false;
                } else if (!filter.getValue().equalsIgnoreCase(string)) {
                    return true;
                }
            } else if (filter.getOperator().equals(Filter.Operator.contains) && string.toLowerCase().toLowerCase().contains(filter.getValue().toLowerCase())) {
                return true;
            } else if (filter.getOperator().equals(Filter.Operator.doesnotcontain) && !string.toLowerCase().toLowerCase().contains(filter.getValue().toLowerCase())) {
                return true;
            } else if (filter.getOperator().equals(Filter.Operator.beginswith) && string.toLowerCase().startsWith(filter.getValue().toLowerCase())) {
                return true;
            } else if (filter.getOperator().equals(Filter.Operator.endswith) && string.toLowerCase().endsWith(filter.getValue().toLowerCase())) {
                return true;
            } else if (filter.getOperator().equals(Filter.Operator.isempty) && (string.equals("null") || string.equals(""))) {
                return true;
            } else if (filter.getOperator().equals(Filter.Operator.isnotempty) && !(string.equals("null") || string.equals(""))) {
                return true;
            }
        }
        return false;
    }

    private boolean applyFilterToNumber(String columnName, String string) {
        double dl;
        double filterValue;

        for (Filter filter: filtersForFields.get(columnName)) {

            if (filter.getOperator().equals(Filter.Operator.isnull)) {
                if (string.equals("null") || string.equals("")) {
                    return true;
                } else {
                    return false;
                }
            } else if (filter.getOperator().equals(Filter.Operator.isnotnull)) {
                if (!(string.equals("null") || string.equals(""))) {
                    return true;
                } else {
                    return false;
                }
                //only if we do not work with isnull/isnotnull operators, we can parse values and work with other operators
            } else {

                //if string is "null" it cannot pass any other test
                if (string.equalsIgnoreCase("null") || string.equalsIgnoreCase("")) {
                    if (filter.getOperator().equals(Filter.Operator.neq)) {
                        return true;
                    } else {
                        return false;
                    }
                }
                dl = Double.parseDouble(string);
                filterValue = Double.parseDouble(filter.getValue());
                if (filter.getOperator().equals(Filter.Operator.eq) && filterValue == dl) {
                    return true;
                } else if (filter.getOperator().equals(Filter.Operator.neq) && filterValue != dl) {
                    return true;
                } else if (filter.getOperator().equals(Filter.Operator.gt) && dl > filterValue) {
                    return true;
                } else if (filter.getOperator().equals(Filter.Operator.gte) && dl >= filterValue) {
                    return true;
                } else if (filter.getOperator().equals(Filter.Operator.lt) && dl < filterValue) {
                    return true;
                } else if (filter.getOperator().equals(Filter.Operator.lte) && dl <= filterValue) {
                    return true;
                }
            }

        }
        return false;
    }

    private boolean applyFilterToDatetime(String columnName, String string) {
        //use the same method as for String as we have our Datetime in string from JSON.
        //Added as a new method in case we will need to add other operators and to compare dates
        for (Filter filter: filtersForFields.get(columnName)) {
            if (filter.getOperator().equals(Filter.Operator.eq) && filter.getValue().equalsIgnoreCase(string)) {
                return true;
            } else if (filter.getOperator().equals(Filter.Operator.neq) && !filter.getValue().equalsIgnoreCase(string)) {
                return true;
            }
        }
        return false;
    }
}
