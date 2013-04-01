if (!Ung.hasResource["Ung.Network"]) {
    Ung.hasResource["Ung.Network"] = true;

    Ung.NetworkUtil={
        getPortForwardMatchers: function (settingsCmp) {
            return [
                {name:"DST_LOCAL",displayName: settingsCmp.i18n._("Destined Local"), type: "boolean", visible: true},
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
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
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"]], visible: false, allowInvert: false}
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
                {name:"DST_ADDR",displayName: settingsCmp.i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"DST_PORT",displayName: settingsCmp.i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
                {name:"DST_INTF",displayName: settingsCmp.i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
                {name:"SRC_ADDR",displayName: settingsCmp.i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
                {name:"SRC_PORT",displayName: settingsCmp.i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: false},
                {name:"SRC_INTF",displayName: settingsCmp.i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true, allowInvert: false},
                {name:"PROTOCOL",displayName: settingsCmp.i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"]], visible: true, allowInvert: false}
            ];
        }
    };
    
    Ext.define("Ung.Network", {
        extend: "Ung.ConfigWin",
        gridPortForwardRules: null,
        gridNatRules: null,
        gridBypassRules: null,
        gridStaticRoutes: null,
        panelInterfaces: null,
        panelHostName: null,
        panelPortForwardRules: null,
        panelNatRules: null,
        panelRoutes: null,
        panelAdvanced: null,
        initComponent: function() {
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
            this.buildPortForwardRules();
            this.buildNatRules();
            this.buildBypassRules();
            this.buildRoutes();
            this.buildAdvanced();
            
            // builds the tab panel with the tabs
            var pageTabs = [ this.panelInterfaces, this.panelHostName, this.panelPortForwardRules, this.panelNatRules, this.panelBypassRules, this.panelRoutes, this.panelAdvanced ];
            this.buildTabPanel(pageTabs);

            //Check if QoS is enabled and there are some initial WANs without downloadBandwidthKbps or uploadBandwidthKbps limits set and mark dirty if true,
            // in order to make the user save the valid settings when new WANs are added
            if(this.settings.qosSettings.qosEnabled) {
                for(var i=0; i<this.settings.interfaces.list.length; i++) {
                    var intf =this.settings.interfaces.list[i];
                    if(intf.isWan && (intf.downloadBandwidthKbps == null || intf.uploadBandwidthKbps == null)) {
                        this.markDirty();
                        break;
                    }
                }
            }

            this.callParent(arguments);
        },
        // Interfaces Panel
        buildInterfaces: function() {
            this.gridInterfaces = Ext.create('Ung.EditorGrid',{
                anchor: '100% -80',
                name: 'Interfaces',
                settingsCmp: this,
                paginated: false,
                hasReorder: false,
                hasDelete: false,
                hasAdd: false,
                title: this.i18n._("Interfaces"),
                recordJavaClass: "com.untangle.uvm.network.InterfaceSettings",
                columnsDefaultSortable: false,
                dataProperty: "interfaces",
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
                },{
                    name: 'isWan'
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
                    name: 'javaClass'
                }, { name: "macAddress" }, { name: "connected" }, { name: "duplex" }, { name: "vendor" }, { name: "mbit" }],
                columns: [{
                    header: this.i18n._("Interface Id"),
                    width: 80,
                    dataIndex: 'interfaceId'
                }, {
                    header: this.i18n._("Name"),
                    dataIndex: 'name',
                    width:100
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
                            divClass = "ua-cell-enabled-interface";
                        } else if ( value == "DISCONNECTED" ) {
                            connectedStr = this.i18n._("disconnected");
                        }
                        return "<div class='" + divClass + "'>" + connectedStr + "</div>";
                    }, this)
                }, {
                    header: this.i18n._("Physical Dev"),
                    dataIndex: 'physicalDev',
                    width:75
                }, {
                    header: this.i18n._("System Dev"),
                    dataIndex: 'systemDev',
                    width:75
                }, {
                    header: this.i18n._("Symbolic Dev"),
                    dataIndex: 'symbolicDev',
                    width:75
                }, {
                    header: this.i18n._("Config"),
                    dataIndex: 'configType',
                    width:100
                }, {
                    header: this.i18n._("is WAN"),
                    dataIndex: 'isWan',
                    width:55
                }],
                bbar: [{
                    xtype: "button",
                    name: "remap_interfaces",
                    iconCls: 'icon-refresh',
                    text: this.i18n._("Remap Interfaces"),
                    handler: Ext.bind(function() {
                        this.gridInterfaces.onMapDevices();
                    }, this)
                }],
                onMapDevices: Ext.bind(function() {
                    Ext.MessageBox.wait(i18n._("Loading device mapper..."), i18n._("Please wait"));
                    if (!this.winMapDevices) {
                        this.mapDevicesStore = Ext.create('Ext.data.ArrayStore', {
                            fields:[{name: "interfaceId"}, { name: "name" }, { name: "physicalDev" }, { name: "systemDev" },{ name: "symbolicDev" }, { name: "macAddress" }, { name: "connected" }, { name: "duplex" }, { name: "vendor" }, { name: "mbit" }],
                            data: []
                        });
                        this.availableDevicesStore = Ext.create('Ext.data.ArrayStore', {
                            fields:[{ name: "physicalDev" }],
                            data: []
                        });

                        this.gridMapDevices = Ext.create('Ext.grid.Panel', {
                            anchor: '100% -80',
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
                            viewConfig:{
                               forceFit: true,
                               disableSelection: false,
                               plugins:{
                                    ptype: 'gridviewdragdrop',
                                    dragText: i18n._('Drag and drop to reorganize')
                                },
                                listeners: {
                                    "drop": {
                                        fn:  Ext.bind(function(node, data, overModel, dropPosition, eOpts) {
                                            var sm = this.gridMapDevices.getSelectionModel();
                                            var rows=sm.getSelection();

                                            if ( rows.length != 1 ) {
                                                return false;
                                            }
                                            var intfId = rows[0].get("interfaceId");
                                            var intfName = rows[0].get("name");
                                            var origIntfId = overModel.get("interfaceId");
                                            var origIntfName = overModel.get("name");

                                            this.mapDevicesStore.each( function( currentRow ) {
                                                if ( currentRow == overModel) {
                                                    currentRow.set("interfaceId", intfId);
                                                    currentRow.set("name", intfName);
                                                }
                                                if ( currentRow == rows[0]) {
                                                    currentRow.set("interfaceId", origIntfId);
                                                    currentRow.set("name", origIntfName);
                                                }
                                            });
                                            sm.clearSelections();
                                            return true;
                                        },this )
                                    }
                                }
                            },
                            columns: [{
                                header: i18n._( "Name" ),
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
                                header: i18n._( "Device" ),
                                dataIndex: 'physicalDev',
                                sortable: false,
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
                                                    if(sourceRecord!=null && targetRecord!=null) {
                                                        return false;
                                                    }
                                                });
                                                if(sourceRecord==null || targetRecord==null || sourceRecord==targetRecord) {
                                                    console.log(sourceRecord, targetRecord);
                                                    return false;
                                                }
                                                var soruceData = Ext.decode(Ext.encode(sourceRecord.data));
                                                var targetData = Ext.decode(Ext.encode(targetRecord.data));
                                                
                                                sourceRecord.set("systemDev", targetData.systemDev);
                                                sourceRecord.set("symbolicDev", targetData.symbolicDev);
                                                sourceRecord.set("macAddress",targetData.macAddress);
                                                sourceRecord.set("connected",targetData.connected);
                                                sourceRecord.set("duplex",targetData.duplex);
                                                sourceRecord.set("vendor",targetData.vendor);
                                                sourceRecord.set("mbit",targetData.mbit);

                                                targetRecord.set("physicalDev", soruceData.physicalDev);
                                                targetRecord.set("systemDev", soruceData.systemDev);
                                                targetRecord.set("symbolicDev", soruceData.symbolicDev);
                                                targetRecord.set("macAddress",soruceData.macAddress);
                                                targetRecord.set("connected",soruceData.connected);
                                                targetRecord.set("duplex",soruceData.duplex);
                                                targetRecord.set("vendor",soruceData.vendor);
                                                targetRecord.set("mbit",soruceData.mbit);
                                            }, this)
                                        }
                                    }
                                },
                                width:200
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
                                header: this.i18n._( "Speed" ),
                                dataIndex: 'mbit',
                                sortable: false,
                                width: 100
                            }, {
                                header: this.i18n._( "Duplex" ),
                                dataIndex: 'duplex',
                                sortable: false,
                                width: 100,
                                renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                                    return (value=="FULL_DUPLEX")?this.i18n._("full-duplex") : (value=="HALF_DUPLEX") ? this.i18n._("half-duplex") : this.i18n._("unknown");
                                }, this)
                            }, {
                                header: this.i18n._( "Vendor" ),
                                dataIndex: 'vendor',
                                sortable: false,
                                width: 180
                            }, {
                                header: this.i18n._( "MAC Address" ),
                                dataIndex: 'macAddress',
                                sortable: false,
                                width: 150,
                                renderer: function(value, metadata, record, rowIndex, colIndex, store, view) {
                                    var text = ""
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
                                layout: 'anchor',
                                items: [{
                                    xtype: 'fieldset',
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
                                    currentRow.set("physicalDev",interfaceData.physicalDev);
                                    currentRow.set("systemDev",interfaceData.systemDev);
                                    currentRow.set("symbolicDev",interfaceData.symbolicDev);
                                    currentRow.set("macAddress",interfaceData.macAddress);
                                    currentRow.set("connected",interfaceData.connected);
                                    currentRow.set("duplex",interfaceData.duplex);
                                    currentRow.set("vendor",interfaceData.vendor);
                                    currentRow.set("mbit",interfaceData.mbit);
                                });
                                this.winMapDevices.cancelAction();
                            }, this)
                        });
                    }
                    Ext.MessageBox.hide();
                    var interfaces = this.gridInterfaces.getPageList();
                    this.mapDevicesStore.loadData( interfaces );
                    this.availableDevicesStore.loadData( interfaces );
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
                    currentRow.suspendEvents();
                    currentRow.set("isWan", interfaceData.isWan);
                    currentRow.set("name", interfaceData.name);
                    currentRow.set("configType", interfaceData.configType);
                    currentRow.resumeEvents();
                });
                qosBandwidthStore.filter([{property: "configType", value: "ADDRESSED"}, {property:"isWan", value: true}]);
                this.gridQosWanBandwidth.updateTotalBandwidth();
            }, this));
            
            this.panelInterfaces = Ext.create('Ext.panel.Panel',{
                name: 'panelInterfaces',
                helpSource: 'network_interfaces',
                parentId: this.getId(),
                title: this.i18n._('Interfaces'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Note'),
                    html: this.i18n._("<b>Interfaces</b> are legit. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
                }, this.gridInterfaces]
            });
            this.gridInterfacesAliasesEditor = Ext.create('Ung.EditorGrid',{
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
                    "v4StaticAddress": "1.2.3.4",
                    "v4StaticPrefix": "24",
                    "javaClass": "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias"
                },
                fields: [{
                    name: 'v4StaticAddress'
                }, {
                    name: 'v4StaticPrefix'
                }],
                columns: [{
                    header: this.i18n._("Address"),
                    dataIndex: 'v4StaticAddress',
                    width:200,
                    editor : {
                        xtype: 'textfield',
                        vtype: 'ip4Address',
                        allowBlank: false
                    }
                }, {
                    header: this.i18n._("Netmask / Prefix"),
                    dataIndex: 'v4StaticPrefix',
                    flex: 1,
                    editor : {
                        xtype: 'textfield',
                        allowBlank: false
                    }
                }],
                columnsDefaultSortable: false,
                setValue: function (value) {
                    var data = [];
                    if(value!=null && value.list!=null) {
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
                            fn: Ext.bind(function(combo, ewVal, oldVal) {
                                this.gridInterfaces.rowEditor.syncRuleEditorComponents();
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
                                this.gridInterfaces.rowEditor.syncRuleEditorComponents();
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
                                    fn: Ext.bind(function(combo, ewVal, oldVal) {
                                        this.gridInterfaces.rowEditor.syncRuleEditorComponents();
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
                            fieldLabel: i18n._( "Netmask" ),
                            store: Ung.Util.getV4NetmaskList( false ),
                            queryMode: 'local',
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
                            xtype:'textfield',
                            dataIndex: "v4AutoAddressOverride",
                            fieldLabel: this.i18n._("Address Override"),
                            vtype: "ip4Address"
                        }, {
                            xtype: "combo",
                            dataIndex: "v4AutoPrefixOverride",
                            fieldLabel: this.i18n._("Netmask Override"),
                            store: Ung.Util.getV4NetmaskList( true ),
                            valueField: "value",
                            displayField: "displayName",
                            queryMode: 'local',
                            editable: false
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4AutoGatewayOverride",
                            fieldLabel: this.i18n._("Gateway Override"),
                            vtype: "ip4Address"
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4AutoDns1Override",
                            fieldLabel: this.i18n._("Primary DNS Override"),
                            vtype: "ip4Address"
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4AutoDns2Override",
                            fieldLabel: this.i18n._("Secondary DNS Override"),
                            vtype: "ip4Address"
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4PPPoEUsername",
                            fieldLabel: this.i18n._("Username")
                        }, {
                            xtype:'textfield',
                            inputType:'password',
                            dataIndex: "v4PPPoEPassword",
                            fieldLabel: this.i18n._("Password")
                        }, {
                            xtype:'checkbox',
                            dataIndex: "v4PPPoEUsePeerDns",
                            fieldLabel: this.i18n._("Use Peer DNS"),
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.gridInterfaces.rowEditor.syncRuleEditorComponents();
                                    }, this)
                                }
                            }
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4PPPoEDns1",
                            fieldLabel: this.i18n._("Primary DNS"),
                            vtype: "ip4Address"
                        }, {
                            xtype:'textfield',
                            dataIndex: "v4PPPoEDns2",
                            fieldLabel: this.i18n._("Secondary DNS"),
                            vtype: "ip4Address"
                        }]
                    }, {
                        xtype: 'fieldset',
                        title: this.i18n._("IPv4 Aliases"),
                        items: [this.gridInterfacesAliasesEditor]
                    }, {
                        xtype: 'fieldset',
                        title: this.i18n._("IPv4 Options"),
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
                    collapsed: true,
                    items: [{
                        xtype: "combo",
                        dataIndex: "v6ConfigType",
                        allowBlank: false, 
                        fieldLabel: this.i18n._("Config Type"),
                        editable: false,
                        store: [ ["AUTO", this.i18n._('Auto (SLAAC/RA)')], ["STATIC", this.i18n._('Static')] ],
                        valueField: "value",
                        displayField: "displayName",
                        queryMode: 'local',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.gridInterfaces.rowEditor.syncRuleEditorComponents();
                                }, this)
                            }
                        }
                    }, {
                        xtype:'textfield',
                        dataIndex: "v6StaticAddress",
                        
                        fieldLabel: this.i18n._("Address"),
                        vtype: "ip6Address",
                        width: 350
                    }, {
                        xtype:'textfield',
                        dataIndex: "v6StaticPrefixLength",
                        fieldLabel: this.i18n._("Prefix Length"),
                        width: 150
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
                    }]
                }, {
                    xtype: 'fieldset',
                    name: 'dhcp',
                    border: true,
                    title: this.i18n._("DHCP Configuration"),
                    collapsible: true,
                    collapsed: false,
                    items: [{
                        xtype:'checkbox',
                        dataIndex: "dhcpEnabled",
                        boxLabel: this.i18n._("Enable DHCP")
                    }, {
                        xtype: 'textfield',
                        dataIndex: "dhcpRangeStart",
                        fieldLabel: this.i18n._("Range Start"),
                        vtype: "ip4Address"
                    }, {
                        xtype:'textfield',
                        dataIndex: "dhcpRangeEnd",
                        fieldLabel: this.i18n._("Range End"),
                        vtype: "ip4Address"
                    }, {
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: 'textfield',
                            dataIndex: "dhcpLeaseDuration",
                            fieldLabel: this.i18n._("Lease Duration")
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
                            width: 300
                        },
                        items: [{
                            xtype:'textfield',
                            dataIndex: "dhcpGatewayOverride",
                            fieldLabel: this.i18n._("Gateway Override"),
                            vtype: "ip4Address"
                        }, {
                            xtype: "combo",
                            dataIndex: "dhcpPrefixOverride",
                            fieldLabel: this.i18n._("Netmask Override"),
                            store: Ung.Util.getV4NetmaskList( true ),
                            valueField: "value",
                            displayField: "displayName",
                            queryMode: 'local',
                            editable: false
                        }, {
                            xtype:'textfield',
                            dataIndex: "dhcpDnsOverride",
                            fieldLabel: this.i18n._("DNS Override"),
                            vtype: "ip4Address"
                        }]
                    }]
                }, {
                    xtype: "combo",
                    dataIndex: "bridgedTo",
                    fieldLabel: this.i18n._("Bridged To"),
                    store: Ung.Util.getInterfaceAddressedList(),
                    width: 300,
                    queryMode: 'local',
                    editable: false
                }],
                syncRuleEditorComponents: function() {
                    var configTypeValue = this.query('combo[dataIndex="configType"]')[0].getValue();
                    var isWan = this.query('checkbox[dataIndex="isWan"]')[0];
                    var isWanValue = isWan.getValue();
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
                    
                    var v4AutoAddressOverride = this.query('textfield[dataIndex="v4AutoAddressOverride"]')[0];
                    var v4AutoGatewayOverride = this.query('textfield[dataIndex="v4AutoGatewayOverride"]')[0];
                    var v4AutoPrefixOverride = this.query('combo[dataIndex="v4AutoPrefixOverride"]')[0];
                    var v4AutoDns1Override = this.query('textfield[dataIndex="v4AutoDns1Override"]')[0];
                    var v4AutoDns2Override = this.query('textfield[dataIndex="v4AutoDns2Override"]')[0];
                    
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
                    
                    // hide everything
                    isWan.hide();
                    bridgedTo.hide();
                    v4Config.hide();
                    v6Config.hide();
                    dhcp.hide();
                    
                    v4ConfigType.hide();
                    v6ConfigType.hide(); v6ConfigType.disable();
                    v4StaticAddress.hide(); v4StaticAddress.disable();
                    v4StaticPrefix.hide();
                    v4StaticGateway.hide(); v4StaticGateway.disable();
                    v4StaticDns1.hide(); v4StaticDns1.disable();
                    v4StaticDns2.hide();
                    v4AutoAddressOverride.hide();
                    v4AutoPrefixOverride.hide();
                    v4AutoGatewayOverride.hide();
                    v4AutoDns1Override.hide();
                    v4AutoDns2Override.hide();
                    v4PPPoEUsername.hide();
                    v4PPPoEPassword.hide();
                    v4PPPoEUsePeerDns.hide();
                    v4PPPoEDns1.hide();
                    v4PPPoEDns2.hide();
                    v4NatEgressTraffic.hide(); 
                    v4NatIngressTraffic.hide(); 

                    v6StaticAddress.hide(); v6StaticAddress.disable();
                    v6StaticPrefixLength.hide(); v6StaticPrefixLength.disable();
                    v6StaticGateway.hide(); v6StaticGateway.disable();
                    v6StaticDns1.hide();
                    v6StaticDns2.hide();

                    // if config disabled show nothing
                    if ( configTypeValue == "DISABLED") {
                        return;
                    }

                    // if config bridged just show the one field 
                    if ( configTypeValue == "BRIDGED") {
                        bridgedTo.show();
                        return;
                    }

                    // if config addressed show necessary options
                    if ( configTypeValue == "ADDRESSED") {
                        isWan.show();
                        v4Config.show();
                        v6Config.show();

                        // if not a WAN, must configure statically
                        // if a WAN, can use auto or static
                        if ( isWanValue ) {
                            v4ConfigType.show(); //show full config options for WANs
                            v6ConfigType.show(); v6ConfigType.enable(); //show full config options for WANs
                            v4NatEgressTraffic.show(); // show NAT egress option on WANs
                        } else {
                            v4ConfigType.setValue("STATIC"); //don't allow auto/pppoe for non-WAN
                            v6ConfigType.setValue("STATIC");  v6ConfigType.enable();//don't allow auto/pppoe for non-WAN
                            v4StaticGateway.hide(); v4StaticGateway.disable(); // no gateways for non-WAN
                            v6StaticGateway.hide(); v6StaticGateway.disable();// no gateways for non-WAN
                            v4NatIngressTraffic.show(); // show NAT ingress options on non-WANs
                            dhcp.show(); // show DHCP options on non-WANs
                        }
                        
                        // if static show static fields
                        // if auto show override fields (auto is only allowed on WANs)
                        // if pppoe show pppoe fields (pppoe is only allowed on WANs)
                        if ( v4ConfigType.getValue() == "STATIC" ) {
                            v4StaticAddress.show(); v4StaticAddress.enable();
                            v4StaticPrefix.show();
                            if (isWanValue) {
                                v4StaticGateway.show(); v4StaticGateway.enable();
                                v4StaticDns1.show(); v4StaticDns1.enable();
                                v4StaticDns2.show();
                            }
                        } else if ( v4ConfigType.getValue() == "AUTO" ) {
                            v4AutoAddressOverride.show();
                            v4AutoPrefixOverride.show();
                            v4AutoGatewayOverride.show();
                            v4AutoDns1Override.show();
                            v4AutoDns2Override.show();
                        } else if ( v4ConfigType.getValue() == "PPPOE" ) {
                            v4PPPoEUsername.show();
                            v4PPPoEPassword.show();
                            v4PPPoEUsePeerDns.show();
                            if ( v4PPPoEUsePeerDns.getValue() == false ) {
                                v4PPPoEDns1.show();
                                v4PPPoEDns2.show();
                            }
                        }

                        // if static show static fields
                        // if auto show override fields
                        if ( v6ConfigType.getValue() == "STATIC" ) {
                            v6StaticAddress.show(); v6StaticAddress.enable();
                            v6StaticPrefixLength.show(); v6StaticPrefixLength.enable();
                            if (isWanValue) {
                                v6StaticGateway.show(); v6StaticGateway.enable();
                                v6StaticDns1.show();
                                v6StaticDns2.show();
                            }
                        } else  { //auto
                            // no overriding in IPv6 so nothing to show
                        }
                    }
                },
                populate: function(record) {
                    // refresh interface selector store (may have changed since last display)
                    var bridgedTo = this.query('combo[dataIndex="bridgedTo"]')[0];
                    bridgedTo.getStore().loadData( Ung.Util.getInterfaceAddressedList() );
                    Ung.RowEditorWindow.prototype.populate.call(this, record);
                    this.syncRuleEditorComponents();
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
        // PortForwardRules Panel
        buildPortForwardRules: function() {
            this.gridPortForwardRules = Ext.create( 'Ung.EditorGrid', {
                anchor: '100% -80',
                name: 'Port Forward Rules',
                settingsCmp: this,
                paginated: false,
                hasReorder: true,
                addAtTop: false,
                emptyRow: {
                    "ruleId": -1,
                    "enabled": true,
                    "description": this.i18n._("[no description]"),
                    "javaClass": "com.untangle.uvm.network.PortForwardRule"
                },
                title: this.i18n._("Port Forward Rules"),
                recordJavaClass: "com.untangle.uvm.network.PortForwardRule",
                dataProperty:'portForwardRules',
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
                    flex:1,
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
                }],
                columnsDefaultSortable: false,
                rowEditorInputLines: [{
                    xtype:'checkbox',
                    name: "Enable Port Forward Rule",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enable Port Forward Rule")
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
                        javaClass: "com.untangle.uvm.network.PortForwardRuleMatcher",
                        anchor:"98%",
                        width: 900,
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getPortForwardMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype:'textfield',
                        name: "newDestination",
                        allowBlank: false,
                        dataIndex: "newDestination",
                        fieldLabel: this.i18n._("New Destination"),
                        vtype: 'ipAddress'
                    }, {
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype:'textfield',
                            name: "newPort",
                            allowBlank: true,
                            width: 200,
                            dataIndex: "newPort",
                            fieldLabel: this.i18n._("New Port"),
                            vtype: 'port'
                        }, {
                            xtype: 'label',
                            html: this.i18n._("(optional)"),
                            cls: 'boxlabel'
                        }]
                    }]
                }]
            });
            
            this.panelPortForwardRules = Ext.create('Ext.panel.Panel',{
                name: 'panelPortForwardRules',
                helpSource: 'network_port_forwards',
                parentId: this.getId(),
                title: this.i18n._('Port Forward Rules'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Note'),
                    html: this.i18n._(" <b>Port Forward Rules</b>. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
                },  this.gridPortForwardRules]
            });
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
                    flex:1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                columnsDefaultSortable: false,
                rowEditorInputLines: [{
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
                        anchor:"98%",
                        width: 900,
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
                                var newSource = this.gridNatRules.rowEditor.query('textfield[dataIndex="newSource"]')[0];
                                if (combo.value == true) /* Auto */ {
                                    newSource.disable();
                                    newSource.hide();
                                } else {
                                    newSource.enable();
                                    newSource.show();
                                }
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
                
                syncRuleEditorComponents: function () {
                    var natType  = this.query('combo[dataIndex="auto"]')[0];
                    var newSource = this.query('textfield[dataIndex="newSource"]')[0];

                    newSource.disable();
                    
                    switch(natType.value) {
                      case true:
                        break;
                      case false:
                        newSource.enable();
                        break;
                    }
                    newSource.setVisible(!newSource.disabled); 
                }
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
                    title: this.i18n._('Note'),
                    html: this.i18n._(" <b>NAT Rules</b>. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
                },  this.gridNatRules]
            });
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
                    flex:1,
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
                columnsDefaultSortable: false,
                rowEditorInputLines: [{
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
                        anchor:"98%",
                        width: 900,
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getBypassRuleMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype: "combo",
                        name: "bypass",
                        allowBlank: false,
                        dataIndex: "bypass",
                        fieldLabel: this.i18n._("Bypass"),
                        editable: false,
                        store: [[true,i18n._('Bypass')], [false,i18n._('Capture')]],
                        valueField: "value",
                        displayField: "displayName",
                        queryMode: 'local'
                    }]
                }]
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
                    title: this.i18n._('Note'),
                    html: this.i18n._(" <b>Bypass Rules</b>. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
                }, this.gridBypassRules]
            });
        },
        // Routes Panel
        buildRoutes: function() {
            var devList = [];
            if( Ung.Util.networkSettings == null ) {
                Ung.Util.networkSettings = main.getNetworkManager().getNetworkSettings();
            }
            for ( var c = 0 ; c < Ung.Util.networkSettings.interfaces.list.length ; c++ ) {
                var intf = Ung.Util.networkSettings.interfaces.list[c];
                var name = "Local on " + intf['systemDev'];
                var key = intf['systemDev'];
                devList.push( [ key, name ] );
            }
            this.gridStaticRoutes = Ext.create('Ung.EditorGrid', {
                anchor: "100% -80",
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
                    dataIndex: 'nextHop'
                }, {
                    header: this.i18n._("Description"),
                    width: 300,
                    dataIndex: 'description',
                    flex:1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                sortField: 'network',
                columnsDefaultSortable: true,
                rowEditorInputLines: [, {
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
                    fieldLabel: i18n._( "Netmask/Prefix" ),
                    store: Ung.Util.getV4NetmaskList( false ),
                    valueField: "value",
                    displayField: "displayName",
                    width: 300,
                    listWidth: 70,
                    queryMode: 'local',
                    editable: false
                }, {
                    xtype: "combo",
                    editable : true,
                    allowBlank: false,
                    dataIndex: "nextHop",
                    fieldLabel: i18n._("Next Hop"),
                    editable: true,
                    store: devList,
                    valueField: "value",
                    displayField: "displayName",
                    queryMode: 'local'
                }, {
                    xtype: 'fieldset',
                    cls: 'description',
                    html: this.i18n._("If <b>Next Hop</b> is an IP address that network will routed via the specified IP address.") + "<br/>" +
                        this.i18n._("If <b>Next Hop</b> is an interface that network will be routed <b>locally</b> on that interface.")
                }]
            });

            this.panelRoutes = Ext.create('Ext.panel.Panel',{
                name: 'panelRoutes',
                helpSource: 'network_route_rules',
                parentId: this.getId(),
                title: this.i18n._('Routes'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Note'),
                    html: this.i18n._(" <b>Static Routes</b> are global routes that control how traffic is routed by destination address. The most specific Static Route is taken for a particular packet, order is not important.")
                }, this.gridStaticRoutes]
            });
        },
        // Advanced Panel
        buildAdvanced: function() {

            this.buildGeneral();
            this.buildQoS();
            this.buildFilter();
            this.buildDnsServer();
            this.buildNetworkCards();

            this.advancedTabPanel = Ext.create('Ext.tab.Panel',{
                activeTab: 0,
                deferredRender: false,
                parentId: this.getId(),
                autoHeight: true,
                flex: 1,
                items: [ this.panelGeneral, this.panelQoS, this.panelFilter, this.panelDnsServer, this.gridNetworkCards ]
            });
            
            this.panelAdvanced = Ext.create('Ext.panel.Panel',{
                name: 'panelAdvanced',
                helpSource: 'network_advanced',
                parentId: this.getId(),
                title: this.i18n._('Advanced'),
                layout: 'anchor',
                cls: 'ung-panel',
                layout: { type: 'vbox', pack: 'start', align: 'stretch' },
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    flex: 0,
                    html: this.i18n._(" <b>Advanced</b> is for advanced settings. Don't change them. YES THIS MEANS YOU.")
                }, this.advancedTabPanel]
            });
        },
        // General Panel
        buildGeneral: function() {
            this.panelGeneral = Ext.create('Ext.panel.Panel',{
                name: 'panelGeneral',
                helpSource: 'network_general',
                parentId: this.getId(),
                title: this.i18n._('General'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: "checkbox",
                    fieldLabel: this.i18n._("Enable SIP NAT Helper"),
                    labelStyle: 'width:150px',
                    name: 'HostName',
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
                    name: 'DomainName',
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
                    name: 'DomainName',
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
                height: 200,
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
                    header: this.i18n._("Interface Id"),
                    width: 80,
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
                    width: 150,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false
                  },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (value == null) {
                            return this.i18n._("Not set"); 
                        } else {
                            return value + this.i18n._( " kbps" );
                        }
                    }, this )
                }, {
                    header: this.i18n._("Upload Bandwidth"),
                    dataIndex: 'uploadBandwidthKbps',
                    width: 150,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (value == null) {
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
                            if(interfaceList[i].uploadBandwidthKbps !=null) {
                                u += interfaceList[i].uploadBandwidthKbps;    
                            }
                            if(interfaceList[i].downloadBandwidthKbps !=null ) {
                                d += interfaceList[i].downloadBandwidthKbps;    
                            }
                        }
                    }

                    var d_Mbit = d/1000;
                    var u_Mbit = u/1000;

                    var message = Ext.String.format( this.i18n._( "<i>Total: {0} kbps ({1} Mbit) download, {2} kbps ({3} Mbit) upload</i>" ), d, d_Mbit, u, u_Mbit );
                    var bandwidthLabel = this.panelQoS.query('label[name="bandwidthLabel"]')[0];
                    bandwidthLabel.setText(this.i18n._("<font color=\"red\">Note</font>: When enabling QoS valid Download Bandwidth and Upload Bandwidth limits must be set for all WAN interfaces.")+"</br>"+message, false);
                }, this)
            });
            this.gridQosWanBandwidth.getStore().on("update", Ext.bind(function() {
                this.gridQosWanBandwidth.updateTotalBandwidth();
            }, this));
            this.gridQosWanBandwidth.getStore().filter([{property: "configType", value: "ADDRESSED"}, {property:"isWan", value: true}]);
            
            this.gridQosRules = Ext.create( 'Ung.EditorGrid', {
                name: 'QoS Custom Rules',
                margin: '5 0 0 0',
                height: 450,
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
                    flex:1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                }],
                columnsDefaultSortable: false,
                rowEditorInputLines: [{
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
                        anchor:"98%",
                        width: 900,
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getQosRuleMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype: "combo",
                        allowBlank: false,
                        dataIndex: "priority",
                        fieldLabel: this.i18n._("Priority"),
                        editable: false,
                        store: this.qosPriorityNoDefaultStore,
                        valueField: "value",
                        displayField: "displayName",
                        queryMode: 'local'
                    }]
                }]
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
                        minValue : .1,
                        maxValue : 100
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (value == 0) 
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
                        minValue : .1,
                        maxValue : 100
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (value == 0) 
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
                        minValue : .1,
                        maxValue : 100
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (value == 0) 
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
                        minValue : .1,
                        maxValue : 100
                    },
                    renderer: Ext.bind(function( value, metadata, record ) { 
                        if (value == 0) 
                            return this.i18n._("No limit"); 
                        else 
                            return value + "%";
                    }, this )
                }],
                columnsDefaultSortable: false
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
                        html: this.i18n._("<font color=\"red\">Note</font>: Custom Rules only match <b>Bypassed</b> traffic.")
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
                    items: [{
                        border: false,
                        html: "TODO: implement this"
                    }]
                }]
            });
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
                    flex:1,
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
                columnsDefaultSortable: false,
                rowEditorInputLines: [{
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
                        anchor:"98%",
                        width: 900,
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getFilterRuleMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype: "checkbox",
                        name: "Block",
                        dataIndex: "bypass",
                        fieldLabel: this.i18n._("Block")
                    }]
                }]
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
                    flex:1,
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
                columnsDefaultSortable: false,
                rowEditorInputLines: [{
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
                        anchor:"98%",
                        width: 900,
                        dataIndex: "matchers",
                        matchers: Ung.NetworkUtil.getFilterRuleMatchers(this)
                    }]
                }, {
                    xtype: 'fieldset',
                    cls:'description',
                    title: i18n._('Perform the following action(s):'),
                    border: false,
                    items: [{
                        xtype: "checkbox",
                        name: "Block",
                        dataIndex: "bypass",
                        fieldLabel: this.i18n._("Block")
                    }]
                }]
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
                    flex:1,
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        vtype:"ipAddress"
                    }
                }]
            });
            this.gridDnsLocalServers = Ext.create( 'Ung.EditorGrid', {
                anchor: '100% 48%',
                name: 'Other Local DNS Servers',
                settingsCmp: this,
                paginated: false,
                hasEdit: false,
                emptyRow: {
                    "domain": this.i18n._("[no domain]"),
                    "localServer": "1.2.3.4",
                    "javaClass": "com.untangle.uvm.network.DnsLocalServer"
                },
                title: this.i18n._("Other Local DNS Servers"),
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
                    flex:1,
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
        save: function (isApply) {
            this.saveSemaphore = 1;
            // save language settings
            rpc.networkManager.setNetworkSettings(Ext.bind(function(result, exception) {
                this.afterSave(exception, isApply);
            }, this), this.settings);
        },
        refreshSettings: function() {
            this.settings = rpc.networkManager.getNetworkSettings();
            var deviceStatus=main.getNetworkManager().getDeviceStatus();
            var deviceStatusMap=Ung.Util.createRecordsMap(deviceStatus.list, "deviceName");
            for(var i=0; i<this.settings.interfaces.list.length; i++) {
                var intf=this.settings.interfaces.list[i];
                var deviceStatus = deviceStatusMap[intf.physicalDev];
                Ext.applyIf(intf, deviceStatus);
            }
        },
        beforeSave: function(isApply, handler) {
            this.beforeSaveCount = 12;

            Ext.MessageBox.wait(i18n._("Applying Network Settings..."), i18n._("Please wait"));

            this.gridInterfaces.getList(Ext.bind(function(saveList) {
                this.settings.interfaces = saveList;
                var qosBandwidthList = this.gridQosWanBandwidth.getPageList();
                var qosBandwidthMap = {};
                for(var i=0; i<qosBandwidthList.length; i++) {
                    qosBandwidthMap[qosBandwidthList[i].interfaceId] = qosBandwidthList[i];
                }
                for(var i=0; i<this.settings.interfaces.list.length; i++) {
                    var intf=this.settings.interfaces.list[i];
                    var intfBandwidth = qosBandwidthMap[intf.interfaceId];
                    if(intfBandwidth != null) {
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
            this.gridNetworkCards.getList(Ext.bind(function(saveList) {
                this.settings.devices = saveList;
                this.beforeSaveCount--;
                if (this.beforeSaveCount <= 0)
                    handler.call(this, isApply);
            }, this));
        },
        afterSave: function(exception, isApply) {
            if(Ung.Util.handleException(exception)) return;

            Ung.Util.networkSettings = null; /* clear cached settings object */
            
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                this.refreshSettings();
                if(isApply) {
                    this.clearDirty();
                    Ext.MessageBox.hide();
                } else {
                    Ext.MessageBox.hide();
                    this.closeWindow();
                }
            }
        }
        
    });
}
//@ sourceURL=network.js
