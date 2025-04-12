package com.example.chatterdemo.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtils {
    private static final Gson gson = new Gson();

    public static JsonObject toJsonObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    public static <T> T fromJsonObject(JsonObject jsonObject, Class<T> clazz) {
        return gson.fromJson(jsonObject, clazz);
    }

    public static String toJson(Object object) {
        return gson.toJson(object);
    }
}    