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
package com.untangle.node.reporting;

import com.untangle.uvm.node.Validator;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.security.User;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.PipeSpec;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.security.RemoteAdminManager;

public class ReportingNodeImpl extends AbstractNode implements ReportingNode
{
    private final Logger logger = Logger.getLogger(getClass());

    private ReportingSettings settings;

    public ReportingNodeImpl() {}

    public void setReportingSettings(final ReportingSettings settings)
    {
        ReportingNodeImpl.this.settings = settings;

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }

    public ReportingSettings getReportingSettings()
    {
        return settings;
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return new PipeSpec[0];
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from ReportingSettings ts where ts.tid = :tid");
                    q.setParameter("tid", getTid());
                    settings = (ReportingSettings)q.uniqueResult();

                    boolean bSave = false;

                    if (null == settings) {
                        // should never happen
                        settings = initSettings();
                        bSave = true;
                    }

                    if (true == bSave) {
                        s.merge(settings);
                    }

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }

    protected void preStart()
    {
        if (this.settings == null) {
            String[] args = {""};
            postInit(args);
        }

        /* If null, pull these from mail settings. */
        if ( this.settings.getReportingUsers() == null ) {
            loadReportingUsers(this.settings);
        }
    }

    private ReportingSettings initSettings()
    {
        ReportingSettings settings = new ReportingSettings();
        settings.setTid(getTid());

        loadReportingUsers(settings);

        return settings;
    }

    public void initializeSettings()
    {
        setReportingSettings(initSettings());
    }

    public Validator getValidator() {
        return new ReportingValidator();
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getReportingSettings();
    }

    public void setSettings(Object settings)
    {
        setReportingSettings((ReportingSettings)settings);
    }

    private void loadReportingUsers(ReportingSettings s)
    {
        RemoteAdminManager adminManager = LocalUvmContextFactory.context().adminManager();
        String reportEmail = adminManager.getMailSettings().getReportEmail();
        if ( reportEmail == null ) {
            reportEmail = "";
        }
        
        reportEmail = reportEmail.trim();
        
        /* Add in any other users that have reporting on and write access off. */
        for ( User user : adminManager.getAdminSettings().getUsers()) {
            if ( !user.getHasWriteAccess() && user.getHasReportsAccess()) {
                if ( reportEmail.length() == 0 ) {
                    reportEmail += ",";
                }
                
                reportEmail += user.getName();
            }
        }
        
        s.setReportingUsers(reportEmail);
    }
}
