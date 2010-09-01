/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.toolbox.MackageDesc;

/**
 * SAX handler for uvm-node.xml files.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UvmNodeHandler extends DefaultHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private final MackageDesc mackageDesc;

    private final List<String> parents = new LinkedList<String>();
    private final List<String> exports = new LinkedList<String>();
    private final List<String> uvmResources = new LinkedList<String>();

    private String className = null;
    private String reportsClassName = null;
    private String nodeBase = null;
    private boolean singleInstance = false;
    private boolean hasPowerButton = true;
    private boolean noStart = false;

    private StringBuilder parentBuilder;
    private StringBuilder exportBuilder;
    private StringBuilder uvmResourceBuilder;

    public UvmNodeHandler(MackageDesc mackageDesc)
    {
        this.mackageDesc = mackageDesc;
    }

    // public methods ---------------------------------------------------------

    public NodeDesc getNodeDesc(NodeId tid)
    {
        return new NodeDesc(tid, mackageDesc, className, reportsClassName,
                            nodeBase, exports, parents, uvmResources,
                            singleInstance, hasPowerButton, noStart);
    }

    // DefaultHandler methods -------------------------------------------------

    @Override
    public void startElement(String uri, String lName, String qName,
                             Attributes attrs)
        throws SAXException
    {
        if (qName.equals("node-desc")) {
            processNodeDesc(attrs);
        } else if (qName.equals("parent")) {
            parentBuilder = new StringBuilder();
        } else if (qName.equals("export")) {
            exportBuilder = new StringBuilder();
        } else if (qName.equals("uvm-resource")) {
            uvmResourceBuilder = new StringBuilder();
        } else if (qName.equals("uvm-node")) {
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
        } else if (qName.equals("uvm-resource")) {
            uvmResources.add(uvmResourceBuilder.toString());
            uvmResourceBuilder = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
    {
        if (null != parentBuilder) {
            parentBuilder.append(ch, start, length);
        } else if (null != exportBuilder) {
            exportBuilder.append(ch, start, length);
        } else if (null != uvmResourceBuilder) {
            uvmResourceBuilder.append(ch, start, length);
        }
    }

    // private methods --------------------------------------------------------

    private void processNodeDesc(Attributes attrs)
        throws SAXException
    {
        for (int i = 0; i < attrs.getLength(); i++) {
            String n = attrs.getQName(i);
            String v = attrs.getValue(i);

            if (n.equals("single-instance")) {
                singleInstance = Boolean.parseBoolean(v);
            } else if (n.equals("classname")) {
                className = v;
            } else if (n.equals("reports-classname")) {
                reportsClassName = v;
            } else if (n.equals("node-base")) {
                nodeBase = v;
            } else if (n.equals("power-button")) {
                hasPowerButton = Boolean.parseBoolean(v);
            } else if (n.equals("no-start")) {
                noStart = Boolean.parseBoolean(v);
            } else {
                logger.warn("skipping unknown attribute: " + n);
            }
        }
    }
}
