/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: NodeReader.java,v 1.1.1.1 2004/12/01 23:32:22 amread Exp $
 */

package com.metavize.mvvm.util;

import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.metavize.mvvm.prefs.ExtendedPreferences;
import com.metavize.mvvm.prefs.PreferencesBinding;
import com.metavize.mvvm.prefs.PreferencesManagerImpl;
import com.metavize.mvvm.schema.AttributeDesc;
import com.metavize.mvvm.schema.ChildCollectionDesc;
import com.metavize.mvvm.schema.ChildDesc;
import com.metavize.mvvm.schema.ChildMapDesc;
import com.metavize.mvvm.schema.NodeType;
import com.metavize.mvvm.schema.SchemaException;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class NodeReader implements com.metavize.mvvm.schema.SchemaTypeConstants
{
    private DocumentBuilder builder;
    private Document document;
    private InputSource inputSource;

    public NodeReader(InputStream is)
    {
        this(new InputSource(is));
    }

    public NodeReader(String s)
    {
        this(new InputSource(new StringReader(s)));
    }

    public NodeReader(InputSource is)
    {
        this.inputSource = is;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            //factory.setValidating(true);
            //factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
            document = builder.parse(is);
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            System.out.println("ParserConfiguration Error parsing node:" + pce.getMessage());
            pce.printStackTrace();
        } catch (SAXException sxe) {
           // Error generated during parsing)
           Exception  x = sxe;
           if (sxe.getException() != null)
               x = sxe.getException();
           System.out.println("SAX Error parsing node:" + x.getMessage());
           x.printStackTrace();
        } catch (IOException ioe) {
           // I/O error
           System.out.println("IO Error parsing node:" + ioe.getMessage());
           ioe.printStackTrace();
        }
    }

    // Here is the top-level parse function
    //
    // Each node can either be:
    // <?xml version="1.0"?> ?????
    //  -- a comment
    //  -- a node complex element:
    //          <node node-type="fully-qualified-node-type-name"h attr1="foo" attr2="bar">
    //             <child1 catr1="baz">...</child1>
    //             <child2 node-type="nuther-fully-qualified-node-type-name" ...>
    //
    public com.metavize.mvvm.schema.Node parse()
        throws SchemaException
    {
        if (document == null)
            // Throw now since we couldn't at constructor time.
            throw new SchemaException("Unable to parse input file " + inputSource);

        List result = new ArrayList();
        Node root = document.getDocumentElement();
        if (root.getNodeName().equals(NODE_ELEMENT_NAME)) {
            com.metavize.mvvm.schema.NodeType nt = getNodeType(root);
            if (nt == null)
                throw new SchemaException("root node type not found");
            return parseNode(root, nt);
        } else {
            throw new SchemaException("root node not found");
        }
    }


    // private static final com.metavize.mvvm.schema.Node[] nodeArrayProto =
    //   new com.metavize.mvvm.schema.Node[] {};


    // Returns the evaluated node-type attribute for this xml node, if it has one,
    // and removes it from the xml node.
    private NodeType getNodeType(Node node)
    {
        NodeType nt = null;
        NamedNodeMap attrs = node.getAttributes();
        if (null != attrs.getNamedItem(NODE_TYPE_ATTR_NAME)) {
            Attr removedAttr = (Attr)attrs
                .removeNamedItem(NODE_TYPE_ATTR_NAME);
            try {
                String nodeClassName = removedAttr.getValue();
                System.out.println("nodeclass: " + nodeClassName);
                Class nodeClass = Thread.currentThread()
                    .getContextClassLoader().loadClass(nodeClassName);
                System.out.println("class: " + nodeClass);
                nt = NodeType.type(nodeClass);
                System.out.println("nt: " + nt);
            } catch (ClassNotFoundException exn) {
                // nt remains null
            }
        }
        return nt;
    }

    // Parses a single Schema Node from the DOM Node assuming the given node type.
    // Recursive.
    private com.metavize.mvvm.schema.Node parseNode(Node node, NodeType nt)
        throws SchemaException
    {
        com.metavize.mvvm.schema.Node result = nt.instantiate();

        // First the attributes
        AttributeDesc[] ads = nt.attributeDescs();
        NamedNodeMap attrs = node.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            String attrName = attr.getName();
            AttributeDesc ad = null;
            for (int j = 0; j < ads.length; j++) {
                if (attrName.equals(ads[j].name())) {
                    ad = ads[j];
                    break;
                }
            }
            if (ad == null)
                throw new SchemaException("unknown attribute " + attrName);

            Object attrValue = ad.decode(attr.getValue());
            result.put(ad, attrValue);
        }

        // Then the children
        ChildDesc[] cds = nt.childDescs();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.NOTATION_NODE)
                continue;
            NodeType specificChildType = getNodeType(child);

            String childName = child.getNodeName();
            boolean foundChild = false;
            for (int j = 0; j < cds.length; j++) {
                ChildDesc cd = cds[j];
                if (childName.equals(cd.name())) {
                    NodeType childType;
                    if (specificChildType != null) {
                        childType = specificChildType;
                    } else {
                        try {
                        childType = NodeType.type(cd.iface());
                        } catch (Exception exn) {
                            throw new RuntimeException(exn);
                        }
                    }
                    com.metavize.mvvm.schema.Node newChild
                        = parseNode(child, childType);

                    if (cd instanceof ChildCollectionDesc) {
                        ChildCollectionDesc ccd = (ChildCollectionDesc)cd;
                        List them = result.collection(ccd);
                        them.add(newChild);
                    } else if (cd instanceof ChildMapDesc) {
                        // XXX
                    } else {
                        // should test cardinality first XXX
                        result.put(cd, newChild);
                    }
                    foundChild = true;
                    break;
                }
            }
            if (!foundChild)
                throw new SchemaException("unknown child " + childName);
        }

        return result;
    }


    private static void usage()
    {
        System.out.println("Usage: pread [-v] [-o outputdir ] prefsnode.xml ...");
        System.out.println("           writes a low-level preferences .xml file to the given output");
        System.out.println("           directory for prefsnode.xml ...");
        System.exit(1);
    }

    private static final int MODE_PREFSOUT = 0;

    public static void main(String args[])
    {
        String[] inputFiles;
        String outputDir = null;
        int mode = MODE_PREFSOUT;
        int verbose = 1;

        if (args == null || args.length == 0)
            usage();

        int i = 0;
        while (i < args.length) {
            if (args[i].equals("-q")) {
                verbose = 0;
            } else if (args[i].equals("-v")) {
                verbose = 2;
            } else if (args[i].equals("-vv")) {
                verbose = 3;
            } else if (args[i].equals("-vvv")) {
                verbose = 4;
            } else if (args[i].equals("-o")) {
                if (i++ == args.length)
                    usage();
                outputDir = args[i];
            } else if (args[i].startsWith("-")) {
                usage();
            } else {
                // Assume file names have begun.
                break;
            }
            i++;
        }
        if (i == args.length)
            usage();

        inputFiles = new String[args.length - i];
        for (int j = i; j < args.length; j++) {
            inputFiles[j-i] = args[j];
        }

        switch (mode) {
        case MODE_PREFSOUT:
            try {
                for (int j = 0; j < inputFiles.length; j++) {
                    NodeReader preader = new NodeReader(inputFiles[j]);
                    com.metavize.mvvm.schema.Node node = preader.parse();
                    ExtendedPreferences epnode
                        = PreferencesManagerImpl.preferencesManager()
                        .systemPreferences().
                        extendedNode("/this/is/a/fake/path");
                    PreferencesBinding pb = (PreferencesBinding) PreferencesBinding.bind(node, epnode);
                    pb.marshall();
                    System.out.println("Node:");
                    epnode.exportSubtree(System.out);
                    System.out.println();
                }
            } catch (Exception x) {
                System.out.println("Error parsing node:" + x.getMessage());
                x.printStackTrace();
                System.exit(1);
                return;         // Stupid java
            }
            break;
        }
    } // main
}
