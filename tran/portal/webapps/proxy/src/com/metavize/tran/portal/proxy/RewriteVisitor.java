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
        for (Attribute a : (List<Attribute>)tag.getAttributesEx()) {
            String name = a.getName();
            if (null != name
                && (name.equalsIgnoreCase("href")
                    || name.equalsIgnoreCase("src"))) {
                a.setValue(rewriter.rewriteUrl(a.getValue()));
            }
        }

        writer.print(tag.toHtml());
    }

    @Override
    public void visitEndTag(Tag tag)
    {
        writer.print(tag.toHtml());
    }

    @Override
    public void visitStringNode(Text string)
    {
        writer.print(string.toHtml());
    }

    @Override
    public void visitRemarkNode(Remark remark)
    {
        writer.print(remark.toHtml());
    }
}
