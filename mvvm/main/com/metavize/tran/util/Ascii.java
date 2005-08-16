/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.util;

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
    public static final char OPEN_PAREN_B = (byte) OPEN_PAREN;
    public static final char CLOSE_PAREN_B = (byte) CLOSE_PAREN;


    /**
     * Same as {@link #PERIOD}
     */
    public static final char DOT = PERIOD;
    public static final char DOT_B = (byte) PERIOD;
    
    /**
     * I always get confused re: "fwd" or "back" slash
     */
    public static final char MICROSOFT_SLASH = BACK_SLASH;
    public static final char MICROSOFT_SLASH_B = (byte) BACK_SLASH;
    /**
     * I always get confused re: "fwd" or "back" slash
     */
    public static final char UNIX_SLASH = FWD_SLASH;
    public static final char UNIX_SLASH_B = (byte) FWD_SLASH;

    public static final String CRLF = "\r\n";
    
    public static final char[] CRLF_CA = new char[] {CR, LF};
    
    public static final byte[] CRLF_BA = new byte[] {CR_B, LF_B};
}
