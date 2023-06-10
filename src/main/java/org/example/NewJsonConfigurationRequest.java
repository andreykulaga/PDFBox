package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.filtration.Filter;

import java.util.ArrayList;
import java.util.HashMap;


@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class NewJsonConfigurationRequest {
    
    @JsonProperty("showHeaderOnEveryPage")
    public boolean headerAtEveryPage;
    public boolean wrapTextInTable;
    @JsonProperty("maxCharactersToWrap")
    public int maxCharactersInTextLine;
    public boolean forceFontSize;
    public float fontSize;
    @JsonProperty("maxColumnsAllowed")
    public int maxColumnsAllowed;
    @JsonProperty("columnHeaderFontColor")
    public String tableHeadFontColor;
    @JsonProperty("rowHeaderBackgroundColor")
    public String tableHeadFillingColor;
    public String rowHeaderVerticalAlignment;
    public String rowHeaderHorizontalAlignment;
    public HashMap<String, String> pageMargin;
    public BoarderOption boarderOption;


    public ArrayList<Sorting> sorting;

    public ArrayList<WhatColumnsToHide> whatColumnsToHide;
    public ArrayList<Field> fields;
    public ArrayList<ColumnsToGroupBy> columnsToGroupBy;
    @JsonProperty("ColumnsToAggregate")
    public ArrayList<ColumnsToAggregate> columnsToAggregate;
    @JsonProperty("pageHeaderRows")
    public ArrayList<PageHeader> pageHeader;
    @JsonProperty("pageFooter")
    public PageFooter pageFooter;
    public static class BoarderOption {
        public boolean showVerticalBoarder;
        public  boolean showHorizontalBoarder;
        public float lineWidth;
        public String lineBoarderColor;

    }

    public static class ColumnsToAggregate{
        public String field;
        public String aggregate;
    }
    
    public static class ColumnsToGroupBy{
        public String field;
        public String type;
        public String fontColor;
        public String backgroundColor;
    }

    public static class Sorting {
        public String field;
        public String type;
    }
    
    public static class Datum{
        public String field;
        public String value;
    }
    
    public static class Field{
        @JsonProperty("name")
        public String field;
        @JsonProperty("displayName")
        public String displayedName;
        @JsonProperty("dataType")
        public String type;
        @JsonProperty("dataFormat")
        public String textFormat;
        public String textAlignment;
        @JsonProperty("fontColor")
        public String textColor;
        public HashMap<String, String> negativeNumberOption;
        @JsonProperty("filters")
        public ArrayList<Filter> filters;
        @JsonProperty("isIncludePercentSign")
        public boolean isIncludePercentSign;
        @JsonProperty("isAbsoluteValue")
        public boolean isAbsoluteValue;
        @JsonProperty("isSuppressDuplicate")
        public boolean isSuppressDuplicate;
    }
    
    public static class PageFooter{
        @JsonProperty("fontSize")
        public float fontSize;
        @JsonProperty("fontColor")
        public String textColor;
        @JsonProperty("backgroundColor")
        public String backGroundColor;
        @JsonProperty("showPageNumber")
        public boolean pageNumberFlag;
        @JsonProperty("data")
        public ArrayList<Datum> data;
    }
    
    public static class PageHeader{
        @JsonProperty("fontSize")
        public float fontSize;
        @JsonProperty("fontColor")
        public String textColor;
        @JsonProperty("backgroundColor")
        public String backGroundColor;
        @JsonProperty("data")
        public ArrayList<Datum> data;
    }
    
    public static class WhatColumnsToHide{
        public String field;
        public String type;
    }
    
}


