package com.clear2pay.model;

import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class RallyItem {
    private String ID;
    private JsonElement fields;

    public RallyItem(String id) {
        setID(id);
    }
    public RallyItem(String id, JsonElement fields) {
        setID(id);
        setFields(fields);
    }


    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public JsonElement getFields() {
        return fields;
    }

    public void setFields(JsonElement fields) {
        this.fields = fields;
    }
}
