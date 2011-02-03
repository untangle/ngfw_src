/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm/impl/com/untangle/uvm/engine/UvmContextImpl.java $
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

package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.SystemStatEvent;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.message.ActiveStat;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.message.Message;
import com.untangle.uvm.message.MessageQueue;
import com.untangle.uvm.message.StatDescs;
import com.untangle.uvm.message.StatInterval;
import com.untangle.uvm.message.Stats;
import com.untangle.uvm.networking.NetworkConfiguration;
import com.untangle.uvm.networking.InterfaceConfiguration;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.SessionEndpoints;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.util.TransactionWork;

class MessageManagerImpl implements LocalMessageManager
{
    private static final long CLIENT_TIMEOUT = 1800000; // 30 min

    private static final Pattern MEMINFO_PATTERN = Pattern.compile("(\\w+):\\s+(\\d+)\\s+kB");

    private static final Pattern VMSTAT_PATTERN = Pattern.compile("(\\w+)\\s+(\\d+)");

    private static final Pattern CPUINFO_PATTERN = Pattern.compile("([ 0-9a-zA-Z]*\\w)\\s*:\\s*(.*)$");

    private static final Pattern CPU_USAGE_PATTERN = Pattern.compile("cpu\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");

    private static final Pattern NET_DEV_PATTERN = Pattern.compile("^\\s*([a-z]+\\d+):\\s*(\\d+)\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+(\\d+)");

    private static final Pattern DISK_STATS_PATTERN = Pattern.compile("\\s*\\d+\\s+\\d+\\s+[hs]d[a-zA-Z]+\\d+\\s+(\\d+)\\s+\\d+\\s+(\\d+)");

    private static final Set<String> MEMINFO_KEEPERS;
    private static final Set<String> VMSTAT_KEEPERS;

    private final Map<NodeId, Counters> counters = new HashMap<NodeId, Counters>();

    private final Random random = new Random();

    private final Map<Integer, List<Message>> messages = new HashMap<Integer, List<Message>>();

    private final Map<Integer, Long> lastMessageAccess = new HashMap<Integer, Long>();

    private final Map<NodeId, List<ActiveStat>> activeMetrics = new HashMap<NodeId, List<ActiveStat>>();

    private final Pulse updatePulse = new Pulse("system-stat-collector", true, new SystemStatCollector());

    private final Map<String,Long> rxtxBytes0 = new HashMap<String,Long>();

    private long lastNetDevUpdate = System.currentTimeMillis();

    private final Map<String,Long> diskRW0 = new HashMap<String,Long>();

    private long lastDiskUpdate = System.currentTimeMillis();

    private volatile Map<String, Object> systemStats = Collections.emptyMap();

    private EventLogger<LogEvent> eventLogger = EventLoggerFactory.factory().getEventLogger();

    private final Logger logger = Logger.getLogger(getClass());

    MessageManagerImpl()
    {
        ensureNodeId0();
    }

    void start()
    {
        updatePulse.start(10000);
    }

    void stop()
    {
        updatePulse.stop();
    }

    // RemoteMessageManager methods -------------------------------------------

    public MessageQueue getMessageQueue()
    {
        return getMessageQueue(null);
    }

    public MessageQueue getMessageQueue(Integer key)
    {
        NodeManager lm = UvmContextImpl.getInstance().nodeManager();
        List<NodeId> tids = lm.nodeInstances();
        tids.add(new NodeId(0L));
        Map<NodeId, Stats> stats = getStats(lm, tids);
        List<Message> messages = getMessages(key);
        return new MessageQueue(messages, stats, systemStats);
    }

    public MessageQueue getMessageQueue(Integer key, Policy p)
    {
        NodeManager lm = UvmContextImpl.getInstance().nodeManager();
        List<NodeId> tids = lm.nodeInstances(p);
        /* Add in the nodes with the null policy */
        tids.addAll( lm.nodeInstances((Policy)null));
        Map<NodeId, Stats> stats = getStats(lm, tids);
        List<Message> messages = getMessages(key);

        return new MessageQueue(messages, stats, systemStats);
    }

    public StatDescs getStatDescs(NodeId t)
    {
        Long id = t.getId();
        if (null != id) {
            StatDescs sd = getCounters(t).getStatDescs();
            return sd;
        } else {
            return null;
        }
    }

