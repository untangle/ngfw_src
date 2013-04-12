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

import static com.untangle.node.util.ASCIIUtil.eatWhitespace;
import static com.untangle.node.util.ASCIIUtil.readString;
import static com.untangle.node.util.Ascii.COLON;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Class which creates HeaderFields from raw data (or creates new ones).  This
 * class has been created to let subclasses define further parsing for headers
 * known to be interesting.  The default implementation simply returns
 * the basic HeaderField class.
 */
public class HeaderFieldFactory
{

    /**
     * Create a new HeaderField based on the name.  SUbclasses
     * should override to provide more typed implementations.
     *
     * @param mixedCaseName the name of the header
     *
     * @return a new HeaderField with the given name
     */
    protected HeaderField createHeaderField(String mixedCaseName) {
        return new HeaderField(mixedCaseName,
                               new LCString(mixedCaseName));
    }


    /**
     * Create a new Headers, with the given contents
     * and source.  Subclasses may wish to override to
     * provide more typed implementation.
     *
     * @param source the MIMESource from-which the headers
     *        were read (assumed to be shared).
     * @param sourceStart the start of the headers
     *        within the source
     * @param sourceLen the length within source
     *        of the Header bytes
     * @param headersInOrder the HeaderFields as
     *        found (order preserved).
     * @param headersByName a map of HeaderFields
     *        by name
     *
     * @return a new Headers (or subclass).
     */
    protected Headers createHeaders(MIMESource source,
                                    int sourceStart,
                                    int sourceLen,
                                    List<HeaderField> headersInOrder,
                                    Map<LCString, List<HeaderField>> headersByName) {

        return new Headers(this,
                           source,
                           sourceStart,
                           sourceLen,
                           headersInOrder,
                           headersByName);

    }


    /**
     * Helper method.  Reads a HeaderFieldName from the Buffer.
     * <p>
     * Returns null if a header name (key) cannot be found, meaning there
     * was nothing before the ":", or there was no ":".  If not found, the Buffer
     * is reset.  Otherwise, the Buffer is advanced past the colon (":") and any
     * LWS
     */
    public static String readHeaderFieldName(ByteBuffer buf) {

        buf.mark();
        String headerFieldName = readString(buf,
                                            (byte) COLON,
                                            false);

        if(headerFieldName == null) {
            buf.reset();
            return null;
        }
        eatWhitespace(buf, false);

        return headerFieldName;
    }

}
