package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
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
    HashMap<String, String> columnNamesForTableHead;
    HashMap<String, String> hashMapOfTypes;

    PDFont ordinaryFont;
//    PDFont ordinaryFontForDescription;
    PDFont boldFont;
//    PDFont boldFontForDescription;

    float footerFontCapHeight;
    float footerFontAscent;
    float footerFontDescent;
    float footerFontLeading;
    float footerCellHeight;



    public Pdf(PDDocument document, Configuration configuration, ArrayList<String> columnNames, 
    HashMap<String, Integer> textLengths, HashMap<String, String> hashMapOfTypes, HashMap<String, String> columnNamesForTableHead,
               File ordinaryFontFile, File boldFontFile) throws IOException {

        this.document = document;
        this.configuration = configuration;
        this.columnNames = columnNames;
        this.textLengths = textLengths;
        this.hashMapOfTypes = hashMapOfTypes;
        this.columnNamesForTableHead = columnNamesForTableHead;

        this.ordinaryFont = PDType0Font.load(document, ordinaryFontFile);
        this.boldFont = PDType0Font.load(document, boldFontFile);

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
        fontSize = tableWidth * 1000 / (rowCharacterLength * getFontDescriptor(ordinaryFont).getAverageWidth());

        fontCapHeight = getFontDescriptor(ordinaryFont).getCapHeight() * fontSize / 1000;
        fontAscent = getFontDescriptor(ordinaryFont).getAscent() * fontSize / 1000;
        fontDescent = getFontDescriptor(ordinaryFont).getDescent() * fontSize / 1000;
        fontLeading = getFontDescriptor(ordinaryFont).getLeading() * fontSize / 1000;
        fontAverageWidth = getFontDescriptor(ordinaryFont).getAverageWidth() * fontSize / 1000;

        //define cell height by font and it's size
        cellHeight = fontCapHeight + fontAscent - fontDescent + fontLeading;


        footerFontCapHeight = getFontDescriptor(ordinaryFont).getCapHeight() * configuration.getPageFooterFontSize() / 1000;
        footerFontAscent = getFontDescriptor(ordinaryFont).getAscent() * configuration.getPageFooterFontSize() / 1000;
        footerFontDescent = getFontDescriptor(ordinaryFont).getDescent() * configuration.getPageFooterFontSize() / 1000;
        footerFontLeading = getFontDescriptor(ordinaryFont).getLeading() * configuration.getPageFooterFontSize() / 1000;
        footerCellHeight = footerFontCapHeight + footerFontAscent - footerFontDescent + footerFontLeading;
    }


    PDFontDescriptor getFontDescriptor(PDFont font) {
        if (font == ordinaryFont) {
            return PDType1Font.HELVETICA.getFontDescriptor();
        }
        if (font == boldFont) {
            return PDType1Font.HELVETICA_BOLD.getFontDescriptor();
        }
        else {
            return PDType1Font.HELVETICA.getFontDescriptor();
        }
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

//        float footerFontCapHeight = getfontDescriptor(ordinaryFont).getCapHeight() * configuration.getPageFooterFontSize() / 1000;
//        float footerFontAscent = getfontDescriptor(ordinaryFont).getAscent() * configuration.getPageFooterFontSize() / 1000;
//        float footerFontDescent = getfontDescriptor(ordinaryFont).getDescent() * configuration.getPageFooterFontSize() / 1000;
//        float footerFontLeading = getfontDescriptor(ordinaryFont).getLeading() * configuration.getPageFooterFontSize() / 1000;
//        float footerCellHeight = footerFontCapHeight + footerFontAscent - footerFontDescent + footerFontLeading;


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
                TextAlign.RIGHT, configuration.getPageFooterBackGroundColor(), configuration.getPageFooterFontColor(), Outline.NOTOUTLINED,
                configuration.getLeftMargin(),
                configuration.getBottomMargin() + configuration.getLinesOfPageFooter().size()*footerCellHeight - (footerCellHeight * (configuration.getLinesOfPageFooter().size()-1))/2,
                tableWidth, configuration.getPageFooterFontSize(), true, ordinaryFont);
            }
            //add text lines
            float tab = 0;
            float footerFontAverageWidth = getFontDescriptor(ordinaryFont).getAverageWidth() * configuration.getPageFooterFontSize() / 1000;
            for (int j=0; j < configuration.getLinesOfPageFooter().size(); j++) {
                String st = configuration.getLinesOfPageFooter().get(j);
                //count the longest line to tabulate page number to prevent overlap
                if (st.length() > tab) {
                    tab = st.length();
                }
                addCellWithText(contentStream, st,
                TextAlign.LEFT, configuration.getPageFooterBackGroundColor(), configuration.getPageFooterFontColor(), Outline.NOTOUTLINED,
                configuration.getLeftMargin(),
                        configuration.getBottomMargin() + configuration.getLinesOfPageFooter().size()*footerCellHeight - (footerCellHeight * j),
                        (tab+1) * footerFontAverageWidth,
                        configuration.getPageFooterFontSize(), true, ordinaryFont);
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
            addCellWithMultipleTextLines(contentStream, text, configuration.getRowHeaderHorizontalAlignment(), configuration.getTableHeadFillingColor(), configuration.getTableHeadFontColor(),
            Outline.OUTLINED, initX, initY, cellWidth, quantityOfLines, fontSize, textLengths.get(string), boldFont);
            // addCellWithMultipleTextLines(contentStream, text,
            //         TextAlign.CENTER, configuration.getHeadFillingColor(),
            //         configuration.getDefaultFontColor(), Outline.OUTLINED, initX, initY, cellWidth, quantityOfLines, configuration.isOnlyVerticalCellBoards());
            initX += cellWidth;
        }
        initX = configuration.getLeftMargin();
        initY -= cellHeight*quantityOfLines;
    }

    private void drawCircle(PDPageContentStream contentStream, float cx, float cy, float r, Color color) throws IOException {
        final float k = 0.552284749831f;
        contentStream.setNonStrokingColor(color);
        contentStream.moveTo(cx - r, cy);
        contentStream.curveTo(cx - r, cy + k * r, cx - k * r, cy + r, cx, cy + r);
        contentStream.curveTo(cx + k * r, cy + r, cx + r, cy + k * r, cx + r, cy);
        contentStream.curveTo(cx + r, cy - k * r, cx + k * r, cy - r, cx, cy - r);
        contentStream.curveTo(cx - k * r, cy - r, cx - r, cy - k * r, cx - r, cy);
        contentStream.fill();
    }
    public void createHeadOfReport() throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(0), PDPageContentStream.AppendMode.APPEND, true);

        //what is the biggest font size, except report name
        float biggestFontSize = 0;
        //"i" starts from 1 as the first one is the report name
        for (int i = 1; i < configuration.getPageHeaderLines().size(); i++) {
            if (Float.parseFloat(configuration.getPageHeaderConfiguration().get(i).get("fontSize")) > biggestFontSize) {
                biggestFontSize = Float.parseFloat(configuration.getPageHeaderConfiguration().get(i).get("fontSize"));
            }
        }
        //count all column lengths
        float[] columnLengths = new float[]{0, 0, 0};
        //"i" starts from 1 as the first one is the report name
        for (int i=1; i < configuration.getPageHeaderLines().size(); i++) {
            for (int j=0; j < 6; j+=2) {
                //as there could be no text in some columns use try
                try {
                    float boldFontStringWidth = boldFont.getStringWidth(configuration.getPageHeaderLines().get(i).get(j) + ": ") / 1000 *
                            Float.parseFloat(configuration.getPageHeaderConfiguration().get(i).get("fontSize"));
                    float ordinaryFontStringWidth = ordinaryFont.getStringWidth(configuration.getPageHeaderLines().get(i).get(j+1)) / 1000 *
                            Float.parseFloat(configuration.getPageHeaderConfiguration().get(i).get("fontSize"));
                    if (columnLengths[j/2] < boldFontStringWidth+ordinaryFontStringWidth) {
                        columnLengths[j/2] = boldFontStringWidth+ordinaryFontStringWidth;
                    }
                } catch (IndexOutOfBoundsException ignored) {
                }
            }
        }
        float allLengths = 0;
        for (int i = 0; i < 3; i++) {
            allLengths += columnLengths[i];
        }


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
            float headerFontCapHeight = getFontDescriptor(ordinaryFont).getCapHeight() * pageHeaderFontSize / 1000;
            float headerFontAscent = getFontDescriptor(ordinaryFont).getAscent() * pageHeaderFontSize / 1000;
            float headerFontDescent = getFontDescriptor(ordinaryFont).getDescent() * pageHeaderFontSize / 1000;
            float headerFontLeading = getFontDescriptor(ordinaryFont).getLeading() * pageHeaderFontSize / 1000;
            float headerCellHeight = headerFontCapHeight + headerFontAscent - headerFontDescent + headerFontLeading;

            //change global cell height and font descent
            float tempCellHeight = cellHeight;
            float tempFontDescent = fontDescent;
            cellHeight = headerCellHeight;
            fontDescent = headerFontDescent;


            //if it is the first line draw it as one cell with green dot at the end
            if (i == 0) {
                //get text from second item of array, because the first one is "Report name"
                String text = configuration.getPageHeaderLines().get(i).get(1);
                TextAlign textAlign = TextAlign.LEFT;
                addCellWithText(contentStream, text,
                        textAlign, pageHeaderBackGroundColor, pageHeaderFontColor, Outline.NOTOUTLINED,
                        initX, initY, tableWidth, pageHeaderFontSize, true, ordinaryFont);

                //set color and draw filling rectangle
                float w = headerFontCapHeight/2;
                float x = initX + ordinaryFont.getStringWidth(text + " ") / 1000 * pageHeaderFontSize;
                float y = initY - cellHeight + w;
                contentStream.setNonStrokingColor(Color.decode("#03af52"));
                //add green dot
                drawCircle(contentStream, x + w/2, y + w/2, w/2, Color.decode("#03af52"));
//                //add green rectangle
//                contentStream.addRect(x,y,w,w);
//                contentStream.fill();

            } else {
                //add additional empty line after Report name
                if (i==1) {
                    initY -= headerCellHeight;
                }

                //draw the line by drawing each part of it's data
                float columnWidth = 0;
                for (int j=0; j<configuration.getPageHeaderLines().get(i).size(); j++) {
                    String text;
                    float cellWidth;

                    if (j%2 == 0) {
                        text = configuration.getPageHeaderLines().get(i).get(j) + ": ";
                        cellWidth = boldFont.getStringWidth(text) / 1000 * pageHeaderFontSize;
                        addCellWithText(contentStream, text,
                                TextAlign.LEFT, pageHeaderBackGroundColor, pageHeaderFontColor, Outline.NOTOUTLINED,
                                initX, initY, cellWidth, pageHeaderFontSize, true, boldFont);
                        initX += cellWidth;
                    } else {
                        text = configuration.getPageHeaderLines().get(i).get(j);
                        cellWidth = ordinaryFont.getStringWidth(text) / 1000 * pageHeaderFontSize;
                        addCellWithText(contentStream, text,
                                TextAlign.LEFT, pageHeaderBackGroundColor, pageHeaderFontColor, Outline.NOTOUTLINED,
                                initX, initY, cellWidth, pageHeaderFontSize, true, ordinaryFont);
//                        initX = configuration.getLeftMargin() + Math.round((float) j /2) * (tableWidth/3);
                        columnWidth += columnLengths[j/2];
                        initX = configuration.getLeftMargin() + columnWidth  * tableWidth / allLengths;
                    }
                }
                initX = configuration.getLeftMargin();

                //add additional empty line after the last line
                if (i == configuration.getPageHeaderConfiguration().size() - 1) {
                    initY -= headerCellHeight;
                }
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
        int howManyLinesInARow = howManyLinesInARow(transaction);

        //create new page if there is no enough space
        if (initY - cellHeight*howManyLinesInARow < configuration.getBottomMargin() + configuration.getLinesOfPageFooter().size()*footerCellHeight) {
            addNewPage();
            contentStream.close();
            contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);
        }


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
                    initX, initY, cellWidth, howManyLinesInARow, fontSize, textLengths.get(string), ordinaryFont);
            initX += cellWidth;
        }

        initX = configuration.getLeftMargin();
        initY -= cellHeight*howManyLinesInARow;

        // if (initY < configuration.getBottomMargin()+cellHeight) {
        //     if (configuration.isOnlyVerticalCellBoards()) {
        //         contentStream.setStrokingColor(configuration.getStrokingColor());
        //         contentStream.setLineWidth(configuration.getLineWidth()/2);
        //         contentStream.moveTo(initX, initY);
        //         contentStream.lineTo(initX+tableWidth, initY);
        //         contentStream.stroke();
        //         }
        //     addNewPage();
        // }
        contentStream.close();
    }

    public void addGroupHead(String columnName, Transaction transaction, int level) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);

        int sumOfAllMaxWidth = 0;
        for (int i: textLengths.values()) {
            sumOfAllMaxWidth += i;
        }


        Color backgroundColor = configuration.getGroupHeadFillingColor().get(columnName);
        Color fontColor = configuration.getGroupHeadFontColor().get(columnName);
