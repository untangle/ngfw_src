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

public class LicenseManagerImpl implements LicenseManager
{
    private static final int SELF_SIGNED_VERSION = 0x1F;

    private static final String LICENSE_DIRECTORY = System.getProperty("uvm.conf.dir") + "/licenses";
    private static final String PROPERTY_BASE = "com.untangle.uvm.license";
    private static final String PROPERTY_PRODUCT_IDENTIFIER = PROPERTY_BASE + ".identifier";
    private static final String PROPERTY_LICENSE_TYPE = PROPERTY_BASE + ".type";
    /* The package that this product is in, this is for mcli extraname */
    private static final String PROPERTY_MACKAGE = PROPERTY_BASE + ".mackage";
    private static final String PROPERTY_START = PROPERTY_BASE + ".start";
    private static final String PROPERTY_END = PROPERTY_BASE + ".end";
    private static final String PROPERTY_KEY = PROPERTY_BASE + ".key";
    private static final String PROPERTY_KEY_VERSION = PROPERTY_BASE + ".keyVersion";

    private static final long LICENSE_SUBSCRIPTION_EXTENSION = (1000l * 60 * 60 * 24 * 365 * 8) + 835l;

    /* 30 day keys */
    private static final long OLD_TRIAL_KEY_DURATION = (1000l * 60 * 60 * 24 * 30) + 539;

    private static final String EXPIRED = "expired";

    /* This is a map from the license to how long it should be */
    private static final Map<String, LicenseType> LICENSE_MAP = new HashMap<String, LicenseType>();

    /* This maps the identifier to a mackage. */
    private static final Map<String, String> IDENTIFIER_TO_MACKAGE_MAP = new HashMap<String, String>();

    /* This maps a mackage to an identifier. */
    private static final Map<String, String> MACKAGE_TO_IDENTIFIER_MAP = new HashMap<String, String>();

    /* These are the various license types */
    private static enum LicenseType {
        TRIAL14("14 Day Trial", 1000l * 60 * 60 * 24 * 15 + 278),
        TRIAL("30 Day Trial", 1000l * 60 * 60 * 24 * 31 + 539),
        SUBSCRIPTION("Subscription", 1000l * 60 * 60 * 24 * 365 * 2 + 344),
        DEVELOPMENT("Untangle Development", 1000l * 60 * 60 * 24 * 60 + 960);

        private final String name;
        private final long duration;

        private LicenseType(String name, long duration) {
            this.name = name;
            this.duration = duration;
        }

        public String getName() {
            return this.name;
        }

        public long getDuration() {
            return this.duration;
        }
    }

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

    /** A set of all of the products to update. */
    private final Set<Product> products = new HashSet<Product>();

    /**
     * A set of all of the products to were notified last time. (This is to work
     * around the race condition on the pulse class that doesn't guarantee the
     * pulse fires after the method starts. 5876
     */
    private final Set<Product> notifiedProducts = new HashSet<Product>();

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

