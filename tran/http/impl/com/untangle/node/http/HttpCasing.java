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

package com.untangle.tran.http;

import java.util.LinkedList;
import java.util.List;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.AbstractCasing;
import com.untangle.tran.token.Parser;
import com.untangle.tran.token.Unparser;
import org.apache.log4j.Logger;

class HttpCasing extends AbstractCasing
{
    private final HttpTransformImpl transform;
    private final HttpParser parser;
    private final HttpUnparser unparser;
    private final List<RequestLineToken> requests = new LinkedList<RequestLineToken>();

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    HttpCasing(TCPSession session, boolean clientSide,
               HttpTransformImpl transform)
    {
        this.transform = transform;
        parser = new HttpParser(session, clientSide, this);
        unparser = new HttpUnparser(session, clientSide, this);
    }

    // Casing methods ---------------------------------------------------------

    public Unparser unparser()
    {
        return unparser;
    }

    public Parser parser()
    {
        return parser;
    }

    // package private methods ------------------------------------------------

    HttpTransformImpl getTransform()
    {
        return transform;
    }

    void queueRequest(RequestLineToken request)
    {
        requests.add(request);
    }

    RequestLineToken dequeueRequest(int statusCode)
    {
        if (0 < requests.size()) {
            return requests.remove(0);
        } else {
            if (4 != statusCode / 100) {
                logger.warn("requests is empty: " + statusCode);
            }
            return null;
        }
    }
}
