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
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

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
    HashMap<String, String> columnNamesForTableHead;
    HashMap<String, String> hashMapOfTypes;



    public Pdf(PDDocument document, Configuration configuration, ArrayList<String> columnNames, 
    HashMap<String, Integer> textLengths, HashMap<String, String> hashMapOfTypes, HashMap<String, String> columnNamesForTableHead) {
        this.document = document;
        this.configuration = configuration;
        this.columnNames = columnNames;
        this.textLengths = textLengths;
        this.hashMapOfTypes = hashMapOfTypes;
        this.columnNamesForTableHead = columnNamesForTableHead;


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
        for (int i : textLengths.values()) {
//            rowCharacterLength += (textLengths.get(string) + 2); // +2 - is one space before and after text
            rowCharacterLength += (i +2); // +2 - is one space before and after text
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

        //define cell height by font and it's size

        float footerFontCapHeight = configuration.getFont().getFontDescriptor().getCapHeight() * configuration.getPageFooterFontSize() / 1000;
        float footerFontAscent = configuration.getFont().getFontDescriptor().getAscent() * configuration.getPageFooterFontSize() / 1000;
        float footerFontDescent = configuration.getFont().getFontDescriptor().getDescent() * configuration.getPageFooterFontSize() / 1000;
        float footerFontLeading = configuration.getFont().getFontDescriptor().getLeading() * configuration.getPageFooterFontSize() / 1000;
        float footerCellHeight = footerFontCapHeight + footerFontAscent - footerFontDescent + footerFontLeading;


        //change global cell height and fontDescent
        float tempCellHeight = cellHeight;
        float tempFontDescent = fontDescent;
        fontDescent = footerFontDescent;
        cellHeight = footerCellHeight;

        for (int i=0; i< document.getNumberOfPages(); i++) {
            PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(i), PDPageContentStream.AppendMode.APPEND, true);

            //add page number
            if (configuration.isPageNumberFlag()) {
                
                
                addCellWithText(contentStream, "Page " + (i+1) + " of " + document.getNumberOfPages(),
                TextAlign.CENTER, configuration.getPageFooterBackGroundColor(), configuration.getPageFooterFontColor(), Outline.NOTOUTLINED,
                configuration.getLeftMargin(),
                configuration.getBottomMargin() - (footerCellHeight * (configuration.getLinesOfPageFooter().size()-1))/2, 
                tableWidth, configuration.getPageFooterFontSize());
            }
            //add text lines
            float tab = 0;
            float footerFontAverageWidth = configuration.getFont().getFontDescriptor().getAverageWidth() * configuration.getPageFooterFontSize() / 1000;
            for (int j=0; j < configuration.getLinesOfPageFooter().size(); j++) {
                String st = configuration.getLinesOfPageFooter().get(j);
                //count the longest line to tabulate page number to prevent overlap
                if (st.length() > tab) {
                    tab = st.length();
                }
                addCellWithText(contentStream, st,
                TextAlign.LEFT, configuration.getPageFooterBackGroundColor(), configuration.getPageFooterFontColor(), Outline.NOTOUTLINED,
                configuration.getLeftMargin(), configuration.getBottomMargin() - (footerCellHeight * j), (tab+1) * footerFontAverageWidth, configuration.getPageFooterFontSize());
            }

            contentStream.close();
        }

        //change global cell height and font descendtback
        cellHeight = tempCellHeight;
        fontDescent = tempFontDescent;


    }

    public void addTableHeader(PDPageContentStream contentStream) throws IOException {
        //count sum of characters of all widths
        int sumOfAllMaxWidth = 0;
        for (int i: textLengths.values()) {
            sumOfAllMaxWidth += i;
        };

        //create fake transaction from column names to count how many lines need for table header
        Transaction transaction = Transaction.createTransactionFromColumnNames(columnNames, columnNamesForTableHead);
        int quantityOfLines = howManyLinesInARow(transaction);


        float cellWidth;
        for (String string: columnNames){
            cellWidth = tableWidth * textLengths.get(string) / sumOfAllMaxWidth;

            String text = transaction.getAllValuesAsString(configuration).get(string);

            addCellWithMultipleTextLines(contentStream, text,
                    TextAlign.CENTER, configuration.getHeadFillingColor(),
                    configuration.getDefaultFontColor(), Outline.OUTLINED, initX, initY, cellWidth, quantityOfLines, configuration.isOnlyVerticalCellBoards());
            initX += cellWidth;
        }
        initX = configuration.getLeftMargin();
        initY -= cellHeight*quantityOfLines;
    }

    public void addHeadOfTable() throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(0), PDPageContentStream.AppendMode.APPEND, true);

        //add all lines of Header line by line

        for (int i=0; i<configuration.getPageHeaderConfiguration().size(); i++) {
            //define font size
            float pageHeaderFontSize = Float.parseFloat(configuration.getPageHeaderConfiguration().get(i).get("fontSize"));

            //define font color
            Color pageHeaderFontColor;
            String fontColorName = configuration.getPageHeaderConfiguration().get(i).get("textColor").toLowerCase();
            try {
                pageHeaderFontColor = Color.decode(fontColorName);
            } catch (NumberFormatException e) {
                pageHeaderFontColor = Color.black;
            }
            
            //define background color
            Color pageHeaderBackGroundColor;
            String backgroundColorName = configuration.getPageHeaderConfiguration().get(i).get("backGroundColor").toLowerCase();
            try {
                pageHeaderBackGroundColor = Color.decode(backgroundColorName);
            } catch (NumberFormatException e) {
                pageHeaderBackGroundColor = Color.white;
            }

            //define cell height by font and it's size
            float headerFontCapHeight = configuration.getFont().getFontDescriptor().getCapHeight() * pageHeaderFontSize / 1000;
            float headerFontAscent = configuration.getFont().getFontDescriptor().getAscent() * pageHeaderFontSize / 1000;
            float headerFontDescent = configuration.getFont().getFontDescriptor().getDescent() * pageHeaderFontSize / 1000;
            float headerFontLeading = configuration.getFont().getFontDescriptor().getLeading() * pageHeaderFontSize / 1000;
            float headerCellHeight = headerFontCapHeight + headerFontAscent - headerFontDescent + headerFontLeading;

            //change global cell height and font descent
            Float tempCellHeight = cellHeight;
            Float tempFontDescent = fontDescent;
            cellHeight = headerCellHeight;
            fontDescent = headerFontDescent;

            //count all cell lengths
            ArrayList<Integer> cellLengths = new ArrayList<>();
            int allLengths = 0;
            for (int z=0; z < configuration.getPageHeaderLines().get(i).size(); z++) {
                int l = configuration.getPageHeaderLines().get(i).get(z).length();
                cellLengths.add(l);
                allLengths += l;
            }
            //draw the line by drawing each part of it's data
            for (int j=0; j<configuration.getPageHeaderLines().get(i).size(); j++) {
                String text = configuration.getPageHeaderLines().get(i).get(j);

                TextAlign textAlign;
                if (j==1 || j%2 !=0 ) {
                    textAlign = TextAlign.LEFT;
                } else {
                    textAlign = TextAlign.RIGHT;
                    text = text.concat(":");
                }
                //if the previous text was empty, then text align center
                if (j > 0 && configuration.getPageHeaderLines().get(i).get(j-1).length() ==0 ) {
    
                    textAlign = TextAlign.CENTER;
                }

                float cellWidth = tableWidth * cellLengths.get(j) / allLengths;

                addCellWithText(contentStream, text,
                textAlign, pageHeaderBackGroundColor, pageHeaderFontColor, Outline.NOTOUTLINED,
                initX, initY, cellWidth, pageHeaderFontSize);
                initX += cellWidth;
            }
            initX = configuration.getLeftMargin();
            initY -= headerCellHeight;

            //change global cell height and font descent back
            cellHeight = tempCellHeight;
            fontDescent = tempFontDescent;

        }

        
        // //Add base for grouping
        // if (configuration.getColumnsToGroupBy() != null && configuration.getColumnsToGroupBy().size() > 0) {
        //     String groupingBase = configuration.getColumnsToGroupBy().get(0);
        //     for (int i = 1; i < configuration.getColumnsToGroupBy().size(); i++) {
        //         groupingBase = groupingBase.concat(" & " + configuration.getColumnsToGroupBy().get(i));
        //     }
        //     addCellWithText(contentStream, "Grouping By " + groupingBase, TextAlign.LEFT, Color.WHITE,
        //             configuration.getFontColor(), Outline.OUTLINED, initX, initY, tableWidth, fontSize);

        // } else {
        //     addCellWithText(contentStream, "", TextAlign.LEFT, Color.WHITE,
        //             configuration.getFontColor(), Outline.OUTLINED, initX, initY, tableWidth, fontSize);

        // }
        // initY -= cellHeight;

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
            TextAlign textAlign = configuration.getTextAlignment().get(string);
            Color fontColor = configuration.getTextColor().get(string);

            if (hashMapOfTypes.get(string).equalsIgnoreCase("number")) {
                //change font color if it is negative
                if (transaction.getNumberFields().get(string) < 0) {
                    fontColor = configuration.getNegativeValueColor().get(string);
                }
            }

            addCellWithMultipleTextLines(contentStream, transaction.getAllValuesAsString(configuration).get(string),
                    textAlign, Color.WHITE, fontColor, Outline.OUTLINED,
                    initX, initY, cellWidth, quantityOfLinesOfText, configuration.isOnlyVerticalCellBoards());
            initX += cellWidth;
        }

        initX = configuration.getLeftMargin();
        initY -= cellHeight*quantityOfLinesOfText;

        if (initY < configuration.getBottomMargin()+cellHeight) {
            if (configuration.isOnlyVerticalCellBoards()) {
                contentStream.setStrokingColor(configuration.getStrokingColor());
                contentStream.setLineWidth(configuration.getLineWidth()/2);
                contentStream.moveTo(initX, initY);
                contentStream.lineTo(initX+tableWidth, initY);
                contentStream.stroke();
                }
            addNewPage();
        }
        contentStream.close();
    }

    public void addGroupHead(String columnName, Transaction transaction, int level) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);

        int sumOfAllMaxWidth = 0;
        for (int i: textLengths.values()) {
            sumOfAllMaxWidth += i;
        }

        Color color;
        if (level == 1) {
            float[] green = Color.RGBtoHSB(107, 136, 17, null);
            color = Color.getHSBColor(green[0], green[1], green[2]);
        } else {
            float[] lightGreen = Color.RGBtoHSB(192, 201, 170, null);
            color = Color.getHSBColor(lightGreen[0], lightGreen[1], lightGreen[2]);
        }

        contentStream.setNonStrokingColor(configuration.getGroupFillingColor());

        String text = columnName + ": " + transaction.getAllValuesAsString(configuration).get(columnName);
        if (level <= 2) {
            addCellWithText(contentStream, text, TextAlign.LEFT, color,
                    configuration.getDefaultFontColor(), Outline.OUTLINED,
                    initX, initY, tableWidth, fontSize);
        } else {
            addCellWithTextWithTabulation(contentStream, text, TextAlign.LEFT, color,
                    configuration.getDefaultFontColor(), Outline.OUTLINED,
                    initX, initY, tableWidth, level-2);
        }



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
        addCellWithText(contentStream, "Sub-total: " + columnName, TextAlign.LEFT, color,
                configuration.getDefaultFontColor(), Outline.OUTLINED, initX, initY, tableWidth, fontSize);



        float cellWidth;
        for (String tempColumnName: columnNames) {
            cellWidth = tableWidth * textLengths.get(tempColumnName) / sumOfAllMaxWidth;
            String text;
            String type = hashMapOfTypes.get(tempColumnName);

            if (type.equalsIgnoreCase("number")) {
                double dbl = subtotal.getNumberFields().get(tempColumnName);
                
                text = DoubleFormatter.format(subtotal.getNumberFields().get(tempColumnName), tempColumnName, configuration); 

                //change color if number is negative
                if (dbl < 0) {
                    addCellWithText(contentStream, text,
                    configuration.getTextAlignment().get(tempColumnName), color,
                            configuration.getNegativeValueColor().get(tempColumnName), Outline.OUTLINED, initX, initY, cellWidth, fontSize);
                } else {
                    addCellWithText(contentStream, text,
                    configuration.getTextAlignment().get(tempColumnName), color,
                    configuration.getTextColor().get(tempColumnName), Outline.OUTLINED, initX, initY, cellWidth, fontSize);
                }
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

        Color color = Color.lightGray;

        contentStream.setNonStrokingColor(configuration.getGroupFillingColor());
        addCellWithText(contentStream, "Grand Total", TextAlign.LEFT, color,
                configuration.getDefaultFontColor(), Outline.OUTLINED, initX, initY, tableWidth, fontSize);

        float cellWidth;

        for (String tempColumnName: columnNames) {
            cellWidth = tableWidth * textLengths.get(tempColumnName) / sumOfAllMaxWidth;
            String text;

            String type = hashMapOfTypes.get(tempColumnName);

            if (type.equalsIgnoreCase("number")) {
                double dbl = subtotal.getNumberFields().get(tempColumnName);

                text = DoubleFormatter.format(subtotal.getNumberFields().get(tempColumnName), tempColumnName, configuration);

                //change color if number is negative
                if (dbl < 0) {
                    addCellWithText(contentStream, text,
                    configuration.getTextAlignment().get(tempColumnName), color,
                    configuration.getNegativeValueColor().get(tempColumnName), Outline.OUTLINED, initX, initY, cellWidth, fontSize);
                } else {
                    addCellWithText(contentStream, text,
                    configuration.getTextAlignment().get(tempColumnName), color,
                    configuration.getTextColor().get(tempColumnName), Outline.OUTLINED, initX, initY, cellWidth, fontSize);
                }

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
                                TextAlign textAlign, Color fillingColor, Color fontColor, Outline outline,
                                float initX, float initY, float cellWidth, float sizeOfFont) throws IOException {

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
        contentStream.setNonStrokingColor(fontColor);

        //define starting position of text
        float textInitY = (float) (initY - cellHeight - fontDescent + (cellHeight * 0.1));
        float textInitX = 0;

        //calculate string length in points
        float stringWidth = configuration.getFont().getStringWidth(text) / 1000 * sizeOfFont;

        if (textAlign == TextAlign.LEFT) {
            textInitX = initX + fontAverageWidth;
        }
        if (textAlign == TextAlign.CENTER) {
            textInitX = initX + cellWidth/2 - stringWidth/2;
        }
        if (textAlign == TextAlign.RIGHT) {
            textInitX = initX + cellWidth - stringWidth - fontAverageWidth;
        }

        //add text
        contentStream.beginText();
        contentStream.newLineAtOffset(textInitX, textInitY);
        contentStream.setFont(configuration.getFont(), sizeOfFont);
        contentStream.showText(text);
        contentStream.endText();

    }

    public void addCellWithTextWithTabulation(PDPageContentStream contentStream, String text,
                                TextAlign textAlign, Color fillingColor, Color fontColor, Outline outline,
                                float initX, float initY, float cellWidth, int howManyTabulation) throws IOException {

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
        contentStream.setNonStrokingColor(fontColor);

        //define starting position of text
        float textInitY = (float) (initY - cellHeight - fontDescent + (cellHeight * 0.1));
        float textInitX = 0;
        float textLength = text.length() * fontAverageWidth;

        if (textAlign == TextAlign.LEFT) {
            textInitX = initX + fontAverageWidth + 8*fontAverageWidth*howManyTabulation;
        }
        if (textAlign == TextAlign.CENTER) {
            textInitX = initX + cellWidth/2 - textLength/2 + 8*fontAverageWidth*howManyTabulation;
        }
        if (textAlign == TextAlign.RIGHT) {
            textInitX = initX + cellWidth - textLength - fontAverageWidth + 8*fontAverageWidth*howManyTabulation;
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

        for (String text : transaction.getAllValuesAsString(configuration).values()){
            //create linked list of all words in text
            LinkedList<String> textByLines = new LinkedList<>();
            //if text is small enough, add only one line
            if (text.length() > configuration.getMaxCharactersInTextLine()) {
                String[] strings = text.split(" ");
                textByLines.addAll(Arrays.asList(strings));
                int size = textByLines.size();

                int i = 0;
                while (i < size) {
                    //divide word if it is too long
                    int max = configuration.getMaxCharactersInTextLine();
                    if (textByLines.get(i).length() > max) {
                        String string = textByLines.get(i);
                        textByLines.remove(i);
                        size--;
                        int numberOfParts = (int)Math.ceil((double)string.length() / (double)max);
                        for (int j = 0; j < numberOfParts-1; j++) {
                            String part = string.substring(j*max, (j+1)*max);
                            textByLines.add(i+j, part);
                            size++;
                        }
                        //add last part
                        String part = string.substring(max*(numberOfParts-1));
                        textByLines.add(i+numberOfParts-1, part);
                        size++;
                    }
                    i++;
                }

                //wrap words
                int k = 0;
                while (k < size-1) {
                    if ((textByLines.get(k).length() + 1 + textByLines.get(k + 1).length()) <= configuration.getMaxCharactersInTextLine()) {
                        String newString = textByLines.get(k) + " " + textByLines.get(k + 1);
                        textByLines.set(k, newString);
                        textByLines.remove(k + 1);
                        size--;
                        k--;
                    }
                    k++;
                }
                if (textByLines.size() > result) {
                    result = textByLines.size();
                }
            }
        }
        return result;
    }

    public void addCellWithMultipleTextLines(PDPageContentStream contentStream, String text,
                                TextAlign textAlign, Color fillingColor, Color fontColor, Outline outline,
                                float initX, float initY, float cellWidth, int quantityOfLines, boolean onlyVerticalCellBoards) throws IOException {

        //create linked list of all words in text
        LinkedList<String> textByLines = new LinkedList<>();
        //if text is small enough, add only one line
        if (text.length() <= configuration.getMaxCharactersInTextLine()) {
            textByLines.add(text);
        } else {
            String[] strings = text.split(" ");
            textByLines.addAll(Arrays.asList(strings));
            int size = textByLines.size();

            int i = 0;
            while (i < size) {
                //divide word if it is too long
                int max = configuration.getMaxCharactersInTextLine();
                if (textByLines.get(i).length() > max) {
                    String string = textByLines.get(i);
                    textByLines.remove(i);
                    size--;
                    int numberOfParts = (int)Math.ceil((double)string.length() / (double)max);
                    for (int j = 0; j < numberOfParts-1; j++) {
                        String part = string.substring(j*max, (j+1)*max);
                        textByLines.add(i+j, part);
                        size++;
                    }
                    //add last part
                    String part = string.substring(max*(numberOfParts-1));
                    textByLines.add(i+numberOfParts-1, part);
                    size++;
                }
                i++;
            }

            //wrap words
            int k = 0;
            while (k < size-1) {
                if ((textByLines.get(k).length() + 1 + textByLines.get(k + 1).length()) <= configuration.getMaxCharactersInTextLine()) {
                    String newString = textByLines.get(k) + " " + textByLines.get(k + 1);
                    textByLines.set(k, newString);
                    textByLines.remove(k + 1);
                    size--;
                    k--;
                }
                k++;
            }
        }

        //set color and draw stroke rectangle
        if (outline == Outline.OUTLINED) {
            contentStream.setStrokingColor(configuration.getStrokingColor());
            contentStream.setLineWidth(configuration.getLineWidth());
            if (onlyVerticalCellBoards) {
                //draw first vertical line
                contentStream.moveTo(initX, initY);
                contentStream.lineTo(initX, initY-(cellHeight * quantityOfLines));
                //draw second vertical line
                contentStream.moveTo(initX+cellWidth, initY);
                contentStream.lineTo(initX+cellWidth, initY-(cellHeight * quantityOfLines));
            } else {
                contentStream.addRect(initX, initY, cellWidth, -(cellHeight * quantityOfLines));
            }
            contentStream.stroke();
        }

        //set color and draw filling rectangle
        contentStream.setNonStrokingColor(fillingColor);
        contentStream.addRect(initX, initY, cellWidth, -(cellHeight * quantityOfLines));
        contentStream.fill();

        //set color for text
        contentStream.setNonStrokingColor(fontColor);

        //define starting position of text
        float textInitY = (float) (initY - cellHeight - fontDescent + (cellHeight * 0.1));
        float textInitX = 0;
//        float textLength = text.length() * fontAverageWidth;
//        //change textLength if it is more than max
//        if (text.length() > configuration.getMaxCharactersInTextLine()) {
//            textLength = configuration.getMaxCharactersInTextLine() * fontAverageWidth;
//        }

        //add text
//        contentStream.beginText();
        contentStream.setFont(configuration.getFont(), fontSize);
        contentStream.setLeading(cellHeight);
//        contentStream.newLineAtOffset(textInitX, textInitY);
        for (String string: textByLines) {
            //calculate string length in points
            float stringWidth = configuration.getFont().getStringWidth(string) / 1000 * fontSize;

            if (textAlign == TextAlign.LEFT) {
                textInitX = initX + fontAverageWidth;
            }
            if (textAlign == TextAlign.CENTER) {
                textInitX = initX + cellWidth/2 - stringWidth/2;
            }
            if (textAlign == TextAlign.RIGHT) {
                textInitX = initX + cellWidth - stringWidth - fontAverageWidth;
            }

            contentStream.beginText();
            contentStream.newLineAtOffset(textInitX, textInitY);
            contentStream.setFont(configuration.getFont(), fontSize);
            contentStream.showText(string);
            contentStream.endText();
            textInitY -= cellHeight;
        }
    }
 }
