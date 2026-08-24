package com.example.travel.domain.gathering.enums;

public enum MyGatheringType {
    HOSTED("hosted"),
    JOINED("joined");

    private final String apiValue;

    MyGatheringType(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static MyGatheringType fromApiValue(String value) {
        for (MyGatheringType type : values()) {
            if (type.apiValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported my gathering type: " + value);
    }
}
