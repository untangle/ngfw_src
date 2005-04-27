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

import com.metavize.mvvm.tran.TransformDesc;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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

    public TransformDesc transformDesc;

    private StringBuilder parentBuilder;

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
            transformDesc.addParent(parentBuilder.toString());
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
        transformDesc = new TransformDesc();
        for (int i = 0; i < attrs.getLength(); i++) {
            String n = attrs.getQName(i);
            String v = attrs.getValue(i);

            if (n.equals("name")) {
                transformDesc.setName(v);
            } else if (n.equals("casing")) {
                transformDesc.setCasing(Boolean.parseBoolean(v));
            } else if (n.equals("single-instance")) {
                transformDesc.setSingleInstance(Boolean.parseBoolean(v));
            } else if (n.equals("classname")) {
                transformDesc.setClassName(v);
            } else if (n.equals("display-name")) {
                transformDesc.setDisplayName(v);
            } else if (n.equals("gui-classname")) {
                transformDesc.setGuiClassName(v);
            }
        }
    }
}
