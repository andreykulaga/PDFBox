package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.*;
import java.util.HashMap;


import static org.example.ColumnName.Investment_Name;
import static org.example.ColumnName.Asset_Type_Nm;


@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class Configuration {
    PDFont font = PDType1Font.TIMES_ROMAN;
    float fontSize = 10;
    Color fontColor = Color.BLACK;
    Color strokingColor = Color.DARK_GRAY;
    Color headFillingColor = Color.GRAY;
    Color subTotalFillingColor = Color.ORANGE;
    Color groupFillingColor = Color.CYAN;
    float lineWidth = 1;
    String reportName = "Investment PDF Report";
    float leftMargin = 50;
    float rightMargin = 50;
    float topMargin = 50;
    float bottomMargin = 50;

    HashMap<ColumnName, Boolean> whatColumnsToShow;
    ColumnName[] columnsToGroupBy = {Investment_Name, Asset_Type_Nm};

    HashMap<ColumnName, TextAlign> textAlignInColumn;

}
