package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
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
        HashMap<String, Float> maxLengthsOfTextInCell = new HashMap<>();

        //create array list to store max length of data in not string column
        HashMap<String, Float> notStringMaxLengths = new HashMap<>();

        try {
            //fill array that we will need to calculate cell width
            for (int i = 0; i < columnNames.size(); i++) {
                String line = columnNamesForTableHead.get(columnNames.get(i));
                float length = PDType1Font.HELVETICA_BOLD.getStringWidth(line + "  ") / 1000;

                if (!configuration.forceFontSize) {
                    if (configuration.isWrapTextInTable() && line.length() > configuration.getMaxCharactersInTextLine()) {
                        maxLengthsOfTextInCell.put(columnNames.get(i), PDType1Font.HELVETICA_BOLD.getStringWidth(line.substring(0, configuration.getMaxCharactersInTextLine() - 1) + "  ") / 1000);
                    } else {
                        maxLengthsOfTextInCell.put(columnNames.get(i), length);
                    }
                } else {
                    maxLengthsOfTextInCell.put(columnNames.get(i), length);
                    notStringMaxLengths.put(columnNames.get(i), (float) 0);

//                    //if font size is forced than count only for text column names
//                    if (hashMapOfTypes.get(columnNames.get(i)).equalsIgnoreCase("string")) {
//                        maxLengthsOfTextInCell.put(columnNames.get(i), length);
//                    } else {
//                        maxLengthsOfTextInCell.put(columnNames.get(i), (float) 0);
//                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //create array list of transactions and extract from JsonResponse, maxLengthsOfTextInCell are updated
        ArrayList<Transaction> transactions = jsonResponse.extractTransactions(maxLengthsOfTextInCell, notStringMaxLengths, configuration);

        //sort transactions
        for (int i = configuration.getColumnsToSortBy().size()-1; i >= 0; i--) {
            String[] sortBase = configuration.getColumnsToSortBy().get(i);
            String fieldNameForSorting = sortBase[0];
            String typeOfSorting = sortBase[1];
            String typeOfField = hashMapOfTypes.get(fieldNameForSorting);

            Comparator<Transaction> comp;
            if (typeOfField.equalsIgnoreCase("number")) {
                comp = Comparator.comparing((Transaction t) -> t.getNumberFields().get(fieldNameForSorting));
            } else if (typeOfField.equalsIgnoreCase("Datetime")) {
                comp = Comparator.comparing((Transaction t) -> t.getDateTimeFields().get(fieldNameForSorting));
            } else {
                comp = Comparator.comparing((Transaction t) -> t.getTextFields().get(fieldNameForSorting));
            }

            if (typeOfSorting.equalsIgnoreCase("desc")) {
                comp = comp.reversed();
            }

            transactions.sort(comp);
        }


        //clean maxLengthsOfTextInCell from columns that are hidden
        for (String string : configuration.getWhatColumnsToHide()) {
            maxLengthsOfTextInCell.remove(string);
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
        try {
            HashMap<String, Double> totals = new HashMap<>();
            for (String column : columnNames) {
                if (hashMapOfTypes.get(column).equalsIgnoreCase("number")) {
                    totals.put(column, (double) 0);
                }
            }
            for (Transaction t : transactions) {
                for (String column : totals.keySet()) {
                    Double d = totals.get(column);
                    totals.put(column, d + t.getNumberFields().get(column));
                }
            }
            for (String column : totals.keySet()) {
                String string = DoubleFormatter.format(totals.get(column), column, configuration);
                float l = PDType1Font.HELVETICA_BOLD.getStringWidth(string + "  ") / 1000;
                if (l > maxLengthsOfTextInCell.get(column)) {
                    maxLengthsOfTextInCell.put(column, l);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //load fonts
        File ordinaryFontFile = new File("arial.ttf");
        File boldFontFile = new File("arial-bold.ttf");

        //to suppress duplicates create hashmap where we are going to store the previous value for suppressed fields
        HashMap<String, String> textFieldsOfPreviousTransaction = new HashMap<>();
        for (String columnName: hashMapOfTypes.keySet()) {
            textFieldsOfPreviousTransaction.put(columnName, "");
        }

        //create preview
        if (configuration.isPreview()) {
            try (PDDocument doc = new PDDocument()) {
                Pdf pdf = new Pdf(doc, configuration, columnNames, maxLengthsOfTextInCell, notStringMaxLengths, hashMapOfTypes, columnNamesForTableHead, ordinaryFontFile, boldFontFile);
                pdf.addNewPage();
                pdf.createHeadOfReport();

                if (configuration.getColumnsToGroupBy() == null || configuration.getColumnsToGroupBy().size() == 0) {
                    Subtotal subtotal = new Subtotal(hashMapOfTypes);
                    for (Transaction t : transactions) {
                        if (doc.getNumberOfPages() <= configuration.getNumberOfPagesInPreview()) {
                            pdf.addTableRow(t, textFieldsOfPreviousTransaction);
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
                        subtotals[j] = new Subtotal(hashMapOfTypes);
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
                            pdf.addTableRow(transactions.get(j), textFieldsOfPreviousTransaction);
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
                                pdf.addSubtotalOrTotalRow(false, subgroupForSubtotal + ": " + transactions.get(j).getAllFieldsAsStrings().get(subgroupForSubtotal),
                                        subtotals[k], hashMapOfTypes);
                                subtotals[k] = new Subtotal(hashMapOfTypes);
                            }
                            //add header for next grouping
                            for (int k = positionOfChangedField; k < configuration.getColumnsToGroupBy().size(); k++) {
                                pdf.addGroupHead(configuration.getColumnsToGroupBy().get(k), transactions.get(j + 1), k+1);
                            }

                            //if we print subtotal and we are suppressing some values we should treat next transaction as a new one
                            //reset textFieldsOfPreviousTransaction
                            for (String columnName: hashMapOfTypes.keySet()) {
                                textFieldsOfPreviousTransaction.put(columnName, "");
                            }

                        }
                    }
                    //add the last transaction
                    if (doc.getNumberOfPages() <= configuration.getNumberOfPagesInPreview()) {
                        int j = transactions.size() - 1;
                        for (Subtotal subtotalTransaction : subtotals) {
                            subtotalTransaction.addToSubtotal(transactions.get(j));
                        }
                        pdf.addTableRow(transactions.get(j), textFieldsOfPreviousTransaction);
                        for (int k = 0; k < subtotals.length - 1; k++) {
                            //name of subgroupForSubtotal get from array of configuration.getColumnsToGroupBy starting from the end of the array
                            String subgroupForSubtotal = configuration.getColumnsToGroupBy().get(configuration.getColumnsToGroupBy().size()-k-1);
                            pdf.addSubtotalOrTotalRow(false, subgroupForSubtotal + ": " + transactions.get(j).getAllFieldsAsStrings().get(subgroupForSubtotal),
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

            //clean textFieldsOfPreviousTransaction after preview creating
            for (String columnName: hashMapOfTypes.keySet()) {
                textFieldsOfPreviousTransaction.put(columnName, "");
            }

        }



        //create result PDF document
        if (configuration.isPdfExport()) {
            try (PDDocument doc = new PDDocument()) {
                Pdf pdf = new Pdf(doc, configuration, columnNames, maxLengthsOfTextInCell, notStringMaxLengths, hashMapOfTypes, columnNamesForTableHead, ordinaryFontFile, boldFontFile);
                pdf.addNewPage();
                pdf.createHeadOfReport();

                if (configuration.getColumnsToGroupBy() == null || configuration.getColumnsToGroupBy().size() == 0) {
                    Subtotal subtotal = new Subtotal(hashMapOfTypes);
                    for (Transaction t : transactions) {
                        pdf.addTableRow(t, textFieldsOfPreviousTransaction);
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
                        subtotals[j] = new Subtotal(hashMapOfTypes);
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
                        pdf.addTableRow(transactions.get(j), textFieldsOfPreviousTransaction);

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
                                pdf.addSubtotalOrTotalRow(false, subgroupForSubtotal + ": " + transactions.get(j).getAllFieldsAsStrings().get(subgroupForSubtotal),
                                        subtotals[k], hashMapOfTypes);
                                subtotals[k] = new Subtotal(hashMapOfTypes);
                            }
                            //add header for next grouping
                            for (int k = positionOfChangedField; k < configuration.getColumnsToGroupBy().size(); k++) {
                                pdf.addGroupHead(configuration.getColumnsToGroupBy().get(k), transactions.get(j + 1), k+1);
                            }

                            //if we print subtotal and we are suppressing some values we should treat next transaction as a new one
                            //reset textFieldsOfPreviousTransaction
                            for (String columnName: hashMapOfTypes.keySet()) {
                                textFieldsOfPreviousTransaction.put(columnName, "");
                            }

                        }
                    }
                    //add the last transaction
                    int j = transactions.size() - 1;
                    for (Subtotal subtotalTransaction : subtotals) {
                        subtotalTransaction.addToSubtotal(transactions.get(j));
                    }
                    pdf.addTableRow(transactions.get(j), textFieldsOfPreviousTransaction);
                    for (int k = 0; k < subtotals.length - 1; k++) {
                        //name of subgroupForSubtotal get from array of configuration.getColumnsToGroupBy starting from the end of the array
                        String subgroupForSubtotal = configuration.getColumnsToGroupBy().get(configuration.getColumnsToGroupBy().size()-k-1);
                        pdf.addSubtotalOrTotalRow(false, subgroupForSubtotal + ": " + transactions.get(j).getAllFieldsAsStrings().get(subgroupForSubtotal),
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
