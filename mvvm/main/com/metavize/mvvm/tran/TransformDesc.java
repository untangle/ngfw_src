/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TransformDesc.java,v 1.12 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.mvvm.tran;

import java.awt.Color;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import com.metavize.mvvm.security.Tid;

/**
 * Transform settings and properties.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TRANSFORM_DESC"
 */
public class TransformDesc implements Serializable
{
    private static final long serialVersionUID = 5392845095643317874L;

    private Long id;
    private Tid tid;
    private String name;
    private String className;
    private byte[] publicKey;

    private String parentTransform;
    private boolean singleInstance;
    private TransformState targetState;
    private List args;

    private boolean readOnly = false;
    private int tcpClientReadBufferSize = 8192;
    private int tcpServerReadBufferSize = 8192;
    private int udpMaxPacketSize = 16384;

    private String displayName;
    private String guiClassName;
    private Color guiBackgroundColor = Color.PINK; /* Your favorite color */
    private String notes;

    public TransformDesc() { }

    public TransformDesc(Tid tid, byte[] publicKey, String name,
                         String className, String[] args, String displayName,
                         String guiClassName)
    {
        this.tid = tid;
        this.publicKey = publicKey;
        this.name = name;
        this.className = className;
        this.args = Arrays.asList(args);
        this.displayName = displayName;
        this.guiClassName = guiClassName;
    }

    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Transform id.
     *
     * @return tid for this instance.
     * @hibernate.many-to-one
     * cascade="none"
     * column="TID"
     */
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Internal name of the transform.
     *
     * @return the transform's name.
     * @hibernate.property
     * column="NAME"
     * not-null="true"
     * length="64"
     */
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Name of the main transform Class.
     *
     * @return transform class name.
     * @hibernate.property
     * column="CLASS_NAME"
     * not-null="true"
     * length="80"
     */
    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    /**
     * Not really used.
     * XXX move into Tid, or associate with TID?
     * XXX for now length of 16 because not really used anyway
     *
     * @return public key
     * @hibernate.property
     * type="binary"
     * column="PUBLIC_KEY"
     * length="16"
     * not-null="true"
     */
    public byte[] getPublicKey()
    {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey)
    {
        this.publicKey = publicKey;
    }

    /**
     * The parent transform, usually a casing.
     *
     * @return the parent transform, null if transform has no parent.
     * @hibernate.property
     * column="PARENT_TRANSFORM"
     * length="64"
     */
    public String getParentTransform()
    {
        return parentTransform;
    }

    public void setParentTransform(String parentTransform)
    {
        this.parentTransform = parentTransform;
    }

    /**
     * Only a single instance may be initialized in the system.
     *
     * @return a <code>boolean</code> value
     * @hibernate.property
     * column="SINGLE_INSTANCE"
     */
    public boolean isSingleInstance()
    {
        return singleInstance;
    }

    public void setSingleInstance(boolean singleInstance)
    {
        this.singleInstance = singleInstance;
    }

    /**
     * The desired state upon initial load. When the MVVM starts, it
     * attempts to place the transform in this state. Subsequent
     * changes in state at runtime become the new target state, such
     * that if the MVVM is restarted, the transform resumes its last
     * state.
     *
     * @return the target state.
     * @hibernate.property
     * type="com.metavize.mvvm.type.TransformStateUserType"
     * column="TARGET_STATE"
     * not-null="true"
     */
    public TransformState getTargetState()
    {
        return targetState;
    }

    public void setTargetState(TransformState targetState)
    {
        this.targetState = targetState;
    }

    /**
     * Transform string arguments, used by some transforms rather than
     * database settings.
     *
     * @return transform arguments.
     * @hibernate.list
     * table="TRANSFORM_ARGS"
     * @hibernate.collection-key
     * column="TRANSFORM_DESC_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-element
     * type="string"
     * column="ARG"
     * not-null="true"
     */
    public List getArgs()
    {
        return args;
    }

