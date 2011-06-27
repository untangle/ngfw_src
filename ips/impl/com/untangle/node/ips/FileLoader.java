/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

class FileLoader {
    public static final String SNORT_RULES_HOME = "/usr/share/untangle-snort-rules";

    public static final String[] IGNORED_RULE_FILES = {
        "deleted.rules", "experimental.rules",
        "icmp-info.rules", "local.rules", "porn.rules", "shellcode.rules" };

    public static final Pattern[] IGNORED_RULE_PATTERNS = {
        Pattern.compile("clsid", Pattern.CASE_INSENSITIVE) };


    // This should be elsewhere.  XXX
    public static final int[] VERY_SLOW_RULES = {
        2001090, 2001091, 2001092, 2001102, 2001101, 2001103, 2001401, 2001727, 2001537, 2002387 };
    public static final int[] VERY_STUPID_RULES = {
        // These are rules that have lots of false positives
        2229, 2250, 2441 };

    // The default Snort priority for some classifications is stupid.
    public static final String[] FORCED_LOW_PRIORITY_CLASSIFICATIONS = {
        "policy-violation" };

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
                        for (String str : FORCED_LOW_PRIORITY_CLASSIFICATIONS)
                            if (str.equalsIgnoreCase(name))
                                priority = 3;
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

    static Set<IpsRule> loadAllRuleFiles(IpsRuleManager manager)
    {
        Set<IpsRule> ruleSet = new HashSet<IpsRule>();
        File file = new File(SNORT_RULES_HOME + "/rules");
        loadRuleFiles(manager, file, ruleSet);
        return ruleSet;
    }

    private static void loadRuleFiles(IpsRuleManager manager, File file,
                                      Set<IpsRule> result)
    {
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
    private static void processRuleFile(IpsRuleManager manager, File file,
                                        Set<IpsRule> result) {
        try {
            String category = file.getName().repl]aceAll(".rules",""); //Should move this to script land
            category = category.replace("bleeding-",""); //Should move this to script land
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            lineloop:
            while ((str = in.readLine()) != null) {
                str = str.trim();
                if (str.length() == 0 || str.charAt(0) == '#')
                    continue;
                for (Pattern pat : IGNORED_RULE_PATTERNS) {
                    if (pat.matcher(str).find()) {
                        logger.info("Skipping Active-X rule " + str);
                        continue lineloop;
                    }
                }
                IpsRule rule = manager.createRule(str.trim(), category);
                if (rule != null) {
                    int sid = rule.getSid();
                    for (int slowRule : VERY_SLOW_RULES) {
                        if (sid == slowRule) {
                            logger.info("Skipping slow rule " + sid);
                            continue lineloop;
                        }
                    }
                    for (int stupidRule : VERY_STUPID_RULES) {
                        if (sid == stupidRule) {
                            logger.info("Skipping stupid rule " + sid);
                            continue lineloop;
                        }
                    }
                    result.add(rule);
                }
            }
            in.close();
        } catch (IOException e) {
            logger.error("Exception loading rule from file", e);
        }
    }
}
