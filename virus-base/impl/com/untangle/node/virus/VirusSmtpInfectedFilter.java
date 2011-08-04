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
 * Filter for HTTP virus events.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class VirusSmtpInfectedFilter implements ListEventFilter<MailLogEventFromReports>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Mail Events (from reports tables)"));

    private final String vendor;
    private final String logQuery;

    // constructors -----------------------------------------------------------

    VirusSmtpInfectedFilter(String vendor)
    {
        if (vendor.equals("Clam")) //FIXME: more cases
            this.vendor = "Clam";
        else
            this.vendor = vendor;

        logQuery = "FROM MailLogEventFromReports evt" + 
            " WHERE evt.virus" + this.vendor + "Clean IS FALSE" + 
            " AND evt.policyId = :policyId" + 
            " ORDER BY evt.timeStamp DESC";
    }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    @SuppressWarnings("unchecked") //Query
    public void doGetEvents(Session s, List<MailLogEventFromReports> l,
                            int limit, Map<String, Object> params) {
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
