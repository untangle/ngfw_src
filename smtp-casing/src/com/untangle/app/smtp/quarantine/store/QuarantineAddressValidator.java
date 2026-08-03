/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine.store;

/**
 * Validates email addresses used as quarantine inbox identifiers against
 * filesystem-safety rules. Rejects path traversal sequences, directory
 * separators, and control characters while accepting all legitimate
 * RFC 5321 email address characters.
 */
public final class QuarantineAddressValidator
{
    private static final int MAX_ADDRESS_LENGTH = 254;

    /** Prevent instantiation. */
    private QuarantineAddressValidator() {}

    /**
     * Check whether an address is safe to use as a quarantine inbox name.
     * @param address the email address to validate.
     * @return true if the address is safe, false otherwise.
     */
    public static boolean isValidAddress(String address)
    {
        return getViolation(address) == null;
    }

    /**
     * Return a human-readable reason why the address is unsafe, or null if valid.
     * The returned message never includes the address value itself.
     * @param address the email address to validate.
     * @return violation description, or null if the address is valid.
     */
    public static String getViolation(String address)
    {
        if (address == null || address.isEmpty()) {
            return "address is null or empty";
        }

        if (address.length() > MAX_ADDRESS_LENGTH) {
            return "address exceeds 254 characters";
        }

        if (address.contains("..")) {
            return "address contains path traversal sequence '..'";
        }

        if (address.indexOf('/') >= 0) {
            return "address contains forward slash";
        }

        if (address.indexOf('\\') >= 0) {
            return "address contains backslash";
        }

        for (int i = 0; i < address.length(); i++) {
            char c = address.charAt(i);
            if (c <= 0x1F || c == 0x7F) {
                return "address contains control character at position " + (i + 1);
            }
        }

        return null;
    }
}
