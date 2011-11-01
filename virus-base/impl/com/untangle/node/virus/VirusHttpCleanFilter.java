/**
 * $Id: VirusHttpCleanFilter.java,v 1.00 2011/11/01 16:25:24 dmorris Exp $
 */
package com.untangle.node.virus;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.logging.HttpLogEventFromReports;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

/**
 * Filter for HTTP non-virus events.
 */
public class VirusHttpCleanFilter implements SimpleEventFilter<HttpLogEventFromReports>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Clean Web Events"));

    private final String vendor;
    private final String logQuery;

    public VirusHttpCleanFilter(String vendor)
    {
        this.vendor = vendor;
        
        logQuery = "FROM HttpLogEventFromReports evt" + 
            " WHERE evt.virus" + this.vendor + "Clean IS TRUE" + 
            " AND evt.policyId = :policyId" + 
            " ORDER BY evt.timeStamp DESC";
    }

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { logQuery };
    }
}
