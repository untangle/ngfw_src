/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/impl/com/untangle/uvm/engine/UvmContextImpl.java $
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.uvm.message.ActiveStat;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.message.Message;
import com.untangle.uvm.message.MessageQueue;
import com.untangle.uvm.message.StatDescs;
import com.untangle.uvm.message.StatInterval;
import com.untangle.uvm.message.Stats;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

class MessageManagerImpl implements LocalMessageManager
{
    private static final Pattern MEMINFO_PATTERN
        = Pattern.compile("(\\w+):\\s+(\\d+)\\s+kB");

    private static final Pattern VMSTAT_PATTERN
        = Pattern.compile("(\\w+)\\s+(\\d+)");

    private static final Pattern CPUINFO_PATTERN
        = Pattern.compile("([ 0-9a-zA-Z]*\\w)\\s*:\\s*(.*)$");

    private static final Pattern CPU_USAGE_PATTERN
        = Pattern.compile("cpu\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");

    private static final Set<String> MEMINFO_KEEPERS;
    private static final Set<String> VMSTAT_KEEPERS;

    private final Map<Tid, Counters> counters = new HashMap<Tid, Counters>();

    // XXX this needs to be per client session
    private final List<Message> messages = new ArrayList<Message>();

    private final Pulse updatePulse = new Pulse("system-stat-collector",
                                                true, new SystemStatCollector());

    private volatile Map<String, Object> systemStats = Collections.emptyMap();

    private final Logger logger = Logger.getLogger(getClass());

    MessageManagerImpl()
    {
        ensureTid0();
    }

    void start()
    {
        updatePulse.start(10000);
    }

    void stop()
    {
        updatePulse.stop();
    }

    // RemoteMessageManager methods --------------------------------------------

    public MessageQueue getMessageQueue()
    {
        LocalNodeManager lm = UvmContextImpl.getInstance().nodeManager();
        List<Tid> tids = lm.nodeInstances();
        tids.add(new Tid(0L));
        Map<Tid, Stats> stats = getStats(lm, tids);
        List<Message> messages = getMessages();
        return new MessageQueue(messages, stats, systemStats);
    }

    public MessageQueue getMessageQueue(Policy p)
    {
        LocalNodeManager lm = UvmContextImpl.getInstance().nodeManager();
        List<Tid> tids = lm.nodeInstances(p);
        Map<Tid, Stats> stats = getStats(lm, tids);
        List<Message> messages = getMessages();
        return new MessageQueue(messages, stats, systemStats);
    }

    public StatDescs getStatDescs(Tid t)
    {
        Long id = t.getId();
        if (null != id) {
            return getCounters(t).getStatDescs();
        } else {
            return null;
        }
    }

    public Map<String, Object> getSystemStats()
    {
        return this.systemStats;
    }

    public List<ActiveStat> getActiveMetrics(final Tid tid)
    {
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

        return tw.getResult();
    }

