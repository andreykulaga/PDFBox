package org.example;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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

    PDRectangle printSizeFromString(String string) {
        PDRectangle result;
        if (string.equalsIgnoreCase("a0")) {
            result = PDRectangle.A0;
        }
        if (string.equalsIgnoreCase("a1")) {
            result = PDRectangle.A1;
        }
        if (string.equalsIgnoreCase("a2")) {
            result = PDRectangle.A2;
        }
        if (string.equalsIgnoreCase("a3")) {
            result = PDRectangle.A3;
        }
        if (string.equalsIgnoreCase("a4")) {
            result = PDRectangle.A4;
        }
        if (string.equalsIgnoreCase("a5")) {
            result = PDRectangle.A5;
        }
        if (string.equalsIgnoreCase("a6")) {
            result = PDRectangle.A6;
        }
        if (string.equalsIgnoreCase("legal")) {
            result = PDRectangle.LEGAL;
        } else {
            result = PDRectangle.LETTER;
        }
        return result;
    }
    
    @Override
    public Configuration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String font1 = node.get("font").textValue();
        PDFont font = fontFromString(font1);
        boolean preview = node.get("preview").asBoolean(true);
        int numberOfPagesInPreview = node.get("number of pages in preview").asInt(1);
        boolean pdfExport = node.get("pdf export").asBoolean(true);
        String outputName = node.get("output name").asText("result");
        boolean printPageNumber = node.get("print page number").asBoolean(true);
        boolean changeOrientationToLandscape = node.get("orientation").asText("landscape").equalsIgnoreCase("landscape");
        PDRectangle printSize = printSizeFromString(node.get("print size").asText());
        
        boolean headerAtEveryPage = node.get("HeaderAtEveryPage").booleanValue();

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

        int maxCharactersInTextLine = node.get("max characters in text line").intValue();

        JsonNode columnsToHideAsNode = node.get("whatColumnsToHide");
        ArrayList<String> whatColumnsToHide = new ArrayList<>();
        for (JsonNode objNode: columnsToHideAsNode) {
            whatColumnsToHide.add(objNode.asText());
        }

        JsonNode columnsToGroupByAsNode = node.get("columnsToGroupBy");
        ArrayList<String> columnsToGroupBy = new ArrayList<>();
        for (JsonNode objNode: columnsToGroupByAsNode) {
            String tempString = objNode.asText().replaceAll("_", " ").toLowerCase();
            tempString = tempString.substring(0,1).toUpperCase().concat(tempString.substring(1));
            columnsToGroupBy.add(tempString);
        }
        String reportId = node.get("reportId").textValue();

//        JsonNode textAlignAsNode = node.get("textAlignInColumn");
//        HashMap<String, TextAlign> textAlignHashMap = new HashMap<>();
//        for (Iterator<String> it = textAlignAsNode.fieldNames(); it.hasNext(); ) {
//            String string = it.next();
//            String child = textAlignAsNode.get(string).textValue();
//            TextAlign newTextAlign = TextAlign.fromStringToTextAlign(child);
//            textAlignHashMap.put(string, newTextAlign);
//
//        }

        return new Configuration(preview, numberOfPagesInPreview, pdfExport, outputName,
                printPageNumber, printSize, changeOrientationToLandscape,
                headerAtEveryPage, font, fontColor, strokingColor, headFillingColor,
                subTotalFillingColor, groupFillingColor, lineWidth, reportName, leftMargin, rightMargin, topMargin, bottomMargin,
                maxCharactersInTextLine, whatColumnsToHide, columnsToGroupBy, reportId);
    }
}