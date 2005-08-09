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
    public static final String EXTRA_PARAMS_PROPERTY = "EXTRA_PARAMS";
    public static final String SUBREPORT_EXTRA_PARAMS_PROPERTY = "SUBREPORT_EXTRA_PARAMS";
    public static final String EXTRA_PARAM_PROPERTY_PREFIX = "EXTRA_PARAM_";
    public static final String DEFAULT_TEMPLATE_SRC_DIR = "mvvm/resources/reports";
    
    // These should be somewhere else.  XXX
    public static final Map<String, String> SlotTypes = new HashMap<String, String>();
    static {
        SlotTypes.put("TOPRMID",       "java.lang.String");
        SlotTypes.put("TOPLMID",       "java.lang.String");
        SlotTypes.put("TOPLEFT",       "java.sql.Timestamp");
        SlotTypes.put("BOTRMID",       "java.lang.String");
        SlotTypes.put("BOTLMID",       "java.lang.String");
        SlotTypes.put("BOTRIGHT",      "java.lang.Integer");
        SlotTypes.put("TOPRIGHT",      "java.lang.String");
    }

    public static final Map<String, String> TypeAliases = new HashMap<String, String>();
    static {
        TypeAliases.put("String",    "java.lang.String");
        TypeAliases.put("Integer",   "java.lang.Integer");
        TypeAliases.put("Float",     "java.lang.Float");
        TypeAliases.put("Double",    "java.lang.Double"); 
        TypeAliases.put("Long",      "java.lang.Long");
        TypeAliases.put("Boolean",   "java.lang.Boolean");
        TypeAliases.put("Timestamp", "java.sql.Timestamp");
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
            File jrxmlFile;
            if (outputDirName == null) {
                jrxmlFileName = rpdFileName.substring(0, rpdFileName.length() - 4) + JRXML_SUFFIX;
                jrxmlFile = new File(jrxmlFileName);
            } else {
                File rpdFile = new File(rpdFileName);
                String rpdSimple = rpdFile.getName();
                String jrxmlSimple = rpdSimple.substring(0, rpdSimple.length() - 4) + JRXML_SUFFIX;
                jrxmlFile = new File(outputDirName, jrxmlSimple);
                jrxmlFileName = jrxmlFile.toString();
            }
            jrxmlFile.delete();
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
        // System.out.println("FIELDS: " + fieldsProp);
        fset.addFilter(FIELDS_PROPERTY, fieldsProp);

        String paramsProps = getParams(props);
        fset.addFilter(EXTRA_PARAMS_PROPERTY, paramsProps);
        
        String subreportParamsProps = getSubreportParams(props);
        fset.addFilter(SUBREPORT_EXTRA_PARAMS_PROPERTY, subreportParamsProps);


        for (String defaultSlotName : SlotTypes.keySet()) {
            String defaultSlotType = SlotTypes.get(defaultSlotName);
            String pname = defaultSlotName + ".type";
            if (props.getProperty(pname) == null)
                fset.addFilter(pname, defaultSlotType);
        }

        FilterSetCollection fsets = new FilterSetCollection();
        fsets.addFilterSet(fset);

        FileUtils.newFileUtils().copyFile(template.toString(), jrxmlFileName, fsets);
    }

    private String getFields(Properties props)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0;; i++) {
            String name = props.getProperty(FIELD_PROPERTY_PREFIX + i + ".name");
            // System.out.println("Looking for " + FIELD_PROPERTY_PREFIX + i + ".name" + ", got " + name);
            if (name == null) {
                if (i > 0)
                    break;
                else
                    continue;
            }
            String type = props.getProperty(FIELD_PROPERTY_PREFIX + i + ".type");
            if (type == null)
                throw new IllegalArgumentException("Missing type " + i);
            String canonType = TypeAliases.get(type);
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

    private String getParams(Properties props)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0;; i++) {
            String name = props.getProperty(EXTRA_PARAM_PROPERTY_PREFIX + i + ".name");
            // System.out.println("Looking for " + FIELD_PROPERTY_PREFIX + i + ".name" + ", got " + name);
            if (name == null) {
                if (i > 0)
                    break;
                else
                    continue;
            }
            String type = props.getProperty(EXTRA_PARAM_PROPERTY_PREFIX + i + ".type");
            if (type == null)
                throw new IllegalArgumentException("Missing type " + i);
            String canonType = TypeAliases.get(type);
            if (canonType != null)
                type = canonType;
            sb.append("<parameter name=\"");
            sb.append(name);
            sb.append("\" class=\"");
            sb.append(type);
            sb.append("\"/>\n");
        }
        return sb.toString();
    }

    private String getSubreportParams(Properties props)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0;; i++) {
            String name = props.getProperty(EXTRA_PARAM_PROPERTY_PREFIX + i + ".name");
            // System.out.println("Looking for " + FIELD_PROPERTY_PREFIX + i + ".name" + ", got " + name);
            if (name == null) {
                if (i > 0)
                    break;
                else
                    continue;
            }
            String type = props.getProperty(EXTRA_PARAM_PROPERTY_PREFIX + i + ".type");
            if (type == null)
                throw new IllegalArgumentException("Missing type " + i);
            String canonType = TypeAliases.get(type);
            if (canonType != null)
                type = canonType;
            sb.append("<subreportParameter name=\"");
            sb.append(name);
            sb.append("\"> <subreportParameterExpression><![CDATA[$P{");
            sb.append(name);
            sb.append("}]]></subreportParameterExpression> </subreportParameter>\n");
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
