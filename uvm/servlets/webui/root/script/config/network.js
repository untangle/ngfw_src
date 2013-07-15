if (!Ung.hasResource["Ung.Network"]) {
    Ung.hasResource["Ung.Network"] = true;

    Ung.NetworkUtil = {
        getPortForwardMatchers: function (settingsCmp) {
            return [
                {name:"DST_LOCAL",displayName: settingsCmp.i18n._("Destined Local"), type: "boolean", visible: true},
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true, allowInvert: false}
            ];
        },
        getNatRuleMatchers: function (settingsCmp) {
            return [
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true, allowInvert: false}
            ];
        },
        getBypassRuleMatchers: function (settingsCmp) {
            return [
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"]], visible: true, allowInvert: false}
            ];
        },
        getQosRuleMatchers: function (settingsCmp) {
            return [
                {name:"DST_LOCAL",displayName: settingsCmp.i18n._("Destined Local"), type: "boolean", visible: true},
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"]], visible: true, allowInvert: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: false}
            ];
        },
        getFilterRuleMatchers: function (settingsCmp) {
            return [
                {name:"DST_LOCAL",displayName: settingsCmp.i18n._("Destined Local"), type: "boolean", visible: true},
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
                {name:"SRC_MAC" ,displayName: settingsCmp.i18n._("Source MAC"), type: "text", visible: true},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true, allowInvert: false}
            ];
        },
        onRefreshDeviceStatus: function(settingsCmp, grid) {
            Ext.MessageBox.wait(settingsCmp.i18n._("Refreshing Device Status..."), i18n._("Please wait"));
            main.getNetworkManager().getDeviceStatus(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                var deviceStatusMap=Ung.Util.createRecordsMap(result.list, "deviceName");
                grid.getStore().suspendEvents();
                grid.getStore().each(function( currentRow ) {
                    var deviceStatus = deviceStatusMap[currentRow.get("deviceName")];
                    if(deviceStatus) {
                        var isDirty = currentRow.dirty;
                        currentRow.set({
                            "macAddress": deviceStatus.macAddress,
                            "duplex": deviceStatus.duplex,
                            "vendor": deviceStatus.vendor,
                            "mbit": deviceStatus.mbit,
                            "connected": deviceStatus.connected
                        });
                        //To prevent coloring the row when device status is changed
                        if(!isDirty) {
                            currentRow.commit();
                        }
                    }
                });
                grid.getStore().resumeEvents();
                grid.getView().refresh();

                Ext.MessageBox.hide();
            }, this));
        }
    };
    Ext.define("Ung.NetworkTest", {
        extend: "Ung.Window",
        settingsCmp: null,
        execResultReader: null,
        initComponent : function( ){
            Ext.applyIf( this, {
                testErrorMessage: this.settingsCmp.i18n._( "Unable to run this Network Utility." ),
                testTopToolbar: []
            });
            
            this.bbar = [{
                iconCls: 'icon-help',
                text: this.settingsCmp.i18n._('Help'),
                handler: function() {
                    this.helpAction();
                },
                scope: this
            },'->',{
                name: "Close",
                iconCls: 'cancel-icon',
                text: this.settingsCmp.i18n._('Cancel'),
                handler: function() {
                    this.cancelAction();
                },
                scope: this
            }];
            
            this.items = [{
                xtype: "panel",
                layout: 'anchor',
                bodyStyle : "padding: 10px 10px 0px 10px;",
                items : [{
                    xtype: "label",
                    html: this.testDescription,
                    style: "padding-bottom: 10px;"
                },{
                    xtype: "panel",
                    style: "margin: 10px 0px 0px 0px",
                    layout: "anchor",
                    anchor: "100% -60",
                    tbar: this.testTopToolbar.concat([this.runTest = Ext.create("Ext.button.Button",{
                        text : this.settingsCmp.i18n._("Run Test"),
                        iconCls : "icon-test-run",
                        handler : this.onRunTest,
                        scope : this
                    }),"->",this.clearOutput = Ext.create("Ext.button.Button",{
                        text : this.settingsCmp.i18n._("Clear Output"),
                        iconCls : "icon-clear-output",
                        handler : this.onClearOutput,
                        scope : this
                    })]),
                    items : [this.output=Ext.create("Ext.form.field.TextArea", {
                        name : "output",
                        emptyText : this.testEmptyText,
                        hideLabel : true,
                        readOnly : true,
                        anchor : "100% 100%",
                        cls : "ua-test-output",
                        style : "padding: 8px"
                    })]
                }]
            }];
            this.callParent(arguments);
        },
        helpAction: function() {
            main.openHelp(this.helpSource);
        },
        getCommand: function () {
            return this.command;
        },
        onRunTest : function() {
            if ( Ext.isFunction(this.isValid)) {
                if ( !this.isValid()) {
                    return;
                }
            }
            
            // Disable the run test button
            this.runTest.setIconCls( "icon-test-running" );
            this.output.focus();
            this.runTest.disable();
            this.enableParameters( false );
            var text = [];
            text.push( this.output.getValue());
            text.push( "" + new Date() + " - " + this.settingsCmp.i18n._("Test Started")+"\n");
            this.output.setValue( text.join( "" ));
            main.getExecManager().execEvil(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return; 
                this.execResultReader = result;
                this.continueNetworkUtility();
            }, this), this.getCommand());
        },
        finishNetworkUtility: function() {
            this.runTest.setIconCls( "icon-test-run" );
            this.enableParameters( true );
            this.runTest.enable();
            this.execResultReader = null;
        },
        continueNetworkUtility: function(){
            if( this.execResultReader === null ) {
                return;
            }
            if ( this.hidden) {
                this.execResultReader.getResult(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    if(result == -1) {
                        this.execResultReader.destroy(Ext.bind(function(result, exception) {
                            this.finishNetworkUtility();
                        }, this));
                    }
                }, this));
                return;
            }
            
            this.execResultReader.readFromOutput(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                var element=this.output.getEl();
                var text = [];
                text.push( this.output.getValue());
                if(result !== null) { //Test is running
                    text.push(result);
                    window.setTimeout( Ext.bind(this.continueNetworkUtility, this), 1000 );  
                } else { //Test is finished
                    text.push( "" + new Date() + " - " + this.settingsCmp.i18n._("Test Completed") +"\n\n");
                    this.finishNetworkUtility();
                }
                this.output.setValue( text.join( "" ));
                element.scroll( "b", 10000 );
               
            }, this));
        },
        onClearOutput: function() {
            this.output.setValue( "" );
        },
        enableParameters: function( isEnabled ){
        }
    });
    
    Ung.NetworkSettingsCmp = null;
    Ext.define("Ung.Network", {
        extend: "Ung.ConfigWin",
        gridPortForwardRules: null,
        gridNatRules: null,
        gridBypassRules: null,
        gridStaticRoutes: null,
        panelInterfaces: null,
        panelHostName: null,
        panelServices: null,
        panelPortForwardRules: null,
        panelNatRules: null,
        panelRoutes: null,
        panelAdvanced: null,
        panelTroubleshooting: null,

        initComponent: function() {
            Ung.NetworkSettingsCmp = this;
            this.breadcrumbs = [{
                title: i18n._("Configuration"),
                action: Ext.bind(function() {
                    this.cancelAction();
                }, this)
            }, {
                title: i18n._('Network')
            }];
            this.refreshSettings();
            // builds the tabs
            this.buildInterfaces();
            this.buildHostName();
            this.buildServices();
            this.buildPortForwardRules();
            this.buildNatRules();
            this.buildBypassRules();
            this.buildRoutes();
            this.buildAdvanced();
            this.buildTroubleshooting();
            
            // builds the tab panel with the tabs
            this.buildTabPanel([ this.panelInterfaces, this.panelHostName, this.panelServices, this.panelPortForwardRules, this.panelNatRules, this.panelBypassRules, this.panelRoutes, this.panelAdvanced, this.panelTroubleshooting ]);

            // Check if QoS is enabled and there are some initial WANs without downloadBandwidthKbps or uploadBandwidthKbps limits set and mark dirty if true,
            // in order to make the user save the valid settings when new WANs are added
            if(this.settings.qosSettings.qosEnabled) {
                for(var i=0; i<this.settings.interfaces.list.length; i++) {
                    var intf =this.settings.interfaces.list[i];
                    if(intf.isWan && (Ext.isEmpty(intf.downloadBandwidthKbps) || Ext.isEmpty(intf.uploadBandwidthKbps))) {
                        this.markDirty();
                        break;
                    }
                }
            }
            this.callParent(arguments);
        },
        // Interfaces Panel
        buildInterfaces: function() {
            var settingsCmp = this;
            var deleteVlanColumn = Ext.create('Ext.grid.column.Action', {
                menuDisabled: true,
                dataIndex: 'isVlanInterface',
                header: this.i18n._("Delete VLAN"),
                width: 80,
                init:function(grid) {
                    this.grid=grid;
                },
                handler: function(view, rowIndex, colIndex) {
                    var rec = view.getStore().getAt(rowIndex);
                    if(rec.get("isVlanInterface")) {
                        this.grid.deleteHandler(rec);
                    }
                },
                getClass: function(value, metadata, record) { 
                    if(record.get("isVlanInterface")) {
                        return 'icon-delete-row'; 
                    } else {
                        return 'x-hide-display';
                                       
                    }
                }
            }); 
            
            this.gridInterfaces = Ext.create('Ung.EditorGrid',{
                flex: 1,
                name: 'Interfaces',
                settingsCmp: this,
                paginated: false,
                hasReorder: false,
                hasDelete: false,
                hasAdd: false,
                addAtTop: false,
                columnsDefaultSortable: true,
                enableColumnHide: true,
                columnMenuDisabled: false,
                title: this.i18n._("Interfaces"),
                recordJavaClass: "com.untangle.uvm.network.InterfaceSettings",
                dataProperty: "interfaces",
                emptyRow: { //Used only to add VLAN Interfaces
                    "interfaceId": -1,
                    "isVlanInterface": true,
                    "vlanTag": 1,
                    "javaClass": "com.untangle.uvm.network.InterfaceSettings",
                    "v4ConfigType": "STATIC", 
                    "v6ConfigType": "DISABLED"
                },
                fields: [{
                    name: 'interfaceId'
                }, {
                    name: 'name'
                }, {
                    name: 'physicalDev'
                }, {
                    name: 'systemDev'
                }, {
                    name: 'symbolicDev'
                }, {
                    name: 'configType'
                }, {
                    name: 'isWan'
                }, {
                    name: 'isVlanInterface'
                }, {
                    name: 'vlanTag'
                }, {
                    name: 'vlanParent'
                }, {
                    name: 'bridgedTo'
                }, {
                    name: 'v4ConfigType'
                }, {
                    name: 'v4StaticAddress'
                }, {
                    name: 'v4StaticPrefix'
                }, {
                    name: 'v4StaticGateway'
                }, {
                    name: 'v4StaticDns1'
                }, {
                    name: 'v4StaticDns2'
                }, {
                    name: 'v4AutoAddressOverride'
                }, {
                    name: 'v4AutoPrefixOverride'
                }, {
                    name: 'v4AutoGatewayOverride'
                }, {
                    name: 'v4AutoDns1Override'
                }, {
                    name: 'v4AutoDns2Override'
                }, {
                    name: 'v4PPPoEUsername'
                }, {
                    name: 'v4PPPoEPassword'
                }, {
                    name: 'v4PPPoEUsePeerDns'
                }, {
                    name: 'v4PPPoEDns1'
                }, {
                    name: 'v4PPPoEDns2'
                }, {
                    name: 'v4NatEgressTraffic'
                }, {
                    name: 'v4NatIngressTraffic'
                }, {
                    name: 'v4Aliases'
                }, {
                    name: 'v6ConfigType'
                }, {
                    name: 'v6StaticAddress'
                }, {
                    name: 'v6StaticPrefixLength'
                }, {
                    name: 'v6StaticGateway'
                }, {
                    name: 'v6StaticDns1'
                }, {
                    name: 'v6StaticDns2'
                }, {
                    name: 'v6Aliases'
                }, {
                    name: 'raEnabled'
                }, {
                    name: 'dhcpEnabled'
                }, {
                    name: 'dhcpRangeStart'
                }, {
                    name: 'dhcpRangeEnd'
                }, {
                    name: 'dhcpLeaseDuration'
                }, {
                    name: 'dhcpGatewayOverride'
                }, {
                    name: 'dhcpPrefixOverride'
                }, {
                    name: 'dhcpDnsOverride'
                }, {
                    name: 'dhcpOptions'
                }, {
                    name: 'javaClass'
                }, 
                {
                    name: "deviceName" //from deviceStatus
                }, {
                    name: "macAddress" //from deviceStatus
                }, {
                    name: "connected" //from deviceStatus
                }, {
                    name: "duplex" //from deviceStatus
                }, {
                    name: "vendor" //from deviceStatus
                }, {
                    name: "mbit" //from deviceStatus
                }, 
                {
                    name: "v4Address" //from interfaceStatus
                }, {
                    name: "v4Netmask" //from interfaceStatus
                }, {
                    name: "v4Gateway" //from interfaceStatus
                }, {
                    name: "v4Dns1" //from interfaceStatus
                }, {
                    name: "v4Dns2" //from interfaceStatus
                }, {
                    name: "v4PrefixLength" //from interfaceStatus
                }],
                columns: [{
                    header: this.i18n._("Id"),
                    width: 30,
                    dataIndex: 'interfaceId'
                }, {
                    header: this.i18n._("Name"),
                    dataIndex: 'name',
                    width:120
                }, {
                    header: this.i18n._( "Connected" ),
                    dataIndex: 'connected',
                    sortable: false,
                    width: 120,
                    renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                        var divClass = "ua-cell-disabled";
                        var connectedStr = this.i18n._("unknown");
                        if ( value == "CONNECTED" ) {
                            connectedStr = this.i18n._("connected");
                            divClass = "ua-cell-enabled";
                        } else if ( value == "DISCONNECTED" ) {
                            connectedStr = this.i18n._("disconnected");
                        }
                        return "<div class='" + divClass + "'>" + connectedStr + "</div>";
                    }, this)
                }, {
                    header: this.i18n._("Device"),
                    dataIndex: 'physicalDev',
                    width:90,
                    renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                        if (record.get("isVlanInterface")) {
                            return record.get("systemDev");
                        }
                        return value;
                    }, this)
                },{
                    hidden: true,
                    header: this.i18n._("Physical Dev"),
                    dataIndex: 'physicalDev',
                    width:80
                }, {
                    hidden: true,
                    header: this.i18n._("System Dev"),
                    dataIndex: 'systemDev',
                    width:80
                }, {
                    hidden: true,
                    header: this.i18n._("Symbolic Dev"),
                    dataIndex: 'symbolicDev',
                    width:80
                }, {
                    header: this.i18n._("Config"),
                    dataIndex: 'configType',
                    width:100
                }, {
                    header: this.i18n._("Current Address"),
                    dataIndex: 'v4Address',
                    width:150,
                    renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                        if (Ext.isEmpty(value))
                            return "";
                        return value + "/" + record.data.v4PrefixLength;
                    }, this)
                }, {
                    header: this.i18n._("is WAN"),
                    dataIndex: 'isWan',
                    width:60,
                    renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                        // only ADDRESSED interfaces can be WANs
                        return (record.data.configType == 'ADDRESSED') ? value: ""; // if its addressed return value
                    }, this)
                    
                }, deleteVlanColumn],
                plugins: [deleteVlanColumn],
                bbar: ['-',{
                    xtype: "button",
                    name: "remap_interfaces",
                    iconCls: 'icon-drag',
                    text: this.i18n._("Remap Interfaces"),
                    handler: function() {
                        this.gridInterfaces.onMapDevices();
                    },
                    scope : this
                },'-',{
                    xtype: "button",
                    iconCls: 'icon-refresh',
                    text: this.i18n._("Refresh Device Status"),
                    handler: function() {
                        Ung.NetworkUtil.onRefreshDeviceStatus(this, this.gridInterfaces);
                    },
                    scope : this
                },'-',{
                    xtype: "button",
                    text : this.i18n._( "Add 802.1q Tagged Interface" ),
                    iconCls : "icon-add-row",
                    handler: function() {
                        this.gridInterfaces.addHandler();
                    },
                    scope : this
                }, '-', {
                    xtype: "button",
                    text : this.i18n._( "Test Connectivity" ),
                    handler : this.testConnectivity,
                    iconCls : "icon-test-connectivity",
                    scope : this
                },{
                    xtype: "button",
                    text : this.i18n._( "Ping Test" ),
                    iconCls : "icon-test-ping",
                    handler : this.openPingTest,
                    scope : this
                }],
                onMapDevices: Ext.bind(function() {
                    Ext.MessageBox.wait(this.i18n._("Loading device mapper..."), this.i18n._("Please wait"));
                    if (!this.winMapDevices) {
                        this.mapDevicesStore = Ext.create('Ext.data.ArrayStore', {
                            fields:[{name: "interfaceId"}, { name: "name" }, {name: "deviceName"}, { name: "physicalDev" }, { name: "systemDev" },{ name: "symbolicDev" }, { name: "macAddress" }, { name: "connected" }, { name: "duplex" }, { name: "vendor" }, { name: "mbit" }],
                            data: []
                        });
                        this.availableDevicesStore = Ext.create('Ext.data.ArrayStore', {
                            fields:[{ name: "physicalDev" }],
                            data: []
                        });

                        this.gridMapDevices = Ext.create('Ext.grid.Panel', {
                            flex: 1,
                            store: this.mapDevicesStore,
                            loadMask: true,
                            stripeRows: true,
                            enableColumnResize: true,
                            enableColumnHide: false,
                            enableColumnMove: false,
                            selModel: Ext.create('Ext.selection.RowModel', {singleSelect: true}),
                            plugins: [
                                Ext.create('Ext.grid.plugin.CellEditing', {
                                    clicksToEdit: 1
                                })
                            ],
                            bbar: [{
                                xtype: "button",
                                iconCls: 'icon-refresh',
                                text: this.i18n._("Refresh Device Status"),
                                handler: function() {
                                    Ung.NetworkUtil.onRefreshDeviceStatus(this, this.gridMapDevices);
                                },
                                scope : this
                            }],
                            viewConfig:{
                                forceFit: true,
                                disableSelection: false,
                                plugins:{
                                    ptype: 'gridviewdragdrop',
                                    dragText: this.i18n._('Drag and drop to reorganize')
                                },
                                listeners: {
                                    "drop": {
                                        fn:  Ext.bind(function(node, data, overModel, dropPosition, eOpts) {
                                            var i = 0;
                                            this.mapDevicesStore.each( Ext.bind(function( currentRow ) {
                                                var intf=this.currentInterfaces[i];
                                                currentRow.set({
                                                    "interfaceId": intf.interfaceId,
                                                    "name": intf.name
                                                });
                                                i++;
                                            }, this));
                                            return true;
                                        },this )
                                    }
                                }
                            },
                            columns: [{
                                header: this.i18n._("Name"),
                                dataIndex: 'name',
                                sortable: false,
                                width: 80,
                                renderer: function( value ) {
                                    return i18n._( value );
                                }
                            }, {
                                xtype: 'templatecolumn',
                                menuDisabled: true,
                                resizable: false,
                                width: 40,
                                tpl: '<img src="'+Ext.BLANK_IMAGE_URL+'" class="icon-drag"/>' 
                            }, {
                                header: this.i18n._( "Device" ),
                                tooltip: this.i18n._( "Click on a Device to open a combo and choose the desired Device from a list. When anoter Device is selected the 2 Devices are swithced." ),
                                dataIndex: 'deviceName',
                                sortable: false,
                                width: 200,
                                tdCls: 'ua-pointer',
                                editor:{
                                    xtype: 'combo',
                                    store: this.availableDevicesStore,
                                    valueField: 'physicalDev',
                                    displayField: 'physicalDev',
                                    queryMode: 'local',
                                    editable: false,
                                    listeners: {
                                        "change": {
                                            fn: Ext.bind(function(elem, newValue, oldValue) {
                                                var sourceRecord = null;
                                                var targetRecord = null;
                                                this.mapDevicesStore.each( function( currentRow ) {
                                                    if(oldValue==currentRow.get( "physicalDev" )) {
                                                        sourceRecord=currentRow;
                                                    } else if(newValue==currentRow.get( "physicalDev" )) {
                                                        targetRecord=currentRow;
                                                    }
                                                    if(sourceRecord !== null && targetRecord !== null) {
                                                        return false;
                                                    }
                                                    return true;
                                                });
                                                if(sourceRecord === null || targetRecord === null || sourceRecord === targetRecord) {
                                                    console.log(oldValue, newValue, sourceRecord, targetRecord);
                                                    return false;
                                                }
                                                var soruceData = Ext.decode(Ext.encode(sourceRecord.data));
                                                var targetData = Ext.decode(Ext.encode(targetRecord.data));
                                                soruceData.deviceName=oldValue;
                                                targetData.deviceName=newValue;
                                                
                                                sourceRecord.set({
                                                    "physicalDev": targetData.physicalDev,
                                                    "systemDev": targetData.systemDev,
                                                    "symbolicDev": targetData.symbolicDev,
                                                    "macAddress": targetData.macAddress,
                                                    "duplex": targetData.duplex,
                                                    "vendor": targetData.vendor,
                                                    "mbit": targetData.mbit,
                                                    "connected": targetData.connected
                                                });
                                                
                                                targetRecord.set({
                                                    "deviceName": soruceData.deviceName,
                                                    "physicalDev": soruceData.physicalDev,
                                                    "systemDev": soruceData.systemDev,
                                                    "symbolicDev": soruceData.symbolicDev,
                                                    "macAddress": soruceData.macAddress,
                                                    "duplex": soruceData.duplex,
                                                    "vendor": soruceData.vendor,
                                                    "mbit": soruceData.mbit,
                                                    "connected": soruceData.connected
                                                });
                                                return true;
                                            }, this)
                                        }
                                    }
                                }
                            }, {
                                header: this.i18n._( "Connected" ),
                                dataIndex: 'connected',
                                sortable: false,
                                width: 120,
                                tdCls: 'ua-draggable',
                                renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                                    var divClass = "ua-cell-disabled";
                                    var connectedStr = this.i18n._("unknown");
                                    if ( value == "CONNECTED" ) {
                                        connectedStr = this.i18n._("connected");
                                        divClass = "ua-cell-enabled";
                                    } else if ( value == "DISCONNECTED" ) {
                                        connectedStr = this.i18n._("disconnected");
                                    }
                                    return "<div class='" + divClass + "'>" + connectedStr + "</div>";
                                }, this)
                            }, {
                                header: this.i18n._( "Speed" ),
                                dataIndex: 'mbit',
                                sortable: false,
                                tdCls: 'ua-draggable',
                                width: 100
                            }, {
                                header: this.i18n._( "Duplex" ),
                                dataIndex: 'duplex',
                                sortable: false,
                                tdCls: 'ua-draggable',
                                width: 100,
                                renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                                    return (value=="FULL_DUPLEX")?this.i18n._("full-duplex") : (value=="HALF_DUPLEX") ? this.i18n._("half-duplex") : this.i18n._("unknown");
                                }, this)
                            }, {
                                header: this.i18n._( "Vendor" ),
                                dataIndex: 'vendor',
                                sortable: false,
                                tdCls: 'ua-draggable',
                                flex: 1
                            }, {
                                header: this.i18n._( "MAC Address" ),
                                dataIndex: 'macAddress',
                                sortable: false,
                                width: 150,
                                renderer: function(value, metadata, record, rowIndex, colIndex, store, view) {
                                    var text = "";
                                    if ( value && value.length > 0 ) {
                                        // Build the link for the mac address
                                        text = '<a target="_blank" href="http://standards.ieee.org/cgi-bin/ouisearch?' + 
                                        value.substring( 0, 8 ).replace( /:/g, "" ) + '">' + value + '</a>';
                                    }
                                    return text; 
                                }
                            }]
                        });
                        
                        this.winMapDevices = Ext.create('Ung.EditWindow', {
                            breadcrumbs: [{
                                title: this.i18n._("Interfaces"),
                                action: Ext.bind(function() {
                                    this.winMapDevices.cancelAction();
                                }, this)
                            }, {
                                title: this.i18n._("Remap Interfaces")
                            }],
                            items: [{
                                xtype: 'panel',
                                layout: { type: 'vbox', align: 'stretch' },
                                items: [{
                                    xtype: 'fieldset',
                                    flex: 0,
                                    margin: '10 0 0 0',
                                    cls: 'description',
                                    title: this.i18n._("How to map Devices with Interfaces"),
                                    html: this.i18n._("<b>Method 1:</b> <b>Drag and Drop</b> the Device to the desired Interface<br/><b>Method 2:</b> <b>Click on a Device</b> to open a combo and choose the desired Device from a list. When anoter Device is selected the 2 Devices are swithced.")
                                }, this.gridMapDevices]
                            }],
                            updateAction: Ext.bind(function() {
                                var interfaceDataMap = {};
                                this.mapDevicesStore.each( function( currentRow ) {
                                    interfaceDataMap[currentRow.get( "interfaceId" )] = currentRow.getData();
                                });
                                this.gridInterfaces.getStore().each(function( currentRow ) {
                                    var interfaceData = interfaceDataMap[currentRow.get("interfaceId")];
                                    if(interfaceData) {
                                        currentRow.set({
                                            "deviceName": interfaceData.deviceName,
                                            "physicalDev": interfaceData.physicalDev,
                                            "systemDev": interfaceData.systemDev,
                                            "symbolicDev": interfaceData.symbolicDev,
                                            "macAddress": interfaceData.macAddress,
                                            "connected": interfaceData.connected,
                                            "duplex": interfaceData.duplex,
                                            "vendor": interfaceData.vendor,
                                            "mbit": interfaceData.mbit
                                        });
                                    }
                                });
                                this.winMapDevices.cancelAction();
                            }, this)
                        });
                    }
                    Ext.MessageBox.hide();
                    var allInterfaces=this.gridInterfaces.getPageList();
                    this.currentInterfaces = [];
                    for(var i=0; i<allInterfaces.length; i++) {
                        if(!allInterfaces[i].isVlanInterface) {
                            this.currentInterfaces.push(allInterfaces[i]);
                        }
                    }
                    this.mapDevicesStore.loadData( this.currentInterfaces );
                    this.availableDevicesStore.loadData( this.currentInterfaces );
                    this.winMapDevices.show();
                }, this)
            });

            this.gridInterfaces.getStore().on("update", Ext.bind(function( store, record, operation, modifiedFieldNames, eOpts) {
                //Sync QoS Bandwith Grid data
                var interfaces = this.gridInterfaces.getPageList();
                var interfacesMap=Ung.Util.createRecordsMap(interfaces, "interfaceId");
                var qosBandwidthStore=this.gridQosWanBandwidth.getStore();
                qosBandwidthStore.clearFilter();
                qosBandwidthStore.each( function( currentRow ) {
                    var interfaceData = interfacesMap[currentRow.get("interfaceId")];
                    if(interfaceData) {
                        currentRow.set({
                            "isWan": interfaceData.isWan,
                            "name": interfaceData.name,
                            "configType": interfaceData.configType
                        });
                    }
                });
                qosBandwidthStore.filter([{property: "configType", value: "ADDRESSED"}, {property:"isWan", value: true},{property: "isVlanInterface", value: false}]);
                this.gridQosWanBandwidth.updateTotalBandwidth();
            }, this));
            
            this.panelInterfaces = Ext.create('Ext.panel.Panel',{
                name: 'panelInterfaces',
                helpSource: 'network_interfaces',
                parentId: this.getId(),
                title: this.i18n._('Interfaces'),
                layout: { type: 'vbox', pack: 'start', align: 'stretch' },
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    flex: 0,
                    cls: 'description',
                    title: this.i18n._("Interface configuration"),
                    html: this.i18n._("Use this page to configure each interface's configuration and its mapping to a physical network card.")
                }, this.gridInterfaces]
            });
            this.gridInterfacesV4AliasesEditor = Ext.create('Ung.EditorGrid',{
                name: 'IPv4 Aliases',
                height: 180,
                width: 450,
                settingsCmp: this,
                paginated: false,
                hasEdit: false,
                dataIndex: 'v4Aliases',
                recordJavaClass: "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias",
                columnsDefaultSortable: false,
                data: [],
                emptyRow: {
                    "staticAddress": "1.2.3.4",
                    "staticPrefix": "24",
                    "javaClass": "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias"
                },
                fields: [{
                    name: 'staticAddress'
                }, {
                    name: 'staticPrefix'
                }],
                columns: [{
                    header: this.i18n._("Address"),
                    dataIndex: 'staticAddress',
                    width:200,
                    editor : {
                        xtype: 'textfield',
                        vtype: 'ip4Address',
                        allowBlank: false
                    }
                }, {
                    header: this.i18n._("Netmask / Prefix"),
                    dataIndex: 'staticPrefix',
                    flex: 1,
                    editor : {
                        xtype: 'textfield',
                        allowBlank: false
                    }
                }],
                setValue: function (value) {
                    var data = [];
                    if(value !== null && value.list !== null) {
                        data=value.list;
                    }
                    this.reload({data:data});
                },
                getValue: function () {
                    return {
                        javaClass: "java.util.LinkedList",
                        list: this.getPageList()
                    };
                }
            });
            this.gridInterfacesV6AliasesEditor = Ext.create('Ung.EditorGrid',{
                name: 'IPv6 Aliases',
                height: 180,
                width: 450,
                settingsCmp: this,
                paginated: false,
                hasEdit: false,
                dataIndex: 'v6Aliases',
                recordJavaClass: "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias",
                columnsDefaultSortable: false,
                data: [],
                emptyRow: {
                    "staticAddress": "::1",
                    "staticPrefix": "64",
                    "javaClass": "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias"
                },
                fields: [{
                    name: 'staticAddress'
                }, {
                    name: 'staticPrefix'
                }],
                columns: [{
                    header: this.i18n._("Address"),
                    dataIndex: 'staticAddress',
                    width:200,
                    editor : {
                        xtype: 'textfield',
                        vtype: 'ip6Address',
                        allowBlank: false
                    }
                }, {
                    header: this.i18n._("Netmask / Prefix"),
                    dataIndex: 'staticPrefix',
                    flex: 1,
                    editor : {
                        xtype: 'textfield',
                        allowBlank: false
                    }
                }],
                setValue: function (value) {
                    var data = [];
                    if(value!==null && value.list!==null) {
                        data=value.list;
                    }
                    this.reload({data:data});
                },
                getValue: function () {
                    return {
                        javaClass: "java.util.LinkedList",
                        list: this.getPageList()
                    };
                }
            });
            this.gridInterfacesDhcpOptionsEditor = Ext.create('Ung.EditorGrid',{
                name: 'DHCP Options',
                height: 180,
                width: 450,
                settingsCmp: this,
                paginated: false,
                hasEdit: false,
                dataIndex: 'dhcpOptions',
                recordJavaClass: "com.untangle.uvm.network.DhcpOption",
                columnsDefaultSortable: false,
                data: [],
                emptyRow: {
                    "enabled": true,
                    "value": "66,1.2.3.4",
                    "description": "[no description]",
                    "javaClass": "com.untangle.uvm.network.DhcpOption"
                },
                fields: [{
                    name: 'enabled'
                }, {
                    name: 'value'
                }, {
                    name: 'description'
                }],
                columns: [{
                    xtype:'checkcolumn',
                    header: this.i18n._("Enable"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }, {
                    header: this.i18n._("Value"),
                    dataIndex: 'value',
                    width:200,
                    editor : {
                        xtype: 'textfield',
                        allowBlank: false
                    }
                }],
                setValue: function (value) {
                    var data = [];
                    if( value && value.list ) {
                        data=value.list;
                    }
                    this.reload({data:data});
                },
                getValue: function () {
                    return {
                        javaClass: "java.util.LinkedList",
                        list: this.getPageList()
                    };
                }
            });
            this.gridInterfaces.setRowEditor( Ext.create('Ung.RowEditorWindow',{
                sizeToComponent: this.panelInterfaces,
                title: this.i18n._('Edit Interface'),
                inputLines: [{
                    xtype:'textfield',
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Interface Name"),
                    allowBlank: false,
                    width: 300
                }, {
                    xtype:'checkbox',
                    dataIndex: "isVlanInterface",
                    fieldLabel: this.i18n._("Is VLAN (802.1q) Interface"),
                    disabled: true,
                    width: 300
                }, {
                    xtype: "combo",
                    allowBlank: false,
                    dataIndex: "vlanParent",
                    fieldLabel: this.i18n._("Parent Interface"),
                    store: Ung.Util.getInterfaceAddressedList(),
                    width: 300,
                    queryMode: 'local',
                    editable: false
                }, {
                    xtype: "numberfield",
                    dataIndex: "vlanTag",
                    fieldLabel: this.i18n._("802.1q Tag"),
                    minValue: 1,
                    maxValue: 4096,
                    allowBlank: false,
                    blankText: this.i18n._("802.1q Tag must be a valid integer."),
                    width: 300
                }, {
                    xtype: "combo",
                    allowBlank: false,
                    dataIndex: "configType",
                    fieldLabel: this.i18n._("Config Type"),
                    editable: false,
                    store: [["ADDRESSED", this.i18n._('Addressed')], ["BRIDGED", this.i18n._('Bridged')], ["DISABLED", this.i18n._('Disabled')]],
                    width: 300,
                    queryMode: 'local',
                    listeners: {
                        "select": {
                            fn: Ext.bind(function(combo, records, eOpts) {
                                this.gridInterfaces.rowEditor.syncComponents();
                            }, this)
                        }
                    }
                }, {
                    xtype:'checkbox',
                    dataIndex: "isWan",
                    fieldLabel: this.i18n._("Is WAN Interface"),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.gridInterfaces.rowEditor.syncComponents();
                                if ( newValue ) {
                                    var v4NatEgressTraffic = this.gridInterfaces.rowEditor.query('checkbox[dataIndex="v4NatEgressTraffic"]')[0];
                                    // auto-enable egress NAT when checking isWan
                                    v4NatEgressTraffic.setValue( true );
                                } 
                            }, this)
                        }
                    }
                }, {
                    xtype:'fieldset',
                    name: 'v4Config',
                    border: true,
                    title: this.i18n._("IPv4 Configuration"),
                    collapsible: true,
                    collapsed: false,
                    items: [{
                        xtype: 'fieldset',
                        defaults: {
                            labelWidth: 150,
                            width: 350
                        },
                        items:[{
                            xtype: "combo",
                            dataIndex: "v4ConfigType",
                            fieldLabel: this.i18n._("Config Type"),
                            editable: false,
                            store: [ ["AUTO", this.i18n._('Auto (DHCP)')], ["STATIC", this.i18n._('Static')],  ["PPPOE", this.i18n._('PPPoE')]],
                            queryMode: 'local',
                            listeners: {
                                "select": {
                                    fn: Ext.bind(function(combo, records, eOpts) {
                                        this.gridInterfaces.rowEditor.syncComponents();
                                    }, this)
                                }
                            }
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4StaticAddress",
                            fieldLabel: this.i18n._("Address"),
                            allowBlank: false, 
                            vtype: "ip4Address"
                        }, {
                            xtype: "combo",
                            dataIndex: "v4StaticPrefix",
                            fieldLabel: this.i18n._( "Netmask" ),
                            store: Ung.Util.getV4NetmaskList( false ),
                            queryMode: 'local',
                            allowBlank: false, 
                            editable: false
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4StaticGateway",
                            fieldLabel: this.i18n._("Gateway"),
                            allowBlank: false, 
                            vtype: "ip4Address"
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4StaticDns1",
                            fieldLabel: this.i18n._("Primary DNS"),
                            allowBlank: false, 
                            vtype: "ip4Address"
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4StaticDns2",
                            fieldLabel: this.i18n._("Secondary DNS"),
                            vtype: "ip4Address"
                        }, {
                            xtype: 'container',
                            layout: 'column',
                            name: "v4AutoAddressOverrideContainer",
                            margin: '0 0 5 0',
                            width: null,
                            items: [{
                                xtype:'textfield',
                                dataIndex: "v4AutoAddressOverride",
                                fieldLabel: this.i18n._("Address Override"),
                                vtype: "ip4Address",
                                labelWidth: 150,
                                width: 350
                            }, {
                                xtype: 'label',
                                dataIndex: "v4Address",
                                setValue: function(value) {
                                    this.dataValue=value;
                                    this.setText(Ext.isEmpty(value)?"":Ext.String.format(settingsCmp.i18n._("Current: {0}"), value));
                                },
                                getValue: function() {
                                    return this.dataValue;
                                },
                                html: "",
                                cls: 'boxlabel'
                            }]
                        },{
                            xtype: 'container',
                            layout: 'column',
                            name: "v4AutoPrefixOverrideContainer",
                            margin: '0 0 5 0',
                            width: null,
                            items: [{
                                xtype: "combo",
                                dataIndex: "v4AutoPrefixOverride",
                                fieldLabel: this.i18n._("Netmask Override"),
                                store: Ung.Util.getV4NetmaskList( true ),
                                queryMode: 'local',
                                editable: false,
                                labelWidth: 150,
                                width: 350

                            }, {
                                xtype: 'label',
                                dataIndex: "v4PrefixLength",
                                setValue: function(value, record) {
                                    this.dataValue=value;
                                    this.setText(Ext.isEmpty(value)?"":Ext.String.format(settingsCmp.i18n._("Current: /{0} - {1}"), value, record.get("v4Netmask")));
                                },
                                getValue: function() {
                                    return this.dataValue;
                                },
                                html: "",
                                cls: 'boxlabel'
                            }]
                        }, {
                            xtype: 'container',
                            layout: 'column',
                            name: "v4AutoGatewayOverrideContainer",
                            margin: '0 0 5 0',
                            width: null,
                            items: [{
                                xtype:'textfield',
                                dataIndex: "v4AutoGatewayOverride",
                                fieldLabel: this.i18n._("Gateway Override"),
                                vtype: "ip4Address",
                                labelWidth: 150,
                                width: 350
                            }, {
                                xtype: 'label',
                                dataIndex: "v4Gateway",
                                setValue: function(value) {
                                    this.dataValue=value;
                                    this.setText(Ext.isEmpty(value)?"":Ext.String.format(settingsCmp.i18n._("Current: {0}"), value));
                                },
                                getValue: function() {
                                    return this.dataValue;
                                },
                                html: "",
                                cls: 'boxlabel'
                            }]
                        }, {
                            xtype: 'container',
                            layout: 'column',
                            name: "v4AutoDns1OverrideContainer",
                            margin: '0 0 5 0',
                            width: null,
                            items: [{
                                xtype:'textfield',
                                dataIndex: "v4AutoDns1Override",
                                fieldLabel: this.i18n._("Primary DNS Override"),
                                vtype: "ip4Address",
                                labelWidth: 150,
                                width: 350
                            }, {
                                xtype: 'label',
                                dataIndex: "v4Dns1",
                                setValue: function(value) {
                                    this.dataValue=value;
                                    this.setText(Ext.isEmpty(value)?"":Ext.String.format(settingsCmp.i18n._("Current: {0}"), value));
                                },
                                getValue: function() {
                                    return this.dataValue;
                                },
                                html: "",
                                cls: 'boxlabel'
                            }]
                        }, {
                            xtype: 'container',
                            layout: 'column',
                            name: "v4AutoDns2OverrideContainer",
                            margin: '0 0 5 0',
                            width: null,
                            items: [{
                                xtype:'textfield',
                                dataIndex: "v4AutoDns2Override",
                                fieldLabel: this.i18n._("Secondary DNS Override"),
                                vtype: "ip4Address",
                                labelWidth: 150,
                                width: 350
                            }, {
                                xtype: 'label',
                                dataIndex: "v4Dns2",
                                setValue: function(value) {
                                    this.dataValue=value;
                                    this.setText(Ext.isEmpty(value)?"":Ext.String.format(settingsCmp.i18n._("Current: {0}"), value));
                                },
                                getValue: function() {
                                    return this.dataValue;
                                },
                                html: "",
                                cls: 'boxlabel'
                            }]
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4PPPoEUsername",
                            fieldLabel: this.i18n._("Username"),
                            width: 350
                        }, {
                            xtype:'textfield',
                            inputType:'password',
                            dataIndex: "v4PPPoEPassword",
                            fieldLabel: this.i18n._("Password"),
                            width: 350
                        }, {
                            xtype:'checkbox',
                            dataIndex: "v4PPPoEUsePeerDns",
                            fieldLabel: this.i18n._("Use Peer DNS"),
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.gridInterfaces.rowEditor.syncComponents();
                                    }, this)
                                }
                            }
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4PPPoEDns1",
                            fieldLabel: this.i18n._("Primary DNS"),
                            vtype: "ip4Address",
                            width: 350
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4PPPoEDns2",
                            fieldLabel: this.i18n._("Secondary DNS"),
                            vtype: "ip4Address",
                            width: 350
                        }]
                    }, {
                        xtype: 'fieldset',
                        title: this.i18n._("IPv4 Aliases"),
                        name: "v4AliasesContainer",
                        items: [this.gridInterfacesV4AliasesEditor]
                    }, {
                        xtype: 'fieldset',
                        title: this.i18n._("IPv4 Options"),
                        name: "v4OptionsContainer",
                        items: [{
                            xtype:'checkbox',
                            dataIndex: "v4NatEgressTraffic",
                            boxLabel: this.i18n._("NAT traffic exiting this interface (and bridged peers)")
                        }, {
                            xtype:'checkbox',
                            dataIndex: "v4NatIngressTraffic",
                            boxLabel: this.i18n._("NAT traffic coming from this interface (and bridged peers)")
                        }]
                    }]
                }, {
                    xtype: 'fieldset',
                    name: 'v6Config',
                    border: true,
                    title: this.i18n._("IPv6 Configuration"),
                    collapsible: true,
                    collapsed: false,
                    defaults: {
                        labelWidth: 150
                    },
                    items: [{
                        xtype: "combo",
                        dataIndex: "v6ConfigType",
                        allowBlank: false,
                        fieldLabel: this.i18n._("Config Type"),
                        editable: false,
                        store: [ ["DISABLED", this.i18n._('Disabled')], ["AUTO", this.i18n._('Auto (SLAAC/RA)')], ["STATIC", this.i18n._('Static')] ],
                        queryMode: 'local',
                        width: 350,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.gridInterfaces.rowEditor.syncComponents();
                                }, this)
                            }
                        }
                    }, {
                        xtype:'textfield',
                        dataIndex: "v6StaticAddress",
                        fieldLabel: this.i18n._("Address"),
                        vtype: "ip6Address",
                        width: 450
                    }, {
                        xtype:'textfield',
                        dataIndex: "v6StaticPrefixLength",
                        fieldLabel: this.i18n._("Prefix Length"),
                        width: 200
                    }, {
                        xtype:'textfield',
                        dataIndex: "v6StaticGateway",
                        fieldLabel: this.i18n._("Gateway"),
                        vtype: "ip6Address",
                        width: 350
                    }, {
                        xtype:'textfield',
                        dataIndex: "v6StaticDns1",
                        fieldLabel: this.i18n._("Primary DNS"),
                        vtype: "ip6Address",
                        width: 350
                    }, {
                        xtype:'textfield',
                        dataIndex: "v6StaticDns2",
                        fieldLabel: this.i18n._("Secondary DNS"),
                        vtype: "ip6Address",
                        width: 350
                    }, {
                        xtype: 'fieldset',
                        title: this.i18n._("IPv6 Aliases"),
                        name: "v6AliasesContainer",
                        items: [this.gridInterfacesV6AliasesEditor]
                    }, {
                        xtype: 'fieldset',
                        title: this.i18n._("IPv6 Options"),
                        name: "v6OptionsContainer",
                        items: [{
                            xtype:'checkbox',
                            dataIndex: "raEnabled",
                            boxLabel: this.i18n._("Send Router Advertisements"),
                            labelWidth: 150,
                            width: 350
                        },{
                            xtype: 'label',
                            name: "v6RouterAdvertisementWarning",
                            html: "<font color=\"red\">" + this.i18n._("Warning: ") + "</font>" + this.i18n._("SLAAC only works with /64 subnets."),
                            cls: 'boxlabel'
                        }]
                    }]
                }, {
                    xtype: 'fieldset',
                    name: 'dhcp',
                    border: true,
                    title: this.i18n._("DHCP Configuration"),
                    collapsible: true,
                    collapsed: false,
                    defaults: {
                        labelWidth: 150
                    },
                    items: [{
                        xtype:'checkbox',
                        dataIndex: "dhcpEnabled",
                        boxLabel: this.i18n._("Enable DHCP Serving")
                    }, {
                        xtype: 'textfield',
                        dataIndex: "dhcpRangeStart",
                        fieldLabel: this.i18n._("Range Start"),
                        vtype: "ip4Address",
                        width: 350
                    }, {
                        xtype:'textfield',
                        dataIndex: "dhcpRangeEnd",
                        fieldLabel: this.i18n._("Range End"),
                        vtype: "ip4Address",
                        width: 350
                    }, {
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: 'textfield',
                            dataIndex: "dhcpLeaseDuration",
                            fieldLabel: this.i18n._("Lease Duration"),
                            labelWidth: 150,
                            width: 350
                        }, {
                            xtype: 'label',
                            html: this.i18n._("(seconds)"),
                            cls: 'boxlabel'
                        }]
                    }, {
                        xtype:'fieldset',
                        border: true,
                        title: this.i18n._("DHCP Advanced"),
                        collapsible: true,
                        collapsed: true,
                        defaults: {
                            labelWidth: 150
                        },
                        items: [{
                            xtype:'textfield',
                            dataIndex: "dhcpGatewayOverride",
                            fieldLabel: this.i18n._("Gateway Override"),
                            vtype: "ip4Address",
                            width: 350
                        }, {
                            xtype: "combo",
                            dataIndex: "dhcpPrefixOverride",
                            fieldLabel: this.i18n._("Netmask Override"),
                            store: Ung.Util.getV4NetmaskList( true ),
                            queryMode: 'local',
                            editable: false,
                            width: 350
                        }, {
                            xtype:'textfield',
                            dataIndex: "dhcpDnsOverride",
                            fieldLabel: this.i18n._("DNS Override"),
                            vtype: "ip4Address",
                            width: 350
                        }, {
                            xtype: 'fieldset',
                            title: this.i18n._("DHCP Options"),
                            name: "dhcpOptionsContainer",
                            items: [this.gridInterfacesDhcpOptionsEditor]
                        }]
                    }]
                }, {
                    xtype: "combo",
                    allowBlank: false,
                    dataIndex: "bridgedTo",
                    fieldLabel: this.i18n._("Bridged To"),
                    store: Ung.Util.getInterfaceAddressedList(),
                    width: 300,
                    queryMode: 'local',
                    editable: false
                }],
                syncComponents: function() {
                    var configTypeValue = this.query('combo[dataIndex="configType"]')[0].getValue();
                    var isVlanInterface = this.query('checkbox[dataIndex="isVlanInterface"]')[0];
                    var isVlanInterfaceValue = isVlanInterface.getValue();
                    var isWan = this.query('checkbox[dataIndex="isWan"]')[0];
                    var isWanValue = isWan.getValue();
                    var vlanTag = this.query('numberfield[dataIndex="vlanTag"]')[0];
                    var vlanParent = this.query('combo[dataIndex="vlanParent"]')[0];
                    var bridgedTo = this.query('combo[dataIndex="bridgedTo"]')[0];
                    var v4Config = this.query('fieldset[name="v4Config"]')[0];
                    var v6Config = this.query('fieldset[name="v6Config"]')[0];
                    var dhcp = this.query('fieldset[name="dhcp"]')[0];
                    var v4ConfigType = this.query('combo[dataIndex="v4ConfigType"]')[0];
                    var v6ConfigType = this.query('combo[dataIndex="v6ConfigType"]')[0];

                    var v4StaticAddress = this.query('textfield[dataIndex="v4StaticAddress"]')[0];
                    var v4StaticPrefix = this.query('combo[dataIndex="v4StaticPrefix"]')[0];
                    var v4StaticGateway = this.query('textfield[dataIndex="v4StaticGateway"]')[0];
                    var v4StaticDns1 = this.query('textfield[dataIndex="v4StaticDns1"]')[0];
                    var v4StaticDns2 = this.query('textfield[dataIndex="v4StaticDns2"]')[0];
                    
                    var v4AutoAddressOverrideContainer = this.query('container[name="v4AutoAddressOverrideContainer"]')[0];
                    var v4AutoGatewayOverrideContainer = this.query('container[name="v4AutoGatewayOverrideContainer"]')[0];
                    var v4AutoPrefixOverrideContainer = this.query('container[name="v4AutoPrefixOverrideContainer"]')[0];
                    var v4AutoDns1OverrideContainer = this.query('container[name="v4AutoDns1OverrideContainer"]')[0];
                    var v4AutoDns2OverrideContainer = this.query('container[name="v4AutoDns2OverrideContainer"]')[0];
                    
                    var v4PPPoEUsername = this.query('textfield[dataIndex="v4PPPoEUsername"]')[0];
                    var v4PPPoEPassword = this.query('textfield[dataIndex="v4PPPoEPassword"]')[0];
                    var v4PPPoEUsePeerDns = this.query('checkbox[dataIndex="v4PPPoEUsePeerDns"]')[0];
                    var v4PPPoEDns1 = this.query('textfield[dataIndex="v4PPPoEDns1"]')[0];
                    var v4PPPoEDns2 = this.query('textfield[dataIndex="v4PPPoEDns2"]')[0];

                    var v4NatEgressTraffic = this.query('checkbox[dataIndex="v4NatEgressTraffic"]')[0];
                    var v4NatIngressTraffic = this.query('checkbox[dataIndex="v4NatIngressTraffic"]')[0];
                    
                    var v6StaticAddress = this.query('textfield[dataIndex="v6StaticAddress"]')[0];
                    var v6StaticPrefixLength = this.query('textfield[dataIndex="v6StaticPrefixLength"]')[0];
                    var v6StaticGateway = this.query('textfield[dataIndex="v6StaticGateway"]')[0];
                    var v6StaticDns1 = this.query('textfield[dataIndex="v6StaticDns1"]')[0];
                    var v6StaticDns2 = this.query('textfield[dataIndex="v6StaticDns2"]')[0];
                    var v6AliasesContainer = this.query('container[name="v6AliasesContainer"]')[0];
                    var v6OptionsContainer = this.query('container[name="v6OptionsContainer"]')[0];
                    var v6SendRouterAdvertisements = this.query('checkbox[dataIndex="raEnabled"]')[0];
                    var v6SendRouterAdvertisementsWarning = this.query('label[name="v6RouterAdvertisementWarning"]')[0];
                    
                    // hide everything
                    isWan.hide();
                    isVlanInterface.hide();
                    vlanParent.hide();
                    vlanParent.disable();
                    
                    vlanTag.hide();
                    vlanTag.disable();
                    
                    bridgedTo.hide();
                    bridgedTo.disable();
                    v4Config.hide();
                    v6Config.hide();
                    dhcp.hide();
                    
                    v4ConfigType.hide();
                    
                    v6ConfigType.hide();
                    v6ConfigType.disable();
                    
                    v4StaticAddress.hide();
                    v4StaticAddress.disable();
                    
                    v4StaticPrefix.hide();
                    v4StaticPrefix.disable();
                    
                    v4StaticGateway.hide();
                    v4StaticGateway.disable();
                    v4StaticDns1.hide();
                    v4StaticDns1.disable();
                    
                    v4StaticDns2.hide();
                    v4AutoAddressOverrideContainer.hide();
                    v4AutoPrefixOverrideContainer.hide();
                    v4AutoGatewayOverrideContainer.hide();
                    v4AutoDns1OverrideContainer.hide();
                    v4AutoDns2OverrideContainer.hide();
                    v4PPPoEUsername.hide();
                    v4PPPoEPassword.hide();
                    v4PPPoEUsePeerDns.hide();
                    v4PPPoEDns1.hide();
                    v4PPPoEDns2.hide();
                    v4NatEgressTraffic.hide(); 
                    v4NatIngressTraffic.hide(); 

                    v6StaticAddress.hide();
                    v6StaticAddress.disable();
                    v6StaticPrefixLength.hide();
                    v6StaticPrefixLength.disable();
                    v6StaticGateway.hide();
                    v6StaticGateway.disable();
                    v6StaticDns1.hide();
                    v6StaticDns2.hide();
                    v6AliasesContainer.hide();
                    v6OptionsContainer.hide();
                    v6SendRouterAdvertisements.hide();
                    v6SendRouterAdvertisementsWarning.hide();
                    
                    if ( isVlanInterfaceValue ) {
                        isVlanInterface.show();
                        vlanParent.show();
                        vlanParent.enable();
                        vlanTag.show();
                        vlanTag.enable();
                    }

                    // if config disabled show nothing
                    if ( configTypeValue == "DISABLED") {
                        return;
                    }

                    // if config bridged just show the one field 
                    if ( configTypeValue == "BRIDGED") {
                        bridgedTo.show();
                        bridgedTo.enable();
                        return;
                    }

                    // if config addressed show necessary options
                    if ( configTypeValue == "ADDRESSED") {
                        if(!isVlanInterfaceValue) { // VLAN cannot be a WAN 
                            isWan.show();
                        }
                        v4Config.show();
                        v6Config.show();

                        // if not a WAN, must configure statically
                        // if a WAN, can use auto or static
                        if ( isWanValue ) {
                            v4ConfigType.show(); //show full config options for WANs
                            v6ConfigType.show(); //show full config options for WANs
                            v6ConfigType.enable(); 
                            v4NatEgressTraffic.show(); // show NAT egress option on WANs
                        } else {
                            v4ConfigType.setValue("STATIC"); //don't allow auto/pppoe for non-WAN
                            
                            v6ConfigType.setValue("STATIC");  
                            v6ConfigType.enable(); //don't allow auto/pppoe for non-WAN
                            
                            v4StaticGateway.hide(); // no gateways for non-WAN
                            v4StaticGateway.disable(); 
                            
                            v6StaticGateway.hide(); // no gateways for non-WAN
                            v6StaticGateway.disable();
                            
                            v4NatIngressTraffic.show(); // show NAT ingress options on non-WANs
                            dhcp.show(); // show DHCP options on non-WANs
                        }
                        
                        // if static show static fields
                        // if auto show override fields (auto is only allowed on WANs)
                        // if pppoe show pppoe fields (pppoe is only allowed on WANs)
                        if ( v4ConfigType.getValue() == "STATIC" ) {
                            v4StaticAddress.show();
                            v4StaticAddress.enable();
                            
                            v4StaticPrefix.show();
                            v4StaticPrefix.enable();
                            
                            if (isWanValue) {
                                v4StaticGateway.show();
                                v4StaticGateway.enable();
                                v4StaticDns1.show();
                                v4StaticDns1.enable();
                                v4StaticDns2.show();
                            }
                        } else if ( v4ConfigType.getValue() == "AUTO" ) {
                            v4AutoAddressOverrideContainer.show();
                            v4AutoPrefixOverrideContainer.show();
                            v4AutoGatewayOverrideContainer.show();
                            v4AutoDns1OverrideContainer.show();
                            v4AutoDns2OverrideContainer.show();
                        } else if ( v4ConfigType.getValue() == "PPPOE" ) {
                            v4PPPoEUsername.show();
                            v4PPPoEPassword.show();
                            v4PPPoEUsePeerDns.show();
                            if ( !v4PPPoEUsePeerDns.getValue()) {
                                v4PPPoEDns1.show();
                                v4PPPoEDns2.show();
                            }
                        }

                        // if static show static fields
                        // if auto show override fields
                        if ( v6ConfigType.getValue() == "STATIC" ) {
                            v6AliasesContainer.show();
                            v6StaticAddress.show();
                            v6StaticAddress.enable();
                            v6StaticPrefixLength.show();
                            v6StaticPrefixLength.enable();
                            if (isWanValue) {
                                v6StaticGateway.show();
                                v6StaticGateway.enable();
                                v6StaticDns1.show();
                                v6StaticDns2.show();
                            } else {
                                v6OptionsContainer.show();
                                v6SendRouterAdvertisements.show();
                                if (v6StaticPrefixLength.getValue() != 64)
                                    v6SendRouterAdvertisementsWarning.show();
                            }
                        } else if ( v6ConfigType.getValue() == "AUTO" ) {
                            v6AliasesContainer.show();
                        } else { //DISABLED
                            v6Config.collapse();
                        }
                    }
                },
                populate: function(record, addMode) {
                    var interfaceId=record.get("interfaceId");
                    var allInterfaces=this.grid.getPageList();
                    var bridgedToInterfaces = [];
                    var vlanParentInterfaces = [];
                    for(var i=0; i<allInterfaces.length; i++) {
                        var intf=allInterfaces[i];
                        if(intf.configType == 'ADDRESSED' && intf.interfaceId!=interfaceId) {
                            bridgedToInterfaces.push([ intf.interfaceId, intf.name]);
                            if(!intf.isVlanInterface) {
                                vlanParentInterfaces.push([intf.interfaceId, intf.name]);    
                            } 
                        }
                    }
                    // refresh interface selector stores
                    var bridgedTo = this.query('combo[dataIndex="bridgedTo"]')[0];
                    bridgedTo.getStore().loadData( bridgedToInterfaces );
                    var vlanParent = this.query('combo[dataIndex="vlanParent"]')[0];
                    vlanParent.getStore().loadData( vlanParentInterfaces );
                    Ung.RowEditorWindow.prototype.populate.apply(this, arguments);
                }
            }) );
        },
        // HostName Panel
        buildHostName: function() {
            this.panelHostName = Ext.create('Ext.panel.Panel',{
                name: 'panelHostName',
                helpSource: 'network_interfaces',
                parentId: this.getId(),
                title: this.i18n._('Hostname'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Hostname'),
                    items: [{
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: "textfield",
                            fieldLabel: this.i18n._("Hostname"),
                            emptyText: "untangle",
                            name: 'HostName',
                            value: this.settings.hostName,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.settings.hostName = newValue;
                                    }, this)
                                }
                            }
                        },{
                            xtype: 'label',
                            html: this.i18n._("(eg: gateway)"),
                            cls: 'boxlabel'
                        }]
                    },{
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: "textfield",
                            fieldLabel: this.i18n._("Domain Name"),
                            emptyText: "example.com",
                            name: 'DomainName',
                            value: this.settings.domainName,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.settings.domainName = newValue;
                                    }, this)
                                }
                            }
                        },{
                            xtype: 'label',
                            html: this.i18n._("(eg: example.com)"),
                            cls: 'boxlabel'
                        }]
                    }]
                }, {
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Dynmaic DNS Service Configuration'),
                    items: [{
                        xtype: "checkbox",
                        fieldLabel: this.i18n._("Enabled"),
                        checked: this.settings.dynamicDnsServiceEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.dynamicDnsServiceEnabled = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: "combo",
                        fieldLabel: this.i18n._("Service"),
                        value: this.settings.dynamicDnsServiceName,
                        store: [['easydns','EasyDNS'], ['zoneedit','ZoneEdit'], ['dyndns','DynDNS'],['namecheap','Namecheap'],['dslreports','DSL-Reports'],['dnspark','DNSPark']],
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.dynamicDnsServiceName = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: "textfield",
                        fieldLabel: this.i18n._("Username"),
                        value: this.settings.dynamicDnsServiceUsername,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.dynamicDnsServiceUsername = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: "textfield",
                        fieldLabel: this.i18n._("Password"),
                        value: this.settings.dynamicDnsServicePassword,
                        inputType: 'password',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.dynamicDnsServicePassword = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: "textfield",
                        fieldLabel: this.i18n._("Hostname(s)"),
                        value: this.settings.dynamicDnsServiceHostnames,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.dynamicDnsServiceHostnames = newValue;
                                }, this)
                            }
                        }
                    }]
                }]
            });
        },
        // Services Panel
        buildServices: function() {
            this.panelServices = Ext.create('Ext.panel.Panel',{
                name: 'panelServices',
                helpSource: 'network_interfaces',
                parentId: this.getId(),
                title: this.i18n._('Services'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Local Services'),
                    items: [{
                        border: false,
                        cls: 'description',
                        style: "padding-bottom: 10px;",
                        html: "<br/>" + this.i18n._('The specified HTTPS port will be forwarded from all interfaces to the local HTTPS server to provide administration and other services.') + "<br/>"
                    }, {
                        xtype: 'numberfield',
                        fieldLabel: this.i18n._('HTTPS port'),
                        name: 'httpsPort',
                        value: this.settings.httpsPort,
                        allowDecimals: false,
                        minValue: 0,
                        allowBlank: false,
                        blankText: this.i18n._("You must provide a valid port."),
                        vtype: 'port',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.httpsPort = newValue;
                                }, this)
                            }
                        }
                    }, {
                        border: false,
                        cls: 'description',
                        style: "padding-bottom: 10px;",
                        html: "<br/>" + this.i18n._('The specified HTTP port will be forwarded on non-WAN interfaces to the local HTTP server to provide administration, blockpages, and other services.') + "<br/>"
                    }, {
                        xtype: 'numberfield',
                        fieldLabel: this.i18n._('HTTP port'),
                        name: 'httpPort',
                        value: this.settings.httpPort,
                        allowDecimals: false,
                        minValue: 0,
                        allowBlank: false,
                        blankText: this.i18n._("You must provide a valid port."),
                        vtype: 'port',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.httpPort = newValue;
                                }, this)
                            }
                        }
                    }]
                }]
            });
        },
        // PortForwardRules Panel
        buildPortForwardRules: function() {
            var troubleshootColumn = Ext.create('Ext.grid.column.Action',{
                header: this.i18n._("Troubleshooting"),
                width: 100,
                iconCls: 'icon-detail-row',
                init: function(grid) {
                    this.grid = grid;
                },
                handler: function(view, rowIndex) {
                    var record = view.getStore().getAt(rowIndex);
                    // select current row
                    this.grid.getSelectionModel().select(record);
                    // show details
                    this.grid.onTroubleshoot(record);
                }
            });
            
            this.gridPortForwardRules = Ext.create( 'Ung.EditorGrid', {
                flex: 1,
                name: 'Port Forward Rules',
                settingsCmp: this,
                paginated: false,
                hasReorder: true,
                addAtTop: false,
                emptyRow: {
                    "ruleId": -1,
                    "enabled": true,
                    "description": this.i18n._("[no description]"),
                    "simple":false,
                    "javaClass": "com.untangle.uvm.network.PortForwardRule"
                },
                addSimpleRuleHandler:function() {
                    var record = Ext.create(Ext.ClassManager.getName(this.getStore().getProxy().getModel()), Ext.apply(Ext.decode(Ext.encode(this.emptyRow)), {
                        "simple":true,
                        "matchers": {
                            javaClass: "java.util.LinkedList", 
                            list:[{
                                matcherType:'DST_LOCAL',
                                invert: false,
                                value: "true",
                                javaClass: "com.untangle.uvm.network.PortForwardRuleMatcher"
                            }, {
                                matcherType: 'PROTOCOL',
                                invert: false,
                                value: "TCP",
                                javaClass: "com.untangle.uvm.network.PortForwardRuleMatcher"
                            }, {
                                matcherType:'DST_PORT',
                                invert: false,
                                value: "80",
                                javaClass: "com.untangle.uvm.network.PortForwardRuleMatcher"
                            }]
                        },
                        "newPort": 80
                        
                    }));
                    record.set("internalId", this.genAddedId());
                    this.stopEditing();
                    this.rowEditor.populate(record, true);
                    this.rowEditor.show();
                },
                initComponent : function() {
                    this.tbar= [{
                        text: this.settingsCmp.i18n._('Add Simple Rule'),
                        iconCls: 'icon-add-row',
                        parentId: this.getId(),
                        handler: this.addSimpleRuleHandler,
                        scope: this
                    }];
                    Ung.EditorGrid.prototype.initComponent.apply(this, arguments);
                }, 
                title: this.i18n._("Port Forward Rules"),
                recordJavaClass: "com.untangle.uvm.network.PortForwardRule",
                dataProperty:'portForwardRules',
                plugins:[troubleshootColumn],
                fields: [{
                    name: 'ruleId'
                }, {
                    name: 'enabled'
                }, {
                    name: 'newDestination'
                }, {
                    name: 'newPort'
                }, {
                    name: 'matchers'
                },{
                    name: 'description'
                },{
                    name: 'simple'
                },{
                    name: 'javaClass'
                }],
                columns: [{
                    header: this.i18n._("Rule Id"),
                    width: 50,
                    dataIndex: 'ruleId',
                    renderer: function(value) {
                        if (value < 0) {
                            return i18n._("new");
                        } else {
                            return value;
                        }
                    }
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Enable"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }, {
                    header: this.i18n._("New Destination"),
                    dataIndex: 'newDestination',
                    resizable: false,
                    width:150
                }, {
                    header: this.i18n._("New Port"),
                    dataIndex: 'newPort',
                    resizable: false,
                    width:55
                },troubleshootColumn],
                columnsDefaultSortable: false,
                onTroubleshoot: Ext.bind(function(record) {
                    if(!this.portForwardTroubleshootWin) {
                        this.portForwardTroubleshootWin = Ext.create('Ung.Window', {
                            title: this.i18n._('Port Forward Troubleshooter'),
                            helpSource: 'Port_Forward_Troubleshooting_Guide',
                            settingsCmp: this,
                            helpAction: function() {
                                main.openHelp(this.helpSource);
                            },
                            bbar: [{
                                iconCls: 'icon-help',
                                text: this.i18n._('Help'),
                                handler: function() {
                                    this.portForwardTroubleshootWin.helpAction();
                                },
                                scope: this
                            },'->',{
                                name: "Close",
                                iconCls: 'cancel-icon',
                                text: this.i18n._('Cancel'),
                                handler: function() {
                                    this.portForwardTroubleshootWin.cancelAction();
                                },
                                scope: this
                            }],
                            items: [{
                                xtype: "panel",
                                layout: 'anchor',
                                cls: 'ung-panel',
                                items: [{
                                    xtype: 'fieldset',
                                    layout: "vbox",
                                    cls: 'description',
                                    title: this.i18n._('Troubleshooting Port Forwards'),
                                    items: [{
                                        xtype: "label",
                                        html: this.i18n._( 'Test 1: Verify pinging the <b>new destination</b>' )
                                    },{
                                        xtype: "button",
                                        text: this.i18n._( "Ping Test" ),
                                        handler: function () {
                                            var destination = this.portForwardTroubleshootWin.recordData.newDestination;
                                            this.openPingTest(destination);
                                        },
                                        scope: this
                                    },{
                                        xtype: "label",
                                        style: "margin-top: 10px",
                                        html: this.i18n._( "Test 2: Verify connecting to the new destination<br/><i>This test applies only to TCP port forwards.</i>" )
                                    },{
                                        xtype: "button",
                                        name: "connect_test_button",
                                        text: this.i18n._( "Connect Test" ),
                                        handler: function () {
                                            var recordData = this.portForwardTroubleshootWin.recordData;
                                            this.openTcpTest(recordData.newDestination, recordData.newPort);
                                        },
                                        scope: this
                                    },{
                                        xtype: "label",
                                        style: "margin-top: 10px",
                                        html: this.i18n._( "Test 3: Watch traffic using the Packet Test" )
                                    },{
                                        xtype: "button",
                                        text: this.i18n._( "Packet Test" ),
                                        handler: this.openPacketTest,
                                        scope: this
                                    },{
                                        xtype: "label",
                                        style: "margin-top: 10px",
                                        html: Ext.String.format( this.i18n._( "For more help troubleshooting port forwards view the<br/>{0}Port Forward Troubleshooting Guide{1}" ), "<a href='http://wiki.untangle.com/index.php/Port_Forward_Troubleshooting_Guide'target='_blank'>", "</a>")
                                    }]
                                }]
                            }],
                            showForRule: function(record) {
                                this.recordData = record.getData();
                                
                                //Show connect_test_button buton only if it has TCP protocol
                                var hasTcpProtocol=false;
                                var matchersList=this.recordData.matchers.list;
                                for(var i=0;i<matchersList.length;i++) {
                                    if(matchersList[i].matcherType == "PROTOCOL") {
                                        hasTcpProtocol = (matchersList[i].value.indexOf("TCP") != -1);
                                        break;
                                    }
                                }
                                var connectTestButton = this.query('button[name="connect_test_button"]')[0];
                                if(hasTcpProtocol) {
                                    connectTestButton.enable();
                                } else {
                                    connectTestButton.disable();
                                }
                                this.show();
                            }
                        });
                        this.subCmps.push(this.portForwardTroubleshootWin);
                    }
                    this.portForwardTroubleshootWin.showForRule(record);
                }, this)
            });
            
            //Build port forward warnings
            var portForwardWarningsHtml=[this.i18n._('The following ports are currently reserved and can not be forwarded:') + "<br/>"];
            var i;
            var intf;
            for ( i = 0 ; i < this.settings.interfaces.list.length ; i++) {
                intf = this.settings.interfaces.list[i];
                if (intf.v4Address) {
                    portForwardWarningsHtml.push( Ext.String.format(this.i18n._("<b>{0}:{1}</b> for HTTPS services."),intf.v4Address, this.settings.httpsPort)+"<br/>");
                }
            }
            for ( i = 0 ; i < this.settings.interfaces.list.length ; i++) {
                intf = this.settings.interfaces.list[i];
                if (intf.v4Address && !intf.isWan) {
                    portForwardWarningsHtml.push( Ext.String.format(this.i18n._("<b>{0}:{1}</b> for HTTP services."),intf.v4Address, this.settings.httpPort)+"<br/>");
                }
            }
            for ( i = 0 ; i < this.settings.interfaces.list.length ; i++) {
                intf = this.settings.interfaces.list[i];
                if (intf.v4Address && intf.isWan) {
                    for ( var j = 0 ; j < this.settings.interfaces.list.length ; j++) {
                        var sub_intf = this.settings.interfaces.list[j];
                        if (sub_intf.configType == "BRIDGED" && sub_intf.bridgedTo == intf.interfaceId) {
                            portForwardWarningsHtml.push( Ext.String.format(this.i18n._("<b>{0}:{1}</b> on {2} interface for HTTP services."),intf.v4Address, this.settings.httpPort, sub_intf.name)+"<br/>");
                        }
                    }
                }
            }
            var protocolStore =[[ "TCP,UDP",  "TCP & UDP"],[ "TCP",  "TCP"],[ "UDP", "UDP"]];
            var portStore =[[ "21", "FTP (21)" ],[ "25", "SMTP (25)" ],[ "53", "DNS (53)" ],[ "80", "HTTP (80)" ],[ "110", "POP3 (110)" ],[ "143", "IMAP (143)" ],[ "443", "HTTPS (443)" ],[ "1723", "PPTP (1723)" ],[ "-1", this.i18n._("Other") ]];
            this.panelPortForwardRules = Ext.create('Ext.panel.Panel',{
                name: 'panelPortForwardRules',
                helpSource: 'network_port_forwards',
                parentId: this.getId(),
                title: this.i18n._('Port Forward Rules'),
                layout: { type: 'vbox', align: 'stretch' },
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Port Forward Rules'),
                    html: this.i18n._("Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT'd) network. The rules are evaluated in order."),
                    style: "margin-bottom: 10px;"
                }, this.gridPortForwardRules, {
                    xtype: 'label',
                    flex: 0,
                    html: portForwardWarningsHtml.join(""),
                    style: 'margin: 10px;'
                }]
            });
            var settingsCmp = this;
            this.gridPortForwardRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
                sizeToComponent: this.panelPortForwardRules,
                rowEditorLabelWidth: 160,
                populate: function(record, addMode) {
                    //reinitialize dataIndex on both editors
                    this.query('fieldset[name="simple_portforward_editor"]')[0].dataIndex="matchers";
                    this.query('rulebuilder')[0].dataIndex="matchers";
                    Ung.RowEditorWindow.prototype.populate.apply(this, arguments);
                },
                syncComponents: function () {
                    var isSimple = this.query('checkbox[dataIndex="simple"]')[0].getValue();
                    var simpleEditor=this.query('fieldset[name="simple_portforward_editor"]')[0];
                    var advancedEditor=this.query('[name="advanced_portforward_editor"]')[0];
                    var rulebuilder=advancedEditor.query('rulebuilder')[0];
                    simpleEditor.setVisible(isSimple);
                    advancedEditor.setVisible(!isSimple);
                    if(isSimple) {
                        simpleEditor.dataIndex="matchers";
                        rulebuilder.dataIndex="";
                    } else {
                        simpleEditor.dataIndex="";
                        rulebuilder.dataIndex="matchers";
                    }
                    this.query('[name="switch_advanced_btn"]')[0].setVisible(isSimple);
                    this.query('[name="new_port_container"]')[0].setVisible(!isSimple);
                    
                    Ext.defer( function(){
                        this.query('fieldset[name="fwd_description"]')[0].setTitle( isSimple ?
                            settingsCmp.i18n._('Traffic matching the above description destined to any Untangle IP will be forwarded to the new location:') :
                            settingsCmp.i18n._('Forward to the following location:'));
                    },1, this);
                },
                inputLines: [{
                    xtype:'checkbox',
                    hidden:true,
                    dataIndex: "simple"
                }, {
                    xtype:'checkbox',
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enable Port Forward Rule")
                }, {
                    xtype:'textfield',
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 500
                }, {   
                    xtype : 'fieldset',
                    name: 'simple_portforward_editor',
                    autoHeight : true,
                    dataIndex: "matchers",
                    setValue: function(value, record) {
                        if(record.get("simple")) {
                            var matchersMap=Ung.Util.createRecordsMap(record.get("matchers").list, "matcherType");
                            var protocol = matchersMap["PROTOCOL"]?matchersMap["PROTOCOL"].value:"TCP";
                            this.query('combo[name="simple_protocol"]')[0].setValue(protocol);

                            var dstPort = matchersMap["DST_PORT"]?matchersMap["DST_PORT"].value:"";
                            var dstPortOther=this.query('numberfield[name="simple_destination_port"]')[0];
                            dstPortOther.setValue(dstPort);
                            var dstPortCombo=this.query('combo[name="simple_basic_port"]')[0];
                            var isOtherPort=true;
                            for(var i=0;i<portStore.length;i++) {
                                if(dstPort==portStore[i][0]) {
                                    isOtherPort=false;
                                    dstPortCombo.setValue(dstPort);
                                    break;
                                }
                            }
                            if(isOtherPort) {
                                dstPortCombo.setValue("-1");
                            }
                            dstPortOther.setVisible(isOtherPort);
                        }
                    },
                    getValue: function() {
                        var isSimple = settingsCmp.gridPortForwardRules.rowEditor.query('checkbox[dataIndex="simple"]')[0].getValue();
                        if(isSimple) {
                            var protocol = this.query('combo[name="simple_protocol"]')[0].getValue();
                            var port = ""+this.query('[name="simple_destination_port"]')[0].getValue();
                            return {
                                javaClass: "java.util.LinkedList", 
                                list:[{
                                    matcherType:'DST_LOCAL',
                                    invert: false,
                                    value: "true",
                                    javaClass: "com.untangle.uvm.network.PortForwardRuleMatcher"
                                }, {
                                    matcherType: 'PROTOCOL',
                                    invert: false,
                                    value: protocol,
                                    javaClass: "com.untangle.uvm.network.PortForwardRuleMatcher"
                                }, {
                                    matcherType:'DST_PORT',
                                    invert: false,
                                    value: port,
                                    javaClass: "com.untangle.uvm.network.PortForwardRuleMatcher"
                                }]
                            };                   
                        } else {
                            return undefined;
                        }
                    },
                    title : this.i18n._("Forward the following traffic:"),
                    items : [{
                        xtype : "combo",
                        fieldLabel : this.i18n._("Protocol"),
                        width : 300,
                        name : "simple_protocol",
                        store : protocolStore,
                        editable : false,
                        queryMode: 'local'
                    }, {
                        xtype : "combo",
                        fieldLabel : this.i18n._("Port"),
                        width : 300,
                        name : "simple_basic_port",
                        store : portStore,
                        editable : false,
                        queryMode: 'local',
                        listeners: {
                            "select": {
                                fn: Ext.bind(function(combo, records, eOpts) {
                                    var value = combo.getValue();
                                    var isVisible = (value == "-1");
                                    var port = this.gridPortForwardRules.rowEditor.query('[name="simple_destination_port"]')[0];
                                    port.setVisible( isVisible );
                                    if ( !isVisible ) {
                                        port.setValue( value );
                                    }
                                }, this)
                            }
                        }
                    }, {
                        xtype : "numberfield",
                        fieldLabel : this.i18n._("Port Number"),
                        hidden: true,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(field, value) {
                                    this.gridPortForwardRules.rowEditor.query('numberfield[dataIndex="newPort"]')[0].setValue(value);
                                }, this)
                            }
                        },
                        name : "simple_destination_port",
                        minValue : 1,
                        maxValue : 0xFFFF,
                        width : 200
                    }]
                }, {
                    xtype:'fieldset',
                    name:"advanced_portforward_editor",
                    hidden:true,
                    title: this.i18n._("If all of the following conditions are met:"),
                    items:[{
                        xtype: 'rulebuilder',
                        settingsCmp: this,
                        javaClass: "com.untangle.uvm.network.PortForwardRuleMatcher",
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getPortForwardMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    name: 'fwd_description',
                    title: "",
                    items: [{
                        xtype:'textfield',
                        allowBlank: false,
                        dataIndex: "newDestination",
                        fieldLabel: this.i18n._("New Destination"),
                        vtype: 'ipAddress'
                    }, {
                        xtype: 'container',
                        hidden:true,
                        name: "new_port_container",
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype:'numberfield',
                            allowBlank: true,
                            width: 200,
                            dataIndex: "newPort",
                            fieldLabel: this.i18n._("New Port"),
                            minValue : 1,
                            maxValue : 0xFFFF,
                            vtype: 'port'
                        }, {
                            xtype: 'label',
                            html: this.i18n._("(optional)"),
                            cls: 'boxlabel'
                        }]
                    }]
                }, {
                    xtype: "button",
                    name: "switch_advanced_btn",
                    text: this.i18n._( "Switch to Advanced" ),
                    handler: function() {
                        var rowEditor = this.gridPortForwardRules.rowEditor;
                        var matchers = rowEditor.query('fieldset[name="simple_portforward_editor"]')[0].getValue();
                        rowEditor.query('checkbox[dataIndex="simple"]')[0].setValue(false);
                        rowEditor.query('rulebuilder')[0].setValue(matchers);
                        rowEditor.syncComponents();
                    },
                    scope: this
                }]
            }));
        },
        // NatRules Panel
        buildNatRules: function() {
            this.gridNatRules = Ext.create( 'Ung.EditorGrid', {
                anchor: '100% -80',
                name: 'NAT Rules',
                settingsCmp: this,
                paginated: false,
                hasReorder: true,
                addAtTop: false,
                emptyRow: {
                    "ruleId": -1,
                    "enabled": true,
                    "auto": true,
                    "description": this.i18n._("[no description]"),
                    "javaClass": "com.untangle.uvm.network.NatRule"
                },
                title: this.i18n._("NAT Rules"),
                recordJavaClass: "com.untangle.uvm.network.NatRule",
                dataProperty:'natRules',
                fields: [{
                    name: 'ruleId'
                }, {
                    name: 'enabled'
                }, {
                    name: 'auto'
                }, {
                    name: 'newSource'
                }, {
                    name: 'matchers'
                },{
                    name: 'description'
                }, {
                    name: 'javaClass'
                }],
                columns: [{
                    header: this.i18n._("Rule Id"),
                    width: 50,
                    dataIndex: 'ruleId',
                    renderer: function(value) {
                        if (value < 0) {
                            return i18n._("new");
                        } else {
                            return value;
                        }
                    }
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Enable"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                columnsDefaultSortable: false
            });
            
            this.panelNatRules = Ext.create('Ext.panel.Panel',{
                name: 'panelNatRules',
                helpSource: 'network_nat_rules',
                parentId: this.getId(),
                title: this.i18n._('NAT Rules'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('NAT Rules'),
                    html: this.i18n._("NAT Rules control the rewriting of the IP source address of traffic (Network Address Translation). The rules are evaluated in order.")
                },  this.gridNatRules]
            });
            this.gridNatRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
                sizeToComponent: this.panelNatRules,
                inputLines: [{
                    xtype:'checkbox',
                    name: "Enable NAT Rule",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enable NAT Rule")
                }, {
                    xtype:'textfield',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 500
                }, {
                    xtype:'fieldset',
                    title: this.i18n._("If all of the following conditions are met:"),
                    items:[{
                        xtype:'rulebuilder',
                        settingsCmp: this,
                        javaClass: "com.untangle.uvm.network.NatRuleMatcher",
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getNatRuleMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: this.i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype: "combo",
                        allowBlank: false,
                        dataIndex: "auto",
                        fieldLabel: this.i18n._("NAT Type"),
                        editable: false,
                        store: [[true, this.i18n._('Auto')], [false, this.i18n._('Custom')]],
                        queryMode: 'local',
                        listeners: {
                            select: Ext.bind(function(combo, ewVal, oldVal) {
                                this.gridNatRules.rowEditor.syncComponents();
                            }, this )
                        }
                    }, {
                        xtype:'textfield',
                        name: "newSource",
                        allowBlank: true,
                        dataIndex: "newSource",
                        fieldLabel: this.i18n._("New Source"),
                        hidden: true,
                        vtype: 'ipAddress'
                    }]
                }],
                syncComponents: function () {
                    var natType  = this.query('combo[dataIndex="auto"]')[0];
                    var newSource = this.query('textfield[dataIndex="newSource"]')[0];
                    if (natType.value) { //Auto
                        newSource.disable();
                        newSource.hide();
                    } else {
                        newSource.enable();
                        newSource.show();
                    }
                }
            }));
        },
        // BypassRules Panel
        buildBypassRules: function() {
            this.gridBypassRules = Ext.create( 'Ung.EditorGrid', {
                anchor: '100% -80',
                name: 'Bypass Rules',
                settingsCmp: this,
                paginated: false,
                hasReorder: true,
                addAtTop: false,
                emptyRow: {
                    "ruleId": -1,
                    "enabled": true,
                    "bypass": true,
                    "description": this.i18n._("[no description]"),
                    "javaClass": "com.untangle.uvm.network.BypassRule"
                },
                title: this.i18n._("Bypass Rules"),
                recordJavaClass: "com.untangle.uvm.network.BypassRule",
                dataProperty:'bypassRules',
                fields: [{
                    name: 'ruleId'
                }, {
                    name: 'enabled'
                }, {
                    name: 'bypass'
                }, {
                    name: 'matchers'
                },{
                    name: 'description'
                }, {
                    name: 'javaClass'
                }],
                columns: [{
                    header: this.i18n._("Rule Id"),
                    width: 50,
                    dataIndex: 'ruleId',
                    renderer: function(value) {
                        if (value < 0) {
                            return i18n._("new");
                        } else {
                            return value;
                        }
                    }
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Enable"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Bypass"),
                    dataIndex: 'bypass',
                    resizable: false,
                    width:55
                }],
                columnsDefaultSortable: false
            });
            
            this.panelBypassRules = Ext.create('Ext.panel.Panel',{
                name: 'panelBypassRules',
                helpSource: 'network_bypass_rules',
                parentId: this.getId(),
                title: this.i18n._('Bypass Rules'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Bypass Rules'),
                    html: this.i18n._("Bypass Rules control what traffic is scanned by the applications. Bypassed traffic skips application processing. The rules are evaluated in order. Sessions that meet no rule are not bypassed.")
                }, this.gridBypassRules]
            });
            this.gridBypassRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
                sizeToComponent: this.panelBypassRules,
                inputLines:[{
                    xtype:'checkbox',
                    name: "Enable Bypass Rule",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enable Bypass Rule")
                }, {
                    xtype:'textfield',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 500
                }, {
                    xtype:'fieldset',
                    title: this.i18n._("If all of the following conditions are met:"),
                    items:[{
                        xtype:'rulebuilder',
                        settingsCmp: this,
                        javaClass: "com.untangle.uvm.network.BypassRuleMatcher",
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getBypassRuleMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: this.i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype: "combo",
                        name: "bypass",
                        allowBlank: false,
                        dataIndex: "bypass",
                        fieldLabel: this.i18n._("Action"),
                        editable: false,
                        store: [[true, this.i18n._('Bypass')], [false, this.i18n._('Process')]],
                        queryMode: 'local'
                    }]
                }]
            }));
        },
        // Routes Panel
        buildRoutes: function() {
            var devList = [];
            var devMap = {};
            for ( var c = 0 ; c < this.settings.interfaces.list.length ; c++ ) {
                var intf = this.settings.interfaces.list[c];
                var name = Ext.String.format(this.i18n._("Local on {0} ({1})"),intf.name, intf.systemDev);
                var key = ""+intf.interfaceId;
                devList.push( [ key, name ] );
                devMap[key]=name;
            }
            this.gridStaticRoutes = Ext.create('Ung.EditorGrid', {
                height: 300,
                name: 'Static Routes',
                settingsCmp: this,
                emptyRow: {
                    "ruleId": -1,
                    "network": "1.2.3.0",
                    "prefix": 24,
                    "nextHop": "4.3.2.1",
                    "description": this.i18n._("[no description]"),
                    "javaClass": "com.untangle.uvm.network.StaticRoute"
                },
                title: this.i18n._("Static Routes"),
                recordJavaClass: "com.untangle.uvm.network.StaticRoute",
                dataProperty: 'staticRoutes',
                fields: [{
                    name: 'ruleId'
                }, {
                    name: 'network'
                }, {
                    name: 'prefix'
                }, {
                    name: 'nextHop'
                },{
                    name: 'description'
                }, {
                    name: 'javaClass'
                }],
                columns: [{
                    header: this.i18n._("Network"),
                    width: 170,
                    dataIndex: 'network'
                }, {
                    header: this.i18n._("Netmask/Prefix"),
                    width: 170,
                    dataIndex: 'prefix'
                }, {
                    header: this.i18n._("Next Hop"),
                    width: 300,
                    dataIndex: 'nextHop',
                    renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                        var intRegex = /^\d+$/;
                        if ( intRegex.test( value ) ) {
                            return devMap[value]?devMap[value]:this.i18n._("Local interface");
                        } else {
                            return value;
                        }
                    }, this)
                }, {
                    header: this.i18n._("Description"),
                    width: 300,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                sortField: 'network',
                columnsDefaultSortable: true,
                rowEditorInputLines: [{
                    xtype:'textfield',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    allowBlank: false,
                    width: 400
                }, {
                    xtype:'textfield',
                    name: "Network",
                    dataIndex: "network",
                    fieldLabel: this.i18n._("Network"),
                    allowBlank: false,
                    vtype:"ipAddress",
                    width: 300
                }, {
                    xtype: "combo",
                    dataIndex: "prefix",
                    fieldLabel: this.i18n._( "Netmask/Prefix" ),
                    store: Ung.Util.getV4NetmaskList( false ),
                    width: 300,
                    listWidth: 70,
                    queryMode: 'local',
                    editable: false
                }, {
                    xtype: "combo",
                    dataIndex: "nextHop",
                    fieldLabel: this.i18n._("Next Hop"),
                    store: devList,
                    width: 300,
                    queryMode: 'local',
                    editable : true,
                    allowBlank: false
                }, {
                    xtype: 'fieldset',
                    cls: 'description',
                    html: this.i18n._("If <b>Next Hop</b> is an IP address that network will routed via the specified IP address.") + "<br/>" +
                        this.i18n._("If <b>Next Hop</b> is an interface that network will be routed <b>locally</b> on that interface.")
                }]
            });

            this.routeArea = Ext.create('Ext.form.TextArea',{
                style: "font-family: monospace",
                name: "state",
                hideLabel: true,
                labelSeperator: "",
                readOnly: true,
                autoCreate: { tag: 'textarea', autocomplete: 'off', spellcheck: 'false' },
                height: 200,
                width: "100%",
                isDirty: function() { return false; }
            });

            this.routeButton = Ext.create('Ext.button.Button',{
                text: " Refresh Routes ",
                handler: function(b,e) {
                    main.getExecManager().exec(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        this.routeArea.setValue( result.output );
                    }, this), "/usr/share/untangle/bin/ut-routedump.sh");  
                },
                scope: this
            });
            
            this.panelRoutes = Ext.create('Ext.panel.Panel',{
                name: 'panelRoutes',
                helpSource: 'network_route_rules',
                parentId: this.getId(),
                title: this.i18n._('Routes'),
                autoScroll: true,
                //layout: { type: 'vbox', pack: 'start', align: 'stretch' },
                layout: "anchor",
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Static Routes'),
                    html: this.i18n._("Static Routes are global routes that control how traffic is routed by destination address. The most specific Static Route is taken for a particular packet, order is not important.")
                }, this.gridStaticRoutes, {
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Current Routes'),
                    html: this.i18n._("Current Routes shows the current routing system's configuration and how all traffic will be routed.")
                }, this.routeArea, this.routeButton]
            });
        },
        // Advanced Panel
        buildAdvanced: function() {

            this.buildOptions();
            this.buildQoS();
            this.buildFilter();
            this.buildDnsServer();
            this.buildDhcpServer();
            this.buildNetworkCards();

            this.advancedTabPanel = Ext.create('Ext.tab.Panel',{
                activeTab: 0,
                deferredRender: false,
                parentId: this.getId(),
                autoHeight: true,
                flex: 1,
                items: [ this.panelOptions, this.panelQoS, this.panelFilter, this.panelDnsServer, this.panelDhcpServer, this.gridNetworkCards ]
            });
            
            this.panelAdvanced = Ext.create('Ext.panel.Panel',{
                name: 'panelAdvanced',
                helpSource: 'network_advanced',
                parentId: this.getId(),
                title: this.i18n._('Advanced'),
                cls: 'ung-panel',
                layout: { type: 'vbox', pack: 'start', align: 'stretch' },
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    flex: 0,
                    title: this.i18n._("Advanced"),
                    html: this.i18n._("Advanced settings require careful configuration. Misconfiguration can compromise the proper operation and security of your server.")
                }, this.advancedTabPanel]
            });
        },
        // Options Panel
        buildOptions: function() {
            this.panelOptions = Ext.create('Ext.panel.Panel',{
                name: 'panelOptions',
                helpSource: 'network_general',
                parentId: this.getId(),
                title: this.i18n._('Options'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: "checkbox",
                    fieldLabel: this.i18n._("Enable SIP NAT Helper"),
                    labelStyle: 'width:150px',
                    checked: this.settings.enableSipNatHelper,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.enableSipNatHelper = newValue;
                            }, this)
                        }
                    }
                },{
                    xtype: "checkbox",
                    fieldLabel: this.i18n._("Send ICMP Redirects"),
                    labelStyle: 'width:150px',
                    checked: this.settings.sendIcmpRedirects,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.sendIcmpRedirects = newValue;
                            }, this)
                        }
                    }
                },{
                    xtype: "checkbox",
                    fieldLabel: this.i18n._("DHCP Authoritative"),
                    labelStyle: 'width:150px',
                    checked: this.settings.dhcpAuthoritative,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.dhcpAuthoritative = newValue;
                            }, this)
                        }
                    }
                }]
            });
        },
        // QoS Panel
        buildQoS: function() {
            this.qosPriorityStore = [
                [0, this.i18n._( "Default" )], 
                [1, this.i18n._( "Very High" )], 
                [2, this.i18n._( "High" )], 
                [3, this.i18n._( "Medium" )], 
                [4, this.i18n._( "Low" )], 
                [5, this.i18n._( "Limited" )],
                [6, this.i18n._( "Limited More" )],
                [7, this.i18n._( "Limited Severely" )]
            ];
            this.qosPriorityMap = Ung.Util.createStoreMap(this.qosPriorityStore);

            this.qosPriorityNoDefaultStore = [
                [1, this.i18n._( "Very High" )], 
                [2, this.i18n._( "High" )], 
                [3, this.i18n._( "Medium" )], 
                [4, this.i18n._( "Low" )], 
                [5, this.i18n._( "Limited" )],
                [6, this.i18n._( "Limited More" )],
                [7, this.i18n._( "Limited Severely" )]
            ];
            this.qosPriorityNoDefaultMap = Ung.Util.createStoreMap(this.qosPriorityNoDefaultStore);
            
            this.gridQosWanBandwidth = Ext.create( 'Ung.EditorGrid', {
                name: 'QoS Priorities',
                margin: '5 0 0 0',
                height: 160,
                settingsCmp: this,
                paginated: false,
                hasAdd: false,
                hasDelete: false,
                hasEdit: false,
                recordJavaClass: "com.untangle.uvm.network.InterfaceSettings",
                dataProperty: "interfaces",
                fields: [{
                    name: 'interfaceId'
                }, {
                    name: 'name'
                }, {
                    name: 'configType'
                }, {
                    name: 'downloadBandwidthKbps'
                }, {
                    name: 'uploadBandwidthKbps'
                },{
                    name: 'isWan'
                }],                
                columns: [{
                    header: this.i18n._("Id"),
                    width: 30,
                    dataIndex: 'interfaceId'
                }, {
                    header: this.i18n._("WAN"),
                    width: 150,
                    dataIndex: 'name'
                }, {
                    header: this.i18n._("Config Type"),
                    dataIndex: 'configType',
                    width: 150
                }, {
                    header: this.i18n._("Download Bandwidth"),
                    dataIndex: 'downloadBandwidthKbps',
                    width: 180,
                    editor: {
                        xtype: 'numberfield',
                        allowBlank : false,
                        allowDecimals: false,
                        minValue: 0
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (Ext.isEmpty(value)) {
                            return this.i18n._("Not set"); 
                        } else {
                            return value + this.i18n._( " kbps" );
                        }
                    }, this )
                }, {
                    header: this.i18n._("Upload Bandwidth"),
                    dataIndex: 'uploadBandwidthKbps',
                    width: 180,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false,
                        allowDecimals: false,
                        minValue: 0
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (Ext.isEmpty(value)) {
                            return this.i18n._("Not set"); 
                        } else {
                            return value + this.i18n._( " kbps" );
                        }
                    }, this )
                }],
                columnsDefaultSortable: false,
                updateTotalBandwidth: Ext.bind(function() {
                    var interfaceList=this.gridQosWanBandwidth.getPageList();
                    var u = 0;
                    var d = 0;

                    for ( var i = 0 ; i < interfaceList.length ; i++ ) {
                        if(interfaceList[i].isWan) {
                            if(!Ext.isEmpty(interfaceList[i].uploadBandwidthKbps)) {
                                u += interfaceList[i].uploadBandwidthKbps;    
                            }
                            if(!Ext.isEmpty(interfaceList[i].downloadBandwidthKbps)) {
                                d += interfaceList[i].downloadBandwidthKbps;    
                            }
                        }
                    }

                    var d_Mbit = d/1000;
                    var u_Mbit = u/1000;

                    var message = Ext.String.format( this.i18n._( "Total: {0} kbps ({1} Mbit) download, {2} kbps ({3} Mbit) upload" ), d, d_Mbit, u, u_Mbit );
                    var bandwidthLabel = this.panelQoS.query('label[name="bandwidthLabel"]')[0];
                    bandwidthLabel.setText(Ext.String.format(this.i18n._("{0}Note{1}: When enabling QoS valid Download Bandwidth and Upload Bandwidth limits must be set for all WAN interfaces."),'<font color="red">','</font>')+"</br><i>"+message+'</i>', false);
                }, this)
            });
            this.gridQosWanBandwidth.getStore().on("update", Ext.bind(function() {
                this.gridQosWanBandwidth.updateTotalBandwidth();
            }, this));
            this.gridQosWanBandwidth.getStore().filter([{property: "configType", value: "ADDRESSED"}, {property:"isWan", value: true}]);
            
            this.gridQosRules = Ext.create( 'Ung.EditorGrid', {
                name: 'QoS Custom Rules',
                margin: '5 0 0 0',
                height: 200,
                settingsCmp: this,
                paginated: false,
                hasReorder: true,
                addAtTop: false,
                emptyRow: {
                    "ruleId": -1,
                    "enabled": true,
                    "description": this.i18n._("[no description]"),
                    "priority": 1,
                    "javaClass": "com.untangle.uvm.network.QosRule"
                },
                recordJavaClass: "com.untangle.uvm.network.QosRule",
                dataExpression:'settings.qosSettings.qosRules.list',
                fields: [{
                    name: 'ruleId'
                }, {
                    name: 'enabled'
                }, {
                    name: 'priority'
                }, {
                    name: 'matchers'
                },{
                    name: 'description'
                }, {
                    name: 'javaClass'
                }],
                columns: [{
                    header: this.i18n._("Rule Id"),
                    width: 50,
                    dataIndex: 'ruleId',
                    renderer: function(value) {
                        if (value < 0) {
                            return i18n._("new");
                        } else {
                            return value;
                        }
                    }
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Enable"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: this.i18n._( "Priority" ),
                    width: 100,
                    dataIndex: "priority",
                    renderer: Ext.bind(function( value, metadata, record ) {
                        return this.qosPriorityNoDefaultMap[value];
                    }, this ),
                    editor: {
                        xtype: 'combo',
                        store: this.qosPriorityNoDefaultStore,
                        queryMode: 'local',
                        editable: false
                    }
                }, {
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                columnsDefaultSortable: false
            });
            
            this.gridQosPriorities = Ext.create( 'Ung.EditorGrid', {
                name: 'QoS Priorities',
                margin: '5 0 0 0',
                height: 190,
                settingsCmp: this,
                paginated: false,
                hasAdd: false,
                hasDelete: false,
                hasEdit: false,
                recordJavaClass: "com.untangle.uvm.network.QosPriority",
                dataExpression:'settings.qosSettings.qosPriorities.list',
                fields: [{
                    name: 'priorityId'
                }, {
                    name: 'priorityName'
                }, {
                    name: 'uploadReservation'
                }, {
                    name: 'uploadLimit'
                },{
                    name: 'downloadReservation'
                },{
                    name: 'downloadLimit'
                }, {
                    name: 'javaClass'
                }],                
                columns: [{
                    header: this.i18n._("Priority"),
                    width: 150,
                    dataIndex: 'priorityName',
                    renderer: Ext.bind(function( value, metadata, record ) {
                        return this.i18n._(value);
                    }, this )
                }, {
                    header: this.i18n._("Upload Reservation"),
                    dataIndex: 'uploadReservation',
                    width: 150,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false,
                        minValue : 0.1,
                        maxValue : 100
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (value === 0) 
                            return this.i18n._("No reservation"); 
                        else 
                            return value + "%";
                    }, this )
                }, {
                    header: this.i18n._("Upload Limit"),
                    dataIndex: 'uploadLimit',
                    width: 150,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false,
                        minValue : 0.1,
                        maxValue : 100
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (value === 0) 
                            return this.i18n._("No limit"); 
                        else 
                            return value + "%";
                    }, this )
                }, {
                    header: this.i18n._("Download Reservation"),
                    dataIndex: 'downloadReservation',
                    width: 150,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false,
                        minValue : 0.1,
                        maxValue : 100
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (value === 0) 
                            return this.i18n._("No reservation"); 
                        else 
                            return value + "%";
                    }, this )
                }, {
                    header: this.i18n._("Download Limit"),
                    dataIndex: 'downloadLimit',
                    width: 150,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false,
                        minValue : 0.1,
                        maxValue : 100
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (value === 0) 
                            return this.i18n._("No limit"); 
                        else 
                            return value + "%";
                    }, this )
                }],
                columnsDefaultSortable: false
            });
            
            this.interfaceList = Ung.Util.getInterfaceList(true, true);
           
            this.gridQosStatistics = Ext.create( 'Ung.EditorGrid', {
                name: 'QoS Statistics',
                margin: '5 0 0 0',
                height: 190,
                settingsCmp: this,
                paginated: false,
                hasAdd: false,
                hasDelete: false,
                hasEdit: false,
                dataRoot:'',
                dataFn: function() {
                    var output=main.getExecManager().execOutput("/usr/share/untangle-netd/bin/get_statistics.py");
                    try {
                        return eval(output);
                    } catch (e) {
                        console.error("Could not execute /usr/share/untangle-netd/bin/get_statistics.py output: ", output, e);
                        return [];
                    }
                },
                fields: [{
                    name: 'interface_name'
                },{
                    name: 'priority'
                },{
                    name: 'sent'
                }],   
                tbar:[{
                    xtype: "button",
                    iconCls: 'icon-refresh',
                    text: this.i18n._("Refresh"),
                    handler: function() {
                        this.gridQosStatistics.reload();
                    },
                    scope : this
                }],
                columnsDefaultSortable: true,
                columns: [{
                    header: this.i18n._("Interface"),
                    width: 150,
                    dataIndex: 'interface_name',
                    renderer: Ext.bind(function( value, metadata, record ) {
                        return this.i18n._(value);
                    }, this )
                }, {
                    header: this.i18n._("Priority"),
                    dataIndex: 'priority',
                    width: 150,
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (this.qosPriorityMap[value]!=null) {
                            return value + '-' + this.qosPriorityMap[value];
                        }
                        return value;
                    }, this )
                }, {
                    header: this.i18n._("Data"),
                    dataIndex: 'sent',
                    width: 150,
                    flex:1
                }],
                groupField:'interface_name'
            });
            
            this.gridQosSessions = Ext.create( 'Ung.EditorGrid', {
                name: 'QoS Sessions',
                margin: '5 0 0 0',
                height: 190,
                settingsCmp: this,
                paginated: false,
                hasAdd: false,
                hasDelete: false,
                hasEdit: false,
                dataRoot:'',
                dataFn: function() {
                    var output=main.getExecManager().execOutput("/usr/share/untangle-netd/bin/get_sessions.py");
                    try {
                        return eval(output);
                    } catch (e) {
                        console.error("Could not execute /usr/share/untangle-netd/bin/get_sessions.py output: ", output, e);
                        return [];
                    }
                },
                
                fields: [{
                    name: 'proto'
                },{
                    name: 'src'
                },{
                    name: 'dest'
                },{
                    name: 'src_port'
                },
                {
                    name: 'dst_port'
                },{
                    name:'priority'
                }],   
                tbar:[{
                    xtype: "button",
                    iconCls: 'icon-refresh',
                    text: this.i18n._("Refresh"),
                    handler: function() {
                        this.gridQosSessions.reload();
                    },
                    scope : this
                }],
                columnsDefaultSortable: true,
                columns: [{
                    header: this.i18n._("Protocol"),
                    width: 150,
                    dataIndex: 'proto',
                }, {
                    header: this.i18n._("Source IP"),
                    dataIndex: 'src',
                    width: 150,
                }, {
                    header: this.i18n._("Destination IP"),
                    dataIndex: 'dest',
                    width: 150
                }, {
                    header: this.i18n._("Source port"),
                    dataIndex: 'src_port',
                    width: 150
                }, {
                    header: this.i18n._("Destination port"),
                    dataIndex: 'dst_port',
                    width: 150
                }, {
                    header: this.i18n._("Priority"),
                    dataIndex: 'priority',
                    width: 150,
                    flex:1,
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        return this.qosPriorityMap[value];
                    }, this )
                }]
            });

            this.panelQoS = Ext.create('Ext.panel.Panel',{
                name: 'panelQoS',
                helpSource: 'network_qos',
                parentId: this.getId(),
                title: this.i18n._('QoS'),
                autoScroll: true,
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('QoS'),
                    items: [{
                        xtype: "checkbox",
                        fieldLabel: this.i18n._("Enabled"),
                        checked: this.settings.qosSettings.qosEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.qosSettings.qosEnabled = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: "combo",
                        fieldLabel: this.i18n._("Default Priority"),
                        value: this.settings.qosSettings.defaultPriority,
                        store : this.qosPriorityNoDefaultStore,
                        editable: false,
                        queryMode: 'local',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.qosSettings.defaultPriority = newValue;
                                }, this)
                            }
                        }
                    }]
                }, {
                    xtype: 'fieldset',
                    cls: 'description',
                    name: 'bandwidth_fieldset',
                    title: this.i18n._('WAN Bandwidth'),
                    items: [{
                        xtype: 'label',
                        name: 'bandwidthLabel',
                        html: "&nbsp;"
                    }, this.gridQosWanBandwidth]
                }, {
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('QoS Rules'),
                    items: [{
                        xtype: "combo",
                        fieldLabel: this.i18n._("Ping Priority"),
                        value: this.settings.qosSettings.pingPriority,
                        store : this.qosPriorityStore,
                        editable: false,
                        queryMode: 'local',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.qosSettings.pingPriority = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: "combo",
                        fieldLabel: this.i18n._("DNS Priority"),
                        value: this.settings.qosSettings.dnsPriority,
                        store : this.qosPriorityStore,
                        editable: false,
                        queryMode: 'local',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.qosSettings.dnsPriority = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: "combo",
                        fieldLabel: this.i18n._("SSH Priority"),
                        value: this.settings.qosSettings.sshPriority,
                        store : this.qosPriorityStore,
                        editable: false,
                        queryMode: 'local',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.qosSettings.sshPriority = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: "combo",
                        fieldLabel: this.i18n._("OpenVPN Priority"),
                        value: this.settings.qosSettings.openvpnPriority,
                        store : this.qosPriorityStore,
                        editable: false,
                        queryMode: 'local',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.qosSettings.openvpnPriority = newValue;
                                }, this)
                            }
                        }
                    }]
                }, {
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('QoS Custom Rules'),
                    items: [{
                        xtype: 'label',
                        html: Ext.String.format(this.i18n._("{0}Note{1}: Custom Rules only match <b>Bypassed</b> traffic."),'<font color="red">','</font>')
                    }, this.gridQosRules]
                }, {
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('QoS Priorities'),
                    items: [this.gridQosPriorities]
                }, {
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('QoS Statistics'),
                    items: [this.gridQosStatistics]
                },{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('QoS Sessions'),
                    items: [this.gridQosSessions]
                }]
            });
            this.gridQosRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
                sizeToComponent: this.panelQoS,
                inputLines:[{
                    xtype:'checkbox',
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enable")
                }, {
                    xtype:'textfield',
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 500
                }, {
                    xtype:'fieldset',
                    title: this.i18n._("If all of the following conditions are met:"),
                    items:[{
                        xtype:'rulebuilder',
                        settingsCmp: this,
                        javaClass: "com.untangle.uvm.network.QosRuleMatcher",
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getQosRuleMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: this.i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype: "combo",
                        allowBlank: false,
                        dataIndex: "priority",
                        fieldLabel: this.i18n._("Priority"),
                        editable: false,
                        store: this.qosPriorityNoDefaultStore,
                        queryMode: 'local'
                    }]
                }]
            }));
            this.gridQosWanBandwidth.updateTotalBandwidth();
        },
        // Filter Panel
        buildFilter: function() {
            this.gridForwardFilterRules = Ext.create( 'Ung.EditorGrid', {
                anchor: '100% 48%',
                name: 'Forward Filter Rules',
                settingsCmp: this,
                paginated: false,
                hasReorder: true,
                addAtTop: false,
                emptyRow: {
                    "ruleId": -1,
                    "enabled": true,
                    "blocked": false,
                    "description": this.i18n._("[no description]"),
                    "javaClass": "com.untangle.uvm.network.FilterRule"
                },
                title: this.i18n._("Forward Filter Rules"),
                recordJavaClass: "com.untangle.uvm.network.FilterRule",
                dataProperty:'forwardFilterRules',
                fields: [{
                    name: 'ruleId'
                }, {
                    name: 'enabled'
                }, {
                    name: 'blocked'
                }, {
                    name: 'matchers'
                },{
                    name: 'description'
                }, {
                    name: 'javaClass'
                }],
                columns: [{
                    header: this.i18n._("Rule Id"),
                    width: 50,
                    dataIndex: 'ruleId',
                    renderer: Ext.bind(function(value) {
                        if (value < 0) {
                            return this.i18n._("new");
                        } else {
                            return value;
                        }
                    }, this)
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Enable"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Block"),
                    dataIndex: 'blocked',
                    resizable: false,
                    width:55
                }],
                columnsDefaultSortable: false
            });
            this.gridInputFilterRules = Ext.create( 'Ung.EditorGrid', {
                anchor: '100% 48%',
                name: 'Input Filter Rules',
                settingsCmp: this,
                paginated: false,
                hasReorder: true,
                addAtTop: false,
                emptyRow: {
                    "ruleId": -1,
                    "enabled": true,
                    "blocked": false,
                    "description": this.i18n._("[no description]"),
                    "javaClass": "com.untangle.uvm.network.FilterRule"
                },
                title: this.i18n._("Input Filter Rules"),
                recordJavaClass: "com.untangle.uvm.network.FilterRule",
                dataProperty:'inputFilterRules',
                fields: [{
                    name: 'ruleId'
                }, {
                    name: 'enabled'
                }, {
                    name: 'blocked'
                }, {
                    name: 'matchers'
                },{
                    name: 'description'
                }, {
                    name: 'javaClass'
                }],
                columns: [{
                    header: this.i18n._("Rule Id"),
                    width: 50,
                    dataIndex: 'ruleId',
                    renderer: Ext.bind(function(value) {
                        if (value < 0) {
                            return this.i18n._("new");
                        } else {
                            return value;
                        }
                    }, this)
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Enable"),
                    dataIndex: 'enabled',
                    resizable: false,
                    width:55
                }, {
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }, {
                    xtype:'checkcolumn',
                    header: this.i18n._("Block"),
                    dataIndex: 'blocked',
                    resizable: false,
                    width:55
                }],
                columnsDefaultSortable: false
            });
            
            this.panelFilter = Ext.create('Ext.panel.Panel',{
                name: 'panelFilter',
                helpSource: 'network_filter',
                parentId: this.getId(),
                title: this.i18n._('Filter Rules'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [this.gridForwardFilterRules, this.gridInputFilterRules]
            });
            this.gridForwardFilterRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
                sizeToComponent: this.panelFilter,
                inputLines:[{
                    xtype:'checkbox',
                    name: "Enable Forward Filter Rule",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enable Forward Filter Rule")
                }, {
                    xtype:'textfield',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 500
                }, {
                    xtype:'fieldset',
                    title: this.i18n._("If all of the following conditions are met:"),
                    items:[{
                        xtype:'rulebuilder',
                        settingsCmp: this,
                        javaClass: "com.untangle.uvm.network.FilterRuleMatcher",
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getFilterRuleMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: this.i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype: "combo",
                        name: "blocked",
                        allowBlank: false,
                        dataIndex: "blocked",
                        fieldLabel: this.i18n._("Action"),
                        editable: false,
                        store: [[true,i18n._('Block')], [false,i18n._('Pass')]],
                        queryMode: 'local'
                    }]
                }]
            }));
            this.gridInputFilterRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
                sizeToComponent: this.panelFilter,
                inputLines:[{
                    xtype:'checkbox',
                    name: "Enable Forward Filter Rule",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enable Input Filter Rule")
                }, {
                    xtype:'textfield',
                    name: "Description",
                    dataIndex: "description",
                    fieldLabel: this.i18n._("Description"),
                    width: 500
                }, {
                    xtype:'fieldset',
                    title: this.i18n._("If all of the following conditions are met:"),
                    items:[{
                        xtype:'rulebuilder',
                        settingsCmp: this,
                        javaClass: "com.untangle.uvm.network.FilterRuleMatcher",
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getFilterRuleMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: this.i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype: "combo",
                        name: "blocked",
                        allowBlank: false,
                        dataIndex: "blocked",
                        fieldLabel: this.i18n._("Action"),
                        editable: false,
                        store: [[true,i18n._('Block')], [false,i18n._('Pass')]],
                        queryMode: 'local'
                    }]
                }]
            }));
        },
        // DnsServer Panel
        buildDnsServer: function() {
            this.gridDnsStaticEntries = Ext.create( 'Ung.EditorGrid', {
                anchor: '100% 48%',
                name: 'Static DNS Entries',
                settingsCmp: this,
                paginated: false,
                hasEdit: false,
                emptyRow: {
                    "name": this.i18n._("[no name]"),
                    "address": "1.2.3.4",
                    "javaClass": "com.untangle.uvm.network.DnsStaticEntry"
                },
                title: this.i18n._("Static DNS Entries"),
                recordJavaClass: "com.untangle.uvm.network.DnsStaticEntry",
                dataExpression:'settings.dnsSettings.staticEntries.list',
                fields: [{
                    name: 'name'
                }, {
                    name: 'address'
                }],
                columns: [{
                    header: this.i18n._("Name"),
                    width: 200,
                    dataIndex: 'name',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                },{
                    header: this.i18n._("Address"),
                    dataIndex: 'address',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        vtype:"ipAddress"
                    }
                }]
            });
            this.gridDnsLocalServers = Ext.create( 'Ung.EditorGrid', {
                anchor: '100% 48%',
                name: 'Local DNS Servers',
                settingsCmp: this,
                paginated: false,
                hasEdit: false,
                emptyRow: {
                    "domain": this.i18n._("[no domain]"),
                    "localServer": "1.2.3.4",
                    "javaClass": "com.untangle.uvm.network.DnsLocalServer"
                },
                title: this.i18n._("Local DNS Servers"),
                recordJavaClass: "com.untangle.uvm.network.DnsLocalServer",
                dataExpression:'settings.dnsSettings.localServers.list',
                fields: [{
                    name: 'domain'
                }, {
                    name: 'localServer'
                }],
                columns: [{
                    header: this.i18n._("Domain"),
                    width: 200,
                    dataIndex: 'domain',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                },{
                    header: this.i18n._("Local Server"),
                    dataIndex: 'localServer',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        vtype:"ipAddress"
                    }
                }]
            });
            this.panelDnsServer = Ext.create('Ext.panel.Panel',{
                name: 'panelDnsServer',
                helpSource: 'network_dns_server',
                parentId: this.getId(),
                title: this.i18n._('DNS Server'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [ this.gridDnsStaticEntries, this.gridDnsLocalServers ]
            });
        },        
        // DhcpServer Panel
        buildDhcpServer: function() {
            this.gridDhcpStaticEntries = Ext.create( 'Ung.EditorGrid', {
                height: 200,
                name: 'Static DHCP Entries',
                settingsCmp: this,
                paginated: false,
                hasEdit: false,
                emptyRow: {
                    "macAddress": "11:22:33:44:55:66",
                    "address": "1.2.3.4",
                    "description": this.i18n._("[no description]"),
                    "javaClass": "com.untangle.uvm.network.DhcpStaticEntry"
                },
                title: this.i18n._("Static DHCP Entries"),
                recordJavaClass: "com.untangle.uvm.network.DhcpStaticEntry",
                dataExpression:'settings.staticDhcpEntries.list',
                fields: [{
                    name: 'macAddress'
                }, {
                    name: 'address'
                }, {
                    name: 'description'
                }],
                columnsDefaultSortable: true,
                columns: [{
                    header: this.i18n._("MAC Address"),
                    width: 200,
                    dataIndex: 'macAddress',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                },{
                    header: this.i18n._("Address"),
                    dataIndex: 'address',
                    flex: 1,
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        vtype:"ipAddress"
                    }
                },{
                    header: this.i18n._("Description"),
                    width: 200,
                    dataIndex: 'description',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }]
            });
            
            this.gridCurrentDhcpLeases = Ext.create('Ung.EditorGrid',{
                name: "gridCurrentDhcpLeases",
                height: 200,
                settingsCmp: this,
                parentId: this.getId(),
                hasAdd: false,
                hasEdit: false,
                hasDelete: false,
                columnsDefaultSortable: true,
                title: this.i18n._("Current DHCP Leases"),
                paginated: false,
                bbar: Ext.create('Ext.toolbar.Toolbar', {
                    items: [
                        '-',
                        {
                            xtype: 'button',
                            id: "refresh_"+this.getId(),
                            text: this.i18n._('Refresh'),
                            name: "Refresh",
                            tooltip: this.i18n._('Refresh'),
                            iconCls: 'icon-refresh',
                            handler: function() {
                                this.gridCurrentDhcpLeases.reload();
                            },
                            scope: this
                        }
                    ]
                }),
                dataRoot: null,
                dataFn: function() {
                    var leaseText = main.getExecManager().execOutput("cat /var/lib/misc/dnsmasq.leases");  
                    var lines = leaseText.split("\n");
                    var leases = [];
                    for ( var i = 0 ; i < lines.length ; i++ ) {
                        if ( lines[i] === null || lines[i] === "" ) continue;
                        
                        var lineparts = lines[i].split(/\s+/);
                        leases.push( {
                            date: lineparts[0],
                            macAddress: lineparts[1],
                            address: lineparts[2],
                            hostname: lineparts[3],
                            clientId: lineparts[4]
                        } );
                    }
                    return leases;
                },
                fields: [{
                    name: "date"
                },{
                    name: "macAddress"
                },{
                    name: "address"
                },{
                    name: "hostname"
                }],
                columns: [{
                    header: this.i18n._("MAC Address"),
                    dataIndex:'macAddress',
                    width: 150
                },{
                    header: this.i18n._("Address"),
                    dataIndex:'address',
                    width: 200
                },{
                    header: this.i18n._("Hostname"),
                    dataIndex:'hostname',
                    width: 200
                },{
                    header: this.i18n._("Expiration Time"),
                    dataIndex:'date',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value*1000); }
                }]
            });

            this.panelDhcpServer = Ext.create('Ext.panel.Panel',{
                name: 'panelDhcpServer',
                helpSource: 'network_dhcp_server',
                parentId: this.getId(),
                title: this.i18n._('DHCP Server'),
                autoScroll: true,
                layout: { type: 'vbox', pack: 'start', align: 'stretch' },
                cls: 'ung-panel',
                items: [ this.gridDhcpStaticEntries, {
                             border: false,
                             cls: 'description',
                             style: "padding-bottom: 10px;"
                         },
                         this.gridCurrentDhcpLeases, {
                             border: false,
                             cls: 'description',
                             html: "<br/><b>" + this.i18n._('Custom dnsmasq options.') + "</b><br/>" +
                                 "<font color=\"red\">" + this.i18n._("Warning: Invalid syntax will halt all DHCP & DNS services.") + "</font>" + "<br/>",
                             style: "padding-bottom: 10px;"
                         }, {
                             xtype: "textarea",
                             width : 397,
                             height : 140,                    
                             value: this.settings.dnsmasqOptions,
                             listeners: {
                                 "change": {
                                     fn: Ext.bind(function(elem, newValue) {
                                         this.settings.dnsmasqOptions = newValue;
                                     }, this)
                                 }
                             }
                         }]
            });
        },        
        // NetworkCards Panel
        buildNetworkCards: function() {
            this.duplexStore = [
                ["AUTO", this.i18n._( "Auto" )], 
                ["M1000_FULL_DUPLEX", this.i18n._( "1000 Mbps, Full Duplex" )],
                ["M1000_HALF_DUPLEX", this.i18n._( "1000 Mbps, Half Duplex" )],
                ["M100_FULL_DUPLEX", this.i18n._( "100 Mbps, Full Duplex" )],
                ["M100_HALF_DUPLEX", this.i18n._( "100 Mbps, Half Duplex" )],
                ["M10_FULL_DUPLEX", this.i18n._( "10 Mbps, Full Duplex" )],
                ["M10_HALF_DUPLEX", this.i18n._( "10 Mbps, Half Duplex" )]
            ];
            this.duplexMap = Ung.Util.createStoreMap(this.duplexStore);

            this.gridNetworkCards = Ext.create( 'Ung.EditorGrid', {
                name: 'Network Cards',
                helpSource: 'network_cards',
                parentId: this.getId(),
                title: this.i18n._('Network Cards'),
                settingsCmp: this,
                paginated: false,
                hasAdd: false,
                hasDelete: false,
                hasEdit: false,
                recordJavaClass: "com.untangle.uvm.network.DeviceSettings",
                dataProperty: 'devices',
                fields: [{
                    name: 'deviceName'
                }, {
                    name: 'duplex'
                }, {
                    name: 'mtu'
                }],
                columns: [{
                    header: this.i18n._("Device Name"),
                    width: 250,
                    dataIndex: 'deviceName'
                }, {
                    header: this.i18n._("MTU"),
                    dataIndex: 'mtu',
                    width: 100,
                    renderer: function(value) {
                        if ( value === "" || value === null ) {
                            return "Auto";
                        } else {
                            return value;
                        }
                    },
                    editor: {
                        xtype:'numberfield'
                    }
                }, {
                    header: this.i18n._("Ethernet Media"),
                    dataIndex: 'duplex',
                    width: 250,
                    renderer: Ext.bind(function( value, metadata, record ) {
                        return this.duplexMap[value];
                    }, this ),
                    editor: {
                        xtype: 'combo',
                        store: this.duplexStore,
                        queryMode: 'local',
                        editable: false
                    }
                }]
            });
        },
        // Troubleshooting Panel
        buildTroubleshooting: function() {
            var settingsCmp = this;
            this.gridNetworkTests = Ext.create( 'Ung.EditorGrid', {
                name: 'Network Cards',
                parentId: this.getId(),
                settingsCmp: this,
                paginated: false,
                hasAdd: false,
                hasDelete: false,
                hasEdit: false,
                data: [{
                    divClass : "ua-cell-test-connectivity",
                    action : "openConnectivityTest",
                    name : this.i18n._( "Connectivity Test" )
                },{
                    divClass : "ua-cell-test-ping",
                    action : "openPingTest",
                    name : this.i18n._( "Ping Test" )
                },{
                    divClass : "ua-cell-test-dns",
                    action : "openDnsTest",
                    name : this.i18n._( "DNS Test" )
                },{
                    divClass : "ua-cell-test-tcp",
                    action : "openTcpTest",
                    name : this.i18n._( "Connection Test" )
                },{
                    divClass : "ua-cell-test-traceroute",
                    action : "openTracerouteTest",
                    name : this.i18n._( "Traceroute Test" )
                },{
                    divClass : "ua-cell-test-packet",
                    action : "openPacketTest",
                    name : this.i18n._( "Packet Test" )
                }],
                fields: [{
                    name: 'name'
                }, {
                    name: 'divClass'
                }, {
                    name: 'action'
                }],
                columns: [{
                    header: this.i18n._("Network Tests"),
                    flex: 1,
                    dataIndex: 'name',
                    renderer: Ext.bind(function( value, metadata, record ) {
                        return "<a href='/' onClick='Ung.NetworkSettingsCmp." + record.get("action") + "();return false;'><div class=' ua-cell-test " + record.get("divClass") + "'>" + value + "</div></a>";
                    }, this )
                }]
            });
            this.panelTroubleshooting = Ext.create('Ext.panel.Panel',{
                helpSource: 'troubleshoot',
                parentId: this.getId(),
                title: this.i18n._('Troubleshooting'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Network Tests')
                }, this.gridNetworkTests]
            });
        },
        openConnectivityTest: function() {
            if(!this.connectivityTest) {
                this.connectivityTest = Ext.create('Ung.NetworkTest',{
                    helpSource: 'connectivity_test',
                    settingsCmp: this,
                    title: this.i18n._('Connectivity Test'),
                    testDescription: this.i18n._("The <b>Connectivity Test</b> verifies a working connection to the Internet."),
                    testErrorMessage : this.i18n._( "Unable to complete the Connectivity Test." ),
                    testEmptyText: this.i18n._("Connectivity Test Output"),
                    initComponent : function() {
                        Ung.NetworkTest.prototype.initComponent.apply(this, arguments);
                    },
                    getCommand: function() {
                        var script= [
                            'echo -n "Testing DNS ... " ; success="Successful";',
                            'dig updates.untangle.com > /dev/null 2>&1; if [ "$?" = "0" ]; then echo "OK"; else echo "FAILED"; success="Failure"; fi;',
                            'echo -n "Testing TCP Connectivity ... ";',
                            'echo "GET /" | netcat -q 0 -w 15 updates.untangle.com 80 > /dev/null 2>&1;', 
                            'if [ "$?" = "0" ]; then echo "OK"; else echo "FAILED"; success="Failure"; fi;',
                            'echo "Test ${success}!"'
                        ];
                        return ["/bin/bash","-c", script.join("")];
                    }
                });
                this.subCmps.push(this.connectivityTest);
            }
            this.connectivityTest.show();
        },
        testConnectivity: function () {
            Ext.MessageBox.wait( this.i18n._( "Testing Internet Connectivity" ), this.i18n._( "Please wait" ));
            var script = [
                'dig updates.untangle.com > /dev/null 2>&1;',
                'if [ "$?" != "0" ]; then echo "'+this.i18n._('Failed to connect to the Internet, DNS failed.')+'"; exit 1; fi;',
                'echo "GET /" | netcat -q 0 -w 15 updates.untangle.com 80 > /dev/null 2>&1;', 
                'if [ "$?" != "0" ]; then echo "'+this.i18n._('Failed to connect to the Internet, TCP failed.')+'"; exit 1; fi;',
                'echo "'+this.i18n._('Successfully connected to the Internet.')+'";'
            ];
            var command =  "/bin/bash -c " + script.join("");
            var execResultReader = null; 
            main.getExecManager().exec(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                Ext.MessageBox.alert(this.i18n._("Test Connectivity Result"), result.output);
            }, this), command);  
        },
        openPingTest: function(destination) {
            if(!this.pingTest) {
                this.pingTest = Ext.create('Ung.NetworkTest',{
                    helpSource: 'ping_test',
                    settingsCmp: this,
                    title: this.i18n._('Ping Test'),
                    testDescription: this.i18n._("The <b>Ping Test</b> can be used to test that a particular host or client can be pinged"),
                    testErrorMessage : this.i18n._( "Unable to complete the Ping Test." ),
                    testEmptyText: this.i18n._("Ping Test Output"),
                    initComponent : function() {
                        this.testTopToolbar = [this.destination = new Ext.form.TextField({
                            xtype : "textfield",
                            width:150,
                            emptyText : this.settingsCmp.i18n._( "IP Address or Hostname" )
                        })];
                        Ung.NetworkTest.prototype.initComponent.apply(this, arguments);
                    },
                    getCommand: function() {
                        var destination = this.destination.getValue();
                        return "ping -c 5 "+destination;
                    },
                    enableParameters : function( isEnabled ){
                        if ( isEnabled ) {
                            this.destination.enable();
                        } else {
                            this.destination.disable();
                        }
                    },
                    isValid : function() {
                        var destination = this.destination.getValue();
                        
                        if ( Ext.isEmpty(destination) /*TODO: verify host or ip
                             ( !Ext.form.VTypes.ipAddress( destination, this.destination ) && 
                               !Ext.form.VTypes.hostname( destination, this.destination ))*/) {
                            Ext.MessageBox.show({
                                title : this.settingsCmp.i18n._( "Warning" ),
                                msg : this.settingsCmp.i18n._( "Please enter a valid IP Address or Hostname" ),
                                icon : Ext.MessageBox.WARNING,
                                buttons : Ext.MessageBox.OK
                            });
                            return false;
                        }

                        return true;
                    }
                });
                this.subCmps.push(this.pingTest);
            }
            if(destination !== undefined) {
                this.pingTest.destination.setValue(destination);
            }
            this.pingTest.show();
        },
        openDnsTest: function() {
            if(!this.dnsTest) {
                this.dnsTest = Ext.create('Ung.NetworkTest',{
                    helpSource: 'dns_test',
                    settingsCmp: this,
                    title: this.i18n._('DNS Test'),
                    testDescription: this.i18n._("The <b>DNS Test</b> can be used to test DNS lookups"),
                    testErrorMessage : this.i18n._( "Unable to complete DNS test." ),
                    testEmptyText: this.i18n._("DNS Test Output"),
                    initComponent : function() {
                        this.testTopToolbar = [this.destination = new Ext.form.TextField({
                            xtype : "textfield",
                            width:150,
                            emptyText : this.settingsCmp.i18n._( "Hostname" )
                        })];
                        Ung.NetworkTest.prototype.initComponent.apply(this, arguments);
                    },
                    getCommand: function() {
                        var destination = this.destination.getValue();
                        var script=['host '+ destination+';',
                            'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'];
                        return ["/bin/bash","-c", script.join("")];
                        
                        
                    },
                    enableParameters : function( isEnabled ){
                        if ( isEnabled ) {
                            this.destination.enable();
                        } else {
                            this.destination.disable();
                        }
                    },
                    isValid : function() {
                        var destination = this.destination.getValue();
                        if ( Ext.isEmpty(destination) /*TODO: verify host or ip
                             ( !Ext.form.VTypes.ipAddress( destination, this.destination ) && 
                               !Ext.form.VTypes.hostname( destination, this.destination ))*/) {
                            Ext.MessageBox.show({
                                title : this.settingsCmp.i18n._( "Warning" ),
                                msg : this.settingsCmp.i18n._( "Please enter a valid Hostname" ),
                                icon : Ext.MessageBox.WARNING,
                                buttons : Ext.MessageBox.OK
                            });
                            return false;
                        }
                        return true;
                    }
                });
                this.subCmps.push(this.dnsTest);
            }
            this.dnsTest.show();
        },        
        openTcpTest: function(destination, port) {
            if(!this.tcpTest) {
                this.tcpTest = Ext.create('Ung.NetworkTest',{
                    helpSource: 'tcp_test',
                    settingsCmp: this,
                    title: this.i18n._('Connection Test'),
                    testDescription: this.i18n._("The <b>Connection Test</b> verifies that Untangle can open a TCP connection to a port on the given host or client."),
                    testErrorMessage : this.i18n._( "Unable to complete Connection test." ),
                    testEmptyText: this.i18n._("Connection Test Output"),
                    initComponent : function() {
                        this.testTopToolbar = [this.destination = new Ext.form.TextField({
                            xtype : "textfield",
                            width:150,
                            emptyText : this.settingsCmp.i18n._( "IP Address or Hostname" )
                        }), this.port = new Ext.form.field.Number({
                            xtype : "numberfield",
                            minValue : 1,
                            maxValue : 65536,
                            width: 60,
                            emptyText : this.settingsCmp.i18n._( "Port" )
                        })];
                        Ung.NetworkTest.prototype.initComponent.apply(this, arguments);
                    },
                    getCommand: function() {
                        var destination = this.destination.getValue();
                        var port = this.port.getValue();
                        var script=['echo 1 | netcat -q 0 -v -w 15 ' + destination + ' ' + port +';',
                            'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'];
                        return ["/bin/sh","-c", script.join("")];
                    },
                    enableParameters : function( isEnabled ){
                        if ( isEnabled ) {
                            this.destination.enable();
                            this.port.enable();
                        } else {
                            this.destination.disable();
                            this.port.disable();
                        }
                    },
                    isValid : function() {
                        var destination = this.destination.getValue();
                        var port = this.port.getValue();
                        
                        if ( Ext.isEmpty(destination) /*TODO: verify host or ip
                             ( !Ext.form.VTypes.ipAddress( destination, this.destination ) && 
                               !Ext.form.VTypes.hostname( destination, this.destination ))*/) {
                            Ext.MessageBox.show({
                                title : this.settingsCmp.i18n._( "Warning" ),
                                msg : this.settingsCmp.i18n._( "Please enter a valid IP Address or Hostname" ),
                                icon : Ext.MessageBox.WARNING,
                                buttons : Ext.MessageBox.OK
                            });
                            return false;
                        }
                        return true;
                    }
                });
                this.subCmps.push(this.tcpTest);
            }
            if(destination !== undefined) {
                this.tcpTest.destination.setValue(destination);
            }
            if(port !== undefined) {
                this.tcpTest.port.setValue(port);
            }
            this.tcpTest.show();
        },
        openTracerouteTest: function() {
            if(!this.tracerouteTest) {
                this.tracerouteTest = Ext.create('Ung.NetworkTest',{
                    helpSource: 'traceroute_test',
                    settingsCmp: this,
                    title: this.i18n._('Traceroute Test'),
                    testDescription: this.i18n._("The <b>Traceroute Test</b> traces the route to a given host or client."),
                    testErrorMessage : this.i18n._( "Unable to complete the Traceroute Test." ),
                    testEmptyText: this.i18n._("Traceroute Test Output"),
                    initComponent : function() {
                        this.testTopToolbar = [this.destination = new Ext.form.TextField({
                            xtype : "textfield",
                            width:150,
                            emptyText : this.settingsCmp.i18n._( "IP Address or Hostname" )
                        })];
                        Ung.NetworkTest.prototype.initComponent.apply(this, arguments);
                    },
                    getCommand: function() {
                        var destination = this.destination.getValue();
                        var script = ['traceroute '+destination + ';',
                          'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'];
                        return ["/bin/sh","-c", script.join("")];
                    },
                    enableParameters : function( isEnabled ){
                        if ( isEnabled ) {
                            this.destination.enable();
                        } else {
                            this.destination.disable();
                        }
                    },
                    isValid : function() {
                        var destination = this.destination.getValue();
                        
                        if ( Ext.isEmpty(destination) /*TODO: verify host or ip
                             ( !Ext.form.VTypes.ipAddress( destination, this.destination ) && 
                               !Ext.form.VTypes.hostname( destination, this.destination ))*/) {
                            Ext.MessageBox.show({
                                title : this.settingsCmp.i18n._( "Warning" ),
                                msg : this.settingsCmp.i18n._( "Please enter a valid IP Address or Hostname" ),
                                icon : Ext.MessageBox.WARNING,
                                buttons : Ext.MessageBox.OK
                            });
                            return false;
                        }
                        return true;
                    }
                });
                this.subCmps.push(this.tracerouteTest);
            }
            this.tracerouteTest.show();
        },
        openPacketTest: function() {
            if(!this.packetTest) {
                this.packetTest = Ext.create('Ung.NetworkTest',{
                    helpSource: 'packet_test',
                    settingsCmp: this,
                    title: this.i18n._('Packet Test'),
                    testDescription: this.i18n._("The <b>Packet Test</b> can be used to view packets on the network wire for troubleshooting."),
                    testErrorMessage : this.i18n._( "Unable to complete the Packet Test." ),
                    testEmptyText: this.i18n._("Packet Test Output"),
                    initComponent : function() {
                        var timeouts = [[ 5, this.settingsCmp.i18n._( "5 seconds" )],
                                        [ 30, this.settingsCmp.i18n._( "30 seconds" )],
                                        [ 120, this.settingsCmp.i18n._( "120 seconds" )]];
                        var interfaceStore =Ung.Util.getInterfaceList(false, false); 
                        this.testTopToolbar = [this.destination = new Ext.form.TextField({
                            xtype : "textfield",
                            value : "any",
                            width:150,
                            emptyText : this.settingsCmp.i18n._( "IP Address or Hostname" )
                        }), this.port = new Ext.form.field.Number({
                            xtype : "numberfield",
                            minValue : 1,
                            maxValue : 65536,
                            width: 60,
                            emptyText : this.settingsCmp.i18n._( "Port" )
                        }), this.intf = new Ext.form.field.ComboBox({
                            xtype : "combo",
                            editable : false,
                            style : "margin-left: 10px",
                            width : 100,
                            value : interfaceStore[0][0],
                            store : interfaceStore
                        }),{
                            xtype : "label",
                            html : this.settingsCmp.i18n._("Timeout:"),
                            style : "margin-left: 18px"
                        }, this.timeout = new Ext.form.ComboBox({
                            xtype : "combo",
                            style : "margin-left: 2px",
                            value : timeouts[0][0],
                            editable : false,
                            width : 100,
                            store : timeouts
                        })];
                        Ung.NetworkTest.prototype.initComponent.apply(this, arguments);
                    },
                    getCommand: function() {
                        var destination = this.destination.getValue();
                        var port = this.port.getValue();
                        var intf = this.intf.getValue();
                        var timeout = this.timeout.getValue();
                        if(destination === null || destination.toLowerCase() == "any") {
                            destination = "";
                        }
                        if(port === null) {
                            port = "";
                        }
                        if(destination !== "" && destination != null) {
                            destination = "host "+destination;
                        }
                        if(port !== "") {
                            port = "port " + port;
                        }
                        if(destination !== "" && port !== "") {
                            port = "and "+port;
                        }
                        var script = [
                            'intf_name='+intf+';',
                            /*'pppoe_name=`/usr/share/untangle-net-alpaca/scripts/get_pppoe_name ${intf_name}`;',
                            'if [ "${pppoe_name}" != "ppp.${intf_name}" ]; then intf_name=${pppoe_name}; fi;',*/
                            'tcpdump -i ${intf_name} -l -q -c 1024 -v -n ' + destination + ' '+port+' 2>&1 & echo "";',
                            'for t in `seq 1 ' + timeout + '`; do sleep 1;',
                            '  ps aux | grep -q " $! .*[t]cpdump -i";',
                            '  if [ "$?" != "0" ]; then break; fi;',
                            'done;',
                            'ps aux | grep -q " $! .*[t]cpdump -i" && kill -INT $!;',
                            'ps aux | grep -q " $! .*[t]cpdump -i" && wait $!;'
                        ];
                        return ["/bin/bash","-c", script.join("")];
                    },
                    enableParameters : function( isEnabled ){
                        if ( isEnabled ) {
                            this.destination.enable();
                            this.port.enable();
                            this.intf.enable();
                            this.timeout.enable();
                        } else {
                            this.destination.disable();
                            this.port.disable();
                            this.intf.disable();
                            this.timeout.disable();
                        }
                    },
                    isValid : function() {
                        var destination = this.destination.getValue();
                        
                        if ( Ext.isEmpty(destination) /*TODO: verify host or ip
                             ( !Ext.form.VTypes.ipAddress( destination, this.destination ) && 
                               !Ext.form.VTypes.hostname( destination, this.destination ))*/) {
                            Ext.MessageBox.show({
                                title : this.settingsCmp.i18n._( "Warning" ),
                                msg : this.settingsCmp.i18n._( "Please enter a valid IP Address or Hostname" ),
                                icon : Ext.MessageBox.WARNING,
                                buttons : Ext.MessageBox.OK
                            });
                            return false;
                        }
                        return true;
                    }
                });
                this.subCmps.push(this.packetTest);
            }
            this.packetTest.show();
        },        
        validate: function() {
            if(this.settings.qosSettings.qosEnabled) {
                var qosBandwidthList = this.gridQosWanBandwidth.getPageList();
                for(var i=0; i<qosBandwidthList.length; i++) {
                    var qosBandwidth = qosBandwidthList[i]; 
                    if(qosBandwidth.configType == "ADDRESSED" &&
                       qosBandwidth.isWan &&
                       ( Ext.isEmpty(qosBandwidth.downloadBandwidthKbps) || Ext.isEmpty(qosBandwidth.uploadBandwidthKbps) )) {
                        this.tabs.setActiveTab(this.panelAdvanced);
                        this.advancedTabPanel.setActiveTab(this.panelQoS);
                        this.gridQosWanBandwidth.focus();
                        Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("QoS is Enabled. Please set valid Download Bandwidth and Upload Bandwidth limits in WAN Bandwidth for all WAN interfaces."));
                        return false;
                    }
                }
            }
            return true;
        },
        needRackReload: false,
        save: function (isApply) {
            this.saveSemaphore = 1;
            this.needRackReload = true;
            // save language settings
            main.getNetworkManager().setNetworkSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.settings);
        },
        refreshSettings: function() {
            this.settings = main.getNetworkManager().getNetworkSettings();
            var deviceStatus=main.getNetworkManager().getDeviceStatus();
            var deviceStatusMap=Ung.Util.createRecordsMap(deviceStatus.list, "deviceName");
            var interfaceStatus=main.getNetworkManager().getInterfaceStatus();
            var interfaceStatusMap=Ung.Util.createRecordsMap(interfaceStatus.list, "interfaceId");
            for(var i=0; i<this.settings.interfaces.list.length; i++) {
                var intf=this.settings.interfaces.list[i];
                var deviceStatusInner = deviceStatusMap[intf.physicalDev];
                Ext.applyIf(intf, deviceStatusInner);
                var interfaceStatusInner = interfaceStatusMap[intf.interfaceId];
                Ext.applyIf(intf, interfaceStatusInner);
            }
            rpc.networkSettings = this.settings;
        },
        beforeSave: function(isApply, handler) {
            this.beforeSaveCount = 13;

            Ext.MessageBox.wait(this.i18n._("Applying Network Settings..."), this.i18n._("Please wait"));

            this.gridInterfaces.getList(Ext.bind(function(saveList) {
                var i;
                this.settings.interfaces = saveList;
                var qosBandwidthList = this.gridQosWanBandwidth.getPageList();
                var qosBandwidthMap = {};
                for(i=0; i<qosBandwidthList.length; i++) {
                    qosBandwidthMap[qosBandwidthList[i].interfaceId] = qosBandwidthList[i];
                }
                for(i=0; i<this.settings.interfaces.list.length; i++) {
                    var intf=this.settings.interfaces.list[i];
                    var intfBandwidth = qosBandwidthMap[intf.interfaceId];
                    if(intfBandwidth) {
                        intf.downloadBandwidthKbps=intfBandwidth.downloadBandwidthKbps;
                        intf.uploadBandwidthKbps=intfBandwidth.uploadBandwidthKbps;
                    }
                }

                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));

            this.gridPortForwardRules.getList(Ext.bind(function(saveList) {
                this.settings.portForwardRules = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));

            this.gridNatRules.getList(Ext.bind(function(saveList) {
                this.settings.natRules = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));

            this.gridBypassRules.getList(Ext.bind(function(saveList) {
                this.settings.bypassRules = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));

            this.gridStaticRoutes.getList(Ext.bind(function(saveList) {
                this.settings.staticRoutes = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));
            
            this.gridQosRules.getList(Ext.bind(function(saveList) {
                this.settings.qosSettings.qosRules = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));
            
            this.gridQosPriorities.getList(Ext.bind(function(saveList) {
                this.settings.qosSettings.qosPriorities = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));
            
            this.gridForwardFilterRules.getList(Ext.bind(function(saveList) {
                this.settings.forwardFilterRules = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));

            this.gridInputFilterRules.getList(Ext.bind(function(saveList) {
                this.settings.inputFilterRules = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));
            
            this.gridDnsStaticEntries.getList(Ext.bind(function(saveList) {
                this.settings.dnsSettings.staticEntries = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));

            this.gridDnsLocalServers.getList(Ext.bind(function(saveList) {
                this.settings.dnsSettings.localServers = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));

            this.gridDhcpStaticEntries.getList(Ext.bind(function(saveList) {
                this.settings.staticDhcpEntries = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));
            
            this.gridNetworkCards.getList(Ext.bind(function(saveList) {
                this.settings.devices = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));
        },
        afterSave: function(exception, isApply) {
            if(Ung.Util.handleException(exception)) return;

            delete rpc.networkSettings; // clear cached settings object
            
            this.saveSemaphore--;
            if (this.saveSemaphore === 0) {
                if(isApply) {
                    //On apply we have to reload all and keep selected tabs
                    var activeTabIndex = this.tabs.items.findIndex('id', this.tabs.getActiveTab().id);
                    var advancedTabIndex =this.advancedTabPanel.items.findIndex('id', this.advancedTabPanel.getActiveTab().id);
                    Ung.Network.superclass.closeWindow.call(this);
                    main.openConfig(main.config[0]);  
                    Ext.defer(function() {
                        Ung.NetworkSettingsCmp.needRackReload=true;
                        Ung.NetworkSettingsCmp.tabs.setActiveTab(activeTabIndex);
                        Ung.NetworkSettingsCmp.advancedTabPanel.setActiveTab(advancedTabIndex);
                    },10);
                } else {
                    Ext.MessageBox.hide();
                    this.closeWindow();
                }
            }
        },
        closeWindow: function() {
            Ung.Network.superclass.closeWindow.call(this);
            if (this.needRackReload) {
                Ung.Util.goToStartPage();
            }
        }        
    });
}
//@ sourceURL=network.js
