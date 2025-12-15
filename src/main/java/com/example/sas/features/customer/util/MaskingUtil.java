package com.example.sas.features.customer.util;

public class MaskingUtil {
    public static String maskSsn(String ssn) {
        if (ssn == null) return null;
        String digits = ssn.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "***";
        String last4 = digits.substring(digits.length() - 4);
        return "XXX-XX-" + last4;
    }
}

