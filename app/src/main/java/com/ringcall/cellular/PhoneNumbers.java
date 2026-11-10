package com.ringcall.cellular;

import java.util.Set;

/** Helpers for comparing phone numbers written in different formats (050-1234567, +972501234567 ...). */
public final class PhoneNumbers {
    private static final int MATCH_DIGITS = 9;

    private PhoneNumbers() {}

    public static String digitsOnly(String number) {
        if (number == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : number.toCharArray()) {
            if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.toString();
    }

    /** Compares the trailing digits, so local and international formats of the same number match. */
    public static boolean same(String a, String b) {
        String da = digitsOnly(a);
        String db = digitsOnly(b);
        if (da.isEmpty() || db.isEmpty()) return false;
        int n = Math.min(MATCH_DIGITS, Math.min(da.length(), db.length()));
        if (n < 7) return da.equals(db); // short numbers must match exactly
        return da.substring(da.length() - n).equals(db.substring(db.length() - n));
    }

    public static boolean isWhitelisted(String incoming, Set<String> whitelist) {
        for (String w : whitelist) {
            if (same(incoming, w)) return true;
        }
        return false;
    }
}
