/**
 * To use this, rename it ExtensionImpl.java
 * put it in ~/work/src/reports/src/com.untangle.app/reports
 * then run rake
 * then restart the UVM and tail console.log
 * It will attempt to print the event object documentation into console.log
 * If it is missing descriptions or attributes it will throw a runtime exception
 */
package com.untangle.uvm;

import java.io.File;
import java.io.FileWriter;

import java.util.HashMap;

import org.apache.log4j.Logger;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import com.untangle.uvm.logging.LogEvent;

/**
 * ExtensionImpl
 */
public class ExtensionImpl implements Runnable
{
    private static final Logger logger = Logger.getLogger( ExtensionImpl.class );

    private static final String CLASS_EVENTS_JSON_FILE_NAME = "/tmp/classFields.json";

    private FileWriter FileWriter = null;
    private HashMap<String,String> attributeDescriptions = new HashMap<>();
    private HashMap<String,String> classDescriptions = new HashMap<>();
    private HashMap<String,HashMap<String,String>> classSpecificAttributeDescriptions = new HashMap<>();

    /**
     * ExtensionImpl
     */
    private ExtensionImpl()
    {
        classDescriptions.put("AdminLoginEvent","These events are created by the base system and inserted to the [[Database_Schema#admin_logins|admin_logins]] table when the host table is modified.");
        classDescriptions.put("UserTableEvent","These events are created by the base system and inserted to the [[Database_Schema#host_table_updates|host_table_updates]] table when the host table is modified.");
        classDescriptions.put("HostTableEvent","These events are created by the base system and inserted to the [[Database_Schema#host_table_updates|host_table_updates]] table when the host table is modified.");
        classDescriptions.put("DeviceTableEvent","These events are created by the base system and inserted to the [[Database_Schema#device_table_updates|device_table_updates]] table when the device list is modified.");
        classDescriptions.put("SessionStatsEvent","These events are created by the base system and update the [[Database_Schema#sessions|sessions]] table when a session ends with the updated stats.");
        classDescriptions.put("SessionEvent","These events are created by the base system and update the [[Database_Schema#sessions|sessions]] table each time a session is created.");
        classDescriptions.put("SessionMinuteEvent","These events are created by the base system and update the [[Database_Schema#sessions|session_minutes]] table each minute a session exists.");
        classDescriptions.put("SessionNatEvent","These events are created by the base system and update the [[Database_Schema#sessions|sessions]] table each time a session is NATd with the post-NAT information.");
        classDescriptions.put("QuotaEvent","These events are created by the [[Bandwidth Control]] and inserted or update the [[Database_Schema#quotas|quotas]] table when quotas are given or exceeded.");
        classDescriptions.put("PrioritizeEvent","These events are created by the [[Bandwidth Control]] and update the [[Database_Schema#sessions|session]] table when a session is prioritized.");
        classDescriptions.put("SettingsChangesEvent","These events are created by the base system and inserted to the [[Database_Schema#settings_changes|settings_changes]] table when settings are changed.");
        classDescriptions.put("LogEvent","These base class for all events.");
        classDescriptions.put("InterfaceStatEvent","These events are created by the base system and inserted to the [[Database_Schema#settings_changes|interface_stat_events]] table periodically with interface stats.");
        classDescriptions.put("StatisticEvent","These events are created by the base system and inserted to the the [[Database_Schema#settings_changes|interface_stat_events]] table periodically with interface stats.");
        classDescriptions.put("SystemStatEvent","These events are created by the base system and inserted to the [[Database_Schema#server_events|server_events]] table periodically.");
        classDescriptions.put("TunnelStatusEvent","These events are created by [[IPsec VPN]] and inserted to the [[Database_Schema#ipsec_tunnel_stats|ipsec_tunnel_stats]] table periodically.");
        classDescriptions.put("IpsecVpnEvent","These events are created by [[IPsec VPN]] and inserted to the [[Database_Schema#ipsec_vpn_events|ipsec_vpn_events]] table when an IPsec connection event occurs.");
        classDescriptions.put("TunnelVpnEvent","These events are created by [[Tunnel VPN]] and inserted to the [[Database_Schema#tunnel_vpn_events|tunnel_vpn_events]] table when a tunnel connection event occurs.");
        classDescriptions.put("TunnelVpnStatusEvent","These events are created by [[Tunnel VPN]] and inserted to the [[Database_Schema#tunnel_vpn_stats|tunnel_vpn_stats]] table periodically.");
        classDescriptions.put("VirtualUserEvent","These events are created by [[IPsec VPN]] and inserted to the [[Database_Schema#ipsec_user_events|ipsec_user_events]] table when a user event occurs.");
        classDescriptions.put("WireguardVpnStats","These events are created by [[Wireguard VPN]] and inserted to the [[Database_Schema#wireguard_vpn_stats|wireguard_vpn_stats]] table periodically.");
        classDescriptions.put("AlertEvent","These events are created by [[Reports]] and inserted to the [[Database_Schema#alerts|alerts]] table when an alert fires.");
        classDescriptions.put("ConfigurationBackupEvent","These events are created by [[Configuration Backup]] and inserted to the [[Database_Schema#configuratio_backup_events|configuratio_backup_events]] table when a backup occurs.");
        classDescriptions.put("WebCacheEvent","These events are created by [[Web Cache]] and inserted to the [[Database_Schema#web_cache_stats|web_cache_stats]] table periodically.");
        classDescriptions.put("HttpResponseEvent","These events are created by HTTP subsystem and update the [[Database_Schema#http_events|http_events]] table when a web response happens.");
        classDescriptions.put("HttpRequestEvent","These events are created by HTTP subsystem and inserted to the [[Database_Schema#http_events|http_events]] table when a web request happens.");
        classDescriptions.put("ApplicationControlLiteEvent","These events are created by [[Application Control Lite]] and update the [[Database_Schema#sessions|sessions]] table when application control lite identifies a session.");
        classDescriptions.put("ApplicationControlLogEvent","These events are created by [[Application Control]] and update the [[Database_Schema#sessions|sessions]] table when application control identifies a session.");
        classDescriptions.put("FirewallEvent","These events are created by [[Firewall]] and update the [[Database_Schema#sessions|sessions]] table when a firewall rule matches a session.");
        classDescriptions.put("WebFilterEvent","These events are created by [[Web Filter]] and update the [[Database_Schema#http_events|http_events]] table when web filter processes a web request.");
        classDescriptions.put("WebFilterQueryEvent","These events are created by [[Web Filter]] and inserted to the [[Database_Schema#http_query_events|http_query_events]] table when web filter processes a search engine search.");
        classDescriptions.put("SslInspectorLogEvent","These events are created by [[SSL Inspector]] and update the [[Database_Schema#sessions|sessions]] table when a session is processed by SSL Inspector.");
        classDescriptions.put("SpamSmtpTarpitEvent","These events are created by [[Spam Blocker]] and inserted to the [[Database_Schema#smtp_tarpit_events|smtp_tarpit_events]] table when a session is tarpitted.");
        classDescriptions.put("SpamLogEvent","These events are created by [[Spam Blocker]] and update the [[Database_Schema#mail_msgs|mail_msgs]] table when an email is scanned.");
        classDescriptions.put("CookieEvent","These events are created by [[Ad Blocker]] and update the [[Database_Schema#http_events|http_events]] table when a cookie is blocked.");
        classDescriptions.put("AdBlockerEvent","These events are created by [[Ad Blocker]] and update the [[Database_Schema#http_events|http_events]] table when an ad is blocked.");
        classDescriptions.put("IntrusionPreventionLogEvent","These events are created by [[Intrusion Prevention]] and inserted to the [[Database_Schema#intrusion_prevention_events|intrusion_prevention_events]] table when a rule matches.");
        classDescriptions.put("WanFailoverTestEvent","These events are created by [[WAN Failover]] and inserted to the [[Database_Schema#wan_failover_test_events|wan_failover_test_events]] table when a test is run.");
        classDescriptions.put("WanFailoverEvent","These events are created by [[WAN Failover]] and inserted to the [[Database_Schema#wan_failover_action_events|wan_failover_action_events]] table when WAN Failover takes an action.");
        classDescriptions.put("CaptivePortalUserEvent","These events are created by [[Captive Portal]] and inserted to the [[Database_Schema#captive_portal_user_events|captive_portal_user_events]] table when Captive Portal user takes an action.");
        classDescriptions.put("CaptureRuleEvent","These events are created by [[Captive Portal]] and update the [[Database_Schema#sessions|sessions]] table when Captive Portal processes a session.");
        classDescriptions.put("VirusSmtpEvent","These events are created by [[Virus Blocker]] and update the [[Database_Schema#mail_msgs|mail_msgs]] table when Virus Blocker scans an email.");
        classDescriptions.put("VirusFtpEvent","These events are created by [[Virus Blocker]] and update the [[Database_Schema#ftp_events|ftp_events]] table when Virus Blocker scans an FTP transfer.");
        classDescriptions.put("VirusHttpEvent","These events are created by [[Virus Blocker]] and update the [[Database_Schema#http_events|http_events]] table when Virus Blocker scans an HTTP transfer.");
        classDescriptions.put("OpenVpnEvent","These events are created by [[OpenVPN]] and update the [[Database_Schema#openvpn_events|openvpn_events]] table when OpenVPN processes a client action.");
        classDescriptions.put("OpenVpnStatusEvent","These events are created by [[OpenVPN]] and update the [[Database_Schema#openvpn_stats|openvpn_stats]] table periodically.");
        classDescriptions.put("SmtpMessageAddressEvent","These events are created by SMTP subsystem and inserted to the [[Database_Schema#mail_addrs|mail_addrs]] table for each address on each email.");
        classDescriptions.put("SmtpMessageEvent","These events are created by SMTP subsystem and inserted to the [[Database_Schema#mail_msgs|mail_msgs]] table for each email.");
        classDescriptions.put("LoginEvent","These events are created by [[Directory Connector]] and inserted to the [[Database_Schema#directory_connector_login_events|directory_connector_login_events]] table for each login.");
        classDescriptions.put("ThreatPreventionEvent","These events are created by [[Threat Prevention]] and inserted to the [[Database_Schema#sessions|sessions]] table for each threat lookup.");
        classDescriptions.put("ThreatPreventionHttpEvent","These events are created by [[Threat Prevention]] and inserted to the [[Database_Schema#http_events|http_events]] table for each threat lookup.");

        attributeDescriptions.put("partitionTablePostfix","");
        attributeDescriptions.put("tag","");

        attributeDescriptions.put("address","The address");
        attributeDescriptions.put("class","The class name");
        attributeDescriptions.put("key","The key");
        attributeDescriptions.put("timeStamp","The timestamp");
        attributeDescriptions.put("value","The value");
        attributeDescriptions.put("macAddress","The MAC address");
        attributeDescriptions.put("device","The Device");
        attributeDescriptions.put("entryTime","The entry time");
        attributeDescriptions.put("exitTime","The exit time");
        attributeDescriptions.put("action","The action");
        attributeDescriptions.put("c2sBytes","The number of bytes sent from the client to the server");
        attributeDescriptions.put("s2cBytes","The number of bytes sent from the server to the client");
        attributeDescriptions.put("c2pBytes","The number of bytes sent from the client to Untangle");
        attributeDescriptions.put("c2pChunks","The number of chunks/packets sent from the client to Untangle");
        attributeDescriptions.put("p2cBytes","The number of bytes sent to the client from Untangle");
        attributeDescriptions.put("p2cChunks","The number of chunks/packets sent to the client from Untangle");
        attributeDescriptions.put("p2sBytes","The number of bytes sent to the server from Untangle");
        attributeDescriptions.put("p2sChunks","The number of chunks/packets sent to the server from Untangle");
        attributeDescriptions.put("s2pBytes","The number of bytes sent from the server to Untangle");
        attributeDescriptions.put("s2pChunks","The number of chunks/packets sent from the server to Untangle");
        attributeDescriptions.put("sessionId","The session ID");
        attributeDescriptions.put("CClientAddr","The client-side (pre-NAT) client address");
        attributeDescriptions.put("CClientPort","The client-side (pre-NAT) client port");
        attributeDescriptions.put("CServerAddr","The client-side (pre-NAT) server address");
        attributeDescriptions.put("CServerPort","The client-side (pre-NAT) server port");
        attributeDescriptions.put("SClientAddr","The server-side (post-NAT) client address");
        attributeDescriptions.put("SClientPort","The server-side (post-NAT) client port");
        attributeDescriptions.put("SServerAddr","The server-side (post-NAT) server address");
        attributeDescriptions.put("SServerPort","The server-side (post-NAT) server port");
        attributeDescriptions.put("bypassed","True if bypassed, false otherwise");
        attributeDescriptions.put("clientIntf","The client interface ID");
        attributeDescriptions.put("entitled","The entitled status");
        attributeDescriptions.put("filterPrefix","The filter prefix if blocked by the filter rules");
        attributeDescriptions.put("hostname","The hostname");
        attributeDescriptions.put("icmpType","The ICMP type");
        attributeDescriptions.put("policyId","The policy ID");
        attributeDescriptions.put("protocol","The protocol");
        attributeDescriptions.put("protocolName","The protocol name");
        attributeDescriptions.put("serverIntf","The server interface ID");
        attributeDescriptions.put("username","The username");
        attributeDescriptions.put("quotaSize","The quota size");
        attributeDescriptions.put("reason","The reason");
        attributeDescriptions.put("interfaceId","The interface ID");
        attributeDescriptions.put("rxRate","The RX rate in byte/s");
        attributeDescriptions.put("txRate","The TX rate in byte/s");
        attributeDescriptions.put("activeHosts","The active host count");
        attributeDescriptions.put("cpuSystem","The system CPU utilization");
        attributeDescriptions.put("cpuUser","The user CPU utilization");
        attributeDescriptions.put("diskUsed","The amount of disk used");
        attributeDescriptions.put("diskUsedPercent","The percentage of disk used");
        attributeDescriptions.put("diskFree","The amount of disk free");
        attributeDescriptions.put("diskFreePercent","The percentage of disk free");
        attributeDescriptions.put("diskTotal","The total size of the disk");
        attributeDescriptions.put("load1","The 1-minute CPU load");
        attributeDescriptions.put("load15","The 15-minute CPU load");
        attributeDescriptions.put("load5","The 5-minute CPU load");
        attributeDescriptions.put("memBuffers","The amount of memory used by buffers");
        attributeDescriptions.put("memCache","The amount of memory used by cache");
        attributeDescriptions.put("memUsed","The amount of used memory");
        attributeDescriptions.put("memUsedPercent","The percentage of total memory that is used");
        attributeDescriptions.put("memFree","The amount of free memory");
        attributeDescriptions.put("memFreePercent","The percentage of total memory that is free");
        attributeDescriptions.put("memTotal","The total amount of memory");
        attributeDescriptions.put("swapUsed","The amount of used swap");
        attributeDescriptions.put("swapUsedPercent","The percentage of total swap that is used");
        attributeDescriptions.put("swapFree","The amount of free swap");
        attributeDescriptions.put("swapFreePercent","The percentage of total swap that is free");
        attributeDescriptions.put("swapTotal","The total size of swap");
        attributeDescriptions.put("inBytes","The number of bytes received from this tunnel");
        attributeDescriptions.put("outBytes","The number of bytes sent in this tunnel");
        attributeDescriptions.put("localAddress","The local host address");
        attributeDescriptions.put("serverAddress","The server address");
        attributeDescriptions.put("tunnelName","The name of this tunnel");
        attributeDescriptions.put("clientAddress","The client address");
        attributeDescriptions.put("clientProtocol","The client protocol");
        attributeDescriptions.put("clientUsername","The client username");
        attributeDescriptions.put("elapsedTime","The elapsed time");
        attributeDescriptions.put("eventId","The event ID");
        attributeDescriptions.put("netInterface","The net interface");
        attributeDescriptions.put("netProcess","The net process");
        attributeDescriptions.put("netRXbytes","The number of RX (received) bytes");
        attributeDescriptions.put("netTXbytes","The number of TX (transmitted) bytes");
        attributeDescriptions.put("cause","The cause");
        attributeDescriptions.put("description","The description");
        attributeDescriptions.put("json","The JSON string");
        attributeDescriptions.put("summaryText","The summary text");
        attributeDescriptions.put("destination","The destination");
        attributeDescriptions.put("success","True if successful, false otherwise");
        attributeDescriptions.put("detail","The details");
        attributeDescriptions.put("bypassCount","The number of bypasses");
        attributeDescriptions.put("hitCount","The number of hits");
        attributeDescriptions.put("missCount","The number of misses");
        attributeDescriptions.put("systemCount","The number of system bypasses");
        attributeDescriptions.put("hitBytes","The number of bytes worth of hits");
        attributeDescriptions.put("missBytes","The number of bytes worth of misses");
        attributeDescriptions.put("priority","The priority");
        attributeDescriptions.put("ruleId","The rule ID");
        attributeDescriptions.put("sessionEvent","The session event");
        attributeDescriptions.put("contentFilename","The content filename");
        attributeDescriptions.put("contentLength","The content length");
        attributeDescriptions.put("contentType","The content type");
        attributeDescriptions.put("requestLine","The request line");
        attributeDescriptions.put("domain","The domain");
        attributeDescriptions.put("host","The host");
        attributeDescriptions.put("referer","The referer");
        attributeDescriptions.put("requestId","The request ID");
        attributeDescriptions.put("requestUri","The request URI");
        attributeDescriptions.put("blocked","True if blocked, false otherwise");
        attributeDescriptions.put("flagged","True if flagged, false otherwise");
        attributeDescriptions.put("category","The category");
        attributeDescriptions.put("appName","The name of the application");
        attributeDescriptions.put("application","The application");
        attributeDescriptions.put("protochain","The protochain");
        attributeDescriptions.put("state","The state");
        attributeDescriptions.put("confidence","The confidence (0-100)");
        attributeDescriptions.put("status","The status");
        attributeDescriptions.put("IPAddr","The IP address");
        attributeDescriptions.put("vendorName","The application name");
        attributeDescriptions.put("clientAddr","The client address");
        attributeDescriptions.put("clientPort","The client port");
        attributeDescriptions.put("clientCountry","The client country");
        attributeDescriptions.put("clientLatitude","The client latitude");
        attributeDescriptions.put("clientLongitude","The client longitude");
        attributeDescriptions.put("messageId","The message ID");
        attributeDescriptions.put("receiver","The receiver");
        attributeDescriptions.put("score","The score");
        attributeDescriptions.put("sender","The sender");
        attributeDescriptions.put("serverAddr","The server address");
        attributeDescriptions.put("serverPort","The server port");
        attributeDescriptions.put("serverCountry","The server country");
        attributeDescriptions.put("serverLatitude","The server latitude");
        attributeDescriptions.put("serverLongitude","The server longitude");
        attributeDescriptions.put("smtpMessageEvent","The parent SMTP message event");
        attributeDescriptions.put("isSpam","True if spam, false otherwise");
        attributeDescriptions.put("subject","The subject");
        attributeDescriptions.put("testsString","The tests string from the spam engine");
        attributeDescriptions.put("identification","The identification string");
        attributeDescriptions.put("classificationId","The classification ID");
        attributeDescriptions.put("classtype","The classtype");
        attributeDescriptions.put("dportIcode","The dportIcode"); //FIXME
        attributeDescriptions.put("eventMicrosecond","The event microsecond");
        attributeDescriptions.put("eventSecond","The event second");
        attributeDescriptions.put("eventType","The event type");
        attributeDescriptions.put("generatorId","The generator ID"); // FIXME
        attributeDescriptions.put("impact","The impact"); // FIXME
        attributeDescriptions.put("impactFlag","The impact flag"); // FIXME
        attributeDescriptions.put("ipDestination","The IP address destination");
        attributeDescriptions.put("ipSource","The IP address source");
        attributeDescriptions.put("mplsLabel","The mplsLabel"); // FIXME
        attributeDescriptions.put("msg","The msg"); // FIXME
        attributeDescriptions.put("padding","The padding"); // FIXME
        attributeDescriptions.put("priorityId","The priority ID");
        attributeDescriptions.put("sensorId","The sensor ID");
        attributeDescriptions.put("signatureId","The signature ID");
        attributeDescriptions.put("signatureRevision","The signature revision");
        attributeDescriptions.put("sportItype","The sportItype"); // FIXME
        attributeDescriptions.put("vlanId","The VLAN Id"); // FIXME
        attributeDescriptions.put("method","The method");
        attributeDescriptions.put("term","The search term/phrase");
        attributeDescriptions.put("name","The name");
        attributeDescriptions.put("osName","The O/S interface name");
        attributeDescriptions.put("authenticationType","The authentication type");
        attributeDescriptions.put("authenticationTypeValue","The authentication type as a string");
        attributeDescriptions.put("event","The event");
        attributeDescriptions.put("loginName","The login name");
        attributeDescriptions.put("captured","True if captured, false otherwise");
        attributeDescriptions.put("clean","True if clean, false otherwise");
        attributeDescriptions.put("virusName","The virus name, if not clean");
        attributeDescriptions.put("uri","The URI");
        attributeDescriptions.put("clientName","The client name");
        attributeDescriptions.put("poolAddress","The pool address");
        attributeDescriptions.put("type","The type");
        attributeDescriptions.put("bytesRxDelta","The delta number of RX (received) bytes from the previous event");
        attributeDescriptions.put("bytesRxTotal","The total number of RX (received) bytes");
        attributeDescriptions.put("rxBytes","The total of received bytes");
        attributeDescriptions.put("txBytes","The total of transmitted bytes");
        attributeDescriptions.put("bytesTxDelta","The delta number of TX (transmitted) bytes from the previous event");
        attributeDescriptions.put("bytesTxTotal","The total number of TX (transmitted) bytes");
        attributeDescriptions.put("end","The end");
        attributeDescriptions.put("port","The port");
        attributeDescriptions.put("start","The start");
        attributeDescriptions.put("addr","The address");
        attributeDescriptions.put("addresses","The addresses");
        attributeDescriptions.put("envelopeFromAddress","The envelop FROM address");
        attributeDescriptions.put("envelopeToAddress","The envelope TO address");
        attributeDescriptions.put("tmpFile","The /tmp file");
        attributeDescriptions.put("endTime","The end time/date");
        attributeDescriptions.put("localAddr","The local host address");
        attributeDescriptions.put("remoteAddr","The remote host address");
        attributeDescriptions.put("remoteAddress","The remote host address");
        attributeDescriptions.put("policyRuleId","The policy rule ID");
        attributeDescriptions.put("settingsFile","The settings file");
        attributeDescriptions.put("httpRequestEvent","The corresponding HTTP request event");
        attributeDescriptions.put("tagsString","The string value of all tags");
        attributeDescriptions.put("entity","The entity");
        attributeDescriptions.put("causalRule","The causal rule");
        attributeDescriptions.put("eventSent","True if the event was sent, false otherwise");
        attributeDescriptions.put("oldValue","The old value");
        attributeDescriptions.put("login","The login username");
        attributeDescriptions.put("succeeded","1 if successful, 0 otherwise");
        attributeDescriptions.put("rid","Rule ID");
        attributeDescriptions.put("loginType","W = Windows login, A=Active Directory, R=RADIUS, T=test");
        attributeDescriptions.put("categoryId","Numeric value of matching category");
        attributeDescriptions.put("tunnelDescription","Description of tunnel");
        attributeDescriptions.put("clientCategories","Client threat categories");
        attributeDescriptions.put("clientReputation","Client threat reputation");
        attributeDescriptions.put("serverCategories","Server threat categories");
        attributeDescriptions.put("serverReputation","Server threat reputation");
        attributeDescriptions.put("categories","Server threat categories");
        attributeDescriptions.put("reputation","Server threat reputation");

        HashMap<String,String> specificDescriptions;

        specificDescriptions = new HashMap<>();
        specificDescriptions.put("action","The action (1=Quota Given, 2=Quota Exceeded)");
        classSpecificAttributeDescriptions.put("QuotaEvent",specificDescriptions);

        specificDescriptions = new HashMap<>();
        specificDescriptions.put("method","The HTTP method");
        classSpecificAttributeDescriptions.put("HttpRequestEvent",specificDescriptions);

        specificDescriptions = new HashMap<>();
        specificDescriptions.put("name","The test name");
        classSpecificAttributeDescriptions.put("WanFailoverTestEvent",specificDescriptions);

        specificDescriptions = new HashMap<>();
        specificDescriptions.put("event","The event (LOGIN, FAILED, TIMEOUT, INACTIVE, USER_LOGOUT, ADMIN_LOGOUT)");
        specificDescriptions.put("eventValue","The event value as a string (LOGIN, FAILED, TIMEOUT, INACTIVE, USER_LOGOUT, ADMIN_LOGOUT)");
        classSpecificAttributeDescriptions.put("CaptivePortalUserEvent",specificDescriptions);

        specificDescriptions = new HashMap<>();
        specificDescriptions.put("kind","The type for this address (F=From, T=To, C=CC, G=Envelope From, B=Envelope To, X=Unknown)");
        specificDescriptions.put("personal","personal"); // FIXME
        classSpecificAttributeDescriptions.put("SmtpMessageAddressEvent",specificDescriptions);

        specificDescriptions = new HashMap<>();
        specificDescriptions.put("blocked","1 if blocked, 0 otherwise");
        classSpecificAttributeDescriptions.put("IntrusionPreventionLogEvent",specificDescriptions);

        specificDescriptions = new HashMap<>();
        specificDescriptions.put("local","1 if login is done via local console, 0 otherwise");
        classSpecificAttributeDescriptions.put("AdminLoginEvent",specificDescriptions);
    }

