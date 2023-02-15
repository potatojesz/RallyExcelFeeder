package com.clear2pay.model;

import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class RallyItem {
    private String ID;
    private JsonElement fields;
    private String type;

    public RallyItem(String id) {
        setID(id);
    }
    public RallyItem(String id, JsonElement fields, String type) {
        setID(id);
        setFields(fields);
        setType(type);
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

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}
