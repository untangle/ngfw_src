/**
 * $Id$
 */

package com.untangle.uvm.util;

/**
 * String utility class
 */
public class StringUtil
{
    private static final StringUtil INSTANCE = new StringUtil();

    /* Done this way to add magic like yes, no, etc */
    private final String TRUTH_CONSTANTS[] = { "true" };
    private final String FALSE_CONSTANTS[] = { "false" };

    /**
     * Constructor
     */
    private StringUtil()
    {
    }

    /**
     * Parse an integer from a string
     * 
     * @param value
     *        The string to parse
     * @param defaultValue
     *        The default value
     * @return The parsed value, or the default on parsing failure
     */
    public int parseInt(String value, int defaultValue)
    {
        if (null == value) return defaultValue;

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parse a boolean from a string
     * 
     * @param value
     *        The string to parse
     * @param defaultValue
     *        The default value
     * @return THe parsed value, or the default on parsing failure
     */
    public boolean parseBoolean(String value, boolean defaultValue)
    {
        if (null == value) return defaultValue;

        value = value.trim();
        for (String truth : TRUTH_CONSTANTS)
            if (truth.equals(value)) return true;
        for (String falseness : FALSE_CONSTANTS)
            if (falseness.equals(value)) return false;

        return defaultValue;
    }

    /**
     * Return the singleton instance
     * 
     * @return The instance
     */
    public static StringUtil getInstance()
    {
        return INSTANCE;
    }
}
