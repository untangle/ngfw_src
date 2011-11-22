package com.untangle.node.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.LevelRangeFilter;
import org.jabsorb.JSONSerializer;

import com.untangle.uvm.servlet.ServletUtils;

/**
 * This is a command line utility for adding a main to a class.  If you create an instance with base, it allows you to run all of the public methods using JSON on the comand line.
 * For instance, if you define an object A
 * @author rbscott
 *
 */
public class CommandLineUtil<T>
{
    private final JSONSerializer serializer = new JSONSerializer();
    private final TestInitializer<T> testInitializer;

    public CommandLineUtil(TestInitializer<T> testInitializer) throws Exception {
        this.testInitializer = testInitializer;
        ServletUtils.getInstance().registerSerializers(serializer);
    }
    
    /* Change the system property com.untangle.clu.log4j to change logging level */
    public void setLog4JLevel(LevelRangeFilter filter)
    {
        String level = System.getProperty("com.untangle.clu.log4j", "INFO");
        filter.setLevelMin(Level.toLevel(level));
        filter.setLevelMax(Level.FATAL);
    }
    
    public void main(String[] args) throws Exception {
        /* Initialize Log4J */
        ConsoleAppender appender = new ConsoleAppender(new PatternLayout());
        LevelRangeFilter filter = new LevelRangeFilter();
        setLog4JLevel(filter);
        appender.addFilter(filter);
        appender.setTarget(ConsoleAppender.SYSTEM_OUT);
        appender.setName("testing");
        appender.activateOptions();
        BasicConfigurator.configure(appender);
        
        args = this.testInitializer.parseArgs(args, serializer);
        T base = this.testInitializer.initTest();

        if (args.length > 0) {
            Object o = runCommand(base, args[0], args[1]);
            dumpObject(o);
        } else {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    System.in));
            String line;

            while ((line = in.readLine()) != null) {
                String[] commandArray = line.split(" ", 2);
                if (commandArray.length == 0) {
                    continue;
                }
                
                String methodName = commandArray[0].trim();
                if ("exit".equalsIgnoreCase(commandArray[0])) {
                    System.out.println("Exiting.");
                    break;
                }
                
                if ( methodName.length() == 0 ) {
                    continue;
                }

                try {

                    Object o = null;
                    if (commandArray.length == 2) {
                        o = runCommand(base, methodName, commandArray[1]);
                    } else if (commandArray.length == 1) {
                        o = runCommand(base, methodName, null);
                    }
                    dumpObject(o);
                } catch (NoSuchMethodException e) {
                    System.out.printf("The method '%s' doesn't exist\n",
                            methodName);
                    e.printStackTrace(System.out);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Object runCommand(T base, String methodName, String params) throws Exception {
        Object[] args = new Object[0];

        if (params != null) {
            params = String.format( "{ 'javaClass' : 'java.util.ArrayList', list : [%s]}", params);
            args = ((List<Object>)this.serializer.fromJSON(params)).toArray();
        }

        Class[] argsClass = new Class[args.length];

        for (int c = 0; c < args.length; c++) {
            Class clz = null;

            if ( args[c] != null ) {
                clz = args[c].getClass();
            } 
            
            if ( clz == Boolean.class ) {
                clz = boolean.class;
            }
            argsClass[c] = clz;
        }

        Method m = base.getClass().getMethod(methodName, argsClass);
        return this.testInitializer.run(m, base, args);
    }

    @SuppressWarnings("unchecked") 
    public void dumpObject( Object o ) throws Exception
    {
        if ( o instanceof Iterable<?> ) {
            Iterable<Object> iterable = (Iterable<Object>)o;
            for ( Object item : iterable ) {
                System.out.println(this.serializer.toJSON(item));
            }
        } else {
            System.out.println(this.serializer.toJSON(o));
        }
    }
    
    public interface TestInitializer<T>
    {
        /**
         * Use this method to strip off args. Return a new array after consuming
         * any used arguments. EG. If you get an array with 2 arguments, but
         * need to consume the first one. Take the first argument, and then
         * return a new array with just the last one.
         * 
         * @param args
         *            The array that was passed in from the command line.
         * @return The new array with any arguments take away.
         */
        public String[] parseArgs(String args[], JSONSerializer serializer) throws Exception;
        
        /**
         * Initial the object for testing. You should save the values from
         * parseArgs if you need some command line arguments.
         * 
         * @return
         * @throws Exception
         */
        public T initTest() throws Exception;
        
        /**
         * To get around IllegalAccessException, you have to call from the testing class.
         * @param m Method object to call.
         * @param base This is the object that is being invoked on.
         * @param args Arguments to pass to the object.
         * @return Just call return m.invoke(base, args); and you will be fine.
         */
        public Object run(Method m, T base, Object[] args ) throws Exception;
    }
}

