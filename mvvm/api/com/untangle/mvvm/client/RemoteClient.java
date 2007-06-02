/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.StringBuilder;
import java.net.InetAddress;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.untangle.mvvm.policy.Policy;
import com.untangle.mvvm.policy.PolicyException;
import com.untangle.mvvm.security.AdminSettings;
import com.untangle.mvvm.security.LoginSession;
import com.untangle.mvvm.security.MvvmPrincipal;
import com.untangle.mvvm.security.RegistrationInfo;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.security.User;
import com.untangle.mvvm.tapi.IPSessionDesc;
import com.untangle.mvvm.tapi.SessionDesc;
import com.untangle.mvvm.tapi.SessionStats;
import com.untangle.mvvm.tapi.TCPSessionDesc;
import com.untangle.mvvm.tapi.UDPSessionDesc;
import com.untangle.mvvm.toolbox.DownloadComplete;
import com.untangle.mvvm.toolbox.DownloadProgress;
import com.untangle.mvvm.toolbox.DownloadSummary;
import com.untangle.mvvm.toolbox.InstallComplete;
import com.untangle.mvvm.toolbox.InstallProgress;
import com.untangle.mvvm.toolbox.InstallTimeout;
import com.untangle.mvvm.toolbox.MackageDesc;
import com.untangle.mvvm.toolbox.MackageInstallException;
import com.untangle.mvvm.toolbox.MackageInstallRequest;
import com.untangle.mvvm.toolbox.ProgressVisitor;
import com.untangle.mvvm.toolbox.ToolboxManager;
import com.untangle.mvvm.toolbox.ToolboxMessageVisitor;
import com.untangle.mvvm.tran.Transform;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformDesc;
import com.untangle.mvvm.tran.TransformManager;
import com.untangle.mvvm.util.SessionUtil;
import org.apache.log4j.helpers.AbsoluteTimeDateFormat;

public class RemoteClient
{
    private static String host = "localhost";
    private static String username = "admin";
    private static String passwd = "passwd";
    private static String policyName = null;
    private static Policy policy = null;

    private static final String SHIELD_STATUS_USAGE = "mcli shieldStatus ip port [interval]";

    private static boolean verbose = false;
    private static int timeout = 120000;

    private static MvvmRemoteContext mc;

    private static ToolboxManager tool;
    private static TransformManager tm;

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

        MvvmRemoteContextFactory factory = MvvmRemoteContextFactory.factory();
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
        tm = mc.transformManager();

