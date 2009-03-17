/*
 * $HeadURL: svn://chef/work/src/uvm-lib/impl/com/untangle/uvm/engine/ReportingManagerImpl.java $
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

package com.untangle.uvm.reports;

import java.util.HashMap;
import java.util.Map;

import com.untangle.uvm.node.NodeDesc;
import org.apache.log4j.Logger;

public class ReportDesc implements Comparable<ReportDesc>
{
    private static final Map<String, ReportDesc> REPORT_DESCS
        = new HashMap<String, ReportDesc>();

    private static final Logger logger = Logger.getLogger(ReportDesc.class);

    private final NodeDesc nodeDesc;
    private final AbstractReport report;

    private ReportDesc(NodeDesc nodeDesc, AbstractReport report)
    {
        this.nodeDesc = nodeDesc;
        this.report = report;
    }

    public static ReportDesc getReportDesc(NodeDesc nd)
    {
        ReportDesc rd = null;

        String name = nd.getMackageDesc().getName();

        synchronized (REPORT_DESCS) {
            rd = REPORT_DESCS.get(name);
            if (null == rd) {
                AbstractReport r = getReport(nd);
                if (null != r) {
                    rd = new ReportDesc(nd, r);
                    REPORT_DESCS.put(name, rd);
                }
            }
        }

        return rd;
    }

    public NodeDesc getNodeDesc()
    {
        return nodeDesc;
    }

    public AbstractReport getReport()
    {
        return report;
    }

    public Application getApplication()
    {
        return new Application(nodeDesc.getName(),
                               nodeDesc.getDisplayName());
    }

    // Comparable methods ------------------------------------------------------

    public int compareTo(ReportDesc o)
    {
        return new Integer(nodeDesc.getMackageDesc().getViewPosition())
            .compareTo(o.nodeDesc.getMackageDesc().getViewPosition());
    }

    // private methods ---------------------------------------------------------

    private static AbstractReport getReport(NodeDesc nd)
    {
        String rcl = nd.getReportsClassName();
        if (null != rcl) {
                try {
                    Class c = Class.forName(rcl);
                    AbstractReport ar = (AbstractReport)c.newInstance();
                    return ar;
                } catch (ClassNotFoundException exn) {
                    logger.warn("could not instantiate " + rcl,
                                exn);
                } catch (InstantiationException exn) {
                    logger.warn("could not instantiate " + rcl,
                                exn);
                } catch (IllegalAccessException exn) {
                    logger.warn("could not instantiate " + rcl,
                                exn);
                }
            }

        return null;
    }
}