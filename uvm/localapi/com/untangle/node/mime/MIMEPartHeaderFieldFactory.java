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

import java.util.*;

import static com.untangle.node.mime.HeaderNames.*;

/**
 * Subclass of HeaderFieldFactory which adds strong typing
 * for the following headers:
 * <ul>
 * <li><b>Content-Type</b> via the {@link com.untangle.node.mime.ContentTypeHeaderField class}</li>
 * <li><b>Content-Disposition</b> via the {@link com.untangle.node.mime.ContentDispositionHeaderField class}</li>
 * <li><b>Content-Transfer-Encoding</b> via the {@link com.untangle.node.mime.ContentXFerEncodingHeaderField class}</li>
 * </ul>
 *
 */
public class MIMEPartHeaderFieldFactory
    extends HeaderFieldFactory {

    @Override
    protected HeaderField createHeaderField(String mixedCaseName) {


        LCString lcString = new LCString(mixedCaseName);

        if(lcString.equals(CONTENT_TYPE_LC)) {
            return new ContentTypeHeaderField(mixedCaseName);
        }
        if(lcString.equals(CONTENT_DISPOSITION_LC)) {
            return new ContentDispositionHeaderField(mixedCaseName);
        }
        if(lcString.equals(CONTENT_TRANSFER_ENCODING_LC)) {
            return new ContentXFerEncodingHeaderField(mixedCaseName);
        }

        return super.createHeaderField(mixedCaseName);
    }

    @Override
    protected Headers createHeaders(MIMESource source,
                                    int sourceStart,
                                    int sourceLen,
                                    List<HeaderField> headersInOrder,
                                    Map<LCString, List<HeaderField>> headersByName) {

        return new MIMEPartHeaders(this,
                                   source,
                                   sourceStart,
                                   sourceLen,
                                   headersInOrder,
                                   headersByName);

    }

}
