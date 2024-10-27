package com.umlgenerator.entity;
import java.util.Map;

public class ClassInfo {
    private String name;
    private Map<String, String> fields;
    private Map<String, String> methods;
    private Map<String, String> interfaces; // 인터페이스 정보를 저장할 Map 추가

    public ClassInfo(String name, Map<String, String> fields, Map<String, String> methods, Map<String, String> interfaces) {
        this.name = name;
        this.fields = fields;
        this.methods = methods;
        this.interfaces = interfaces;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public Map<String, String> getMethods() {
        return methods;
    }

    public Map<String, String> getInterfaces() {
        return interfaces; // 인터페이스 정보 반환
    }
}
