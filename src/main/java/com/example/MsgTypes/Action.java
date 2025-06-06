package com.example.MsgTypes;

import java.util.*;
import com.example.Utils;

public sealed interface Action {
    Action withId(int newId);

    record AmbientLight(int id, int lightLevel, String room, Date timestamp) implements Action {

        public static AmbientLight fromJson(String json) {
            return Utils.fromJsonToMessageType(json, AmbientLight.class);
        }

        public String toJson() {
            return Utils.toJson(this);
        }

        @Override
        public String toString() {
            return String.format("AmbientLight{id=%d, lightLevel=%d, room=%s, timestamp=%s}", id, lightLevel, room,
                    timestamp);
        }

        @Override
        public AmbientLight withId(int newId) {
            return new AmbientLight(newId, lightLevel, room, timestamp);
        }
    }

    record Consumption(int id, int consumption, Date timestamp) implements Action {
        public static Consumption fromJson(String json) {
            return Utils.fromJsonToMessageType(json, Consumption.class);
        }

        public String toJson() {
            return Utils.toJson(this);
        }

        @Override
        public String toString() {
            return String.format("Consumption{id=%d, consumption=%d, timestamp=%s}", id, consumption, timestamp);
        }

        @Override
        public Consumption withId(int newId) {
            return new Consumption(newId, consumption, timestamp);
        }
    }

    record Contact(int id, boolean status, String room, Date timestamp) implements Action {

        public static Contact fromJson(String json) {
            return Utils.fromJsonToMessageType(json, Contact.class);
        }

        public String toJson() {
            return Utils.toJson(this);
        }

        @Override
        public String toString() {
            return String.format("Contact{id=%d, status=%s, room=%s, timestamp=%s}", id, status, room, timestamp);
        }

        @Override
        public Contact withId(int newId) {
            return new Contact(newId, status, room, timestamp);
        }
    }

    record DoorBell(int id, Date timestamp) implements Action {

        public static DoorBell fromJson(String json) {
            return Utils.fromJsonToMessageType(json, DoorBell.class);
        }

        public String toJson() {
            return Utils.toJson(this);
        }

        @Override
        public String toString() {
            return String.format("DoorBell{id=%d, timestamp=%s}", id, timestamp);
        }

        @Override
        public DoorBell withId(int newId) {
            return new DoorBell(newId, timestamp);
        }
    }

    record HeatingF(int id, String tp, Date timestamp) implements Action {
        public static HeatingF fromJson(String json) {
            return Utils.fromJsonToMessageType(json, HeatingF.class);
        }

        public String toJson() {
            return Utils.toJson(this);
        }

        @Override
        public String toString() {
            return String.format("HeatingF{id=%d, tp=%s, timestamp=%s}", id, tp, timestamp);
        }

        @Override
        public HeatingF withId(int newId) {
            return new HeatingF(newId, tp, timestamp);
        }
    }

    record Light(int id, boolean status, String room, Date timestamp) implements Action {

        public static Light fromJson(String json) {
            return Utils.fromJsonToMessageType(json, Light.class);
        }

        public String toJson() {
            return Utils.toJson(this);
        }

        @Override
        public String toString() {
            return String.format("Light{id=%d, status=%s, room='%s', timestamp=%s}", id, status, room, timestamp);
        }

        @Override
        public Light withId(int newId) {
            return new Light(newId, status, room, timestamp);
        }
    }

    record Motion(int id, boolean status, String room, Date timestamp) implements Action {
        public static Motion fromJson(String json) {
            return Utils.fromJsonToMessageType(json, Motion.class);
        }

        public String toJson() {
            return Utils.toJson(this);
        }

        @Override
        public String toString() {
            return String.format("Motion{id=%d, status=%s, room='%s', timestamp=%s}", id, status, room, timestamp);
        }

        @Override
        public Motion withId(int newId) {
            return new Motion(newId, status, room, timestamp);
        }
    }

    record ShutOff() implements Action {

        public static ShutOff fromJson(String json) {
            return Utils.fromJsonToMessageType(json, ShutOff.class);
        }

        public String toJson() {
            return Utils.toJson(this);
        }

        @Override
        public String toString() {
            return "ShutOff{}";
        }

        @Override
        public ShutOff withId(int newId) {
            return new ShutOff();
        }
    }
}