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

package com.untangle.tran.mime;

import java.util.*;
import static com.untangle.tran.mime.HeaderNames.*;

//TODO: bscott This class should be renamed "MIMEMessageHeadersFieldFactory".

/**
 * Subclass of MIMEPartHeaderFieldFactory which adds strong typing
 * for the following headers:
 * <ul>
 * <li><b>TO</b> via the {@link com.untangle.tran.mime.EmailAddressHeaderField class}</li>
 * <li><b>CC</b> via the {@link com.untangle.tran.mime.EmailAddressHeaderField class}</li>
 * </ul>
 *
 */
public class MailMessageHeaderFieldFactory
    extends MIMEPartHeaderFieldFactory {

    @Override
    protected HeaderField createHeaderField(String mixedCaseName) {


        LCString lcString = new LCString(mixedCaseName);

        if(lcString.equals(TO_LC) ||
           lcString.equals(CC_LC) ||
           lcString.equals(FROM_LC)) {
            return new EmailAddressHeaderField(mixedCaseName,
                                               lcString);
        }

        return super.createHeaderField(mixedCaseName);
    }

    @Override
    protected Headers createHeaders(MIMESource source,
                                    int sourceStart,
                                    int sourceLen,
                                    List<HeaderField> headersInOrder,
                                    Map<LCString, List<HeaderField>> headersByName) {

        return new MIMEMessageHeaders(this,
                                      source,
                                      sourceStart,
                                      sourceLen,
                                      headersInOrder,
                                      headersByName);

    }

}
