/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

/**
 * String utility class
 */
public class StringUtil
{
    private static final StringUtil INSTANCE = new StringUtil();

    /* Done this way to add magic like yes, no, etc */
    private final String TRUTH_CONSTANTS[] = { "true" };
    private final String FALSE_CONSTANTS[] = { "false" };

    private static final Pattern HUMAN_READABLE_PATTERN = Pattern.compile("([-+]?[0-9]*\\.?[0-9]+)\\s*(.)");
    private static final Map<String,Long> humanReadableMap;
    static {
        humanReadableMap = new HashMap<>();
        humanReadableMap.put("P", 1125899906842624L);
        humanReadableMap.put("T", 1099511627776L);
        humanReadableMap.put("G", 1073741824L);
        humanReadableMap.put("M", 1048576L);
        humanReadableMap.put("K", 1024L);
    }

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

    /**
     * Convert a human readable value like 1k to 1024.
     *
     * @param value
     *        String value to convert.
     * @return long of numeric value.
     */
    static public long humanReadabletoLong(String value){
        double bytes = 0;
        Matcher matcher = HUMAN_READABLE_PATTERN.matcher(value);
        if(matcher.find()){
            String q = matcher.group(2).toUpperCase();
            if(humanReadableMap.get(q) != null){
                try{
                    bytes = Double.parseDouble(matcher.group(1)) * (double) humanReadableMap.get(q);
                }catch(Exception e){}
            }
        }
        return Math.round(bytes);
    }
}
