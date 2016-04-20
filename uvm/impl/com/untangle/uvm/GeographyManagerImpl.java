/*
 * $Id: GeographyManagerImpl.java 42115 2016-01-15 17:56:36Z mahotz $
 */

package com.untangle.uvm;

import com.untangle.uvm.GeographyManager;
import com.maxmind.geoip2.record.Subdivision;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.db.CHMCache;
import org.apache.log4j.Logger;
import java.net.InetAddress;
import java.io.File;

public class GeographyManagerImpl implements GeographyManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private DatabaseReader databaseReader;
    private File databaseFile;
    private CHMCache chmCache;
    private boolean initFlag = false;

    protected GeographyManagerImpl()
    {
        try {
            chmCache = new CHMCache();
            databaseFile = new File("GeoLite2-City.mmdb");
            databaseReader = new DatabaseReader.Builder(databaseFile).withCache(chmCache).build();
            initFlag = true;
        } catch (Exception exn) {
            logger.warn("Exception initializing geography manager", exn);
        }
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
        if (location == null) return (null);
        Coordinates coordinates = new Coordinates();
        coordinates.latitude = location.getLatitude();
        coordinates.longitude = location.getLongitude();
        return (coordinates);
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
}