    /* Lazily initialized */
    private String standardLicense = null;
    private String professionalLicense = null;

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
     * Register a product with the license manager.
     * 
     * @param product
     *            The product to register.
     */
    @Override
    public final void register(Product product)
    {
        synchronized (this) {
            this.products.add(product);
        }

        /* schedule the task and wait for it to return */
        scheduleAndWait();

        synchronized (this) {
            if (this.notifiedProducts.size() == this.products.size()) {
                return;
            }
        }

        /* run it again just in case the new product was missed. */
        scheduleAndWait();
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
    public final LicenseStatus getLicenseStatus(String identifier)
    {
        /* The mackage map contains both expired and unexpired licenses. */
        return getStatus(identifier, null);
    }

    @Override
    public final LicenseStatus getMackageStatus(String mackageName)
    {
        /* The mackage map contains both expired and unexpired licenses. */
        String identifier = MACKAGE_TO_IDENTIFIER_MAP.get(mackageName);
        if (identifier == null)
            identifier = "unknown";
        return getStatus(identifier, mackageName);
    }

    @Override
    public final boolean hasPremiumLicense()
    {
        return this.licenseSettings.getLicenses().size() > 0;
    }

    /* --------------------- PACKAGE --------------------- */

    /* --------------------- PRIVATE --------------------- */
    private synchronized LicenseStatus getStatus(String identifier, String mackageName)
    {
        License license = this.licenseMap.get(identifier);

        if (mackageName == null)
            mackageName = IDENTIFIER_TO_MACKAGE_MAP.get(identifier);
        if (mackageName == null)
            mackageName = "unknown";

        /* Found a valid license */
        if (license != null)
            return makeLicenseStatus(identifier, mackageName, license);

        /* search for the newest license */
        for (License l : this.licenseSettings.getLicenses()) {
            if (!l.getProductIdentifier().equals(identifier))
                continue;

            /* newer license */
            if ((license != null) && (license.getEnd() > l.getEnd()))
                continue;

            /* use this license */
            license = l;
        }

        /* Expired license exists (function is designed to properly handle null */
        return makeLicenseStatus(identifier, mackageName, license);
    }

    /**
     * Read the licenses and load them into the current settings object
     */
    private synchronized void readLicenses()
    {
        /* Open up the directory with all of the licenses */
        File licenseDirectory = new File(LICENSE_DIRECTORY);

        /* nothing to do */
        if (!licenseDirectory.exists() || !licenseDirectory.isDirectory()) {
            this.licenseSettings = new LicenseSettings(new ArrayList<License>());
            return;
        }

        File[] licenseFiles = licenseDirectory.listFiles(LICENSE_FILE_FILTER);

        if (licenseFiles == null) {
            this.licenseSettings = new LicenseSettings(new ArrayList<License>());
            return;
        }

        List<License> licenses = new LinkedList<License>();

        for (File licenseFile : licenseFiles) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(licenseFile));
                String identifier = props
                        .getProperty(PROPERTY_PRODUCT_IDENTIFIER);
                if (identifier == null)
                    throw new Exception("missing product id");
                String mackage = props.getProperty(PROPERTY_MACKAGE);
                if (mackage == null)
                    throw new Exception("missing mackage id");
                String type = props.getProperty(PROPERTY_LICENSE_TYPE);
                if (type == null)
                    throw new Exception("missing license type");
                long start = Long.parseLong(props.getProperty(PROPERTY_START));
                if (start == 0)
                    throw new Exception("missing start date");
                long end = Long.parseLong(props.getProperty(PROPERTY_END));
                if (end == 0)
                    throw new Exception("missing end date");
                String key = props.getProperty(PROPERTY_KEY);
                if (key == null)
                    throw new Exception("missing key");
                int keyVersion = Integer.parseInt(props
                        .getProperty(PROPERTY_KEY_VERSION));
                if (keyVersion == 0)
                    throw new Exception("invalid key version");

                /* Make a special case for subscriptions */
                if (LicenseType.SUBSCRIPTION.getName().equals(type)) {
                    end += LICENSE_SUBSCRIPTION_EXTENSION;
                }

                /* add the license, if it passes verification. */
                License license = new License(identifier, mackage, type, start,
                        end, key, keyVersion);

                /* Verify the license */
                if (!verifyLicense(license, false)) {
                    // NO_DEBUG_IN_LOGGING logger.debug( "The license: " +
                    // license + " is not valid." );
                    continue;
                }

                licenses.add(license);

                // NO_DEBUG_IN_LOGGING logger.debug( "loaded " + license );
            } catch (Exception e) {
                /* get rid of the logging before release */
                // NO_DEBUG_IN_LOGGING logger.warn( "Unable to license from " +
                // licenseFile, e );
            }
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

            String identifier = license.getProductIdentifier();
            License current = newMap.get(identifier);

            /* current license is newer and better */
            if ((current != null) && (current.getEnd() > license.getEnd()))
                continue;

