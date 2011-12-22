package com.untangle.node.spam;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.MailLogEventFromReports;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.util.I18nUtil;

public class SpamAllFilter implements ListEventFilter<MailLogEventFromReports>
{
    private static final RepositoryDesc SPAM_REPO_DESC = new RepositoryDesc(I18nUtil.marktr("All Events"));
    private static final RepositoryDesc CLAM_REPO_DESC = new RepositoryDesc(I18nUtil.marktr("All Email Events"));

    private final String vendor;
    private final String logQuery;
    private final RepositoryDesc repoDesc;

    // constructors -----------------------------------------------------------

    public SpamAllFilter(String vendor)
    {
        if (vendor.equals("SpamAssassin")) //FIXME: more cases
            this.vendor = "sa";
        else if (vendor.equals("CommtouchAs"))
            this.vendor = "ct";
        else if (vendor.equals("Clam"))
            this.vendor = "phish";
        else
            this.vendor = vendor;
         
        logQuery = "FROM MailLogEventFromReports AS evt" +
            " WHERE evt.addrKind IN ('T', 'C')" +
            " AND evt." + this.vendor + "Action IS NOT NULL" +
            " AND evt.policyId = :policyId" + 
            " ORDER BY evt.timeStamp DESC";

        if (false == vendor.equals("Clam")) {
            repoDesc = SPAM_REPO_DESC;
        } else {
            repoDesc = CLAM_REPO_DESC;
        }
    }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return repoDesc;
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
