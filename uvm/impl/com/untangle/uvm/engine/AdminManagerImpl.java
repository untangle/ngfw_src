/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.TransactionRolledbackException;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.MailSender;
import com.untangle.uvm.MailSettings;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.User;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.security.UvmPrincipal;
import com.untangle.uvm.snmp.SnmpManager;
import com.untangle.uvm.snmp.SnmpManagerImpl;
import com.untangle.uvm.util.FormUtil;
import com.untangle.uvm.util.HasConfigFiles;
import com.untangle.uvm.util.TransactionWork;

/**
 * Remote interface for administrative user management.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class AdminManagerImpl implements AdminManager, HasConfigFiles
{
    private static final String INITIAL_USER_NAME = "System Administrator";
    private static final String INITIAL_USER_LOGIN = "admin";
    private static final String INITIAL_USER_PASSWORD = "passwd";

    private static final String SET_TIMEZONE_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-timezone";
    private static final String UVM_VERSION_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-uvm-version.sh";
    private static final String REBOOT_COUNT_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-reboot-count.sh";
    private static final String TIMEZONE_FILE = "/etc/timezone";
    
    private static final String ALPACA_NONCE_FILE = "/etc/untangle-net-alpaca/nonce";

    private final UvmContextImpl uvmContext;
    private final InheritableThreadLocal<HttpServletRequest> threadRequest;

    private final Logger logger = Logger.getLogger(this.getClass());

    private AdminSettings adminSettings;
    private SnmpManagerImpl snmpManager;

    AdminManagerImpl(UvmContextImpl uvmContext, InheritableThreadLocal<HttpServletRequest> threadRequest)
    {
        this.uvmContext = uvmContext;
        this.threadRequest = threadRequest;

        TransactionWork<Void> tw = new TransactionWork<Void>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from AdminSettings");
                    adminSettings = (AdminSettings)q.uniqueResult();

                    if (null == adminSettings) {
                        adminSettings = new AdminSettings();
                        adminSettings.addUser(new User(INITIAL_USER_LOGIN,
                                                       INITIAL_USER_PASSWORD,
                                                       INITIAL_USER_NAME));
                        s.save(adminSettings);

                    }
                    return true;
                }
            };
        uvmContext.runTransaction(tw);

        snmpManager = SnmpManagerImpl.snmpManager();

        // If timezone on box is different (example: kernel upgrade), reset it:
        TimeZone currentZone = getTimeZone();
        if (!currentZone.equals(TimeZone.getDefault()))
            try {
                setTimeZone(currentZone);
            } catch (Exception x) {
                // Already logged.
            }

        logger.info("Initialized AdminManager");
    }

    public void syncConfigFiles()
    {
        snmpManager.syncConfigFiles();
    }

    public MailSettings getMailSettings()
    {
        MailSender ms = uvmContext.mailSender();
        return ms.getMailSettings();
    }

    public void setMailSettings(MailSettings settings)
    {
        MailSender ms = uvmContext.mailSender();
        ms.setMailSettings(settings);
    }

    public boolean sendTestMessage(String recipient)
    {
        MailSender ms = uvmContext.mailSender();
        return ms.sendTestMessage(recipient);
    }

    public AdminSettings getAdminSettings()
    {
        return adminSettings;
    }

    public void setAdminSettings(final AdminSettings as)
    {
        updateUserPasswords(as);

        // Do something with summaryPeriod? XXX
        TransactionWork<Void> tw = new TransactionWork<Void>()
            {
                public boolean doWork(Session s)
                {
                    adminSettings = (AdminSettings)s.merge(as);
                    return true;
                }
            };
        uvmContext.runTransaction(tw);

    }

    private void updateUserPasswords(final AdminSettings as) {
        for ( Iterator<User> i = adminSettings.getUsers().iterator(); i.hasNext(); ) {
            User user = i.next();
            User mUser = null;
            if ( ( mUser = modifiedUser( user, as.getUsers() )) != null &&
                    mUser.getPassword() == null) {
                mUser.updatePassword(user);
            }
        }
    }

    private User modifiedUser( User user, Set<User> updatedUsers )
    {
        for ( User currentUser : updatedUsers ) {
            if( user.getId().equals(currentUser.getId())) return currentUser;
        }

        return null;
    }

    @Override
    public TimeZone getTimeZone()
    {
        try {
            BufferedReader in = new BufferedReader(new FileReader(TIMEZONE_FILE));
            String str = in.readLine();
            str = str.trim();
            in.close();
            TimeZone current = TimeZone.getTimeZone(str);
            return current;
        } catch (Exception x) {
            logger.warn("Unable to get timezone, using java default:" , x);
            return TimeZone.getDefault();
        }
    }

    public void setTimeZone(TimeZone timezone)
        throws TransactionRolledbackException
    {
        String id = timezone.getID();

        try {
            Integer exitValue = uvmContext.execManager().execResult( SET_TIMEZONE_SCRIPT + " " + id );
            if (0 != exitValue) {
                String message = "Unable to set time zone (" + exitValue + ") to: " + id;
                logger.error(message);
                throw new TransactionRolledbackException(message);
            } else {
                logger.info("Time zone set to : " + id);
                TimeZone.setDefault(timezone); // Note: Only works for threads who haven't yet cached the zone!  XX
            }
        } catch (IOException exn) {
            String message = "Exception during set time zone to: " + id;
            logger.error(message, exn);
            throw new TransactionRolledbackException(message);
        }
    }

    public String getDate()
    {
        return (new Date(System.currentTimeMillis())).toString();
    }

    public SnmpManager getSnmpManager() {
        return snmpManager;
    }

    public String getAlpacaNonce()
    {
        BufferedReader stream = null;
        try {
            stream = new BufferedReader(new FileReader(ALPACA_NONCE_FILE));

            String nonce = stream.readLine();
            if (nonce.length() < 3) {
                logger.warn("Invalid nonce in the file ["
                            + ALPACA_NONCE_FILE + "]: ', " +
                            nonce + "'");
                return null;
            }

            return nonce;
        } catch (IOException exn) {
            logger.warn("could not get alpaca nonce", exn);
            return null;
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                logger.warn("Unable to close the nonce file: "
                            + ALPACA_NONCE_FILE, e);
            }
        }
    }

    public String getFullVersionAndRevision()
    {
        try {
            String version = uvmContext.execManager().execOutput(UVM_VERSION_SCRIPT);

            if (version == null)
                return "";
            else
                return version.replaceAll("(\\r|\\n)", "");
        } catch (Exception e) {
            logger.warn("Unable to fetch version",e);
        }

        /**
         * that method probably timed out
         * fall back to this method
         */
        return uvmContext.getFullVersion();
    }

    public String getModificationState()
    {
        File zshHistoryFile = new File("/root/.zsh_history");
        File blessedFile = new File(System.getProperty("uvm.conf.dir") + "/mods-blessed-flag");

        /* if there is no zsh_history file it obviously hasn't been modified */
        if (!zshHistoryFile.exists())
            return "none";

        /* if there is a zsh_history, but the blessed flag is newer these changes have been approved */
        if (blessedFile.exists() && blessedFile.lastModified() > zshHistoryFile.lastModified())
            return "blessed";

        ExecManagerResult result = uvmContext.execManager().exec("cat /root/.zsh_history | /usr/bin/wc -l");
        int exitCode = result.getResult();
        String output = result.getOutput();

        output = output.replaceAll("(\\r|\\n)", "");
            
        if( exitCode == 0 ) {
            return new String("yes (" + output + ")");
        }

        return "UNKNOWN";
    }

    public String getRebootCount()
    {
        try {
            String count = uvmContext.execManager().execOutput(REBOOT_COUNT_SCRIPT);
	    
            if (count == null)
                return "";
            else
                return count.replaceAll("(\\r|\\n)", "");
        } catch (Exception e) {
            logger.warn("Unable to fetch version",e);
        }

        return "Unknown";
    }
    
}
