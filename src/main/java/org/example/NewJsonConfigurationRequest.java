package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.*;
import java.util.ArrayList;


@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class NewJsonConfigurationRequest {
    
    @JsonProperty("HeaderAtEveryPage") 
    public boolean headerAtEveryPage;
    public int maxCharactersInTextLine;
    @JsonProperty("MaxColumnsAllowed") 
    public int maxColumnsAllowed;
    public int linewidth;
    public String leftMargin;
    public String rightMargin;
    public String topMargin;
    public String bottomMargin;
    public ArrayList<WhatColumnsToHide> whatColumnsToHide;
    public ArrayList<Field> fields;
    public ArrayList<ColumnsToGroupBy> columnsToGroupBy;
    @JsonProperty("ColumnsToAggregate") 
    public ArrayList<ColumnsToAggregate> columnsToAggregate;
    public ArrayList<PageHeader> pageHeader;
    @JsonProperty("PageFooter") 
    public PageFooter pageFooter;

    public static class ColumnsToAggregate{
        public String field;
        public String aggregate;
    }
    
    public static class ColumnsToGroupBy{
        public String field;
        public String type;
    }
    
    public static class Datum{
        public String field;
        public String value;
    }
    
    public static class Field{
        public String field;
        public String type;
        public String textFormat;
        public String textAlignment;
        public String textColor;
        @JsonProperty("NegativeValueColor") 
        public String negativeValueColor;
        @JsonProperty("NegativeAsParentheses") 
        public boolean negativeAsParentheses;
    }
    
    public static class PageFooter{
        @JsonProperty("Font Size") 
        public String fontSize;
        @JsonProperty("Text Color") 
        public String textColor;
        @JsonProperty("BackGroundColor") 
        public String backGroundColor;
        @JsonProperty("PageNumberFlag") 
        public boolean pageNumberFlag;
        @JsonProperty("Data") 
        public ArrayList<Datum> data;
    }
    
    public static class PageHeader{
        @JsonProperty("Font Size") 
        public String fontSize;
        @JsonProperty("Text Color") 
        public String textColor;
        @JsonProperty("BackGroundColor") 
        public String backGroundColor;
        @JsonProperty("Data") 
        public ArrayList<Datum> data;
    }
    
    public static class WhatColumnsToHide{
        public String field;
        public String type;
    }
    
}