    public void setActiveMetrics(final Tid tid,
                                 final List<ActiveStat> activeMetrics)
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
                        bs = new StatSettings(tid, activeMetrics);
                        s.save(bs);
                    } else {
                        bs.setActiveMetrics(activeMetrics);
                        s.merge(bs);
                    }

                    return true;
                }
            };

        UvmContextImpl.getInstance().runTransaction(tw);
    }

    public List<Message> getMessages()
    {
        List<Message> l = new ArrayList<Message>(messages.size());

        synchronized (messages) {
            l.addAll(messages);
            messages.clear();
        }

        return l;
    }

    // LocalMessageManager methods ---------------------------------------------

    public Counters getUvmCounters()
    {
        return getCounters(new Tid(0L));
    }

    public Counters getCounters(Tid t)
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
        synchronized (messages) {
            messages.add(m);
        }
    }

    public void setActiveMetricsIfNotSet(final Tid tid,
                                         final BlingBlinger... blingers)
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

    // private methods ---------------------------------------------------------

    private Map<Tid, Stats> getStats(LocalNodeManager lm, List<Tid> tids)
    {
        Map<Tid, Stats> stats = new HashMap<Tid, Stats>(tids.size());

        for (Tid t : tids) {
            List<ActiveStat> as = getActiveMetrics(t);
            Counters c = getCounters(t);
            stats.put(t, c.getAllStats(as));
        }

        return stats;
    }

    private void ensureTid0()
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from Tid t where t.id = 0");
                    Tid t = (Tid)q.uniqueResult();
                    if (null == t) {
                        t = new Tid(0L);
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

    private class SystemStatCollector implements Runnable
    {
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
                getNumProcs(m);
                getCpuUsage(m);
            } catch (IOException exn) {
                logger.warn("could not get memory information", exn);
            }

            systemStats = Collections.unmodifiableMap(m);
        }

        private void readMeminfo(Map<String, Object> m)
            throws IOException
        {

            readProcFile("/proc/meminfo", MEMINFO_PATTERN, MEMINFO_KEEPERS, m);
        }

        private void readVmstat(Map<String, Object> m)
            throws IOException
        {
            readProcFile("/proc/vmstat", VMSTAT_PATTERN, VMSTAT_KEEPERS, m);
        }

        private void readProcFile(String filename, Pattern p,
                                  Set<String> keepers, Map<String, Object> m)
            throws IOException
        {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            for (String l = br.readLine(); null != l; l = br.readLine()) {
                Matcher matcher = p.matcher(l);
                if (matcher.find()) {
                    String n = matcher.group(1);
                    if (keepers.contains(n)) {
                        String s = matcher.group(2);
                        try {
                            m.put(n, Integer.parseInt(s));
                        } catch (NumberFormatException exn) {
                            logger.warn("could not add value for: " + n);
                        }
                    }
                }
            }
        }

        private void readCpuinfo(Map<String, Object> m)
            throws IOException
        {
            String cpuModel = null;
            float cpuSpeed = 0;
            int numCpus = 0;

            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
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
                            cpuSpeed = Float.parseFloat(v);
                        } catch (NumberFormatException exn) {
                            logger.warn("could not parse cpu speed: " + v);
                        }
                    }
                }
            }

            m.put("cpuModel", cpuModel);
            m.put("cpuSpeed", cpuSpeed);
            m.put("numCpus", numCpus);
        }

        private void readUptime(Map<String, Object> m)
            throws IOException
        {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String l = br.readLine();
            if (null != l) {
                String s = l.split(" ")[0];
                try {
                    m.put("uptime", Float.parseFloat(s));
                } catch (NumberFormatException exn) {
                    logger.warn("could not parse uptime: " + s);
                }
            }
        }

        private void readLoadAverage(Map<String, Object> m)
            throws IOException
        {
            BufferedReader br = new BufferedReader(new FileReader("/proc/loadavg"));
            String l = br.readLine();
            if (null != l) {
                String[] s = l.split(" ");
                if (s.length >= 3) {
                    try {
                        m.put("oneMinuteLoadAvg", Float.parseFloat(s[0]));
                        m.put("fiveMinuteLoadAvg", Float.parseFloat(s[1]));
                        m.put("fifteenMinuteLoadAvg", Float.parseFloat(s[2]));
                    } catch (NumberFormatException exn) {
                        logger.warn("could not parse loadavg: " + l);
                    }
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
            BufferedReader br = new BufferedReader(new FileReader("/proc/loadavg"));
            for (String l = br.readLine(); null != l; l = br.readLine()) {
                Matcher matcher = CPU_USAGE_PATTERN.matcher(l);
                if (matcher.find()) {
                    try {
                        long user1 = Long.parseLong(matcher.group(1));
                        long nice1 = Long.parseLong(matcher.group(2));
                        long system1 = Long.parseLong(matcher.group(3));
                        long idle1 = Long.parseLong(matcher.group(4));

                        long totalTime = (user1 - user0) + (nice1 - nice0)
                            + (system1 - system0) + (idle1 - idle0);

                        if (0 == totalTime) {
                            m.put("userCpuUtilization", (float)0);
                            m.put("systemCpuUtilization", (float)0);
                        } else {
                            m.put("userCpuUtilization",
                                  ((user1 - user0) + (nice1 - nice0))
                                  / (float)totalTime);
                            m.put("systemCpuUtilization",
                                  (system1 - system0) / (float)totalTime);
                        }

                        user0 = user1;
                        nice0 = nice1;
                        system0 = system1;
                        idle0 = idle1;
                    } catch (NumberFormatException exn) {
                        m.put("userCpuUtilization", (float)0);
                        m.put("systemCpuUtilization", (float)0);
                    }

                    break;
                }
            }
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