/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.exploder.browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.Reader;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

public class XsltFilter implements Filter
{
    private static final String XSL_PATH = "browser.xsl";

    private final Logger logger = Logger.getLogger(getClass());

    private ServletContext servletContext;

    public void doFilter(ServletRequest req, ServletResponse resp,
                         FilterChain fc)
        throws ServletException
    {
        resp.setContentType("text/html");

        try {
            Wrapper wrapper = new Wrapper((HttpServletResponse)resp);

            fc.doFilter(req, wrapper);

            Transformer t = getTransformer();
            Source s = new StreamSource(wrapper.getReader());
            Result r = new StreamResult(resp.getWriter());

            t.transform(s, r);

        } catch (IOException exn) {
            throw new ServletException("couldn't transform", exn);
        } catch (TransformerException exn) {
            throw new ServletException("couldn't transform", exn);
        }
    }

    public void init(FilterConfig filterConfig)
    {
        servletContext = filterConfig.getServletContext();
    }

    public void destroy()
    {
    }

    private Transformer getTransformer()
        throws ServletException
    {
        // XXX make my own factory cache instances
        TransformerFactory transFactory = TransformerFactory.newInstance();

        InputStream is = servletContext.getResourceAsStream(XSL_PATH);

        Source src = new StreamSource(is);

        try {
            return transFactory.newTransformer(src);
        } catch (TransformerConfigurationException exn) {
            throw new ServletException("couldn't create Transformer", exn);
        }
    }

    private static class Wrapper extends HttpServletResponseWrapper
    {
        private final PipedReader reader;
        private final PrintWriter writer;

        public Wrapper(HttpServletResponse resp) throws IOException
        {
            super(resp);

            reader = new PipedReader();
            writer = new PrintWriter(new PipedWriter(reader));
        }

        public PrintWriter getWriter()
        {
            return writer;
        }

        public Reader getReader()
        {
            return reader;
        }

        public void setContentType(String contentType)
        {
            // output is 'text/html'
        }
    }
}

