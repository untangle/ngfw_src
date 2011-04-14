/* $HeadURL$ */
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

import javax.servlet.http.HttpServletRequest;
import javax.transaction.TransactionRolledbackException;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.node.util.SimpleExec;
import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.MailSender;
import com.untangle.uvm.MailSettings;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.SystemInfo;
import com.untangle.uvm.User;
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
    private static final String TIMEZONE_FILE = "/etc/timezone";
    private static final String REGISTRATION_INFO_FILE = System.getProperty("uvm.conf.dir") + "/registration.info";
    private static final String BRAND_INFO_FILE = "/usr/share/untangle/tmp/brand";
    
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
            Process p = uvmContext.exec(new String[] { SET_TIMEZONE_SCRIPT, id });
            for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );
            int exitValue = p.waitFor();
            if (0 != exitValue) {
                String message = "Unable to set time zone (" + exitValue + ") to: " + id;
                logger.error(message);
                throw new TransactionRolledbackException(message);
            } else {
                logger.info("Time zone set to : " + id);
                TimeZone.setDefault(timezone); // Note: Only works for threads who haven't yet cached the zone!  XX
            }
        } catch (InterruptedException exn) {
            String message = "Interrupted during set time zone";
            logger.error(message);
            throw new TransactionRolledbackException(message);
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

    /*
     * Activate the box, used during the setup wizard to create the initial UID.
     */
    public boolean activate( )
    {
        return uvmContext.activate( null );
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

    private String getBrandNonce()
    {
        BufferedReader stream = null;
        String brand = "xxxxxx";
        try {
            stream = new BufferedReader(new FileReader(BRAND_INFO_FILE));

            brand = stream.readLine();
            if (brand.length() < 6) {
                logger.warn("Invalid brand in the file ["
                            + BRAND_INFO_FILE + "]: ', " +
                            brand + "'");
                return "xxxxxx";
            }
            brand = brand.substring(0,6).replaceAll("[^a-zA-Z0-9]", ".");
 
            return brand;
        } catch (Exception exn) {
            logger.warn("could not get brand", exn);
            return "xxxxxx";
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                logger.warn("Unable to close brand file: "
                            + BRAND_INFO_FILE, e);
            }
        }
    }
    
    public String getFullVersionAndRevision()
    {
        try {
            SimpleExec.SimpleExecResult result = SimpleExec.exec("/usr/share/untangle/bin/ut-uvm-version.sh",null,null,null,true,true,1000*20);
	    
            if(result.exitCode==0) {
                return new String(result.stdOut);
            }
        } catch (Exception e) {
            logger.warn("Unable to fetch version",e);
        }

        /**
         * that method probably timed out
         * fall back to this method
         */
        return uvmContext.getFullVersion();
    }

    public SystemInfo getSystemInfo()
    {
        String UID = uvmContext.getServerUID();
        String fullVersion = getFullVersionAndRevision();
        String javaVersion = System.getProperty("java.version");

        return new SystemInfo(UID, fullVersion, javaVersion);
    }

}
