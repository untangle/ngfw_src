if (!Ung.hasResource["Ung.OpenVPN"]) {
    Ung.hasResource["Ung.OpenVPN"] = true;
    Ung.Settings.registerClassName('untangle-node-openvpn', "Ung.OpenVPN");

    Ung.OpenVPN = Ext.extend(Ung.Settings, {
        configState : null,
        groupsStore : null,
        panelStatus : null,
        panelClients : null,
        gridClients : null,
        gridSites : null,
        gridExports : null,
        gridGroups : null,
        panelAdvanced : null,
        gridEventLog : null,
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
                this.configState = result;

                this.buildStatus();
                var tabs = [this.panelStatus];
                if (this.configState == "SERVER_ROUTE") {
                    this.buildClients();
                    this.buildExports();
                    this.buildAdvanced();
                    this.buildEventLog();
                    tabs.push(this.panelClients);
                    tabs.push(this.gridExports);
                    tabs.push(this.panelAdvanced);
                    tabs.push(this.gridEventLog);
                } else if (this.configState == "CLIENT") {
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
        getOpenVpnValidator : function(forceReload) {
            if (forceReload || this.rpc.openVpnValidator === undefined) {
                this.rpc.openVpnValidator = this.getRpcNode().getValidator();
            }
            return this.rpc.openVpnValidator;
        },

        getGroupsStore : function() {
            if (this.groupsStore == null) {
                this.groupsStore = new Ext.data.JsonStore({
                    fields : ['id', 'name','javaClass'],
                    data : this.getVpnSettings().groupList.list
                });
            }
            return this.groupsStore;
        },

        // Block lists panel
        buildStatus : function() {
            var statusLabel = "";
            var serverButtonDisabled = false;
            var clientButtonDisabled = false;
            if (this.configState == "UNCONFIGURED") {
                statusLabel = this.i18n._("Unconfigured: Use buttons below.");
            } else if (this.configState == "CLIENT") {
                statusLabel = this.i18n.sprintf(this.i18n._("VPN Client: Connected to %s"), this.getVpnServerAddress());
                clientButtonDisabled = true;
            } else if (this.configState == "SERVER_ROUTE") {
                statusLabel = this.i18n._("VPN Server");
                serverButtonDisabled=true;
            } else {
                clientButtonDisabled = true;
                serverButtonDisabled = true;
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
                        value : statusLabel
                    }]
                }, {
                    title : this.i18n._('Wizard'),
                    layout : 'table',
                    layoutConfig : {
                        columns : 2
                    },
                    items : [{
                        xtype : "button",
                        name : 'Configure as VPN Server',
                        text : this.i18n._("Configure as VPN Server"),
                        iconCls : "actionIcon",
                        disabled : serverButtonDisabled,
                        handler : function() {
                            this.configureVNPServer();
                        }.createDelegate(this)
                    }, {
                        html : this.i18n
                                ._("This configures OpenVPN so remote users and networks can connect and access exported hosts and networks."),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        xtype : "button",
                        name : 'Configure as VPN Client',
                        text : this.i18n._("Configure as VPN Client"),
                        iconCls : "actionIcon",
                        disabled : clientButtonDisabled,
                        handler : function() {
                            this.configureVNPClient();
                        }.createDelegate(this)
                    }, {
                        html : this.i18n
                                ._("This configures OpenVPN so it connects to a remote OpenVPN Server and can access exported hosts and networks."),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }]
                }]
            });
        },

        // overwrite default event manager
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
                        var date = end;
                        if (date == null) {
                            date = new Date();
                            date.setTime(0);
                        }
                        return date;
                    }
                }, {
                    name : 'clientName'
                }, {
                    name : 'address',
                    convert : function(val, rec) {
                        return val == null ? "" : val.addr + ":" + rec.port;
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
        getDistributeColumn : function() {
            return new Ext.grid.ButtonColumn({
                width : 110,
                header : this.i18n._("distribute"),
                dataIndex : null,
                handle : function(record) {
                    this.grid.distributeWindow.show();
                    this.grid.distributeWindow.populate(record);
                },
                renderer : function(value, metadata, record) {
                    var out= '';
                    if(record.data.id>=0) {
                    	out= '<div class="ungButton buttonColumn" style="text-align:center;" >' + i18n._("Distribute Client") + '</div>';
                    }
                    return out;
                }
            });
        },
        getDistributeWindow : function(grid) {
        	return new Ung.ButtonsWindow({
        		grid: grid,
        		sizeToGrid:true,
        		settingsCmp: this,
        		title: i18n._('OpenVPN Question...'),
        		distributeUsb: false,
                initButtons : function() {
                    this.subCmps.push(new Ext.Button({
                        name : 'Cancel',
                        renderTo : 'button_inner_right_' + this.getId(),
                        iconCls : 'cancelIcon',
                        text : i18n._('Cancel'),
                        handler : function() {
                            this.cancelAction();
                        }.createDelegate(this)
                    }));
                    this.subCmps.push(new Ext.Button({
                        name : 'Proceed',
                        renderTo : 'button_margin_right_' + this.getId(),
                        iconCls : 'saveIcon',
                        text : i18n._('Proceed'),
                        handler : function() {
                            this.proceedAction();
                        }.createDelegate(this)
                    }));
                },
                onRender : function(container, position) {
                    Ung.ButtonsWindow.superclass.onRender.call(this, container, position);
                    this.initSubComponents.defer(1, this);
                },
                initSubComponents : function(container, position) {
                    this.formPanel = new Ext.FormPanel({
                        renderTo : this.getContentEl(),
                        labelWidth : 75,
                        buttonAlign : 'right',
                        border : false,
                        bodyStyle : 'padding:10px 10px 0px 10px;',
                        autoScroll: true,
                        autoHeight : true,
                        defaults : {
                            selectOnFocus : true,
                            msgTarget : 'side'
                        },
                        items : [{
                            xtype : 'fieldset',
                            title :  i18n._('Question:'),
                            autoHeight : true,
                            items: [{
                                bodyStyle : 'padding:0px 0px 5px 5px;',
                                border : false,
                                html: i18n.sprintf(this.settingsCmp.i18n._("Please choose how you would like to distribute your digital key. <br>Note: If you choose to send via email, you must supply an email address to send the email to. If you choose to download to USB key, the data will be located on the key at: %s"),
                                    '<br>/untangle-data/openvpn/setup-<span id="openvpn_distributeWindow_client_internal_name"></span>.exe')
                            }, {
                                xtype : 'radio',
                                boxLabel : this.settingsCmp.i18n._('Distribute via Email'),
                                hideLabel : true,
                                id: 'openvpn_distributeWindow_distributeMethod_email',
                                name : 'distributeMethod',
                                checked : true,
                                value: 1,
                                listeners : {
                                    "check" : {
                                        fn : function(elem, checked) {
                                        	var emailCmp=Ext.getCmp('openvpn_distributeWindow_email_address');
                                        	this.distributeUsb=!checked;
                                        	if(checked) {
                                                emailCmp.enable();
                                                this.record.data.distributionEmail=emailCmp.getValue();
                                        	} else {
                                        		emailCmp.disable();
                                        		this.record.data.distributionEmail=null;
                                        	}
                                        }.createDelegate(this)
                                    }
                                }
                            }, {
                                xtype : 'textfield',
                                fieldLabel : this.settingsCmp.i18n._('Email Address'),
                                name : 'outsideNetwork',
                                id : 'openvpn_distributeWindow_email_address',
                                labelStyle: "width:150px;padding-left:20px;",
                                width: 200,
                                allowBlank : false,
                                blankText : this.settingsCmp.i18n._("You must specify an email address to send the key to."),
                                listeners : {
                                    "change" : {
                                        fn : function(elem, newValue) {
                                            this.record.data.distributionEmail = newValue;
                                        }.createDelegate(this)
                                    }
                                }
                            }, {
                                xtype : 'radio',
                                boxLabel : this.settingsCmp.i18n._('Distribute via USB Key'),
                                hideLabel : true,
                                name : 'distributeMethod',
                                checked : false,
                                value: 2
                            }]
                        }]
                    });
                    this.subCmps.push(this.formPanel);
                },
                populate : function(record) {
                    this.record = record;
                },                
                proceedAction : function() {
                	if(!this.distributeUsb && this.record.data.distributionEmail==null) {
                		Ext.MessageBox.alert(i18n._("Failure"), this.settingsCmp.i18n._("You must specify an email address to send the key to."));
                        return;
                	}
                	Ext.MessageBox.wait(this.settingsCmp.i18n._("Distributing digital key..."), i18n._("Please wait"));
                    this.settingsCmp.getRpcNode().distributeClientConfig(function(result, exception) {
                        if (exception) {
                            var errorMsg=(this.record.data.distributionEmail==null)?
                               this.settingsCmp.i18n._("OpenVPN was not able to save your digital key to your USB key.  Please try again."):
                               this.settingsCmp.i18n._("OpenVPN was not able to send your digital key via email.  Please try again.");
                            Ext.MessageBox.alert(this.settingsCmp.i18n._("Error saving/sending key"), errorMsg);
                            return;
                        }
                        var successMsg=(this.record.data.distributionEmail==null)?
                           this.settingsCmp.i18n._("OpenVPN successfully saved your digital key to your USB key."):
                           this.settingsCmp.i18n._("OpenVPN successfully sent your digital key via email.");
                        // go to next step
                        Ext.MessageBox.alert(this.settingsCmp.i18n._("Success"),successMsg, function() {
                        	this.hide();
                        }.createDelegate(this));
                    }.createDelegate(this), this.record.data)
                }
        	});
        },
        getGroupsColumn : function() {
            return {
                id : 'groupId',
                header : this.i18n._("address pool"),
                width : 200,
                dataIndex : 'groupId',
                renderer : function(value, metadata, record) {
                    var result = ""
                    var store = this.getGroupsStore();
                    if (store) {
                        var index = store.findBy(function(record, id) {
                            if (record.data.id == value) {
                                return true;
                            } else {
                                return false;
                            }
                        });
                        if (index >= 0) {
                            result = store.getAt(index).get("name");
                            record.data.group = store.getAt(index).data;
                        } else {
                        	record.data.group = null;
                        }
                        
                    }
                    return result;
                }.createDelegate(this),
                editor : new Ext.form.ComboBox({
                    store : this.getGroupsStore(),
                    displayField : 'name',
                    valueField : 'id',
                    editable : false,
                    mode : 'local',
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small'
                })
            }        	
        },
        generateGridClients : function(inWizard) {
            // live is a check column
        	var clientList=this.getVpnSettings().clientList.list;
        	
            var liveColumn = new Ext.grid.CheckColumn({
                id : "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width : 80,
                fixed : true
            });
            var distributeColumn = this.getDistributeColumn();
            var groupsColumn=this.getGroupsColumn();
            var defaultGroup= this.getGroupsStore().getCount()>0?this.getGroupsStore().getAt(0).data:null;
            var gridClients = new Ung.EditorGrid({
                initComponent : function() {
                	this.distributeWindow=this.settingsCmp.getDistributeWindow(this)
                    Ung.EditorGrid.prototype.initComponent.call(this);
                },
                settingsCmp : this,
                name : 'VPN Clients',
                // the total records is set from the base settings
                paginated : false,
                anchor : inWizard?null:"100% 50%",
                height : inWizard?250:250,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "groupId" : defaultGroup!=null?defaultGroup.id:null,
                    "group" : defaultGroup,
                    "address" : null
                },
                title : this.i18n._("VPN Clients"),
                // the column is autoexpanded if the grid width permits
                // autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.VpnClient",
                data : clientList,
                // the list of fields
                autoGenerateId: true,
                fields : [{
                    name : 'live'
                }, {
                    name : 'name'
                }, {
                    name : 'originalName',
                    mapping: 'name'
                }, {
                    name : 'group'
                }, {
                    name : 'groupId',
                    mapping: 'group',
                    convert : function(val, rec) {
                        return val==null?null:val.id;
                    }
                }, {
                    name : 'address'
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
                }, groupsColumn, distributeColumn, {
                    id : 'address',
                    header : this.i18n._("virtual address"),
                    width : 100,
                    dataIndex : 'address',
                    renderer : function(value, metadata, record) {
                        return value==null?this.i18n._("unassigned"):value;
                    }.createDelegate(this)
                }],
                // sortField : 'name',
                // columnsDefaultSortable : true,
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
                    dataIndex : "groupId",
                    fieldLabel : this.i18n._("Address pool"),
                    store : this.getGroupsStore(),
                    displayField : 'name',
                    valueField : 'id',
                    editable : false,
                    mode : 'local',
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small',
                    width : 200
                }]
            });
            return gridClients;
        },
        generateGridSites : function(inWizard) {
            // live is a check column
            var siteList=this.getVpnSettings().siteList.list;

            var liveColumn = new Ext.grid.CheckColumn({
                id : "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width : 80,
                fixed : true
            });
            var distributeColumn = this.getDistributeColumn();
            var groupsColumn=this.getGroupsColumn();
            var defaultGroup= this.getGroupsStore().getCount()>0?this.getGroupsStore().getAt(0).data:null

            var gridSites = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'VPN Sites',
                // the total records is set from the base settings
                paginated : false,
                anchor : inWizard?null:"100% 50%",
                height : inWizard?250:250,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "groupId" : defaultGroup!=null?defaultGroup.id:null,
                    "group" : defaultGroup,
                    "network" : "1.2.3.4",
                    "netmask" : "255.255.255.0",
                    "exportedAddressList" : {
                    	javaClass: "java.util.ArrayList",
                    	list:[{
                            javaClass:"com.untangle.node.openvpn.ClientSiteNetwork",
                    	   "network" : "1.2.3.4",
                    	   "netmask" : "255.255.255.0"
                        }]
                    }
                },
                title : this.i18n._("VPN Sites"),
                // the column is autoexpanded if the grid width permits
                // autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.VpnSite",
                data : siteList,
                // the list of fields
                autoGenerateId: true,
                fields : [{
                    name : 'live'
                }, {
                    name : 'name'
                }, {
                	name: 'exportedAddressList'
                }, {
                    name : 'network',
                    mapping : 'exportedAddressList',
                    convert : function(val, rec) {
                        return (val!=null && val.list!=null && val.list.length>0)?val.list[0].network:null;
                    }
                }, {
                    name : 'netmask',
                    mapping : 'exportedAddressList',
                    convert : function(val, rec) {
                        return (val!=null && val.list!=null && val.list.length>0)?val.list[0].netmask:null;
                    }
                }, {
                    name : 'group'
                }, {
                    name : 'groupId',
                    mapping: 'group',
                    convert : function(val, rec) {
                        return val==null?null:val.id;
                    }
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
                }, groupsColumn, {
                    id : 'network',
                    header : this.i18n._("network address"),
                    width : 100,
                    dataIndex : 'network',
                    renderer : function(value, metadata, record) {
                    	record.data.exportedAddressList.list[0].network=value;
                        return value;
                    },
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, {
                    id : 'netmask',
                    header : this.i18n._("network mask"),
                    width : 100,
                    dataIndex : 'netmask',
                    renderer : function(value, metadata, record) {
                        record.data.exportedAddressList.list[0].netmask=value;
                        return value;
                    },
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, distributeColumn],
                // sortField : 'name',
                // columnsDefaultSortable : true,
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
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Site name"),
                    allowBlank : false,
                    width : 200
                }, {
                    xtype : "combo",
                    name : "Address pool",
                    dataIndex : "groupId",
                    fieldLabel : this.i18n._("Address pool"),
                    store : this.getGroupsStore(),
                    displayField : 'name',
                    valueField : 'id',
                    editable : false,
                    mode : 'local',
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small',
                    width : 200
                }, {
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
            return gridSites;
        },
        buildClients : function() {
            this.gridClients = this.generateGridClients();
            this.gridSites = this.generateGridSites();
            this.panelClients = new Ext.Panel({
                // private fields
                name : 'Clients',
                parentId : this.getId(),
                title : this.i18n._('Clients'),
                layout : "form",
                autoScroll : true,
                items : [this.gridClients, this.gridSites]
            });
        },
        generateGridExports : function(inWizard) {
            // live is a check column
            var exportedAddressList=this.getVpnSettings().exportedAddressList.list;

            // live is a check column
            var liveColumn = new Ext.grid.CheckColumn({
                id : "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width : 80,
                fixed : true
            });

            var gridExports = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Exported Hosts and Networks',
                // the total records is set from the base settings
                paginated : false,
                height : inWizard?250:null,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "network" : "192.168.1.0",
                    "netmask" : "255.255.255.0"
                },
                title : this.i18n._("Exported Hosts and Networks"),
                // the column is autoexpanded if the grid width permits
                // autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.ServerSiteNetwork",
                data : exportedAddressList,
                // the list of fields
                autoGenerateId: true,
                fields : [{
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
                    editor : new Ext.form.TextField({})
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
                // sortField : 'name',
                // columnsDefaultSortable : true,
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
            return gridExports;
        },
        buildExports : function() {
            this.gridExports = this.generateGridExports();
        },
        generateGridGroups : function(inWizard) {
            // live is a check column
            var liveColumn = new Ext.grid.CheckColumn({
                id : "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width : 80,
                fixed : true
            });
            var exportDNSColumn = new Ext.grid.CheckColumn({
                id : "useDNS",
                header : this.i18n._("export DNS"),
                dataIndex : 'useDNS',
                width : 90,
                fixed : true
            });

            var gridGroups = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Address Pools',
                // the total records is set from the base settings
                paginated : false,
                height : inWizard?250:250,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "group" : null,
                    "address" : "172.16.16.0",
                    "netmask": "255.255.255.0"
                },
                title : this.i18n._("Address Pools"),
                // the column is autoexpanded if the grid width permits
                // autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.VpnGroup",
                data : this.getVpnSettings().groupList,
                dataRoot : 'list',
                // the list of fields
                fields : [{
                    name : 'id'
                }, {
                    name : 'live'
                }, {
                    name : 'name'
                }, {
                    name : 'address'
                }, {
                    name : 'netmask'
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
                }, exportDNSColumn],
                // sortField : 'name',
                // columnsDefaultSortable : true,
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
            return gridGroups;
        },
        buildAdvanced : function() {
            this.gridGroups = this.generateGridGroups();
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
                    items : [this.gridGroups]
                }, {
                    xtype : 'fieldset',
                    autoHeight : true,
                    defaults : {
                        labelStyle : "width:160px;"
                    },
                    items : [{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Server Port (UDP)'),
                        width : 80,
                        name : 'Server Port (UDP)',
                        id : 'openvpn_advanced_publicPort',
                        value : this.getVpnSettings().publicPort,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._("You must provide a valid port."),
                        vtype : 'port',
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getVpnSettings().publicPort = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Site Name'),
                        name : 'Site Name',
                        value : this.getVpnSettings().siteName,
                        id : 'openvpn_advanced_siteName',
                        allowBlank : false,
                        blankText : this.i18n._("You must enter a site name."),
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
                        columns : 1,
                        name : 'Site Name',
                        items : [{
                            boxLabel : this.i18n._('Enabled'),
                            name : 'DNSOverride',
                            inputValue : true,
                            checked : this.getVpnSettings().isDnsOverrideEnabled,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getVpnSettings().isDnsOverrideEnabled = checked;
                                        if (checked) {
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
                            boxLabel : this.i18n._('Disabled'),
                            name : 'DNSOverride',
                            inputValue : false,
                            checked : !this.getVpnSettings().isDnsOverrideEnabled,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getVpnSettings().isDnsOverrideEnabled = !checked;
                                        if (!checked) {
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
                    }, {
                        xtype : 'fieldset',
                        autoHeight : true,
                        style : 'margin:0px 0px 0px 160px;',
                        defaults : {
                            labelStyle : "width:160px;"
                        },
                        border : false,
                        items : [{
                            xtype : 'textfield',
                            fieldLabel : this.i18n._('Primary IP'),
                            name : 'outsideNetwork',
                            id : 'openvpn_advanced_dns1',
                            value : this.getVpnSettings().dns1,
                            disabled : !this.getVpnSettings().isDnsOverrideEnabled,
                            allowBlank : false,
                            blankText : this.i18n._('A Valid Primary IP Address must be specified.'),
                            vtype : 'ipAddress'
                        }, {
                            xtype : 'textfield',
                            fieldLabel : this.i18n._('Secondary IP (optional)'),
                            name : 'outsideNetwork',
                            id : 'openvpn_advanced_dns2',
                            disabled : !this.getVpnSettings().isDnsOverrideEnabled,
                            value : this.getVpnSettings().dns2,
                            vtype : 'ipAddress'
                        }]
                    }]
                }]
            });
        },
        
        // validation function
        validateClient : function() {
            return  this.validateAdvanced() && this.validateGroups() && this.validateVpnClients();
        },
        
        //validate OpenVPN Advanced settings
        validateAdvanced : function() {
        	//validate port
            var portCmp = Ext.getCmp("openvpn_advanced_publicPort"); 
            if(!portCmp.validate()) {
                Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("You must provide a valid port."), 
                    function () {
                        this.tabs.activate(this.panelAdvanced);
                        portCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            };
        	
            //validate site name
            var siteCmp = Ext.getCmp("openvpn_advanced_siteName"); 
            if(!siteCmp.validate()) {
                Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("You must enter a site name."), 
                    function () {
                        this.tabs.activate(this.panelAdvanced);
                        siteCmp.focus(true);
                    }.createDelegate(this) 
                );
                return false;
            };
            
            if (this.getVpnSettings().isDnsOverrideEnabled) {
                var dns1Cmp = Ext.getCmp("openvpn_advanced_dns1"); 
                if(!dns1Cmp.validate()) {
                    Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("A valid Primary IP Address must be specified."), 
                        function () {
                            this.tabs.activate(this.panelAdvanced);
                            dns1Cmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                };
                
                var dns2Cmp = Ext.getCmp("openvpn_advanced_dns2"); 
                if(!dns2Cmp.validate()) {
                    Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("A valid Secondary IP Address must be specified."), 
                        function () {
                            this.tabs.activate(this.panelAdvanced);
                            dns2Cmp.focus(true);
                        }.createDelegate(this) 
                    );
                    return false;
                };
                
                //prepare for save
                this.getVpnSettings().dns1 = dns1Cmp.getValue();
                this.getVpnSettings().dns2 = dns2Cmp.getValue() == "" ? null : dns2Cmp.getValue();
            }  else {
                this.getVpnSettings().dns1 = null;
                this.getVpnSettings().dns2 = null;
            }
            
            return true;
        },
        
        validateGroups : function() {
            var groupList=this.gridGroups.getFullSaveList();
            
            // verify that there is at least one group
            if(groupList.length <= 0 ){
                Ext.MessageBox.alert(this.i18n._('Failed'), this.i18n._("You must create at least one group."),
                    function () {
                        this.tabs.activate(this.panelAdvanced);
                    }.createDelegate(this) 
                );
                return false;
            }
            
            // removed groups should not be referenced
            var removedGroups = this.gridGroups.getDeletedList();
            for(var i=0; i<removedGroups.length;i++) {
                var clientList = this.gridClients.getFullSaveList();
                for(var j=0; j<clientList.length;j++) {
                    if (removedGroups[i].id == clientList[j].groupId) {
                        Ext.MessageBox.alert(this.i18n._('Failed'), 
                            i18n.sprintf(this.i18n._("The group: \"%s\" cannot be deleted because it is being used by the client: %s in the Client To Site List."), removedGroups[i].name, clientList[j].name),
                            function () {
                                this.tabs.activate(this.panelAdvanced);
                            }.createDelegate(this) 
                        );
                        return false;
                    }
                }
                var siteList=this.gridSites.getFullSaveList();
                for(var j=0; j<siteList.length;j++) {
                    if (removedGroups[i].id == siteList[j].groupId) {
                        Ext.MessageBox.alert(this.i18n._('Failed'), 
                            i18n.sprintf(this.i18n._("The group: \"%s\" cannot be deleted because it is being used by the site: %s in the Site To Site List."), removedGroups[i].name, siteList[j].name),
                            function () {
                                this.tabs.activate(this.panelAdvanced);
                            }.createDelegate(this) 
                        );
                        return false;
                    }
                }
            }
            
            return true;
        },
        
        validateVpnClients : function() {
            var clientList=this.gridClients.getFullSaveList();
            for(var i=0;i<clientList.length;i++) {
                if(clientList[i].id>=0 && clientList[i].name!=clientList[i].originalName) {
                    Ext.MessageBox.alert(i18n._("Failed"), i18n.sprintf(this.i18n._('You cannot change an account name after its key has been distributed. Client name should be %s.'), clientList[i].originalName),
                        function () {
                            this.tabs.activate(this.panelClients);
                        }.createDelegate(this) 
                    );
                    return false;
                }
            }
            return true;
        },
        
        validateServer : function () {
        	//TODO: start this from 0 and make it work.
        	// implement validation functions from VpnSettings.validate()
        	var validateData = {
                map : {},
                javaClass : "java.util.HashMap"
            };
            try {
                var result = this.getOpenVpnValidator().validate(validateData);
                if (!result.valid) {
                    var errorMsg = "";

                    Ext.MessageBox.alert(this.i18n._("Validation failed"), errorMsg);
                    return false;
                }
            } catch (e) {
                Ext.MessageBox.alert(i18n._("Failed"), e.message);
                return false;
            }
        	return true;
        },
        // save function
        save : function() {
            // validate first
            if(this.configState == "SERVER_ROUTE") {
                if (this.validate()) {
                	var vpnSettings=this.getVpnSettings();
                	vpnSettings.groupList.list=this.gridGroups.getFullSaveList();
                    vpnSettings.exportedAddressList.list=this.gridExports.getFullSaveList();
                    vpnSettings.clientList.list=this.gridClients.getFullSaveList();
                    vpnSettings.siteList.list=this.gridSites.getFullSaveList();
                    Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                    this.getRpcNode().setVpnSettings(function(result, exception) {
                        Ext.MessageBox.hide();
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        // exit settings screen
                        this.cancelAction();
                    }.createDelegate(this), vpnSettings);
                }
            }
        },

        configureVNPClient : function() {
            if (this.clientSetup) {
                Ext.destroy(this.clientSetup)
            }
            var welcomeCard = {
                title : this.i18n._("Welcome"),
                cardTitle : this.i18n._("Welcome to the OpenVPN Setup Wizard!"),
                panel : {
                    xtype : 'form',
                    items : [{
                        html : this.i18n
                                ._('This wizard will help guide you through your initial setup and configuration of OpenVPN as a VPN Client.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        html : this.i18n
                                ._('Warning:  Finishing this wizard will cause any previous OpenVPN settings you had to be lost, and overwritten by new settings.  Only finish this wizard if you would like completely new settings.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }]
                },
                onNext : function(handler) {
                    this.getRpcNode().startConfig(function(result, exception) {
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        this.handler();
                    }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), "CLIENT")
                }.createDelegate(this)
            };
            var downloadCard = {
                title : this.i18n._("Download Configuration"),
                cardTitle : this.i18n._("Welcome to the OpenVPN Setup Wizard!"),
                panel : {
                    xtype : 'form',
                    items : [{
                        html : this.i18n
                                ._('Please specify where your VPN Client configuration should come from.  You may specify a Server or USB Key.  If you choose USB Key, you must press "Read USB Key" to load configurations from the key, and then choose a configuration from the drop-down-list.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        xtype : 'fieldset',
                        autoHeight : true,
                        buttonAlign : 'left',
                        items : [{
                            xtype : "radio",
                            boxLabel : this.i18n._('Download from Server'),
                            hideLabel : true,
                            name : 'downloadClientConfiguration',
                            inputValue : true,
                            checked : true,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        if (checked) {
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
                            id : 'openvpn_client_wizard_server_ip',
                            name : "Server IP Address",
                            labelStyle : 'text-align: right;  width: 150px;',
                            fieldLabel : this.i18n._("Server IP Address"),
                            allowBlank : false,
                            width : 200
                                // vtype : 'ipAddress'
                                }, {
                                    xtype : "textfield",
                                    id : 'openvpn_client_wizard_password',
                                    name : "Password",
                                    labelStyle : 'text-align: right; width: 150px;',
                                    fieldLabel : this.i18n._("Password"),
                                    width : 200,
                                    inputType : 'password'
                                }, {
                                    xtype : "radio",
                                    boxLabel : this.i18n._('Download from USB Key'),
                                    style : "margin-left:20px;",
                                    hideLabel : true,
                                    name : 'downloadClientConfiguration',
                                    inputValue : true,
                                    checked : false

                                }, {
                                    xtype : "panel",
                                    border : false,
                                    width : 500,
                                    layout : 'column',
                                    items : [{
                                        width : 180,
                                        border : false,
                                        items : [{
                                            xtype : 'button',
                                            id : 'openvpn_client_read_usb',
                                            name : 'Read USB Key',
                                            text : this.i18n._('Read USB Key'),
                                            style : "padding-left: 30px;",
                                            disabled : true,
                                            handler : function() {
                                                // getAvailableUsbList
                                                this.getRpcNode().getAvailableUsbList(function(result, exception) {
                                                    if (exception) {
                                                        Ext.MessageBox
                                                                .alert(
                                                                        i18n._("OpenVPN Setup Wizard Warning"),
                                                                        this.i18n
                                                                                ._("The USB Key could not be read.  Please make sure the key is properly inserted, and try again."));
                                                        return;
                                                    }
                                                    var configurationsCmp = Ext.getCmp("openvpn_client_configurations");
                                                    var storeData = [];
                                                    for (var i = 0; i < result.list.length; i++) {
                                                        storeData.push([result.list[i], result.list[i]])
                                                    }
                                                    configurationsCmp.store.loadData(storeData);
                                                }.createDelegate(this))
                                            }.createDelegate(this)
                                        }]
                                    }, {
                                        width : 280,
                                        border : false,
                                        items : [{
                                            xtype : 'combo',
                                            name : 'Configurations',
                                            id : 'openvpn_client_configurations',
                                            editable : false,
                                            hideLabel : true,
                                            width : 230,
                                            mode : 'local',
                                            triggerAction : 'all',
                                            listClass : 'x-combo-list-small',
                                            store : new Ext.data.SimpleStore({
                                                fields : ['key', 'name'],
                                                data : [["[No Configurations]", this.i18n._("[No Configurations]")],]
                                            }),
                                            displayField : 'name',
                                            valueField : 'key',
                                            value : "[No Configurations]",
                                            disabled : true
                                        }]
                                    }]
                                }]
                    }]
                },
                onNext : function(handler) {
                    if (!Ext.getCmp("openvpn_client_wizard_server_ip").disabled) {
                        // download from server
                        var serverAddress = Ext.getCmp("openvpn_client_wizard_server_ip").getValue();
                        var serverPort = null;
                        var serverAddressErrorMsg = this.i18n._('The "Server Address" is not a valid IP address.');
                        if (serverAddress == null || serverAddress.length == 0) {
                            Ext.MessageBox.alert(i18n._("Failed"), serverAddressErrorMsg);
                            return;
                        }
                        var serverAddressArr = serverAddress.split(":");
                        if (serverAddressArr.length == 1) {
                            serverPort = "443"
                        } else if (serverAddressArr.length == 2) {
                            serverPort = serverAddressArr[1];
                        } else {
                            Ext.MessageBox.alert(i18n._("Failed"), serverAddressErrorMsg);
                            return;
                        }
                        var ipAddr = serverAddressArr[0];
                        var password = Ext.getCmp("openvpn_client_wizard_password").getValue();
                        if (password == null || password.length == 0) {
                            Ext.MessageBox.alert(i18n._("Failed"), this.i18n
                                    ._('Please supply a password will be used to connect to the server.'));
                            return;
                        }
                        Ext.MessageBox.wait(i18n._("Downloading Configuration... (This may take up to one minute)"), i18n._("Please wait"));
                        this.getRpcNode().downloadConfig(function(result, exception) {
                            Ext.MessageBox.hide();
                            if (exception) {
                                Ext.MessageBox.alert(this.settingsCmp.i18n._("Error downloading config from server."), i18n
                                        ._("Your VPN Client configuration could not be downloaded from the server.  Please try again."));
                                return;
                            }
                            this.getRpcNode().completeConfig(function(result, exception) {
                                if (exception) {
                                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                    return;
                                }
                                // go to next step
                                this.handler();
                            }.createDelegate(this))
                        }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), ipAddr, serverPort, password)
                    } else {
                        // download from usb key
                        var configCombo = Ext.getCmp("openvpn_client_configurations");
                        var selection = configCombo.getValue();
                        if (selection == "[No Configurations]") {
                            Ext.MessageBox.alert(i18n._("Failed"), this.i18n._('You must click "Read USB Key" before proceeding.'));
                            return;
                        } else if (selection == "") {
                            Ext.MessageBox
                                    .alert(
                                            i18n._("Failed"),
                                            this.i18n
                                                    ._('You must click "Read USB Key", and select a valid configuration from the drop-down-list before proceeding."'));
                            return;
                        }
                        Ext.MessageBox.wait(i18n._("Downloading Configuration... (This may take up to one minute)"), i18n._("Please wait"));
                        this.getRpcNode().downloadConfigUsb(function(result, exception) {
                            if (exception) {
                                Ext.MessageBox.alert(this.settingsCmp.i18n._("Error downloading config from USB key."), i18n
                                        ._("Your VPN Client configuration could not be downloaded from the USB key.  Please try again."));
                                return;
                            }
                            this.getRpcNode().completeConfig(function(result, exception) {
                            	Ext.MessageBox.hide();
                                if (exception) {
                                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                    return;
                                }
                                // go to next step
                                this.handler();
                            }.createDelegate(this))
                        }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), selection)
                    }
                }.createDelegate(this)
            };
            var congratulationsCard = {
                title : this.i18n._("Congratulations"),
                cardTitle : this.i18n._("Congratulations! OpenVPN is configured as a VPN Client."),
                panel : {
                    xtype : 'form',
                    items : [{
                        html : this.i18n
                                ._('If necessary, you can change the configuration of OpenVPN by launching the Setup Wizard again.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }]
                },
                onNext : function(handler) {
                    this.clientSetup.endAction();
                }.createDelegate(this)
            };
            var clientWizard = new Ung.Wizard({
                height : 450,
                width : 800,
                cardDefaults : {
                    labelWidth : 150,
                    cls : 'untangle-form-panel'
                },
                cards : [welcomeCard, downloadCard, congratulationsCard]
            });

            this.clientSetup = new Ung.Window({
                title : this.i18n._("OpenVPN Client Setup Wizard"),
                closeAction : "endAction",
                items : [{
                    region : "center",
                    items : [clientWizard],
                    border : false,
                    autoScroll : true,
                    cls : 'windowBackground',
                    bodyStyle : 'background-color: transparent;'
                }],
                endAction : function() {
                    this.clientSetup.hide();
                    Ext.destroy(this.clientSetup);
                    if(this.mustRefresh || true) {
                    	var nodeWidget=this.node;
                    	nodeWidget.settingsWin.cancelAction();
                    	nodeWidget.onSettingsAction();
                    }
                }.createDelegate(this)
            });

            this.clientSetup.show();
            clientWizard.goToPage(0);
        },

        //
        configureVNPServer : function() {
            if (this.serverSetup) {
                Ext.destroy(this.serverSetup)
            }
            var welcomeCard = {
                title : this.i18n._("Welcome"),
                cardTitle : this.i18n._("Welcome to the OpenVPN Setup Wizard!"),
                panel : {
                    xtype : 'form',
                    items : [{
                        html : this.i18n
                                ._('This wizard will help guide you through your initial setup and configuration of OpenVPN as a VPN Routing Server.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        html : this.i18n
                                ._('Warning:  Finishing this wizard will cause any previous OpenVPN settings you had to be lost, and overwritten by new settings.  Only finish this wizard if you would like completely new settings.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }]
                },
                onNext : function(handler) {
                    this.getRpcNode().startConfig(function(result, exception) {
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        this.handler();
                    }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), "SERVER_ROUTE")
                }.createDelegate(this)
            };
            var certificateCard = {
                title : this.i18n._("Generate Certificate"),
                cardTitle : this.i18n._("Generate Certificate"),
                panel : {
                    xtype : 'form',
                    items : [{
                        html : this.i18n
                                ._('Please take a moment to specify some information about your location, to be used to generate a secure digital certificate.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        xtype : 'fieldset',
                        autoHeight : true,
                        buttonAlign : 'left',
                        title : this.i18n._('This information is required.'),
                        items : [{
                            xtype : "textfield",
                            id : 'openvpn_server_wizard_organization',
                            name : "Organization",
                            fieldLabel : this.i18n._("Organization"),
                            allowBlank : false,
                            width : 200
                        }, {
                            xtype : "textfield",
                            id : 'openvpn_server_wizard_country',
                            name : "Country",
                            fieldLabel : this.i18n._("Country"),
                            allowBlank : false,
                            value : 'US',
                            width : 200
                        }, {
                            xtype : "textfield",
                            id : 'openvpn_server_wizard_state',
                            name : "State",
                            fieldLabel : this.i18n._("State"),
                            allowBlank : false,
                            width : 200
                        }, {
                            xtype : "textfield",
                            id : 'openvpn_server_wizard_locality',
                            name : "City",
                            fieldLabel : this.i18n._("City"),
                            allowBlank : false,
                            width : 200
                        }]
                    }]
                },
                onNext : function(handler) {
                	var organization=Ext.getCmp("openvpn_server_wizard_organization").getValue();
                    var country=Ext.getCmp("openvpn_server_wizard_country").getValue();
                    var state=Ext.getCmp("openvpn_server_wizard_state").getValue();
                    var locality=Ext.getCmp("openvpn_server_wizard_locality").getValue();
                    if(organization.length==0) {
                    	Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("You must fill out the name of your organization."));
                    	return;
                    }
                    if(country.length==0) {
                        Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("You must fill out the name of your country."));
                        return;
                    }
                    if(country.length!=2) {
                        Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("The country code must be 2 characters long."));
                        return;
                    }
                    if(state.length==0) {
                        Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("You must fill out the name of your state."));
                        return;
                    }
                    if(locality.length==0) {
                        Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("You must fill out the name of your locality."));
                        return;
                    }
                    Ext.MessageBox.wait(i18n._("Generating Certificate..."), i18n._("Please wait"));
                    var certificateParameters={
                        javaClass: "com.untangle.node.openvpn.CertificateParameters",
                        organization: organization,
                        domain: "",
                        country: country,
                        state: state,
                        locality: locality,
                        storeCaUsb: false
                    };
                    this.getRpcNode().generateCertificate(function(result, exception) {
                        Ext.MessageBox.hide();
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        this.handler();
                    }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), certificateParameters)
                }.createDelegate(this)
            };
            var gridGroups = this.generateGridGroups(true);
            var groupsCard = {
                title : this.i18n._("Add Address Pools"),
                cardTitle : this.i18n._("Add Address Pools"),
                panel : {
                    xtype : 'form',
                    items : [{
                        html : "<b>" + this.i18n._('Please add at least one Address Pool.') + "</b>",
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        html : this.i18n
                                ._('Connecting VPN clients and sites will be assigned IP addresses from these pools, and each pool can have a policy applied to it.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, gridGroups]
                },
                onNext : function(handler) {
                	var gridGroups=Ext.getCmp(this.serverSetup.gridGroupsId);
                	var saveList=gridGroups.getFullSaveList();
                	if(saveList.length==0) {
                		Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("You must create at least one group."));
                        return;
                	}
                	Ext.MessageBox.wait(i18n._("Adding Address Pools..."), i18n._("Please wait"));
                	this.getRpcNode().setAddressGroups(function(result, exception) {
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        this.settingsCmp.getRpcNode().getAddressGroups(function(result, exception) {
                            Ext.MessageBox.hide();
                            if (exception) {
                                Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                return;
                            }
                            // go to next step
                            this.settingsCmp.getGroupsStore().loadData(result.groupList.list);
                            this.handler();
                        }.createDelegate(this))
                        
                    }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), {javaClass:"com.untangle.node.openvpn.GroupList", groupList:{javaClass:"java.util.ArrayList", list: saveList}})
                }.createDelegate(this)
            };
            var gridExports = this.generateGridExports(true);
            var exportsCard = {
                title : this.i18n._("Add Exports"),
                cardTitle : this.i18n._("Add Exports"),
                panel : {
                    xtype : 'form',
                    items : [{
                        html : "<b>" + this.i18n._('Optionally add exported hosts and networks.') + "</b>",
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        html : this.i18n._('These exports will be visible to clients and sites on the VPN, and vice versa.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, gridExports]
                },
                onNext : function(handler) {
                    var gridExports=Ext.getCmp(this.serverSetup.gridExportsId);
                    var saveList=gridExports.getFullSaveList();
                    Ext.MessageBox.wait(i18n._("Adding Exports..."), i18n._("Please wait"));
                    this.getRpcNode().setExportedAddressList(function(result, exception) {
                        Ext.MessageBox.hide();
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        var gridClients=Ext.getCmp(this.settingsCmp.serverSetup.gridClientsId);
                        var gridSites=Ext.getCmp(this.settingsCmp.serverSetup.gridSitesId);
                        gridClients.clearChangedData();
                        gridSites.clearChangedData();
                        Ung.Util.loadGridStoreData(gridClients,[]);
                        Ung.Util.loadGridStoreData(gridSites,[]);
                        this.handler();
                    }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), {javaClass:"com.untangle.node.openvpn.ExportList", exportsList:{javaClass:"java.util.ArrayList", list: saveList}})
                }.createDelegate(this)
            };
            var gridClients = this.generateGridClients(true);
            var clientsCard = {
                title : this.i18n._("Add VPN Clients"),
                cardTitle : this.i18n._("Add VPN Clients"),
                panel : {
                    xtype : 'form',
                    items : [{
                        html : "<b>" + this.i18n._('Optionally add VPN Clients.') + "</b>",
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        html : this.i18n
                                ._('VPN Clients can remotely login to the VPN, and access any exported hosts or networks, and vice versa.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, gridClients]
                },
                onNext : function(handler) {
                    var gridClients=Ext.getCmp(this.serverSetup.gridClientsId);
                    var saveList=gridClients.getFullSaveList();
                    Ext.MessageBox.wait(i18n._("Adding VPN Clients..."), i18n._("Please wait"));
                    this.getRpcNode().setClients(function(result, exception) {
                        Ext.MessageBox.hide();
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        this.handler();
                    }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), {javaClass:"com.untangle.node.openvpn.ClientList", exportsList:{javaClass:"java.util.ArrayList", list: saveList}})
                }.createDelegate(this)
            };
            var gridSites = this.generateGridSites(true);
            var sitesCard = {
                title : this.i18n._("Add VPN Sites"),
                cardTitle : this.i18n._("Add VPN Sites"),
                panel : {
                    xtype : 'form',
                    items : [{
                        html : "<b>" + this.i18n._('Optionally add VPN Sites.') + "</b>",
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, {
                        html : this.i18n
                                ._('VPN Sites are remote networks that can access any exported hosts and networks on the VPN, and vice versa.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }, gridSites]
                },
                onNext : function(handler) {
                    var gridSites=Ext.getCmp(this.serverSetup.gridSitesId);
                    var saveList=gridSites.getFullSaveList();
                    Ext.MessageBox.wait(i18n._("Adding VPN Sites..."), i18n._("Please wait"));
                    this.getRpcNode().setSites(function(result, exception) {
                        if (exception) {
                            Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                            return;
                        }
                        this.settingsCmp.getRpcNode().completeConfig(function(result, exception) {
                            Ext.MessageBox.hide();
                            if (exception) {
                                Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                                return;
                            }
                            // go to next step
                            this.handler();
                        }.createDelegate(this))
                    }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), {javaClass:"com.untangle.node.openvpn.SiteList", exportsList:{javaClass:"java.util.ArrayList", list: saveList}})
                }.createDelegate(this)
            };
            var congratulationsCard = {
                title : this.i18n._("Congratulations"),
                cardTitle : this.i18n._("Congratulations! OpenVPN is configured as a VPN Routing Server."),
                panel : {
                    xtype : 'form',
                    items : [{
                        html : this.i18n
                                ._('If necessary, you can change the configuration of OpenVPN by launching the Setup Wizard again.'),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        border : false
                    }]
                },
                onNext : function(handler) {
                    this.serverSetup.endAction();
                }.createDelegate(this)
            };
            var serverWizard = new Ung.Wizard({
                height : 450,
                width : 800,
                cardDefaults : {
                    labelWidth : 150,
                    cls : 'untangle-form-panel'
                },
                cards : [welcomeCard, certificateCard, groupsCard, exportsCard, clientsCard, sitesCard, congratulationsCard]
            });

            this.serverSetup = new Ung.Window({
                gridGroupsId: gridGroups.getId(),
                gridExportsId: gridExports.getId(),
                gridClientsId: gridClients.getId(),
            	gridSitesId: gridSites.getId(),
                title : this.i18n._("OpenVPN Client Setup Wizard"),
                closeAction : "endAction",
                items : [{
                    region : "center",
                    items : [serverWizard],
                    border : false,
                    autoScroll : true,
                    cls : 'windowBackground',
                    bodyStyle : 'background-color: transparent;'
                }],
                endAction : function() {
                    this.serverSetup.hide();
                    Ext.destroy(this.serverSetup);
                    if(this.mustRefresh || true) {
                        var nodeWidget=this.node;
                        nodeWidget.settingsWin.cancelAction();
                        nodeWidget.onSettingsAction();
                    }
                }.createDelegate(this)
            });

            this.serverSetup.show();
            serverWizard.goToPage(0);
        }
    });
}
