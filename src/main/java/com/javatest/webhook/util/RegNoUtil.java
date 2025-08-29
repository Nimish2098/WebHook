package com.javatest.webhook.util;

public final class RegNoUtil {
    private RegNoUtil() {}

    public static boolean isOddByLastTwoDigits(String regNo) {
        if (regNo == null || regNo.isBlank()) return true; // default odd
        String digits = regNo.replaceAll("\\D", "");
        if (digits.isEmpty()) return true;
        int n = digits.length();
        int lastTwo = Integer.parseInt(digits.substring(Math.max(0, n - 2)));
        return (lastTwo % 2) == 1;
    }
}


