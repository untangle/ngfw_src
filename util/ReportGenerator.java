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

import java.io.*;
import java.util.*;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;

/**
 * Generate a JRXML from a RPD template.
 *
 * @author
 * @version 1.0
 */
public class ReportGenerator
{
    public static final String RPD_SUFFIX = ".rpd";
    public static final String JRXML_SUFFIX = ".jrxml";
    public static final String WHICH_TEMPLATE_PROPERTY = "TEMPLATE";
    public static final String FIELDS_PROPERTY = "FIELDS";
    public static final String FIELD_PROPERTY_PREFIX = "FIELD_";
    public static final String DEFAULT_TEMPLATE_SRC_DIR = "mvvm/resources/reports";

    public static Map<String, String> typeAliases = new HashMap<String, String>();
    static {
        typeAliases.put("String", "java.lang.String");
        typeAliases.put("Integer", "java.lang.Integer");
        typeAliases.put("Float", "java.lang.Float");
        typeAliases.put("Double", "java.lang.Double"); 
        typeAliases.put("Long", "java.lang.Long");
        typeAliases.put("Timestamp", "java.sql.Timestamp");
    }
        
    private File templateSrcDir;

    public static void main(String[] args)
    {
        boolean verbose = false;
        String rpdFileName = null;
        String templateSrcDirName = DEFAULT_TEMPLATE_SRC_DIR;;
        String outputDirName = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-v")) {
                verbose = true;
            } else if (args[i].equals("-t")) {
                templateSrcDirName = args[++i];
            } else if (args[i].equals("-o")) {
                outputDirName = args[++i];
            } else if (args[i].endsWith(RPD_SUFFIX)) {
                if (rpdFileName == null)
                    rpdFileName = args[i];
                else
                    dieUsage();
            } else {
                dieUsage();
            }
        }
        if (rpdFileName == null)
            dieUsage();

        try {
            File templateSrcDir = new File(templateSrcDirName);
            if (!templateSrcDir.isDirectory())
                throw new IllegalArgumentException("Template dir " + templateSrcDir.getAbsolutePath() + " does not exist");
            ReportGenerator rg = new ReportGenerator(templateSrcDir);
            String jrxmlFileName;
            if (outputDirName == null) {
                jrxmlFileName = rpdFileName.substring(0, rpdFileName.length() - 4) + JRXML_SUFFIX;
            } else {
                File rpdFile = new File(rpdFileName);
                String rpdSimple = rpdFile.getName();
                String jrxmlSimple = rpdSimple.substring(0, rpdSimple.length() - 4) + JRXML_SUFFIX;
                File jrxmlFile = new File(outputDirName, jrxmlSimple);
                jrxmlFileName = jrxmlFile.toString();
            }
            rg.generate(rpdFileName, jrxmlFileName);
        } catch (Exception x) {
            System.out.println("Unable to process " + rpdFileName + ": " + x.getMessage());
            x.printStackTrace(System.out);
            System.exit(1);
        }
        System.exit(0);
    }

    private ReportGenerator(File templateSrcDir) {
        this.templateSrcDir = templateSrcDir;
    }

    private void generate(String rpdFileName, String jrxmlFileName)
        throws Exception
    {
        Properties props = new Properties();
        File rpdFile = new File(rpdFileName);
        FileInputStream fis = new FileInputStream(rpdFile);
        props.load(fis);
        fis.close();
        String whichTemplate = props.getProperty(WHICH_TEMPLATE_PROPERTY);
        if (whichTemplate == null)
            throw new IllegalArgumentException("RPD file " + rpdFileName + " does not contain " +
                                               WHICH_TEMPLATE_PROPERTY + " property");
        File template = new File(templateSrcDir, whichTemplate + JRXML_SUFFIX);
        if (!template.isFile())
            throw new IllegalArgumentException("Template file " + template + " does not exist");

        FilterSet fset = new FilterSet();
        fset.setFiltersfile(rpdFile);
        String fieldsProp = getFields(props);
        System.out.println("FIELDS: " + fieldsProp);
        fset.addFilter(FIELDS_PROPERTY, fieldsProp);

        FilterSetCollection fsets = new FilterSetCollection();
        fsets.addFilterSet(fset);
        FileUtils.newFileUtils().copyFile(template.toString(), jrxmlFileName, fsets);
    }

    private String getFields(Properties props)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0;; i++) {
            String name = props.getProperty(FIELD_PROPERTY_PREFIX + i + ".name");
            System.out.println("Looking for " + FIELD_PROPERTY_PREFIX + i + ".name" + ", got " + name);
            if (name == null) {
                if (i > 0)
                    break;
                else
                    continue;
            }
            String type = props.getProperty(FIELD_PROPERTY_PREFIX + i + ".type");
            if (type == null)
                throw new IllegalArgumentException("Missing type " + i);
            String canonType = typeAliases.get(type);
            if (canonType != null)
                type = canonType;
            sb.append("<field name=\"");
            sb.append(name);
            sb.append("\" class=\"");
            sb.append(type);
            sb.append("\"/>\n");
        }
        return sb.toString();
    }


    private static void dieUsage()
    {
        System.out.println("Usage: ReportGenerator [ -v ] [ -t templateSrcDir ] [ -o outputDir ] reporttemplate.rpd");
        System.out.println();
        System.exit(1);
    }
}
