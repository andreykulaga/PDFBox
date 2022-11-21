package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Pdf {

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum Outline {
        OUTLINED,
        NOTOUTLINED

        }

    final PDDocument document;
    final Configuration configuration;
    float fontCapHeight;
    float fontAscent;
    float fontDescent;
    float fontLeading;
    float fontAverageWidth;
    float cellHeight;
    float pageHeight;
    float pageWidth;
    float tableWidth;
    float initX;
    float initY;
    float pageXSize;
    float pageYSize;
    float fontSize;
//    boolean doWeNeedToWrapTextInCell;

    HashMap<ColumnName, Integer> columnNameHashMap;


    public Pdf(PDDocument document, Configuration configuration, HashMap<ColumnName, Integer> columnNameHashMap) {
        this.document = document;
        this.configuration = configuration;
        this.columnNameHashMap = columnNameHashMap;


        pageXSize = PDRectangle.A3.getWidth();
        pageYSize = PDRectangle.A3.getHeight();
        PDPage page = new PDPage(new PDRectangle(pageYSize, pageXSize));

        pageHeight = page.getTrimBox().getHeight();
        pageWidth = page.getTrimBox().getWidth();
        tableWidth = pageWidth - configuration.getLeftMargin() - configuration.getRightMargin();

        //count max length of all columns in characters to decide font size
        int rowCharacterLength = 0;
        for (ColumnName columnName: ColumnName.values()){
            if (columnNameHashMap.containsKey(columnName)) {
                rowCharacterLength += (columnNameHashMap.get(columnName) + 2); // +2 - is one space before and after text
            }
        }

        //define font size
        fontSize = tableWidth*1000/(rowCharacterLength*configuration.getFont().getFontDescriptor().getAverageWidth());

        fontCapHeight = configuration.getFont().getFontDescriptor().getCapHeight() * fontSize/1000;
        fontAscent = configuration.getFont().getFontDescriptor().getAscent() * fontSize/1000;
        fontDescent = configuration.getFont().getFontDescriptor().getDescent() * fontSize/1000;
        fontLeading = configuration.getFont().getFontDescriptor().getLeading() * fontSize/1000;
        fontAverageWidth = configuration.getFont().getFontDescriptor().getAverageWidth() * fontSize/1000;
        //define cell height by font and it's size
        cellHeight = fontCapHeight + fontAscent - fontDescent + fontLeading;

//        for (ColumnName columnName: columnNameHashMap.keySet()) {
//            if (columnNameHashMap.get(columnName) > configuration.getMaxCharactersInTextLine())
//                doWeNeedToWrapTextInCell = true;
//        }

    }


    public void addNewPage() throws IOException {
        PDPage page = new PDPage(new PDRectangle(pageYSize, pageXSize));
        int pageNumber = document.getNumberOfPages()+1;

        document.addPage(page);

        initX = configuration.getLeftMargin();
        initY = pageHeight - configuration.getTopMargin();


        PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);

        //add TableHeader
        if (pageNumber > 1 && configuration.isHeaderAtEveryPage()) {
            int sumOfHashMapValues = 0;
            for (int i: columnNameHashMap.values()) {
                sumOfHashMapValues += i;
            };
            float cellWidth;
            for (ColumnName columnName: ColumnName.values()){
                if (columnNameHashMap.containsKey(columnName)) {
                    cellWidth = tableWidth * columnNameHashMap.get(columnName) / sumOfHashMapValues;
                    addCellWithText(contentStream, columnName.toString(),
                            configuration.getTextAlignInColumn().get(columnName), configuration.getHeadFillingColor(),
                            Outline.OUTLINED, initX, initY, cellWidth);
                    initX += cellWidth;
                }
            }
            initX = configuration.getLeftMargin();
            initY -= cellHeight;
        }

        //add page number
        addCellWithText(contentStream, "Page number " + pageNumber, TextAlign.RIGHT, Color.WHITE, Outline.NOTOUTLINED, initX, configuration.getBottomMargin(), tableWidth);
        contentStream.close();
    }

    public void addHeadOfTable() throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(0), PDPageContentStream.AppendMode.APPEND, true);

        //Add report name
        addCellWithText(contentStream, configuration.getReportName(), TextAlign.CENTER, configuration.getHeadFillingColor(), Outline.OUTLINED, initX, initY, tableWidth);
        initY -= cellHeight;


        //Add base for grouping
        if (configuration.getColumnsToGroupBy() != null && configuration.getColumnsToGroupBy().length > 0) {
            String groupingBase = configuration.getColumnsToGroupBy()[0].toString();
            for (int i = 1; i < configuration.getColumnsToGroupBy().length; i++) {
                groupingBase = groupingBase.concat(" & " + configuration.getColumnsToGroupBy()[i]);
            }
            addCellWithText(contentStream, "Grouping By " + groupingBase, TextAlign.LEFT, Color.WHITE, Outline.OUTLINED, initX, initY, tableWidth);

        } else {
            addCellWithText(contentStream, "", TextAlign.LEFT, Color.WHITE, Outline.OUTLINED, initX, initY, tableWidth);

        }

        //Add today date in the same row
        String localDate = "Date:  " + LocalDate.now();
        addCellWithText(contentStream, localDate, TextAlign.RIGHT, Color.WHITE, Outline.NOTOUTLINED, initX+tableWidth-fontAverageWidth*localDate.length(), initY, fontAverageWidth*localDate.length());
        initY -= cellHeight;

        int sumOfHashMapValues = 0;
        for (int i: columnNameHashMap.values()) {
            sumOfHashMapValues += i;
        };

        float cellWidth;
        for (ColumnName columnName: ColumnName.values()){
            if (columnNameHashMap.containsKey(columnName)) {
                cellWidth = tableWidth * columnNameHashMap.get(columnName) / sumOfHashMapValues;
                addCellWithText(contentStream, columnName.toString(),
                        configuration.getTextAlignInColumn().get(columnName), configuration.getHeadFillingColor(),
                        Outline.OUTLINED, initX, initY, cellWidth);
                initX += cellWidth;
            }
        }
        contentStream.close();
        initX = configuration.getLeftMargin();
        initY -= cellHeight;
    }

    public void addTableRow(Transaction transaction) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);
        int sumOfHashMapValues = 0;
        for (int i: columnNameHashMap.values()) {
            sumOfHashMapValues += i;
        };

        float cellWidth;
        int quantityOfLinesOfText = howManyLinesInARow(transaction);
