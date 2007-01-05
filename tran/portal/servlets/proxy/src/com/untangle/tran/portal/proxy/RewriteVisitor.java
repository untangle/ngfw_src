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

import java.io.PrintWriter;
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
        }

        for (Attribute a : (List<Attribute>)tag.getAttributesEx()) {
            String name = a.getName();
            if (null != name) {
                name = name.intern();
                if (name.equalsIgnoreCase("href")
                    || name.equalsIgnoreCase("src")) {
                    a.setValue(rewriter.rewriteUrl(a.getValue()));
                } else if (name.equalsIgnoreCase("action") && tagName.equalsIgnoreCase("form")) {
                    a.setValue(rewriter.rewriteUrl(a.getValue()));
                } else if (name.equalsIgnoreCase("http-equiv") && a.getValue().equalsIgnoreCase("refresh")) {
                    boolean changed = false;

                    Attribute contentAttr = tag.getAttributeEx("content");

                    String value = contentAttr.getValue();
                    String[] ca = value.split(";");
                    for (int i = 0; i < ca.length; i++) {
                        String s = ca[i];
                        if (s.trim().toLowerCase().startsWith("url=")) {
                            int j = s.indexOf('=');
                            if (s.length() > j + 1) {
                                ca[i] = s.substring(0, j + 1) + rewriter.rewriteUrl(s.substring(j + 1));
                                changed = true;
                            }
                        }
                    }

                    if (changed) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < ca.length; i++) {
                            sb.append(ca[i]);
                        }

                        contentAttr.setValue(sb.toString());
                    }
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
        writer.print(tag.toHtml());
    }

    @Override
    public void visitStringNode(Text string)
    {
        String text = string.getText();
        writer.print(text);
    }

    @Override
    public void visitRemarkNode(Remark remark)
    {
        writer.print(remark.toHtml());
    }
}
