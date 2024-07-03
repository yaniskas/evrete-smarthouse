package com.example;

import com.example.MsgTypes.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.lang.reflect.Type;

public final class Utils {

    private static final Gson GSON = new GsonBuilder().setDateFormat("MMM dd, yyyy, hh:mm:ss a").setPrettyPrinting().create();

    public static String toJson(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Object to be serialized cannot be null");
        }
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("type", obj.getClass().getSimpleName());
        jsonObj.add("data", GSON.toJsonTree(obj));
        return GSON.toJson(jsonObj);
    }

    public static <T> T fromJsonToMessageType(String json, Type typeOfT) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string to be deserialized cannot be null or empty");
        }
        if (typeOfT == null) {
            throw new IllegalArgumentException("Type of T cannot be null");
        }
        JsonObject jsonObj = GSON.fromJson(json, JsonObject.class);
        return GSON.fromJson(jsonObj.get("data"), typeOfT);
    }

    private static Action parseMessage(String json) {
        JsonObject jsonObj = GSON.fromJson(json, JsonObject.class);
        String type = GSON.fromJson(jsonObj.get("type"), String.class);
        switch (type) {
            case "Motion":
                return Action.Motion.fromJson(json);
            case "Contact":
                return Action.Contact.fromJson(json);
            case "AmbientLight":
                return Action.AmbientLight.fromJson(json);
            case "Light": 
                return Action.Light.fromJson(json);
            case "Consumption":
                return Action.Consumption.fromJson(json);
            case "HeatingF":
                return Action.HeatingF.fromJson(json);
            case "DoorBell":
                return Action.DoorBell.fromJson(json);
            case "ShutOff":
                return Action.ShutOff.fromJson(json);
            default:
                throw new IllegalArgumentException("Unknown message type: " + type);
        }
    }

    public static BenchmarkData parseJsonToMessagesRow(String json) {
        JsonObject jsonObj = GSON.fromJson(json, JsonObject.class);
        JsonArray messages = jsonObj.getAsJsonArray("messages");
        List<Action> messagesList = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            JsonObject message = messages.get(i).getAsJsonObject();
            Action msg = Utils.parseMessage(message.toString());
            messagesList.add(msg);
        }

        return new BenchmarkData(
                jsonObj.get("number_of_messages").getAsInt(),
                jsonObj.get("number_of_random_messages").getAsInt(),
                messagesList);
    }
    
    public static List<BenchmarkData> parseMessagesRows(JsonArray jsonArray) {
        List<BenchmarkData> messagesRows = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            String messagesRowString = jsonArray.get(i).toString();
            messagesRows.add(Utils.parseJsonToMessagesRow(messagesRowString));
        }

        return messagesRows;
    }

    public static String readFileAsJsonStr(String path) {
        File file = new File(path);
        String line = null;
        String ls = System.getProperty("line.separator");
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }

            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public static JsonArray parseJson(String json) {
        return GSON.fromJson(json, JsonArray.class);
    }
}


