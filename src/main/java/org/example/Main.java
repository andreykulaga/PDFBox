package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Configuration.class, new ConfigurationDeserializer());
        mapper.registerModule(module);

        Configuration configuration = new Configuration();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("./configuration.txt"))){
            String line = bufferedReader.readLine();
            configuration = mapper.readValue(line, Configuration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        HashMap<ColumnName, Integer> columnNameHashMap = new HashMap<>();
        for (ColumnName columnName: ColumnName.values()) {
            columnNameHashMap.put(columnName, columnName.toString().length());
        }


        ArrayList<Transaction> transactions = new ArrayList<>();

        int[] valuesLengths = new int[12];

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("./Raw_Data.txt"))){
            String line = bufferedReader.readLine();
            line = bufferedReader.readLine();
            while (line != null) {
                String[] strings = line.split("\\|");

                //check length and keep the biggest one to calculate cell width latter
                for (int i = 0; i < 12; i++) {
                    if (strings[i].length() > valuesLengths[i]) {
                        valuesLengths[i] = strings[i].length();
                    }
                }
                //create arraylist of all transactions
                Transaction transaction = new Transaction(strings, configuration);
                if (transaction.getFieldsToCheck().length > 0) {
                    transaction.setFieldsToCheck(configuration);
                }
                transactions.add(transaction);
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //update ColumnNameHashMap with the biggest length
        int i = 0;
        for (ColumnName columnName: ColumnName.values()) {
            if (valuesLengths[i] > (columnNameHashMap.get(columnName))) {
                columnNameHashMap.put(columnName, valuesLengths[i]);
            }
            i++;
        }
        //clean columnNameHashMap from columns that are hided
        for (ColumnName columnName: ColumnName.values()) {
            if (!configuration.getWhatColumnsToShow().get(columnName)) {
                columnNameHashMap.remove(columnName);
            }
        }

        try (PDDocument doc = new PDDocument()){
            Pdf pdf = new Pdf(doc, configuration, columnNameHashMap);
            pdf.addNewPage();
            pdf.addHeadOfTable();

            if (configuration.getColumnsToGroupBy() == null || configuration.getColumnsToGroupBy().length == 0) {
                Transaction subtotal = new Transaction(0, 0, 0);
                for (Transaction t: transactions) {
                    pdf.addTableRow(t);
                    subtotal.setUnitPriceLocalAmount(subtotal.getUnitPriceLocalAmount() + t.getUnitPriceLocalAmount());
                    subtotal.setNetLocalAmount(subtotal.getNetLocalAmount() + t.getNetLocalAmount());
                    subtotal.setTaxLocalAmount(subtotal.getTaxLocalAmount() + t.getTaxLocalAmount());
                }
                pdf.addGrandTotalRow(subtotal);
            } else {
                //Create custom comparator with needed columns
                TransactionComparator transactionComparator = new TransactionComparator(configuration.getColumnsToGroupBy());
                transactions.sort(transactionComparator);

                //Create array of transaction to store subtotals and grand total
                Transaction[] subtotals = new Transaction[configuration.getColumnsToGroupBy().length+1];
                for (int j = 0; j < configuration.getColumnsToGroupBy().length+1; j++) {
                    subtotals[j] = new Transaction(0, 0, 0);
                }



                //add header for the first grouping
                for (int k = 0; k < configuration.getColumnsToGroupBy().length; k++) {
                    pdf.addGroupHead(configuration.getColumnsToGroupBy()[k], transactions.get(1));
                }

                for (int j = 0; j < transactions.size()-1; j++) {
                    //for every transaction add values to all subtotals
                    for (Transaction subtotalTransaction:subtotals) {
                        subtotalTransaction.setUnitPriceLocalAmount(subtotalTransaction.getUnitPriceLocalAmount() + transactions.get(j).getUnitPriceLocalAmount());
                        subtotalTransaction.setNetLocalAmount(subtotalTransaction.getNetLocalAmount() + transactions.get(j).getNetLocalAmount());
                        subtotalTransaction.setTaxLocalAmount(subtotalTransaction.getTaxLocalAmount() + transactions.get(j).getTaxLocalAmount());
                    }
                    //print transaction
                    pdf.addTableRow(transactions.get(j));

                    //if next transaction has not equal some fields that we are grouping by
                    if (transactions.get(j).isFieldChanged(transactions.get(j+1), configuration)) {
                        int positionOfChangedField = transactions.get(j).whatFieldIsChanged(transactions.get(j+1), configuration);
                        int columnPlace = subtotals.length - 2 - positionOfChangedField;
                        //add subtotals
                        for (int k = 0; k <= columnPlace; k++) {
                            pdf.addSubtotalRow(configuration.getColumnsToGroupBy()[k], subtotals[k]);
                            subtotals[k] = new Transaction(0, 0 ,0);
                        }
                        //add header for next grouping
                        for (int k = positionOfChangedField; k < configuration.getColumnsToGroupBy().length; k++) {
                            pdf.addGroupHead(configuration.getColumnsToGroupBy()[k], transactions.get(j+1));
                        }

                    }
                }
                //add the last transaction
                int j = transactions.size()-1;
                for (Transaction subtotalTransaction:subtotals) {
                    subtotalTransaction.setUnitPriceLocalAmount(subtotalTransaction.getUnitPriceLocalAmount() + transactions.get(j).getUnitPriceLocalAmount());
                    subtotalTransaction.setNetLocalAmount(subtotalTransaction.getNetLocalAmount() + transactions.get(j).getNetLocalAmount());
                    subtotalTransaction.setTaxLocalAmount(subtotalTransaction.getTaxLocalAmount() + transactions.get(j).getTaxLocalAmount());
                }
                pdf.addTableRow(transactions.get(j));
                for (int k = 0; k < subtotals.length-1; k++) {
                    pdf.addSubtotalRow(configuration.getColumnsToGroupBy()[k], subtotals[k]);
                }
                //add total
                pdf.addGrandTotalRow(subtotals[subtotals.length-1]);

            }

            doc.save("./result.pdf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}