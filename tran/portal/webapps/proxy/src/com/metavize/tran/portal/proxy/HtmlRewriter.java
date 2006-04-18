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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Enumeration;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;

import org.apache.log4j.Logger;

public class HtmlRewriter
{
    private final PrintWriter writer;
    private final Logger logger = Logger.getLogger(getClass());

    HtmlRewriter(OutputStream os)
    {
        this.writer = new PrintWriter(os);
    }

    public void flush() throws BadLocationException
    {
        System.out.println("FLUSH" + "\n");
    }

    public void handleText(char[] data, int pos)
    {
        System.out.println("TEXT: " + new String(data) + "\n");
    }

    public void handleComment(char[] data, int pos)
    {
        System.out.println("COMMENT: " + new String(data) + "\n");
    }

    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos)
    {
        try {
            writer.write("<");
            writer.write(t.toString());
            writeAttributes(writer, a);
            writer.write(">");
        } catch (IOException exn) {
            logger.warn("could not write", exn);
        }
    }

    public void handleEndTag(HTML.Tag t, int pos)
    {
        writer.print("</");
        writer.print(t.toString());
        writer.print(">");
    }

    public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos)
    {
        System.out.println("SIMPLE: " + t);
    }

    public void handleError(String errorMsg, int pos)
    {
        System.out.println("ERROR: " + errorMsg);
    }

    public void handleEndOfLineString(String eol)
    {
        System.out.println("EOL: " + eol + "\n");
    }

    // private methods --------------------------------------------------------

    private void writeAttributes(Writer writer, MutableAttributeSet a)
        throws IOException
    {
        for (Enumeration e = a.getAttributeNames(); e.hasMoreElements(); ) {
            Object o = e.nextElement();
            writer.write(" ");
            writer.write(o.toString());
            writer.write("=\"");
            writer.write(a.getAttribute(o).toString());
            writer.write("\"");
        }
    }
}
