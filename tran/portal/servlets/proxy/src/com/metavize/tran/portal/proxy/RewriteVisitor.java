/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
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
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.apache.log4j.Logger;
import org.htmlparser.Attribute;
import org.htmlparser.Remark;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.visitors.NodeVisitor;

class RewriteVisitor extends NodeVisitor
{
    private final PrintWriter writer;
    private final UrlRewriter rewriter;

    private final Logger logger = Logger.getLogger(getClass());

    private boolean seenBody = false;
    private boolean inScript = false;

    // constructs -------------------------------------------------------------

    RewriteVisitor(PrintWriter writer, UrlRewriter rewriter)
    {
        this.writer = writer;
        this.rewriter = rewriter;
    }

    // NodeVisitor methods ----------------------------------------------------

    @Override
    public void beginParsing()
    {
    }

    @Override
    public void finishedParsing()
    {
    }

    @Override
    public void visitTag(Tag tag)
    {
        String tagName = tag.getTagName();

        if (!seenBody && tagName.equalsIgnoreCase("body")) {
            writer.println("<head>");
            rewriter.writeJavaScript(writer);
            writer.println("</head>");
        }

        if (tagName.equalsIgnoreCase("script")) {
            String type = tag.getAttribute("type");
            String language = tag.getAttribute("language");
            if (null != type
                && (type.equalsIgnoreCase("text/javascript")
                    || type.equalsIgnoreCase("application/x-javascript"))) {
                inScript = true;
            } else if (null != language && language.equalsIgnoreCase("javascript")) {
                inScript = true;
            }
        }

        for (Attribute a : (List<Attribute>)tag.getAttributesEx()) {
            String name = a.getName();
            if (null != name) {
                name = name.intern();
                if (name.equalsIgnoreCase("href")
                    || name.equalsIgnoreCase("src")) {
                    a.setValue(rewriter.rewriteUrl(a.getValue()));
                } else if (name.equalsIgnoreCase("onload")
                           || name.equalsIgnoreCase("onunload")
                           || name.equalsIgnoreCase("onclick")
                           || name.equalsIgnoreCase("ondblclick")
                           || name.equalsIgnoreCase("onmousedown")
                           || name.equalsIgnoreCase("onmouseup")
                           || name.equalsIgnoreCase("onmouseover")
                           || name.equalsIgnoreCase("onmousemove")
                           || name.equalsIgnoreCase("onmouseout")
                           || name.equalsIgnoreCase("onfocus")
                           || name.equalsIgnoreCase("onblur")
                           || name.equalsIgnoreCase("onkeypress")
                           || name.equalsIgnoreCase("onkeydown")
                           || name.equalsIgnoreCase("onkeyup")
                           || name.equalsIgnoreCase("onsubmit")
                           || name.equalsIgnoreCase("onreset")
                           || name.equalsIgnoreCase("onselect")
                           || name.equalsIgnoreCase("onchange")) {
                    String s = a.getValue();
                    if (!s.endsWith(";")) {
                        s += ";";
                    }
                    Reader r = new StringReader(s);
                    StringWriter w = new StringWriter(s.length());
                    try {
                        rewriter.filterJavaScript(r, w);
                    } catch (IOException exn) {
                        logger.error("this won't happen", exn);
                    }
                    StringBuffer sb = w.getBuffer();
                    while (Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    a.setValue(sb.toString());
                }
            }
        }

        writer.print(tag.toHtml());

        if (tagName.equalsIgnoreCase("head")) {
            seenBody = true;
            rewriter.writeJavaScript(writer);
        }
    }

    @Override
    public void visitEndTag(Tag tag)
    {
        if (tag.getTagName().equalsIgnoreCase("script")) {
            inScript = false;
        }

        writer.print(tag.toHtml());
    }

    @Override
    public void visitStringNode(Text string)
    {
        String text = string.getText();
        if (inScript) {
            Reader r = new StringReader(text);
            Writer w = new StringWriter(text.length());
            try {
                rewriter.filterJavaScript(r, w);
            } catch (IOException exn) {
                logger.error("this won't happen", exn);
            }
            writer.print(w.toString());
        } else {
            writer.print(text);
        }
    }

    @Override
    public void visitRemarkNode(Remark remark)
    {
        writer.print(remark.toHtml());
    }
}
