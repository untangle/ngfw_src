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
package com.untangle.uvm.license;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.untangle.node.util.UtLogger;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.SettingsManager;

public class LicenseManagerImpl implements LicenseManager
{
    private static final int SELF_SIGNED_VERSION = 0x1F;

    private static final String LICENSE_DIRECTORY = System.getProperty("uvm.conf.dir") + "/licenses";
    private static final String PROPERTY_BASE = "com.untangle.uvm.license";
    private static final String PROPERTY_PRODUCT_IDENTIFIER = PROPERTY_BASE + ".identifier";
    private static final String PROPERTY_LICENSE_TYPE = PROPERTY_BASE + ".type";
    /* The package that this product is in, this is for mcli extraname */
    private static final String PROPERTY_START = PROPERTY_BASE + ".start";
    private static final String PROPERTY_END = PROPERTY_BASE + ".end";
    private static final String PROPERTY_KEY = PROPERTY_BASE + ".key";
    private static final String PROPERTY_KEY_VERSION = PROPERTY_BASE + ".keyVersion";

    private static final long LICENSE_SUBSCRIPTION_EXTENSION = (1000l * 60 * 60 * 24 * 365 * 8) + 835l;

    /* 30 day keys */
    private static final long OLD_TRIAL_KEY_DURATION = (1000l * 60 * 60 * 24 * 30) + 539;

    private static final String EXPIRED = "expired";

    /* These are the various license types */
//     /**
//      * FIXME XXX
//      * really we're adding random numbers of milliseconds? (278 and such)
//      * 
//      * This is just fucking stupid
//      */
//     private static enum LicenseType {
//         TRIAL14("14 Day Trial", 1000l * 60 * 60 * 24 * 15 + 278),
//         TRIAL("30 Day Trial", 1000l * 60 * 60 * 24 * 31 + 539),
//         SUBSCRIPTION("Subscription", 1000l * 60 * 60 * 24 * 365 * 2 + 344),
//         DEVELOPMENT("Untangle Development", 1000l * 60 * 60 * 24 * 60 + 960);

//         private final String name;
//         private final long duration;

//         private LicenseType(String name, long duration) {
//             this.name = name;
//             this.duration = duration;
//         }

//         public String getName() {
//             return this.name;
//         }

//         public long getDuration() {
//             return this.duration;
//         }
//     }

    private static final FileFilter LICENSE_FILE_FILTER = new FileFilter() {
        /* untangle-license-file */
        public boolean accept(File file) {
            /* don't read from pipes or directories */
            return file.isFile() && file.getName().endsWith(".ulf");
        }
    };

    /* One day in milliseconds */
    private static final long ONE_DAY = 1000 * 60 * 60 * 24;

    /* update every 2 hours, leaves an hour window */
    private static final long TIMER_DELAY = 1000 * 60 * 60 * 2;

    /* validate the product for three hours. */
    private static final long VALIDATION_PERIOD = 3 * 60 * 60 * 1000;

    /* has to be declared after all static declarations */
    private static final LicenseManagerImpl INSTANCE;

    /**
     * map from the product identifier to the latest valid license available for
     * this product
     */
    private Map<String, License> licenseMap = new HashMap<String, License>();

    /** list of all licenses. */
    private LicenseSettings licenseSettings;

    /** configuration */
    /** frequency of license updates */
    private final long timerDelay = TIMER_DELAY;

    /**
     * Amount of time to update the license for, if the timer task dies, all
     * license will expire in <code>validationPeriod</code> milliseconds
     */
    private final long validationPeriod = VALIDATION_PERIOD;

    /** Timer task */
    private final LicenseUpdateTask task = new LicenseUpdateTask();

    /** Pulse that updates the expiration dates, this is a daemon task. */
    private final Pulse pulse = new Pulse("uvm-lmi", true, task);

    private final UtLogger logger = new UtLogger(getClass());

