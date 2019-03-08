/**
 * $Id$
 */
package com.untangle.uvm.util;

/**
 * ASCII characters and sequences.
 */
public class Ascii
{
    public static final char CR = '\r';
    public static final char LF = '\n';
    public static final char HT = '\t';
    public static final char COLON = ':';
    public static final char PERIOD = '.';
    public static final char DASH = '-';
    public static final char SP = ' ';
    public static final char HTAB = '\t';
    public static final char EQ = '=';
    public static final char COMMA = ',';
    public static final char QUOTE = '"';
    public static final char BACK_SLASH = '\\';
    public static final char FWD_SLASH = '/';
    public static final char SEMI = ';';
    public static final char OPEN_PAREN = '(';
    public static final char CLOSE_PAREN = ')';
    public static final char GT = '>';
    public static final char LT = '<';
    public static final char AT = '@';
    public static final char OPEN_BRACKET = '[';
    public static final char CLOSE_BRACKET = ']';
    public static final char OPEN_BRACE = '{';
    public static final char CLOSE_BRACE = '}';
    public static final char STAR = '*';
    public static final char PLUS = '+';

    public static final byte CR_B = (byte) CR;
    public static final byte LF_B = (byte) LF;
    public static final byte HT_B = (byte) HT;
    public static final byte COLON_B = (byte) COLON;
    public static final byte PERIOD_B = (byte) PERIOD;
    public static final byte DASH_B = (byte) DASH;
    public static final byte SP_B = (byte) SP;
    public static final byte HTAB_B = (byte) HTAB;
    public static final byte EQ_B = (byte) EQ;
    public static final byte COMMA_B = (byte) COMMA;
    public static final byte QUOTE_B = (byte) QUOTE;
    public static final byte BACK_SLASH_B = (byte) BACK_SLASH;
    public static final byte FWD_SLASH_B = (byte) FWD_SLASH;
    public static final byte SEMI_B = (byte) SEMI;
    public static final byte OPEN_PAREN_B = (byte) OPEN_PAREN;
    public static final byte CLOSE_PAREN_B = (byte) CLOSE_PAREN;
    public static final byte GT_B = (byte) GT;
    public static final byte LT_B = (byte) LT;
    public static final byte AT_B = (byte) AT;
    public static final byte OPEN_BRACKET_B = (byte) OPEN_BRACKET;
    public static final byte CLOSE_BRACKET_B = (byte) CLOSE_BRACKET;
    public static final byte OPEN_BRACE_B = (byte) OPEN_BRACE;
    public static final byte CLOSE_BRACE_B = (byte) CLOSE_BRACE;
    public static final byte STAR_B = (byte) STAR;
    public static final byte PLUS_B = (byte) PLUS;


    /**
     * Same as {@link #PERIOD}
     */
    public static final char DOT = PERIOD;
    public static final byte DOT_B = (byte) PERIOD;
    
    /**
     * I always get confused re: "fwd" or "back" slash
     */
    public static final char MICROSOFT_SLASH = BACK_SLASH;
    public static final byte MICROSOFT_SLASH_B = (byte) BACK_SLASH;
    /**
     * I always get confused re: "fwd" or "back" slash
     */
    public static final char UNIX_SLASH = FWD_SLASH;
    public static final byte UNIX_SLASH_B = (byte) FWD_SLASH;

    public static final String CRLF = "\r\n";
    
    public static final char[] CRLF_CA = new char[] {CR, LF};
    
    public static final byte[] CRLF_BA = new byte[] {CR_B, LF_B};
}
