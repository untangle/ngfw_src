/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.text.MessageFormat;
import java.util.Map;

/**
 * I18n utility functions.
 */
public class I18nUtil
{
    private final Map<String, String> i18nMap;

    /**
     * Constructor
     * 
     * @param i18nMap
     *        The language map
     */
    public I18nUtil(Map<String, String> i18nMap)
    {
        this.i18nMap = i18nMap;
    }

    /**
     * Translate a string=
     * 
     * @param value
     *        The string to translate
     * @return The translated string
     */
    public String tr(String value)
    {
        String tr = i18nMap.get(value);
        if (tr == null) {
            tr = value;
        }
        return tr;
    }

    /**
     * Translate a string and objects
     * 
     * @param value
     *        The string
     * @param objects
     *        The objects
     * @return The translated string
     */
    public String tr(String value, Object[] objects)
    {
        return MessageFormat.format(tr(value, i18nMap), objects);
    }

    /**
     * Translate a string and an object
     * 
     * @param value
     *        The string
     * @param o1
     *        The object
     * @return The translated string
     */
    public String tr(String value, Object o1)
    {
        return tr(value, new Object[] { o1 }, i18nMap);
    }

    /**
     * Translate a string
     * 
     * @param value
     *        The string
     * @param i18nMap
     *        The language map
     * @return The translated string
     */
    public static String tr(String value, Map<String, String> i18nMap)
    {
        String tr = i18nMap.get(value);
        if (tr == null) {
            tr = value;
        }
        return tr;
    }

    /**
     * Translate a string and objects
     * 
     * @param value
     *        The string
     * @param objects
     *        The objects
     * @param i18nMap
     *        The language map
     * @return The translated string
     */
    public static String tr(String value, Object[] objects, Map<String, String> i18nMap)
    {
        return MessageFormat.format(tr(value, i18nMap), objects);
    }

    /**
     * Translate a string and an object
     * 
     * @param value
     *        The string
     * @param o1
     *        The object
     * @param i18nMap
     *        The language map
     * @return The translated string
     */
    public static String tr(String value, Object o1, Map<String, String> i18nMap)
    {
        return tr(value, new Object[] { o1 }, i18nMap);
    }

    /**
     * I have no idea what this function does.
     * 
     * @param value
     *        The value
     * @return The value
     */
    public static String marktr(String value)
    {
        return value;
    }
}