            newMap.put(identifier, license);
        }

        this.licenseMap = Collections.unmodifiableMap(newMap);
    }

    /**
     * Notify the products.
     */
    private synchronized void notifyProducts()
    {

        long nextExpirationDate = (System.nanoTime() / 1000000l)
                + this.validationPeriod;

        this.notifiedProducts.clear();
        this.notifiedProducts.addAll(this.products);

        for (Product product : this.products) {
            try {
                String identifier = product.identifier();

                License license = this.licenseMap.get(identifier);

                if (license == null) {
                    if (product.isActivated())
                        product.expire();
                } else {
                    // NO_DEBUG_IN_LOGGING logger.debug(
                    // "Updating with the license: " + license );

                    /*
                     * by always using the nextExpirationDate, the product won't
                     * expire before the expiration date, therefore although
                     * they will get the product for a little longer there is no
                     * need to schedule a separate task
                     */
                    product.updateExpirationDate(nextExpirationDate);
                }
            } catch (Exception e) {
                logger.warn("error notifying a product.", e);
            }
        }
    }

    private synchronized void notifyClients()
    {
        Map<String, LicenseStatus> identifierMap = new HashMap<String, LicenseStatus>();
        Map<String, LicenseStatus> mackageMap = new HashMap<String, LicenseStatus>();

        for (Map.Entry<String, License> entry : this.licenseMap.entrySet()) {
            String identifier = entry.getKey();
            License license = entry.getValue();

            String mackageName = IDENTIFIER_TO_MACKAGE_MAP.get(identifier);
            if (mackageName == null)
                mackageName = "unknown";

            LicenseStatus status = makeLicenseStatus(identifier, mackageName,
                    license);

            identifierMap.put(identifier, status);
            if ("unknown".equals(mackageName))
                continue;

            mackageMap.put(mackageName, status);
        }

        LicenseUpdateMessage message = new LicenseUpdateMessage(identifierMap,
                mackageMap);
        LocalUvmContextFactory.context().localMessageManager().submitMessage(
                message);
    }

    private String createSelfSignedLicenseKey(License license) throws NoSuchAlgorithmException
    {
        if (license.getKeyVersion() != SELF_SIGNED_VERSION)
            return null;

        MessageDigest digest = MessageDigest.getInstance("MD5");
        String firstKey = license.getProductIdentifier() + "-license-"
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
            // NO_DEBUG_IN_LM logger.debug( "The license: " + license +
            // " isn't valid yet." );
            return false;
        }

        if ((license.getEnd() < now) && verifyExpiration) {
            // NO_DEBUG_IN_LM logger.debug( "The license: " + license +
            // " has already expired." );
            return false;
        }

        /* verify the license type is valid */
        LicenseType licenseType = LICENSE_MAP.get(license.getType());
        if (licenseType == null) {
            // NO_DEBUG_IN_LM logger.debug( "unknown license type: '" +
            // license.getType() + "'" );
            return false;
        }

        /* Verify the duration lines up properly */
        long duration = license.getEnd() - license.getStart();

        if (LicenseType.SUBSCRIPTION.equals(licenseType)) {
            duration -= LICENSE_SUBSCRIPTION_EXTENSION;
        }

        switch (licenseType) {
        case TRIAL:
            /* fallthrough */
        case TRIAL14:
            /* special case for the old license key duration */
            if (duration == OLD_TRIAL_KEY_DURATION)
                break;
            /* fallthrough */
        default:
            if (licenseType.getDuration() != duration)
                return false;
        }

        switch (version) {
        case SELF_SIGNED_VERSION:
            try {
                String expected = createSelfSignedLicenseKey(license);
                if (!expected.equals(license.getKey())) {
                    // NO_DEBUG_IN_LM logger.debug( "Invalid license key for " +
                    // license );
                    // NO_DEBUG_IN_LM logger.debug( "expected " + expected );
                    // NO_DEBUG_IN_LM logger.debug( "key " + license.getKey());
                    return false;
                }

                return true;
            } catch (NoSuchAlgorithmException e) {
                /* perhaps this should just return true for safety */
                return false;
            }

        default:
            // NO_DEBUG_IN_LM logger.warn( "Unknown key version: " + version );
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
            if (LicenseType.SUBSCRIPTION.getName().equals(license.getType())) {
                end -= LICENSE_SUBSCRIPTION_EXTENSION;
            }
            sb.append("untangle: " + (0xC0DED + 0xDEC0DED) + "\n");
            sb.append("version: " + version + "\n");
            sb.append("type: " + license.getType() + "\n");
            sb.append("product: " + license.getProductIdentifier() + "\n");
            sb.append("start: " + license.getStart() + "\n");
            sb.append("end: " + end + "\n");
        }

        return sb.toString();
    }

    private String toHex(byte data[])
    {
        String response = "";
        for (byte b : data) {
            int c = (int) b;
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

    private LicenseStatus makeLicenseStatus(String identifier, String mackageName, License license)
    {
        if (license == null) {
            if (mackageName == null)
                mackageName = "no-mackage";
            if (identifier == null)
                identifier = "unknown";

            return new LicenseStatus(false, identifier, mackageName, "invalid",
                    new Date(0), EXPIRED, false);
        }

        if (mackageName == null)
            mackageName = license.getMackage();
        if (identifier == null)
            identifier = license.getProductIdentifier();

        long end = license.getEnd();
        String timeLeft = timeRemaining(System.currentTimeMillis(), end);
        String type = license.getType();
        boolean isTrial = type.equals(LicenseType.TRIAL14.getName())
            || type.equals(LicenseType.TRIAL.getName());

        return new LicenseStatus(true, identifier, mackageName, license
                .getType(), new Date(end), timeLeft, isTrial);
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
            // NO_DEBUG_IN_LOGGING logger.debug(
            // "testing licenses and updating products." );

            synchronized (LicenseManagerImpl.this) {
                readLicenses();
                mapLicenses();
                notifyProducts();
                notifyClients();
            }

            synchronized (this) {
                /* notify any threads that are waiting on this */
                notifyAll();
            }
        }
    }

    private static void addMackageMap(String mackageName, String identifier)
    {
        IDENTIFIER_TO_MACKAGE_MAP.put(identifier, mackageName);
        MACKAGE_TO_IDENTIFIER_MAP.put(mackageName, identifier);
    }

    static {
        /* Add the license to the map */
        LICENSE_MAP.put(LicenseType.TRIAL.getName(), LicenseType.TRIAL);
        LICENSE_MAP.put(LicenseType.TRIAL14.getName(), LicenseType.TRIAL14);
        LICENSE_MAP.put(LicenseType.SUBSCRIPTION.getName(), LicenseType.SUBSCRIPTION);
        LICENSE_MAP.put(LicenseType.DEVELOPMENT.getName(), LicenseType.DEVELOPMENT);

        addMackageMap("untangle-node-adconnector", ProductIdentifier.PHONE_BOOK);
        addMackageMap("untangle-node-policy", ProductIdentifier.POLICY_MANAGER);
        addMackageMap("untangle-node-kav", ProductIdentifier.KASPERSKY_AV);
        addMackageMap("untangle-node-portal", ProductIdentifier.PORTAL);
        addMackageMap("untangle-node-license", "untangle-license-manager");
        addMackageMap("untangle-node-branding", ProductIdentifier.BRANDING_MANAGER);
        addMackageMap("untangle-node-boxbackup", ProductIdentifier.CONFIGURATION_BACKUP);
        addMackageMap("untangle-node-pcremote", ProductIdentifier.PC_REMOTE);
        addMackageMap("untangle-node-sitefilter", ProductIdentifier.SITEFILTER);
        addMackageMap("untangle-node-commtouch", ProductIdentifier.COMMTOUCH);
        addMackageMap("untangle-node-splitd", ProductIdentifier.SPLITD);
        addMackageMap("untangle-node-faild", ProductIdentifier.FAILD);

        INSTANCE = new LicenseManagerImpl();

        /*
         * This will actually run it twice, but the second call is blocking
         * until the licenses have been loaded
         */
        INSTANCE.scheduleFixedTask();
        INSTANCE.reloadLicenses();
    }
}
