package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.*;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {

        //load configuration
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Configuration.class, new ConfigurationDeserializer());
        mapper.registerModule(module);

        Configuration configuration = new Configuration();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("./configuration.txt"))) {
            String line = bufferedReader.readLine();
            configuration = mapper.readValue(line, Configuration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //create array list of transactions
        ArrayList<Transaction> transactions = new ArrayList<>();

        //create array list that we will need to calculate cell width
        HashMap<String, Integer> textLengths = new HashMap<>();

        //create hash map to store types of columns
        HashMap<String, String> hashMapOfTypes;

        //create array list to store all columns names
        ArrayList<String> columnNames = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("./Raw_Data.txt"))) {

            String[] firstLine = bufferedReader.readLine().split("\\|");
            columnNames.addAll(Arrays.asList(firstLine));

            //fill array that we will need to calculate cell width
            for (int i = 0; i < columnNames.size(); i++) {
                textLengths.put(columnNames.get(i), columnNames.get(i).length());
            }

            //define fields types
            String line = bufferedReader.readLine();
            hashMapOfTypes = TransactionParser.defineFieldsTypes(line, columnNames);


            //fill list of transactions
            long transactionNumber = 1;
            while (line != null) {

                String[] strings = line.split("\\|");

                //check length and keep the biggest one to calculate cell width latter
                for (int i = 0; i < strings.length; i++) {
                    if (strings[i].length() > textLengths.get(columnNames.get(i))) {
                        textLengths.put(columnNames.get(i), strings[i].length());
                    }
                }
                //fill arraylist with a transaction
                try {
                    Transaction transaction = TransactionParser.parseTextLineIntoTransaction(transactionNumber, line, hashMapOfTypes, columnNames, configuration.getWhatColumnsToHide());
                    transactions.add(transaction);
                } catch (ArrayIndexOutOfBoundsException | DateTimeParseException e) {
                    throw new RuntimeException("check the line number " + transactionNumber + ". It has not the same quantity of fields as the first one or some fields has other type");
                }
                //read next line
                transactionNumber++;
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        //clean textLengths from columns that are hided
        for (String string : configuration.getWhatColumnsToHide()) {
            textLengths.remove(string);
        }

        //clean array list of column names from hidden columns
        for (String string : configuration.getWhatColumnsToHide()) {
            columnNames.remove(string);
        }

        //clean map of types from hidden columns
        for (String string : configuration.getWhatColumnsToHide()) {
            hashMapOfTypes.remove(string);
        }



        //create PDF document

        try (PDDocument doc = new PDDocument()) {
            Pdf pdf = new Pdf(doc, configuration, columnNames, textLengths);
            pdf.addNewPage();
            pdf.addHeadOfTable();

            if (configuration.getColumnsToGroupBy() == null || configuration.getColumnsToGroupBy().size() == 0) {
                Subtotal subtotal = new Subtotal(transactions.get(0));
                for (Transaction t : transactions) {
                    pdf.addTableRow(t);
                    subtotal.addToSubtotal(t);
                }
                pdf.addGrandTotalRow(subtotal, hashMapOfTypes);
            } else {
                //Create custom comparator with needed columns
                TransactionComparator transactionComparator = new TransactionComparator(configuration.getColumnsToGroupBy(), hashMapOfTypes);
                transactions.sort(transactionComparator);

                //Create array of transaction to store subtotals and grand total
                Subtotal[] subtotals = new Subtotal[configuration.getColumnsToGroupBy().size() + 1];
                for (int j = 0; j < configuration.getColumnsToGroupBy().size() + 1; j++) {
                    subtotals[j] = new Subtotal(transactions.get(0));
                }


                //add header for the first grouping
                for (int k = 0; k < configuration.getColumnsToGroupBy().size(); k++) {
                    pdf.addGroupHead(configuration.getColumnsToGroupBy().get(k), transactions.get(0));
                    //TODO why it was 1 ?
                }

                for (int j = 0; j < transactions.size() - 1; j++) {
                    //for every transaction add values to all subtotals
                    for (Subtotal subtotalTransaction : subtotals) {
                        subtotalTransaction.addToSubtotal(transactions.get(j));
                    }
                    //print transaction
                    pdf.addTableRow(transactions.get(j));

                    //if next transaction has not equal some fields that we are grouping by
                    if (transactions.get(j).isFieldChanged(transactions.get(j + 1), configuration)) {
                        int positionOfChangedField = transactions.get(j).whatFieldIsChanged(transactions.get(j + 1), configuration);
                        int columnPlace = subtotals.length - 2 - positionOfChangedField;
                        //add subtotals
                        for (int k = 0; k <= columnPlace; k++) {
                            pdf.addSubtotalRow(configuration.getColumnsToGroupBy().get(k), subtotals[k], hashMapOfTypes);
                            subtotals[k] = new Subtotal(transactions.get(0));
                        }
                        //add header for next grouping
                        for (int k = positionOfChangedField; k < configuration.getColumnsToGroupBy().size(); k++) {
                            pdf.addGroupHead(configuration.getColumnsToGroupBy().get(k), transactions.get(j + 1));
                        }

                    }
                }
                //add the last transaction
                int j = transactions.size() - 1;
                for (Subtotal subtotalTransaction : subtotals) {
                    subtotalTransaction.addToSubtotal(transactions.get(j));
                }
                pdf.addTableRow(transactions.get(j));
                for (int k = 0; k < subtotals.length - 1; k++) {
                    pdf.addSubtotalRow(configuration.getColumnsToGroupBy().get(k), subtotals[k], hashMapOfTypes);
                }
                //add total
                pdf.addGrandTotalRow(subtotals[subtotals.length - 1], hashMapOfTypes);

            }

            doc.save("./result.pdf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
