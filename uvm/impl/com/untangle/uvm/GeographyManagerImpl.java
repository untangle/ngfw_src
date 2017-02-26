/*
 * $Id: GeographyManagerImpl.java 42115 2016-01-15 17:56:36Z mahotz $
 */

package com.untangle.uvm;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.URL;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
        databaseChecker = new Pulse("GeographyManagerUpdater", new DatabaseUpdateChecker(this), DATABASE_CHECK_FREQUENCY);
        databaseChecker.start();
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
        if (response == null) return "XU";
        Country country = response.getCountry();
        if (country == null) return "XU";
        String isoCode = country.getIsoCode();
        if (isoCode == null) return "XU";
        return isoCode;
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
            if (location.getLongitude() != null) coordinates.longitude = location.getLongitude().doubleValue();
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

    public JSONObject[] getGeoSessionStats()
    {
        List<SessionMonitorEntry> sessions = UvmContextFactory.context().sessionMonitor().getMergedSessions();

        HashMap<Double,JSONObject> coordinatesMap = new HashMap<Double,JSONObject>();
        
        for ( SessionMonitorEntry session : sessions ) {
            Double coordinatesKey = null;
            Double latitude = null;
            Double longitude = null;
            String country = null;
            Double kbps;
            
            if ( coordinatesKey == null && session.getClientLatitude() != null && session.getClientLongitude() != null ) {
                latitude = session.getClientLatitude();
                longitude = session.getClientLongitude();
                country = session.getClientCountry();
                coordinatesKey = getCoordinatesKey( latitude, longitude );
            }
            if ( coordinatesKey == null && session.getServerLatitude() != null && session.getServerLongitude() != null ) {
                latitude = session.getServerLatitude();
                longitude = session.getServerLongitude();
                country = session.getServerCountry();
                coordinatesKey = getCoordinatesKey( latitude, longitude );
            }
            // if this session has no coordinates (on either client or server, just skip it)
            if ( coordinatesKey == null )
                continue;

            if ( session.getTotalKBps() == null )
                kbps = 0.0;
            else
                kbps = session.getTotalKBps().doubleValue();

            JSONObject value = coordinatesMap.get( coordinatesKey );

            // if this is no existing entry for this lat & longitutde, create one
            if ( value == null ) {
                JSONObject newValue = createCoordinatesValue(  latitude, longitude, country, 1, kbps );
                coordinatesMap.put( coordinatesKey, newValue );
            }
            // if one exists, just increment the count and add to the kbps value
            else {
                try {
                    int oldCount = value.getInt("sessionCount");
                    double oldKbps = value.getDouble("kbps");
                    value.put("sessionCount",oldCount+1);
                    value.put("kbps",oldKbps+kbps);
                } catch (Exception e) {
                    logger.warn("Exception",e);
                }
            }
        }

        Collection<JSONObject> values = coordinatesMap.values();
        JSONObject[] jsonValues = values.toArray(new JSONObject[0]);
        return jsonValues;
    }

    private JSONObject createCoordinatesValue( Double latitude, Double longitude, String country, int sessionCount, Double kbps )
    {
        JSONObject json = new JSONObject();

        try {
            json.put("latitude",latitude);
            json.put("longitude",longitude);
            if ( country != null )
                json.put("country",country);
            json.put("sessionCount",sessionCount);
            json.put("kbps",kbps);
        } catch (Exception e) {
            logger.warn("Exception",e);
            return null;
        }

        return json;
    }
    
    private Double getCoordinatesKey( Double latitude, Double longitude )
    {
        return latitude + 1024.0*longitude;
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
