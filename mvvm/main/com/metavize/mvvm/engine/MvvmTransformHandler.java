/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;


import java.util.HashSet;

import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.TransformDesc;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.Set;

/**
 * SAX handler for mvvm-transform.xml files.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmTransformHandler extends DefaultHandler
{
    private static final Logger logger = Logger
        .getLogger(MvvmTransformHandler.class);

    private final Set<String> parents = new HashSet();

    private String name = null;
    private String className = null;
    private String guiClassName = null;
    private boolean casing = false;
    private boolean singleInstance = false;
    private String displayName = null;

    private StringBuilder parentBuilder;

    // public methods ---------------------------------------------------------

    public TransformDesc getTransformDesc(Tid tid)
    {
        return new TransformDesc(tid, name, className, guiClassName, casing,
                                 parents, singleInstance, displayName);
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
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
    {
        if (null != parentBuilder) {
            parentBuilder.append(ch, start, length);
        }
    }

    // private methods --------------------------------------------------------

    private void processTranDesc(Attributes attrs)
    {
        for (int i = 0; i < attrs.getLength(); i++) {
            String n = attrs.getQName(i);
            String v = attrs.getValue(i);

            if (n.equals("name")) {
                name = v;
            } else if (n.equals("casing")) {
                casing = Boolean.parseBoolean(v);
            } else if (n.equals("single-instance")) {
                singleInstance = Boolean.parseBoolean(v);
            } else if (n.equals("classname")) {
                className = v;
            } else if (n.equals("display-name")) {
                displayName = v;
            } else if (n.equals("gui-classname")) {
                guiClassName = v;
            }
        }
    }
}
