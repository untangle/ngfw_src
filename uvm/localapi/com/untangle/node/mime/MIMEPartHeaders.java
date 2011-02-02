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

import static com.untangle.node.mime.HeaderNames.CONTENT_DISPOSITION_LC;
import static com.untangle.node.mime.HeaderNames.CONTENT_TRANSFER_ENCODING_LC;
import static com.untangle.node.mime.HeaderNames.CONTENT_TYPE_LC;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * <b>Work in progress</b>
 */
public class MIMEPartHeaders
    extends Headers {

    public MIMEPartHeaders(MIMEPartHeaderFieldFactory factory) {
        super(factory);
    }

    public MIMEPartHeaders() {
        super(new MIMEPartHeaderFieldFactory());
    }

    public MIMEPartHeaders(MIMEPartHeaderFieldFactory factory,
                           MIMESource source,
                           int sourceStart,
                           int sourceLen,
                           List<HeaderField> headersInOrder,
                           Map<LCString, List<HeaderField>> headersByName) {

        super(factory, source, sourceStart, sourceLen, headersInOrder, headersByName);

    }


    public ContentTypeHeaderField getContentTypeHF() {
        List<HeaderField> headers = getHeaderFields(CONTENT_TYPE_LC);
        return (ContentTypeHeaderField) ((headers == null || headers.size() == 0)?
                                         null:
                                         headers.get(0));
    }

    public ContentDispositionHeaderField getContentDispositionHF() {
        List<HeaderField> headers = getHeaderFields(CONTENT_DISPOSITION_LC);
        return (ContentDispositionHeaderField) ((headers == null || headers.size() == 0)?
                                                null:
                                                headers.get(0));
    }

    public ContentXFerEncodingHeaderField getContentXFerEncodingHF() {
        List<HeaderField> headers = getHeaderFields(CONTENT_TRANSFER_ENCODING_LC);
        return (ContentXFerEncodingHeaderField) ((headers == null || headers.size() == 0)?
                                                 null:
                                                 headers.get(0));
    }
    /**
     * Only applies to attachments, but still may be null
     */
    public String getFilename() {
        return getContentDispositionHF()==null?
            null:getContentDispositionHF().getFilename();
    }

    /**
     * Helper method.  Parses the headers from source
     * in one call.
     */
    public static MIMEPartHeaders parseMPHeaders(MIMEParsingInputStream stream,
                                                 MIMESource streamSource)
        throws IOException,
               InvalidHeaderDataException,
               HeaderParseException {
        return parseMPHeaders(stream, streamSource, new MIMEPolicy());
    }

    /**
     * Helper method.  Parses the headers from source
     * in one call.
     */
    public static MIMEPartHeaders parseMPHeaders(MIMEParsingInputStream stream,
                                                 MIMESource streamSource,
                                                 MIMEPolicy policy)
        throws IOException,
               InvalidHeaderDataException,
               HeaderParseException {
        return (MIMEPartHeaders) parseHeaders(stream,
                                              streamSource,
                                              new MIMEPartHeaderFieldFactory(),
                                              policy);
    }


}
