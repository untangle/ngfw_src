/*
 * $Id: GeographyManagerImpl.java 42115 2016-01-15 17:56:36Z mahotz $
 */

package com.untangle.uvm;

import com.untangle.uvm.GeographyManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.Pulse;

import com.maxmind.geoip2.record.Subdivision;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.db.CHMCache;
import org.apache.log4j.Logger;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.URL;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.Date;

public class GeographyManagerImpl implements GeographyManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private final static String GEOIP_DATABASE_FILE = "/var/cache/untangle-geoip/GeoLite2-City.mmdb";
    private final static String GEOIP_PREVIOUS_FILE = "/var/cache/untangle-geoip/GeoLite2-City.previous";
    private final static String GEOIP_UPDATE_FILE = "/var/cache/untangle-geoip/GeoLite2-City.update";

    private static final String CLOUD_IP_DETECTION_URL = "http://www.untangle.com/ddclient/ip.php";

    // we check for the update file once per hour which is more than enough
    private final static long DATABASE_CHECK_FREQUENCY = (60 * 60 * 1000L);

    private DatabaseReader databaseReader = null;
    private Pulse databaseChecker = null;
    private File databaseFile = null;
    private CHMCache chmCache = null;
    private boolean initFlag = false;

    protected GeographyManagerImpl()
    {
        openDatabaseInstance();
        databaseChecker = new Pulse("GeographyManagerUpdater", true, new DatabaseUpdateChecker(this));
        databaseChecker.start(DATABASE_CHECK_FREQUENCY);
    }

    public String getCountryName(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);
        Country country = response.getCountry();
        if (country == null) return (null);
        return (country.getName());
    }

    public String getCountryCode(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);
        Country country = response.getCountry();
        if (country == null) return (null);
        return (country.getIsoCode());
    }

    public String getSubdivisionName(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);
        Subdivision subdivision = response.getMostSpecificSubdivision();
        if (subdivision == null) return (null);
        return (subdivision.getName());
    }

    public String getSubdivisionCode(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);
        Subdivision subdivision = response.getMostSpecificSubdivision();
        if (subdivision == null) return (null);
        return (subdivision.getIsoCode());
    }

    public String getCityName(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);
        City city = response.getCity();
        if (city == null) return (null);
        return (city.getName());
    }

    public String getPostalCode(String netAddress)
    {
        if (!initFlag) return (null);

        CityResponse response = getCityObject(netAddress);
        if (response == null) return (null);
        Postal postal = response.getPostal();
        if (postal == null) return (null);
        return (postal.getCode());
    }

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
            if (location.getLongitude() != null) coordinates.longitude = location.getLongitude.doubleValue();
        }

        return (coordinates);
    }

    public String detectPublicNetworkAddress()
    {
        try {
            URL myurl = new URL(CLOUD_IP_DETECTION_URL + "?activation=" + UvmContextFactory.context().getServerUID());
            HttpURLConnection mycon = (HttpURLConnection) myurl.openConnection();
            mycon.setRequestMethod("GET");
            mycon.setRequestProperty("User-Agent", "Untangle NGFW GeographyManager");
            mycon.setDoOutput(false);
            mycon.setDoInput(true);
            mycon.connect();

            DataInputStream input = new DataInputStream(mycon.getInputStream());
            StringBuilder builder = new StringBuilder(256);

            for (int c = input.read(); c != -1; c = input.read()) {
                builder.append((char) c);
            }

            input.close();
            mycon.disconnect();

            return (builder.toString());
        } catch (Exception exn) {
            return (null);
        }
    }

    public Coordinates getPublicNetworkAddressCoordinates()
    {
        if (!initFlag) return (null);

        String netAddress = detectPublicNetworkAddress();
        if (netAddress == null) return (null);
        return (getCoordinates(netAddress));
    }

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

    public boolean checkForDatabaseUpdate()
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

        // open the new database
        openDatabaseInstance();
        return (true);
    }

    private static class DatabaseUpdateChecker implements Runnable
    {
        GeographyManagerImpl owner;

        public DatabaseUpdateChecker(GeographyManagerImpl owner)
        {
            this.owner = owner;
        }

        public void run()
        {
            owner.checkForDatabaseUpdate();
        }
    }
}