    public Map<String, Object> getSystemStats()
    {
        return this.systemStats;
    }

    public List<ActiveStat> getActiveMetrics(final NodeId tid)
    {
        List<ActiveStat> l = null;

        synchronized (activeMetrics) {
            l = activeMetrics.get(tid);
        }

        if (null == l) {
            TransactionWork<List<ActiveStat>> tw = new TransactionWork<List<ActiveStat>>()
                {
                    private List<ActiveStat> result;

                    public boolean doWork(Session s)
                    {
                        Query q = s.createQuery
                            ("from StatSettings bs where bs.tid = :tid");
                        q.setParameter("tid", tid);
                        StatSettings bs = (StatSettings)q.uniqueResult();
                        if (null == bs) {
                            result = null;
                        } else {
                            result = bs.getActiveMetrics();
                        }

                        return true;
                    }

                    @Override
                    public List<ActiveStat> getResult()
                    {
                        return result;
                    }
                };
            UvmContextImpl.getInstance().runTransaction(tw);
            l = tw.getResult();
            synchronized (activeMetrics) {
                activeMetrics.put(tid, l);
            }
        }

        return l;
    }

    public void setActiveMetrics(final NodeId tid, final List<ActiveStat> activeMetrics)
    {
        synchronized (this.activeMetrics) {
            this.activeMetrics.put(tid, activeMetrics);
        }

        TransactionWork<List<ActiveStat>> tw = new TransactionWork<List<ActiveStat>>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                    ("from StatSettings bs where bs.tid = :tid");
                    q.setParameter("tid", tid);
                    StatSettings bs = (StatSettings)q.uniqueResult();
                    if (null == bs) {
                        bs = new StatSettings(tid, activeMetrics);
                        s.save(bs);
                    } else {
                        List<ActiveStat> l = bs.getActiveMetrics();
                        if (null != bs ) {
                            l.clear();
                            l.addAll(activeMetrics);
                        }

                        s.merge(bs);
                    }

                    return true;
                }
            };

        UvmContextImpl.getInstance().runTransaction(tw);
    }

    public Integer getMessageKey()
    {
        int key;

        synchronized (messages) {
            do {
                key = random.nextInt();
            } while (messages.keySet().contains(key));

            messages.put(key, new ArrayList<Message>());
            lastMessageAccess.put(key, System.currentTimeMillis());
        }

        return key;
    }

    // LocalMessageManager methods --------------------------------------------

    public Counters getUvmCounters()
    {
        return getCounters(new NodeId(0L));
    }

    public Counters getCounters(NodeId t)
    {
        Counters c;
        synchronized (counters) {
            c = counters.get(t);
            if (null == c) {
                c = new Counters(t);
                counters.put(t, c);
            }
        }

        return c;
    }

    public void submitMessage(Message m)
    {
        long now = System.currentTimeMillis();

        List<Integer> removals = new ArrayList<Integer>(messages.keySet().size());

        synchronized (messages) {
            for (Integer k : messages.keySet()) {
                Long d = lastMessageAccess.get(k);
                if (null == d) {
                    removals.add(k);
                } else if (now - d > CLIENT_TIMEOUT) {
                    removals.add(k);
                } else {
                    List<Message> l = messages.get(k);
                    if (null == l) {
                        l = new ArrayList<Message>();
                        messages.put(k, l);
                    }

                    l.add(m);
                }
            }

            for (Integer k : removals) {
                messages.remove(k);
                lastMessageAccess.remove(k);
            }
        }
    }

    public void setActiveMetricsIfNotSet(final NodeId tid, final BlingBlinger... blingers)
    {
        TransactionWork<List<ActiveStat>> tw = new TransactionWork<List<ActiveStat>>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                    ("from StatSettings bs where bs.tid = :tid");
                    q.setParameter("tid", tid);
                    StatSettings bs = (StatSettings)q.uniqueResult();
                    if (null == bs) {
                        List<ActiveStat> l = getActiveStats(blingers);
                        bs = new StatSettings(tid, l);
                        s.save(bs);
                    } else {
                        List<ActiveStat> as = bs.getActiveMetrics();
                        if (null == as) {
                            as = new ArrayList<ActiveStat>();
                            bs.setActiveMetrics(as);
                        }

                        if (0 == as.size()) {
                            as.addAll(getActiveStats(blingers));
                            s.merge(bs);
                        }
                    }

                    return true;
                }
            };

        UvmContextImpl.getInstance().runTransaction(tw);
    }

    public Stats getStats(NodeId t)
    {
        List<ActiveStat> as = getActiveMetrics(t);
        Counters c = getCounters(t);

        return c.getAllStats(as);
    }

    public Stats getAllStats(NodeId t)
    {
        Counters c = getCounters(t);
        return c.getAllStats();
    }

    public List<Message> getMessages(Integer key)
    {
        List<Message> l = new ArrayList<Message>();

        synchronized (messages) {
            List<Message> m = messages.get(key);
            if (null != m) {
                l.addAll(m);
                m.clear();
                lastMessageAccess.put(key, System.currentTimeMillis());
            }
        }

        return l;
    }

    public List<Message> getMessages()
    {
        return getMessages(null);
    }

    // private methods --------------------------------------------------------

    private Map<NodeId, Stats> getStats(NodeManager lm, List<NodeId> tids)
    {
        Map<NodeId, Stats> stats = new HashMap<NodeId, Stats>(tids.size());

        for (NodeId t : tids) {
            List<ActiveStat> as = getActiveMetrics(t);
            Counters c = getCounters(t);
            stats.put(t, c.getAllStats(as));
        }

        return stats;
    }

    private void ensureNodeId0()
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from NodeId t where t.id = 0");
                    NodeId t = (NodeId)q.uniqueResult();
                    if (null == t) {
                        t = new NodeId(0L);
                        s.save(t);
                    }

                    return true;
                }
            };

        UvmContextImpl.getInstance().runTransaction(tw);
    }

    private List<ActiveStat> getActiveStats(BlingBlinger[] blingers)
    {
        List<ActiveStat> l = new ArrayList<ActiveStat>();

        for (BlingBlinger b : blingers) {
            l.add(new ActiveStat(b.getStatDesc().getName(),
                                 StatInterval.SINCE_MIDNIGHT));
        }

        return l;
    }

    // private classes --------------------------------------------------------

    private class SystemStatCollector implements Runnable
    {
	private int LOG_DELAY = 60; // in seconds
	private long ONE_BILLION = 1000000000l;
	private long timeStamp = 0;

        private long user0 = 0;
        private long nice0 = 0;
        private long system0 = 0;
        private long idle0 = 0;

        public void run()
        {
            Map<String, Object> m = new HashMap<String, Object>();

            try {
                readMeminfo(m);
                readVmstat(m);
                readCpuinfo(m);
                readLoadAverage(m);
                readUptime(m);
                getNumProcs(m);
                getCpuUsage(m);
                getNetDevUsage(m);
                getDiskUsage(m);
            } catch (IOException exn) {
                logger.warn("could not get memory information", exn);
            }

	    long time = System.nanoTime();
	    if ((time - timeStamp)/ONE_BILLION >= LOG_DELAY) {
		SystemStatEvent sse = new SystemStatEvent();
		// FIXME: wrap the following in public method in SystemStatEvent
		sse.setMemFree(Long.parseLong(m.get("MemFree").toString()));
		sse.setMemCache(Long.parseLong(m.get("Cached").toString()));
		sse.setMemCache(Long.parseLong(m.get("Buffers").toString()));
		sse.setLoad1(Float.parseFloat(m.get("oneMinuteLoadAvg").toString()));
		sse.setLoad5(Float.parseFloat(m.get("fiveMinuteLoadAvg").toString()));
		sse.setLoad15(Float.parseFloat(m.get("fifteenMinuteLoadAvg").toString()));
		sse.setCpuUser(Float.parseFloat(m.get("userCpuUtilization").toString()));
		sse.setCpuSystem(Float.parseFloat(m.get("systemCpuUtilization").toString()));
		sse.setDiskTotal(Long.parseLong(m.get("totalDiskSpace").toString()));
		sse.setDiskFree(Long.parseLong(m.get("freeDiskSpace").toString()));
                sse.setSwapFree(Long.parseLong(m.get("SwapFree").toString()));
                sse.setSwapTotal(Long.parseLong(m.get("SwapTotal").toString()));
		logger.debug("Logging SystemStatEvent");
		eventLogger.log(sse);
		timeStamp = time;
	    }

            systemStats = Collections.unmodifiableMap(m);
        }

        private void readMeminfo(Map<String, Object> m)
            throws IOException
        {

            readProcFile("/proc/meminfo", MEMINFO_PATTERN, MEMINFO_KEEPERS, m,
                         1024);

            Long memFree = (Long)m.get("MemFree");
            if (null == memFree) {
                memFree = 0L;
            }

            Long i = (Long)m.get("Cached");
            if (null != i) {
                memFree += i;
            }
	    //            m.remove("Cached");

            i = (Long)m.get("Buffers");
            if (null != i) {
                memFree += i;
            }
	    //            m.remove("Buffers");

            m.put("MemFree", memFree);
        }

        private void readVmstat(Map<String, Object> m)
            throws IOException
        {
            readProcFile("/proc/vmstat", VMSTAT_PATTERN, VMSTAT_KEEPERS, m, 1);
        }

        private void readProcFile(String filename, Pattern p,
                                  Set<String> keepers, Map<String, Object> m,
                                  int multiple)
            throws IOException
        {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(filename));
                for (String l = br.readLine(); null != l; l = br.readLine()) {
                    Matcher matcher = p.matcher(l);
                    if (matcher.find()) {
                        String n = matcher.group(1);
                        if (keepers.contains(n)) {
                            String s = matcher.group(2);
                            try {
                                m.put(n, Long.parseLong(s) * multiple);
                            } catch (NumberFormatException exn) {
                                logger.warn("could not add value for: " + n);
                            }
                        }
                    }
                }
            } finally {
                if (null != br) {
                    br.close();
                }
            }
        }

        private void readCpuinfo(Map<String, Object> m)
            throws IOException
        {
            String cpuModel = null;
            double cpuSpeed = 0;
            int numCpus = 0;

            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/proc/cpuinfo"));
                for (String l = br.readLine(); null != l; l = br.readLine()) {
                    Matcher matcher = CPUINFO_PATTERN.matcher(l);
                    if (matcher.find()) {
                        String n = matcher.group(1);
                        if (n.equals("model name")) {
                            cpuModel = matcher.group(2);
                        } else if (n.equals("processor")) {
                            numCpus++;
                        } else if (n.equals("cpu MHz")) {
                            String v = matcher.group(2);
                            try {
                                cpuSpeed = Double.parseDouble(v);
                            } catch (NumberFormatException exn) {
                                logger.warn("could not parse cpu speed: " + v);
                            }
                        }
                    }
                }
            } finally {
                if (null != br) {
                    br.close();
                }
            }

            m.put("cpuModel", cpuModel);
            m.put("cpuSpeed", cpuSpeed);
            m.put("numCpus", numCpus);
        }

        private void readUptime(Map<String, Object> m)
            throws IOException
        {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/proc/uptime"));

                String l = br.readLine();
                if (null != l) {
                    String s = l.split(" ")[0];
                    try {
                        m.put("uptime", Double.parseDouble(s));
                    } catch (NumberFormatException exn) {
                        logger.warn("could not parse uptime: " + s);
                    }
                }
            } finally {
                if (null != br) {
                    br.close();
                }
            }
        }

        private void readLoadAverage(Map<String, Object> m)
            throws IOException
        {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/proc/loadavg"));
                String l = br.readLine();
                if (null != l) {
                    String[] s = l.split(" ");
                    if (s.length >= 3) {
                        try {
                            m.put("oneMinuteLoadAvg", Double.parseDouble(s[0]));
                            m.put("fiveMinuteLoadAvg", Double.parseDouble(s[1]));
                            m.put("fifteenMinuteLoadAvg", Double.parseDouble(s[2]));
                        } catch (NumberFormatException exn) {
                            logger.warn("could not parse loadavg: " + l);
                        }
                    }
                }
            } finally {
                if (null != br) {
                    br.close();
                }
            }
        }

        private void getNumProcs(Map<String, Object> m)
        {
            int numProcs = 0;

            File dir = new File("/proc");
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    try {
                        Integer.parseInt(f.getName());
                        numProcs++;
                    } catch (NumberFormatException exn) { }
                }
            }

            m.put("numProcs", numProcs);
        }

        private void getCpuUsage(Map<String, Object> m)
            throws IOException
        {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/proc/stat"));
                for (String l = br.readLine(); null != l; l = br.readLine()) {
                    Matcher matcher = CPU_USAGE_PATTERN.matcher(l);
                    if (matcher.find()) {
                        try {
                            long user1 = Long.parseLong(matcher.group(1));
                            user1 = incrementCount(user0, user1);
                            long nice1 = Long.parseLong(matcher.group(2));
                            nice1 = incrementCount(nice0, nice1);
                            long system1 = Long.parseLong(matcher.group(3));
                            system1 = incrementCount(system0, system1);
                            long idle1 = Long.parseLong(matcher.group(4));
                            idle1 = incrementCount(idle0, idle1);

                            long totalTime = (user1 - user0) + (nice1 - nice0)
                                + (system1 - system0) + (idle1 - idle0);

                            if (0 == totalTime) {
                                m.put("userCpuUtilization", (double)0);
                                m.put("systemCpuUtilization", (double)0);
                            } else {
                                m.put("userCpuUtilization",
                                      ((user1 - user0) + (nice1 - nice0))
                                      / (double)totalTime);
                                m.put("systemCpuUtilization",
                                      (system1 - system0) / (double)totalTime);
                            }

                            user0 = user1;
                            nice0 = nice1;
                            system0 = system1;
                            idle0 = idle1;
                        } catch (NumberFormatException exn) {
                            m.put("userCpuUtilization", 0.0);
                            m.put("systemCpuUtilization", 0.0);
                        }

                        break;
                    }
                }
            } finally {
                if (null != br) {
                    br.close();
                }
            }
        }

        private synchronized void getNetDevUsage(Map<String, Object> m) throws IOException
        {
            long rxBytes0 = 0, rxBytes1 = 0;
            long txBytes0 = 0, txBytes1 = 0;

            ArgonManager am = UvmContextImpl.getInstance().argonManager();

            m.put("uvmSessions",am.getSessionCount());
            m.put("uvmTCPSessions",am.getSessionCount(SessionEndpoints.PROTO_TCP));
            m.put("uvmUDPSessions",am.getSessionCount(SessionEndpoints.PROTO_UDP));
            
            long currentTime = System.currentTimeMillis();

            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/proc/net/dev"));
                Integer i = new Integer(0);

                for (String l = br.readLine(); null != l; l = br.readLine(), i += 1) {
                    Matcher matcher = NET_DEV_PATTERN.matcher(l);
                    if (matcher.find()) {
                        String iface = matcher.group(1);
                        
                        NetworkConfiguration netConf = LocalUvmContextFactory.context().networkManager().getNetworkConfiguration();
                        if (netConf == null) {
                            logger.warn("Failed to read network configuration");
                            continue;
                        } else {
                            InterfaceConfiguration intfConf = netConf.findBySystemName(iface);
                            // Restrict to only the WAN interfaces (bug 5616)
                            if ( intfConf == null || !intfConf.isWAN() )
                                continue;
                        }

                        // get stored previous values or initialize them to 0
                        Long rxbytes0 = rxtxBytes0.get("rx"+i);
                        if (rxbytes0 == null) rxbytes0 = 0L;
                        Long txbytes0 = rxtxBytes0.get("tx"+i);
                        if (txbytes0 == null) txbytes0 = 0L;

                        try {
                            // accumulate previous values
                            rxBytes0 += rxbytes0;
                            txBytes0 += txbytes0;
                            // parse new incoming values w/64-bit correction for 32-bit rollover
                            long rxbytes1 = incrementCount(rxbytes0.longValue(), Long.parseLong(matcher.group(2)));
                            long txbytes1 = incrementCount(txbytes0.longValue(), Long.parseLong(matcher.group(3)));
                            // accumulate 64-bit corrected values
                            rxBytes1 += rxbytes1;
                            txBytes1 += txbytes1;
                            // update stored previous values w/new 64 corrected values
                            rxtxBytes0.put("rx"+i, rxbytes1);
                            rxtxBytes0.put("tx"+i, txbytes1);
                        } catch (NumberFormatException exn) {
                            logger.warn("could not add interface info for: "
                                        + iface, exn);
                        }
                    }
                }
            } finally {
                if (null != br) {
                    br.close();
                }
            }

            double dt = (currentTime - lastNetDevUpdate) / 1000.0;
            if (Math.abs(dt) < 5.0e-5) {
                m.put("rxBps", 0.0);
                m.put("txBps", 0.0);
            } else {
                m.put("rxBps", (rxBytes1 - rxBytes0) / dt);
                m.put("txBps", (txBytes1 - txBytes0) / dt);
            }
            lastNetDevUpdate = currentTime;
        }

        private synchronized void getDiskUsage(Map<String, Object> m)
            throws IOException
        {
            File root = new File("/");
            m.put("totalDiskSpace", root.getTotalSpace());
            m.put("freeDiskSpace", root.getFreeSpace());

            long diskReads0 = 0, diskReads1 = 0;
            long diskWrites0 = 0, diskWrites1 = 0;

            long currentTime = System.currentTimeMillis();

            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/proc/diskstats"));
                Integer i = new Integer(0);
                for (String l = br.readLine(); null != l; l = br.readLine(), i += 1) {
                    Matcher matcher = DISK_STATS_PATTERN.matcher(l);
                    if (matcher.find()) {
                        Long diskreads0 = diskRW0.get("dr"+i);
                        if (diskreads0 == null) diskreads0 = 0L;
                        Long diskwrites0 = diskRW0.get("dw"+i);
                        if (diskwrites0 == null) diskwrites0 = 0L;

                        try {
                            // accumulate previous values
                            diskReads0 += diskreads0;
                            diskWrites0 += diskwrites0;
                            // parse new incoming values w/64-bit correction for 32-bit rollover
                            long diskreads1 = incrementCount(diskreads0.longValue(), Long.parseLong(matcher.group(1)));
                            long diskwrites1 = incrementCount(diskwrites0.longValue(), Long.parseLong(matcher.group(2)));
                            // accumulate 64-bit corrected values
                            diskReads1 += diskreads1;
                            diskWrites1 += diskwrites1;
                            // update stored previous values w/new 64 corrected values
                            diskRW0.put("dr"+i, diskreads1);
                            diskRW0.put("dw"+i, diskwrites1);
                        } catch (NumberFormatException exn) {
                            logger.warn("could not get disk data", exn);
                        }
                    }
                }
            } finally {
                if (null != br) {
                    br.close();
                }
            }

            m.put("diskReads", diskReads1);
            m.put("diskWrites", diskWrites1);

            double dt = (currentTime - lastDiskUpdate) / 1000.0;
            if (Math.abs(dt) < 5.0e-5) {
                m.put("diskReadsPerSecond", 0.0);
                m.put("diskWritesPerSecond", 0.0);
            } else {
                m.put("diskReadsPerSecond", (diskReads1 - diskReads0) / dt);
                m.put("diskWritesPerSecond", (diskWrites1 - diskWrites0) / dt);
            }
            lastDiskUpdate = currentTime;
        }

        private long incrementCount(long previousCount, long kernelCount)
        {
            /* If the kernel is counting in 64-bits, just return the
             * kernel count */
            if (kernelCount >= (1L << 32)) return kernelCount;

            long previousKernelCount = previousCount & 0xFFFFFFFFL;
            if (previousKernelCount > kernelCount) previousCount += (1L << 32);

            return ((previousCount & 0x7FFFFFFF00000000L) + kernelCount);
        }
    }

    static {
        Set<String> s = new HashSet<String>();
        s.add("MemTotal");
        s.add("MemFree");
        s.add("Cached");
        s.add("Buffers");
        s.add("Active");
        s.add("Inactive");
        s.add("SwapTotal");
        s.add("SwapFree");
        MEMINFO_KEEPERS = Collections.unmodifiableSet(s);

        s = new HashSet<String>();
        s.add("pgpgin");
        s.add("pgpgout");
        s.add("pgfault");
        VMSTAT_KEEPERS = Collections.unmodifiableSet(s);
    }
}
