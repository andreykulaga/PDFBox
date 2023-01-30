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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    HashMap<String, Integer> textLengths;
    ArrayList<String> columnNames;
    HashMap<String, String> hashMapOfTypes;



    public Pdf(PDDocument document, Configuration configuration, ArrayList<String> columnNames, HashMap<String, Integer> textLengths, HashMap<String, String> hashMapOfTypes) {
        this.document = document;
        this.configuration = configuration;
        this.columnNames = columnNames;
        this.textLengths = textLengths;
        this.hashMapOfTypes = hashMapOfTypes;


        if (configuration.isChangeOrientationToLandscape()) {
            pageXSize = configuration.getPrintSize().getHeight();
            pageYSize = configuration.getPrintSize().getWidth();
        } else {
            pageXSize = configuration.getPrintSize().getWidth();
            pageYSize = configuration.getPrintSize().getHeight();
        }
        PDPage page = new PDPage(new PDRectangle(pageXSize, pageYSize));

        pageHeight = page.getTrimBox().getHeight();
        pageWidth = page.getTrimBox().getWidth();
        tableWidth = pageWidth - configuration.getLeftMargin() - configuration.getRightMargin();

        //count max length of all columns in characters to decide font size
        int rowCharacterLength = 0;
        for (String string : textLengths.keySet()) {
            rowCharacterLength += (textLengths.get(string) + 2); // +2 - is one space before and after text
        }

        //define font size
        fontSize = tableWidth * 1000 / (rowCharacterLength * configuration.getFont().getFontDescriptor().getAverageWidth());

        fontCapHeight = configuration.getFont().getFontDescriptor().getCapHeight() * fontSize / 1000;
        fontAscent = configuration.getFont().getFontDescriptor().getAscent() * fontSize / 1000;
        fontDescent = configuration.getFont().getFontDescriptor().getDescent() * fontSize / 1000;
        fontLeading = configuration.getFont().getFontDescriptor().getLeading() * fontSize / 1000;
        fontAverageWidth = configuration.getFont().getFontDescriptor().getAverageWidth() * fontSize / 1000;
        //define cell height by font and it's size
        cellHeight = fontCapHeight + fontAscent - fontDescent + fontLeading;
    }



    public void addNewPage() throws IOException {
        PDPage page = new PDPage(new PDRectangle(pageXSize, pageYSize));
        int pageNumber = document.getNumberOfPages()+1;

        document.addPage(page);

        initX = configuration.getLeftMargin();
        initY = pageHeight - configuration.getTopMargin();


        PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);

        //add TableHeader if needed
        if (pageNumber > 1 && configuration.isHeaderAtEveryPage()) {
            addTableHeader(contentStream);
        }

