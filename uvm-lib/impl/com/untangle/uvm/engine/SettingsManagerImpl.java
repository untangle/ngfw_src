package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
    public static final Pattern VALID_CHARACTERS = Pattern.compile("^[a-zA-Z0-9_-]+$");

    /**
     * Formatting for the version string (yyyy-mm-dd-hhmm)
     */
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd-HHmm");

    /**
     * Default base path used when a base path is not specified
     */
    private String defaultBasePath = System.getProperty("uvm.settings.dir");

    private JSONSerializer serializer = null;

    private final Map<String, Object> pathLocks = new HashMap<String, Object>();

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
     * Load the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @param id
     *            Unique identifier to select the object.
     * @return The object that was loaded or null if an object was not loaded.
     * @throws SettingsException
     */
    public <T> T load(Class<T> clz, String packageName, String id)
        throws SettingsException
    {
        if (!VALID_CHARACTERS.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid id value: '" + id + "'");
        }

        return loadImpl(clz, this.defaultBasePath, packageName, id);
    }

    /**
     * Load the settings from the store using a unique identifier.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @param id
     *            Unique identifier to select the object.
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws SettingsException
     */
    public <T> T save(Class<T> clz, String packageName, String id, T value)
        throws SettingsException
    {
        if (!VALID_CHARACTERS.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid id value: '" + id + "'");
        }

        return saveImpl(clz, this.defaultBasePath, packageName, id, value);
    }

    public <T> T loadBasePath(Class<T> clz, String basePath, String packageName, String id)
        throws SettingsException
    {
        if (!VALID_CHARACTERS.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid id value: '" + id + "'");
        }

        return loadImpl(clz, basePath, packageName, id);
    }

    public <T> T saveBasePath(Class<T> clz, String basePath, String packageName, String id, T value)
        throws SettingsException
    {
        if (!VALID_CHARACTERS.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid id value: '" + id + "'");
        }

        return saveImpl(clz, basePath, packageName, id, value);
    }
    
    /**
     * 
     * @param <T>
     *            The type of class to return
     * @param clz
     *            The type of class to return.
     * @param packageName
     *            The name of the package where this setting is stored.
     * @param query
     *            The unique query string that was built ahead.
     * @return null if the object doesn't exist, otherwise it returns a copy of
     *         the object
     * @throws SettingsException
     */
    @SuppressWarnings("unchecked") //JSON
    private <T> T loadImpl(Class<T> clz, String basePath, String packageName, String query)
        throws SettingsException
    {
        File f = buildHeadPath(clz, basePath, packageName, query);
        if (!f.exists()) {
            return null;
        }

        Object lock = this.getLock(f);

        synchronized (lock) {
            BufferedReader reader = null;
            try {
                StringBuilder jsonString = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

                char buffer[] = new char[1024];
                while (true) {
                    if (reader.read(buffer) <= 0) {
                        break;
                    }

                    jsonString.append(buffer);
                }

                logger.debug("Loading Settings: \n" + jsonString);

                return (T) serializer.fromJSON(jsonString.toString());

            } catch (IOException e) {
                throw new SettingsException("Unable to load the file: '" + f
                        + "'", e);
            } catch (UnmarshallException e) {
                throw new SettingsException(
                        "Unable to unmarshal string the file: '" + f + "'", e);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    private <T> T saveImpl(Class<T> clz, String basePath, String packageName, String query, T value)
        throws SettingsException
    {
        File link = buildHeadPath(clz, basePath, packageName, query);
        File output = buildVersionPath(clz, basePath, packageName, query);

        Object lock = this.getLock(output);

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
                File parentFile = output.getParentFile();

                /* Create the directory structure */
                parentFile.mkdirs();

                fileWriter = new FileWriter(output);
                String json = this.serializer.toJSON(value);
                logger.debug("Saving Settings: \n" + json);

                fileWriter.write(json);
                fileWriter.close();
                fileWriter = null;

                /*
                 * Why must SUN/Oracle try everyone's patience; The API for
                 * creating symbolic links is in Java 1.7
                 */
                String[] chops = output.toString().split(File.separator);
                String filename = chops[chops.length - 1];
                String cmdArray[] = new String[] { "ln", "-sf", "./"+filename, link.toString() };

                Process process = UvmContextImpl.context().exec(cmdArray);
                int exitCode = process.waitFor();
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
                 * File history = buildHistoryPath(clz, packageName);
                 *  fileWriter = new FileWriter(history, true);
                 *  fileWriter.append(String.valueOf(System.currentTimeMillis()));
                 *  fileWriter.append(": ").append(link.getName()).append(" ").append(output.getName()).append("\n");
                 *  fileWriter.close();
                 *  fileWriter = null;
                 */
            } catch (IOException e) {
                throw new SettingsException("Unable to load the file: '" + output + "'", e);
            } catch (MarshallException e) {
                throw new SettingsException("Unable to marshal json string:", e);
            } catch (InterruptedException e) {
                throw new SettingsException("Unable to create symbolic link:", e);
            } finally {
                try {
                    if (fileWriter != null) {
                        fileWriter.close();
                    }
                } catch (Exception e) {
                }
            }
            return loadImpl(clz, basePath, packageName, query);
        }
    }

    private File buildHeadPath(Class<?> clz, String basePath, String packageName, String query)
    {
        String clzName = clz.getCanonicalName();
        if (clzName == null) {
            throw new IllegalArgumentException("null canonical name: '" + clz.toString() + "'");
        }

        /* First build the file string */
        String s = File.separator;
        return new File(basePath + s + packageName + s /* + clzName */ + s + query + ".js");
    }

    private File buildVersionPath(Class<?> clz, String basePath, String packageName, String query)
    {
        String clzName = clz.getCanonicalName();
        if (clzName == null) {
            throw new IllegalArgumentException("null canonical name: '"
                    + clz.toString() + "'");
        }

        /* First build the file string */
        String s = File.separator;

        //String versionString = String.valueOf(System.currentTimeMillis()) + "-" + DATE_FORMATTER.format(new Date());
        String versionString = String.valueOf(DATE_FORMATTER.format(new Date()));
        return new File(basePath + s + packageName + s /* + clzName */ + s + query + ".js" + "-version-" + versionString + ".js");
    }

    /**
     * Retrieve a lock to use for a given path. By locking on an object you
     * guarantee the object is unique.
     * http://www.javalobby.org/java/forums/t96352.html
     * 
     * @param path
     *            The path to lock on.
     * @return An object that can be used to lock on path.
     */
    private Object getLock(File file)
    {
        String path = file.getParentFile().getAbsolutePath();
        Object lock = this.pathLocks.get(path);
        if (lock == null) {
            lock = new Object();
            this.pathLocks.put(path, lock);
        }

        return lock;
    }

}
