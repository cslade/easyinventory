package com.kinvo.easyinventory;

public enum Tier {
    DEMO, BASIC, PREMIUM;

    public static Tier fromString(String s) {
        if (s == null) return BASIC;
        switch (s.trim().toLowerCase()) {
            case "demo": return DEMO;
            case "basic": return BASIC;
            case "premium": return PREMIUM;
            default: return BASIC;
        }
    }
}


