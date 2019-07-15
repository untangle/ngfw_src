/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;
import com.untangle.uvm.util.I18nUtil;

import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.LocaleInfo;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.SettingsManager;

/**
 * Implementation of LanguageManagerImpl.
 */
@SuppressWarnings("serial")
public class LanguageManagerImpl implements LanguageManager
{
    private static final String LANGUAGES_DIR;
    private static final String RESOURCES_DIR;
    private static final String REMOTE_LANGUAGES_URL = "http://translations.untangle.com/";
    private static final String REMOTE_LANGUAGES_PROJECT = "ngfw";
    private static final String LOCALE_DIR = "/usr/share/locale";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_DEV_LANGUAGE = "xx";
    private static final String LANGUAGES_CFG = "lang.cfg";
    private static final String COUNTRIES_CFG = "country.cfg";
    private static final String LC_MESSAGES = "LC_MESSAGES";
    private static final String MO_FILENAME = "untangle.mo";
    private static final int MAX_BUFFER_SIZE = 2048;
    private static final int CLEANER_SLEEP_TIME_MILLI = 60 * 1000; /* Check every minute */
    private final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    private final Logger logger = Logger.getLogger(getClass());

    private LanguageSettings languageSettings;
    private Map<String, String> allLanguages;
    private Map<String, String> allCountries;

    private Map<String, Map<String, String>> translations;
    private Map<String, Long> translationsLastAccessed;

    private volatile Thread cleanerThread;
    private translationsCleaner cleaner = new translationsCleaner();

    static {
        LANGUAGES_DIR = System.getProperty("uvm.lang.dir"); // place for languages resources files
        RESOURCES_DIR = System.getProperty("uvm.lang.dir") + File.separator + "i18n"; // place for languages resources files
    }

    /**
     * Source of language (official, unofficial, etc.)
     */
    private static class languageSource
    {
        private String id;
        private String title;
        private String component;
        private String directory;
        private String prefix;
        private String resourcePath;

        /**
         * Initialize instance of languageSource.
         * @param  id    String id.
         * @param  title String title.
         * @param  component   String url.
         * @return       instance of languageSource.
         */
        public languageSource(String id, String title, String component)
        {
            this.id = id;
            this.title = title;
            this.component = component;
            this.directory = LANGUAGES_DIR + File.separator + id;
            this.prefix = "i18n." + id;
            this.resourcePath = "i18n." + id + ".untangle";
        }
        /**
         * Return source id.
         * @return String of source id.
         */
        public String getId(){ return this.id; }
        /**
         * Return source title.
         * @return String of source title.
         */
        public String getTitle(){ return this.title; }
        /**
         * Return source component.
         * @return String of source component.
         */
        public String getComponent(){ return this.component; }
        /**
         * Return source directory.
         * @return String of source directory.
         */
        public String getDirectory(){ return this.directory;  }
        /**
         * Return source prefix.
         * @return String of source prefix.
         */
        public String getPrefix(){ return this.prefix; }
        /**
         * Return source resource path.
         * @return String of source resource path.
         */
        public String getResourcePath(){ return this.resourcePath; }
    }

    private static final ArrayList<languageSource> LanguageSources;
    static {
        LanguageSources = new ArrayList<>();
        LanguageSources.add(new languageSource("official", I18nUtil.marktr("Official"), "official"));
        LanguageSources.add(new languageSource("community", I18nUtil.marktr("Community"), "community"));
    }

    /**
     * Initialize instance of lanuage manager:
     *
     * * Get settings.
     * * Load all local languages list.
     * * Load all valid countries list.
     * * Initialize translations and translations last accessed maps.
     * * Start the translations map cleaner.
     * 
     * @return Initialize instance of language manager.
     */
    public LanguageManagerImpl()
    {
        readLanguageSettings();
        allLanguages = loadAllLanguages();
        allCountries = loadAllCountries();
        translations = new HashMap<>();
        translationsLastAccessed = new HashMap<>();
        UvmContextFactory.context().newThread(this.cleaner).start();
    }

    // public methods ---------------------------------------------------------

    /**
     * Get language settings.
     * @return LanguageSettings object.
     */
    public LanguageSettings getLanguageSettings()
    {
        return languageSettings;
    }

