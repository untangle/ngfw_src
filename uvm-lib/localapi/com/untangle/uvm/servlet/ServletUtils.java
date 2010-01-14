package com.untangle.uvm.servlet;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONSerializer;
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


public class ServletUtils {
    public static final ServletUtils INSTANCE = new ServletUtils();
    
    private ServletUtils()
    {
        
    }
    
    public void registerSerializers(JSONRPCBridge bridge ) throws Exception
    {
        // general serializers
        bridge.registerSerializer(new JSONBeanSerializer());
        bridge.registerSerializer(new EnumSerializer());
        bridge.registerSerializer(new URLSerializer());
        bridge.registerSerializer(new InetAddressSerializer());
        bridge.registerSerializer(new TimeSerializer());
        
        // uvm related serializers
        bridge.registerSerializer(new IPMaddrSerializer());
        bridge.registerSerializer(new IPaddrSerializer());
        bridge.registerSerializer(new HostNameSerializer());
        bridge.registerSerializer(new HostAddressSerializer());
        bridge.registerSerializer(new TimeZoneSerializer());

        bridge.registerSerializer(new MimeTypeSerializer());
        bridge.registerSerializer(new RFC2253NameSerializer());
        
        // hibernate related serializers
        bridge.registerSerializer(new LazyInitializerSerializer());
        bridge.registerSerializer(new ExtendedListSerializer());
        bridge.registerSerializer(new ExtendedSetSerializer());

        // firewal related serializers
        bridge.registerSerializer(new ProtocolMatcherSerializer());
        bridge.registerSerializer(new IntfMatcherSerializer());
        bridge.registerSerializer(new IPMatcherSerializer());
        bridge.registerSerializer(new PortMatcherSerializer());
        bridge.registerSerializer(new TimeMatcherSerializer());
        bridge.registerSerializer(new UserMatcherSerializer());
    }
    
    public void registerSerializers(JSONSerializer serializer) throws Exception
    {
        serializer.registerDefaultSerializers();
        // general serializers
        serializer.registerSerializer(new JSONBeanSerializer());
        serializer.registerSerializer(new EnumSerializer());
        serializer.registerSerializer(new URLSerializer());
        serializer.registerSerializer(new InetAddressSerializer());
        serializer.registerSerializer(new TimeSerializer());
        
        // uvm related serializers
        serializer.registerSerializer(new IPMaddrSerializer());
        serializer.registerSerializer(new IPaddrSerializer());
        serializer.registerSerializer(new HostNameSerializer());
        serializer.registerSerializer(new HostAddressSerializer());
        serializer.registerSerializer(new TimeZoneSerializer());

        serializer.registerSerializer(new MimeTypeSerializer());
        serializer.registerSerializer(new RFC2253NameSerializer());
        
        // hibernate related serializers
        serializer.registerSerializer(new LazyInitializerSerializer());
        serializer.registerSerializer(new ExtendedListSerializer());
        serializer.registerSerializer(new ExtendedSetSerializer());

        // firewal related serializers
        serializer.registerSerializer(new ProtocolMatcherSerializer());
        serializer.registerSerializer(new IntfMatcherSerializer());
        serializer.registerSerializer(new IPMatcherSerializer());
        serializer.registerSerializer(new PortMatcherSerializer());
        serializer.registerSerializer(new TimeMatcherSerializer());
        serializer.registerSerializer(new UserMatcherSerializer());
    }
    
    public static ServletUtils getInstance()
    {
        return INSTANCE;
    }
}
