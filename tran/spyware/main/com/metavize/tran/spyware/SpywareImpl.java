/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spyware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.Direction;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.IPMaddrRule;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.tran.token.TokenAdaptor;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class SpywareImpl extends AbstractTransform implements Spyware
{
    private static final String HTTP_QUERY
        = "SELECT req.time_stamp, "
        + "CASE WHEN NOT bl.event_id IS NULL THEN 'BLACKLIST' "
        + "     WHEN NOT co.event_id IS NULL THEN 'COOKIE' "
        + "     WHEN NOT ax.event_id IS NULL THEN 'ACTIVEX' "
        + "     ELSE 'NONE' "
        + "END AS type, "
        + "CASE WHEN NOT bl.event_id IS NULL THEN host "
        + "     ELSE 'http://' || host || uri "
        + "END AS location, "
        + "CASE WHEN NOT bl.event_id IS NULL THEN host "
        + "     WHEN NOT co.event_id IS NULL THEN co.ident "
        + "     WHEN NOT ax.event_id IS NULL THEN ax.ident "
        + "     ELSE host, "
        + "END AS ident, "
        + "CASE WHEN NOT bl.event_id IS NULL THEN true "
        + "     WHEN NOT co.event_id IS NULL THEN true "
        + "     WHEN NOT ax.event_id IS NULL THEN true "
        + "     ELSE false "
        + "END AS blocked, "
        + "c_client_addr, c_client_port, s_server_addr, s_server_port, "
        + "client_intf, server_intf "
        + "FROM tr_http_req_line rl "
        + "JOIN tr_http_evt_req req USING (request_id) "
        + "JOIN pl_endp endp USING (session_id) "
        + "LEFT OUTER JOIN tr_spyware_evt_cookie co ON co.request_id = rl.request_id "
        + "LEFT OUTER JOIN tr_spyware_evt_activex ax ON ax.request_id = rl.request_id "
        + "LEFT OUTER JOIN tr_spyware_evt_blacklist bl ON bl.request_id = rl.request_id "
        + "ORDER BY req.time_stamp DESC LIMIT ?";

    private static final String ACCESS_QUERY
        = "SELECT create_date AS time_stamp, "
        +        "'ACCESS' AS type, "
        +        "text(ipmaddr) AS location, "
        +        "ident, "
        +        "blocked, "
        +        "c_client_addr, c_client_port, s_server_addr, s_server_port, "
        +        "client_intf, server_intf "
        + "FROM tr_spyware_evt_access acc "
        + "JOIN pl_endp endp USING (session_id) "
        + "ORDER BY create_date DESC LIMIT ?";


    private static final String[] QUERIES = new String[]
        { HTTP_QUERY, ACCESS_QUERY };

    private static final String ACTIVEX_LIST
        = "com/metavize/tran/spyware/blocklist.reg";
    private static final String COOKIE_LIST
        = "com/metavize/tran/spyware/cookie.txt";
    private static final String SUBNET_LIST
        = "com/metavize/tran/spyware/subnet.txt";
    private static final String URL_LIST
        = "com/metavize/tran/spyware/urlblacklist.txt";

    private static final Pattern ACTIVEX_PATTERN = Pattern
        .compile(".*\\{([a-fA-F0-9\\-]+)\\}.*");

    private final Logger logger = Logger.getLogger(getClass());

    private static final int HTTP = 0;
    private static final int BYTE = 1;

    private final SpywareHttpFactory factory = new SpywareHttpFactory(this);
    private final TokenAdaptor tokenAdaptor = new TokenAdaptor(factory);
    private final SpywareEventHandler streamHandler = new SpywareEventHandler(this);

    private final Set urlBlacklist;

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { new SoloPipeSpec("spyware-http", this, tokenAdaptor,
                           Fitting.HTTP_TOKENS, Affinity.SERVER, 0),
          new SoloPipeSpec("spyware-byte", this, streamHandler,
                           Fitting.OCTET_STREAM, Affinity.SERVER, 0) };

    private volatile SpywareSettings settings;
    private volatile Map<String, StringRule> activeXRules;
    private volatile Map<String, StringRule> cookieRules;

    // constructors -----------------------------------------------------------

    public SpywareImpl()
    {
        urlBlacklist = buildUrlList();
    }

    // SpywareTransform methods -----------------------------------------------

    public SpywareSettings getSpywareSettings()
    {
        return settings;
    }

    public void setSpywareSettings(SpywareSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get SpywareSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate session", exn);
            }
        }

        reconfigure();
    }

    public List<SpywareLog> getEventLogs(int limit)
    {
        List<SpywareLog> l = new ArrayList<SpywareLog>(QUERIES.length * limit);

        for (String q : QUERIES) {
            getEventLogs(q, l, limit);
        }

        Collections.sort(l);

        for (int i = Math.min(limit, l.size()); i < l.size(); i++) {
            l.remove(i);
        }

        return l;
    }

    // Transform methods ------------------------------------------------------

    // XXX avoid

    public void reconfigure()
    {
        logger.info("Reconfigure.");
        if (this.settings.getSpywareEnabled()) {
            streamHandler.subnetList(this.settings.getSubnetRules());
        }

        List<StringRule> l = (List<StringRule>)settings.getActiveXRules();
        if (null != l) {
            Map<String, StringRule> s = new HashMap<String, StringRule>();
            for (StringRule sr : l) {
                s.put(sr.getString(), sr);
            }
            activeXRules = s;
        } else {
            activeXRules = null;
        }

        l = (List<StringRule>)settings.getCookieRules();
        if (null != l) {
            Map<String, StringRule> s = new HashMap<String, StringRule>();
            for (StringRule sr : l) {
                s.put(sr.getString(), sr);
            }
            cookieRules = s;
        } else {
            cookieRules = null;
        }
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    protected void initializeSettings()
    {
        SpywareSettings settings = new SpywareSettings(getTid());
        settings.setActiveXRules(new ArrayList());
        settings.setCookieRules(new ArrayList());
        settings.setSubnetRules(new ArrayList());

        updateActiveX(settings);
        updateCookie(settings);
        updateSubnet(settings);

        setSpywareSettings(settings);
    }

    protected void postInit(String[] args)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from SpywareSettings ss where ss.tid = :tid");
            q.setParameter("tid", getTid());
            this.settings = (SpywareSettings)q.uniqueResult();

            updateActiveX(this.settings);
            updateCookie(this.settings);
            updateSubnet(this.settings);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get SpywareSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        reconfigure();
    }

    // package private methods ------------------------------------------------

    boolean isBlacklistDomain(String domain)
    {
        if (!settings.getUrlBlacklistEnabled()) {
            return false;
        }

        boolean match = false;

        for (String d = domain; !match && null != d; d = nextHost(d)) {
            match = urlBlacklist.contains(domain);
        }

        return match;
    }

    boolean isBlockedCookie(String domain)
    {
        if (null == cookieRules && !settings.getCookieBlockerEnabled()) {
            return false;
        }

        boolean match = false;

        for (String d = domain; !match && null != d; d = nextHost(d)) {
            StringRule sr = cookieRules.get(domain);
            match = null != sr && sr.isLive();
        }

        return match;
    }

    StringRule getBlockedActiveX(String clsId)
    {
        return null == activeXRules ? null : activeXRules.get(clsId);
    }

    // private methods --------------------------------------------------------

    private Set buildUrlList()
    {
        InputStream is = getClass().getClassLoader().getResourceAsStream(URL_LIST);
        HashSet urls = new HashSet();

        if (null == is) {
            logger.error("Could not find: " + URL_LIST);
            return null;
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String line = br.readLine(); null != line; line = br.readLine()) {
                logger.debug("ADDING URL: " + line);
                urls.add(line);
            }
        } catch (IOException exn) {
            logger.error("Could not read file: " + ACTIVEX_LIST, exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("Could not close file: " + ACTIVEX_LIST, exn);
            }
        }

        return urls;
    }

    private void updateActiveX(SpywareSettings settings)
    {
        List rules = settings.getActiveXRules();
        InputStream is = getClass().getClassLoader().getResourceAsStream(ACTIVEX_LIST);

        if (null == is) {
            logger.error("Could not find: " + ACTIVEX_LIST);
            return;
        }

        logger.info("Checking for activeX updates...");

        HashSet ruleHash = new HashSet();
        for (Iterator i=rules.iterator() ; i.hasNext() ; ) {
            StringRule rule = (StringRule) i.next();
            ruleHash.add(rule.getString());
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String l = br.readLine(); null != l; l = br.readLine()) {
                Matcher matcher = ACTIVEX_PATTERN.matcher(l);
                if (matcher.matches()) {
                    String clsid = matcher.group(1);

                    if (!ruleHash.contains(clsid)) {
                        logger.debug("ADDING activeX Rule: " + clsid);
                        rules.add(new StringRule(clsid));
                    }
                }
            }
        } catch (IOException exn) {
            logger.error("Could not read file: " + ACTIVEX_LIST, exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("Could not close file: " + ACTIVEX_LIST, exn);
            }
        }

        return;
    }

    private void updateCookie(SpywareSettings settings)
    {
        List rules = settings.getCookieRules();
        InputStream is = getClass().getClassLoader().getResourceAsStream(COOKIE_LIST);

        if (null == is) {
            logger.error("Could not find: " + COOKIE_LIST);
            return;
        }

        logger.info("Checking for cookie  updates...");

        HashSet ruleHash = new HashSet();
        for (Iterator i=rules.iterator() ; i.hasNext() ; ) {
            StringRule rule = (StringRule) i.next();
            ruleHash.add(rule.getString());
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String l = br.readLine(); null != l; l = br.readLine()) {
                if (!ruleHash.contains(l)) {
                    logger.debug("ADDING cookie Rule: " + l);
                    rules.add(new StringRule(l));
                }
            }
        } catch (IOException exn) {
            logger.error("Could not read file: " + COOKIE_LIST, exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("Could not close file: " + COOKIE_LIST, exn);
            }
        }

        return;
    }

    private void updateSubnet(SpywareSettings settings)
    {
        List rules = settings.getSubnetRules();
        InputStream is = getClass().getClassLoader().getResourceAsStream(SUBNET_LIST);

        if (null == is) {
            logger.warn("Could not find: " + SUBNET_LIST);
            return;
        }

        logger.info("Checking for subnet  updates...");

        HashSet ruleHash = new HashSet();
        for (Iterator i=rules.iterator() ; i.hasNext() ; ) {
            IPMaddrRule rule = (IPMaddrRule) i.next();
            ruleHash.add(rule.getIpMaddr());
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String line = br.readLine(); null != line; line = br.readLine()) {
                StringTokenizer tok = new StringTokenizer(line, ":,");

                String addr = null;
                String description = null;
                String name = null;
                IPMaddr maddr = null;

                try {
                    addr = tok.nextToken();
                    description = tok.nextToken();
                    name = tok.nextToken();
                    maddr = IPMaddr.parse(addr);
                    int i = maddr.maskNumBits(); /* if bad subnet throws exception */
                }
                catch (Exception e) {
                    logger.warn("Invalid Subnet in " + SUBNET_LIST + ": " + line + ": " + e);
                    maddr = null;
                }

                if (maddr != null && !ruleHash.contains(maddr)) {
                    logger.debug("ADDING subnet Rule: " + addr);
                    IPMaddrRule rule = new IPMaddrRule(maddr, name, "[no category]", description);
                    rule.setLog(true);
                    rule.setLive(false);
                    rules.add(rule);
                }
            }
        } catch (IOException exn) {
            logger.error("Could not read file: " + SUBNET_LIST, exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("Could not close file: " + SUBNET_LIST, exn);
            }
        }

        return;
    }

    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }

    private List<SpywareLog> getEventLogs(String q, List<SpywareLog> l,
                                          int limit)
    {

        Session s = TransformContextFactory.context().openSession();
        try {
            Connection c = s.connection();
            PreparedStatement ps = c.prepareStatement(q);
            ps.setInt(1, limit);
            long l0 = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long ts = rs.getTimestamp("time_stamp").getTime();
                Date createDate = new Date(ts);
                String type = rs.getString("type");
                String location = rs.getString("location");
                String ident = rs.getString("ident");
                boolean blocked = rs.getBoolean("blocked");
                String clientAddr = rs.getString("c_client_addr");
                int clientPort = rs.getInt("c_client_port");
                String serverAddr = rs.getString("s_server_addr");
                int serverPort = rs.getInt("s_server_port");
                byte clientIntf = rs.getByte("client_intf");
                byte serverIntf = rs.getByte("server_intf");

                Direction d = Direction.getDirection(clientIntf, serverIntf);

                SpywareLog rl = new SpywareLog
                    (createDate, type, location, ident, blocked, clientAddr,
                     clientPort, serverAddr, serverPort, d);

                l.add(0, rl);
            }
            long l1 = System.currentTimeMillis();
            logger.debug("getActiveXLogs() in: " + (l1 - l0));
        } catch (SQLException exn) {
            logger.warn("could not get events", exn);
        } catch (HibernateException exn) {
            logger.warn("could not get events", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        return l;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getSpywareSettings();
    }

    public void setSettings(Object settings)
    {
        setSpywareSettings((SpywareSettings)settings);
    }
}
