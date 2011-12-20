/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.log4j.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
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
    public static final Pattern VALID_CHARACTERS = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+$");

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
    public <T> T load(Class<T> clz, String fileName) throws SettingsException
    {
        if (!_checkLegalName(fileName)) {
            throw new IllegalArgumentException("Invalid file name: '" + fileName + "'");
        }

        return _loadImpl(clz, fileName);
    }

    /**
     * Documented in SettingsManager.java
     */
    public <T> T loadUrl(Class<T> clz, String urlStr) throws SettingsException
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
    public <T> T save(Class<T> clz, String fileName, T value) throws SettingsException
    {
        if (!_checkLegalName(fileName)) {
            throw new IllegalArgumentException("Invalid file name: '" + fileName + "'");
        }

        return _saveImpl(clz, fileName, value);
    }

    /**
     * @param serializer
     *            the serializer to set
     */
    protected void setSerializer(JSONSerializer serializer)
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
     * This appends ".js" to the filename, opens the file, and then calls loadInputStream
     */
    @SuppressWarnings("unchecked") //JSON
    private <T> T _loadImpl(Class<T> clz, String fileName) throws SettingsException
    {
        File f = new File(fileName + ".js");
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
     * 
     */
    @SuppressWarnings("unchecked") //JSON
    private <T> T _loadInputStream(Class<T> clz, InputStream is) throws SettingsException
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

            return (T) serializer.fromJSON(jsonString.toString());

        } catch (IOException e) {
            logger.warn("IOException: ",e);
            throw new SettingsException("Unable to the settings: '" + is + "'", e);
        } catch (UnmarshallException e) {
            logger.warn("UnmarshallException: ",e);
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
    private <T> T _saveImpl(Class<T> clz, String fileName, T value) throws SettingsException
    {
        File link = new File(fileName + ".js");
        String versionString = String.valueOf(DATE_FORMATTER.format(new Date()));
        String outputFile    = fileName + ".js" + "-version-" + versionString + ".js";
        String outputFileTmp = outputFile + ".tmp";
        File outputTmp = new File(outputFileTmp);

        Object lock = this.getLock(outputTmp.getParentFile().getAbsolutePath());

        /*
         * Synchronized on the name of the parent directory, so two files cannot
         * modify the same file at the same time
         */
        synchronized (lock) {
            /**
             * If the file exists just overwrite it.
             * The old settings won't be saved but they were around for less than 60 seconds anyway
             */
            /**
             *  if (output.exists()) {
             *  throw new SettingsException("Output file '" + output.toString() + "'already exists!");
             * }
             */

            FileWriter fileWriter = null;
            try {
                File parentFile = outputTmp.getParentFile();

                /* Create the directory structure */
                parentFile.mkdirs();

                fileWriter = new FileWriter(outputTmp);
                String json = this.serializer.toJSON(value);
                logger.debug("Saving Settings: \n" + json);
                fileWriter.write(json);
                fileWriter.close();
                fileWriter = null;

                String  formatCmdArray[] = new String[] { "/bin/sh", "-c", "python -m simplejson.tool " + outputFileTmp + " > " + outputFile };
                Process process = UvmContextImpl.context().exec(formatCmdArray);
                int exitCode = process.waitFor();
                outputTmp.delete();
                process.destroy();
                
                /*
                 * Why must SUN/Oracle try everyone's patience; The API for
                 * creating symbolic links is in Java 1.7
                 */
                String[] chops = outputFile.split(File.separator);
                String filename = chops[chops.length - 1];
                String cmdArray[] = new String[] { "ln", "-sf", "./"+filename, link.toString() };

                process = UvmContextImpl.context().exec(cmdArray);
                exitCode = process.waitFor();
                String line = null;
                BufferedReader tmp = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
                while ((line = tmp.readLine()) != null) {
                    System.out.println("out: " + line);
                }

                tmp = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
                while ((line = tmp.readLine()) != null) {
                    System.out.println("err: " + line);
                }

                if (exitCode != 0) {
                    throw new SettingsException(
                            "Unable to create symbolic link[" + exitCode + "]");
                }
                process.destroy();

                /* Append the history to the end of the history file. */
                /**
                 * File history = buildHistoryPath(clz, dirName);
                 *  fileWriter = new FileWriter(history, true);
                 *  fileWriter.append(String.valueOf(System.currentTimeMillis()));
                 *  fileWriter.append(": ").append(link.getName()).append(" ").append(output.getName()).append("\n");
                 *  fileWriter.close();
                 *  fileWriter = null;
                 */
            } catch (IOException e) {
                logger.warn("Failed to save settings: ", e);
                throw new SettingsException("Unable to load the file: '" + fileName + "'", e);
            } catch (MarshallException e) {
                logger.warn("Failed to save settings: ", e);
                throw new SettingsException("Unable to marshal json string:", e);
            } catch (InterruptedException e) {
                logger.warn("Failed to save settings: ", e);
                throw new SettingsException("Unable to create symbolic link:", e);
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

    private boolean _checkLegalName(String name) throws IllegalArgumentException
    {
        if (!VALID_CHARACTERS.matcher(name.replace("/","")).matches()) {
            logger.error("Illegal name: " + name);
            return false;
        }
        
        return true;
    }
}
