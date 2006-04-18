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

import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class HtmlRewriter implements ContentHandler
{
    private static final String TEXT_STRING = "#text";

    private final PrintWriter writer;
    private final Logger logger = Logger.getLogger(getClass());

    HtmlRewriter(PrintWriter writer)
    {
        this.writer = writer;
    }

    public void setDocumentLocator(Locator locator)
    {
        System.out.println("setDocumentLocator");
    }

    public void startDocument() throws SAXException
    {
        System.out.println("startDocument");
    }

    public void endDocument() throws SAXException
    {
        System.out.println("endDocument");
    }

    public void startPrefixMapping(String prefix, String uri)
        throws SAXException
    {
        System.out.println("startPrefixMapping");
    }

    public void endPrefixMapping(String prefix) throws SAXException
    {
        System.out.println("endPrefixMapping");
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes atts)
        throws SAXException
    {
        writer.print("<");
        writer.print(localName);
        int l = atts.getLength();
        for (int i = 0; i < l; i++) {
            String k = atts.getQName(i);
            String v = atts.getValue(i);

            if (TEXT_STRING == k.intern()) {
                writer.print(v);
            } else {
                writer.print(k);
                writer.print("=\"");
                writer.print(v);
                writer.print("\"");
            }
        }

        writer.print(">");
    }

    public void endElement(String uri, String localName, String qName)
        throws SAXException
    {
        writer.print("</");
        writer.print(localName);
        writer.print(">");
    }

    public void characters(char ch[], int start, int length)
        throws SAXException
    {
        writer.print(new String(ch, start, length));
    }

    public void ignorableWhitespace(char ch[], int start, int length)
        throws SAXException
    {
        writer.print(new String(ch, start, length));
    }

    public void processingInstruction(String target, String data)
        throws SAXException
    {
        System.out.println("processingInstruction");
    }

    public void skippedEntity(String name)
        throws SAXException
    {
        System.out.println("skippedEntity");
    }
}