//        //add page number
//        if (configuration.isPrintPageNumber()) {
//            addCellWithText(contentStream, "Page number " + pageNumber, TextAlign.RIGHT, Color.WHITE, Outline.NOTOUTLINED, initX, configuration.getBottomMargin(), tableWidth);
//        }
        contentStream.close();
    }

    public void addFooters() throws IOException {

        //prepare text for time creation stamp
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        String text = offsetDateTime.format(DateTimeFormatter.ofPattern("mm/DD/yyyy h:mm:ss a O"));

        for (int i=0; i< document.getNumberOfPages(); i++) {
            PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(i), PDPageContentStream.AppendMode.APPEND, true);


            //add report ID
            addCellWithText(contentStream, configuration.getReportId(),
                    TextAlign.LEFT, Color.WHITE, Outline.NOTOUTLINED,
                    configuration.getLeftMargin(), configuration.getBottomMargin(), tableWidth);


            //add page number
            if (configuration.isPrintPageNumber()) {
                addCellWithText(contentStream, "Page " + (i+1) + " of " + document.getNumberOfPages(),
                        TextAlign.RIGHT, Color.WHITE, Outline.NOTOUTLINED,
                        configuration.getLeftMargin()+tableWidth/2, configuration.getBottomMargin(), tableWidth/2);
            }

            //add report creation date and time
            addCellWithText(contentStream, text,
                    TextAlign.LEFT, Color.WHITE, Outline.NOTOUTLINED,
                    configuration.getLeftMargin(), configuration.getBottomMargin() - cellHeight, tableWidth);

            contentStream.close();
        }


    }

    public void addTableHeader(PDPageContentStream contentStream) throws IOException {
        //count sum of characters of all widths
        int sumOfAllMaxWidth = 0;
        for (int i: textLengths.values()) {
            sumOfAllMaxWidth += i;
        };

        float cellWidth;
        for (String string: columnNames){
            cellWidth = tableWidth * textLengths.get(string) / sumOfAllMaxWidth;
            addCellWithText(contentStream, string,
                    TextAlign.CENTER, configuration.getHeadFillingColor(),
                    Outline.OUTLINED, initX, initY, cellWidth);
            initX += cellWidth;
        }
        initX = configuration.getLeftMargin();
        initY -= cellHeight;
    }

    public void addHeadOfTable() throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(0), PDPageContentStream.AppendMode.APPEND, true);

        //Add report name
        addCellWithText(contentStream, configuration.getReportName(), TextAlign.CENTER, configuration.getHeadFillingColor(), Outline.OUTLINED, initX, initY, tableWidth);
        initY -= cellHeight;


        //Add base for grouping
        if (configuration.getColumnsToGroupBy() != null && configuration.getColumnsToGroupBy().size() > 0) {
            String groupingBase = configuration.getColumnsToGroupBy().get(0);
            for (int i = 1; i < configuration.getColumnsToGroupBy().size(); i++) {
                groupingBase = groupingBase.concat(" & " + configuration.getColumnsToGroupBy().get(i));
            }
            addCellWithText(contentStream, "Grouping By " + groupingBase, TextAlign.LEFT, Color.WHITE, Outline.OUTLINED, initX, initY, tableWidth);

        } else {
            addCellWithText(contentStream, "", TextAlign.LEFT, Color.WHITE, Outline.OUTLINED, initX, initY, tableWidth);

        }

        //Add today date in the same row
        String localDate = "Date:  " + LocalDate.now();
        addCellWithText(contentStream, localDate, TextAlign.RIGHT, Color.WHITE, Outline.NOTOUTLINED, initX+tableWidth-fontAverageWidth*localDate.length(), initY, fontAverageWidth*localDate.length());
        initY -= cellHeight;

        //Add table header

        addTableHeader(contentStream);
        contentStream.close();
    }

    public void addTableRow(Transaction transaction) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);

        //count sum of characters of all widths
        int sumOfAllMaxWidth = 0;
        for (int i: textLengths.values()) {
            sumOfAllMaxWidth += i;
        }

        float cellWidth;
        int quantityOfLinesOfText = howManyLinesInARow(transaction);

        for (String string: columnNames){
            cellWidth = tableWidth * textLengths.get(string) / sumOfAllMaxWidth;
            TextAlign textAlign;
            if (hashMapOfTypes.get(string).equalsIgnoreCase("float")) {
                textAlign = TextAlign.RIGHT;
            } else {
                textAlign = TextAlign.LEFT;
            }
            addCellWithMultipleTextLines(contentStream, transaction.getAllValuesAsString().get(string),
                    textAlign, Color.WHITE, Outline.OUTLINED,
                    initX, initY, cellWidth, quantityOfLinesOfText);
            initX += cellWidth;
        }

        initX = configuration.getLeftMargin();
        initY -= cellHeight*quantityOfLinesOfText;

        contentStream.close();

        if (initY < configuration.getBottomMargin()+cellHeight) {
            addNewPage();
        }
    }

    public void addGroupHead(String columnName, Transaction transaction) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);

        int sumOfAllMaxWidth = 0;
        for (int i: textLengths.values()) {
            sumOfAllMaxWidth += i;
        }

        Color color = configuration.getGroupFillingColor();
        contentStream.setNonStrokingColor(configuration.getGroupFillingColor());

        String text = columnName + ": " + transaction.getAllValuesAsString().get(columnName);
        addCellWithText(contentStream, text, TextAlign.LEFT, color, Outline.OUTLINED, initX, initY, tableWidth);


        initX = configuration.getLeftMargin();
        initY -= cellHeight;

        contentStream.close();

        if (initY < configuration.getBottomMargin()+cellHeight) {
            addNewPage();
        }
    }

    public void addSubtotalRow(String columnName, Subtotal subtotal, HashMap<String, String> hashMapOfTypes) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);

        int sumOfAllMaxWidth = 0;
        for (int i: textLengths.values()) {
            sumOfAllMaxWidth += i;
        }

        Color color = configuration.getSubTotalFillingColor();
        contentStream.setNonStrokingColor(configuration.getGroupFillingColor());
        addCellWithText(contentStream, columnName + " Sub Total", TextAlign.LEFT, color, Outline.OUTLINED, initX, initY, tableWidth);



        float cellWidth;
        for (String tempColumnName: columnNames) {
            cellWidth = tableWidth * textLengths.get(tempColumnName) / sumOfAllMaxWidth;
            String text;

            String type = hashMapOfTypes.get(tempColumnName);

            if (type.equalsIgnoreCase("float")) {
                text = subtotal.getNumberFields().get(tempColumnName).toString();
                addCellWithText(contentStream, text,
                        TextAlign.RIGHT,
                        color, Outline.OUTLINED, initX, initY, cellWidth);
                initX += cellWidth;
            } else {
                initX += cellWidth;
            }
        }
        contentStream.close();


        initX = configuration.getLeftMargin();
        initY -= cellHeight;
        if (initY < configuration.getBottomMargin()+cellHeight) {
            addNewPage();
        }
    }

    public void addGrandTotalRow(Subtotal subtotal,  HashMap<String, String> hashMapOfTypes) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);

        int sumOfAllMaxWidth = 0;
        for (int i: textLengths.values()) {
            sumOfAllMaxWidth += i;
        }

        Color color = configuration.getSubTotalFillingColor();

        contentStream.setNonStrokingColor(configuration.getGroupFillingColor());
        addCellWithText(contentStream, "Grand Total", TextAlign.LEFT, color, Outline.OUTLINED, initX, initY, tableWidth);

        float cellWidth;

        for (String tempColumnName: columnNames) {
            cellWidth = tableWidth * textLengths.get(tempColumnName) / sumOfAllMaxWidth;
            String text;

            String type = hashMapOfTypes.get(tempColumnName);

            if (type.equalsIgnoreCase("float")) {
                text = subtotal.getNumberFields().get(tempColumnName).toString();

                addCellWithText(contentStream, text,
                        TextAlign.RIGHT,
                        color, Outline.OUTLINED, initX, initY, cellWidth);
                initX += cellWidth;
            } else {
                initX += cellWidth;
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
            textInitX = initX + cellWidth - textLength - fontAverageWidth;
        }

        //add text
        contentStream.beginText();
        contentStream.newLineAtOffset(textInitX, textInitY);
        contentStream.setFont(configuration.getFont(), fontSize);
        contentStream.showText(text);
        contentStream.endText();

    }


    public int howManyLinesInARow(Transaction transaction) {

        //return one line by default
        int result = 1;

        for (String text: transaction.getAllValuesAsString().values()) {
            if (text.length() > configuration.getMaxCharactersInTextLine()) {
                LinkedList<String> textByLines = new LinkedList<>();
                String[] strings = text.split(" ");
                textByLines.addAll(Arrays.asList(strings));

                int size = textByLines.size();
                for (int i = 0; i < size-1; i++) {
                    //divide word if it is too long
                    int max = configuration.getMaxCharactersInTextLine();
                    if (textByLines.get(i).length() > max) {
                        String string = textByLines.get(i);
                        textByLines.remove(i);
                        int numberOfParts = (int)Math.ceil((double)string.length() / (double)max);
                        for (int j = 0; j < numberOfParts-1; j++) {
                            String part = string.substring(j*max, (j+1)*max);
                            textByLines.add(i+j, part);
                        }
                        //add last part
                        String part = string.substring(max*(numberOfParts-1));
                        textByLines.add(i+numberOfParts-1, part);
                    }
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

                //divide word if it is too long
                int max = configuration.getMaxCharactersInTextLine();
                if (textByLines.get(i).length() > max) {
                    String string = textByLines.get(i);
                    textByLines.remove(i);
                    int numberOfParts = (int)Math.ceil((double)string.length() / (double)max);
                    for (int j = 0; j < numberOfParts-1; j++) {
                        String part = string.substring(j*max, (j+1)*max);
                        textByLines.add(i+j, part);
                    }
                    //add last part
                    String part = string.substring(max*(numberOfParts-1));
                    textByLines.add(i+numberOfParts-1, part);
                }

                //wrap words
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
            textInitX = initX + cellWidth - textLength - fontAverageWidth;
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