//        if (level == 1) {
//            backgroundColor = configuration.getGroupHead1FillingColor();
//            fontColor = configuration.getGroupHead1FontColor();
//        } else {
//            backgroundColor = configuration.getGroupHead2FillingColor();
//            fontColor = configuration.getGroupHead2FontColor();
//        }

//        contentStream.setNonStrokingColor(configuration.getGroupFillingColor());

        String text = columnName + ": " + transaction.getAllValuesAsString(configuration).get(columnName);
        if (level <= 2) {
            addCellWithText(contentStream, text, TextAlign.LEFT, backgroundColor,
                    fontColor, Outline.OUTLINED,
                    initX, initY, tableWidth, fontSize, true, ordinaryFont);
        } else {
            addCellWithTextWithTabulation(contentStream, text, TextAlign.LEFT, backgroundColor,
                    fontColor, Outline.OUTLINED,
                    initX, initY, tableWidth, level-2);
        }



        initX = configuration.getLeftMargin();
        initY -= cellHeight;

        contentStream.close();

        if (initY < configuration.getBottomMargin()+cellHeight) {
            addNewPage();
        }
    }

    public void addSubtotalOrTotalRow(boolean isItTotal, String columnName, Subtotal subtotal, HashMap<String, String> hashMapOfTypes) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);

        int sumOfAllMaxWidth = 0;
        for (int i: textLengths.values()) {
            sumOfAllMaxWidth += i;
        }
        
        //Change the first column of the subtotal to the subtotal line name and set filling color
        Color color;
        HashMap <String, String> tempMap = new HashMap<>();
        String lineName;
        if (isItTotal) {
            lineName = "GrandTotal:";
            color = Color.lightGray;
        } else {
            lineName = "Sub-total: " + columnName;
            color = configuration.getSubTotalFillingColor();
        }
        tempMap.put(columnNames.get(0), lineName);
        subtotal.setTextFields(tempMap);

        
        int howManyLinesInARow = howManyLinesInARow(subtotal);
        //create new page if there is no enough space
        if (initY - cellHeight*howManyLinesInARow < configuration.getBottomMargin() + configuration.getLinesOfPageFooter().size()*footerCellHeight) {
            addNewPage();
            contentStream.close();
            contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);
        }

        float cellWidth;
        //add flag for the first column to add textname of the line
        int f = 0;
        for (String tempColumnName: columnNames) {
            cellWidth = tableWidth * textLengths.get(tempColumnName) / sumOfAllMaxWidth;
            String text;
            String type = hashMapOfTypes.get(tempColumnName);

            Color textColor = configuration.getTextColor().get(tempColumnName);
            if (type.equalsIgnoreCase("number")) {
                double dbl = subtotal.getNumberFields().get(tempColumnName);
                text = DoubleFormatter.format(subtotal.getNumberFields().get(tempColumnName), tempColumnName, configuration); 
                //change color if number is negative
                if (dbl < 0) {
                    textColor = configuration.getNegativeValueColor().get(tempColumnName);
                }
            } else {
                text = "";
            }

            TextAlign textAlign = configuration.getTextAlignment().get(tempColumnName);
            //add the name of the subtotal line
            if (f == 0) {
                textAlign = TextAlign.LEFT;
                if (isItTotal) {
                    text = "GrandTotal:";
                } else {
                    text = "Sub-total: " + columnName;
                }
            }
            f++;

            addCellWithMultipleTextLines(contentStream, text,
                textAlign, color, textColor,
                Outline.OUTLINED, initX, initY, cellWidth, howManyLinesInARow,
                    fontSize, textLengths.get(tempColumnName), boldFont);
           
            initX += cellWidth;


            // if (type.equalsIgnoreCase("number")) {
            //     double dbl = subtotal.getNumberFields().get(tempColumnName);
                
            //     text = DoubleFormatter.format(subtotal.getNumberFields().get(tempColumnName), tempColumnName, configuration); 
                
            //     //change color if number is negative
            //     if (dbl < 0) {
            //         addCellWithMultipleTextLines(contentStream, text,
            //         configuration.getTextAlignment().get(tempColumnName), color, configuration.getNegativeValueColor().get(tempColumnName),
            //         Outline.OUTLINED, initX, initY, cellWidth, howManyLinesInARow, fontSize, configuration.isOnlyVerticalCellBoards());
                    
            //     } else {
            //         addCellWithMultipleTextLines(contentStream, text,
            //         configuration.getTextAlignment().get(tempColumnName), color, configuration.getTextColor().get(tempColumnName),
            //         Outline.OUTLINED, initX, initY, cellWidth, howManyLinesInARow, fontSize, configuration.isOnlyVerticalCellBoards());
            //     }
            //     initX += cellWidth;
            // } else {
            //     initX += cellWidth;
            // }

        }


        // initX = configuration.getLeftMargin();
        //  //  //add text of name of the line
        //  addCellWithMultipleTextLines(contentStream, "Sub-total: " + columnName, TextAlign.LEFT, color, configuration.getDefaultFontColor(), 
        //  Outline.NOTOUTLINED, initX, initY, tableWidth, howManyLinesInARow, fontSize, configuration.isOnlyVerticalCellBoards());
        //  // addCellWithText(contentStream, "Sub-total: " + columnName, TextAlign.LEFT, color, configuration.getDefaultFontColor(),
        //  // Outline.NOTOUTLINED, initX, initY, tableWidth, fontSize);

        contentStream.close();

        initX = configuration.getLeftMargin();
        initY -= (cellHeight * howManyLinesInARow);

        if (initY < configuration.getBottomMargin()+cellHeight) {
            addNewPage();
        }
    }


    // public void addGrandTotalRow(Subtotal subtotal,  HashMap<String, String> hashMapOfTypes) throws IOException {
    //     PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);

    //     int sumOfAllMaxWidth = 0;
    //     for (int i: textLengths.values()) {
    //         sumOfAllMaxWidth += i;
    //     }

    //     Color color = Color.lightGray;

    //     contentStream.setNonStrokingColor(configuration.getGroupFillingColor());
    //     addCellWithText(contentStream, "Grand Total", TextAlign.LEFT, color,
    //             configuration.getDefaultFontColor(), Outline.OUTLINED, initX, initY, tableWidth, fontSize);

    //     float cellWidth;

    //     for (String tempColumnName: columnNames) {
    //         cellWidth = tableWidth * textLengths.get(tempColumnName) / sumOfAllMaxWidth;
    //         String text;

    //         String type = hashMapOfTypes.get(tempColumnName);

    //         if (type.equalsIgnoreCase("number")) {
    //             double dbl = subtotal.getNumberFields().get(tempColumnName);

    //             text = DoubleFormatter.format(subtotal.getNumberFields().get(tempColumnName), tempColumnName, configuration);

    //             //change color if number is negative
    //             if (dbl < 0) {
    //                 addCellWithText(contentStream, text,
    //                 configuration.getTextAlignment().get(tempColumnName), color,
    //                 configuration.getNegativeValueColor().get(tempColumnName), Outline.OUTLINED, initX, initY, cellWidth, fontSize);
    //             } else {
    //                 addCellWithText(contentStream, text,
    //                 configuration.getTextAlignment().get(tempColumnName), color,
    //                 configuration.getTextColor().get(tempColumnName), Outline.OUTLINED, initX, initY, cellWidth, fontSize);
    //             }

    //             initX += cellWidth;
    //         } else {
    //             initX += cellWidth;
    //         }
    //     }

    //     contentStream.close();

    //     initX = configuration.getLeftMargin();
    //     initY -= cellHeight;
    //     if (initY < configuration.getBottomMargin()+cellHeight) {
    //         addNewPage();
    //     }
    // }

    public void addCellWithText(PDPageContentStream contentStream, String text,
                                TextAlign textAlign, Color fillingColor, Color fontColor, Outline outline,
                                float initX, float initY, float cellWidth, float sizeOfFont, boolean fill, PDFont font) throws IOException {

        //set color and draw stroke rectangle
        if (outline == Outline.OUTLINED) {
            contentStream.setStrokingColor(configuration.getStrokingColor());
            contentStream.setLineWidth(configuration.getLineWidth());
            contentStream.addRect(initX, initY, cellWidth, -cellHeight);
            contentStream.stroke();
        }


        if (fill) {
            //set color and draw filling rectangle
            contentStream.setNonStrokingColor(fillingColor);
            contentStream.addRect(initX, initY, cellWidth, -cellHeight);
            contentStream.fill();
        }


        //set color for text
        contentStream.setNonStrokingColor(fontColor);

        //define starting position of text
        float textInitY = (float) (initY - cellHeight - fontDescent + (cellHeight * 0.1));
        float textInitX = 0;

        //calculate string length in points
        float stringWidth = ordinaryFont.getStringWidth(text) / 1000 * sizeOfFont;

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
        contentStream.setFont(font, sizeOfFont);
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
        contentStream.setFont(ordinaryFont, fontSize);
        contentStream.showText(text);
        contentStream.endText();

    }

    public int howManyLinesInARow(Transaction transaction) {

        //return one line by default
        int result = 1;

        for (String key : columnNames){
            String text = transaction.getAllValuesAsString(configuration).get(key);
            int maxCharactersInTextLine = textLengths.get(key);

            //create linked list of all words in text
            LinkedList<String> textByLines = new LinkedList<>();
            //if text is small enough, add only one line
            if (text.length() > maxCharactersInTextLine) {
                String[] strings = text.split(" ");
                textByLines.addAll(Arrays.asList(strings));
                int size = textByLines.size();

                int i = 0;
                while (i < size) {
                    //divide word if it is too long
                    int max = maxCharactersInTextLine;
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
                    if ((textByLines.get(k).length() + 1 + textByLines.get(k + 1).length()) <= maxCharactersInTextLine) {
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

    public int howManyLinesInARow(Subtotal subtotal) {

        //return one line by default
        int result = 1;
        HashMap<String, String> hashMap = new HashMap<>();
        //create hashmap of all values as string
        for (String str: subtotal.getNumberFields().keySet()) {
            double dbl = subtotal.getNumberFields().get(str);
            hashMap.put(str, DoubleFormatter.format(dbl, str, configuration));
        }
        //add the only string value
        String firstColumn = columnNames.get(0);
        hashMap.put(firstColumn, subtotal.getTextFields().get(firstColumn));

        for (String str: hashMap.keySet()) {
            String text = hashMap.get(str);
            int maxCharactersInTextLine = textLengths.get(str);
            
            //create linked list of all words in text
            LinkedList<String> textByLines = new LinkedList<>();
            //if text is small enough, add only one line
            if (text.length() > maxCharactersInTextLine) {
                String[] strings = text.split(" ");
                textByLines.addAll(Arrays.asList(strings));
                int size = textByLines.size();

                int i = 0;
                while (i < size) {
                    //divide word if it is too long
                    int max = maxCharactersInTextLine;
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
                    if ((textByLines.get(k).length() + 1 + textByLines.get(k + 1).length()) <= maxCharactersInTextLine) {
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
                                float initX, float initY, float cellWidth, int quantityOfLines, float fontSize,
                                             int maxCharactersInTextLine, PDFont font) throws IOException {

        //create linked list of all words in text
        LinkedList<String> textByLines = new LinkedList<>();
        //if text is small enough, add only one line
        if (text.length() <= maxCharactersInTextLine) {
            textByLines.add(text);
        } else {
            String[] strings = text.split(" ");
            textByLines.addAll(Arrays.asList(strings));
            int size = textByLines.size();

            int i = 0;
            while (i < size) {
                //divide word if it is too long
                int max = maxCharactersInTextLine;
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
                if ((textByLines.get(k).length() + 1 + textByLines.get(k + 1).length()) <= maxCharactersInTextLine) {
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
            if (!configuration.isShowHorizontalBoarders() && configuration.isShowVerticalBoarders()) {
                //draw first vertical line
                contentStream.moveTo(initX, initY);
                contentStream.lineTo(initX, initY-(cellHeight * quantityOfLines));
                //draw second vertical line
                contentStream.moveTo(initX+cellWidth, initY);
                contentStream.lineTo(initX+cellWidth, initY-(cellHeight * quantityOfLines));
            } if (configuration.isShowHorizontalBoarders() && !configuration.isShowVerticalBoarders()) {
                //draw first horizontal line
                contentStream.moveTo(initX, initY);
                contentStream.lineTo(initX+cellWidth, initY);
                //draw second horizontal line
                contentStream.moveTo(initX, initY-(cellHeight * quantityOfLines));
                contentStream.lineTo(initX+cellWidth, initY-(cellHeight * quantityOfLines));
            } if (configuration.isShowHorizontalBoarders() && configuration.isShowVerticalBoarders()) {
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
//        contentStream.setFont(font, fontSize);
//        contentStream.setLeading(cellHeight);
//        contentStream.newLineAtOffset(textInitX, textInitY);
        for (String string: textByLines) {
            //calculate string length in points
            float stringWidth = font.getStringWidth(string) / 1000 * fontSize;

            if (textAlign == TextAlign.LEFT) {
                textInitX = initX + getFontDescriptor(font).getAverageWidth() / 1000 * fontSize;
            }
            if (textAlign == TextAlign.CENTER) {
                textInitX = initX + cellWidth/2 - stringWidth/2;
            }
            if (textAlign == TextAlign.RIGHT) {
                textInitX = initX + cellWidth - stringWidth - getFontDescriptor(font).getAverageWidth() / 1000 * fontSize;
            }

            contentStream.beginText();
            contentStream.newLineAtOffset(textInitX, textInitY);
            contentStream.setFont(font, fontSize);
            contentStream.showText(string);
            contentStream.endText();
            textInitY -= cellHeight;
        }
    }
 }