    public void setArgs(List args)
    {
        this.args = args;
    }

    public void setArgs(String[] args)
    {
        this.args = Arrays.asList(args);
    }

    public String[] getArgArray()
    {
        return (String[])args.toArray(new String[args.size()]);
    }

    /**
     * Readonly for transforms that do not modify data.
     *
     * @return true if transform is read only.
     * @hibernate.property
     * column="READ_ONLY"
     * not-null="true"
     */
    public boolean isReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    /**
     * TCP client read BufferSize is between 1 and 65536 bytes.
     *
     * @return the TCP client read bufferSize.
     * @hibernate.property
     * column="TCP_CLIENT_READ_BUFFER_SIZE"
     * not-null="true"
     */
    public int getTcpClientReadBufferSize()
    {
        return tcpClientReadBufferSize;
    }

    public void setTcpClientReadBufferSize(int tcpClientReadBufferSize)
    {
        if (0 > tcpClientReadBufferSize || 65536 < tcpClientReadBufferSize) {
            throw new IllegalArgumentException("out of range: "
                                               + tcpClientReadBufferSize);
        }

        this.tcpClientReadBufferSize = tcpClientReadBufferSize;
    }

    /**
     * TCP server read bufferSize is between 1 and 65536 bytes.
     *
     * @return the TCP server read bufferSize.
     * @hibernate.property
     * column="TCP_SERVER_READ_BUFFER_SIZE"
     * not-null="true"
     */
    public int getTcpServerReadBufferSize()
    {
        return tcpServerReadBufferSize;
    }

    public void setTcpServerReadBufferSize(int tcpServerReadBufferSize)
    {
        this.tcpServerReadBufferSize = tcpServerReadBufferSize;
    }

    /**
     * UDP max packet size, between 1 and 65536 bytes, defaults to 16384.
     *
     * @return UDP max packet size.
     * @hibernate.property
     * column="UDP_MAX_PACKET_SIZE"
     * not-null="true"
     */
    public int getUdpMaxPacketSize()
    {
        return udpMaxPacketSize;
    }

    public void setUdpMaxPacketSize(int udpMaxPacketSize)
    {
        if (1 > udpMaxPacketSize || 65536 < udpMaxPacketSize) {
            throw new IllegalArgumentException("out of range: "
                                               + udpMaxPacketSize);
        }

        this.udpMaxPacketSize = udpMaxPacketSize;
    }

    /**
     * The name of the transform, for display purposes.
     *
     * @return display name.
     * @hibernate.property
     * column="DISPLAY_NAME"
     * not-null="true"
     */
    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * The class name of the GUI module.
     *
     * @return class name of GUI component.
     * @hibernate.property
     * column="GUI_CLASS_NAME"
     * not-null="true"
     */
    public String getGuiClassName()
    {
        return guiClassName;
    }

    public void setGuiClassName(String guiClassName)
    {
        this.guiClassName = guiClassName;
    }

    /**
     * Background color for the GUI panel.
     *
     * @return background color.
     * @hibernate.property
     * type="com.metavize.mvvm.type.ColorUserType"
     * @hibernate.column
     * name="RED"
     * @hibernate.column
     * name="GREEN"
     * @hibernate.column
     * name="BLUE"
     * @hibernate.column
     * name="ALPHA"
     */
    public Color getGuiBackgroundColor()
    {
        return guiBackgroundColor;
    }

    public void setGuiBackgroundColor(Color guiBackgroundColor)
    {
        this.guiBackgroundColor = guiBackgroundColor;
    }

    // Object methods ---------------------------------------------------------

    /**
     * Equality based on the business key (tid).
     *
     * @param o the object to compare to.
     * @return true if equal.
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof TransformDesc)) {
            return false;
        }

        TransformDesc td = (TransformDesc)o;

        return tid.equals(td.getTid());
    }

    public int hashCode()
    {
        return tid.hashCode();
    }
}
