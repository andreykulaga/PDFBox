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
import java.util.*;

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
    float fontShoulder;
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

    HashMap<String, Float> maxLengthsOfTextInCell;
    ArrayList<String> columnNames;
    HashMap<String, String> columnNamesForTableHead;
    HashMap<String, String> hashMapOfTypes;

    PDFont ordinaryFont;
    PDFont boldFont;

    float footerCellHeight;
    float footerTopBoarder;

    float sumOfAllMaxWidth;



    public Pdf(PDDocument document, Configuration configuration, ArrayList<String> columnNames, 
    HashMap<String, Float> maxLengthsOfTextInCell, HashMap<String, Float> notStringMaxLengths, HashMap<String, String> hashMapOfTypes, HashMap<String, String> columnNamesForTableHead,
               File ordinaryFontFile, File boldFontFile) throws IOException {

        this.document = document;
        this.configuration = configuration;
        this.columnNames = columnNames;
        this.maxLengthsOfTextInCell = maxLengthsOfTextInCell;
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

        //count max length of all columns to decide font size
        float rowLength = 0;
        for (float i : this.maxLengthsOfTextInCell.values()) {
            float doubleSpaceWidth = PDType1Font.HELVETICA_BOLD.getStringWidth("  ") / 1000;
            rowLength += (i + doubleSpaceWidth); //  - additional is one space before and after text
        }

        sumOfAllMaxWidth = 0;
        for (float i: this.maxLengthsOfTextInCell.values()) {
            sumOfAllMaxWidth += i;
        }

        //define font size
        if (!configuration.forceFontSize) {
            fontSize = tableWidth / rowLength;
        } else {
            fontSize = configuration.fontSize;

            //calculate column new column width according to their longest text
            HashMap<String, Float> newMaxLengthsOfTextInCell = new HashMap<>();
            for (String string: hashMapOfTypes.keySet()) {
                float newLength = maxLengthsOfTextInCell.get(string) * tableWidth / sumOfAllMaxWidth;
                newMaxLengthsOfTextInCell.put(string, newLength);
            }

            //clear to use further
            maxLengthsOfTextInCell.clear();
            //for number and date column check if it is enough width to accommodate their text in one row in forced font size
            for (String string: hashMapOfTypes.keySet()) {
                if (hashMapOfTypes.get(string).equalsIgnoreCase("number")
                        || hashMapOfTypes.get(string).equalsIgnoreCase("Datetime")) {
                    float doubleSpaceWidth = boldFont.getStringWidth("   ") * fontSize / 1000;
                    float lengthOfTheLongestRowExceptColumnName = notStringMaxLengths.get(string) * fontSize + doubleSpaceWidth;
                    if (lengthOfTheLongestRowExceptColumnName > newMaxLengthsOfTextInCell.get(string)) {
                        maxLengthsOfTextInCell.put(string, lengthOfTheLongestRowExceptColumnName);
                        newMaxLengthsOfTextInCell.remove(string);
                    }
                }
            }

            //count sum on newLengths (that was changed) and unchanged
            float sumOfChangedLengths = 0;
            float sumOfUnchangedLengths = 0;
            for (float fl: maxLengthsOfTextInCell.values()) {
                sumOfChangedLengths += fl;
            }
            for (float fl: newMaxLengthsOfTextInCell.values()) {
                sumOfUnchangedLengths += fl;
            }
            //for how much we need to decrease unchanged columns
            float difference = sumOfUnchangedLengths + sumOfChangedLengths - tableWidth;

            while (difference > 0) {
                //decrease unchanged columns
                Set<String> set = new HashSet<>(newMaxLengthsOfTextInCell.keySet());
                for (String string: set) {
                    float newLength = newMaxLengthsOfTextInCell.get(string) * (1 - difference/sumOfUnchangedLengths);
                    if (hashMapOfTypes.get(string).equalsIgnoreCase("number")
                            || hashMapOfTypes.get(string).equalsIgnoreCase("Datetime")) {
                        float doubleSpaceWidth = boldFont.getStringWidth("   ") * fontSize / 1000;
                        float lengthOfTheLongestRowExceptColumnName = notStringMaxLengths.get(string) * fontSize + doubleSpaceWidth;
                        if (lengthOfTheLongestRowExceptColumnName > newLength) {
                            maxLengthsOfTextInCell.put(string, lengthOfTheLongestRowExceptColumnName);
                            newMaxLengthsOfTextInCell.remove(string);
                        } else {
                            newMaxLengthsOfTextInCell.put(string, newLength);
                        }
                    } else {
                        newMaxLengthsOfTextInCell.put(string, newLength);
                    }
                }
                //recalculate sum on newLengths (that was changed) and unchanged
                sumOfChangedLengths = 0;
                sumOfUnchangedLengths = 0;
                for (float fl: maxLengthsOfTextInCell.values()) {
                    sumOfChangedLengths += fl;
                }
                for (float fl: newMaxLengthsOfTextInCell.values()) {
                    sumOfUnchangedLengths += fl;
                }
                //recalculate the difference
                difference = sumOfUnchangedLengths + sumOfChangedLengths - tableWidth;
            }

            maxLengthsOfTextInCell.putAll(newMaxLengthsOfTextInCell);

            //recalculate sum of width
            sumOfAllMaxWidth = 0;
            for (float i: this.maxLengthsOfTextInCell.values()) {
                sumOfAllMaxWidth += i;
            }

        }

        fontCapHeight = getFontDescriptor(ordinaryFont).getCapHeight() * fontSize / 1000;
        fontAscent = getFontDescriptor(ordinaryFont).getAscent() * fontSize / 1000;
        fontDescent = getFontDescriptor(ordinaryFont).getDescent() * fontSize / 1000;
        fontLeading = fontCapHeight;
        fontAverageWidth = getFontDescriptor(ordinaryFont).getAverageWidth() * fontSize / 1000;

        if (fontCapHeight > fontAscent) {
            fontShoulder = (fontSize + fontDescent - fontCapHeight)/2;
        } else {
            fontShoulder = (fontSize + fontDescent - fontAscent)/2;
        }

        cellHeight = fontSize + fontLeading;

        footerCellHeight =  configuration.getPageFooterFontSize();


        footerTopBoarder = configuration.getBottomMargin() + ((float) configuration.getLinesOfPageFooter().size() / 2 + 1.5F)*footerCellHeight;
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

        contentStream.close();
    }

    public void addFooters() throws IOException {
        //we should add footers only after all document is ready, because only then
        // we can calculate how many pages in the document to display it in footers

        //change global cell height and fontDescent
        float tempCellHeight = cellHeight;
        cellHeight = footerCellHeight;

        for (int i=0; i< document.getNumberOfPages(); i++) {
            PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(i), PDPageContentStream.AppendMode.APPEND, true);

            //draw a line above the footer
            contentStream.moveTo(configuration.getLeftMargin(), footerTopBoarder - footerCellHeight);
            contentStream.setLineWidth(0.5F);
            contentStream.lineTo(initX+tableWidth, footerTopBoarder - footerCellHeight);
            contentStream.stroke();

            //add page number
            if (configuration.isPageNumberFlag()) {
                
                addCellWithText(contentStream, "Page " + (i+1) + " of " + document.getNumberOfPages(),
                TextAlign.RIGHT, configuration.getPageFooterBackGroundColor(), configuration.getPageFooterFontColor(), Outline.NOTOUTLINED,
                configuration.getLeftMargin(),
                footerTopBoarder - 1.5F*footerCellHeight - (footerCellHeight * ((float) configuration.getLinesOfPageFooter().size()/2 -1))/2,
                tableWidth, configuration.getPageFooterFontSize(), true, boldFont);
            }

            for (int j=0; j < configuration.getLinesOfPageFooter().size(); j+=2) {
                String boldString = configuration.getLinesOfPageFooter().get(j) + ":";
                float lengthOfBoldString = boldFont.getStringWidth(boldString + "  ") * configuration.getPageFooterFontSize() / 1000;

                String ordinaryString = configuration.getLinesOfPageFooter().get(j+1);
                float lengthOfOrdinaryString = ordinaryFont.getStringWidth(ordinaryString + " ") * configuration.getPageFooterFontSize() / 1000;

                float y = footerTopBoarder - 1.5F*footerCellHeight - (footerCellHeight * j/2);

                addCellWithText(contentStream, boldString,
                TextAlign.LEFT, configuration.getPageFooterBackGroundColor(), configuration.getPageFooterFontColor(), Outline.NOTOUTLINED,
                configuration.getLeftMargin(),
                        y, lengthOfBoldString,
                        configuration.getPageFooterFontSize(), true, boldFont);

                addCellWithText(contentStream, ordinaryString,
                TextAlign.RIGHT, configuration.getPageFooterBackGroundColor(), configuration.getPageFooterFontColor(), Outline.NOTOUTLINED,
                        (float) (configuration.getLeftMargin() + (lengthOfBoldString)),
                        y, lengthOfOrdinaryString,
                        configuration.getPageFooterFontSize(), true, ordinaryFont);
            }
            contentStream.close();
        }

        //change global cell height and font descent back
        cellHeight = tempCellHeight;
    }

    public void addTableHeader(PDPageContentStream contentStream) throws IOException {

        int quantityOfLines = 1;
        if (configuration.isWrapTextInTable()) {
            //create fake transaction from column names to count how many lines need for table header
            Transaction transaction = Transaction.createTransactionFromColumnNames(columnNames, columnNamesForTableHead);
            quantityOfLines = howManyLinesInARow(transaction, boldFont);
        }

        float cellWidth;
        for (String string: columnNames){
            cellWidth = tableWidth * maxLengthsOfTextInCell.get(string) / sumOfAllMaxWidth;
            String text = columnNamesForTableHead.get(string);


            addCellWithMultipleTextLines(contentStream, text, configuration.getRowHeaderHorizontalAlignment(),
                    configuration.getTableHeadFillingColor(), configuration.getTableHeadFontColor(),
            Outline.NOTOUTLINED, initX, initY, cellWidth, quantityOfLines, fontSize, boldFont);
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
            float headerFontLeading = headerFontCapHeight;
            float headerCellHeight = pageHeaderFontSize + headerFontLeading;

            float headerFontShoulder;
            if (headerFontCapHeight > headerFontAscent) {
                headerFontShoulder = (pageHeaderFontSize + headerFontDescent - headerFontCapHeight)/2;
            } else {
                headerFontShoulder = (pageHeaderFontSize + headerFontDescent - headerFontAscent)/2;
            }

            //change global cell height, font descent, leading and shoulder
            float tempCellHeight = cellHeight;
            float tempFontDescent = fontDescent;
            float tempFontLeading = fontLeading;
            float tempFontShoulder = fontShoulder;
            cellHeight = headerCellHeight;
            fontDescent = headerFontDescent;
            fontLeading = headerFontLeading;
            fontShoulder = headerFontShoulder;

            //move initY up to compensate empty cell height above Report name.
            initY += pageHeaderFontSize + headerFontDescent + pageHeaderFontSize/2 + headerFontDescent/2 - headerFontAscent/2 - headerFontCapHeight/2;

            //if it is the first line draw it as one cell with green dot at the end
            if (i == 0) {
                //get text from second item of array, because the first one is "Report name"
                String text = configuration.getPageHeaderLines().get(i).get(1);
                TextAlign textAlign = TextAlign.LEFT;
                //For all other cells in report we start text with tabulation of one font average font width
                // (to prevent the first letter from connecting with cell boarder), but for Report name we need it
                // to start as the table boarder -> iniX minus average font width minus width of table boarder

                float spaceWidth = ordinaryFont.getStringWidth(" ") / 1000 * pageHeaderFontSize;
                addCellWithMultipleTextLines(contentStream, text,
                        textAlign, pageHeaderBackGroundColor, pageHeaderFontColor, Outline.NOTOUTLINED,
                        initX - spaceWidth - configuration.getLineWidth(),
                        initY, tableWidth, 1, pageHeaderFontSize, ordinaryFont);
//                addCellWithText(contentStream, text,
//                        textAlign, pageHeaderBackGroundColor, pageHeaderFontColor, Outline.NOTOUTLINED,
//                        initX - fontAverageWidth - configuration.getLineWidth(),
//                        initY, tableWidth, pageHeaderFontSize, true, ordinaryFont);

                //add green dot
                float w = headerFontCapHeight/2;
                float x = initX + ordinaryFont.getStringWidth(text + " ") / 1000 * pageHeaderFontSize;
                float y = initY - pageHeaderFontSize - fontLeading/2 - fontDescent - fontShoulder;
//                float y = initY - cellHeight + w;
                contentStream.setNonStrokingColor(Color.decode("#03af52"));
                drawCircle(contentStream, x + w/2, y + w/2, w/2, Color.decode("#03af52"));
//                //add green rectangle
//                contentStream.addRect(x,y,w,w);
//                contentStream.fill();
                //add additional empty line after Report name
                initY -= headerCellHeight/4;


            } else {

                //draw the line by drawing each part of it's data
                float columnWidth = 0;
                for (int j=0; j<configuration.getPageHeaderLines().get(i).size(); j++) {
                    String text;
                    float cellWidth;

                    if (j%2 == 0) {
                        text = configuration.getPageHeaderLines().get(i).get(j) + ": ";
                        cellWidth = boldFont.getStringWidth(text) / 1000 * pageHeaderFontSize;
                        float spaceWidth = ordinaryFont.getStringWidth(" ") / 1000 * pageHeaderFontSize;
                        addCellWithMultipleTextLines(contentStream, text,
                                TextAlign.LEFT, pageHeaderBackGroundColor, pageHeaderFontColor, Outline.NOTOUTLINED,
                                initX - spaceWidth - configuration.getLineWidth(), initY, cellWidth, 1, pageHeaderFontSize, boldFont);
//                        addCellWithText(contentStream, text,
//                                TextAlign.LEFT, pageHeaderBackGroundColor, pageHeaderFontColor, Outline.NOTOUTLINED,
//                                initX, initY, cellWidth, pageHeaderFontSize, true, boldFont);
                        initX += cellWidth;
                    } else {
                        text = configuration.getPageHeaderLines().get(i).get(j);
                        cellWidth = ordinaryFont.getStringWidth(text) / 1000 * pageHeaderFontSize;
                        addCellWithMultipleTextLines(contentStream, text,
                                TextAlign.LEFT, pageHeaderBackGroundColor, pageHeaderFontColor, Outline.NOTOUTLINED,
                                initX, initY, cellWidth, 1, pageHeaderFontSize, ordinaryFont);
//                        addCellWithText(contentStream, text,
//                                TextAlign.LEFT, pageHeaderBackGroundColor, pageHeaderFontColor, Outline.NOTOUTLINED,
//                                initX, initY, cellWidth, pageHeaderFontSize, true, ordinaryFont);
//                        initX = configuration.getLeftMargin() + Math.round((float) j /2) * (tableWidth/3);
                        columnWidth += columnLengths[j/2];
                        initX = configuration.getLeftMargin() + columnWidth  * tableWidth / allLengths;
                    }
                }
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
            fontLeading = tempFontLeading;
            fontShoulder = tempFontShoulder;

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

        float cellWidth;


        int quantityOfLines = 1;
        if (configuration.isWrapTextInTable()) {
            quantityOfLines = howManyLinesInARow(transaction, ordinaryFont);
        }

        //create new page if there is no enough space
        if (initY - cellHeight*quantityOfLines < footerTopBoarder) {
            addNewPage();
            contentStream.close();
            contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);
        }


        for (String string: columnNames){
            cellWidth = tableWidth * maxLengthsOfTextInCell.get(string) / sumOfAllMaxWidth;
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
                    initX, initY, cellWidth, quantityOfLines, fontSize, ordinaryFont);
            initX += cellWidth;
        }

        initX = configuration.getLeftMargin();
        initY -= cellHeight*quantityOfLines;
        if (configuration.isShowHorizontalBoarders()) {
            initY -= configuration.getLineWidth();
        }

        contentStream.close();
    }

    public void addGroupHead(String columnName, Transaction transaction, int level) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);


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

        if (initY - cellHeight < footerTopBoarder) {
            addNewPage();
        }
    }

    public void addSubtotalOrTotalRow(boolean isItTotal, String columnName, Subtotal subtotal, HashMap<String, String> hashMapOfTypes) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);

        
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

        
        int howManyLinesInARow = howManyLinesInARow(subtotal, boldFont);
        //create new page if there is no enough space
        if (initY - cellHeight*howManyLinesInARow < footerTopBoarder) {
            addNewPage();
            contentStream.close();
            contentStream = new PDPageContentStream(document, document.getPage(document.getNumberOfPages()-1), PDPageContentStream.AppendMode.APPEND, true);
        }

        float cellWidth;
        //add flag for the first column to add textname of the line
        int f = 0;
        for (String tempColumnName: columnNames) {
            cellWidth = tableWidth * maxLengthsOfTextInCell.get(tempColumnName) / sumOfAllMaxWidth;
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
                    fontSize, boldFont);
           
            initX += cellWidth;
        }

        contentStream.close();

        initX = configuration.getLeftMargin();
        initY -= (cellHeight * howManyLinesInARow);
    }

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
        float stringWidth = font.getStringWidth(text) / 1000 * sizeOfFont;
        float spaceWidth = font.getStringWidth(" ") / 1000 * sizeOfFont;

        if (textAlign == TextAlign.LEFT) {
            textInitX = initX + spaceWidth;
        }
        if (textAlign == TextAlign.CENTER) {
            textInitX = initX + cellWidth/2 - stringWidth/2;
        }
        if (textAlign == TextAlign.RIGHT) {
             textInitX = initX + cellWidth - stringWidth - spaceWidth;
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
//        float textInitY = (float) (initY - cellHeight - fontDescent + (cellHeight * 0.1));
        float textInitY = initY - cellHeight;
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

    public int howManyLinesInARow(Transaction transaction, PDFont font) throws IOException {

        //return one line by default
        int result = 1;

        for (String key : columnNames) {
            String text = transaction.getAllValuesAsString(configuration).get(key);
            float cellWidth = tableWidth * maxLengthsOfTextInCell.get(key) / sumOfAllMaxWidth;
            float doubleSpaceWidth = font.getStringWidth("  ") * fontSize / 1000;
            float widthAvailableForText = cellWidth - doubleSpaceWidth;
            //create linked list of all words in text
            LinkedList<String> textByLines = splitTextByLines(text, widthAvailableForText, font);
            if (textByLines.size() > result) {
                result = textByLines.size();
            }
        }
        return result;
    }

    public int howManyLinesInARow(Subtotal subtotal, PDFont font) throws IOException {

        //return one line by default
        int result = 1;

        //create hashmap of all values as string
        HashMap<String, String> hashMap = new HashMap<>();
        for (String columnName: subtotal.getNumberFields().keySet()) {
            double dbl = subtotal.getNumberFields().get(columnName);
            hashMap.put(columnName, DoubleFormatter.format(dbl, columnName, configuration));
        }

        //add the only string value
        String firstColumn = columnNames.get(0);
        hashMap.put(firstColumn, subtotal.getTextFields().get(firstColumn));

        for (String key: hashMap.keySet()) {
            String text = hashMap.get(key);
            float cellWidth = tableWidth * maxLengthsOfTextInCell.get(key) / sumOfAllMaxWidth;
            float widthAvailableForText = cellWidth - font.getStringWidth("  ") * fontSize / 1000;
            //create linked list of all words in text
            LinkedList<String> textByLines = splitTextByLines(text, widthAvailableForText, font);
            if (textByLines.size() > result) {
                result = textByLines.size();
            }
        }
        return result;
    }

    public LinkedList<String> splitTextByLines(String text, float widthAvailableForText, PDFont font) throws IOException {
        //create linked list of all words in text
        LinkedList<String> textByLines = new LinkedList<>();
        //if text is small enough, add only one line
        if (font.getStringWidth(text) * fontSize / 1000<= widthAvailableForText) {
            textByLines.add(text);
        } else {
            String[] strings = text.split(" ");
            textByLines.addAll(Arrays.asList(strings));
            int size = textByLines.size();

            int i = 0;
            while (i < size) {
                //divide word if it is too long
                String string = textByLines.get(i);
                float stringWidth = font.getStringWidth(string) * fontSize / 1000;
                if (stringWidth > widthAvailableForText) {
                    for (int j = string.length() - 1; j >= 0; j--) {
                        if (j == 0) {
                            textByLines = new LinkedList<>();
                            for (int k=0; k < text.length(); k++) {
                                textByLines.add(k, text.substring(k, k+1));
                            }
                            return textByLines;
                        }
                        String tempString = string.substring(j);
                        float tempStringWidth = font.getStringWidth(tempString) * fontSize / 1000;
                        if (stringWidth - tempStringWidth < widthAvailableForText) {
                            //if we found such part of word, then we stop divide it by characters,
                            // add part of word as next in linked list, replace the word at the current place
                            textByLines.add(i + 1, tempString);
                            textByLines.add(i, string.substring(0, j));
                            //remove the string that we work with (it moved to +1 place)
                            textByLines.remove(i+1);
                            size++;
                            j = 0;
                        }
                    }
                }
                i++;
            }

            //wrap words
            int k = 0;
            while (k < size-1) {
                if (font.getStringWidth(textByLines.get(k) + " " + textByLines.get(k + 1)) * fontSize / 1000 <= widthAvailableForText) {
                    String newString = textByLines.get(k) + " " + textByLines.get(k + 1);
                    textByLines.set(k, newString);
                    textByLines.remove(k + 1);
                    size--;
                    k--;
                }
                k++;
            }
        }
        return textByLines;
    }

    public void addCellWithMultipleTextLines(PDPageContentStream contentStream, String text,
                                TextAlign textAlign, Color fillingColor, Color fontColor, Outline outline,
                                float initX, float initY, float cellWidth, int quantityOfLines, float fontSize, PDFont font) throws IOException {

        float doubleSpaceWidth = font.getStringWidth("  ") * fontSize / 1000;
        float widthAvailableForText = cellWidth - doubleSpaceWidth;
        LinkedList<String> textByLines = splitTextByLines(text, widthAvailableForText, font);

        //set color and draw lines
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
                //horizontal line after text
                contentStream.moveTo(initX, initY - configuration.getLineWidth()/2 - cellHeight*quantityOfLines);
                contentStream.lineTo(initX+cellWidth, initY - configuration.getLineWidth()/2 - cellHeight*quantityOfLines);
            } if (configuration.isShowHorizontalBoarders() && configuration.isShowVerticalBoarders()) {
                //draw first vertical line
                contentStream.moveTo(initX, initY);
                contentStream.lineTo(initX, initY-(cellHeight * quantityOfLines));
                //draw second vertical line
                contentStream.moveTo(initX+cellWidth, initY);
                contentStream.lineTo(initX+cellWidth, initY-(cellHeight * quantityOfLines));
                //draw first horizontal line
                contentStream.moveTo(initX - configuration.getLineWidth()/2, initY - configuration.getLineWidth()/2 - cellHeight*quantityOfLines);
                contentStream.lineTo(initX+cellWidth + configuration.getLineWidth()/2, initY - configuration.getLineWidth()/2 - cellHeight*quantityOfLines);
            }
            contentStream.stroke();
        }


        //set color and draw filling rectangle

        float rectangleWidth = cellWidth;
        float moveX = 0;

        if (outline == Outline.NOTOUTLINED) {
            rectangleWidth += 0.1;
        }
        if (configuration.isShowVerticalBoarders()) {
            rectangleWidth -= configuration.getLineWidth();
            moveX = configuration.getLineWidth()/2;
            if (outline == Outline.NOTOUTLINED) {
                rectangleWidth += configuration.getLineWidth();
                moveX = 0;
            }
        }
        contentStream.setNonStrokingColor(fillingColor);
        contentStream.addRect(initX + moveX, initY, rectangleWidth, -cellHeight * quantityOfLines);
        contentStream.fill();

        //set color for text
        contentStream.setNonStrokingColor(fontColor);

        //define starting position of text
        float textInitX = 0;
        float textInitY = initY - fontSize - fontLeading/2 - fontDescent - fontShoulder;
        //change initY for cells if their text has fewer rows than cell, if TextAlign is Center or Bottom.
        //TextAlign Top is default for all
        if (textByLines.size() < quantityOfLines) {
            if (configuration.getRowHeaderVerticalAlignment().equals(TextAlign.CENTER)) {
                textInitY -= cellHeight * (quantityOfLines - textByLines.size())/2;
            }
            if (configuration.getRowHeaderVerticalAlignment().equals(TextAlign.BOTTOM)) {
                textInitY -= cellHeight * (quantityOfLines - textByLines.size());
            }
        }


        for (String string: textByLines) {
            //calculate string length in points
            float stringWidth = font.getStringWidth(string) / 1000 * fontSize;
            float spaceWidth = font.getStringWidth(" ") / 1000 * fontSize;

            if (textAlign == TextAlign.LEFT) {
                textInitX = initX + spaceWidth;
            }
            if (textAlign == TextAlign.CENTER) {
                textInitX = initX + cellWidth/2 - stringWidth/2;
            }
            if (textAlign == TextAlign.RIGHT) {
                textInitX = initX + cellWidth - stringWidth - spaceWidth;
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
