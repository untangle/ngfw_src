/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.log4j.Logger;

/**
 * Sends a friendly error page when a problem occurs.
 * The error message is supplied by either setting the system property
 * {
 * @link #UVM_WEB_MESSAGE_ATTR} or by using the {
 * @link HttpServletResponse.sendError(int, String))} method.
 */
public class UvmErrorReportValve extends ErrorReportValve
{
    public static final String UVM_WEB_MESSAGE_ATTR = "com.untangle.uvm.web.message";

    private static final Logger logger = Logger.getLogger(UvmErrorReportValve.class);

    /**
     * report
     * @param request
     * @param response
     * @param throwable
     */
    protected void report(Request request, Response response, Throwable throwable)
    {
        try {
            doReport(request, response, throwable);
        } catch (Throwable t) {
            logger.warn("could not make error page", t);
        }
    }

    /**
     * doReport
     * @param request
     * @param response
     * @param throwable
     * @throws IOException
     */
    protected void doReport(Request request, Response response, Throwable throwable)
        throws IOException
    {
        int statusCode = response.getStatus();
        if ((statusCode < 400) || (response.getContentWritten() > 0)) {
            return;
        }

        Map<String, String> i18nMap = getTranslations();
        String errorMessage = null;

        Object o = request.getAttribute(UVM_WEB_MESSAGE_ATTR);
        if (o instanceof String) {
            errorMessage = (String)o;
        }

        if (errorMessage == null) {
            errorMessage = getError(i18nMap, statusCode);
            if ( response.getMessage() != null ) {
                errorMessage += " - ";
                errorMessage += response.getMessage();
            }
            if (throwable != null ) {
                errorMessage += " - ";
                errorMessage += throwable.toString();
            }
        }

        errorMessage = RequestUtil.filter(errorMessage);

        response.setContentType("text/html");
        response.setCharacterEncoding("utf-8");
        PrintWriter writer = response.getReporter();
        if (null != writer) {
            writeReport(writer, i18nMap, errorMessage);
        }
    }

    /**
     * writeReport
     * @param w
     * @param i18nMap
     * @param errorMessage
     * @throws IOException
     */
    private void writeReport(PrintWriter w, Map<String, String> i18nMap, String errorMessage)
        throws IOException
    {
        String companyName = Main.getMain().getCompanyName();

        w.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
        w.write("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        w.write("<head>\n");
        w.write("<title>");
        w.write(tr("{0} Server", companyName, i18nMap));
        w.write("</title>\n");
        w.write("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\" />\n");
        w.write("<style type=\"text/css\">\n");
        w.write("/* <![CDATA[ */\n");
        w.write("@import url(/images/base.css);\n");
        w.write("/* ]]> */\n");
        w.write("</style>\n");
        w.write("</head>\n");
        w.write("<body>\n");
        w.write("<div id=\"main\" style=\"width:500px;margin:50px auto 0 auto;\">\n");
        w.write("<div class=\"main-top-left\"></div><div class=\"main-top-right\"></div><div class=\"main-mid-left\"><div class=\"main-mid-right\"><div class=\"main-mid\">\n");
        w.write("<center>");
        w.write("<img alt=\"\" src=\"/images/BrandingLogo.png\" style=\"max-width: 150px; max-height: 140px;\"/><br /><br />\n");
        w.write("<b>");
        w.write(tr("{0} Server", companyName, i18nMap));
        w.write("</b><br /><br />\n");
        w.write("<em>");  w.write(errorMessage);  w.write("</em>\n");
        w.write("</center><br /><br />\n");
        w.write("</div></div></div><div class=\"main-bot-left\"></div><div class=\"main-bot-right\"></div>\n");
        w.write("</div>\n");
        w.write("</body>\n");
        w.write("</html>\n");
    }

    /**
     * Get the translation map
     * @return map
     */
    private Map<String, String> getTranslations()
    {
        return Main.getMain().getTranslations("untangle");
    }

    /**
     * translate a string
     * @param value - the string
     * @param i18nMap - the translation map
     * @return String - the translated String
     */
    private String tr(String value, Map<String, String> i18nMap)
    {
        String tr = i18nMap.get(value);
        return null == tr ? value : tr;
    }

    /**
     * translate a string
     * @param value - the string
     * @param objects 
     * @param  i18nMap - the translation map
     * @return String - the translated String
     */
    private String tr(String value, Object[] objects, Map<String, String> i18nMap)
    {
        return MessageFormat.format( tr(value,i18nMap), objects);
    }

    /**
     * translate a string
     * @param value - the string
     * @param o1
     * @param  i18nMap - the translation map
     * @return String - the translated String
     */
    private String tr(String value, Object o1, Map<String, String> i18nMap)
    {
        return tr(value, new Object[]{ o1 }, i18nMap);
    }

    /**
     * getError - get the translate error code message
     * @param  i18nMap - the translation map
     * @param errorCode - http error code
     * @return String - the translated String
     */
    private String getError(Map<String, String> i18nMap, int errorCode)
    {
        switch (errorCode) {
        case 400:
            return tr("Bad Request", i18nMap);

        case 401:
            return tr("Unauthorized", i18nMap);

        case 402:
            return tr("Payment Required", i18nMap);

        case 403:
            return tr("Forbidden", i18nMap);

        case 404:
            return tr("Not Found", i18nMap);

        case 405:
            return tr("Method Not Allowed", i18nMap);

        case 406:
            return tr("Not Acceptable", i18nMap);

        case 407:
            return tr("Proxy Authentication Required", i18nMap);

        case 408:
            return tr("Request Timeout", i18nMap);

        case 409:
            return tr("Conflict", i18nMap);

        case 410:
            return tr("Gone", i18nMap);

        case 411:
            return tr("Length Required", i18nMap);

        case 412:
            return tr("Precondition Failed", i18nMap);

        case 413:
            return tr("Request Entity Too Large", i18nMap);

        case 414:
            return tr("Request-URI Too Long", i18nMap);

        case 415:
            return tr("Unsupported Media Type", i18nMap);

        case 416:
            return tr("Requested Range Not Satisfiable", i18nMap);

        case 417:
            return tr("Expectation Failed", i18nMap);

        case 500:
            return tr("Internal Server Error", i18nMap);

        case 501:
            return tr("Not Implemented", i18nMap);

        case 502:
            return tr("Bad Gateway", i18nMap);

        case 503:
            return tr("Service Unavailable", i18nMap);

        case 504:
            return tr("Gateway Timeout", i18nMap);

        case 505:
            return tr("HTTP Version Not Supported", i18nMap);

        default:
            return tr("Error", i18nMap);
        }
    }
}
