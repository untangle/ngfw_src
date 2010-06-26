package com.untangle.uvm.servlet;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.Serializer;
import org.jabsorb.serializer.impl.JSONBeanSerializer;

import com.untangle.uvm.webui.jabsorb.serializer.EnumSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.ExtendedListSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.ExtendedSetSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.HostAddressSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.HostNameSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.IPMaddrSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.IPMatcherSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.IPaddrSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.InetAddressSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.IntfMatcherSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.LazyInitializerSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.MimeTypeSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.PortMatcherSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.ProtocolMatcherSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.RFC2253NameSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.TimeMatcherSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.TimeSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.TimeZoneSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.URLSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.UserMatcherSerializer;

@SuppressWarnings("unchecked")
public class ServletUtils 
{
    public static final ServletUtils INSTANCE = new ServletUtils();
    
    private ServletUtils()
    {
        
    }
    
    public void registerSerializers(JSONRPCBridge bridge ) throws Exception
    {
        registerSerializers(JSON_RPC_BRIDGE_REGISTRATOR, bridge);
    }
    
    public void registerSerializers(JSONSerializer serializer) throws Exception
    {
        serializer.registerDefaultSerializers();
        registerSerializers(JSON_SERIALIZER_REGISTRATOR, serializer);
    }
    
    public static ServletUtils getInstance()
    {
        return INSTANCE;
    }
    
    private <T> void registerSerializers(Registrator<T> registrator, T root) throws Exception
    {
        // general serializers
        registrator.registerSerializer(root, new JSONBeanSerializer());
        registrator.registerSerializer(root, new EnumSerializer());
        registrator.registerSerializer(root, new URLSerializer());
        registrator.registerSerializer(root, new InetAddressSerializer());
        registrator.registerSerializer(root, new TimeSerializer());
        
        // uvm related serializers
        registrator.registerSerializer(root, new IPMaddrSerializer());
        registrator.registerSerializer(root, new IPaddrSerializer());
        registrator.registerSerializer(root, new HostNameSerializer());
        registrator.registerSerializer(root, new HostAddressSerializer());
        registrator.registerSerializer(root, new TimeZoneSerializer());

        registrator.registerSerializer(root, new MimeTypeSerializer());
        registrator.registerSerializer(root, new RFC2253NameSerializer());
        
        // hibernate related serializers
        registrator.registerSerializer(root, new LazyInitializerSerializer());
        registrator.registerSerializer(root, new ExtendedListSerializer());
        registrator.registerSerializer(root, new ExtendedSetSerializer());

        // firewal related serializers
        registrator.registerSerializer(root, new ProtocolMatcherSerializer());
        registrator.registerSerializer(root, new IntfMatcherSerializer());
        registrator.registerSerializer(root, new IPMatcherSerializer());
        registrator.registerSerializer(root, new PortMatcherSerializer());
        registrator.registerSerializer(root, new TimeMatcherSerializer());
        registrator.registerSerializer(root, new UserMatcherSerializer());
    }
    
    private static interface Registrator<T>
    {
        void registerSerializer(T base, Serializer s) throws Exception;
    }
      

	private static Registrator JSON_SERIALIZER_REGISTRATOR = new Registrator<JSONSerializer>() {       
        public void registerSerializer(JSONSerializer serializer, Serializer s ) throws Exception {
            serializer.registerSerializer(s);
        }
    };

    private static Registrator JSON_RPC_BRIDGE_REGISTRATOR = new Registrator<JSONRPCBridge>() {        
        public void registerSerializer(JSONRPCBridge bridge, Serializer s ) throws Exception {
            bridge.registerSerializer(s);
        }
    };

}