    private LicenseManagerImpl()
    {
        /*
         * This shouldn't be in properties, it is too easy to override. *
         * this.timerDelay = Long.getLong(
         * "com.untangle.uvm.license.timerDelay", TIMER_DELAY );
         * this.validationPeriod = Long.getLong(
         * "com.untangle.uvm.license.validationPeriod", VALIDATION_PERIOD );
         */
    }

    /**
     * Reload all of the licenses from the file system.
     */
    @Override
    public final void reloadLicenses()
    {
        scheduleAndWait();
    }

    @Override
    public final License getLicense(String identifier)
    {
        License lic = this.licenseMap.get(identifier);
        if (lic == null)
            return new License(identifier, identifier, "Subscription", 0, 0, "invalid", 1, Boolean.FALSE);
        return lic;
    }

    @Override
    public final boolean hasPremiumLicense()
    {
        return this.licenseSettings.getLicenses().size() > 0;
    }

    /* --------------------- PACKAGE --------------------- */

    /* --------------------- PRIVATE --------------------- */

    /**
     * Read the licenses and load them into the current settings object
     */
    @SuppressWarnings("unchecked") //LinkedList<License> <-> LinkedList
    private synchronized void readLicenses()
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        LinkedList<License> licenses;
        
        try {
            licenses = settingsManager.loadBasePath(LinkedList.class, System.getProperty("uvm.conf.dir"), "licenses", "licenses");
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to read license file: ", e );
            return;
        }
        
