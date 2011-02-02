/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.servlet.alpaca;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.ChunkedOutputStream;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.ExpectContinueMethod;

/**
 * Provides a HttpClient method that allows arbitrary content from an
 * <code>InputStream</code>.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class RawPostMethod extends ExpectContinueMethod
{
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
