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
 * Filter for HTTP virus events.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class VirusHttpInfectedFilter implements SimpleEventFilter<HttpLogEventFromReports>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("HTTP Events (from reports tables)"));

    private final String vendor;
    private final String logQuery;

    // constructors -----------------------------------------------------------

    VirusHttpInfectedFilter(String vendor)
    {
        if (vendor.equals("Clam")) //FIXME: more cases
            this.vendor = "Clam";
        else
            this.vendor = vendor;

        logQuery = "FROM HttpLogEventFromReports evt" + 
            " WHERE evt.virus" + this.vendor + "Clean IS FALSE" + 
            " AND evt.policyId = :policyId" + 
            " ORDER BY evt.timeStamp DESC";
    }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries() {
        return new String[] { logQuery };
    }
}
