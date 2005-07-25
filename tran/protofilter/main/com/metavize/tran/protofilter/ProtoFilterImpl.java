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
package com.metavize.tran.protofilter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.Direction;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class ProtoFilterImpl extends AbstractTransform implements ProtoFilter
{
    private final EventHandler handler = new EventHandler( this );
    private static final String EVENT_QUERY
        = "SELECT create_date, protocol, blocked, "
        + "c_client_addr, c_client_port, "
        + "s_server_addr, s_server_port, "
        + "client_intf, server_intf "
        + "FROM pl_endp endp "
        + "JOIN tr_protofilter_evt USING (session_id) "
        + "ORDER BY create_date DESC LIMIT ?";

    private final SoloPipeSpec pipeSpec = new SoloPipeSpec
        ("protofilter", this, handler, Fitting.OCTET_STREAM,
         Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private final Logger logger = Logger.getLogger(ProtoFilterImpl.class);

    private ProtoFilterSettings settings = null;

    // constructors -----------------------------------------------------------

    public ProtoFilterImpl() { }

    // ProtoFilter methods ----------------------------------------------------

    public ProtoFilterSettings getProtoFilterSettings()
    {
        return this.settings;
    }

    public void setProtoFilterSettings(ProtoFilterSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get HttpBlockerSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate sessino", exn);
            }
        }

        try {
            reconfigure();
        }
        catch (TransformException exn) {
            logger.error("Could not save ProtoFilter settings", exn);
        }
    }

    public List<ProtoFilterLog> getLogs(int limit)
    {
        List<ProtoFilterLog> l = new LinkedList<ProtoFilterLog>();

        Session s = TransformContextFactory.context().openSession();
        try {
            Connection c = s.connection();
            PreparedStatement ps = c.prepareStatement(EVENT_QUERY);
            ps.setInt(1, limit);
            long l0 = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long cd = rs.getTimestamp("create_date").getTime();
                Date createDate = new Date(cd);
                String protocol = rs.getString("protocol");
                boolean blocked = rs.getBoolean("blocked");
                String clientAddr = rs.getString("c_client_addr");
                int clientPort = rs.getInt("c_client_port");
                String serverAddr = rs.getString("s_server_addr");
                int serverPort = rs.getInt("s_server_port");
                byte clientIntf = rs.getByte("client_intf");
                byte serverIntf = rs.getByte("server_intf");

                Direction d = Direction.getDirection(clientIntf, serverIntf);

                ProtoFilterLog rl = new ProtoFilterLog
                    (createDate, protocol, blocked, clientAddr, clientPort,
                     serverAddr, serverPort, d);

                l.add(0, rl);
            }
            long l1 = System.currentTimeMillis();
            logger.debug("getAccessLogs() in: " + (l1 - l0));
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

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // Transform methods ------------------------------------------------------

    protected void initializeSettings()
    {
        ProtoFilterSettings settings = new ProtoFilterSettings(this.getTid());
        logger.info("Initializing Settings...");

        updateToCurrent(settings);

        setProtoFilterSettings(settings);
    }

    protected void postInit(String[] args)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from ProtoFilterSettings hbs where hbs.tid = :tid");
            q.setParameter("tid", getTid());
            this.settings = (ProtoFilterSettings)q.uniqueResult();

            updateToCurrent(this.settings);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get HttpBlockerSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }
    }

    protected void preStart() throws TransformStartException
    {
        try {
            reconfigure();
        } catch (Exception e) {
            throw new TransformStartException(e);
        }
    }

    public    void reconfigure() throws TransformException
    {
        ProtoFilterSettings settings = getProtoFilterSettings();
        ArrayList enabledPatternsList = new ArrayList();

        logger.info("Reconfigure()");

        if (settings == null) {
            throw new TransformException("Failed to get ProtoFilter settings: " + settings);
        }

        List curPatterns = settings.getPatterns();
        if (curPatterns == null)
            logger.warn("NULL pattern list. Continuing anyway...");
        else {
            for (Iterator i=curPatterns.iterator() ; i.hasNext() ; ) {
                ProtoFilterPattern pat = (ProtoFilterPattern)i.next();

                if ( pat.getLog() || pat.getAlert() || pat.isBlocked() ) {
                    logger.info("Matching on pattern \"" + pat.getProtocol() + "\"");
                    enabledPatternsList.add(pat);
                }
            }
        }

        handler.patternList(enabledPatternsList);
        handler.byteLimit(settings.getByteLimit());
        handler.chunkLimit(settings.getChunkLimit());
        handler.unknownString(settings.getUnknownString());
        handler.stripZeros(settings.isStripZeros());
    }


    private   void updateToCurrent(ProtoFilterSettings settings)
    {
        if (settings == null) {
            logger.error("NULL ProtoFilter Settings");
            return;
        }

        HashMap    allPatterns = LoadPatterns.getPatterns(); /* Global List of Patterns */
        List       curPatterns = settings.getPatterns(); /* Current list of Patterns */

        if (curPatterns == null) {
            /*
             * First time initialization
             */
            logger.info("UPDATE: Importing patterns...");
            settings.setPatterns(new ArrayList(allPatterns.values()));
            curPatterns = settings.getPatterns();
        }
        else {
            /*
             * Look for updates
             */
            for (Iterator i=curPatterns.iterator() ; i.hasNext() ; ) {
                ProtoFilterPattern pat = (ProtoFilterPattern) i.next();
                String name = pat.getProtocol();
                String def  = pat.getDefinition();

                if (allPatterns.containsKey(name)) {
                    /*
                     * Key is present in current config
                     * Update definition and description if needed
                     */
                    ProtoFilterPattern newpat = (ProtoFilterPattern)allPatterns.get(name);
                    if (newpat == null) {
                        logger.error("Missing pattern");
                        continue;
                    }

                    if (!newpat.getDescription().equals(pat.getDescription())) {
                        logger.info("UPDATE: Updating Description for Pattern (" + name + ")");
                        pat.setDescription(newpat.getDescription());
                    }
                    if (!newpat.getDefinition().equals(pat.getDefinition())) {
                        logger.info("UPDATE: Updating Definition  for Pattern (" + name + ")");
                        pat.setDefinition(newpat.getDefinition());
                    }

                    /*
                     * Remove it, its been accounted for
                     */
                    allPatterns.remove(name);
                }
            }

            /*
             * Add all the necessary new patterns.  Whatever is left
             * in allPatterns at this point, is not in the curPatterns
             */
            for (Iterator i=allPatterns.values().iterator() ; i.hasNext() ; ) {
                ProtoFilterPattern pat = (ProtoFilterPattern) i.next();
                logger.info("UPDATE: Adding New Pattern (" + pat.getProtocol() + ")");
                curPatterns.add(pat);
            }
        }

        logger.info("UPDATE: Complete");
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getProtoFilterSettings();
    }

    public void setSettings(Object settings)
    {
        setProtoFilterSettings((ProtoFilterSettings)settings);
    }
}
