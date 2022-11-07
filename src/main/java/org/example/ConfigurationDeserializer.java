package org.example;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;

import static org.example.ColumnName.*;
import static org.example.ColumnName.Create_Date;

class ConfigurationDeserializer extends StdDeserializer<Configuration> {

    public ConfigurationDeserializer() {
        this(null);
    }

    public ConfigurationDeserializer(Class<?> vc) {
        super(vc);
    }

    Color colorFromString(String string) {
        int r = Integer.parseInt(string.split(" ")[0]);
        int g = Integer.parseInt(string.split(" ")[1]);
        int b = Integer.parseInt(string.split(" ")[2]);
        return new Color(r, g, b);
    }

    PDFont fontFromString(String string) {
        if (string.equalsIgnoreCase("TIMES_ROMAN")) {
            return PDType1Font.TIMES_ROMAN;
        }
        if (string.equalsIgnoreCase("TIMES_BOLD")) {
            return PDType1Font.TIMES_BOLD;
        }
        if (string.equalsIgnoreCase("TIMES_ITALIC")) {
            return PDType1Font.TIMES_ITALIC;
        }
        if (string.equalsIgnoreCase("TIMES_BOLD_ITALIC")) {
            return PDType1Font.TIMES_BOLD_ITALIC;
        }
        if (string.equalsIgnoreCase("HELVETICA")) {
            return PDType1Font.HELVETICA;
        }
        if (string.equalsIgnoreCase("HELVETICA_BOLD")) {
            return PDType1Font.HELVETICA_BOLD;
        }
        if (string.equalsIgnoreCase("HELVETICA_OBLIQUE")) {
            return PDType1Font.HELVETICA_OBLIQUE;
        }
        if (string.equalsIgnoreCase("HELVETICA_BOLD_OBLIQUE")) {
            return PDType1Font.HELVETICA_BOLD_OBLIQUE;
        }
        if (string.equalsIgnoreCase("COURIER")) {
            return PDType1Font.COURIER;
        }
        if (string.equalsIgnoreCase("COURIER_BOLD")) {
            return PDType1Font.COURIER_BOLD;
        }
        if (string.equalsIgnoreCase("COURIER_OBLIQUE")) {
            return PDType1Font.COURIER_OBLIQUE;
        }
        if (string.equalsIgnoreCase("COURIER_BOLD_OBLIQUE")) {
            return PDType1Font.COURIER_BOLD_OBLIQUE;
        }
        if (string.equalsIgnoreCase("SYMBOL")) {
            return PDType1Font.SYMBOL;
        }
        if (string.equalsIgnoreCase("ZAPF_DINGBATS")) {
            return PDType1Font.ZAPF_DINGBATS;
        } else {
            return PDType1Font.TIMES_ROMAN;
        }
    }
    @Override
    public Configuration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String font1 = node.get("font").textValue();
        PDFont font = fontFromString(font1);


        float fontSize = node.get("fontSize").floatValue();



        String fontColor1 = node.get("fontColor").textValue();
        Color fontColor = colorFromString(fontColor1);

        String strokingColor1 = node.get("strokingColor").textValue();
        Color strokingColor = colorFromString(strokingColor1);

        String headFillingColor1 = node.get("headFillingColor").textValue();
        Color headFillingColor = colorFromString(headFillingColor1);

        String subTotalFillingColor1 = node.get("subTotalFillingColor").textValue();
        Color subTotalFillingColor = colorFromString(subTotalFillingColor1);

        String groupFillingColor1 = node.get("groupFillingColor").textValue();
        Color groupFillingColor = colorFromString(groupFillingColor1);

        float lineWidth = node.get("lineWidth").floatValue();
        String reportName = node.get("reportName").textValue();
        float leftMargin = node.get("leftMargin").floatValue();
        float rightMargin = node.get("rightMargin").floatValue();
        float topMargin = node.get("topMargin").floatValue();
        float bottomMargin = node.get("bottomMargin").floatValue();

