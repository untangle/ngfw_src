/*
 * $HeadURL: svn://chef/work/src/uvm-lib/api/com/untangle/uvm/util/FormUtil.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.util;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

/**
 * Utilities for escaping URIs.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UriUtil
{
    private static Logger logger = Logger.getLogger(UriUtil.class);

    public static String escapeUri(String uri)
    {
        StringBuilder sb = new StringBuilder(uri.length() + 32);

        for (int i = 0; i < uri.length(); i++) {
            char c = (char)uri.charAt(i);

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
