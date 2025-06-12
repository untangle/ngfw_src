/**
 * $Id$
 */

package com.untangle.uvm.util;

/**
 * Constants
 */
public class Constants {

    /**
     * Private constructor to hide implicit constructor of utility class (RSPEC-1118)
     */
    private Constants() {
        // no-op
    }


    // Symbols
    public static final String DOT = ".";
    public static final String HYPHEN = "-";
    public static final String UNDERSCORE = "_";
    public static final String SPACE = " ";
    public static final String COMMA_STRING = ",";

    public static final String EQUALS_TO = "=";
    public static final String SLASH = "/";
    public static final String DOUBLE_SLASH = "//";

    // Boolean
    public static final String FALSE = "False";
    public static final String TRUE = "True";

    // Condition Keys
    public static final String CLASS = "class";
    public static final String COMPONENT = "component";
    public static final String ACTION = "action";
    public static final String S_SERVER_PORT = "SServerPort";
    public static final String BLOCKED = "blocked";
    public static final String CATEGORY = "category";

    // Class Regex as condition matcher
    public static final String CRITICAL_ALERT_EVENT_RGX = "*CriticalAlertEvent*";
    public static final String SYSTEM_STAT_EVENT_RGX = "*SystemStatEvent*";
    public static final String SESSION_EVENT_RGX = "*SessionEvent*";
    public static final String WEB_FILTER_EVENT_RGX = "*WebFilterEvent*";
    public static final String APP_CONTROL_LOG_EVENT_RGX = "*ApplicationControlLogEvent*";

    // Escape sequence
    public static final String NEW_LINE = "\n";

    // Empty string
    public static final String EMPTY_STRING = "\" \"";
}
