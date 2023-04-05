package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;


@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class Configuration {

    //Configuration not in JSON, from previous iteration of program
    boolean preview = true;
    int numberOfPagesInPreview = 1;
    boolean pdfExport = true;
    String outputName = "result";
    PDRectangle printSize = PDRectangle.LETTER;
    boolean changeOrientationToLandscape = true;
//    PDFont font = PDType1Font.TIMES_ROMAN;
//    PDFont boldFont = PDType1Font.TIMES_BOLD;
    Color strokingColor = Color.GRAY;
//    boolean onlyVerticalCellBoards = false;
    Color defaultFontColor = Color.BLACK;
    Color subTotalFillingColor = Color.WHITE;

    //configuration of table head and grouping heads
    Color tableHeadFontColor;
    Color tableHeadFillingColor;
    Color groupHead1FontColor;
    Color groupHead1FillingColor;
    Color groupHead2FontColor;
    Color groupHead2FillingColor;
//    Color tableHeadFontColor = Color.WHITE;
//    Color tableHeadFillingColor = Color.BLACK;
//    Color groupHead1FontColor = Color.white;
//    Color groupHead1FillingColor = Color.decode("#313094");
//    Color groupHead2FontColor = Color.white;
//    Color groupHead2FillingColor = Color.decode("#8a81be");

    

    //General configuration
    boolean headerAtEveryPage;
    int maxCharactersInTextLine;
    int maxColumnsAllowed;
    float lineWidth;
    float leftMargin;
    float rightMargin;
    float topMargin;
    float bottomMargin;
    boolean showVerticalBoarders;
    boolean showHorizontalBoarders;
    ArrayList<String> columnsToGroupBy = new ArrayList<>();
    ArrayList<String> whatColumnsToHide = new ArrayList<>();
    

    //PageFooter configuration
    float pageFooterFontSize;
    Color pageFooterFontColor;
    Color pageFooterBackGroundColor;
    boolean pageNumberFlag;
    ArrayList<String> linesOfPageFooter = new ArrayList<>();


     //PageHeader configuration
    ArrayList<HashMap<String, String>> pageHeaderConfiguration = new ArrayList<>();
    ArrayList<ArrayList<String>> pageHeaderLines = new ArrayList<>();

    //Fields configuration
    HashMap<String, String> textFormat = new HashMap<>();
    HashMap<String, String> columnNamesForTableHead = new HashMap<>();
    HashMap<String, TextAlign> textAlignment = new HashMap<>();
    HashMap<String, Color> textColor = new HashMap<>();
    HashMap<String, Color> negativeValueColor = new HashMap<>();
    HashMap<String, Boolean> negativeAsParenthesesHashMap = new HashMap<>();


    String reportName;
    String reportId;
        
    public void importNewJsonConfigurationRequest (NewJsonConfigurationRequest nJCR) {
        headerAtEveryPage = nJCR.headerAtEveryPage;
        maxCharactersInTextLine = nJCR.maxCharactersInTextLine;
        maxColumnsAllowed = nJCR.maxColumnsAllowed;
        lineWidth = nJCR.linewidth;
        leftMargin = Float.parseFloat(nJCR.leftMargin);
        rightMargin = Float.parseFloat(nJCR.rightMargin);
        topMargin = Float.parseFloat(nJCR.topMargin);
        bottomMargin = Float.parseFloat(nJCR.bottomMargin);

        showHorizontalBoarders = nJCR.showHorizontalBoarders;
        showVerticalBoarders = nJCR.showVerticalBoarders;


        tableHeadFontColor = Color.decode(nJCR.tableHeadFontColor);
        tableHeadFillingColor = Color.decode(nJCR.tableHeadFillingColor);
        groupHead1FontColor = Color.decode(nJCR.groupHead1FontColor);
        groupHead1FillingColor = Color.decode(nJCR.groupHead1FillingColor);
        groupHead2FontColor = Color.decode(nJCR.groupHead2FontColor);
        groupHead2FillingColor = Color.decode(nJCR.groupHead2FillingColor);



        if (nJCR.columnsToGroupBy.size() > 0) {
            for (int i=0; i<nJCR.columnsToGroupBy.size(); i++) {
                columnsToGroupBy.add(nJCR.columnsToGroupBy.get(i).field.toLowerCase().replace("_", " "));
            }
        }

        if (nJCR.whatColumnsToHide.size() > 0) {
            for (int i=0; i<nJCR.whatColumnsToHide.size(); i++) {
                whatColumnsToHide.add(nJCR.whatColumnsToHide.get(i).field.toLowerCase().replace("_", " "));
            }
        }

        //PageFooter Configuration
        pageFooterFontSize = Float.parseFloat(nJCR.pageFooter.fontSize);

        try {
            pageFooterFontColor = Color.decode(nJCR.pageFooter.textColor);
        } catch (NumberFormatException e) {
            pageFooterFontColor = Color.BLACK;
        }
        try {
            pageFooterBackGroundColor = Color.decode(nJCR.pageFooter.backGroundColor);
        } catch (NumberFormatException e) {
            pageFooterBackGroundColor = Color.WHITE;
        }
        pageNumberFlag = nJCR.pageFooter.pageNumberFlag;

        for (int i=0; i < nJCR.pageFooter.data.size(); i++) {
            String f = nJCR.pageFooter.data.get(i).field;
            String v = nJCR.pageFooter.data.get(i).value;
            linesOfPageFooter.add(f.concat(": ").concat(v));
        }


        //PageHeader Configuration
        for (int i=0; i < nJCR.getPageHeader().size(); i++) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("fontSize", nJCR.getPageHeader().get(i).fontSize);
            hashMap.put("textColor", nJCR.getPageHeader().get(i).textColor);
            hashMap.put("backGroundColor", nJCR.getPageHeader().get(i).backGroundColor);
            pageHeaderConfiguration.add(hashMap);
         
            ArrayList<String> arrayList = new ArrayList<>();
            for (int j=0; j < nJCR.getPageHeader().get(i).data.size(); j++) {
                arrayList.add(nJCR.getPageHeader().get(i).data.get(j).field);
                arrayList.add(nJCR.getPageHeader().get(i).data.get(j).value);
            }
            pageHeaderLines.add(arrayList);
            
        }


        //Fields configuration
        for (int i = 0; i < nJCR.getFields().size(); i++) {
            String columnName = nJCR.getFields().get(i).field.toLowerCase().replace("_", " ");
            textFormat.put(columnName, nJCR.getFields().get(i).textFormat);
            columnNamesForTableHead.put(columnName, nJCR.getFields().get(i).displayedName);

            textAlignment.put(columnName, TextAlign.fromStringToTextAlign(nJCR.getFields().get(i).textAlignment));

            negativeAsParenthesesHashMap.put(columnName, nJCR.getFields().get(i).negativeAsParentheses);

            try {
                textColor.put(columnName, Color.decode(nJCR.getFields().get(i).textColor));
            } catch (NumberFormatException e) {
                textColor.put(columnName, Color.BLACK);
            }

            try {
                negativeValueColor.put(columnName, Color.decode(nJCR.getFields().get(i).negativeValueColor));
            } catch (NumberFormatException e) {
                negativeValueColor.put(columnName, Color.BLACK);
            }
            
        }
        
        reportName = nJCR.pageHeader.get(0).data.get(0).value;
        reportId = nJCR.pageFooter.data.get(0).value;
    }
}


