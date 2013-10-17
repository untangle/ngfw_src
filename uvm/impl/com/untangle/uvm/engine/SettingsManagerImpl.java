/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.UnmarshallException;

import com.untangle.uvm.SettingsManager;

public class SettingsManagerImpl implements SettingsManager
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Valid characters for settings file names
     */ 
    public static final Pattern VALID_CHARACTERS = Pattern.compile("^[a-zA-Z0-9_\\-\\.@]+$");

    /**
     * The extension on the filename (usually .js)
     */ 
    public static final Pattern FILE_EXTENSION = Pattern.compile("\\.\\w+$");
    
    /**
     * Formatting for the version string (yyyy-mm-dd-hhmm)
     */
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd-HHmm");

    /**
     * This is the actual JSON serializer
     */
    private JSONSerializer serializer = null;

    /**
     * This is the locks for the various files to guarantee synchronous file access
     */
    private final Map<String, Object> pathLocks = new HashMap<String, Object>();
     
    /**
     * Documented in SettingsManager.java
     */
    public <T> T load( Class<T> clz, String fileName ) throws SettingsException
    {
        if (!_checkLegalName(fileName)) {
            throw new IllegalArgumentException("Invalid file name: '" + fileName + "'");
        }

        return _loadImpl(clz, fileName);
    }

    /**
     * Documented in SettingsManager.java
     */
    public <T> T loadUrl( Class<T> clz, String urlStr ) throws SettingsException
    {
        InputStream is = null;

        try {
            URL url = new URL(urlStr);
            logger.debug("Fetching Settings from URL: " + url); 

            HttpClient hc = new HttpClient();
            HttpMethod get = new GetMethod(url.toString());
            get.setRequestHeader("Accept-Encoding", "gzip");
            hc.executeMethod(get);

            Header h = get.getResponseHeader("Content-Encoding");

            /**
             * Check for gzipped response
             */
            if (h != null) {
                String ce = h.getValue();
                if (ce != null && ce.equals("gzip")) {
                    is = new GZIPInputStream(get.getResponseBodyAsStream());
                }
            }

            /**
             * Otherwise just assume its in clear text
             */
            if (is == null) {
                is = get.getResponseBodyAsStream();
            }

            Object lock = this.getLock(urlStr);
            synchronized(lock) {
                return _loadInputStream(clz, is);
            }
        }
        catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: '" + urlStr + "'", e);
        }
        catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid content in URL: '" + urlStr + "'", e);
        }
    }

    /**
     * Documented in SettingsManager.java
     */
    public <T> T save( Class<T> clz, String fileName, T value ) throws SettingsException
    {
        return save(clz, fileName, value, true);
    }
    
    public <T> T save(Class<T> clz, String fileName, T value, boolean saveVersion) throws SettingsException
    {
        if (!_checkLegalName(fileName)) {
            throw new IllegalArgumentException("Invalid file name: '" + fileName + "'");
        }

        return _saveImpl(clz, fileName, value, saveVersion);
    }

    /**
     * @param serializer
     *            the serializer to set
     */
    protected void setSerializer( JSONSerializer serializer )
    {
        this.serializer = serializer;
    }

    /**
     * @return the serializer
     */
    protected JSONSerializer getSerializer()
    {
        return serializer;
    }
    
    /**
     * Implementation of the load
     * This opens the file, and then calls loadInputStream
     */
    private <T> T _loadImpl( Class<T> clz, String fileName ) throws SettingsException
    {
        File f = new File( fileName );
        if (!f.exists()) {
            return null;
        }

        InputStream is = null;
        try {
            is = new FileInputStream(f);
        }
        catch (java.io.FileNotFoundException e) {
            throw new SettingsException("File not found: " + f);
        }

        Object lock = this.getLock(f.getParentFile().getAbsolutePath());
        synchronized(lock) {
            return _loadInputStream(clz, is);
        }
    }

    /**
     * Implementation of the load using a stream
     */
    @SuppressWarnings("unchecked") //JSON
    private <T> T _loadInputStream( Class<T> clz, InputStream is ) throws SettingsException
    {
        BufferedReader reader = null;
        try {
            StringBuilder jsonString = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line+"\n");
            }

            logger.debug("Loading Settings: \n" + "-----------------------------\n" + jsonString + "-----------------------------\n");

            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return (T) serializer.fromJSON(jsonString.toString());
        } catch (IOException e) {
            logger.warn("IOException: ",e);
            throw new SettingsException("Unable to the settings: '" + is + "'", e);
        } catch (UnmarshallException e) {
            logger.warn("UnmarshallException: ",e);
            for ( Throwable cause = e.getCause() ; cause != null ; cause = cause.getCause() ) {
                logger.warn("Exception cause: ", cause);
            }
            throw new SettingsException("Unable to unmarshal the settings: '" + is + "'", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {}
        }
    }
    
    /**
     * Implementation of the save
     *
     * This serializes the JSON object to a tmp file
     * Then formats that tmp file and copies it to another file
     * Then it repoints the symlink
     */
    private <T> T _saveImpl( Class<T> clz, String fileName, T value, boolean saveVersion) throws SettingsException
    {
        File link = new File( fileName );
        String outputFileName;
        if (saveVersion){
            String versionString = String.valueOf(DATE_FORMATTER.format(new Date()));
            outputFileName = fileName + "-version-" + versionString + _findFileExtension( fileName );
        } else {
            outputFileName = fileName;
        }
        File outputFile = new File(outputFileName);

        Object lock = this.getLock(outputFile.getParentFile().getAbsolutePath());

        /*
         * Synchronized on the name of the parent directory, so two files cannot
         * modify the same file at the same time
         */
        synchronized (lock) {
            FileWriter fileWriter = null;

            try {
                File parentFile = outputFile.getParentFile();

                /* Create the directory structure */
                parentFile.mkdirs();

                fileWriter = new FileWriter(outputFile);
                String json = this.serializer.toJSON(value);
                logger.debug("Saving Settings: \n" + json);
                fileWriter.write(json);
                fileWriter.close();
                fileWriter = null;

                String formatCmd = new String(System.getProperty("uvm.bin.dir") + "/" + "ut-format-json" + " " + outputFileName);
                UvmContextImpl.context().execManager().execResult(formatCmd);
                
                if (saveVersion){
                    /*
                     * The API for creating symbolic links is in Java 7
                     */
                    String[] chops = outputFileName.split(File.separator);
                    String filename = chops[chops.length - 1];
                    String linkCmd = "ln -sf ./"+filename + " " + link.toString();
                    UvmContextImpl.context().execManager().exec(linkCmd);
                }

            } catch (IOException e) {
                logger.warn("Failed to save settings: ", e);
                throw new SettingsException("Unable to save the file: '" + fileName + "'", e);
            } catch (MarshallException e) {
                logger.warn("Failed to save settings: ", e);
                for ( Throwable cause = e.getCause() ; cause != null ; cause = cause.getCause() ) {
                    logger.warn("Exception cause: ", cause);
                }
                throw new SettingsException("Unable to marshal json string:", e);
            } finally {
                try {
                    if (fileWriter != null) {
                        fileWriter.close();
                    }
                } catch (Exception e) {
                }
            }
            return _loadImpl(clz, fileName);
        }
    }

    /**
     * Retrieve a lock to use for a given path. By locking on an object you
     * guarantee the object is unique.
     * http://www.javalobby.org/java/forums/t96352.html
     * 
     * @param lockName
     *            The name of the lock
     * @return An object that can be used to lock on path.
     */
    private Object getLock(String lockName)
    {
        Object lock = this.pathLocks.get(lockName);
        if (lock == null) {
            lock = new Object();
            this.pathLocks.put(lockName, lock);
        }

        return lock;
    }

    /**
     * Check if a filename is "legal"
     * Must have valid characterns and an extension
     */
    private boolean _checkLegalName(String name) throws IllegalArgumentException
    {
        if (!VALID_CHARACTERS.matcher( name.replace("/","") ).matches()) {
            logger.error("Illegal name (Invalid characters): " + name);
            return false;
        }

        Matcher m = FILE_EXTENSION.matcher( name );
        if ( ! m.find()) {
            logger.error("Illegal name (Missing file extension): " + name);
            return false;
        }

        if ( name.contains("../") ) {
            logger.error("Illegal name (contains ../): " + name);
            return false;
        }
            
        return true;
    }

    /**
     * Finds the file extension and returns it as a string
     * Example fileName = foo.js returns ".js"
     */
    private String _findFileExtension( String fileName )
    {
        Matcher m = FILE_EXTENSION.matcher( fileName );
        if ( m.find() ) {
            return m.group();
        }

        return null;
    }
}