    /**
     * instance
     * @return
     */
    public static ExtensionImpl instance()
    {
        return new ExtensionImpl();
    }

    /**
     * run
     */
    public final void run()
    {
        String result = UvmContextFactory.context().execManager().execOutput("find " + System.getProperty("uvm.lib.dir") + " -name '*Event.class' | xargs grep -l 'logging.LogEvent' | sed -e 's|.*com/\\(.*\\)|com/\\1|' -e 's|/|.|g' -e 's/.class//'");

        try {
            File file = new File(CLASS_EVENTS_JSON_FILE_NAME);
            FileWriter = new FileWriter(file);
            String lines[] = result.split("\\n");
            FileWriter.write("{\n");
            for ( String line : lines ) {
                printClassDescription( line );
            }
            FileWriter.write("}\n");
            FileWriter.close();
        } catch (Exception e) {
            System.out.println(e + " " + e.getMessage());
        }
    }

    /**
     * printClassDescription
     * @param fullName
     */
    @SuppressWarnings("rawtypes")
    public void printClassDescription( String fullName )
    {
        String shortName = fullName.replaceAll(".*\\.","");

        String classDescription = classDescriptions.get(shortName);
        if (classDescription == null ) {
            System.out.println("Missing class description: " + shortName);
            throw new RuntimeException("Missing class description: " + shortName);
        }

        try{
            FileWriter.write("    " + shortName + ": {\n");
            FileWriter.write("    description: \"" + classDescription + "\",\n");
        }catch(Exception e){
            System.out.println("Unable to write class information to file:" + e);
        }

        Class clazz;
        try {
            clazz = Class.forName(fullName);
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + fullName);
            throw new RuntimeException("Class not found: " + fullName);
        }
        if ( clazz == null ) {
            System.out.println("Class not found: " + fullName);
            throw new RuntimeException("Class not found: " + fullName);
        }

