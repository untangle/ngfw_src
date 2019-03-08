/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

/**
 * Utilities for escaping URIs.
 */
public class UriUtil
{
    private static Logger logger = Logger.getLogger(UriUtil.class);

    /**
     * Escape a URI
     * @param uri The URI to escape
     * @return The escaped URI
     */
    public static String escapeUri(String uri)
    {
        StringBuilder sb = new StringBuilder(uri.length() + 32);

        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);

            switch (c) {
                // unwise
            case '{': sb.append("%7B"); break;
            case '}': sb.append("%7D"); break;
            case '|': sb.append("%7C"); break;
            case '\\': sb.append("%5C"); break;
            case '^': sb.append("%5E"); break;
            case '[': sb.append("%5B"); break;
            case ']': sb.append("%5D"); break;
            case '`': sb.append("%60"); break;
                // delimiter (except #)
            case '<': sb.append("%3C"); break;
            case '>': sb.append("%3E"); break;
            case '"': sb.append("%22"); break;
            case '%':
                if (3 > uri.length() - i
                    || !isHex((byte)uri.charAt(i + 1))
                    || !isHex((byte)uri.charAt(i + 2))) {
                    sb.append("%25");
                } else {
                    sb.append('%');
                }
                break;
            default:
                if (0x7F < c) {
                    String in;
                    if (Character.isHighSurrogate(c)) {
                        in = new String(new char[] { c, uri.charAt(++i) });
                    } else {
                        in = new String(new char[] { c });
                    }
                    byte[] utf8;
                    try {
                        utf8 = in.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException exn) {
                        logger.warn("could not encode UTF-8", exn);
                        utf8 = in.getBytes();
                    }
                    for (int j = 0; j < utf8.length; j++) {
                        sb.append('%');
                        String hexStr = Integer.toHexString(utf8[j] & 0x00FF);
                        if (2 > hexStr.length()) {
                            sb.append("0");
                        }
                        sb.append(hexStr.toUpperCase());
                    }
                } else if (Character.isISOControl(c)) {
                    sb.append('%');
                    String hexStr = Integer.toHexString(c);
                    if (2 > hexStr.length()) {
                        sb.append("0");
                    }
                    sb.append(hexStr.toUpperCase());
                } else {
                    sb.append(c);
                }
                break;
            }
        }

        return sb.toString();
    }

    /**
     * See if a byte is hex
     * @param b The byte to check
     * @return True if hex, otherwise false
     */
    private static boolean isHex(byte b)
    {
        switch (b) {
        case '0': case '1': case '2': case '3': case '4': case '5':
        case '6': case '7': case '8': case '9': case 'a': case 'b':
        case 'c': case 'd': case 'e': case 'f': case 'A': case 'B':
        case 'C': case 'D': case 'E': case 'F':
            return true;
        default:
            return false;
        }
    }
}
