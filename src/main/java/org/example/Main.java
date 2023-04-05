package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class Main {
    public static void main(String[] args) {


        //Create Configuration, read NewJsonConfigurationRequest and import it to Configuration;
        Configuration configuration = new Configuration();
        NewJsonConfigurationRequest nJCR = new NewJsonConfigurationRequest();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("./NewJsonConfigurationRequestModel.txt"))) {
            String line = bufferedReader.readLine();
            String myJsonString = "";
            
            while (line != null) {
                myJsonString = myJsonString.concat(line);
                line = bufferedReader.readLine();
            }
            
            ObjectMapper om = new ObjectMapper();    
            nJCR = om.readValue(myJsonString, NewJsonConfigurationRequest.class);
            configuration.importNewJsonConfigurationRequest(nJCR);
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        
        
        //Read JSON response 
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(JsonResponse.class, new JsonResponseDeserializer());
        mapper.registerModule(module);
        
        JsonResponse jsonResponse = new JsonResponse();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("./JsonResponseWithData.txt"))) {
            String line = bufferedReader.readLine();
            String myJsonString = "";
            
            while (line != null) {
                myJsonString = myJsonString.concat(line);
                line = bufferedReader.readLine();
            }
            
            jsonResponse = mapper.readValue(myJsonString, JsonResponse.class);
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        

        //create array list of all columns names
        ArrayList<String> columnNames = jsonResponse.createColumnNames();

        //create HashMap of column names for table head
//        HashMap<String, String> columnNamesForTableHead = jsonResponse.createColumnNamesForTableHead();
        HashMap<String, String> columnNamesForTableHead = configuration.getColumnNamesForTableHead();

        //create hash map of types of columns
        HashMap<String, String> hashMapOfTypes = jsonResponse.createHashMapOfTypes();


        //create array list that we will need to calculate cell width
        HashMap<String, Integer> textLengths = new HashMap<>();
        
        //fill array that we will need to calculate cell width
    
        for (int i = 0; i < columnNames.size(); i++) {
            String line = columnNamesForTableHead.get(columnNames.get(i));
            int length = line.length();
            
             
            //increase length for each capitalised letter, as capitilized letters are too wide
            String lineToCompare = line.toLowerCase();
            int howManyCapitalizedLetters = 0;
            for (int j = 0; j < length; j++) {
                if (line.charAt(j) != lineToCompare.charAt(j)) {
                    howManyCapitalizedLetters++;
                }
            }
            length += (howManyCapitalizedLetters*4/5);

            if (length > configuration.getMaxCharactersInTextLine()) {
                textLengths.put(columnNames.get(i), configuration.getMaxCharactersInTextLine());
            } else {
                textLengths.put(columnNames.get(i), length);
            }
        }
        
        //create array list of transactions and extract from JsonResponse, textLengths are updated
        ArrayList<Transaction> transactions = jsonResponse.extractTransactions(textLengths, configuration);

    

        //clean textLengths from columns that are hidden
        for (String string : configuration.getWhatColumnsToHide()) {
            textLengths.remove(string);
        }

        
        
        //clean array list of column names from hidden columns
        for (String string : configuration.getWhatColumnsToHide()) {
            columnNames.remove(string);
        }
        
        //warning message when there are more columns than max in configuration
        if (columnNames.size() > configuration.getMaxColumnsAllowed()) {
            JOptionPane optionPane = new JOptionPane("There are more columns in your report than column limit",JOptionPane.WARNING_MESSAGE);
            JDialog dialog = optionPane.createDialog("Warning!");
            dialog.setAlwaysOnTop(true); // to show top of all other application
            dialog.setVisible(true); // to visible the dialog
        }

        //clean map of types from hidden columns
        for (String string : configuration.getWhatColumnsToHide()) {
            hashMapOfTypes.remove(string);
        }

        //count sum of all number fields of transaction to count max width of it's columns
        HashMap<String, Double> totals = new HashMap<>();
        for (String column: columnNames) {
            if (hashMapOfTypes.get(column).equalsIgnoreCase("number")) {
                totals.put(column, (double) 0);
            }
        }
        for (Transaction t: transactions) {
            for (String column: totals.keySet()) {
                Double d = totals.get(column);
                totals.put(column, d+t.getNumberFields().get(column));
            }
        }
        for (String column: totals.keySet()) {
            int l = DoubleFormatter.format(totals.get(column), column, configuration).length();
            if (l > textLengths.get(column)) {
                textLengths.put(column, l);
            }
        }

        //create preview
        if (configuration.isPreview()) {
            try (PDDocument doc = new PDDocument()) {
                Pdf pdf = new Pdf(doc, configuration, columnNames, textLengths, hashMapOfTypes, columnNamesForTableHead);
                pdf.addNewPage();
                pdf.createHeadOfReport();

                if (configuration.getColumnsToGroupBy() == null || configuration.getColumnsToGroupBy().size() == 0) {
                    Subtotal subtotal = new Subtotal(transactions.get(0));
                    for (Transaction t : transactions) {
                        if (doc.getNumberOfPages() <= configuration.getNumberOfPagesInPreview()) {
                            pdf.addTableRow(t);
                            subtotal.addToSubtotal(t);
                        }
                    }
                    if (doc.getNumberOfPages() <= configuration.getNumberOfPagesInPreview()) {
                        pdf.addSubtotalOrTotalRow(true, "", subtotal, hashMapOfTypes);
                    }
                    if (doc.getNumberOfPages() > configuration.getNumberOfPagesInPreview()) {
                        doc.removePage(configuration.getNumberOfPagesInPreview());
                    }
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
                        pdf.addGroupHead(configuration.getColumnsToGroupBy().get(k), transactions.get(0), k+1);
                    }

                    for (int j = 0; j < transactions.size() - 1; j++) {
                        //for every transaction add values to all subtotals
                        for (Subtotal subtotalTransaction : subtotals) {
                            subtotalTransaction.addToSubtotal(transactions.get(j));
                        }
                        //print transaction
                        if (doc.getNumberOfPages() <= configuration.getNumberOfPagesInPreview()) {
                            pdf.addTableRow(transactions.get(j));
                        }

                        //if next transaction has not equal some fields that we are grouping by
                        if (transactions.get(j).isFieldChanged(transactions.get(j + 1), configuration)
                                && doc.getNumberOfPages() <= configuration.getNumberOfPagesInPreview()) {
                            int positionOfChangedField = transactions.get(j).whatFieldIsChanged(transactions.get(j + 1), configuration);
                            int columnPlace = subtotals.length - 2 - positionOfChangedField;
                            //add subtotals
                            for (int k = 0; k <= columnPlace; k++) {
                                //name of subgroupForSubtotal get from array of configuration.getColumnsToGroupBy starting from the end of the array
                                String subgroupForSubtotal = configuration.getColumnsToGroupBy().get(configuration.getColumnsToGroupBy().size()-k-1);
                                pdf.addSubtotalOrTotalRow(false, subgroupForSubtotal + ": " + transactions.get(j).getAllValuesAsString(configuration).get(subgroupForSubtotal),
                                        subtotals[k], hashMapOfTypes);
                                subtotals[k] = new Subtotal(transactions.get(0));
                            }
                            //add header for next grouping
                            for (int k = positionOfChangedField; k < configuration.getColumnsToGroupBy().size(); k++) {
                                pdf.addGroupHead(configuration.getColumnsToGroupBy().get(k), transactions.get(j + 1), k+1);
                            }

                        }
                    }
                    //add the last transaction
                    if (doc.getNumberOfPages() <= configuration.getNumberOfPagesInPreview()) {
                        int j = transactions.size() - 1;
                        for (Subtotal subtotalTransaction : subtotals) {
                            subtotalTransaction.addToSubtotal(transactions.get(j));
                        }
                        pdf.addTableRow(transactions.get(j));
                        for (int k = 0; k < subtotals.length - 1; k++) {
                            //name of subgroupForSubtotal get from array of configuration.getColumnsToGroupBy starting from the end of the array
                            String subgroupForSubtotal = configuration.getColumnsToGroupBy().get(configuration.getColumnsToGroupBy().size()-k-1);
                            pdf.addSubtotalOrTotalRow(false, subgroupForSubtotal + ": " + transactions.get(j).getAllValuesAsString(configuration).get(subgroupForSubtotal),
                                    subtotals[k], hashMapOfTypes);
                        }
                    }
                    //add total
                    if (doc.getNumberOfPages() <= configuration.getNumberOfPagesInPreview()) {
                        pdf.addSubtotalOrTotalRow(true, "", subtotals[subtotals.length - 1], hashMapOfTypes);
                    }
                    if (doc.getNumberOfPages() > configuration.getNumberOfPagesInPreview()) {
                        doc.removePage(configuration.getNumberOfPagesInPreview());
                    }
                }
                pdf.addFooters();
                doc.save("preview.pdf");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //create result PDF document
        if (configuration.isPdfExport()) {
            try (PDDocument doc = new PDDocument()) {
                Pdf pdf = new Pdf(doc, configuration, columnNames, textLengths, hashMapOfTypes, columnNamesForTableHead);
                pdf.addNewPage();
                pdf.createHeadOfReport();

                if (configuration.getColumnsToGroupBy() == null || configuration.getColumnsToGroupBy().size() == 0) {
                    Subtotal subtotal = new Subtotal(transactions.get(0));
                    for (Transaction t : transactions) {
                        pdf.addTableRow(t);
                        subtotal.addToSubtotal(t);
                    }
                    pdf.addSubtotalOrTotalRow(true, "", subtotal, hashMapOfTypes);
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
                        pdf.addGroupHead(configuration.getColumnsToGroupBy().get(k), transactions.get(0), k+1);
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
                            //define what field is changed. The result is a position of changed field in array configuration.columnsToGroupBy
                            int positionOfChangedField = transactions.get(j).whatFieldIsChanged(transactions.get(j + 1), configuration);
//                            int columnPlace = positionOfChangedField;
                            int columnPlace = subtotals.length - 2 - positionOfChangedField;
                            //add subtotals
                            for (int k = 0; k <= columnPlace; k++) {
                                //name of subgroupForSubtotal get from array of configuration.getColumnsToGroupBy starting from the end of the array
                                String subgroupForSubtotal = configuration.getColumnsToGroupBy().get(configuration.getColumnsToGroupBy().size()-k-1);
                                pdf.addSubtotalOrTotalRow(false, subgroupForSubtotal + ": " + transactions.get(j).getAllValuesAsString(configuration).get(subgroupForSubtotal),
                                        subtotals[k], hashMapOfTypes);
                                subtotals[k] = new Subtotal(transactions.get(0));
                            }
                            //add header for next grouping
                            for (int k = positionOfChangedField; k < configuration.getColumnsToGroupBy().size(); k++) {
                                pdf.addGroupHead(configuration.getColumnsToGroupBy().get(k), transactions.get(j + 1), k+1);
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
                        //name of subgroupForSubtotal get from array of configuration.getColumnsToGroupBy starting from the end of the array
                        String subgroupForSubtotal = configuration.getColumnsToGroupBy().get(configuration.getColumnsToGroupBy().size()-k-1);
                        pdf.addSubtotalOrTotalRow(false, subgroupForSubtotal + ": " + transactions.get(j).getAllValuesAsString(configuration).get(subgroupForSubtotal),
                                subtotals[k], hashMapOfTypes);
                    }
                    //add total
                    pdf.addSubtotalOrTotalRow(true, "", subtotals[subtotals.length - 1], hashMapOfTypes);

                }
                pdf.addFooters();
                doc.save(configuration.getOutputName() + ".pdf");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
