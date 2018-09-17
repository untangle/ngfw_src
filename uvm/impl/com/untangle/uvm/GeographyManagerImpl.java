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

import com.maxmind.geoip2.record.Subdivision;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.db.CHMCache;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.GeographyManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.Pulse;

/**
 * The Geography Manager provides location information for IP addresses using
 * the free MaxMind GeoIP2 city database.
 */
public class GeographyManagerImpl implements GeographyManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private final static String GEOIP_DATABASE_FILE = "/var/cache/untangle-geoip/GeoLite2-City.mmdb";
    private final static String GEOIP_PREVIOUS_FILE = "/var/cache/untangle-geoip/GeoLite2-City.previous";
    private final static String GEOIP_UPDATE_FILE = "/var/cache/untangle-geoip/GeoLite2-City.update";
    private final static String LOCAL_COUNTRY_CODE = "XL";
    private final static String UNKNOWN_COUNTRY_CODE = "XU";

    // we check for the update file once per hour which is more than enough
    private final static long DATABASE_CHECK_FREQUENCY = (60 * 60 * 1000L);

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
    public String getCountryName(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
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
    public String getCountryCode(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return UNKNOWN_COUNTRY_CODE;
        Country country = response.getCountry();
        if (country == null) return UNKNOWN_COUNTRY_CODE;
        String isoCode = country.getIsoCode();
        if (isoCode == null) return UNKNOWN_COUNTRY_CODE;
        return isoCode;
    }

    /**
     * Get the subdivision name for an IP address
     * 
     * @param netAddress
     *        The address
     * @return The subdivision name
     */
    public String getSubdivisionName(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);
        Subdivision subdivision = response.getMostSpecificSubdivision();
        if (subdivision == null) return (null);
        return (subdivision.getName());
    }

    /**
     * Get the subdivision code for an IP address
     * 
     * @param netAddress
     *        The address
     * @return The subdivision code
     */
    public String getSubdivisionCode(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);
        Subdivision subdivision = response.getMostSpecificSubdivision();
        if (subdivision == null) return (null);
        return (subdivision.getIsoCode());
    }

    /**
     * Get the city name for an IP address
     * 
     * @param netAddress
     *        The address
     * @return The city name
     */
    public String getCityName(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);
        City city = response.getCity();
        if (city == null) return (null);
        return (city.getName());
    }

    /**
     * Get the postal code for an IP address
     * 
     * @param netAddress
     *        The address
     * @return The postal code
     */
    public String getPostalCode(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);
        Postal postal = response.getPostal();
        if (postal == null) return (null);
        return (postal.getCode());
    }

    /**
     * Get the coordinates for an IP address
     * 
     * @param netAddress
     *        The address
     * @return The coordinates
     */
    public Coordinates getCoordinates(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);

        Location location = response.getLocation();
        Country country = response.getCountry();

        Coordinates coordinates = new Coordinates();

        if (country != null) {
            coordinates.country = country.getIsoCode();
        }

        if (location != null) {
            if (location.getLatitude() != null) coordinates.latitude = location.getLatitude().doubleValue();
            if (location.getLongitude() != null) coordinates.longitude = location.getLongitude().doubleValue();
        }

        return (coordinates);
    }

    /**
     * Private function called by the public functions to get the city object
     * associated with an IP address. The city object holds all of the fields
     * that we can return.
     * 
     * @param netAddress
     *        The address
     * @return The corresponding city object, or null if no match
     */
    private CityResponse getCityObject(String netAddress)
    {
        try {
            InetAddress inetAddress = InetAddress.getByName(netAddress);
            CityResponse response = databaseReader.city(inetAddress);
            return (response);
        } catch (Exception exn) {
            logger.debug("Exception getting database object for " + netAddress, exn);
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
            logger.info("Successfully loaded " + databaseType + " database created " + databaseDate.toString());
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

        HashMap<Double, JSONObject> coordinatesMap = new HashMap<Double, JSONObject>();

        for (SessionMonitorEntry session : sessions) {
            Double coordinatesKey = null;
            Double latitude = null;
            Double longitude = null;
            String country = null;
            Double kbps;

            if (coordinatesKey == null && session.getClientLatitude() != null && session.getClientLongitude() != null) {
                latitude = session.getClientLatitude();
                longitude = session.getClientLongitude();
                country = session.getClientCountry();
                coordinatesKey = getCoordinatesKey(latitude, longitude);
            }
            if (coordinatesKey == null && session.getServerLatitude() != null && session.getServerLongitude() != null) {
                latitude = session.getServerLatitude();
                longitude = session.getServerLongitude();
                country = session.getServerCountry();
                coordinatesKey = getCoordinatesKey(latitude, longitude);
            }
            // if this session has no coordinates (on either client or server, just skip it)
            if (coordinatesKey == null) continue;

            if (session.getTotalKBps() == null) kbps = 0.0;
            else kbps = session.getTotalKBps().doubleValue();

            JSONObject value = coordinatesMap.get(coordinatesKey);

            // if this is no existing entry for this lat & longitutde, create one
            if (value == null) {
                JSONObject newValue = createCoordinatesValue(latitude, longitude, country, 1, kbps);
                coordinatesMap.put(coordinatesKey, newValue);
            }
            // if one exists, just increment the count and add to the kbps value
            else {
                try {
                    int oldCount = value.getInt("sessionCount");
                    double oldKbps = value.getDouble("kbps");
                    value.put("sessionCount", oldCount + 1);
                    value.put("kbps", oldKbps + kbps);
                } catch (Exception e) {
                    logger.warn("Exception", e);
                }
            }
        }

        Collection<JSONObject> values = coordinatesMap.values();
        JSONObject[] jsonValues = values.toArray(new JSONObject[0]);
        return jsonValues;
    }

    /**
     * Private function called to create a JSON object holding GeoIP coordinates
     * and other location information.
     * 
     * @param latitude
     *        The latitude
     * @param longitude
     *        The longitude
     * @param country
     *        The country
     * @param sessionCount
     *        The session count
     * @param kbps
     *        The amount of traffic
     * @return The JSON object
     */
    private JSONObject createCoordinatesValue(Double latitude, Double longitude, String country, int sessionCount, Double kbps)
    {
        JSONObject json = new JSONObject();

        try {
            json.put("latitude", latitude);
            json.put("longitude", longitude);
            if (country != null) json.put("country", country);
            json.put("sessionCount", sessionCount);
            json.put("kbps", kbps);
        } catch (Exception e) {
            logger.warn("Exception", e);
            return null;
        }

        return json;
    }

    /**
     * Private function to combine latitude and longitude into a key value
     * 
     * @param latitude
     *        The latitude
     * @param longitude
     *        The longitude
     * @return The key value
     */
    private Double getCoordinatesKey(Double latitude, Double longitude)
    {
        return latitude + 1024.0 * longitude;
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
