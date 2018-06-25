/**
 * $Id$
 */
package com.untangle.uvm.servlet;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.Serializer;

import com.untangle.uvm.admin.jabsorb.serializer.EnumSerializer;
import com.untangle.uvm.admin.jabsorb.serializer.IPMaskedAddressSerializer;
import com.untangle.uvm.admin.jabsorb.serializer.InetAddressSerializer;
import com.untangle.uvm.admin.jabsorb.serializer.MimeTypeSerializer;
import com.untangle.uvm.admin.jabsorb.serializer.TimeSerializer;
import com.untangle.uvm.admin.jabsorb.serializer.TimeZoneSerializer;
import com.untangle.uvm.admin.jabsorb.serializer.URLSerializer;
import com.untangle.uvm.admin.jabsorb.serializer.GenericStringSerializer;
import com.untangle.uvm.app.ProtocolMatcher;
import com.untangle.uvm.app.IPMatcher;
import com.untangle.uvm.app.IntMatcher;
import com.untangle.uvm.app.IntfMatcher;
import com.untangle.uvm.app.DayOfWeekMatcher;
import com.untangle.uvm.app.TimeOfDayMatcher;
import com.untangle.uvm.app.UserMatcher;
import com.untangle.uvm.app.GlobMatcher;
import com.untangle.uvm.app.UrlMatcher;

/**
 * ServletUtils provides utility methods for servlets
 */
@SuppressWarnings("unchecked")
/** ServletUtils */
public class ServletUtils 
{
    public static final ServletUtils INSTANCE = new ServletUtils();
    
    /**
     * private constructor use getInstance()
     */
    private ServletUtils()
    {
        
    }
    
    /**
     * registerSerializers - register standard serializers
     * @param bridge
     * @throws Exception
     */
    public void registerSerializers(JSONRPCBridge bridge) throws Exception
    {
        registerSerializers(JSON_RPC_BRIDGE_REGISTRATOR, bridge);
    }
    
    /**
     * registerSerializers - register default serializers
     * @param serializer
     * @throws Exception
     */
    public void registerSerializers(JSONSerializer serializer) throws Exception
    {
        serializer.registerDefaultSerializers();
        registerSerializers(JSON_SERIALIZER_REGISTRATOR, serializer);
    }
    
    /**
     * getInstance
     * @return ServletUtils
     */
    public static ServletUtils getInstance()
    {
        return INSTANCE;
    }
    
    /**
     * registerSerializers registers common serializers
     * @param registrator
     * @param root
     * @throws Exception
     */
    private <T> void registerSerializers(Registrator<T> registrator, T root) throws Exception
    {
        // general serializers
        registrator.registerSerializer(root, new EnumSerializer());
        registrator.registerSerializer(root, new URLSerializer());
        registrator.registerSerializer(root, new InetAddressSerializer());
        registrator.registerSerializer(root, new TimeSerializer());
        
        // uvm related serializers
        registrator.registerSerializer(root, new IPMaskedAddressSerializer());
        registrator.registerSerializer(root, new TimeZoneSerializer());

        registrator.registerSerializer(root, new MimeTypeSerializer());
        
        // matchers
        registrator.registerSerializer(root, new GenericStringSerializer(ProtocolMatcher.class));
        registrator.registerSerializer(root, new GenericStringSerializer(IPMatcher.class));
        registrator.registerSerializer(root, new GenericStringSerializer(IntMatcher.class));
        registrator.registerSerializer(root, new GenericStringSerializer(IntfMatcher.class));
        registrator.registerSerializer(root, new GenericStringSerializer(DayOfWeekMatcher.class));
        registrator.registerSerializer(root, new GenericStringSerializer(TimeOfDayMatcher.class));
        registrator.registerSerializer(root, new GenericStringSerializer(UserMatcher.class));
        registrator.registerSerializer(root, new GenericStringSerializer(GlobMatcher.class));
        registrator.registerSerializer(root, new GenericStringSerializer(UrlMatcher.class));
    }
    
    private static interface Registrator<T>
    {
        void registerSerializer(T base, Serializer s) throws Exception;
    }
      

    private static Registrator<JSONSerializer> JSON_SERIALIZER_REGISTRATOR = new Registrator<JSONSerializer>() {       
        public void registerSerializer(JSONSerializer serializer, Serializer s ) throws Exception {
            serializer.registerSerializer(s);
        }
    };

    private static Registrator<JSONRPCBridge> JSON_RPC_BRIDGE_REGISTRATOR = new Registrator<JSONRPCBridge>() {        
        public void registerSerializer(JSONRPCBridge bridge, Serializer s ) throws Exception {
            bridge.registerSerializer(s);
        }
    };

}