    /**
     * Commit language settings and if language has changed, download from remote.
     * @param newSettings New LanguageSettings to compare.
     */
    public void setLanguageSettings(LanguageSettings newSettings)
    {
        String oldSource = languageSettings.getSource();
        String oldLanguage = languageSettings.getLanguage();
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-vm/language.js";

        try {
            settingsManager.save( settingsName, newSettings );
        } catch (Exception exn) {
            logger.error("Could not save language settings", exn);
            return;
        }

        String source = newSettings.getSource();
        String language = newSettings.getLanguage();
        if(!oldSource.equals(source) || 
           !oldLanguage.equals(language)){
            downloadLanguage(source, language);
            newSettings.setLastSynchronized(System.currentTimeMillis());
        }

        this.languageSettings = newSettings;
        synchronized( this ) {
            translations = new HashMap<>();
            translationsLastAccessed = new HashMap<>();
        }
    }

    /**
     * Download the currently selected language.
     */
    public void synchronizeLanguage()
    {
        downloadLanguage(languageSettings.getSource(), languageSettings.getLanguage());
        
        synchronized( this ) {
            translations = new HashMap<>();
            translationsLastAccessed = new HashMap<>();
        }

        LanguageSettings settings = getLanguageSettings();
        settings.setLastSynchronized(System.currentTimeMillis());
        setLanguageSettings(settings);
    }

    /**
     * Get locale language list
     * @return List of LocaleInfo.
     */
    public List<LocaleInfo> getLanguagesList()
    {
        List<LocaleInfo> locales = new ArrayList<>();
        boolean headerAdded = true;
        boolean defaultAdded = false;

        /* Contact translation server */
        for(languageSource source : LanguageSources){
            Set<String> available = new HashSet<>();
            Collections.addAll(available, (new File(source.getDirectory())).list());
            if(getRemoteLanguagesList(available, locales, source) == false){
                // Add header 
                headerAdded = false;
            }else if(source.getId().equals("official")){
                locales.add(new LocaleInfo(source.getId() + "-" + DEFAULT_LANGUAGE, allLanguages.get(DEFAULT_LANGUAGE), null, null));
                defaultAdded = true;
            }

            for (String code : available) {
                /* Add local-only stragglers like test.*/
                /* Or if server had problems, everything local. */
                if(headerAdded == false){
                    locales.add(new LocaleInfo(null, "<em><b>" + source.title + "</b></em>", null, null));
                    headerAdded = true;
                    if(source.getId().equals("official")){
                        locales.add(new LocaleInfo(source.getId() + "-" + DEFAULT_LANGUAGE, allLanguages.get(DEFAULT_LANGUAGE), null, null));
                        defaultAdded = true;
                    }
                }
                String tokens[] = code.split("_");
                String langCode = tokens[0];
                String langName = allLanguages.get(langCode);
                String countryCode = tokens.length == 2 ? tokens[1] : null;
                String countryName = countryCode == null ? null : allCountries.get(countryCode);
                locales.add(new LocaleInfo(source.id + "-" + langCode, langName, countryCode, countryName));
            }
        }

        return locales;
    }

