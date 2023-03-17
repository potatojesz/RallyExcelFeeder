package com.clear2pay;

import com.clear2pay.model.RallyItem;
import com.google.gson.JsonElement;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.*;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ExcelUtil {
    String excelFilePath;

    public ExcelUtil() {
        try (InputStream input = Files.newInputStream(Paths.get("src/main/resources/settings.properties"))) {
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            excelFilePath = prop.getProperty("excel.file.path");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Map<String, RallyItem> getRallyItems() throws IOException {
        Map<String, RallyItem> rallyItems = new LinkedHashMap<>();

        try(FileInputStream file = new FileInputStream(excelFilePath)) {
            try(Workbook workbook = new XSSFWorkbook(file)) {
                Sheet sheet = workbook.getSheetAt(0);
                int i = 0;
                for(Row row : sheet) {
                    if(i == 0) {
                        i++;
                        continue;
                    }
                    String id = row.getCell(0).getStringCellValue().toUpperCase();
                    rallyItems.put(id, new RallyItem(id));
                }

                return rallyItems;
            }
        }
    }

    public void feed(Map<String, RallyItem> items) throws IOException {
        FileInputStream file = new FileInputStream(excelFilePath);

        Map<Integer, String> headers = new HashMap<>();
        try(Workbook workbook = new XSSFWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            int i = 0;
            for (Row row : sheet) {
                String id = null;
                for(Cell cell : row) {
                    //first row
                    if(i == 0) {
                        String value = cell.getStringCellValue();
                        if(StringUtil.isNotBlank(value)) {
                            headers.put(cell.getColumnIndex(), value);
                        }
                    }
                }
                if(i == 0){
                    i++;
                } else {
                    Cell firstCell = row.getCell(0);
                    id = firstCell.getStringCellValue().toUpperCase();
                    if(items.containsKey(id) && StringUtil.isNotBlank(id)) {
                        createHyperlink(items.get(id), workbook, firstCell);
                        for(Map.Entry<Integer, String> header : headers.entrySet()) {
                            if(header.getKey() != 0) {
                                Cell cell = row.getCell(header.getKey(), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                if(header.getValue().equals("JSON")) {
                                    setCellValue(cell, items.get(id).getFields().toString());
                                } else {
                                    JsonElement jsonElement = items.get(id).getFields().getAsJsonObject().get(header.getValue());
                                    if (jsonElement != null) {
                                        String value = jsonElement.toString();
                                        if (jsonElement.isJsonObject()) {
                                            try {
                                                JsonElement refObjectName = jsonElement.getAsJsonObject().get("_refObjectName");
                                                if (refObjectName != null) {
                                                    value = refObjectName.toString();
                                                } else {
                                                    JsonElement tagsNameArray = jsonElement.getAsJsonObject().get("_tagsNameArray");
                                                    if (tagsNameArray != null) {
                                                        value = extractTagsNames(tagsNameArray);
                                                    } else {
                                                        JsonElement count = jsonElement.getAsJsonObject().get("Count");
                                                        if (count != null) {
                                                            value = count.toString();
                                                        }
                                                    }
                                                }
                                            } catch (Exception ex) {
                                                System.out.println("_refObjectName " + ex.getClass().toString() + ": " + ex.getLocalizedMessage());
                                            }
                                        }
                                        if (StringUtil.isNotBlank(value)) {
                                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                                value = value.substring(1, value.length() - 1);
                                            }
                                            setCellValue(cell, value);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            file.close();
            try(FileOutputStream outputStream = new FileOutputStream(excelFilePath)) {
                workbook.write(outputStream);
            }
        }
    }

    private void createHyperlink(RallyItem rallyItem, Workbook workbook, Cell cell) {
        XSSFWorkbook myWorkbook = (XSSFWorkbook)workbook;
        CreationHelper helper
                = myWorkbook.getCreationHelper();
        XSSFCellStyle linkStyle
                = myWorkbook.createCellStyle();
        XSSFFont linkFont = myWorkbook.createFont();

        // Setting the Link Style
        linkFont.setUnderline(XSSFFont.U_SINGLE);
        linkFont.setColor(Font.COLOR_RED);
        linkStyle.setFont(linkFont);

        XSSFHyperlink link
                = (XSSFHyperlink)helper.createHyperlink(HyperlinkType.URL);

        link.setAddress(getAddress(rallyItem));

        cell.setHyperlink(link);
        cell.setCellStyle(linkStyle);
    }

    private String getAddress(RallyItem rallyItem) {
        return "https://rally1.rallydev.com/#/?detail=/" + rallyItem.getType() + "/" + getObjectID(rallyItem) + "&fdp=true";
    }

    private String getObjectID(RallyItem rallyItem) {
       return rallyItem.getFields().getAsJsonObject().get("ObjectID").getAsString();
    }

    private String extractTagsNames(JsonElement tagsNameArray) {
        String result = "";
        for(JsonElement tagName : tagsNameArray.getAsJsonArray()) {
            if(tagName != null && tagName.isJsonObject()) {
                JsonElement name = tagName.getAsJsonObject().get("Name");
                if(name != null) {
                    String nameStr = name.toString();
                    if(nameStr.startsWith("\"") && nameStr.endsWith("\"")) {
                        nameStr = nameStr.substring(1, nameStr.length() - 1);
                    }
                    result = StringUtil.isNotBlank(result) ? result + "," + nameStr : nameStr;
                }
            }
        }
        return result;
    }

    private static void setCellValue(Cell cell, String value) {
        try {
            cell.setCellValue(value);
        } catch(Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
    }
}
