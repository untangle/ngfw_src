/**
 * $Id: GeographyManager.java 42115 2016-01-15 17:56:36Z mahotz $
 */

package com.untangle.uvm;

public interface GeographyManager
{
    boolean checkForDatabaseUpdate(boolean loadFlag);

    String getCountryName(String netAddress);

    String getCountryCode(String netAddress);

    /**
     * Stores coordinates
     */
    class Coordinates
    {
        public String country;
        public double latitude;
        public double longitude;
    }
}
