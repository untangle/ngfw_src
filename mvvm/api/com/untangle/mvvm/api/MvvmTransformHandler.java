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

package com.untangle.mvvm.api;

import java.util.LinkedList;
import java.util.List;

import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.toolbox.MackageDesc;
import com.untangle.mvvm.tran.TransformDesc;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX handler for mvvm-transform.xml files.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmTransformHandler extends DefaultHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private final MackageDesc mackageDesc;

    private final List<String> parents = new LinkedList<String>();
    private final List<String> exports = new LinkedList<String>();

    private String name = null;
    private String className = null;
    private String guiClassName = null;
    private String transformBase = null;
    private boolean singleInstance = false;
    private String displayName = null;

    private StringBuilder parentBuilder;
    private StringBuilder exportBuilder;

    public MvvmTransformHandler(MackageDesc mackageDesc)
    {
        this.mackageDesc = mackageDesc;
    }

    // public methods ---------------------------------------------------------

    public TransformDesc getTransformDesc(Tid tid)
    {
        return new TransformDesc(tid, name, className, guiClassName,
                                 transformBase, exports, parents,
                                 singleInstance, displayName);
    }

    // DefaultHandler methods -------------------------------------------------

    @Override
    public void startElement(String uri, String lName, String qName,
                             Attributes attrs)
        throws SAXException
    {
        if (qName.equals("transform-desc")) {
            processTranDesc(attrs);
        } else if (qName.equals("parent")) {
            parentBuilder = new StringBuilder();
        } else if (qName.equals("export")) {
            exportBuilder = new StringBuilder();
        } else if (qName.equals("mvvm-transform")) {
        } else {
            logger.warn("ignoring unknown element: " + qName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
        if (qName.equals("parent")) {
            parents.add(parentBuilder.toString());
            parentBuilder = null;
        } else if (qName.equals("export")) {
            exports.add(exportBuilder.toString());
            exportBuilder = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
    {
        if (null != parentBuilder) {
            parentBuilder.append(ch, start, length);
        } else if (null != exportBuilder) {
            exportBuilder.append(ch, start, length);
        }
    }

    // private methods --------------------------------------------------------

    private void processTranDesc(Attributes attrs)
        throws SAXException
    {
        for (int i = 0; i < attrs.getLength(); i++) {
            String n = attrs.getQName(i);
            String v = attrs.getValue(i);

            if (n.equals("name")) {
                name = v;
            } else if (n.equals("single-instance")) {
                singleInstance = Boolean.parseBoolean(v);
            } else if (n.equals("classname")) {
                className = v;
            } else if (n.equals("display-name")) {
                displayName = v;
            } else if (n.equals("gui-classname")) {
                guiClassName = v;
            } else if (n.equals("transform-base")) {
                transformBase = v;
            } else {
                logger.warn("skipping unknown attribute: " + n);
            }
        }
    }
}
