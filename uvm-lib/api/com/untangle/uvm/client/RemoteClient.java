/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.StringBuilder;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.RemoteNodeManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.policy.PolicyException;
import com.untangle.uvm.security.AdminSettings;
import com.untangle.uvm.security.LoginSession;
import com.untangle.uvm.security.RegistrationInfo;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.security.User;
import com.untangle.uvm.security.UvmPrincipal;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.toolbox.RemoteToolboxManager;
import com.untangle.uvm.util.SessionUtil;
import com.untangle.uvm.vnet.IPSessionDesc;
import com.untangle.uvm.vnet.SessionDesc;
import com.untangle.uvm.vnet.SessionStats;
import com.untangle.uvm.vnet.TCPSessionDesc;
import com.untangle.uvm.vnet.UDPSessionDesc;
import org.apache.log4j.helpers.AbsoluteTimeDateFormat;

/**
 * A simple command line interface to the Untangle VM.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class RemoteClient
{
    private static String host = "localhost";
    private static String username = "admin";
    private static String passwd = "passwd";
    private static String policyName = null;
    private static Policy policy = null;

    private static boolean verbose = false;
    private static int timeout = 120000;

    private static RemoteUvmContext mc;

    private static RemoteToolboxManager tool;
    private static RemoteNodeManager tm;

    public static void main(String[] args) throws Exception
    {
        List pass = new java.util.LinkedList();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h")) {
                host = args[++i];
            } else if (args[i].equals("-u")) {
                username = args[++i];
            } else if (args[i].equals("-w")) {
                passwd = args[++i];
            } else if (args[i].equals("-v")) {
                verbose = true;
            } else if (args[i].equals("-t")) {
                timeout = 1000 * Integer.parseInt(args[++i]);
            } else if (args[i].equals("-p")) {
                policyName = args[++i];
            } else {
                pass.add(args[i]);
            }
        }
        args = (String[])pass.toArray(new String[0]);

        if (0 == args.length) {
            printUsage();
            System.exit(-1);
        }

        RemoteUvmContextFactory factory = RemoteUvmContextFactory.factory();
        if (host.equals("localhost")) {
            mc = factory.systemLogin(timeout);
        } else {
            mc = factory.interactiveLogin(host, username, passwd, timeout,
                                          null, true, false);
        }

        if (args[0].equalsIgnoreCase("serverStats")) {
            System.out.println("Running");
            factory.logout();
            System.exit(0);
        }

        if (null != policyName) {
            policy = mc.policyManager().getPolicy(policyName);
        }

        tool = mc.toolboxManager();
        tm = mc.nodeManager();

        if (args[0].equalsIgnoreCase("install")) {
            install(args[1]);
        } else if (args[0].equalsIgnoreCase("installAndInstantiate")) {
            installAndInstantiate(args[1]);
        } else if (args[0].equalsIgnoreCase("uninstall")) {
            uninstall(args[1]);
        } else if (args[0].equalsIgnoreCase("update")) {
            update();
        } else if (args[0].equalsIgnoreCase("upgrade")) {
            upgrade();
        } else if (args[0].equalsIgnoreCase("extraName")) {
            extraName(args[1], args[2]);
        } else if (args[0].equalsIgnoreCase("requestInstall")) {
            requestInstall(args[1]);
        } else if (args[0].equalsIgnoreCase("available")) {
            available();
        } else if (args[0].equalsIgnoreCase("installed")) {
            installed();
        } else if (args[0].equalsIgnoreCase("uninstalled")) {
            uninstalled();
        } else if (args[0].equalsIgnoreCase("upgradable")) {
            upgradable();
        } else if (args[0].equalsIgnoreCase("uptodate")) {
            upToDate();
        } else if (args[0].equalsIgnoreCase("instantiate")) {
            String[] initArgs = new String[args.length - 2];
            System.arraycopy(args, 2, initArgs, 0, initArgs.length);
            instantiate(args[1], initArgs);
        } else if (args[0].equalsIgnoreCase("loadt")) {
            String[] initArgs = new String[args.length - 2];
            System.arraycopy(args, 2, initArgs, 0, initArgs.length);
            loadt(args[1], initArgs);
        } else if (args[0].equalsIgnoreCase("reloadt")) {
            reloadt(args[1]);
        } else if (args[0].equalsIgnoreCase("unloadt")) {
            unloadt(args[1]);
        } else if (args[0].equalsIgnoreCase("start")) {
            start(args[1]);
        } else if (args[0].equalsIgnoreCase("stop")) {
            stop(args[1]);
        } else if (args[0].equalsIgnoreCase("destroy")) {
            destroy(args[1]);
        } else if (args[0].equalsIgnoreCase("neverStarted")) {
            neverStarted(args[1]);
        } else if (args[0].equalsIgnoreCase("instances")) {
            instances();
        } else if (args[0].equalsIgnoreCase("sessions")) {
            if (1 == args.length) {
                sessions();
            } else {
                sessions(args[1]);
            }
        } else if (args[0].equalsIgnoreCase("who")) {
            who();
        } else if (args[0].equalsIgnoreCase("getRegInfo")) {
            getRegInfo();
        } else if (args[0].equalsIgnoreCase("dumpSessions")) {
            dumpSessions();
        } else if (args[0].equalsIgnoreCase("register")) {
            register(args[1]);
        } else if (args[0].equalsIgnoreCase("unregister")) {
            unregister(args[1]);
        } else if (args[0].equalsIgnoreCase("shutdown")) {
            shutdown();
        } else if (args[0].equalsIgnoreCase("isReportingEnabled")) {
            isReportingEnabled();
        } else if (args[0].equalsIgnoreCase("areReportsAvailable")) {
            areReportsAvailable();
        } else if (args[0].equalsIgnoreCase("startReports")) {
            startReports();
        } else if (args[0].equalsIgnoreCase("stopReports")) {
            stopReports();
        } else if (args[0].equalsIgnoreCase("isReportingRunning")) {
            isReportingRunning();
        } else if (args[0].equalsIgnoreCase("prepareReports")) {
            String[] prepArgs = new String[args.length - 1];
            System.arraycopy(args, 1, prepArgs, 0, prepArgs.length);
            prepareReports(prepArgs);
        } else if (args[0].equalsIgnoreCase("resetLogs")) {
            resetLogs();
        } else if (args[0].equalsIgnoreCase("logError")) {
            if (1 == args.length) {
                logError(null);
            } else {
                String[] pwArgs = new String[args.length - 1];
                System.arraycopy(args, 1, pwArgs, 0, pwArgs.length);
                logError(pwArgs);
            }
        } else if (args[0].equalsIgnoreCase("updateAddress")) {
            updateAddress();
        } else if (args[0].equalsIgnoreCase("gc")) {
            doFullGC();
        } else if (args[0].equalsIgnoreCase("loadRup")) {
            loadRup();
        } else if (args[0].equalsIgnoreCase("setProperty")) {
            setProperty(args[1], args[2]);
        } else if (args[0].equalsIgnoreCase("addPolicy")) {
            addPolicy(args[1], 3 > args.length ? null : args[2]);
        } else if (args[0].equalsIgnoreCase("listPolicies")) {
            listPolicies();
        } else if (args[0].equalsIgnoreCase("aptTail")) {
            doAptTail();
        } else if (args[0].equalsIgnoreCase("passwd")) {
            String[] pwArgs = new String[args.length - 1];
            System.arraycopy(args, 1, pwArgs, 0, pwArgs.length);
            doPasswd(pwArgs);
        } else if (args[0].equalsIgnoreCase("reloadLicenses")) {
            doReloadLicenses();
        } else if (args[0].equalsIgnoreCase("restartCliServer")) {
            doRestartCliServer();
        } else if (args[0].equalsIgnoreCase("stopCliServer")) {
            doStopCliServer();
        } else {
            System.out.print("dont know: ");
            for (int i = 0; i < args.length; i++) {
                System.out.print(args[i] + " ");
            }
            System.out.println();
            printUsage();
            factory.logout();
            System.exit(-1);
        }

        factory.logout();
    }

    private static void install(String mackageName)
        throws Exception
    {
        tool.install(mackageName);

        doAptTail();
    }

    private static void installAndInstantiate(String mackageName)
        throws Exception
    {
        Policy p = mc.policyManager().getDefaultPolicy();
        tool.installAndInstantiate(mackageName, p);
        doAptTail();
    }

    private static void uninstall(String mackageName)
        throws Exception
    {
        tool.uninstall(mackageName);
    }

    private static void update()
        throws Exception
    {
        tool.update();
    }

    private static void upgrade()
        throws Exception
    {
        tool.upgrade();

        doAptTail();
    }

    private static void extraName(String mackage, String extraName)
    {
        tool.extraName(mackage, extraName);
    }

    private static void requestInstall(String mackage)
    {
        tool.requestInstall(mackage);
    }

    private static void available()
    {
        MackageDesc[] mkgs = tool.available();
        for (int i = 0; i < mkgs.length; i++) {
            System.out.println(pad(mkgs[i].getName())
                               + "extraName: " + mkgs[i].getExtraName()
                               + "\tinstalled: " + mkgs[i].getInstalledVersion()
                               + "\tavailable: "
                               + mkgs[i].getAvailableVersion()
                               + "\twebpage: " + mkgs[i].getWebsite());
        }
    }

    private static void installed()
    {
        MackageDesc[] mkgs = tool.installed();
        for (int i = 0; i < mkgs.length; i++) {
            System.out.println(pad(mkgs[i].getName())
                               + "extraName: " + mkgs[i].getExtraName()
                               + "\tinstalled: " + mkgs[i].getInstalledVersion()
                               + "\tavailable: "
                               + mkgs[i].getAvailableVersion());
        }
    }

    private static void uninstalled()
    {
        MackageDesc[] mkgs = tool.uninstalled();
        for (int i = 0; i < mkgs.length; i++) {
            System.out.println(pad(mkgs[i].getName())
                               + "extraName: " + mkgs[i].getExtraName()
                               + "\tinstalled: " + mkgs[i].getInstalledVersion()
                               + "\tavailable: " + mkgs[i].getAvailableVersion()
                               + "\ttype: " + mkgs[i].getType());
        }
    }

    private static void upgradable()
    {
        MackageDesc[] mkgs = tool.upgradable();
        for (int i = 0; i < mkgs.length; i++) {
            System.out.println(pad(mkgs[i].getName())
                               + "extraName: " + mkgs[i].getExtraName()
                               + "\tinstalled: " + mkgs[i].getInstalledVersion()
                               + "\tavailable: " + mkgs[i].getAvailableVersion());
        }
    }

    private static void upToDate()
    {
        MackageDesc[] mkgs = tool.upToDate();
        for (int i = 0; i < mkgs.length; i++) {
            System.out.println(pad(mkgs[i].getName())
                               + "extraName: " + mkgs[i].getExtraName()
                               + "\tinstalled: " + mkgs[i].getInstalledVersion()
                               + "\tavailable: " + mkgs[i].getAvailableVersion());
        }
    }

    private static Tid instantiate(String mackageName, String[] args)
        throws Exception
    {
        NodeDesc nd = null == policy ? tm.instantiate(mackageName, args)
            : tm.instantiate(mackageName, policy, args);
        Tid tid = nd.getTid();
        System.out.println(tid.getName());

        return tid;
    }

    private static void loadt(String shortName, String[] args)
        throws Exception
    {
        String pkg = pkgName(shortName);

        if (!isInstalled(pkg)) {
            install(pkg);
        }

        Tid tid = instantiate(pkg, args);

        tm.nodeContext(tid).node().start();
    }

    private static void reloadt(String shortName)
        throws Exception
    {
        String pkg = pkgName(shortName);

        tool.unregister(pkg);
        tool.register(pkg);
    }

    private static void unloadt(String shortName)
        throws Exception
    {
        String pkg = pkgName(shortName);

        List<Tid> tids = tm.nodeInstances();
        for (Tid t : tids) {
            NodeContext tctx = tm.nodeContext(t);
            if (tctx == null) {
                System.err.println("NULL Node Context (tid:" + t + ")");
                throw new Exception("NULL Node Context (tid:" + t + ")");
            }
            Node node = tctx.node();
            if (node == null) {
                System.err.println("NULL Node (tid:" + t + ")");
                throw new Exception("NULL Node (tid:" + t + ")");
            }
            String name = tctx.getNodeDesc().getName();
            if (name.equals(pkg)) {
                tm.destroy(t);
            }
        }
    }

    private static void start(String tidStr)
        throws Exception
    {
        Tid tid = new Tid(Long.parseLong(tidStr));
        tm.nodeContext(tid).node().start();
    }

    private static void stop(String tidStr)
        throws Exception
    {
        Tid tid = new Tid(Long.parseLong(tidStr));
        tm.nodeContext(tid).node().stop();
    }

    private static void destroy(String tidStr)
        throws Exception
    {
        Tid tid = new Tid(Long.parseLong(tidStr));
        tm.destroy(tid);
    }

    private static void neverStarted(String tidStr)
    {
        Tid tid = new Tid(Long.parseLong(tidStr));
        NodeContext tctx = tm.nodeContext(tid);
        if (tctx == null) {
            System.err.println("NULL Node Context (tid:" + tid + ")");
            return;
        }
        Node node = tctx.node();
        if (node == null) {
            System.err.println("NULL Node Context (tid:" + tid + ")");
            return;
        }

        System.out.println(node.neverStarted());
        return;
    }

    private static void doPasswd(String[] args)
        throws Exception
    {
        boolean addUser = false;
        boolean delUser = false;
        int i = 0;
        String login;
        String password = null;
        if (args.length < 1) {
            System.out.println("Usage: ");
            System.out.println("    ucli passwd [ -a | -d ] login [ password ]");
            System.exit(-1);
        }
        if ("-a".equals(args[i])) {
            addUser = true;
            i++;
        } else if ("-d".equals(args[i])) {
            delUser = true;
            i++;
        }
        if (args.length <= i || args.length > i + 2 || (delUser && args.length > i + 1)) {
            System.out.println("Usage: ");
            System.out.println("    ucli passwd [ -a | -d ] login [ password ]");
            System.exit(-1);
        }
        login = args[i++];
        if (args.length > i)
            password = args[i];

        AdminSettings as = mc.adminManager().getAdminSettings();
        Set users = as.getUsers();
        User user = null;
        for (Iterator iter = users.iterator(); iter.hasNext(); ) {
            user = (User)iter.next();
            if (login.equals(user.getLogin())) {
                if (addUser) {
                    System.out.println("Error: User already exists.  Aborting.");
                    System.exit(-1);
                } else {
                    break;
                }
            }
            user = null;
        }
        if (!addUser && user == null) {
            System.out.println("Error: User not found.  Aborting.");
            System.exit(-1);
        }
        if (delUser) {
            users.remove(user);
            mc.adminManager().setAdminSettings(as);
            System.out.println("Removed user with login: " + login);
        } else {
            if (password == null)
                password = readPassword();
            if (addUser) {
                users.add(new User(login, password, "[created by Untangle support]", false));
                mc.adminManager().setAdminSettings(as);
                System.out.println("Created new user with login: " + login);
            } else {
                user.setClearPassword(password);
                mc.adminManager().setAdminSettings(as);
                System.out.println("Set password of user with login: " + login);
            }
        }
    }

    private static String readPassword()
    {
        try {
            System.out.print("Password: ");
            BufferedReader d
                = new BufferedReader(new InputStreamReader(System.in));
            return d.readLine();
        } catch (IOException x) {
            x.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    private static void instances()
    {
        for (Tid t : tm.nodeInstances()) {
            NodeContext tctx = tm.nodeContext(t);
            if (tctx == null) {
                System.err.println(t + "\tNULL Node Context");
                continue;
            }
            Node node = tctx.node();
            if (node == null) {
                System.err.println(t + "\tNULL Node Context");
                continue;
            }
            String name = pad(tctx.getNodeDesc().getName(), 25);
            System.out.println(t.getName() + "\t" + name + "\t" + t.getPolicy()
                               + "\t" + node.getRunState());
        }
    }

    private static void dumpSessions()
    {
        for (Tid t : tm.nodeInstances()) {
            NodeContext tctx = tm.nodeContext(t);
            if (tctx == null) {
                System.err.println("NULL Node Context (tid:" + t + ")");
                continue;
            }
            Node node = tctx.node();
            if (node == null) {
                System.err.println("NULL Node (tid:" + t + ")");
                continue;
            }
            node.dumpSessions();
        }
    }

    private static void sessions(String tidStr)
    {
        Tid t = new Tid(Long.parseLong(tidStr));
        sessions(t);
    }

    private static void sessions()
    {
        for (Tid t : tm.nodeInstances()) {
            sessions(t);
        }
    }

    private static void sessions(Tid t)
    {
        AbsoluteTimeDateFormat atdf = new AbsoluteTimeDateFormat();
        NodeContext tctx = tm.nodeContext(t);
        if (tctx == null) {
            System.out.println("NULL Node Context (tid:" + t + ")");
            return;
        }
        Node node = tctx.node();
        if (node == null) {
            System.out.println("NULL Node (tid:" + t + ")");
            return;
        }
        NodeDesc tdesc = tctx.getNodeDesc();
        if (tdesc == null) {
            System.out.println("NULL Node Desc (tid:" + t + ")");
            return;
        }
        SessionDesc[] sdescs = node.liveSessionDescs();
        if (sdescs == null) {
            System.out.println("NULL Session Desc (tid:" + t + ")");
            return;
        }

        StringBuffer result = new StringBuffer(128);
        result.append("Live sessions for ");
        result.append(tdesc.getName());
        result.append("\n");
        result.append("ID\t\tDir\tC State\tC Addr : C Port\tS State\tS Addr : S Port\t");
        result.append("Created\t\tLast Activity\tC->T B\tT->S B\tS->T B\tT->C B");
        System.out.println(result.toString());
        for (int j = 0; j < sdescs.length; j++) {
            result.setLength(0);
            IPSessionDesc sd = (IPSessionDesc) sdescs[j];
            SessionStats stats = sd.stats();
            if (sd instanceof UDPSessionDesc)
                result.append("U");
            else if (sd instanceof TCPSessionDesc)
                result.append("T");
            result.append(sd.id());
            result.append("\t");
            result.append(SessionUtil.prettyState(sd.clientState()));
            result.append("\t");
            result.append(sd.clientAddr().getHostAddress());
            result.append(":");
            result.append(sd.clientPort());
            result.append("\t");
            result.append(SessionUtil.prettyState(sd.serverState()));
            result.append("\t");
            result.append(sd.serverAddr().getHostAddress());
            result.append(":");
            result.append(sd.serverPort());
            result.append("\t");
            atdf.format(stats.creationDate(), result, null);
            result.append("\t");
            atdf.format(stats.lastActivityDate(), result, null);
            result.append("\t");
            result.append(stats.c2tBytes());
            result.append("\t");
            result.append(stats.t2sBytes());
            result.append("\t");
            result.append(stats.s2tBytes());
            result.append("\t");
            result.append(stats.t2cBytes());
            System.out.println(result.toString());
        }
    }

    private static void who()
    {
        LoginSession l = RemoteUvmContextFactory.factory().loginSession();
        String ln = null == l ? "nobody" : l.getUvmPrincipal().getName();
        System.out.println("You are: " + ln + " " + l.getSessionId());
        LoginSession[] ls = mc.adminManager().loggedInUsers();
        for (int i = 0; i < ls.length; i++) {
            UvmPrincipal mp = ls[i].getUvmPrincipal();
            ln = null == mp ? "nobody" : mp.getName();
            System.out.println(ls[i].getSessionId() + "\t" + ln);
        }
    }

    private static void getRegInfo()
    {
        RegistrationInfo regInfo = mc.adminManager().getRegistrationInfo();
        if (regInfo == null) {
            System.out.println("No registration info found!");
        } else {
            System.out.println(regInfo.toString());
        }
    }

    private static void register(String name)
        throws Exception
    {
        System.out.println("registering mackage: " + name);
        tool.register(name);
    }

    private static void unregister(String mackageName)
        throws Exception
    {
        System.out.println("unregistering mackage: " + mackageName);
        tool.unregister(mackageName);
    }

    private static void shutdown()
    {
        mc.shutdown();
    }

    private static void isReportingEnabled()
    {
        System.out.println(mc.reportingManager().isReportingEnabled());
    }

    private static void areReportsAvailable()
    {
        System.out.println(mc.reportingManager().isReportsAvailable());
    }

    private static void isReportingRunning()
    {
        System.out.println(mc.reportingManager().isRunning());
    }

    private static void startReports()
        throws Exception
    {
        System.out.println("Report generation starting");
        mc.reportingManager().startReports();
    }

    private static void stopReports()
        throws Exception
    {
        System.out.println("Report generation stopping");
        mc.reportingManager().stopReports();
    }

    private static final SimpleDateFormat DAYDATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static void prepareReports(String[] args)
        throws Exception
    {
        String outputBaseDirName = "/tmp";
        int daysToKeep = 90;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date midnight = c.getTime();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-o")) {
                outputBaseDirName = args[++i];
            } else if (args[i].equals("-d")) {
                daysToKeep = Integer.parseInt(args[++i]);
                if (daysToKeep < 1)
                    daysToKeep = 1;
            } else if (args[i].equals("-n")) {
                midnight = DAYDATE_FORMAT.parse(args[++i], new ParsePosition(0));
                if (midnight == null) {
                    System.out.println("Unable to parse date " + args[i]);
                    System.exit(-1);
                }
            } else {
                printUsage();
                System.exit(-1);
            }
        }
        System.out.println("Preparing for report generation to " + outputBaseDirName);
        mc.reportingManager().prepareReports(outputBaseDirName, midnight, daysToKeep);
    }


    /**
     * <code>resetLogs</code> re-reads all log configuration files
     * (jboss, uvm, all node instances).  This allows changing
     * logging levels, etc.  The old output files will be erased and
     * new files begun.
     */
    private static void resetLogs()
    {
        mc.loggingManager().resetAllLogs();
    }

    /**
     * <code>logError</code> logs <code>errText</code> or
     * if not specified, logs default error text to the log file.
     */
    private static void logError(String[] errTexts)
    {
        if (null == errTexts) {
            mc.loggingManager().logError(null);
            return;
        }

        StringBuilder errText = new StringBuilder();
        for (String errFrag : errTexts) {
            if (0 != errText.length()) {
                errText.append(" ");
            }
            errText.append(errFrag);
        }
        mc.loggingManager().logError(errText.toString());
        return;
    }

    private static void doFullGC()
    {
        mc.doFullGC();
    }

    private static void loadRup()
    {
        mc.loadRup();
    }

    private static void setProperty(String key, String value)
    {
        mc.setProperty(key, value);
    }

    private static void addPolicy(String policy, String notes)
        throws PolicyException
    {
        mc.policyManager().addPolicy(policy, null == notes ? Policy.NO_NOTES : notes);
    }

    private static void listPolicies()
    {
        for (Policy p : mc.policyManager().getPolicies()) {
            System.out.println(p);
        }
    }

    private static void doAptTail()
    {
        // XXX implement me
    }

    private static void updateAddress() throws Exception
    {
        mc.networkManager().updateAddress();
    }

    private static void doReloadLicenses()
    {
        mc.licenseManager().reloadLicenses();
    }


    // helper functions -------------------------------------------------------

    private static boolean isInstalled(String mackageName)
    {
        MackageDesc[] installed = tool.installed();
        for (int i = 0; i < installed.length; i++) {
            if (installed[i].getName().equals(mackageName)) {
                return true;
            }
        }
        return false;
    }

    private static String pkgName(String mkg)
    {
        if (mkg.equals("http") || mkg.equals("ftp") || mkg.equals("mail")) {
            return ("untangle-casing-" + mkg);
        } else {
            return ("untangle-node-" + mkg);
        }
    }

    private static final int DEFAULT_PAD = 25;

    private static final String pad(String str)
    {
        return pad(str, DEFAULT_PAD);
    }

    private static final String pad(String str, int padsize)
    {
        StringBuilder sb = new StringBuilder(str.trim());
        if (str.length() >= padsize) {
            return sb.append(' ').toString();
        }
        for (int i = str.length(); i < padsize; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static void printUsage()
    {
        System.out.println("Usage: ");
        System.out.println("  optional args: ");
        System.out.println("    -h hostname");
        System.out.println("    -u username");
        System.out.println("    -w password");
        System.out.println("    -t timeout (default 120000)");
        System.out.println("    -p policy");
        System.out.println("    -v");
        System.out.println("  toolbox commands:");
        System.out.println("    ucli install mackage-name");
        System.out.println("    ucli uninstall mackage-name");
        System.out.println("    ucli update");
        System.out.println("    ucli upgrade");
        System.out.println("    ucli requestInstall mackage-name");
        System.out.println("  toolbox lists:");
        System.out.println("    ucli available");
        System.out.println("    ucli installed");
        System.out.println("    ucli uninstalled");
        System.out.println("    ucli upgradable");
        System.out.println("    ucli uptodate");
        System.out.println("  node manager commands:");
        System.out.println("    ucli instantiate mackage-name [ args ]");
        System.out.println("    ucli start TID");
        System.out.println("    ucli stop TID");
        System.out.println("    ucli destroy TID");
        System.out.println("    ucli neverStarted");
        System.out.println("  node manager lists:");
        System.out.println("    ucli instances");
        System.out.println("  node live sessions:");
        System.out.println("    ucli sessions [ TID ]");
        System.out.println("  admin manager:");
        System.out.println("    ucli who");
        System.out.println("    ucli getRegInfo");
        System.out.println("    ucli passwd [ -a | -d ] login [ password ]");
        System.out.println("  uvm commands:");
        System.out.println("    ucli shutdown");
        System.out.println("    ucli serverStats");
        System.out.println("    ucli gc");
        System.out.println("    ucli loadRup");
        System.out.println("    ucli setProperty key value");
        System.out.println("  policy manager:");
        System.out.println("    ucli addPolicy name [notes]");
        System.out.println("    ucli listPolicies");
        System.out.println("  reporting manager: ");
        System.out.println("    ucli isReportingEnabled");
        System.out.println("    ucli areReportsAvailable");
        System.out.println("    ucli prepareReports [ args ]");
        System.out.println("    ucli startReports");
        System.out.println("    ucli stopReports");
        System.out.println("    ucli isReportingRunning");
        System.out.println("  logging manager: ");
        System.out.println("    ucli userLogs tid");
        System.out.println("    ucli resetLogs");
        System.out.println("    ucli logError [text]");
        System.out.println("  combo commands:");
        System.out.println("    ucli loadt short-name [ args ]");
        System.out.println("    ucli reloadt short-name");
        System.out.println("    ucli unloadt short-name");
        System.out.println("  apt commands:");
        System.out.println("    ucli register mackage-name");
        System.out.println("    ucli unregister mackage-name");
        System.out.println("  argon commands:");
        System.out.println("  nucli server commands:");
        System.out.println("    ucli restartCliServer");
        System.out.println("  debugging commands:");
        System.out.println("    ucli aptTail");
    }

    private static void doRestartCliServer()
    {
        mc.restartCliServer();
    }

    private static void doStopCliServer()
    {
        mc.stopCliServer();
    }
}