    /**
     * Get translations
     * @param module Language to get.
     * @return Map of String, String from i18n key to translated value.
     */
    public Map<String, String> getTranslations(String module)
    {
        Map<String, String> map = null;

        if (null == module) {
            return new HashMap<>();
        }

        String i18nModule = module.replaceAll("-", "_");
        Locale locale = getLocale();
        String sourceId = "official";
        if (languageSettings != null && languageSettings.getSource() != null) {
            sourceId = languageSettings.getSource();
        }
        languageSource source = getLanguageSource(sourceId);
        if(source == null){
            return map;
        }

        String translationKey = i18nModule + "_" + source.getId() + "_" + locale.getLanguage();
        
        synchronized( this ) {
            map = translations.get(translationKey);

            if(map == null){
                translations.put(translationKey, new HashMap<String, String>());
                map = translations.get(translationKey);
            }

            if(map.size() == 0){
                /**
                 * Read .mo file directly
                 * https://www.gnu.org/software/gettext/manual/html_node/MO-Files.html
                 */
                RandomAccessFile in = null;
                String filename = LOCALE_DIR + File.separator + locale + File.separator + LC_MESSAGES + File.separator + MO_FILENAME;
                File file = new File(filename);
                if (file.exists()) {
                    try {
                        byte buffer[] = new byte[MAX_BUFFER_SIZE];
                        ByteBuffer byteBuffer;
                        int version = -1;
                        int numberOfStrings = 0;
                        int stringIndexOffset = 0;
                        int translateIndexOffset = 0;

                        int currentLength = 0;
                        int currentOffset = 0;

                        in = new RandomAccessFile(filename, "r");

                        in.seek(4);
                        in.read(buffer, 0, 4);
                        byteBuffer = ByteBuffer.wrap(buffer, 0, 4);
                        byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                        version = byteBuffer.getInt();
                        if(version != 0){
                            logger.warn("Unknown .mo version=" + byteBuffer.getInt());
                        }else{
                            in.read(buffer, 0, 4);
                            byteBuffer = ByteBuffer.wrap(buffer, 0, 4);
                            byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                            numberOfStrings = byteBuffer.getInt();

                            in.read(buffer, 0, 4);
                            byteBuffer = ByteBuffer.wrap(buffer, 0, 4);
                            byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                            stringIndexOffset = byteBuffer.getInt();

                            in.read(buffer, 0, 4);
                            byteBuffer = ByteBuffer.wrap(buffer, 0, 4);
                            byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                            translateIndexOffset = byteBuffer.getInt();

                            int stringLength = 0;
                            int stringOffset = 0;
                            int translateLength = 0;
                            int translateOffset = 0;
                            String string = null;
                            for(int i = 0; i < numberOfStrings; i++ ){
                                in.seek(stringIndexOffset);
                                in.read(buffer, 0, 4);
                                byteBuffer = ByteBuffer.wrap(buffer, 0, 4);
                                byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                                stringLength = byteBuffer.getInt();
                                if(stringLength > MAX_BUFFER_SIZE){
                                    buffer = new byte[stringLength + 1024];
                                }
                                in.read(buffer, 0, 4);
                                byteBuffer = ByteBuffer.wrap(buffer, 0, 4);
                                byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                                stringOffset = byteBuffer.getInt();
                                stringIndexOffset += 8;

                                in.seek(stringOffset);
                                in.read(buffer, 0, stringLength);
                                string = new String(buffer, 0, stringLength);

                                in.seek(translateIndexOffset);
                                in.read(buffer, 0, 4);
                                byteBuffer = ByteBuffer.wrap(buffer, 0, 4);
                                byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                                translateLength = byteBuffer.getInt();
                                if(stringLength > MAX_BUFFER_SIZE){
                                    buffer = new byte[stringLength + 1024];
                                }
                                in.read(buffer, 0, 4);
                                byteBuffer = ByteBuffer.wrap(buffer, 0, 4);
                                byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                                translateOffset = byteBuffer.getInt();
                                translateIndexOffset += 8;

                                in.seek(translateOffset);
                                in.read(buffer, 0, translateLength);

                                map.put(string, new String(buffer, 0, translateLength));
                            }
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to load language!", e);
                    } finally {
                        if(in != null){
                            try{
                                in.close();
                            }catch(IOException ex){
                                logger.error("Unable to close file", ex);
                            }
                        }
                    }
                }
            }

            if(getLanguageSettings().getRegionalFormats().equals("override")){
                map.put("decimal_sep", getLanguageSettings().getOverrideDecimalSep());
                map.put("thousand_sep", getLanguageSettings().getOverrideThousandSep());
                map.put("date_fmt", getLanguageSettings().getOverrideDateFmt());
                map.put("timestamp_fmt", getLanguageSettings().getOverrideTimestampFmt());
            }else{
                if(map.get("decimal_sep") == null){
                    map.put("decimal_sep", ".");
                }
                if(map.get("thousand_sep") == null){
                    map.put("thousand_sep", ",");
                }
                if(map.get("date_fmt") == null){
                    map.put("date_fmt", "Y-m-d");
                }
                if(map.get("timestamp_fmt") == null){
                    map.put("timestamp_fmt", "Y-m-d h:i:s a");
                }
            }
            translationsLastAccessed.put(translationKey, System.currentTimeMillis());
            return map;
        }
    }

    /*
     * Private methods -----------------------------------------------------------
     */

    /**
     * Check if a language pack entry conform to the correct naming: <lang_code>/<module_name>.po
     * @param entry ZipEntry containing naming to check.
     * @return true if valid, false if not.
     */
    private boolean isValid(ZipEntry entry)
    {
        String tokens[] = entry.getName().split(File.separator);
        if (entry.isDirectory()) {
            // in order to be a valid entry, the folder name should be a valid language code
            if (tokens.length != 1 || !isValidLocaleCode(tokens[0])) {
                logger.warn("The folder " + entry.getName() + " does not correspond to a valid language code");
                return false;
            }
        }
        return true;
    }

    /**
     * Copy file from zip stream to disk.
     * @param zipInputStream ZipInputStream from Translation server.
     * @param entry ZipEntry containing information about the language.
     * @param destinationDirectory String of location to store downloaded file.
     * @return true if copy succeeded, false if not.
     */
    private boolean copyZipEntryToDisk(ZipInputStream zipInputStream, ZipEntry entry, String destinationDirectory){
        boolean success = true;

        BufferedOutputStream dest = null;

        String entryFilename = null;
        int entryDirectoryIndex = entry.getName().lastIndexOf("/");
        if(entryDirectoryIndex > 0){
            entryFilename = entry.getName().substring(entryDirectoryIndex + 1);
        }else{
            entryFilename = entry.getName();
        }

        File file = new File(destinationDirectory + File.separator + entryFilename);
        File parentDir = file.getParentFile();
        if (parentDir!=null && !parentDir.exists()) {
            parentDir.mkdir();
        }

        int count;
        byte data[] = new byte[MAX_BUFFER_SIZE];
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            logger.warn("copyZipEntryToDisk: File not found", e);
            success = false;
            return success;
        }
        try{
            dest = new BufferedOutputStream(fos, MAX_BUFFER_SIZE);
            while ((count = zipInputStream.read(data, 0, MAX_BUFFER_SIZE)) != -1) {
                dest.write(data, 0, count);
            }
            dest.flush();
        }catch(IOException e){
            logger.warn("copyZipEntryToDisk: Unable to write file", e);
            success = false;
        }finally{
            if(dest != null){
                try{
                    dest.close();
                }catch(IOException ex){
                    logger.error("Unable to close file", ex);
                }
            }
        }
        return success;
    }

