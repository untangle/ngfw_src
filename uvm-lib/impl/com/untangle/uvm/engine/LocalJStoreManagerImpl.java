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

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.UnmarshallException;

import com.untangle.uvm.LocalJStoreManager;

public class LocalJStoreManagerImpl implements LocalJStoreManager {
    public static final Pattern VALID_CHARACTERS = Pattern
            .compile("^[a-zA-Z0-9]+$");
    public static final String SINGLETON_ID = "singleton";
    /**
     * Formatting for the version string (yyyy-mm-dd-hhmm)
     */
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(
            "yyyy-MM-dd-HHmm");

    private static final EntryComparator ENTRY_COMPARATOR = new EntryComparator();

    private String basePath = "/usr/share/untangle/conf/jstore";
    private JSONSerializer serializer = null;

    private final Map<String, Object> pathLocks = new HashMap<String, Object>();

    String getBasePath() {
        return this.basePath;
    }

    void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    /**
     * @param serializer
     *            the serializer to set
     */
    void setSerializer(JSONSerializer serializer) {
        this.serializer = serializer;
    }

    /**
     * @return the serializer
     */
    JSONSerializer getSerializer() {
        return serializer;
    }

    /**
     * Load the settings from the store for a singleton.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @return The object that was loaded or null if an object was not loaded.
     * @throws TransactionException
     */
    public <T> T load(Class<T> clz, String packageName)
            throws TransactionException {
        return load(clz, packageName, SINGLETON_ID);
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
     * @throws TransactionException
     */
    public <T> T load(Class<T> clz, String packageName, String id)
            throws TransactionException {
        if (!VALID_CHARACTERS.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid id value: '" + id + "'");
        }

        return loadImpl(clz, packageName, id);
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
     * @param k
     *            Name of the key for v.
     * @param v
     *            Value for the key k.
     * @return The object that was loaded or null if an object was not loaded.
     * @throws TransactionException
     */
    public <T> T load(Class<T> clz, String packageName, String k, String v)
            throws TransactionException {
        return loadImpl(clz, packageName, buildQuery(k, v));
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
     * @param criteria
     *            Map of key value pairs to select on.
     * @return The object that was loaded or null if an object was not loaded.
     * @throws TransactionException
     */
    public <T> T load(Class<T> clz, String packageName,
            Map<String, String> criteria) throws TransactionException {
        return loadImpl(clz, packageName, buildQuery(criteria));
    }

    /**
     * Save the settings from the store for a singleton.
     * 
     * @param <T>
     *            Type of class to load
     * @param clz
     *            Type of class to load.
     * @param packageName
     *            Name of the debian package that is making the request.
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws TransactionException
     */
    public <T> T save(Class<T> clz, String packageName, T value)
            throws TransactionException {
        return save(clz, packageName, SINGLETON_ID, value);
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
     * @throws TransactionException
     */
    public <T> T save(Class<T> clz, String packageName, String id, T value)
            throws TransactionException {
        if (!VALID_CHARACTERS.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid id value: '" + id + "'");
        }

        return saveImpl(clz, packageName, id, value);
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
     * @param k
     *            Name of the key for v.
     * @param v
     *            Value for the key k.
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws TransactionException
     */
    public <T> T save(Class<T> clz, String packageName, String k, String v,
            T value) throws TransactionException {
        return saveImpl(clz, packageName, buildQuery(k, v), value);
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
     * @param criteria
     *            Map of key value pairs to select on.
     * @param value
     *            The value to be saved.
     * @return The object that was saved.
     * @throws TransactionException
     */
    public <T> T save(Class<T> clz, String packageName,
            Map<String, String> criteria, T value) throws TransactionException {
        return saveImpl(clz, packageName, buildQuery(criteria), value);
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
     * @throws TransactionException
     */
    private <T> T loadImpl(Class<T> clz, String packageName, String query)
            throws TransactionException {
        File f = buildHeadPath(clz, packageName, query);
        if (!f.exists()) {
            return null;
        }

        Object lock = this.getLock(f);

        synchronized (lock) {
            BufferedReader reader = null;
            try {
                StringBuilder jsonString = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(f)));

                char buffer[] = new char[1024];
                while (true) {
                    if (reader.read(buffer) <= 0) {
                        break;
                    }

                    jsonString.append(buffer);
                }

                return (T) serializer.fromJSON(jsonString.toString());

            } catch (IOException e) {
                throw new TransactionException("Unable to load the file: '" + f
                        + "'", e);
            } catch (UnmarshallException e) {
                throw new TransactionException(
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

    private <T> T saveImpl(Class<T> clz, String packageName, String query,
            T value) throws TransactionException {
        File link = buildHeadPath(clz, packageName, query);
        File output = buildVersionPath(clz, packageName);
        File history = buildHistoryPath(clz, packageName);

        Object lock = this.getLock(output);

        /*
         * Synchronized on the name of the parent directory, so two files cannot
         * modify the same file at the same time
         */
        synchronized (lock) {
            if (output.exists()) {
                throw new TransactionException("Output file '"
                        + output.toString() + "'already exists!");
            }

            FileWriter fileWriter = null;
            try {
                File parentFile = output.getParentFile();

                /* Create the directory structure */
                parentFile.mkdirs();

                fileWriter = new FileWriter(output);
                String json = this.serializer.toJSON(value);
                fileWriter.write(json);
                fileWriter.close();
                fileWriter = null;

                /*
                 * Why must SUN/Oracle try everyone's patience; The API for
                 * creating symbolic links is in Java 1.7
                 */
                String cmdArray[] = new String[] { "ln", "-sf",
                        output.toString(), link.toString() };

                Process process = UvmContextImpl.context().exec(cmdArray);
                int exitCode = process.waitFor();
                String line = null;
                BufferedReader tmp = new BufferedReader(new InputStreamReader(
                        process.getInputStream()));
                while ((line = tmp.readLine()) != null) {
                    System.out.println("out: " + line);
                }
                tmp = new BufferedReader(new InputStreamReader(process
                        .getInputStream()));
                while ((line = tmp.readLine()) != null) {
                    System.out.println("err: " + line);
                }

                if (exitCode != 0) {
                    throw new TransactionException(
                            "Unable to create symbolic link[" + exitCode + "]");
                }
                process.destroy();

                /* Append the history to the end of the history file. */
                fileWriter = new FileWriter(history, true);

                fileWriter.append(String.valueOf(System.currentTimeMillis()));
                fileWriter.append(": ").append(link.getName()).append(" ")
                        .append(output.getName()).append("\n");
                fileWriter.close();
                fileWriter = null;
            } catch (IOException e) {
                throw new TransactionException("Unable to load the file: '"
                        + output + "'", e);
            } catch (MarshallException e) {
                throw new TransactionException(
                        "Unable to marshal json string:", e);
            } catch (InterruptedException e) {
                throw new TransactionException(
                        "Unable to create symbolic link:", e);
            } finally {
                try {
                    if (fileWriter != null) {
                        fileWriter.close();
                    }
                } catch (Exception e) {
                }
            }
            return loadImpl(clz, packageName, query);
        }
    }

    private String buildQuery(String k, String v) {
        if (!VALID_CHARACTERS.matcher(k).matches()) {
            throw new IllegalArgumentException("Invalid key value: '" + v + "'");
        }

        if (!VALID_CHARACTERS.matcher(v).matches()) {
            throw new IllegalArgumentException("Invalid value: '" + v + "'");
        }

        return k + "_" + v;
    }

    private String buildQuery(Map<String, String> criteria) {
        StringBuilder queryBuilder = new StringBuilder();

        List<Map.Entry<String, String>> criteriaList = new LinkedList<Map.Entry<String, String>>(
                criteria.entrySet());

        /* Sort the entries alphabetically by the keys */
        Collections.sort(criteriaList, ENTRY_COMPARATOR);

        for (Map.Entry<String, String> entry : criteriaList) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append("-");
            }

            String k = entry.getKey();
            String v = entry.getValue();

            if (!VALID_CHARACTERS.matcher(k).matches()) {
                throw new IllegalArgumentException("Invalid key value: '" + k
                        + "'");
            }

            if (!VALID_CHARACTERS.matcher(v).matches()) {
                throw new IllegalArgumentException("Invalid value: '" + v + "'");
            }

            queryBuilder.append(k + "_" + v);
        }

        return queryBuilder.toString();
    }

    private File buildHeadPath(Class<?> clz, String packageName, String query) {
        String clzName = clz.getCanonicalName();
        if (clzName == null) {
            throw new IllegalArgumentException("null canonical name: '"
                    + clz.toString() + "'");
        }

        /* First build the file string */
        String s = File.separator;
        return new File(this.basePath + s + packageName + s + clzName + s
                + "head-" + query + ".js");
    }

    private File buildVersionPath(Class<?> clz, String packageName) {
        String clzName = clz.getCanonicalName();
        if (clzName == null) {
            throw new IllegalArgumentException("null canonical name: '"
                    + clz.toString() + "'");
        }

        /* First build the file string */
        String s = File.separator;

        String versionString = String.valueOf(System.currentTimeMillis()) + "-"
                + DATE_FORMATTER.format(new Date());
        return new File(this.basePath + s + packageName + s + clzName + s
                + "version-" + versionString + ".js");
    }

    private File buildHistoryPath(Class<?> clz, String packageName) {
        String clzName = clz.getCanonicalName();
        if (clzName == null) {
            throw new IllegalArgumentException("null canonical name: '"
                    + clz.toString() + "'");
        }

        /* First build the file string */
        String s = File.separator;

        return new File(this.basePath + s + packageName + s + clzName + s
                + "history.txt");
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
    private Object getLock(File file) {
        String path = file.getParentFile().getAbsolutePath();
        Object lock = this.pathLocks.get(path);
        if (lock == null) {
            lock = new Object();
            this.pathLocks.put(path, lock);
        }

        return lock;
    }

    private static class EntryComparator implements
            Comparator<Map.Entry<String, String>> {
        public int compare(Map.Entry<String, String> o1,
                Map.Entry<String, String> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    }

    public static void main(String args[]) throws Exception {
        LocalJStoreManagerImpl jStore = new LocalJStoreManagerImpl();

        JSONSerializer serializer = new JSONSerializer();
        serializer.registerDefaultSerializers();

        jStore.setBasePath("/tmp/simple");
        jStore.setSerializer(serializer);

        String v = jStore.save(String.class, "simple-test", "This is a value");
        System.out.printf("Saved and then loaded the string '%s'\n", v);

        List<String> list = new LinkedList<String>();
        list.add("a");
        list.add("b");
        list.add("c");

        List<String> serializedList = (List<String>) jStore.save(List.class,
                "simple-test", "key", "value", list);
        System.out.printf("Saved and then loaded the string '%s'\n",
                serializedList.toString());

        Map<String, String> m = new HashMap<String, String>();
        m.put("tid", "1");
        m.put("k", "v");
        jStore.save(List.class, "simple-test", m, list);
    }
}