//        if (doWeNeedToWrapTextInCell) {
            for (ColumnName columnName: ColumnName.values()){
                if (columnNameHashMap.containsKey(columnName)) {
                    cellWidth = tableWidth * columnNameHashMap.get(columnName) / sumOfHashMapValues;
                    addCellWithMultipleTextLines(contentStream, transaction.getValue(columnName),
                            configuration.getTextAlignInColumn().get(columnName), Color.WHITE, Outline.OUTLINED,
                            initX, initY, cellWidth, quantityOfLinesOfText);
                    initX += cellWidth;
                }
            }
//        } else {
//            for (ColumnName columnName: ColumnName.values()){
//                if (columnNameHashMap.containsKey(columnName)) {
//                    cellWidth = tableWidth * columnNameHashMap.get(columnName) / sumOfHashMapValues;
//                    addCellWithText(contentStream, transaction.getValue(columnName),
//                            configuration.getTextAlignInColumn().get(columnName), Color.WHITE, Outline.OUTLINED,
//                            initX, initY, cellWidth);
//                    initX += cellWidth;
//                }
//            }
//        }

        contentStream.close();
        initX = configuration.getLeftMargin();
        initY -= cellHeight*quantityOfLinesOfText;

        if (initY < configuration.getBottomMargin()+cellHeight) {
            addNewPage();
        }
    }

    public void addGroupHead(ColumnName columnName, Transaction transaction) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);
        int sumOfHashMapValues = 0;
        for (int i: columnNameHashMap.values()) {
            sumOfHashMapValues += i;
        };

        Color color = configuration.getGroupFillingColor();
        contentStream.setNonStrokingColor(configuration.getGroupFillingColor());

        String text = columnName.toString() + ": " + transaction.getValue(columnName);
        addCellWithText(contentStream, text, TextAlign.LEFT, color, Outline.OUTLINED, initX, initY, tableWidth);

        contentStream.close();

        initX = configuration.getLeftMargin();
        initY -= cellHeight;
        if (initY < configuration.getBottomMargin()+cellHeight) {
            addNewPage();
        }
    }

    public void addSubtotalRow(ColumnName columnName, Transaction transaction) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);
        int sumOfHashMapValues = 0;
        for (int i: columnNameHashMap.values()) {
            sumOfHashMapValues += i;
        };

        Color color = configuration.getSubTotalFillingColor();
        contentStream.setNonStrokingColor(configuration.getGroupFillingColor());
        addCellWithText(contentStream, columnName.toString() + " Sub Total", TextAlign.LEFT, color, Outline.OUTLINED, initX, initY, tableWidth);



        float cellWidth;
        for (ColumnName columnNameTemp: ColumnName.values()){
            if (columnNameHashMap.containsKey(columnNameTemp)) {
                cellWidth = tableWidth * columnNameHashMap.get(columnNameTemp) / sumOfHashMapValues;
                String text;
                if (columnNameTemp == ColumnName.Unit_Price_Local_Amt ||
                        columnNameTemp == ColumnName.Net_Local_Amt ||
                        columnNameTemp == ColumnName.Tax_Local_Amt) {
                    text = transaction.getValue(columnNameTemp);
                    addCellWithText(contentStream, text,
                            configuration.getTextAlignInColumn().get(columnNameTemp),
                            color, Outline.OUTLINED, initX, initY, cellWidth);
                    initX += cellWidth;
                } else {
                    initX += cellWidth;
                }
            }
        }
        contentStream.close();

        initX = configuration.getLeftMargin();
        initY -= cellHeight;
        if (initY < configuration.getBottomMargin()+cellHeight) {
            addNewPage();
        }
    }

    public void addGrandTotalRow(Transaction transaction) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);
        int sumOfHashMapValues = 0;
        for (int i: columnNameHashMap.values()) {
            sumOfHashMapValues += i;
        };
        Color color = configuration.getSubTotalFillingColor();

        contentStream.setNonStrokingColor(configuration.getGroupFillingColor());
        addCellWithText(contentStream, "Grand Total", TextAlign.LEFT, color, Outline.OUTLINED, initX, initY, tableWidth);

        float cellWidth;
        for (ColumnName columnNameTemp: ColumnName.values()){
            if (columnNameHashMap.containsKey(columnNameTemp)) {
                cellWidth = tableWidth * columnNameHashMap.get(columnNameTemp) / sumOfHashMapValues;
                String text;
                if (columnNameTemp == ColumnName.Unit_Price_Local_Amt ||
                        columnNameTemp == ColumnName.Net_Local_Amt ||
                        columnNameTemp == ColumnName.Tax_Local_Amt) {
                    text = transaction.getValue(columnNameTemp);
                    addCellWithText(contentStream, text,
                            configuration.getTextAlignInColumn().get(columnNameTemp),
                            color, Outline.OUTLINED, initX, initY, cellWidth);
                    initX += cellWidth;
                } else {
                    initX += cellWidth;
                }
            }
        }
        contentStream.close();

        initX = configuration.getLeftMargin();
        initY -= cellHeight;
        if (initY < configuration.getBottomMargin()+cellHeight) {
            addNewPage();
        }
    }

    public void addCellWithText(PDPageContentStream contentStream, String text,
                                TextAlign textAlign, Color fillingColor, Outline outline,
                                float initX, float initY, float cellWidth) throws IOException {

        //set color and draw stroke rectangle
        if (outline == Outline.OUTLINED) {
            contentStream.setStrokingColor(configuration.getStrokingColor());
            contentStream.setLineWidth(configuration.getLineWidth());
            contentStream.addRect(initX, initY, cellWidth, -cellHeight);
            contentStream.stroke();
        }


        //set color and draw filling rectangle
        contentStream.setNonStrokingColor(fillingColor);
        contentStream.addRect(initX, initY, cellWidth, -cellHeight);
        contentStream.fill();


        //set color for text
        contentStream.setNonStrokingColor(configuration.getFontColor());

        //define starting position of text
        float textInitY = (float) (initY - cellHeight - fontDescent + (cellHeight * 0.1));
        float textInitX = 0;
        float textLength = text.length() * fontAverageWidth;

        if (textAlign == TextAlign.LEFT) {
            textInitX = initX + fontAverageWidth;
        }
        if (textAlign == TextAlign.CENTER) {
            textInitX = initX + cellWidth/2 - textLength/2;
        }
        if (textAlign == TextAlign.RIGHT) {
            textInitX = initX + cellWidth - textLength;
        }

        //add text
        contentStream.beginText();
        contentStream.newLineAtOffset(textInitX, textInitY);
        contentStream.setFont(configuration.getFont(), fontSize);
        contentStream.showText(text);
        contentStream.endText();

    }


    public int howManyLinesInARow(Transaction transaction) {
        int result = 1;

        for (ColumnName columnName: ColumnName.values()){
            if (columnNameHashMap.containsKey(columnName)) {
                String text = transaction.getValue(columnName);
                //if text is small enough, only one line
                if (text.length() > configuration.getMaxCharactersInTextLine()) {
                    LinkedList<String> textByLines = new LinkedList<>();
                    String[] strings = text.split(" ");
                    textByLines.addAll(Arrays.asList(strings));
                    int size = textByLines.size();
                    for (int i = 0; i < size-1; i++) {
                        while ((textByLines.get(i).length() + 1 + textByLines.get(i+1).length()) <= configuration.getMaxCharactersInTextLine()) {
                            String newString = textByLines.get(i) + " " + textByLines.get(i+1);
                            textByLines.set(i, newString);
                            textByLines.remove(i+1);
                            size--;

                            if (i+1 == size) {
                                break;
                            }
                        }
                    }
                    if (textByLines.size() > result) {
                        result = textByLines.size();
                    }
                }
            }
        }
        return result;
    }

    public void addCellWithMultipleTextLines(PDPageContentStream contentStream, String text,
                                TextAlign textAlign, Color fillingColor, Outline outline,
                                float initX, float initY, float cellWidth, int quantityOfLines) throws IOException {

        //create linked list of all words in text
        LinkedList<String> textByLines = new LinkedList<>();
        //if text is small enough, add only one line
        if (text.length() <= configuration.getMaxCharactersInTextLine()) {
            textByLines.add(text);
        } else {
            String[] strings = text.split(" ");
            textByLines.addAll(Arrays.asList(strings));
            int size = textByLines.size();
            for (int i = 0; i < size-1; i++) {
                while ((textByLines.get(i).length() + 1 + textByLines.get(i+1).length()) <= configuration.getMaxCharactersInTextLine()) {
                    String newString = textByLines.get(i) + " " + textByLines.get(i+1);
                    textByLines.set(i, newString);
                    textByLines.remove(i+1);
                    size--;

                    if (i+1 == size) {
                        break;
                    }
                }
            }
        }

        //set color and draw stroke rectangle
        if (outline == Outline.OUTLINED) {
            contentStream.setStrokingColor(configuration.getStrokingColor());
            contentStream.setLineWidth(configuration.getLineWidth());
            contentStream.addRect(initX, initY, cellWidth, -(cellHeight * quantityOfLines));
            contentStream.stroke();
        }



        //set color and draw filling rectangle
        contentStream.setNonStrokingColor(fillingColor);
        contentStream.addRect(initX, initY, cellWidth, -(cellHeight * quantityOfLines));
        contentStream.fill();


        //set color for text
        contentStream.setNonStrokingColor(configuration.getFontColor());

        //define starting position of text
        float textInitY = (float) (initY - cellHeight - fontDescent + (cellHeight * 0.1));
        float textInitX = 0;
        float textLength = text.length() * fontAverageWidth;
        //change textLength if it is more than max
        if (text.length() > configuration.getMaxCharactersInTextLine()) {
            textLength = configuration.getMaxCharactersInTextLine() * fontAverageWidth;
        }
        if (textAlign == TextAlign.LEFT) {
            textInitX = initX + fontAverageWidth;
        }
        if (textAlign == TextAlign.CENTER) {
            textInitX = initX + cellWidth/2 - textLength/2;
        }
        if (textAlign == TextAlign.RIGHT) {
            textInitX = initX + cellWidth - textLength;
        }

        //add text
        contentStream.beginText();
        contentStream.newLineAtOffset(textInitX, textInitY);
        contentStream.setFont(configuration.getFont(), fontSize);
        contentStream.setLeading(cellHeight);
        for (String string: textByLines) {
            contentStream.showText(string);
            contentStream.newLine();
        }
        contentStream.endText();
    }
 }
