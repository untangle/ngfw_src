package com.metavize.tran.ids;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.Direction;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.util.TransactionWork;
import com.metavize.tran.token.TokenAdaptor;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class IDSTransformImpl extends AbstractTransform implements IDSTransform {

    private static final String EVENT_QUERY_BASE
        = "SELECT create_date, message, blocked, "
        + "c_client_addr, c_client_port, "
        + "s_server_addr, s_server_port, "
        + "policy_inbound AS incoming "
        + "FROM pl_endp endp "
        + "JOIN tr_ids_evt USING (session_id) "
        + "WHERE endp.policy_id = ? ";

    private static final String EVENT_QUERY
        = EVENT_QUERY_BASE
        + "ORDER BY create_date DESC LIMIT ?";

    private static final String EVENT_BLOCKED_QUERY
        = EVENT_QUERY_BASE
        + "AND blocked "
        + "ORDER BY create_date DESC LIMIT ?";

    private static final Logger log = Logger.getLogger(IDSTransformImpl.class);
    static {
        log.setLevel(Level.DEBUG);
    }

    private IDSSettings settings = null;

    private final EventHandler handler;
    private final SoloPipeSpec octetPipeSpec, httpPipeSpec;
    private final PipeSpec[] pipeSpecs;

    private List ruleList = Collections.synchronizedList(new ArrayList());
    private static IDSDetectionEngine engine;

    public IDSTransformImpl() {
        handler = new EventHandler(this);
        octetPipeSpec = new SoloPipeSpec("ids-octet", this, handler,Fitting.OCTET_STREAM, Affinity.SERVER,10);
        httpPipeSpec = new SoloPipeSpec("ids-http", this, new TokenAdaptor(this, new IDSHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.SERVER,0);
        pipeSpecs = new PipeSpec[] { httpPipeSpec, octetPipeSpec };
        engine.setTransform(this);
    }

    protected PipeSpec[] getPipeSpecs() {
        log.debug("Getting PipeSpec");
        return pipeSpecs;
    }

    // backwards compat
    public List<IDSLog> getLogs(int limit) {
        return getLogs(limit, false);
    }

    public List<IDSLog> getLogs(final int limit, final boolean blockedOnly) {
        final List<IDSLog> l = new ArrayList<IDSLog>(limit);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s) throws SQLException
                {
                    Connection c = s.connection();
                    PreparedStatement ps;
                    if (blockedOnly)
                        ps = c.prepareStatement(EVENT_BLOCKED_QUERY);
                    else
                        ps = c.prepareStatement(EVENT_QUERY);
                    ps.setLong(1, getPolicy().getId());
                    ps.setInt(2, limit);
                    long l0 = System.currentTimeMillis();
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        long cd = rs.getTimestamp("create_date").getTime();
                        Date createDate = new Date(cd);
                        String message = rs.getString("message");
                        boolean blocked = rs.getBoolean("blocked");
                        String clientAddr = rs.getString("c_client_addr");
                        int clientPort = rs.getInt("c_client_port");
                        String serverAddr = rs.getString("s_server_addr");
                        int serverPort = rs.getInt("s_server_port");
                        boolean incoming = rs.getBoolean("incoming");

                        Direction d = incoming ? Direction.INCOMING : Direction.OUTGOING;
                        IDSLog rl = new IDSLog(createDate, message, blocked, clientAddr, clientPort, serverAddr, serverPort, d);
                        l.add(rl);
                    }
                    long l1 = System.currentTimeMillis();
                    log.debug("getAccessLogs() in: " + (l1 - l0));

                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        return l;
    }

    public IDSSettings getIDSSettings() {
        return this.settings;
    }

    public void setIDSSettings(final IDSSettings settings) {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    IDSTransformImpl.this.settings = settings;
                    engine.setSettings(settings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
    }

    protected void initializeSettings() {

        log.info("Loading Rules...");
        //IDSSettings settings = new IDSSettings(getTid());
        settings = queryDBForSettings();
        if(settings == null || settings.getRules() == null) {
            settings = new IDSSettings(getTid());
            settings.setVariables(IDSRuleManager.defaultVariables);
            settings.setImmutableVariables(IDSRuleManager.immutableVariables);

            log.info("Settings was null, loading from file");
            String path =  System.getProperty("bunnicula.home");
            File file = new File(path+"/idsrules");
            visitAllFiles(file);

            settings.setMaxChunks(engine.getMaxChunks());
            settings.setRules(ruleList);
            setIDSSettings(settings);
        }
        else
            log.info("Settings was loaded from DB: " + settings);

        //setIDSSettings(settings);
        log.info(ruleList.size() + " rules loaded");
        //}

        IDSStatisticManager.instance().stop();
    }

    /** Temp subroutines for loading local snort rules.
     */
    private void visitAllFiles(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i=0; i<children.length; i++)
                visitAllFiles(new File(file, children[i]));
        }
        else
            processFile(file);
    }

    /** Temp subroutines for loading local snort rules.
     */
    private void processFile(File file) {
        //System.out.println(file);
        IDSRuleManager testManager = new IDSRuleManager();
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                if(testManager.canParse(str.trim())) {
                    IDSRuleSignature sig = testManager.getNewestSignature();
                    String message = (sig == null) ? "The signature failed to load" : sig.getMessage();
                    String category = file.getName().replaceAll(".rules",""); //Should move this to script land
                    category = category.replace("bleeding-",""); //Should move this to script land
                    ruleList.add(new IDSRule(str, category ,message));
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private IDSSettings queryDBForSettings() {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from IDSSettings ids where ids.tid = :tid");
                    q.setParameter("tid", getTid());
                    IDSTransformImpl.this.settings = (IDSSettings)q.uniqueResult();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        return null;
    }

    protected void postInit(String args[]) {
        log.info("Post init");
        settings = queryDBForSettings();
    }

    protected void preStart() throws TransformStartException {
        IDSTest test = new IDSTest();
        if(!test.runTest())
          throw new TransformStartException("IDS Test failed"); // */

        try {
            reconfigure();
        }
        catch (Exception e) {
            throw new TransformStartException(e);
        }

        IDSStatisticManager.instance().start();
    }

    public static IDSDetectionEngine getEngine() {
        if(engine == null)
            engine = new IDSDetectionEngine();
        return engine;
    }

    protected void postStop() {
        IDSStatisticManager.instance().stop();
    }

    public void reconfigure() throws TransformException {

        if(settings == null) {
            settings = queryDBForSettings();

            if(settings == null)
                throw new TransformException("Failed to get IDS settings: " + settings);
        }

        engine.setSettings(settings);
        engine.onReconfigure();
        engine.setMaxChunks(settings.getMaxChunks());
        List<IDSRule> rules = (List<IDSRule>) settings.getRules();
        for(IDSRule rule : rules) {
            engine.updateRule(rule);
        }
        //remove all deleted rules

        setIDSSettings(settings);
    }

    //XXX soon to be deprecated ------------------------------------------

    public Object getSettings() { return getIDSSettings(); }

    public void setSettings(Object obj) { setIDSSettings((IDSSettings)obj); }
}
