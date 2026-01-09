/**
 * $Id: GeographyManagerImpl.java 42115 2016-01-15 17:56:36Z mahotz $
 */

package com.untangle.uvm;

import java.net.InetAddress;
import java.io.File;
import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;

import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.db.CHMCache;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;
import java.util.Optional;
import com.untangle.uvm.util.Pulse;

/**
 * The Geography Manager provides location information for IP addresses using
 * the free MaxMind GeoIP2 country database.
 */
public class GeographyManagerImpl implements GeographyManager
{
    private final Logger logger = LogManager.getLogger(getClass());

    private static final String GEOIP_DATABASE_FILE = "/var/cache/untangle-geoip/GeoLite2-Country.mmdb";
    private static final String GEOIP_PREVIOUS_FILE = "/var/cache/untangle-geoip/GeoLite2-Country.previous";
    private static final String GEOIP_UPDATE_FILE = "/var/cache/untangle-geoip/GeoLite2-Country.update";
    public static final String LOCAL_COUNTRY_CODE = "XL";
    public static final String UNKNOWN_COUNTRY_CODE = "XU";

    // we check for the update file once per hour which is more than enough
    private static final long DATABASE_CHECK_FREQUENCY = (60 * 60 * 1000L);

    private DatabaseReader databaseReader = null;
    private Pulse databaseChecker = null;
    private File databaseFile = null;
    private CHMCache chmCache = null;
    private boolean initFlag = false;

    /**
     * Constructor
     */
    protected GeographyManagerImpl()
    {
        checkForDatabaseUpdate(false);
        openDatabaseInstance();
        databaseChecker = new Pulse("GeographyManagerUpdater", new DatabaseUpdateChecker(this), DATABASE_CHECK_FREQUENCY);
        databaseChecker.start();
    }

    /**
     * Get the country name for an IP address
     * 
     * @param netAddress
     *        The address
     * @return The country name
     */
    @Override
    public String getCountryName(String netAddress)
    {
        if (!initFlag) return (null);

        CountryResponse response = getCountryObject(netAddress);
        if (response == null) return (null);
        Country country = response.getCountry();
        if (country == null) return (null);
        return (country.getName());
    }

    /**
     * Get the country code for an IP address
     * 
     * @param netAddress
     *        The address
     * @return The country code
     */
    @Override
    public String getCountryCode(String netAddress)
    {
        if (!initFlag) return (null);

        CountryResponse response = getCountryObject(netAddress);
        if (response == null) return UNKNOWN_COUNTRY_CODE;
        Country country = response.getCountry();
        if (country == null) return UNKNOWN_COUNTRY_CODE;
        String isoCode = country.getIsoCode();
        if (isoCode == null) return UNKNOWN_COUNTRY_CODE;
        return isoCode;
    }

    /**
     * Private function called by the public functions to get the country object
     * associated with an IP address. The country object holds all the fields
     * that we can return.
     *
     * @param netAddress
     *        The address
     * @return The corresponding city object, or null if no match
     */
    private CountryResponse getCountryObject(String netAddress)
    {
        try {
            InetAddress inetAddress = InetAddress.getByName(netAddress);
            Optional<CountryResponse> response = databaseReader.tryCountry(inetAddress);
            if (response.isPresent() ) return (response.get());
        } catch (Exception exn) {
            logger.debug("Exception getting database object for {}", netAddress, exn);
        }
        return (null);
    }

    /**
     * Opens the database instance
     */
    private void openDatabaseInstance()
    {
        try {
            chmCache = new CHMCache();
            databaseFile = new File(GEOIP_DATABASE_FILE);
            databaseReader = new DatabaseReader.Builder(databaseFile).withCache(chmCache).build();
            String databaseType = databaseReader.getMetadata().getDatabaseType();
            Date databaseDate = databaseReader.getMetadata().getBuildDate();
            initFlag = true;
            logger.info("Successfully loaded {} database created {}", databaseType, databaseDate);
        } catch (Exception exn) {
            logger.warn("Exception initializing geography manager database", exn);
        }
    }

