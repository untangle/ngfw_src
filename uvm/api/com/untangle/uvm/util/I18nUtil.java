/**
 * $Id$
 */
package com.untangle.uvm.util;

import java.text.MessageFormat;
import java.util.Map;

/**
 * I18n utility functions.
 *
 */
public class I18nUtil
{
    private final Map<String, String> i18nMap;

    public I18nUtil(Map<String, String> i18nMap)
    {
        this.i18nMap = i18nMap;
    }

    // public functions --------------------------------------------------------

    public String tr(String value)
    {
        String tr = i18nMap.get(value);
        if (tr == null) {
            tr = value;
        }
        return tr;
    }

    public String tr(String value, Object[] objects)
    {
        return MessageFormat.format( tr(value,i18nMap), objects);
    }

    public String tr(String value, Object o1)
    {
        return tr(value, new Object[]{ o1 }, i18nMap);
    }

    // static functions --------------------------------------------------------

    public static String tr(String value, Map<String, String> i18nMap)
    {
        String tr = i18nMap.get(value);
        if (tr == null) {
            tr = value;
        }
        return tr;
    }

    public static String tr(String value, Object[] objects, Map<String, String> i18nMap)
    {
        return MessageFormat.format( tr(value,i18nMap), objects);
    }

    public static String tr(String value, Object o1, Map<String, String> i18nMap)
    {
        return tr(value, new Object[]{ o1 }, i18nMap);
    }
    
    public static String marktr(String value)
    {
        return value;
    }    
}
