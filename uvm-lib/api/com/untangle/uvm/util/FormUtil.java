/*
 * $HeadURL$
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

package com.untangle.uvm.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

// Helper class for dealing with form data
public class FormUtil {

    static Hashtable<String,String[]> emptyHashtable = new Hashtable<String,String[]>();

    private FormUtil() {}

    @SuppressWarnings("unchecked")
    public static Hashtable<String,String[]> parseQueryString(String s) {

        String valArray[] = null;

        if (s == null) {
            throw new IllegalArgumentException();
        }
        Hashtable<String,String[]> ht = new Hashtable<String,String[]>();
        StringBuffer sb = new StringBuffer();
        StringTokenizer st = new StringTokenizer(s, "&");
        while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            int pos = pair.indexOf('=');
            if (pos == -1) {
                // XXX
                // should give more detail about the illegal argument
                throw new IllegalArgumentException();
            }
            String key = parseName(pair.substring(0, pos), sb);
            String val = parseName(pair.substring(pos+1, pair.length()), sb);
            if (ht.containsKey(key)) {
                String oldVals[] = (String []) ht.get(key);
                valArray = new String[oldVals.length + 1];
                for (int i = 0; i < oldVals.length; i++)
                    valArray[i] = oldVals[i];
                valArray[oldVals.length] = val;
            } else {
                valArray = new String[1];
                valArray[0] = val;
            }
            ht.put(key, valArray);
        }
        return ht;
    }

    public static String unparseQueryString(Hashtable<String,String[]> values)
    {
        if (values == null)
            return "";
        StringBuilder result = new StringBuilder();
        try {
            boolean doneOne = false;
            for (Iterator<String> iter = values.keySet().iterator(); iter.hasNext();) {
                String key = iter.next();
                String encodedKey = URLEncoder.encode(key, "UTF-8");
                String[] vals = (String[]) values.get(key);
                if (vals != null) {
                    for (int i = 0; i < vals.length; i++) {
                        if (doneOne)
                            result.append('&');
                        result.append(encodedKey);
                        result.append('=');
                        result.append(URLEncoder.encode(vals[i], "UTF-8"));
                        doneOne = true;
                    }
                }
            }
        } catch (UnsupportedEncodingException x) {
            // Can't happen.
        }
        return result.toString();
    }

    public static Hashtable<String,String[]> parsePostData(File file)
    {
        try {
            InputStream in = new FileInputStream(file);
            int len = (int) file.length();

            int inputLen, offset;
            byte[] postedBytes = null;
            String postedBody;

            // XXX
            // should a length of 0 be an IllegalArgumentException

            if (len <=0)
                return new Hashtable<String,String[]>(); // cheap hack to return an empty hash

            if (in == null) {
                throw new IllegalArgumentException();
            }

            //
            // Make sure we read the entire POSTed body.
            //
            postedBytes = new byte [len];
            offset = 0;
            do {
                inputLen = in.read (postedBytes, offset, len - offset);
                if (inputLen <= 0) {
                    throw new IOException ("short read from form data file");
                }
                offset += inputLen;
            } while ((len - offset) > 0);
            in.close();

            // XXX we shouldn't assume that the only kind of POST body
            // is FORM data encoded using ASCII or ISO Latin/1 ... or
            // that the body should always be treated as FORM data.

            postedBody = new String(postedBytes, 0, len);

            return parseQueryString(postedBody);
        } catch (IOException e) {
            return emptyHashtable;
        }

    }

    private static String parseName(String s, StringBuffer sb) {
        sb.setLength(0);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '+':
                sb.append(' ');
                break;
            case '%':
                try {
                    sb.append((char) Integer.parseInt(s.substring(i+1, i+3),
                                                      16));
                    i += 2;
                } catch (NumberFormatException e) {
                    // XXX
                    // need to be more specific about illegal arg
                    throw new IllegalArgumentException();
                } catch (StringIndexOutOfBoundsException e) {
                    String rest  = s.substring(i);
                    sb.append(rest);
                    if (rest.length()==2)
                        i++;
                }

                break;
            default:
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }
}