        if (args[0].equalsIgnoreCase("install")) {
            install(args[1]);
        } else if (args[0].equalsIgnoreCase("uninstall")) {
            uninstall(args[1]);
        } else if (args[0].equalsIgnoreCase("update")) {
            update();
        } else if (args[0].equalsIgnoreCase("upgrade")) {
            upgrade();
        } else if (args[0].equalsIgnoreCase("enable")) {
            enable(args[1]);
        } else if (args[0].equalsIgnoreCase("disable")) {
            disable(args[1]);
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
        } else if (args[0].equalsIgnoreCase("shieldStatus")) {
            shieldStatus(args);
        } else if (args[0].equalsIgnoreCase("shieldReconfigure")) {
            shieldReconfigure();
        } else if (args[0].equalsIgnoreCase("updateAddress")) {
            updateAddress();
        } else if (args[0].equalsIgnoreCase("gc")) {
            doFullGC();
        } else if (args[0].equalsIgnoreCase("messageQueue")) {
            messageQueue();
        } else if (args[0].equalsIgnoreCase("addPolicy")) {
            addPolicy(args[1], 3 > args.length ? null : args[2]);
        } else if (args[0].equalsIgnoreCase("listPolicies")) {
            listPolicies();
        } else if (args[0].equalsIgnoreCase("aptTail")) {
            doAptTail(Long.parseLong(args[1]));
        } else if (args[0].equalsIgnoreCase("passwd")) {
            String[] pwArgs = new String[args.length - 1];
            System.arraycopy(args, 1, pwArgs, 0, pwArgs.length);
            doPasswd(pwArgs);
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

    private static class McliToolboxMessageVisitor
        implements ToolboxMessageVisitor
    {
        public void visitMackageInstallRequest(MackageInstallRequest req)
        {
            String mackageName = req.getMackageName();
            System.out.println("Installing: " + mackageName);
            try {
                tool.install(mackageName);
                System.out.println("booyakasha");
            } catch (MackageInstallException exn) {
                System.out.println("could not install: " + mackageName);
                exn.printStackTrace();
            }
        }
    }

    private static class Visitor implements ProgressVisitor
    {
        private boolean done = false;

        // public methods ----------------------------------------------------

        public boolean isDone()
        {
            return done;
        }

        // ProgressVisitor methods -------------------------------------------

        public void visitDownloadSummary(DownloadSummary ds)
        {
            System.out.println("Downloading " + ds.getCount() + " packages "
                               + ds.getSize() + " bytes.");
        }

        public void visitDownloadProgress(DownloadProgress dp)
        {
            System.out.println("Downloading " + dp.getName() + " "
                               + dp.getBytesDownloaded() + "/" + dp.getSize()
                               + " " + dp.getSpeed());
        }

        public void visitInstallComplete(InstallComplete ic)
        {
            if (ic.getSuccess()) {
                System.out.println("Installation succeeded");
            } else {
                System.out.println("Installation failed");
            }
            done = true;
        }

        public void visitDownloadComplete(DownloadComplete dc)
        {
            if (dc.getSuccess()) {
                System.out.println("Download succeeded");
            } else {
                System.out.println("Download failed");
            }
        }

        public void visitInstallTimeout(InstallTimeout it)
        {
            System.out.println("Install timed out at: " + it.getTime());
            done = true;
        }
    }

    private static void install(String mackageName)
        throws Exception
    {
        long key = tool.install(mackageName);

        doAptTail(key);
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
        long key = tool.upgrade();

        doAptTail(key);
    }

    private static void enable(String mackage)
        throws Exception
    {
        tool.enable(mackage);
    }

    private static void disable(String mackage)
        throws Exception
    {
        tool.disable(mackage);
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
        Tid tid = null == policy ? tm.instantiate(mackageName, args)
            : tm.instantiate(mackageName, policy, args);
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

        tm.transformContext(tid).transform().start();
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

        List<Tid> tids = tm.transformInstances();
        for (Tid t : tids) {
            TransformContext tctx = tm.transformContext(t);
            if (tctx == null) {
                System.err.println("NULL Transform Context (tid:" + t + ")");
                throw new Exception("NULL Transform Context (tid:" + t + ")");
            }
            Transform tran = tctx.transform();
            if (tran == null) {
                System.err.println("NULL Transform (tid:" + t + ")");
                throw new Exception("NULL Transform (tid:" + t + ")");
            }
            String name = tctx.getTransformDesc().getName();
            if (name.equals(pkg)) {
                tm.destroy(t);
            }
        }
    }

    private static void start(String tidStr)
        throws Exception
    {
        Tid tid = new Tid(Long.parseLong(tidStr));
        tm.transformContext(tid).transform().start();
    }

    private static void stop(String tidStr)
        throws Exception
    {
        Tid tid = new Tid(Long.parseLong(tidStr));
        tm.transformContext(tid).transform().stop();
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
        TransformContext tctx = tm.transformContext(tid);
        if (tctx == null) {
            System.err.println("NULL Transform Context (tid:" + tid + ")");
            return;
        }
        Transform tran = tctx.transform();
        if (tran == null) {
            System.err.println("NULL Transform Context (tid:" + tid + ")");
            return;
        }

        System.out.println(tran.neverStarted());
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
            System.out.println("    mcli passwd [ -a | -d ] login [ password ]");
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
            System.out.println("    mcli passwd [ -a | -d ] login [ password ]");
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
        for (Tid t : tm.transformInstances()) {
            TransformContext tctx = tm.transformContext(t);
            if (tctx == null) {
                System.err.println(t + "\tNULL Transform Context");
                continue;
            }
            Transform tran = tctx.transform();
            if (tran == null) {
                System.err.println(t + "\tNULL Transform Context");
                continue;
            }
            String name = pad(tctx.getTransformDesc().getName(), 25);
            System.out.println(t.getName() + "\t" + name + "\t" + t.getPolicy()
                               + "\t" + tran.getRunState());
        }
    }

    private static void dumpSessions()
    {
        for (Tid t : tm.transformInstances()) {
            TransformContext tctx = tm.transformContext(t);
            if (tctx == null) {
                System.err.println("NULL Transform Context (tid:" + t + ")");
                continue;
            }
            Transform tran = tctx.transform();
            if (tran == null) {
                System.err.println("NULL Transform (tid:" + t + ")");
                continue;
            }
            tran.dumpSessions();
        }
    }

    private static void sessions(String tidStr)
    {
        Tid t = new Tid(Long.parseLong(tidStr));
        sessions(t);
    }

    private static void sessions()
    {
        for (Tid t : tm.transformInstances()) {
            sessions(t);
        }
    }

    private static void sessions(Tid t)
    {
        AbsoluteTimeDateFormat atdf = new AbsoluteTimeDateFormat();
        TransformContext tctx = tm.transformContext(t);
        if (tctx == null) {
            System.out.println("NULL Transform Context (tid:" + t + ")");
            return;
        }
        Transform tran = tctx.transform();
        if (tran == null) {
            System.out.println("NULL Transform (tid:" + t + ")");
            return;
        }
        TransformDesc tdesc = tctx.getTransformDesc();
        if (tdesc == null) {
            System.out.println("NULL Transform Desc (tid:" + t + ")");
            return;
        }
        SessionDesc[] sdescs = tran.liveSessionDescs();
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
            result.append(sd.isInbound() ? "In" : "Out");
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
        LoginSession l = MvvmRemoteContextFactory.factory().loginSession();
        String ln = null == l ? "nobody" : l.getMvvmPrincipal().getName();
        System.out.println("You are: " + ln + " " + l.getSessionId());
        LoginSession[] ls = mc.adminManager().loggedInUsers();
        for (int i = 0; i < ls.length; i++) {
            MvvmPrincipal mp = ls[i].getMvvmPrincipal();
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
     * (jboss, mvvm, all tran instances).  This allows changing
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

    private static void messageQueue()
    {
        MessageClient msgClient = new MessageClient(mc);
        msgClient.setToolboxMessageVisitor(new McliToolboxMessageVisitor());
        msgClient.start();

        // msgClient uses a daemon thread (only needed for mcli)
        while (true) {
            try {
                Thread.sleep(600000);
            } catch (InterruptedException exn) {
                System.out.println("interrupted");
                break;
            }
        }
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

    private static void doAptTail(long key)
    {
        Visitor v = new Visitor();
        while (!v.isDone()) {
            List<InstallProgress> lip = tool.getProgress(key);
            for (InstallProgress ip : lip) {
                ip.accept(v);
            }
        }
    }

    /**
     * <code>shieldStatus</code> Sends out the current state of the shield
     * via UDP to the host and port specified in the command line
     */
    private static void shieldStatus(String args[]) throws Exception
    {
        /* If interval is zero, it doesn't loop */
        int interval = 0;
        InetAddress destination;
        int port = 0;

        switch (args.length) {
        case 4:
            /* Convert from seconds to milliseconds */
            interval = Integer.parseInt(args[3]) * 1000;
            /* fallthrough */
        case 3:
            destination = InetAddress.getByName(args[1]);
            port = Integer.parseInt(args[2]);
            break;

        default:
            throw new Exception("Usage: " + SHIELD_STATUS_USAGE);
        }

        mc.shieldManager().shieldStatus(destination,port,interval);
    }

    /**
     * <code>shieldStatus</code> Sends out the current state of the shield
     * via UDP to the host and port specified in the command line
     */
    private static void shieldReconfigure() throws Exception
    {
        mc.shieldManager().shieldReconfigure();
    }

    private static void updateAddress() throws Exception
    {
        mc.networkManager().updateAddress();
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
        if (mkg.equals("http") || mkg.equals("ftp")) {
            return (mkg + "-casing");
        } else {
            return (mkg + "-transform");
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
        System.out.println("    mcli install mackage-name");
        System.out.println("    mcli uninstall mackage-name");
        System.out.println("    mcli update");
        System.out.println("    mcli upgrade");
        System.out.println("    mcli enable mackage-name");
        System.out.println("    mcli disable mackage-name");
        System.out.println("    mcli extraName mackage-name extra-name");
        System.out.println("    mcli upgrade");
        System.out.println("    mcli requestInstall mackage-name");
        System.out.println("  toolbox lists:");
        System.out.println("    mcli available");
        System.out.println("    mcli installed");
        System.out.println("    mcli uninstalled");
        System.out.println("    mcli upgradable");
        System.out.println("    mcli uptodate");
        System.out.println("  transform manager commands:");
        System.out.println("    mcli instantiate mackage-name [ args ]");
        System.out.println("    mcli start TID");
        System.out.println("    mcli stop TID");
        System.out.println("    mcli destroy TID");
        System.out.println("    mcli neverStarted");
        System.out.println("  transform manager lists:");
        System.out.println("    mcli instances");
        System.out.println("  transform live sessions:");
        System.out.println("    mcli sessions [ TID ]");
        System.out.println("  admin manager:");
        System.out.println("    mcli who");
        System.out.println("    mcli getRegInfo");
        System.out.println("    mcli passwd [ -a | -d ] login [ password ]");
        System.out.println("  mvvm commands:");
        System.out.println("    mcli shutdown");
        System.out.println("    mcli serverStats");
        System.out.println("    mcli gc");
        System.out.println("    mcli messageQueue");
        System.out.println("  policy manager:");
        System.out.println("    mcli addPolicy name [notes]");
        System.out.println("    mcli listPolicies");
        System.out.println("  reporting manager: ");
        System.out.println("    mcli isReportingEnabled");
        System.out.println("    mcli areReportsAvailable");
        System.out.println("    mcli prepareReports [ args ]");
        System.out.println("    mcli startReports");
        System.out.println("    mcli stopReports");
        System.out.println("    mcli isReportingRunning");
        System.out.println("  logging manager: ");
        System.out.println("    mcli userLogs tid");
        System.out.println("    mcli resetLogs");
        System.out.println("    mcli logError [text]");
        System.out.println("  combo commands:");
        System.out.println("    mcli loadt short-name [ args ]");
        System.out.println("    mcli reloadt short-name");
        System.out.println("    mcli unloadt short-name");
        System.out.println("  apt commands:");
        System.out.println("    mcli register mackage-name");
        System.out.println("    mcli unregister mackage-name");
        System.out.println("  argon commands:");
        System.out.println("    " + SHIELD_STATUS_USAGE);
        System.out.println("    mcli shieldReconfigure");
        System.out.println("  debugging commands:");
        System.out.println("    mcli aptTail");
    }
}
