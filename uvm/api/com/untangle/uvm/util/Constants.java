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
    public static final String COMMA = ",";
    public static final String SLASH = "/";
    public static final String DOUBLE_SLASH = "//";
    public static final String IS_EQUALS_TO = "==";
    public static final String IS_NOT_EQUALS_TO = "!=";
    public static final String LOG_ELLIPSIS = "..";


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

    //encoding Key
    public static final String BASE64 = "Base64";

    // Empty string
    public static final String EMPTY_STRING = "\" \"";

    // Numbers
    public static final String FOUR = "4";
    public static final String SEVEN = "7";

    //clamav daemons
    public static final String CLAMAV_DAEMON = "clamav-daemon";
    public static final String FRESHCLAM = "clamav-freshclam";

    // Operators
    public static final String IS = "is";
    public static final String IS_NOT = "is not";
    public static final String IN = "in";
    public static final String NOT_IN = "not in";

    // Column types
    public static final String NUMERIC = "numeric";
    public static final String INTEGER = "integer";
    public static final String INT = "int";
    public static final String INT2 = "int2";
    public static final String INT4 = "int4";
    public static final String INT8 = "int8";
    public static final String TINYINT = "tinyint";
    public static final String SMALLINT = "smallint";
    public static final String MEDIUMINT = "mediumint";
    public static final String BIGINT = "bigint";
    public static final String REAL = "real";
    public static final String FLOAT = "float";
    public static final String FLOAT4 = "float4";
    public static final String FLOAT8 = "float8";
    public static final String INET = "inet";
    public static final String TIMESTAMP = "timestamp";
    public static final String BOOL = "bool";
    public static final String BOOLEAN = "boolean";
    public static final String BPCHAR = "bpchar";
    public static final String CHARACTER = "character";
    public static final String VARCHAR = "varchar";
    public static final String TEXT = "text";
    public static final String CHAR_PREFIX = "char";

    // Keywords
    public static final String NULL = "null";
    public static final String UNKNOWN = "unknown";
    public static final String NOT_NULL = "not null";
}