    /**
     * Return the locale from current language in settings.
     * @return Locale containing the current locale.
     */
    private Locale getLocale()
    {
        Locale locale = new Locale(DEFAULT_LANGUAGE);
        if (languageSettings != null && languageSettings.getLanguage() != null) {
            String tokens[] = languageSettings.getLanguage().split("_");
            if (tokens.length == 1) {
                locale = new Locale(tokens[0]);
            } else if (tokens.length == 2) {
                locale = new Locale(tokens[0], tokens[1]);
            }
        }
        return locale;
    }

    /**
     * Get the list of all supported languages.
     * @return Map of language code to languafe name.
     */
    private Map<String, String> loadAllLanguages()
    {
        Map<String, String> languages = new HashMap<>();

        // Reading all languages from config file
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(LANGUAGES_DIR + File.separator + LANGUAGES_CFG));
            String s = new String();
            while((s = in.readLine())!= null) {
                s = s.trim();
                if (s.length() > 0){
                    String[] tokens = s.split("\\s+");
                    if (tokens.length >= 2) {
                        String langCode = tokens[0];
                        String langName = tokens[1];
                        languages.put(langCode, langName);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed getting all languages!", e);
        } finally {
            if(in != null){
                try{
                    in.close();
                }catch(IOException ex){
                    logger.error("Unable to close file", ex);
                }
            }
        }

        return languages;
    }

    /**
     * Get list of countries from country config
     * @return Map of country code to full country name.
     */
    private Map<String, String> loadAllCountries()
    {
        Map<String, String> countries = new HashMap<>();

        // Reading all countries from config file
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(LANGUAGES_DIR + File.separator + COUNTRIES_CFG));
            String s = new String();
            while((s = in.readLine())!= null) {
                if (s.trim().length() > 0){
                    String[] tokens = s.split("\\s+");
                    if (tokens.length >= 2) {
                        String countryCode = tokens[0];
                        String countryName = s.replaceFirst(countryCode, "").trim();;
                        countries.put(countryCode, countryName);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed getting all countries!", e);
        } finally {
            if(in != null ){
                try{
                    in.close();
                }catch(IOException ex){
                    logger.error("Unable to close file", ex);
                }
            }
        }

        return countries;
    }

    /**
     * Determine if this locale code is ok to use.
     * @param  code String of language code to check.
     * @return      boolean of true if locale code can be used, false otherwise.
     */
    private boolean isValidLocaleCode(String code)
    {
        if (code == null) {
            return false;
        }
        String tokens[] = code.split("_");
        if (tokens.length == 0 || tokens.length > 2) {
            return false;
        }

        String langCode = tokens[0];
        String countryCode = tokens.length == 2 ? tokens[1] : null;
        return isValidLanguageCode(langCode)
            && (countryCode == null || isValidCountryCode(countryCode));
    }

    /**
     * Determine if this locale code is within supported languages.
     * @param  code String of language code to check.
     * @return      boolean of true if valid language, false if not.
     */
    private boolean isValidLanguageCode(String code)
    {
        return allLanguages.containsKey(code);
    }

    /**
     * Determine if this country code is within supported countries.
     * @param  code String of country code to check.
     * @return      boolean of true if valid country, false if not.
     */
    private boolean isValidCountryCode(String code)
    {
        return allCountries.containsKey(code);
    }

    /**
     * Load language settings into local languageSettings.
     */
    private void readLanguageSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-vm/language.js";
        LanguageSettings readSettings = null;

        logger.debug("Loading language settings from " + settingsFile);

        try {
            readSettings =  settingsManager.load( LanguageSettings.class, settingsFile );
        } catch (Exception exn) {
            logger.error("Could not read language settings", exn);
        }

        if (readSettings == null) {
            logger.warn("No settings found... initializing with defaults");
            languageSettings = new LanguageSettings();
            if ( UvmContextFactory.context().isDevel() )
                languageSettings.setLanguage(DEFAULT_DEV_LANGUAGE);
            else
                languageSettings.setLanguage(DEFAULT_LANGUAGE);
            setLanguageSettings(languageSettings);
        }
        else {
            languageSettings = readSettings;
        }
    }

    /**
     * This thread periodically walks through translations instances and removes
     * those maps that have not been used recently.
     */
    private class translationsCleaner implements Runnable
    {
        /**
         * Walk though translations and for any that has not ben used in CLEANER_LAST_ACCESS_MAX_TIME,
         * remove from memory.
         */
        public void run()
        {
            cleanerThread = Thread.currentThread();

            while (cleanerThread != null) {
                try {
                    Thread.sleep(CLEANER_SLEEP_TIME_MILLI);
                } catch (Exception e) {}

                try {
                    Long now = System.currentTimeMillis();
                    Map<String, String> map;

                    /**
                     * Remove old entries from map
                     */
                    synchronized( this ) {
                        Long lastAccessed;
                        for( String translationKey : translationsLastAccessed.keySet() ){
                            lastAccessed = translationsLastAccessed.get(translationKey);
                            if(lastAccessed == 0L){
                                continue;
                            } 
                            if((lastAccessed + CLEANER_LAST_ACCESS_MAX_TIME) < now){
                                translationsLastAccessed.put(translationKey, 0L);

                                map = translations.get(translationKey);

                                if(map != null){
                                    map.clear();
                                }
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    logger.warn("Exception while cleaning translations",e);
                }
            }
        }
    }

    /**
     * Return the languageSource for a source identifer.
     * @param  sourceId String of sourceID to find.
     * @return          LanguageSource object.  If not found, null.
     */
    private languageSource getLanguageSource(String sourceId)
    {
        for(languageSource ls : LanguageSources){
            if(ls.getId().equals(sourceId)){
                return ls;
            }
        }
        return null;        
    }

    /**
     * Query Translation server and get the list of supported languages/
     * @param  available Set of languages we have locally.
     * @param  locales   Found locales.
     * @param  source    LanguageSource to use for query.
     * @return           boolean of true if was able to get remote list, false if failed.
     */
    private boolean getRemoteLanguagesList(Set<String> available, List<LocaleInfo> locales, languageSource source ){
        boolean result = true;
        boolean headerAdded = false;

        InputStream is = null;

        RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setSocketTimeout(5000)
            .setConnectTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .build();        
        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(defaultRequestConfig)
            .build();
        CloseableHttpResponse response = null;

        JSONObject remoteObject = null;
        // JSONObject remoteTable = null;
        // JSONObject remoteStats = null;

        String urlString = REMOTE_LANGUAGES_URL + "json/" + REMOTE_LANGUAGES_PROJECT + '/' + source.getComponent();
        try {
            URL url = new URL(urlString);

            HttpGet get = new HttpGet(url.toString());

            get.addHeader("Accept-Encoding", "gzip");
            response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            if ( entity == null ) {
                logger.warn("Invalid Response: " + entity);
                return result;
            }
            is = entity.getContent();

            BufferedReader reader = null;
            try {
                StringBuilder jsonString = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                String line;
                while ((line = reader.readLine()) != null) {
                    jsonString.append(line+"\n");
                }

                try{
                    remoteObject = new JSONObject(jsonString.toString());
                    try{
                        JSONObject remoteStats = remoteObject.getJSONObject("stats");
                        JSONObject stats = null;
                        String[] codes = remoteStats.names().join(",").split(",");
                        Arrays.sort(codes);
                        String code;
                        for(int i = 0; i < codes.length; i++){
                            code = codes[i];
                            code = code.substring(1,code.length()-1);
                            if(code.equals("templates")){
                                continue;
                            }
                            if(headerAdded == false){
                                // Add header
                                locales.add(new LocaleInfo(null, "<em><b>" + source.getTitle() + "</b></em>", null, null));
                                headerAdded = true;
                            }
                            stats = remoteStats.getJSONObject(code);
                            StringBuilder statistics = new StringBuilder();
                            if(!stats.isNull("last_change")){
                                int lastChange = stats.getInt("last_change");
                                if(lastChange > 0){
                                    Date d = new Date((long) lastChange * 1000);
                                    statistics.append(I18nUtil.marktr("Last modified") + ": " + dateFormatter.format(d.getTime()));
                                    statistics.append("<br>");
                                }
                            }
                            statistics.append(I18nUtil.marktr("Percent completed") + ": " + stats.getDouble("translated_percent") + "%");
                            locales.add(new LocaleInfo(source.getId() + "-" + code, stats.getString("name"), null, null, statistics.toString()));
                        }
                    }catch(JSONException e){
                        logger.warn("JSON Exception " + e);
                    }
                }catch( JSONException e){
                    logger.warn("Unable to convert json to remoteObject " + e);
                    result = false;
                }
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception e) {}
            }

        }
        catch (java.net.MalformedURLException e) {
            logger.warn("Invalid URL: '" + urlString + "'", e);
        }
        catch (java.io.IOException e) {
            logger.warn("Invalid content in URL: '" + urlString + "'", e);
        } finally {
            try { if ( response != null ) response.close(); } catch (Exception e) { logger.warn("close",e); }
            try { httpClient.close(); } catch (Exception e) { logger.warn("close",e); }
        }

        return result;

    }

    /**
     * Initiate download of language.
     * @param sourceId String of sourceId group.
     * @param language Language to download.
     */
    private void downloadLanguage(String sourceId, String language){
        InputStream is = null;
        CloseableHttpClient httpClient = HttpClients.custom().build();
        CloseableHttpResponse response = null;
        boolean success = true;
        String msg = "";
        BufferedOutputStream dest = null;

        languageSource source = getLanguageSource(sourceId);
        if(source == null){
            return;
        }

        String urlString = REMOTE_LANGUAGES_URL + "export/" + REMOTE_LANGUAGES_PROJECT + "/" + source.getComponent() + "/" + language + "?format=zip";
        try {
            URL url = new URL(urlString);

            HttpGet get = new HttpGet(url.toString());
            get.addHeader("Accept-Encoding", "gzip");
            response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            if ( entity == null ) {
                logger.warn("Invalid Response: " + entity);
            }else{
                is = entity.getContent();

                ZipEntry entry = null;
                ZipInputStream zis = new ZipInputStream(is);
                String extension = null;
                int extensionIndex = 0;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!isValid(entry)){
                        success = false;
                        msg = "Invalid Entry";
                        break;
                    }
                    if (entry.isDirectory()) {
                        File dir = new File(source.getDirectory() + File.separator + entry.getName());
                        if (!dir.exists()) {
                            dir.mkdir();
                        }
                    } else {
                        // ## look at file extensions
                        extensionIndex = entry.getName().lastIndexOf(".");
                        if(extensionIndex > 0){
                            extension = entry.getName().substring(extensionIndex + 1);
                        }

                        File file = new File(source.getDirectory() + File.separator + entry.getName());
                        File parentDir = file.getParentFile();
                        if(extension != null){
                            switch(extension){
                                case "mo":
                                    copyZipEntryToDisk(zis, entry, LOCALE_DIR + File.separator + language + File.separator + LC_MESSAGES);
                                    break;
                                case "class":
                                    copyZipEntryToDisk(zis, entry, RESOURCES_DIR + File.separator + source.getId());
                                    break;
                            }
                        }
                    }
                }
                zis.close();
                is.close();
            }
        }catch (java.net.MalformedURLException e) {
            logger.warn("Invalid URL: '" + urlString + "'", e);
        }
        catch (java.io.IOException e) {
            logger.warn("Invalid content in URL: '" + urlString + "'", e);
        } finally {
            try { if ( response != null ) response.close(); } catch (Exception e) { logger.warn("close",e); }
            try { httpClient.close(); } catch (Exception e) { logger.warn("close",e); }
        }
    }

}