        try{
            FileWriter.write("    fields: [\n");
            printFields( clazz, "" );
            FileWriter.write("    ]\n");
            FileWriter.write("    },\n");
        }catch(Exception e){
            System.out.println("Unable to write field informaton to file:" + e);
        }
    }

    /**
     * printFields
     * @param clazz
     * @param prefix
     */
    @SuppressWarnings("rawtypes")
    private void printFields( Class clazz, String prefix )
    {
        if ( clazz == null )
            return;

        String shortName = clazz.getName().replaceAll(".*\\.","");

        try {
            for(PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(clazz).getPropertyDescriptors()){
                Method method = propertyDescriptor.getReadMethod();

                if(method == null){
                    continue;
                }
                String methodName = method.getName();
                methodName = methodName.replaceAll("^get","");
                if (methodName.length() > 1) {
                    // if second char is upper case, leave first char
                    if (!Character.isUpperCase(methodName.charAt(1))) {
                        methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
                    }
                } else {
                    methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
                }

                Class returnTypeClazz = method.getReturnType();
                String returnType = returnTypeClazz.toString();
                // not sure why this does not work correctly
                // instead - use a ghetto hack to check the name
                //if ( returnTypeClazz.isInstance(LogEvent.class) ) {
                if ( returnType.contains("Event")
                     && !returnType.contains("$")
                     && !returnType.equals("class com.untangle.uvm.event.EventRule")
                     && !methodName.equals("cause")
                     ) {
                    printFields( returnTypeClazz, prefix + methodName + "." );
                    continue;
                }
                returnType = returnType.replaceAll(".*\\.","");

                String description = null;

                HashMap<String, String> specificDescriptions = classSpecificAttributeDescriptions.get(shortName);
                if ( specificDescriptions != null )
                    description = specificDescriptions.get( methodName );
                if ( description == null )
                    description = attributeDescriptions.get(methodName);
                if (description == null ) {
                    System.out.println("Missing attribute description: \"" + methodName + "\" \"" + shortName + "\"");
                    throw new RuntimeException("Missing attribute description: \"" + methodName + "\" \"" + shortName + "\"");
                }
                if ("".equals(description))
                    continue;
                try{
                    FileWriter.write("        {\n");
                    FileWriter.write("        name: \"" + prefix + methodName + "\",\n");
                    FileWriter.write("        type: \"" + returnType + "\",\n");
                    FileWriter.write("        description: \"" + description + "\",\n");
                    Object[] enums = method.getReturnType().getEnumConstants();
                    if ( enums != null ) {
                        FileWriter.write("        values: [\n");
                        for ( Object o: enums ) {
                            FileWriter.write(" \"" + o.toString() + "\", \n");
                        }
                        FileWriter.write("],\n");
                    }
                    FileWriter.write("        },\n");
                }catch(Exception e){
                    System.out.println("Unable to write field to file:" + e);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}
