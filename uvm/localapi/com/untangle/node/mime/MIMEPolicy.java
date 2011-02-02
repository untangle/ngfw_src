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

package com.untangle.node.mime;

/**
 * Class to enacpaulate policies regarding how non-conforming
 * MIME should be handled.
 *<p>
 * <i>This is still a work in progress (wrs 6/05)</i>
 */
public class MIMEPolicy {

    //XXXXXXX I'm totally guessing.  Need to research these.

    private int m_maxHeaderLineLen = 8192;
    private int m_maxBodyLineLengthForMultipart = 8192;
    private boolean m_lwsLineTerminatesHeaders = true;
    private boolean m_ignoreFoldedFirstLine = false;
    private int m_maxHeaderLines = 1024;
    private NonHeaderLineInHeadersPolicy m_nonHeaderLineInHeadersPolicy =
        MIMEPolicy.NonHeaderLineInHeadersPolicy.TREAT_AS_BODY;
    private BadMultipartPartPolicy m_badMultipartPartPolicy =
        MIMEPolicy.BadMultipartPartPolicy.TREAT_AS_TEXT_AND_CONVERT_TYPE;


    public enum NonHeaderLineInHeadersPolicy {
        TREAT_AS_BODY,
        IGNORE,
        RAISE_EXCEPTION
    };

    public enum BadMultipartPartPolicy {
        TREAT_AS_TEXT,
        TREAT_AS_TEXT_AND_CONVERT_TYPE,
        RAISE_EXCEPTION
    };

    /**
     * Default global MIMEPolicy.  <b>Warning - be kind.  This
     * is a shared reference.  Don't modify the global instance</b>
     */
    public static final MIMEPolicy DEF_POLICY = new MIMEPolicy();

    public MIMEPolicy() {
    }

    /**
     * Get the max line length permitted when scanning for
     * boundaries.  This does not include content, just the preamble, boundary, etc
     */
    public int getMaxBodyLineLengthForMultipart() {
        return m_maxBodyLineLengthForMultipart;
    }

    /**
     * Policy to determine parse behavior if a "multipart/*"
     * section is encountered without a boundary property.
     */
    public BadMultipartPartPolicy getBadMultipartPartPolicy() {
        return m_badMultipartPartPolicy;
    }


    public int getMaxHeaderLineLen() {
        return m_maxHeaderLineLen;
    }

    /**
     * If true, a line with LWS characters terminates
     * a set of HeaderFields.  If false, it is considered
     * to be a folded member of the previously encountered
     * header.
     */
    public boolean isLwsLineTerminatesHeaders() {
        return m_lwsLineTerminatesHeaders;
    }

    /**
     * Policy regarding if the first line within a Header set
     * is folded.  If this is set to true, then the leading LWS is ignored
     * and this is considered the first header.  If this is false, the headers
     * are considered blank and the line is treated as part of the body.
     */
    public boolean isIgnoreFoldedFirstLine() {
        return m_ignoreFoldedFirstLine;
    }

    /**
     * To avoid some obscure attack, this defines the maximum header
     * lines to be read before an attack is assumed.
     */
    public int getMaxHeaderLines() {
        return m_maxHeaderLines;
    }

    /**
     * How a parser should handle a non Header line found
     * within the headers.  Note that a LWS line is not handled
     * as part of this policy (it has its own).
     */
    public NonHeaderLineInHeadersPolicy getNonHeaderLineInHeadersPolicy() {
        return m_nonHeaderLineInHeadersPolicy;
    }
}
