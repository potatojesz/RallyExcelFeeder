package com.clear2pay;

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

import com.google.gson.JsonElement;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.clear2pay.model.RallyItem;

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
                    id = row.getCell(0).getStringCellValue().toUpperCase();
                    if(items.containsKey(id)) {
                        for(Map.Entry<Integer, String> header : headers.entrySet()) {
                            if(header.getKey() != 0) {
                                Cell cell = row.getCell(header.getKey(), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                JsonElement jsonElement = items.get(id).getFields().getAsJsonObject().get(header.getValue());
                                String value = jsonElement.toString();
                                if(jsonElement.isJsonObject()) {
                                    try {
                                        value = jsonElement.getAsJsonObject().get("_refObjectName").toString();
                                    } catch(Exception ex) {
                                        System.out.println("There is no _refObjectName");
                                    }
                                }
                                if(StringUtil.isNotBlank(value)) {
                                    cell.setCellValue(value);
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
}
