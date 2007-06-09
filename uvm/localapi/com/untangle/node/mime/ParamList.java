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

package com.untangle.node.mime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static com.untangle.node.util.Ascii.*;

/**
 * Class to hold the parameters for "standard"
 * structured field bodies.  I haven't found a strict
 * definition of this "standard", but many structured
 * field bodies (FRC 2822 sec 2.2) seem to follow the
 * <code>Header:val [; key=value]</code> pattern.
 * <br><br>
 * In this capacity, the ParamList acts as a Map.  It also
 * serves to print-out the key/value pairs in a MIME-ready
 * format, folding lines and quoting as nessecary.
 * <br><br>
 * <b>Note that it currently does no encoding/decoding
 * for non-ascii text (RFC 2047), nor does it "obey" the
 * goofy continuations of RFC 2184</b>
 *
 */
public class ParamList {

    private Map<LCString, ParamKVP> m_map =
        new HashMap<LCString, ParamKVP>();

    public ParamList() {
    }

    /**
     * Get all parameter names (keys)
     */
    public Iterator<LCString> keys() {
        return m_map.keySet().iterator();
    }

    /**
     * Set the named parameter.  If null is passed
     * as the value, this is an implicit
     * {@link #remove remove}.
     *
     * @param key the param name
     * @param value the param value
     */
    public void set(String key, String value) {
        LCString lcKey = new LCString(key);
        if(value==null) {
            remove(lcKey);
        }
        else {
            m_map.put(lcKey, new ParamKVP(key, value));
        }
    }

    /**
     * Remove the named parameter.  If the param
     * is not mapped, no error is encountered
     *
     * @param key the name of the parameter to remove
     */
    public void remove(LCString key) {
        m_map.remove(key);
    }

    /**
     * Test if the named parameter is mapped
     *
     * @param key the key
     *
     * @return true if a call to {@link #get get}
     *         will return a non-null value
     */
    public boolean contains(LCString key) {
        return m_map.containsKey(key);
    }

    /**
     * Get the named parameter
     *
     * @param key the key
     *
     * @return the value, or null if
     *         {@link contains not mapped}
     */
    public String get(LCString key) {
        ParamKVP ret = m_map.get(key);
        return ret==null?
            null:ret.val;
    }

    /**
     * Remove all key/value pairs from this list
     */
    public void clear() {
        m_map.clear();
    }

    /**
     * Writes the key/value pairs to the given
     * StringBuilder, quoting and folding as required.
     *
     * @param sb the StringBuilder to append-to
     * @param thisLineLen the current length of the
     *        line (i.e. the header name, colon, and
     *        any primary value).
     *
     * @return the length of the current (last) line
     *         of the StringBuilder
     */
    public int writeOut(StringBuilder sb,
                        int thisLineLen) {

        for(ParamKVP pkvp : m_map.values()) {
            sb.append("; ");
            thisLineLen+=2;
            int valLen = 0;

            String val = MIMEUtil.headerQuoteIfNeeded(pkvp.val);
            if((thisLineLen +
                pkvp.key.length() +
                (val==null?0:val.length())) > 76) {
                sb.append(CRLF);
                sb.append(HTAB);
                thisLineLen = 1;
            }
            sb.append(pkvp.key);
            thisLineLen+=pkvp.key.length();
            if(val != null) {
                sb.append('=');
                thisLineLen++;
                sb.append(val);
            }
            thisLineLen+=val.length();
        }
        return thisLineLen;
    }

    private class ParamKVP {
        final String key;
        final String val;

        ParamKVP(String key, String val) {
            this.key = key;
            this.val = val;
        }
    }
}
