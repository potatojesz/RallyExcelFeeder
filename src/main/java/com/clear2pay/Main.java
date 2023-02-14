package com.clear2pay;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.clear2pay.model.RallyItem;

public class Main {
    public static void main(String[] args) throws IOException {
        RallyUtil rallyUtil = new RallyUtil();
        ExcelUtil excelUtil = new ExcelUtil();

        final Map<String, RallyItem> rallyItems = excelUtil.getRallyItems();
        rallyUtil.feed(rallyItems);
        excelUtil.feed(rallyItems);
    }
}