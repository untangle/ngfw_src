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

package com.untangle.node.smtp.mime;

import static com.untangle.node.util.ASCIIUtil.isEOL;
import static com.untangle.node.util.Ascii.COLON;
import static com.untangle.node.util.Ascii.SP;

import java.io.IOException;

/**
 * Class representing an entry in a set of Headers.  A HeaderField
 * models the text format of headers.  That is, if an RFC 822 message
 * contains two "Received:" headers, there are two "HeaderField"
 * Objects.  This is not to be confused with other MIME libraries
 * which fold all like-named headers into a single Object.
 */
public class HeaderField {

    private final LCString m_nameLC;
    private final String m_name;
    private HeaderFieldObserver m_observer;
    private RawHeaderField m_rawFieldData;
    private String m_valueAsString;
    private boolean m_changed;


    public HeaderField(String name,
                       LCString lCName) {
        m_nameLC = lCName;
        m_name = name;
    }


    public HeaderField(String name) {
        this(name, new LCString(name));
    }


    /**
     * Null is a valid argument, indicating "there is
     * no longer a valid parent for this HeaderField".
     */
    public final void setObserver(HeaderFieldObserver observer) {
        m_observer = observer;
    }
    public HeaderFieldObserver getObserver() {
        return m_observer;
    }

    protected RawHeaderField getRawHeaderField() {
        return m_rawFieldData;
    }


    /**
     * One of two ways to cause a Header to assign its
     * content from raw data (the other being
     * {@link #assignFromString assignFromString}).
     * <br>
     * The ordering here is subtle, so pay attention if
     * you are subclassing.
     */
    public final void assignFromLines(RawHeaderField rawField,
                                      boolean markAsChanged)
        throws HeaderParseException {

        RawHeaderField oldRaw = m_rawFieldData;
        String oldValueAsString = m_valueAsString;
        m_rawFieldData = rawField;
        m_valueAsString = null;

        try {
            parseLines();
            if(markAsChanged) {
                changed();
            }
        }
        catch(HeaderParseException ex) {
            m_rawFieldData = oldRaw;
            m_valueAsString = oldValueAsString;
            throw new HeaderParseException(ex);
        }

    }

    protected final void assignFromString(String valueAsString,
                                          boolean markAsChanged)
        throws HeaderParseException {

        RawHeaderField oldRaw = m_rawFieldData;
        String oldValueAsString = m_valueAsString;
        m_rawFieldData = null;
        m_valueAsString = valueAsString;

        try {
            parseStringValue();
            if(markAsChanged) {
                changed();
            }
        }
        catch(HeaderParseException ex) {
            m_rawFieldData = oldRaw;
            m_valueAsString = oldValueAsString;
            throw new HeaderParseException(ex);
        }


    }


    protected void parseLines()
        throws HeaderParseException {
        //Does nothing
    }


    protected void parseStringValue()
        throws HeaderParseException {
        //Does nothing
    }





    public LCString getNameLC() {
        return m_nameLC;
    }
    public String getName() {
        return m_name;
    }
    public String getValueAsString() {
        if(m_valueAsString == null && m_rawFieldData != null) {
            m_valueAsString = Line.linesToString(m_rawFieldData.lines,
                                                 m_rawFieldData.valueStartOffset,
                                                 true);
        }
        return m_valueAsString;
    }



    protected void changed() {
        if(m_observer != null) {
            m_observer.headerFieldChanged(this);
        }
        m_rawFieldData = null;
        m_changed = true;
    }
    protected void clearChanged() {

    }
    protected boolean hasChanged() {
        return m_changed;
    }

    /**
     * This method (unless overidden)
     * first attempts to write-out any preserved
     * original lines.  If these are not present (either
     * because this Object was constructed from
     * method calls or the Object was modified)
     * then the results of {@link #getName getName}
     * and {@link #getValueAsString getValueAsString()}
     * are used.
     */
    public final void writeTo(MIMEOutputStream out)
        throws IOException {
        if(!hasChanged() && m_rawFieldData != null) {
            out.write(m_rawFieldData.lines);
        }
        else {
            writeToAssemble(out);
        }
    }

    /**
     * Intended to be overidden.  This method is called
     * if either the header {@link #hasChanged has changed}
     * or there are no original source Lines.  Implementations
     * of this method should write-out the Header from
     * the relevant attributes maintained by the
     * subclass.
     * <br>
     * The default implementation simply
     * calles the convienence {@link #writeHeader writeHeader()}
     * method.
     */
    public void writeToAssemble(MIMEOutputStream out)
        throws IOException {
        writeHeader(out, getName(), getValueAsString());
    }

    //TODO: bscott Fold lines
    protected static void writeHeader(MIMEOutputStream out,
                                      String headerName,
                                      String headerValue)
        throws IOException {

        out.write(headerName);
        out.write((byte)COLON);
        out.write((byte)SP);

        byte[] bytes = headerValue.getBytes();
        int len = bytes.length;
        //Check if there is already one or two EOL chars
        // at the end of this
        if(isEOL(bytes[len-1])) {
            len--;
        }
        if(isEOL(bytes[len-1])) {
            len--;
        }
        out.write(bytes, 0, len);
        out.writeLine();
    }

    /**
     * Really only for debugging, not to produce output suitable
     * for output.
     */
    public String toString() {
        return getName() + ": " + getValueAsString();
    }

}
