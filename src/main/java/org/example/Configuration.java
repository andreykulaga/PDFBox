package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.example.filtration.Filter;

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
//    Color defaultFontColor = Color.BLACK;

    //configuration of table head and grouping heads
    Color tableHeadFontColor;
    Color tableHeadFillingColor;
    TextAlign rowHeaderVerticalAlignment;
    TextAlign rowHeaderHorizontalAlignment;
    Color subTotalFillingColor = Color.WHITE;


    HashMap<String, Color> groupHeadFontColor = new HashMap<>();
    HashMap<String, Color> groupHeadFillingColor = new HashMap<>();

    //General configuration
    boolean headerAtEveryPage;
    boolean wrapTextInTable;
    int maxCharactersInTextLine;
    public boolean forceFontSize;
    public float fontSize;
    int maxColumnsAllowed;
    float lineWidth;
    float leftMargin;
    float rightMargin;
    float topMargin;
    float bottomMargin;
    Color strokingColor;
    boolean showVerticalBoarders;
    boolean showHorizontalBoarders;
    ArrayList<String> columnsToGroupBy = new ArrayList<>();
    ArrayList<String> whatColumnsToHide = new ArrayList<>();
    ArrayList<String[]> columnsToSortBy = new ArrayList<>();


    //PageFooter configuration
    float pageFooterFontSize;
    Color pageFooterFontColor;
    Color pageFooterBackGroundColor;
    boolean pageNumberFlag;
    ArrayList<String> linesOfPageFooter = new ArrayList<>();


    //PageHeader configuration
    ArrayList<HashMap<String, String>> pageHeaderConfiguration = new ArrayList<>();
    ArrayList<ArrayList<String[]>> pageHeaderLines = new ArrayList<>();

    //Fields configuration
    HashMap<String, String> textFormat = new HashMap<>();
    HashMap<String, String> columnNamesForTableHead = new HashMap<>();
    HashMap<String, TextAlign> textAlignment = new HashMap<>();
    HashMap<String, Color> textColor = new HashMap<>();
    HashMap<String, Color> negativeValueColor = new HashMap<>();
    HashMap<String, Boolean> negativeAsParenthesesHashMap = new HashMap<>();
    HashMap<String, Boolean> isAbsoluteValueHashMap = new HashMap<>();
    HashMap<String, Boolean> isIncludePercentSignHashMap = new HashMap<>();
    HashMap<String, Boolean> isSuppressDuplicateHashMap = new HashMap<>();
    HashMap<String, ArrayList<Filter>> filtersForFields = new HashMap<>();


    String reportName;
    String reportId;
        
    public void importNewJsonConfigurationRequest (NewJsonConfigurationRequest nJCR) {
        headerAtEveryPage = nJCR.headerAtEveryPage;
        wrapTextInTable = nJCR.wrapTextInTable;
        maxCharactersInTextLine = nJCR.maxCharactersInTextLine;
        forceFontSize = nJCR.forceFontSize;

        if (forceFontSize == true) {
            wrapTextInTable = true;
        }

        fontSize = nJCR.fontSize;
        maxColumnsAllowed = nJCR.maxColumnsAllowed;
        lineWidth = nJCR.boarderOption.lineWidth;
        leftMargin = Float.parseFloat(nJCR.pageMargin.get("left"));
        rightMargin = Float.parseFloat(nJCR.pageMargin.get("right"));
        topMargin = Float.parseFloat(nJCR.pageMargin.get("top"));
        bottomMargin = Float.parseFloat(nJCR.pageMargin.get("bottom"));

        showHorizontalBoarders = nJCR.boarderOption.showHorizontalBoarder;
        showVerticalBoarders = nJCR.boarderOption.showVerticalBoarder;


        tableHeadFontColor = Color.decode(nJCR.tableHeadFontColor);
        tableHeadFillingColor = Color.decode(nJCR.tableHeadFillingColor);

        rowHeaderVerticalAlignment = TextAlign.fromStringToTextAlign(nJCR.rowHeaderVerticalAlignment);
        rowHeaderHorizontalAlignment = TextAlign.fromStringToTextAlign(nJCR.rowHeaderHorizontalAlignment);

        strokingColor = Color.decode(nJCR.boarderOption.lineBoarderColor);


//        groupHead1FontColor = Color.decode(nJCR.columnsToGroupBy.get(0).fontColor);
//        groupHead1FillingColor = Color.decode(nJCR.columnsToGroupBy.get(0).backgroundColor);
//        groupHead2FontColor = Color.decode(nJCR.columnsToGroupBy.get(1).fontColor);
//        groupHead2FillingColor = Color.decode(nJCR.columnsToGroupBy.get(1).backgroundColor);

        if (nJCR.columnsToGroupBy.size() > 0) {
            for (int i=0; i<nJCR.columnsToGroupBy.size(); i++) {
                String columnName = nJCR.columnsToGroupBy.get(i).field.toLowerCase().replace("_", " ");
                columnsToGroupBy.add(columnName);
                groupHeadFontColor.put(columnName, Color.decode(nJCR.columnsToGroupBy.get(i).fontColor));
                groupHeadFillingColor.put(columnName, Color.decode(nJCR.columnsToGroupBy.get(i).backgroundColor));
            }
        }

        if (nJCR.whatColumnsToHide.size() > 0) {
            for (int i=0; i<nJCR.whatColumnsToHide.size(); i++) {
                whatColumnsToHide.add(nJCR.whatColumnsToHide.get(i).field.toLowerCase().replace("_", " "));
            }
        }

        if (nJCR.sorting.size() > 0) {
            for (int i=0; i<nJCR.sorting.size(); i++) {

                columnsToSortBy.add(new String[]{nJCR.sorting.get(i).field.toLowerCase().replace("_", " "), nJCR.sorting.get(i).type});
            }
        }

        //PageFooter Configuration
        pageFooterFontSize = nJCR.pageFooter.fontSize;

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
            linesOfPageFooter.add(f);
            String v = nJCR.pageFooter.data.get(i).value;
            linesOfPageFooter.add(v);
//            linesOfPageFooter.add(f.concat(": ").concat(v));
        }


        //PageHeader Configuration
        for (int i=0; i < nJCR.getPageHeader().size(); i++) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("fontSize", String.valueOf(nJCR.getPageHeader().get(i).fontSize));
            hashMap.put("textColor", nJCR.getPageHeader().get(i).textColor);
            hashMap.put("backGroundColor", nJCR.getPageHeader().get(i).backGroundColor);
            pageHeaderConfiguration.add(hashMap);
         
            ArrayList<String[]> arrayList = new ArrayList<>();
            for (int j=0; j < nJCR.getPageHeader().get(i).data.size(); j++) {
                //break if in configuration set more than 3 columns in report headers. We save only the first three.
                if (j == 3) {
                    break;
                }
                String[] strings = new String[2];
                strings[0] = nJCR.getPageHeader().get(i).data.get(j).field;
                strings[1] = nJCR.getPageHeader().get(i).data.get(j).value;
                arrayList.add(strings);

//                arrayList.add(nJCR.getPageHeader().get(i).data.get(j).field);
//                arrayList.add(nJCR.getPageHeader().get(i).data.get(j).value);
            }
            pageHeaderLines.add(arrayList);
            
        }


        //Fields configuration
        for (int i = 0; i < nJCR.getFields().size(); i++) {
            String columnName = nJCR.getFields().get(i).field.toLowerCase().replace("_", " ");

            //if text format is null, then put default for datetime and number
            String format = nJCR.getFields().get(i).textFormat;
            if (format == null) {
                if (nJCR.getFields().get(i).type.equalsIgnoreCase("number")) {
                    format = "#,##0.00";
                } else if (nJCR.getFields().get(i).type.equalsIgnoreCase("Datetime")) {
                    format = "MM/dd/YYYY";
                } else {
                    format = "text";
                }
            }
            textFormat.put(columnName, format);

            columnNamesForTableHead.put(columnName, nJCR.getFields().get(i).displayedName);

            textAlignment.put(columnName, TextAlign.fromStringToTextAlign(nJCR.getFields().get(i).textAlignment));

            if (nJCR.getFields().get(i).negativeNumberOption.get("decorateWith").equalsIgnoreCase("Parenthesis")) {
                negativeAsParenthesesHashMap.put(columnName, true);
            } else {
                negativeAsParenthesesHashMap.put(columnName, false);
            }

            try {
                textColor.put(columnName, Color.decode(nJCR.getFields().get(i).textColor));
            } catch (NumberFormatException e) {
                textColor.put(columnName, Color.BLACK);
            }

            try {
                negativeValueColor.put(columnName, Color.decode(nJCR.getFields().get(i).negativeNumberOption.get("fontColor")));
            } catch (NumberFormatException e) {
                negativeValueColor.put(columnName, Color.BLACK);
            }

            if (nJCR.getFields().get(i).filters != null) {
                filtersForFields.put(columnName, nJCR.getFields().get(i).filters);
            }

            isAbsoluteValueHashMap.put(columnName, nJCR.getFields().get(i).isAbsoluteValue);
            isIncludePercentSignHashMap.put(columnName, nJCR.getFields().get(i).isIncludePercentSign);
            isSuppressDuplicateHashMap.put(columnName, nJCR.getFields().get(i).isSuppressDuplicate);

        }
        
        reportName = nJCR.pageHeader.get(0).data.get(0).value;
        reportId = nJCR.pageFooter.data.get(0).value;
    }
}


