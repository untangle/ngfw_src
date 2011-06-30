/* $HeadURL$ */
package com.untangle.uvm.reports.jabsorb;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.SkinManager;
import com.untangle.uvm.reports.RemoteReportingManager;

import com.untangle.uvm.webui.jabsorb.serializer.EnumSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.ExtendedListSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.ExtendedSetSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.HostAddressSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.HostNameSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.IPMaskedAddressSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.IPMatcherSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.IPAddressSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.InetAddressSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.LazyInitializerSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.MimeTypeSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.PortMatcherSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.IntfMatcherSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.ProtocolMatcherSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.RFC2253NameSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.TimeMatcherSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.TimeSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.TimeZoneSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.URLSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.UserMatcherSerializer;
import org.apache.log4j.Logger;
import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCServlet;
import org.jabsorb.serializer.impl.JSONBeanSerializer;

/**
 * Initializes the JSONRPCBridge.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class UtJsonRpcServlet extends JSONRPCServlet
{
    private static final String BRIDGE_ATTRIBUTE = "ReportsJSONRPCBridge";

    private final Logger logger = Logger.getLogger(getClass());

    private InheritableThreadLocal<HttpServletRequest> threadRequest;

    // HttpServlet methods ----------------------------------------------------

    @SuppressWarnings("unchecked") //getAttribute
    public void init()
    {
        threadRequest = (InheritableThreadLocal<HttpServletRequest>)getServletContext().getAttribute("threadRequest");
        if (null == threadRequest) {
            logger.warn("could not get threadRequest");
        }
    }

    public void service(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        if (null != threadRequest) {
            threadRequest.set(req);
        }

        initSessionBridge(req);

        super.service(req, resp);

        if (null != threadRequest) {
            threadRequest.set(null);
        }
    }

    /**
     * Find the JSONRPCBridge from the current session.
     * If it can't be found in the session, or there is no session,
     * then return the global bridge.
     *
     * @param request The message received
     * @return the JSONRPCBridge to use for this request
     */
    protected JSONRPCBridge findBridge(HttpServletRequest request)
    {
        // Find the JSONRPCBridge for this session or create one
        // if it doesn't exist
        HttpSession session = request.getSession( false );
        JSONRPCBridge jsonBridge = null;
        if (session != null) jsonBridge = (JSONRPCBridge) session.getAttribute( BRIDGE_ATTRIBUTE );

        if ( jsonBridge == null) {
            /* Use the global bridge if it can't find the session bridge. */
            jsonBridge = JSONRPCBridge.getGlobalBridge();
            if ( logger.isDebugEnabled()) logger.debug("Using global bridge.");
        }
        return jsonBridge;
    }

    // private methods --------------------------------------------------------

    private void initSessionBridge(HttpServletRequest req)
    {
        HttpSession s = req.getSession();
        JSONRPCBridge b = (JSONRPCBridge)s.getAttribute(BRIDGE_ATTRIBUTE);

        if (null == b) {
            b = new JSONRPCBridge();
            s.setAttribute(BRIDGE_ATTRIBUTE, b);

            try {
                // general serializers
                b.registerSerializer(new JSONBeanSerializer());
                b.registerSerializer(new EnumSerializer());
                b.registerSerializer(new URLSerializer());
                b.registerSerializer(new InetAddressSerializer());
                b.registerSerializer(new TimeSerializer());
                // uvm related serializers
                b.registerSerializer(new IPMaskedAddressSerializer());
                b.registerSerializer(new IPAddressSerializer());
                b.registerSerializer(new HostNameSerializer());
                b.registerSerializer(new HostAddressSerializer());
                b.registerSerializer(new TimeZoneSerializer());

                b.registerSerializer(new MimeTypeSerializer());
                b.registerSerializer(new RFC2253NameSerializer());
                // hibernate related serializers
                b.registerSerializer(new LazyInitializerSerializer());
                b.registerSerializer(new ExtendedListSerializer());
                b.registerSerializer(new ExtendedSetSerializer());

                // firewal related serializers
                b.registerSerializer(new ProtocolMatcherSerializer());
                b.registerSerializer(new IPMatcherSerializer());
                b.registerSerializer(new PortMatcherSerializer());
                b.registerSerializer(new IntfMatcherSerializer());
                b.registerSerializer(new TimeMatcherSerializer());
                b.registerSerializer(new UserMatcherSerializer());

            } catch (Exception e) {
                logger.warn( "Unable to register serializers", e );
            }

            b.setCallbackController(new UtCallbackController(b));

            ReportsContext rc = ReportsContextImpl.makeReportsContext();
            b.registerObject("ReportsContext", rc, ReportsContext.class);
        }
    }

    public interface ReportsContext
    {
        public RemoteReportingManager reportingManager();

        public SkinManager skinManager();

        public LanguageManager languageManager();                
    }
}
