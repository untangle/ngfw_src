/**
 * $Id$
 */
package com.untangle.uvm;


import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * This class converts exception stacktrace by adding the app name and formats it.
 * The error message is supplied by either setting the system property
 */
@Plugin(name = "UvmExceptionPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"uvm"})
public class UvmExceptionPatternConverter extends LogEventPatternConverter {

    private final String prefix;
    private final SimpleDateFormat dateFormat;
    private static final String LOGGINGHOST = "localhost ";
    private static final String DATETIMEPATTERN= "MMM dd HH:mm:ss";

    /**
     * @param name
     * @param style
     * @param prefix
     */
    protected UvmExceptionPatternConverter(String name, String style, String prefix) {
        super(name, style);
        this.prefix = prefix;
        this.dateFormat = new SimpleDateFormat(DATETIMEPATTERN);
    }

    /**
     * @param options
     * @return  UvmExceptionPatternConverter      
     */
    public static UvmExceptionPatternConverter newInstance(String[] options) {
        String prefix = (options != null && options.length > 0) ? options[0] : "";
        return new UvmExceptionPatternConverter("uvm", "uvm", prefix);
    }


    /**
     * @param event
     * @param builder      
     */
    @Override
    public void format(LogEvent event, StringBuilder builder) {
        Throwable throwable = event.getThrown();
        String timestamp = dateFormat.format(new Date(event.getInstant().getEpochMillisecond()));
        if (throwable != null) {
            builder.append(timestamp).append(" ").append(LOGGINGHOST).append(this.prefix).append(":      ").append(throwable).append(System.lineSeparator());
            for (StackTraceElement element : throwable.getStackTrace()) {
                builder.append(timestamp).append(" ").append(LOGGINGHOST).append(this.prefix).append(":      ").append(element.toString()).append(System.lineSeparator());
            }
        }
    }

}