        HashMap<ColumnName, Boolean> whatColumnsToShow = new HashMap<>();
        whatColumnsToShow.put(Investment_Name, node.get("whatColumnsToShow").get("Investment_Name").booleanValue());
        whatColumnsToShow.put(Transaction_Type_Name, node.get("whatColumnsToShow").get("Transaction_Type_Name").booleanValue());
        whatColumnsToShow.put(Contract_Settlement, node.get("whatColumnsToShow").get("Contract_Settlement").booleanValue());
        whatColumnsToShow.put(Currency_Cd, node.get("whatColumnsToShow").get("Currency_Cd").booleanValue());
        whatColumnsToShow.put(Trade_Dt, node.get("whatColumnsToShow").get("Trade_Dt").booleanValue());
        whatColumnsToShow.put(Unit_Price_Local_Amt, node.get("whatColumnsToShow").get("Unit_Price_Local_Amt").booleanValue());
        whatColumnsToShow.put(Asset_Type_Nm, node.get("whatColumnsToShow").get("Asset_Type_Nm").booleanValue());
        whatColumnsToShow.put(Net_Local_Amt, node.get("whatColumnsToShow").get("Net_Local_Amt").booleanValue());
        whatColumnsToShow.put(Trade_Type, node.get("whatColumnsToShow").get("Trade_Type").booleanValue());
        whatColumnsToShow.put(Transaction_Event, node.get("whatColumnsToShow").get("Transaction_Event").booleanValue());
        whatColumnsToShow.put(Tax_Local_Amt, node.get("whatColumnsToShow").get("Tax_Local_Amt").booleanValue());
        whatColumnsToShow.put(Create_Date, node.get("whatColumnsToShow").get("Create_Date").booleanValue());


        JsonNode columnsToGroupByAsNode = node.get("columnsToGroupBy");
        int size = node.get("columnsToGroupBy").size();
//        ArrayList<ColumnName> list = new ArrayList<>();
        ColumnName[] columnsToGroupBy = new ColumnName[size];
        int i = 0;
        for (JsonNode objNode: columnsToGroupByAsNode) {
            columnsToGroupBy[i] = (ColumnName.fromString(objNode.asText()));
            i++;
        }

        HashMap<ColumnName, TextAlign> textAlignHashMap = new HashMap<>();
        textAlignHashMap.put(Investment_Name, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Investment_Name").textValue()));
        textAlignHashMap.put(Transaction_Type_Name, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Transaction_Type_Name").textValue()));
        textAlignHashMap.put(Contract_Settlement, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Contract_Settlement").textValue()));
        textAlignHashMap.put(Currency_Cd, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Currency_Cd").textValue()));
        textAlignHashMap.put(Trade_Dt, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Trade_Dt").textValue()));
        textAlignHashMap.put(Unit_Price_Local_Amt, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Unit_Price_Local_Amt").textValue()));
        textAlignHashMap.put(Asset_Type_Nm, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Asset_Type_Nm").textValue()));
        textAlignHashMap.put(Net_Local_Amt, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Net_Local_Amt").textValue()));
        textAlignHashMap.put(Trade_Type, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Trade_Type").textValue()));
        textAlignHashMap.put(Transaction_Event, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Transaction_Event").textValue()));
        textAlignHashMap.put(Tax_Local_Amt, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Tax_Local_Amt").textValue()));
        textAlignHashMap.put(Create_Date, TextAlign.fromStringToTextAlign(node.get("textAlignInColumn").get("Create_Date").textValue()));


        Configuration configuration = new Configuration(font, fontSize, fontColor, strokingColor, headFillingColor,
                subTotalFillingColor, groupFillingColor, lineWidth, reportName, leftMargin, rightMargin, topMargin, bottomMargin,
                whatColumnsToShow, columnsToGroupBy, textAlignHashMap);

        return configuration;
    }
}