    /**
     * Checks for database updates and optionally loads the updated file.
     * 
     * @param loadFlag
     *        True to load the new database, false to check and download only.
     * @return True if an update was found, otherwise false
     */
    @Override
    public boolean checkForDatabaseUpdate(boolean loadFlag)
    {
        // check for update file and return false if not found
        File updateFile = new File(GEOIP_UPDATE_FILE);
        if (!updateFile.exists()) return (false);

        logger.info("Database update file detected.");

        // we have an update file so clear the init flag and close existing
        if (initFlag) {
            initFlag = false;
            try {
                databaseReader.close();
            } catch (Exception exn) {
                logger.warn("Exception closing geography manager database", exn);
            }
        }

        // remove the previous backup file
        UvmContextFactory.context().execManager().exec("/bin/rm " + GEOIP_PREVIOUS_FILE);

        // move the existing database file to the backup file
        UvmContextFactory.context().execManager().exec("/bin/mv " + GEOIP_DATABASE_FILE + " " + GEOIP_PREVIOUS_FILE);

        // move the update database file to the active database file
        UvmContextFactory.context().execManager().exec("/bin/mv " + GEOIP_UPDATE_FILE + " " + GEOIP_DATABASE_FILE);

        // open the new database if the load flag is true
        if (loadFlag) openDatabaseInstance();
        return (true);
    }

    /**
     * Gets the GeoIP stats for all active sessions
     * 
     * @return The session stats
     */
    public JSONObject[] getGeoSessionStats()
    {
        List<SessionMonitorEntry> sessions = UvmContextFactory.context().sessionMonitor().getMergedSessions();

        HashMap<String, JSONObject> coordinatesMap = new HashMap<>();

        for (SessionMonitorEntry session : sessions) {
            String country = null;
            Double bps;

            country = session.getClientCountry() != null ? session.getClientCountry() : session.getServerCountry();

            // if this session has no country (on either client or server, just skip it)
            if (country == null) continue;
            
            if (session.getTotalBps() == null) bps = 0.0;
            else bps = session.getTotalBps();
            JSONObject value = coordinatesMap.get(country);

            // if this is no existing entry for this country, create one
            if (value == null) {
                JSONObject newValue = createCoordinatesValue(country, 1, bps);
                coordinatesMap.put(country, newValue);
            }
            // if one exists, just increment the count and add to the bps value
            else {
                try {
                    int oldCount = value.getInt("sessionCount");
                    double oldbps = value.getDouble("bps");
                    value.put("sessionCount", oldCount + 1);
                    value.put("bps", oldbps + bps);
                } catch (Exception e) {
                    logger.warn("Exception", e);
                }
            }
        }

        Collection<JSONObject> values = coordinatesMap.values();
        return values.toArray(new JSONObject[0]);
    }

    /**
     * Private function called to create a JSON object holding GeoIP coordinates
     * and other location information.
     *
     * @param country
     *        The country
     * @param sessionCount
     *        The session count
     * @param bps
     *        The amount of traffic
     * @return The JSON object
     */
    private JSONObject createCoordinatesValue(String country, int sessionCount, Double bps)
    {
        JSONObject json = new JSONObject();

        try {
            if (country != null) json.put("country", country);
            json.put("sessionCount", sessionCount);
            json.put("bps", bps);
        } catch (Exception e) {
            logger.warn("Exception", e);
            return null;
        }

        return json;
    }

    /**
     * The runnable class that handles period database update checks.
     * 
     * @author mahotz
     * 
     */
    private static class DatabaseUpdateChecker implements Runnable
    {
        GeographyManagerImpl owner;

        /**
         * Constructor
         * 
         * @param owner
         *        The owner application
         */
        public DatabaseUpdateChecker(GeographyManagerImpl owner)
        {
            this.owner = owner;
        }

        /**
         * Main run function
         */
        public void run()
        {
            owner.checkForDatabaseUpdate(true);
        }
    }
}
