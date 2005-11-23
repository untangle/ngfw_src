/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ids;

import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;


class FileLoader {
    public static final String[] IGNORED_RULE_FILES = {
        "chat.rules", "deleted.rules", "experimental.rules",
        "icmp-info.rules", "local.rules", "porn.rules", "shellcode.rules" };
    public static final String SNORT_RULES_HOME = "/etc/snort";

    private static final Logger logger = Logger.getLogger(FileLoader.class);

    private FileLoader() { }

    static List<RuleClassification> loadClassifications()
    {
        List<RuleClassification> result = new ArrayList<RuleClassification>();
        try {
            File file = new File(SNORT_RULES_HOME + "/classification.config");
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#"))
                    continue;
                if (line.startsWith("config classification:")) {
                    String classstr = line.substring("config classification:".length()).trim();
                    StringTokenizer st = new StringTokenizer(classstr, ",");
                    String name = null;
                    String description = null;
                    int priority = 0;
                    boolean good = false;
                    if (st.hasMoreTokens())
                        name = st.nextToken().trim();
                    if (st.hasMoreTokens())
                        description = st.nextToken().trim();
                    if (st.hasMoreTokens())
                        try {
                            priority = Integer.parseInt(st.nextToken().trim());
                            good = true;
                        } catch (NumberFormatException x) {
                        }
                    if (good) {
                        logger.debug("Found classification: " + name + "(" + description + ") = " + priority);
                        RuleClassification rc = new RuleClassification(name, description, priority);
                        result.add(rc);
                    } else {
                        logger.error("Bad classification line: " + line);
                    }
                }
            }
            in.close();
        } catch (IOException e) {
            logger.error("Unable to read rule classifications", e);
        }
        return result;
    }

    static List<IDSRule> loadAllRuleFiles(IDSRuleManager manager)
    {
        List<IDSRule> ruleList = new ArrayList<IDSRule>();
        File file = new File(SNORT_RULES_HOME + "/rules");
        loadRuleFiles(manager, file, ruleList);
        return ruleList;
    }

    private static void loadRuleFiles(IDSRuleManager manager, File file, List<IDSRule> result) {
        if (file.isDirectory()) {
            String[] children = file.list();
            Arrays.sort(children);
            for (int i=0; i<children.length; i++)
                loadRuleFiles(manager, new File(file, children[i]), result);
        }
        else {
            String fileName = file.getName();
            for (String str : IGNORED_RULE_FILES) {
                if (str.equals(fileName))
                    return;
            }
            processRuleFile(manager, file, result);
        }
    }

    /** Temp subroutines for loading local snort rules.
     */
    private static void processRuleFile(IDSRuleManager manager, File file, List<IDSRule> result) {
        try {
            String category = file.getName().replaceAll(".rules",""); //Should move this to script land
            category = category.replace("bleeding-",""); //Should move this to script land
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                str = str.trim();
                if (str.length() == 0 || str.charAt(0) == '#')
                    continue;
                IDSRule rule = manager.createRule(str.trim(), category);
                if (rule != null)
                    result.add(rule);
            }
            in.close();
        } catch (IOException e) {
            logger.error("Exception loading rule from file", e);
        }
    }
}
