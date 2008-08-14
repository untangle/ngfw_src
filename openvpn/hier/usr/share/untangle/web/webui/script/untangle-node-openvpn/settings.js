if (!Ung.hasResource["Ung.OpenVPN"]) {
    Ung.hasResource["Ung.OpenVPN"] = true;
    Ung.Settings.registerClassName('untangle-node-openvpn', "Ung.OpenVPN");

    Ung.OpenVPN = Ext.extend(Ung.Settings, {
        configState: null,
        addressPoolStore:null,
        panelStatus: null,
    	panelClients: null,
    	gridVPNClients: null,
    	gridVPNSites: null,
    	gridExports: null,
    	panelAdvanced: null,
    	gridEventLog: null,
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.OpenVPN.superclass.onRender.call(this, container, position);
            this.getRpcNode().getConfigState(function(result, exception) {
                Ext.MessageBox.hide();
                if (exception) {
                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                    return;
                }
                this.configState=result;
                
                this.configState="SERVER_ROUTE" //!!!For test only
                
                this.buildStatus();
                var tabs=[this.panelStatus];
                if(this.configState=="SERVER_ROUTE") {
                    this.buildClients();
                    this.buildExports();
                    this.buildAdvanced();
                    this.buildEventLog();
                    tabs.push(this.panelClients);
                    tabs.push(this.gridExports);
                    tabs.push(this.panelAdvanced);
                    tabs.push(this.gridEventLog);
                } else if(this.configState=="CLIENT") {
                    this.buildEventLog();
                    tabs.push(this.gridEventLog);
                }
                this.buildTabPanel(tabs);
                
            }.createDelegate(this))
        },

        getVpnSettings : function(forceReload) {
            if (forceReload || this.rpc.vpnSettings === undefined) {
                this.rpc.vpnSettings = this.getRpcNode().getVpnSettings();
            }
            return this.rpc.vpnSettings;
        },
        
        getVpnServerAddress : function(forceReload) {
        	if (forceReload || this.rpc.vpnServerAddress === undefined) {
                this.rpc.vpnServerAddress = this.getRpcNode().getVpnServerAddress();
            }
            return this.rpc.vpnServerAddress;
        },
        
        formatHostAddress : function (hostAddress) {
        	return hostAddress==null?"":(hostAddress.hostName==null || hostAddress.hostName=="")?hostAddress.hostName :hostAddress.ip
        },
        getAddressPoolStore : function() {
        	if(this.addressPoolStore==null) {
        		this.addressPoolStore = new Ext.data.JsonStore({
                    fields : ['id', 'name'],
                    data : this.getVpnSettings().groupList.list
                });
        	}
        	return this.addressPoolStore;
        },
        
        // Block lists panel
        buildStatus : function() {
            var statusLabel="";
            var serverButtonDisabled=false;
            var clientButtonDisabled=false;
            if(this.configState=="UNCONFIGURED") {
                statusLabel=this.i18n._("Unconfigured: Use buttons below.");
            } else if(this.configState=="CLIENT") {
                statusLabel=this.i18n.sprintf(this.i18n._("VPN Client: Connected to %s"), this.formatHostAddress(this.getVpnServerAddress()));
                clientButtonDisabled=true;
            } else if(this.configState=="SERVER_ROUTE") {
                statusLabel=this.i18n._("VPN Server");
                serverButtonDisabled=true;
            } else {
            	clientButtonDisabled=true;
            	serverButtonDisabled=true;
            }
        	this.panelStatus = new Ext.Panel({
                name : 'Status',
                title : this.i18n._("Status"),
                parentId : this.getId(),

                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Current Mode'),
                    items : [{
                        xtype : "textfield",
                        name : "Status",
                        readOnly : true,
                        fieldLabel : this.i18n._("Status"),
                        width : 250,
                        value: statusLabel
                    }]
                }, {
                    title : this.i18n._('Wizard'),
                    layout:'table',
                    layoutConfig: {
                        columns: 2
                    },
                    items : [{
                        xtype: "button",
                        name : 'Configure as VPN Server',
                        text : this.i18n._("Configure as VPN Server"),
                        iconCls : "actionIcon",
                        disabled : serverButtonDisabled,
                        handler : function() {
                            this.configureVNPServer();
                        }.createDelegate(this)
                    },{
                        html: this.i18n._("This configures OpenVPN so remote users and networks can connect and access exported hosts and networks."),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        xtype: "button",
                        name : 'Configure as VPN Client',
                        text : this.i18n._("Configure as VPN Client"),
                        iconCls : "actionIcon",
                        disabled : clientButtonDisabled,
                        handler : function() {
                            this.configureVNPClient();
                        }.createDelegate(this)
                    },{
                        html: this.i18n._("This configures OpenVPN so it connects to a remote OpenVPN Server and can access exported hosts and networks."),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }]
                }]
            });
        },
        
        //overwrite default event manager
        getEventManager : function() {
            if (this.node.nodeContext.rpcNode.eventManager === undefined) {
                this.node.nodeContext.rpcNode.eventManager = this.node.nodeContext.rpcNode.getClientConnectEventManager();
            }
            return this.node.nodeContext.rpcNode.eventManager;
        },
        
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                fields : [{
                    name : 'start'
                }, {
                    name : 'end',
                    convert : function(val, rec) {
                        var date=end;
                        if(date==null) {
                        	date=new Date();
                        	date.setTime(0);
                        }
                        return date;
                    }
                }, {
                    name : 'clientName'
                }, {
                    name : 'address',
                    convert : function(val, rec) {
                        return val==null?"":val.addr+":"+rec.port;
                    }
                }, {
                    name : 'bytesTx',
                    convert : function(val, rec) {
                        return parseFloat(val) / 1024;
                    }
                }, {
                    name : 'bytesRx',
                    convert : function(val, rec) {
                        return parseFloat(val) / 1024;
                    }
                }],
                columns : [{
                    header : i18n._("start time"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'start',
                    renderer : function(value) {
                        return i18n.dateFormat(value);
                    }
                }, {
                    header : i18n._("end time"),
                    width : 70,
                    sortable : true,
                    dataIndex : 'actionType',
                    renderer : function(value) {
                        return i18n.dateFormat(value);
                    }
                }, {
                    header : i18n._("client name"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'clientName'
                }, {
                    header : i18n._("client address"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'address'
                }, {
                    header : i18n._("Kbytes<br>sent"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'bytesTx'
                }, {
                    header : i18n._("Kbytes<br>received"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'bytesRx'
                }]
                
            });
        },
        getDistributeColumn : function () {
            return new Ext.grid.ButtonColumn({
                width: 110,
                header: this.i18n._("distribute"), 
                dataIndex : null,
                handle : function(record) {
                    // populate usersWindow
                    alert("todo");
                },
                renderer : function(value, metadata, record) {
                    return '<div class="ungButton buttonColumn" style="text-align:center;">'+i18n._("Distribute Client")+'</div>';
                }
            });
        },
        buildVPNClients : function() {
            // live is a check column
            var liveColumn = new Ext.grid.CheckColumn({
                id: "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width: 80,
                fixed : true
            });
            var distributeColumn=this.getDistributeColumn();

            this.gridVPNClients = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'VPN Clients',
                // the total records is set from the base settings
                paginated: false,
                anchor :"100% 50%",
                height:250,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "group" : null,
                    "address" : "192.168.1.0"
                },
                title : this.i18n._("VPN Clients"),
                // the column is autoexpanded if the grid width permits
                //autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.VpnClient",
                data : this.getVpnSettings().exportedAddressList,
                // the list of fields
                fields : [{
                    name : 'id'
                }, {
                    name : 'live'
                }, {
                    name : 'name'
                }, {
                    name : 'group'
                }, {
                    name : 'address',
                    convert : function(val, rec) {
                        return val==null?"":this.formatHostAddress(val.addr);
                    }.createDelegate(this)
                }],
                // the list of columns for the column model
                columns : [liveColumn, {
                    id : 'name',
                    header : this.i18n._("client name"),
                    width : 200,
                    dataIndex : 'name',
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'group',
                    header : this.i18n._("address pool"),
                    width : 200,
                    dataIndex : 'group',
                    editor : new Ext.form.ComboBox({
                        store: this.getAddressPoolStore(),
                        displayField : 'name',
                        valueField : 'id',
                        editable: false,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small'
                   })
                }, distributeColumn,
                {
                    id : 'address',
                    header : this.i18n._("virtual address"),
                    width : 100,
                    dataIndex : 'address'
                }],
                //sortField : 'name',
                //columnsDefaultSortable : true,
                plugins : [liveColumn, distributeColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines : [{
                    xtype : 'checkbox',
                    name : "Enabled",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Enabled")
                }, {
                    xtype : "textfield",
                    name : "Client name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Client name"),
                    allowBlank : false,
                    width : 200
                }, {
                    xtype : "combo",
                    name : "Address pool",
                    fieldLabel : this.i18n._("Address pool"),
                    store: this.getAddressPoolStore(),
                    displayField : 'name',
                    valueField : 'id',
                    editable: false,
                    mode : 'local',
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small',
                    width : 200
                }]
            });
        },
        buildVPNSites : function() {
            // live is a check column
            var liveColumn = new Ext.grid.CheckColumn({
                id: "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width: 80,
                fixed : true
            });
            var distributeColumn=this.getDistributeColumn();

            this.gridVPNSites = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'VPN Sites',
                // the total records is set from the base settings
                paginated: false,
                anchor :"100% 50%",
                height:250,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "network" : "192.168.1.0",
                    "netmask" : "255.255.255.0"
                },
                title : this.i18n._("VPN Sites"),
                // the column is autoexpanded if the grid width permits
                //autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.VpnSite",
                data : this.getVpnSettings().exportedAddressList,
                // the list of fields
                fields : [{
                    name : 'id'
                }, {
                    name : 'live'
                }, {
                    name : 'name'
                }, {
                    name : 'network'
                }, {
                    name : 'netmask'
                }, {
                    name : 'group'
                }],
                // the list of columns for the column model
                columns : [liveColumn, {
                    id : 'name',
                    header : this.i18n._("site name"),
                    width : 200,
                    dataIndex : 'name',
                    // this is a simple text editor
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'group',
                    header : this.i18n._("address pool"),
                    width : 200,
                    dataIndex : 'group',
                    editor : new Ext.form.ComboBox({
                        store: this.getAddressPoolStore(),
                        displayField : 'name',
                        valueField : 'id',
                        editable: false,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small'
                   })
                }, {
                    id : 'network',
                    header : this.i18n._("network address"),
                    width : 100,
                    dataIndex : 'network',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, {
                    id : 'netmask',
                    header : this.i18n._("network mask"),
                    width : 100,
                    dataIndex : 'netmask',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, distributeColumn],
                //sortField : 'name',
                //columnsDefaultSortable : true,
                plugins : [liveColumn, distributeColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines : [{
                    xtype : 'checkbox',
                    name : "Enabled",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Enabled")
                }, {
                    xtype : "textfield",
                    name : "site name",
                    dataIndex : "network",
                    fieldLabel : this.i18n._("Site name"),
                    allowBlank : false,
                    width : 200
                }, {
                    xtype : "combo",
                    name : "Address pool",
                    fieldLabel : this.i18n._("Address pool"),
                    store: this.getAddressPoolStore(),
                    displayField : 'name',
                    valueField : 'id',
                    editable: false,
                    mode : 'local',
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small',
                    width : 200
                },  {
                    xtype : "textfield",
                    name : "Network address",
                    dataIndex : "network",
                    fieldLabel : this.i18n._("Network address"),
                    allowBlank : false,
                    width : 200,
                    vtype : 'ipAddress'
                }, {
                    xtype : "textfield",
                    name : "Network mask",
                    dataIndex : "netmask",
                    fieldLabel : this.i18n._("Network mask"),
                    allowBlank : false,
                    width : 200,
                    vtype : 'ipAddress'
                }]
            });
        },
        buildClients : function() {
            this.buildVPNClients();
            this.buildVPNSites();
            this.panelClients = new Ext.Panel({
                // private fields
                name : 'Clients',
                parentId : this.getId(),
                title : this.i18n._('Clients'),
                layout : "form",
                autoScroll : true,
                items : [this.gridVPNClients,this.gridVPNSites]
            });
        },        
        buildExports : function() {
            // live is a check column
            var liveColumn = new Ext.grid.CheckColumn({
            	id: "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width: 80,
                fixed : true
            });

            this.gridExports = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Exported Hosts and Networks',
                // the total records is set from the base settings
                paginated: false,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "network" : "192.168.1.0",
                    "netmask" : "255.255.255.0"
                },
                title : this.i18n._("Exported Hosts and Networks"),
                // the column is autoexpanded if the grid width permits
                //autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.ServerSiteNetwork",
                data : this.getVpnSettings().exportedAddressList,
                // the list of fields
                fields : [{
                    name : 'id'
                }, {
                    name : 'live'
                }, {
                    name : 'name'
                }, {
                    name : 'network'
                }, {
                    name : 'netmask'
                }],
                // the list of columns for the column model
                columns : [liveColumn, {
                    id : 'name',
                    header : this.i18n._("host/network name"),
                    width : 200,
                    dataIndex : 'name',
                    // this is a simple text editor
                    editor : new Ext.form.TextField({
                    })
                }, {
                    id : 'network',
                    header : this.i18n._("IP address"),
                    width : 200,
                    dataIndex : 'network',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, {
                    id : 'netmask',
                    header : this.i18n._("netmask"),
                    width : 200,
                    dataIndex : 'netmask',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }],
                //sortField : 'name',
                //columnsDefaultSortable : true,
                plugins : [liveColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines : [{
                    xtype : 'checkbox',
                    name : "Enabled",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Enabled")
                }, {
                    xtype : "textfield",
                    name : "Host/network name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Host/network name"),
                    width : 200
                }, {
                	xtype : "textfield",
                    name : "IP address",
                    dataIndex : "network",
                    allowBlank : false,
                    fieldLabel : this.i18n._("IP address"),
                    vtype : 'ipAddress',
                    width : 200
                }, {
                	xtype : "textfield",
                    name : "Netmask",
                    dataIndex : "netmask",
                    fieldLabel : this.i18n._("Netmask"),
                    allowBlank : false,
                    vtype : 'ipAddress',
                    width : 200
                }]
            });
        },
        buildAddressPools : function() {
            // live is a check column
            var liveColumn = new Ext.grid.CheckColumn({
                id: "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width: 80,
                fixed : true
            });
            var exportDNSColumn = new Ext.grid.CheckColumn({
                id: "useDNS",
                header : this.i18n._("export DNS"),
                dataIndex : 'useDNS',
                width: 90,
                fixed : true
            });

            this.gridAddressPools = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Address Pools',
                // the total records is set from the base settings
                paginated: false,
                height: 300,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "group" : null,
                    "address" : "192.168.1.0"
                },
                title : this.i18n._("Address Pools"),
                // the column is autoexpanded if the grid width permits
                //autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.VpnGroup",
                data : this.getVpnSettings().groupList,
                // the list of fields
                fields : [{
                    name : 'id'
                }, {
                    name : 'live'
                }, {
                    name : 'name'
                }, {
                    name : 'address',
                    convert : function(val, rec) {
                        return val==null?"":this.formatHostAddress(val.addr);
                    }.createDelegate(this)
                }, {
                    name : 'netmask',
                    convert : function(val, rec) {
                        return val==null?"":this.formatHostAddress(val.addr);
                    }.createDelegate(this)
                }],
                // the list of columns for the column model
                columns : [liveColumn, {
                    id : 'name',
                    header : this.i18n._("pool name"),
                    width : 200,
                    dataIndex : 'name',
                    // this is a simple text editor
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'address',
                    header : this.i18n._("IP address"),
                    width : 200,
                    dataIndex : 'address',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, {
                    id : 'netmask',
                    header : this.i18n._("netmask"),
                    width : 200,
                    dataIndex : 'netmask',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                },
                exportDNSColumn],
                //sortField : 'name',
                //columnsDefaultSortable : true,
                plugins : [liveColumn, exportDNSColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines : [{
                    xtype : 'checkbox',
                    name : "Enabled",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Enabled")
                }, {
                    xtype : "textfield",
                    name : "Pool name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Pool name"),
                    allowBlank : false,
                    width : 200
                }, {
                    xtype : "textfield",
                    name : "IP address",
                    dataIndex : "address",
                    fieldLabel : this.i18n._("IP address"),
                    allowBlank : false,
                    vtype : 'ipAddress',
                    width : 200
                }, {
                    xtype : "textfield",
                    name : "Netmask",
                    dataIndex : 'netmask',
                    fieldLabel : this.i18n._("Netmask"),
                    allowBlank : false,
                    vtype : 'ipAddress',
                    width : 200
                }, {
                    xtype : 'checkbox',
                    name : "export DNS",
                    dataIndex : "useDNS",
                    fieldLabel : this.i18n._("export DNS")
                }]
            });
        },
        buildAdvanced : function() {
        	this.buildAddressPools();
            this.panelAdvanced = new Ext.Panel({
                name : 'Advanced',
                title : this.i18n._("Advanced"),
                parentId : this.getId(),

                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    xtype : 'fieldset',
                    autoHeight : true,
                    items:[this.gridAddressPools]
                },{
                    xtype : 'fieldset',
                    autoHeight : true,
                    defaults : {
                        labelStyle: "width:160px;"
                    },                    
                    items:[{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Server Port (UDP)'),
                        width : 80,
                        name : 'Server Port (UDP)',
                        value : this.getVpnSettings().publicPort,
                        allowBlank : false,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getVpnSettings().publicPort = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Site Name'),
                        name : 'Site Name',
                        value : this.getVpnSettings().siteName,
                        allowBlank : false,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getVpnSettings().siteName = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'radiogroup',
                        fieldLabel : this.i18n._('DNS Override'),
                        columns: 1,
                        name : 'Site Name',
                        items: [{
                            boxLabel: this.i18n._('Enabled'),
                            name: 'DNSOverride', 
                            inputValue: true, 
                            checked: this.getVpnSettings().isDnsOverrideEnabled,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getVpnSettings().isDnsOverrideEnabled = checked;
                                        if(checked) {
                                            Ext.getCmp("openvpn_advanced_dns1").enable();
                                            Ext.getCmp("openvpn_advanced_dns2").enable();
                                        	
                                        } else {
                                        	Ext.getCmp("openvpn_advanced_dns1").disable();
                                        	Ext.getCmp("openvpn_advanced_dns2").disable();
                                        }
                                    }.createDelegate(this)
                                }
                            }
                        }, {
                            boxLabel: this.i18n._('Disabled'), 
                            name: 'DNSOverride',
                            inputValue: false, 
                            checked: !this.getVpnSettings().isDnsOverrideEnabled,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getVpnSettings().isDnsOverrideEnabled = !checked;
                                        if(!checked) {
                                            Ext.getCmp("openvpn_advanced_dns1").enable();
                                            Ext.getCmp("openvpn_advanced_dns2").enable();
                                            
                                        } else {
                                            Ext.getCmp("openvpn_advanced_dns1").disable();
                                            Ext.getCmp("openvpn_advanced_dns2").disable();
                                        }
                                    }.createDelegate(this)
                                }
                            }
                            
                        }]
                    },{
                        xtype : 'fieldset',
                        autoHeight : true,
                        style : 'margin:0px 0px 0px 160px;',
                        defaults : {
                            labelStyle: "width:160px;"
                        },
                        border: false,
                        items:[{
                            xtype : 'textfield',
                            fieldLabel : this.i18n._('Primary IP'),
                            name : 'outsideNetwork',
                            id : 'openvpn_advanced_dns1',
                            value : this.getVpnSettings().dns1,
                            disabled: !this.getVpnSettings().isDnsOverrideEnabled,
                            allowBlank : false,
                            blankText : this.i18n._('A Primary IP Address must be specified.'),
                            vtype : 'ipAddress',
                            listeners : {
                                "change" : {
                                    fn : function(elem, newValue) {
                                        this.getVpnSettings().dns1 = newValue;
                                    }.createDelegate(this)
                                }
                            }
                        }, {
                            xtype : 'textfield',
                            fieldLabel : this.i18n._('Secondary IP (optional)'),
                            name : 'outsideNetwork',
                            id : 'openvpn_advanced_dns2',
                            disabled: !this.getVpnSettings().isDnsOverrideEnabled,
                            value : this.getVpnSettings().dns2,
                            vtype : 'ipAddress',
                            listeners : {
                                "change" : {
                                    fn : function(elem, newValue) {
                                        this.getVpnSettings().dns2 = newValue;
                                    }.createDelegate(this)
                                }
                            }
                        }]
                    }]
                }]
            });
        },

        // save function
        save : function() {
            // validate first
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.getRpcNode().setVpnSettings(function(result, exception) {
                    Ext.MessageBox.hide();
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    // exit settings screen
                    this.cancelAction();
                }.createDelegate(this), this.getVpnSettings());
            }
        },
        
        configureVNPClient :function () {
            if(this.clientSetup) {
                Ext.destroy(this.clientSetup)
            }
            var welcomeCard={
                title : this.i18n._("Welcome"),
                cardTitle : this.i18n._( "Welcome to the OpenVPN Setup Wizard!" ),
                panel : {
                	xtype: 'form',
                    items : [{
                        html : this.i18n._( 'This wizard will help guide you through your initial setup and configuration of OpenVPN as a VPN Client.' ),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        html : this.i18n._('Warning:  Finishing this wizard will cause any previous OpenVPN settings you had to be lost, and overwritten by new settings.  Only finish this wizard if you would like completely new settings.' ),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }]
                }
            };
            var downloadCard={
                title : this.i18n._( "Download Configuration" ),
                cardTitle : this.i18n._( "Welcome to the OpenVPN Setup Wizard!" ),
                panel : {
                    xtype: 'form',
                    items : [{
                        html : this.i18n._('Please specify where your VPN Client configuration should come from.  You may specify a Server or USB Key.  If you choose USB Key, you must press "Read USB Key" to load configurations from the key, and then choose a configuration from the drop-down-list.' ),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    },{
                        xtype : 'fieldset',
                        autoHeight : true,
                        buttonAlign : 'left',
                        items:[{
                        	xtype: "radio",
                            boxLabel: this.i18n._('Download from Server'),
                            hideLabel: true,
                            name: 'downloadClientConfiguration', 
                            inputValue: true, 
                            checked: true,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        if(checked) {
                                            Ext.getCmp("openvpn_client_wizard_server_ip").enable();
                                            Ext.getCmp("openvpn_client_wizard_password").enable();
                                            Ext.getCmp("openvpn_client_read_usb").disable();
                                            Ext.getCmp("openvpn_client_configurations").disable();
                                        } else {
                                            Ext.getCmp("openvpn_client_wizard_server_ip").disable();
                                            Ext.getCmp("openvpn_client_wizard_password").disable();
                                            Ext.getCmp("openvpn_client_read_usb").enable();
                                            Ext.getCmp("openvpn_client_configurations").enable();
                                        }
                                    }.createDelegate(this)
                                }
                            }
                        }, {
                            xtype : "textfield",
                            id:'openvpn_client_wizard_server_ip',
                            name : "Server IP Address",
                            labelStyle : 'text-align: right;  width: 150px;',
                            fieldLabel : this.i18n._("Server IP Address"),
                            allowBlank : false,
                            width : 200,
                            vtype : 'ipAddress'
                        }, {
                            xtype : "textfield",
                            id:'openvpn_client_wizard_password',
                            name : "Password",
                            labelStyle : 'text-align: right; width: 150px;',
                            fieldLabel : this.i18n._("Password"),
                            width : 200,
                            inputType: 'password'
                        },{
                            xtype: "radio",
                            boxLabel: this.i18n._('Download from USB Key'),
                            style: "margin-left:20px;",
                            hideLabel: true,
                            name: 'downloadClientConfiguration', 
                            inputValue: true, 
                            checked: false
                            
                        }, {
                        	xtype: "panel",
                        	border : false,
                        	width: 500,
                            layout:'column',
                            items: [{
                            	width: 200,
                            	border : false,
                                items: [{
                                    xtype : 'button',
                                    id:'openvpn_client_read_usb',
                                    name : 'Read USB Key',
                                    style: "margin-left:20px;",
                                    text : this.i18n._('Read USB Key'),
                                    style : 'padding-bottom:10px;',
                                    disabled: true,
                                    handler : function() {
                                        //
                                    }.createDelegate(this)
                                }]
                            }, {
                                columnWidth: 1,
                                border : false,
                                items: [{
                                    xtype : 'combo',
                                    name : 'Configurations',
                                    id : 'openvpn_client_configurations',
                                    editable : false,
                                    hideLabel : true,
                                    mode : 'local',
                                    triggerAction : 'all',
                                    listClass : 'x-combo-list-small',
                                    store : new Ext.data.SimpleStore({
                                        fields : ['key', 'name'],
                                        data :[
                                            ["", this.i18n._("[No Configurations]")],
                                        ]        
                                    }),
                                    displayField : 'name',
                                    valueField : 'key',
                                    value : "",
                                    disabled: true
                                }]
                            }]
                        }]
                    }]
                }
            }
            var congratulationsCard= {
                title : this.i18n._( "Congratulations" ),
                cardTitle : this.i18n._( "Congratulations! OpenVPN is configured as a VPN Client." ),
                panel : {
                    xtype: 'form',
                    items : [{
                        html : this.i18n._('If necessary, you can change the configuration of OpenVPN by launching the Setup Wizard again.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }]
                }
            };
            var clientWizard=new Ung.Wizard({
                height : 500,
                width : 800,
                cardDefaults : {
                    labelWidth : 150,
                    cls : 'untangle-form-panel'
                },
                cards : [welcomeCard, downloadCard,congratulationsCard],
                disableNext : true
                
            });
            
            this.clientSetup=new Ung.Window({
               title: this.i18n._("OpenVPN Client Setup Wizard"),
               items:[{
                    region : "center",
                    items: [clientWizard],
                    border : false,
                    autoScroll : true,
                    cls : 'windowBackground',
                    bodyStyle : 'background-color: transparent;'
               }]
            });
            
            this.clientSetup.show();
            clientWizard.goToPage( 0 );
        },
        configureVNPServer :function () {
            if(this.serverSetup) {
                Ext.destroy(this.serverSetup)
            }
            this.serverSetup=new Ung.Window({
               title: this.i18n._("OpenVPN Server Setup Wizard"),
               items:[{
                    region : "center",
                    html : 'aaa',
                    border : false,
                    autoScroll : true,
                    cls : 'windowBackground',
                    bodyStyle : 'background-color: transparent;'
               }]
            })
            this.serverWizard.show();
        }
    });
}
