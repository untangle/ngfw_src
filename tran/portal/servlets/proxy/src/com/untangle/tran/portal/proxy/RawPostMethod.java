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

package com.untangle.tran.portal.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.ChunkedOutputStream;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.ExpectContinueMethod;
import org.apache.log4j.Logger;

public class RawPostMethod extends ExpectContinueMethod
{
    private final Logger logger = Logger.getLogger(getClass());

    private String contentType = null;
    private InputStream is = null;
    private int length = 0;

    // constructors -----------------------------------------------------------

    public RawPostMethod() {
        super();
    }

    public RawPostMethod(String uri) {
        super(uri);
    }

    // public methods ---------------------------------------------------------

    public void setBodyStream(String contentType, InputStream is, int length)
    {
        this.contentType = contentType;
        this.is = is;
        this.length = length;
    }

    // HttpMethod methods -----------------------------------------------------

    public String getName()
    {
        return "POST";
    }

    protected boolean hasRequestContent()
    {
        return true;
    }

    protected void addRequestHeaders(HttpState state, HttpConnection conn)
        throws IOException, HttpException {
        super.addRequestHeaders(state, conn);

        if (null != contentType) {
            setRequestHeader("Content-Type", contentType);
        }

        if (0 > length) {
            removeRequestHeader("Content-Length");
            setRequestHeader("Transfer-Encoding", "chunked");
        } else {
            setRequestHeader("Content-Length", Integer.toString(length));
            removeRequestHeader("Transfer-Encoding");
        }
    }

    protected boolean writeRequestBody(HttpState state, HttpConnection conn)
        throws IOException, HttpException {
        OutputStream os = conn.getRequestOutputStream();

        if (0 > length) {
            ChunkedOutputStream cos = new ChunkedOutputStream(os);
            copyStream(is, cos);
            cos.finish();
        } else {
            copyStream(is, os);
        }
        return true;
    }

    // private methods --------------------------------------------------------

    private void copyStream(InputStream is, OutputStream os)
        throws IOException
    {
        byte[] buf = new byte[4096];
        int i = 0;
        while (0 <= (i = is.read(buf))) {
            os.write(buf, 0, i);
        }

        os.flush();
    }
}
