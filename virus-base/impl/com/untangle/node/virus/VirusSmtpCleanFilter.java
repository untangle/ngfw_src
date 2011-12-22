/**
 * $Id: VirusSmtpCleanFilter.java,v 1.00 2011/11/01 16:25:18 dmorris Exp $
 */
package com.untangle.node.virus;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.logging.MailLogEventFromReports;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.util.I18nUtil;

/**
 * Filter for SMTP non-virus events.
 */
public class VirusSmtpCleanFilter implements ListEventFilter<MailLogEventFromReports>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Clean Email Events"));

    private final String vendor;
    private final String logQuery;

    public VirusSmtpCleanFilter(String vendor)
    {
        this.vendor = vendor;

        logQuery = "FROM MailLogEventFromReports evt" + 
            " WHERE evt.addrKind IN ('T', 'C')" +
            " AND evt.virus" + this.vendor + "Clean IS TRUE" + 
            " AND evt.policyId = :policyId" + 
            " ORDER BY evt.timeStamp DESC";
    }

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    @SuppressWarnings("unchecked") //Query
    public void doGetEvents(Session s, List<MailLogEventFromReports> l, int limit, Map<String, Object> params)
    {
        Query q = s.createQuery(logQuery);
        for (String param : q.getNamedParameters()) {
            Object o = params.get(param);
            if (null != o)
                q.setParameter(param, o);
        }
        
        q.setMaxResults(limit);

        int c = 0;
        for (Iterator<MailLogEventFromReports> i = q.iterate(); i.hasNext() && c < limit; ) {
            MailLogEventFromReports evt = i.next();
            evt.setVendor(vendor);
            evt.setSender(MailLogEventFromReports.getSenderForMsg(s, evt.getMsgId()));

            Hibernate.initialize(evt);

            l.add(evt);
            c++;
        }
    }
}
