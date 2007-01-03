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

package com.untangle.buildutil;

import java.io.File;
import java.io.FileInputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    public static final String SUBREPORT_EXTRA_PARAMS_PROPERTY = "SUBREPORT_EXTRA_PARAMS";
    public static final String EXTRA_PARAMS_PROPERTY = "EXTRA_PARAMS";
    public static final String EXTRA_PARAM_PROPERTY_PREFIX = "EXTRA_PARAM_";
    public static final String EXTRA_VARS_PROPERTY = "EXTRA_VARS";
    public static final String EXTRA_VAR_PROPERTY_PREFIX = "EXTRA_VAR_";
    public static final String DEFAULT_TEMPLATE_SRC_DIR = "mvvm/resources/reports";
    
    // These should be somewhere else.  XXX
    public static final Map<String, String> SlotTypes = new HashMap<String, String>();
    static {
        SlotTypes.put("TOPRMID",       "java.lang.String");
        SlotTypes.put("TOPMMID",       "java.lang.String");
        SlotTypes.put("TOPLMID",       "java.lang.String");
        SlotTypes.put("TOPLEFT",       "java.sql.Timestamp");
        SlotTypes.put("BOTRMID",       "java.lang.String");
        SlotTypes.put("BOTMMID",       "java.lang.String");
        SlotTypes.put("BOTLMID",       "java.lang.String");
        SlotTypes.put("BOTRIGHT",      "java.lang.String");
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
        
    private final boolean isVerbose;
    private final File templateSrcDir;
    private final String outputDirName;
    private final String fileArray[];

    public static void main(String[] args)
    {
        ReportGenerator rg = parseArgs( args );
        
        rg.generateAllFiles();

        System.exit(0);
    }

    private ReportGenerator(boolean isVerbose, File templateSrcDir, String outputDirName, String fileArray[] )
    {
        this.isVerbose      = isVerbose;
        this.templateSrcDir = templateSrcDir;
        this.outputDirName  = outputDirName;
        this.fileArray      = fileArray;
    }

    private void generateAllFiles()
    {
        if (!this.templateSrcDir.isDirectory())
            throw new IllegalArgumentException("Template dir " + templateSrcDir.getAbsolutePath() + " does not exist");
        for ( String rpdFileName : fileArray ) {
            try {
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
                generate(rpdFileName, jrxmlFileName);
            } catch (Exception x) {
                System.out.println("Unable to process " + rpdFileName + ": " + x.getMessage());
                x.printStackTrace(System.out);
                System.exit(1);
            }
        }
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
        
        String varsProps = getVars(props);
        fset.addFilter(EXTRA_VARS_PROPERTY, varsProps);
        
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
                throw new IllegalArgumentException("Missing field type " + i);
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
                throw new IllegalArgumentException("Missing param type " + i);
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

    /**
     * Describe <code>getVars</code> method here.
     *
     * name, type, and value are required.
     * resetType, incrementType, and calculation are optional.
     *
     * @param props a <code>Properties</code> value
     * @return a <code>String</code> value
     */
    private String getVars(Properties props)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0;; i++) {
            String name = props.getProperty(EXTRA_VAR_PROPERTY_PREFIX + i + ".name");
            if (name == null) {
                if (i > 0)
                    break;
                else
                    continue;
            }
            String type = props.getProperty(EXTRA_VAR_PROPERTY_PREFIX + i + ".type");
            if (type == null)
                throw new IllegalArgumentException("Missing var type " + i);
            String canonType = TypeAliases.get(type);
            if (canonType != null)
                type = canonType;
            String value = props.getProperty(EXTRA_VAR_PROPERTY_PREFIX + i + ".value");
            if (value == null)
                throw new IllegalArgumentException("Missing var value " + i);
            String resetType = props.getProperty(EXTRA_VAR_PROPERTY_PREFIX + i + ".resetType");
            String incrementType = props.getProperty(EXTRA_VAR_PROPERTY_PREFIX + i + ".incrementType");
            String calculation = props.getProperty(EXTRA_VAR_PROPERTY_PREFIX + i + ".calculation");
            sb.append("<variable name=\"");
            sb.append(name);
            sb.append("\" class=\"");
            sb.append(type);
            sb.append("\"");
            if (resetType != null) {
                sb.append(" resetType=\"");
                sb.append(resetType);
                sb.append("\"");
            }
            if (incrementType != null) {
                sb.append(" incrementType=\"");
                sb.append(incrementType);
                sb.append("\"");
            }
            if (calculation != null) {
                sb.append(" calculation=\"");
                sb.append(calculation);
                sb.append("\"");
            }
            sb.append("> <variableExpression><![CDATA[");
            sb.append(value);
            sb.append("]]></variableExpression> </variable>\n");
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
                throw new IllegalArgumentException("Missing subreport param type " + i);
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

    private static ReportGenerator parseArgs( String args[] )
    {
        boolean verbose = false;
        String templateSrcDirName = DEFAULT_TEMPLATE_SRC_DIR;
        String outputDirName = null;

        int i;
        for ( i =0 ; i < args.length ; i++ ) {
            String arg = args[i];
            
            if ( !arg.startsWith( "-" )) break;
            
            if ( arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-t")) {
                templateSrcDirName = args[++i];
            } else if (arg.equals("-o")) {
                outputDirName = args[++i];
            }
        }

        int numFiles = args.length - i;

        if ( numFiles == 0 ) dieUsage();

        String fileArray[] = new String[numFiles];
        System.arraycopy( args, i, fileArray, 0, numFiles );
        
        return new ReportGenerator( verbose, new File( templateSrcDirName ), outputDirName, fileArray );
    }

    private static void dieUsage()
    {
        System.out.println("Usage: ReportGenerator [ -v ] [ -t templateSrcDir ] [ -o outputDir ] <reporttemplate.rpd> <reporttemplate.rpd>*");
        System.out.println();
        System.exit(1);
    }
}
