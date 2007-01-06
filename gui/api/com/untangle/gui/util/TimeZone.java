/*
 * Copyright (c) 2003-2006 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.util;


public enum TimeZone {

    IDLW("GMT-12:00","International Dateline West","Etc/GMT-12"),
    MIDWAY("GMT-11:00","Midway Island, Samoa","Pacific/Midway"),
    HAWAII("GMT-10:00","Hawaii","US/Hawaii"),
    ALASKA("GMT-09:00","Alaska","US/Alaska"),
    PACIFIC("GMT-08:00","Pacific Time (US & Canada), Tijuana","US/Pacific"),
    CHIHUAHUA("GMT-07:00","Chihuahua, La Paz, Mazatlan","America/Chihuahua"),
    MOUNTAIN("GMT-07:00","Mountain Time (US & Canada)","US/Mountain"),
    CENTRAL("GMT-06:00","Central Time (US & Canada & Central America)","US/Central"),
    MEXICO_CITY("GMT-06:00","Guadalajara, Mexico City, Monterrey","America/Mexico_City"),
    SASKATCHEWAN("GMT-06:00","Saskatchewan","Canada/Saskatchewan"),
    BOGOTA("GMT-05:00","Bogota, Lima, Quito","America/Bogota"),
    EASTERN("GMT-05:00","Eastern Time (US & Canada)","US/Eastern"),
    EAST_INDIANA("GMT-05:00","Indiana (East)","US/East-Indiana"),
    ATLANTIC("GMT-04:00","Atlantic Time (Canada)","Canada/Atlantic"),
    CARACAS("GMT-04:00","Caracas, La Paz","America/Caracas"),
    SANTIAGO("GMT-04:00","Santiago","America/Santiago"),
    NEWFOUNDLAND("GMT-03:30","Newfoundland","Canada/Newfoundland"),
    BUENOS_AIRES("GMT-03:00","Buenos Aires, Georgetown, Brasilia, Greenland","America/Buenos_Aires"),
    MID_ATLANTIC("GMT-02:00","Mid-Atlantic","Etc/GMT-2"),
    AZORES("GMT-01:00","Azores","Atlantic/Azores"),
    CAPE_VERDE("GMT-01:00","Cape Verde Is.","Atlantic/Cape_Verde"),
    CASABLANCA("GMT","Casablanca, Monrovia","Africa/Casablanca"),
    GREENWICH("GMT","Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London","Etc/Greenwich"),
    AMSTERDAM("GMT+01:00","Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna","Europe/Amsterdam"),
    BELGRADE("GMT+01:00","Belgrade, Bratislava, Budapest, Ljubljana, Prague","Europe/Belgrade"),
    BRUSSELS("GMT+01:00","Brussels, Copenhagen, Madrid, Paris","Europe/Brussels"),
    SARAJEVO("GMT+01:00","Sarajevo, Skopje, Warsaw, Zagreb","Europe/Sarajevo"),
    WEST_CENTRAL_AFRICA("GMT+01:00","West Central Africa","Etc/GMT+1"),
    ATHENS("GMT+02:00","Athens, Beirut, Istanbul, Minsk","Europe/Athens"),
    BUCHAREST("GMT+02:00","Bucharest","Europe/Bucharest"),
    CAIRO("GMT+02:00","Cairo, Harare, Pretoria","Africa/Cairo"),
    HELSINKI("GMT+02:00","Helsinki, Kyiv, Riga, Sofia, Talinn, Vilnius","Europe/Helsinki"),
    JERUSALEM("GMT+02:00","Jerusalem","Asia/Jerusalem"),
    BAGHDAD("GMT+03:00","Baghdad","Asia/Baghdad"),
    KUWAIT("GMT+03:00","Kuwait, Riyadh","Asia/Kuwait"),
    MOSCOW("GMT+03:00","Moscow, St. Petersburg, Volgograd","Europe/Moscow"),
    NAIROBI("GMT+03:00","Nairobi","Africa/Nairobi"),
    TEHRAN("GMT+03:30","Tehran","Asia/Tehran"),
    ABU_DHABI("GMT+04:00","Abu Dhabi, Muscat","Asia/Muscat"),
    BAKU("GMT+04:00","Baku, Tbilsi, Yerevan","Asia/Baku"),
    KABUL("GMT+04:30","Kabul","Asia/Kabul"),
    KARACHI("GMT+05:00","Ekaterinburg, Islamabad, Karachi, Tashkent","Asia/Karachi"),
    //KATHMANDU("GMT+05:45","Kathmandu","Asia/Kathmandu"),  SEEMS TO CAUSE FAILURE
    ALMATY("GMT+06:00","Almaty, Novosibirsk","Asia/Almaty"),
    DHAKA("GMT+06:00","Astana, Dhaka","Asia/Dhaka"),
    RANGOON("GMT+06:30","Rangoon","Asia/Rangoon"),
    BANGKOK("GMT+07:00","Bangkok, Hanoi, Jakarta","Asia/Bangkok"),
    KRASNOYARSK("GMT+07:00","Krasnoyarsk","Asia/Krasnoyarsk"),
    HONG_KONG("GMT+08:00","Beijing, Chongqing, Hong Kong, Urumqi","Asia/Hong_Kong"),
    IRKUTSK("GMT+08:00","Irkutsk, Ulaan Bataar","Asia/Irkutsk"),
    KUALA_LUMPUR("GMT+08:00","Kuala Lumpur, Singapore","Asia/Kuala_Lumpur"),
    PERTH("GMT+08:00","Perth","Australia/Perth"),
    TAIPEI("GMT+08:00","Taipei","Asia/Taipei"),
    TOKYO("GMT+09:00","Osaka, Sapporo, Tokyo","Asia/Tokyo"),
    SEOUL("GMT+09:00","Seoul","Asia/Seoul"),
    YAKUTSK("GMT+09:00","Yakutsk","Asia/Yakutsk"),
    ADELAIDE("GMT+09:30","Adelaide","Australia/Adelaide"),
    DARWIN("GMT+09:30","Darwin","Australia/Darwin"),
    BRISBANE("GMT+10:00","Brisbane","Australia/Brisbane"),
    CANBERRA("GMT+10:00","Canberra, Melbourne, Sydney","Australia/Canberra"),
    GUAM("GMT+10:00","Guam, Port Moresby","Pacific/Guam"),
    HOBART("GMT+10:00","Hobart","Australia/Hobart"),
    VLADIVOSTOK("GMT+10:00","Vladivostok","Asia/Vladivostok"),
    MAGADAN("GMT+11:00","Magadan, Solomon Is., New Caledonia","Asia/Magadan"),
    AUCKLAND("GMT+12:00","Auckland, Wellington","Pacific/Auckland"),
    FIJI("GMT+12:00","Fiji, Kamchatka, Marshall Is.","Pacific/Fiji");

    
     
    private String gmtValue;
    private String desc;
    private String key;
    private TimeZone(String gmtValue, String desc, String key){
        this.gmtValue = gmtValue;
        this.desc = desc;
        this.key = key;
    }
    public String getGmtValue(){ return gmtValue; }
    public String getDesc(){ return desc; }
    public String getKey(){ return key; }
    public String toString(){ return "(" + gmtValue + ")" + " " + desc; }
    public static TimeZone getDefault(){ return PACIFIC; }
    public static TimeZone getValue(String key){
        TimeZone tz = null;
        for(TimeZone t : values()){
            if(t.getKey().equals(key)){
                tz = t;
                break;
            }
        }
        return tz;
    }
}

