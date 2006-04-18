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

package com.metavize.tran.portal.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class WebProxy extends HttpServlet
{
    private static final String HTML_READER = "org.htmlparser.sax.XMLReader";

    private Logger logger = Logger.getLogger(getClass());

    private HttpClient httpClient;
    private ParserDelegator parserDelegator;

    @Override
    public void init() throws ServletException
    {
        httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
        parserDelegator = new ParserDelegator();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        InputStream is = null;
        PrintWriter w = null;

        try {
            HttpMethod get = new GetMethod(getUrl(req));
            int rc = httpClient.executeMethod(get);

            XMLReader xr = XMLReaderFactory.createXMLReader(HTML_READER);
            w = new PrintWriter(resp.getWriter());
            ContentHandler ch = new HtmlRewriter(w);
            xr.setContentHandler(ch);
            xr.parse(new InputSource(is = get.getResponseBodyAsStream()));
        } catch (IOException exn) {
            logger.warn("could not write response", exn);
        } catch (SAXException exn) {
            logger.warn("could not write parse html", exn);
        } finally {
            if (null != w) {
                w.close();
            }

            if (null != is) {
                try {
                    is.close();
                } catch (IOException exn) {
                    logger.warn("could not close InputStream", exn);
                }
            }
        }
    }

    // private methods --------------------------------------------------------

    private String getUrl(HttpServletRequest req)
    {
        String p = req.getPathInfo();
        String qs = req.getQueryString();

        String url = "http:/" + p + (null == qs ? "" : qs);
        System.out.println("URL: " + url);

        return url;
    }
}
