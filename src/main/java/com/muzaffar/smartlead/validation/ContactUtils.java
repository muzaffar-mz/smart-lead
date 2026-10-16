package com.muzaffar.smartlead.validation;

import org.apache.commons.validator.routines.EmailValidator;

import java.util.regex.Pattern;

public final class ContactUtils {

    private static final Pattern PHONE = Pattern.compile("^\\+?\\d{7,15}$");
    private static final Pattern PHONE_SEPARATORS = Pattern.compile("[\\s\\-().]");

    private ContactUtils() {
    }

    public static boolean isEmail(String contact) {
        return contact != null && EmailValidator.getInstance().isValid(contact.trim());
    }

    public static boolean isPhone(String contact) {
        return contact != null && PHONE.matcher(normalizePhone(contact)).matches();
    }

    /** Strips spaces, dashes, dots and parentheses: "+1 (555) 014-2000" becomes "+15550142000". */
    public static String normalizePhone(String contact) {
        return PHONE_SEPARATORS.matcher(contact.trim()).replaceAll("");
    }
}
