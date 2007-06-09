/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
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

    static Hashtable emptyHashtable = new Hashtable();

    private FormUtil() {}

    public static Hashtable parseQueryString(String s) {

        String valArray[] = null;

        if (s == null) {
            throw new IllegalArgumentException();
        }
        Hashtable ht = new Hashtable();
        StringBuffer sb = new StringBuffer();
        StringTokenizer st = new StringTokenizer(s, "&");
        while (st.hasMoreTokens()) {
            String pair = (String)st.nextToken();
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

    public static String unparseQueryString(Hashtable values)
    {
        if (values == null)
            return "";
        StringBuilder result = new StringBuilder();
        try {
            boolean doneOne = false;
            for (Iterator iter = values.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
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

    public static Hashtable parsePostData(File file)
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
                return new Hashtable(); // cheap hack to return an empty hash

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