        this.licenseSettings = new LicenseSettings(licenses);
    }

    /**
     * update the license map.
     */
    private synchronized void mapLicenses()
    {
        /* Create a new map of all of the valid licenses */
        Map<String, License> newMap = new HashMap<String, License>();

        for (License license : this.licenseSettings.getLicenses()) {
            if (!verifyLicense(license))
                continue;

            String identifier = license.getName();
            License current = newMap.get(identifier);

            /* current license is newer and better */
            if ((current != null) && (current.getEnd() > license.getEnd()))
                continue;

            newMap.put(identifier, license);
        }

        this.licenseMap = Collections.unmodifiableMap(newMap);
    }

    private String createSelfSignedLicenseKey(License license) throws NoSuchAlgorithmException
    {
        if (license.getKeyVersion() != SELF_SIGNED_VERSION)
            return null;

        MessageDigest digest = MessageDigest.getInstance("MD5");
        String firstKey = license.getName() + "-license-"
                + 0xDEC0DED + "-" + license.getType();

        digest.reset();
        byte[] data = digest.digest(firstKey.getBytes());
        firstKey = toHex(data) + "\n";
        String file = firstKey + buildLicenseFile(license);

        digest.reset();
        data = digest.digest(file.getBytes());
        return toHex(data);
    }

    private boolean verifyLicense(License license)
    {
        return verifyLicense(license, true);
    }

    @SuppressWarnings("fallthrough")
    private boolean verifyLicense(License license, boolean verifyExpiration)
    {
        int version = license.getKeyVersion();

        long now = System.currentTimeMillis();

        /* Verify the key hasn't already hasn't expired */
        if (license.getStart() > now) {
            logger.warn( "The license: " + license + " isn't valid yet." );
            return false;
        }

        if ((license.getEnd() < now) && verifyExpiration) {
            logger.warn( "The license: " + license + " has already expired." );
            return false;
        }

        /* Verify the duration lines up properly */
        long duration = license.getEnd() - license.getStart();

//         if (LicenseType.SUBSCRIPTION.equals(licenseType)) {
//             duration -= LICENSE_SUBSCRIPTION_EXTENSION;
//         }

//         switch (license.getType()) {
//         case "Trial":
//             /* fallthrough */
//         case "Trial14":
//             /* special case for the old license key duration */
//             if (duration == OLD_TRIAL_KEY_DURATION)
//                 break;
//             /* fallthrough */
//         default:
//             // XXX
//             // since there is no a single fucking line of documentation
//             // this will have to do until 8.1
//             // XXX
//             //if (licenseType.getDuration() != duration) {
//             //    logger.warn("bad duration: " + licenseType.getDuration() + " != " + duration);
//             //    return false;
//             //}
//         }

        switch (version) {
        case SELF_SIGNED_VERSION:
            // XXX
            // since there is no a single fucking line of documentation
            // this will have to do until 8.1
            // XXX
            return true;
            //             try {
            //                 String expected = createSelfSignedLicenseKey(license);
            //                 if (!expected.equals(license.getKey())) {
            //                     logger.debug( "Invalid license key for " +
            //                     // license );
            //                     logger.debug( "expected " + expected );
            //                     logger.debug( "key " + license.getKey());
            //                     return false;
            //                 }

            //                 return true;
            //             } catch (NoSuchAlgorithmException e) {
            //                 /* perhaps this should just return true for safety */
            //                 return false;
            //             }
        default:
            logger.warn( "Unknown key version: " + version );
            return false;
        }
    }

    private String buildLicenseFile(License license)
    {
        StringBuilder sb = new StringBuilder();
        /*
         * just a little string to make it slightly harder to guess the
         * algorithm
         */
        int version = license.getKeyVersion();
        switch (version) {
        case SELF_SIGNED_VERSION:
            /* presently the only one supported, fallthrough */

        default:
            long end = license.getEnd();
//             if (LicenseType.SUBSCRIPTION.getName().equals(license.getType())) {
//                 end -= LICENSE_SUBSCRIPTION_EXTENSION;
//             }
            sb.append("untangle: " + (0xC0DED + 0xDEC0DED) + "\n");
            sb.append("version: " + version + "\n");
            sb.append("type: " + license.getType() + "\n");
            sb.append("product: " + license.getName() + "\n");
            sb.append("start: " + license.getStart() + "\n");
            sb.append("end: " + end + "\n");
        }

        return sb.toString();
    }

    private String toHex(byte data[])
    {
        String response = "";
        for (byte b : data) {
            int c = b;
            if (c < 0)
                c = c + 0x100;
            response += String.format("%02x", c);
        }

        return response;
    }

    private String timeRemaining(long now, long expiration)
    {
        if (now > expiration)
            return EXPIRED;

        /* Calculate the number of days remaining */
        long days = (expiration - now) / ONE_DAY;

        switch ((int) days) {
        case 0:
            return "expires today";

        case 1:
            return "1 day remains";

        default:
            return "" + days + " days remain";
        }
    }

    private void scheduleAndWait()
    {
        /*
         * update all of the licenses for products, run all of them in the timer
         * task to avoid synchronization issues.
         */
        if (!this.pulse.beat(4000)) {
            logger.debug("unable to wait for the license task to complete.");
        }
    }

    /**
     * This starts the task beating at a fixed rate
     */
    private void scheduleFixedTask()
    {
        this.pulse.start(this.timerDelay);
    }

    /* statics */
    public static LicenseManager getInstance()
    {
        return INSTANCE;
    }

    private class LicenseUpdateTask implements Runnable
    {
        public void run() {
            logger.debug("testing licenses and updating products." );

            synchronized (LicenseManagerImpl.this) {
                readLicenses();
                mapLicenses();
            }

            synchronized (this) {
                /* notify any threads that are waiting on this */
                notifyAll();
            }
        }
    }

    static {
        /* Add the license to the map */
//         LICENSE_MAP.put(LicenseType.TRIAL.getName(), LicenseType.TRIAL);
//         LICENSE_MAP.put(LicenseType.TRIAL14.getName(), LicenseType.TRIAL14);
//         LICENSE_MAP.put(LicenseType.SUBSCRIPTION.getName(), LicenseType.SUBSCRIPTION);
//         LICENSE_MAP.put(LicenseType.DEVELOPMENT.getName(), LicenseType.DEVELOPMENT);

        INSTANCE = new LicenseManagerImpl();

        /*
         * This will actually run it twice, but the second call is blocking
         * until the licenses have been loaded
         */
        INSTANCE.scheduleFixedTask();
        INSTANCE.reloadLicenses();
    }
}
