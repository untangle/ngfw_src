Ext.define('Webui.untangle-node-wan-balancer.settings', {
    extend:'Ung.NodeWin',
    getAppSummary: function() {
        return i18n._("WAN Balancer spreads network traffic across multiple internet connections for better performance.");
    },
    initComponent: function() {
        this.generateSettings();
        this.buildTrafficAllocation();
        this.buildRoutingRules();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelTrafficAllocation, this.panelRouteRules]);
        this.callParent(arguments);
    },
    getRouteRuleConditions: function () {
        return [
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, false), visible: true},
            {name:"PROTOCOL",displayName: i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["any", i18n._("any")]], visible: true},
            {name:"CLIENT_COUNTRY",displayName: i18n._("Client Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true},
            {name:"SERVER_COUNTRY",displayName: i18n._("Server Country"), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true}
        ];
    },
    // Status Panel
    buildStatus: function() {
        var interfaceItems = [], i, intf;
        for ( i = 0 ; i < this.interfaceWeightList.length ; i++ ) {
            intf = this.interfaceWeightList[i];
            interfaceItems.push({
                name: 'interfaceStatus' + intf.interfaceId,
                fieldLabel: Ext.String.format( i18n._( "{0} interface" ), intf.name),
                value: intf.description
            });
        }

        this.panelStatus = Ext.create('Ung.panel.Status', {
            settingsCmp: this,
            helpSource: "wan_balancer_status",
            itemsAfterLicense: [{
                title: i18n._("Current Traffic Allocation"),
                defaults: {
                    xtype: "displayfield",
                    labelWidth: 180
                },
                items: [{
                    xtype: 'component',
                    html: i18n._( 'Currently, WAN Balancer is attempting to share traffic over the existing WAN interfaces with the ratio displayed below. To change this ratio click on Traffic Allocation.' ),
                    margin: '0 0 15 0'
                }]
                .concat(interfaceItems)
                .concat([{
                    xtype: "button",
                    text: i18n._("Configure additional WAN interfaces"),
                    handler: Ext.bind(function() {
                        this.cancelAction(function() {
                            Ung.Main.openConfig(Ung.Main.configMap["network"]);
                        });
                    }, this)
                }])
            }]
        });
    },
    buildTrafficAllocation: function() {
        this.gridInterfaceWeight = Ext.create('Ung.grid.Panel', {
            flex: 1,
            settingsCmp: this,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            title: i18n._( "Interface Weights" ),
            storeData: this.interfaceWeightList,
            fields: [{
                name: "interfaceId"
            },{
                name: "name"
            },{
                name: "weight"
            },{
                name: "description"
            }],
            columns: [{
                header: i18n._( "Interface" ),
                width: 170,
                dataIndex: "name"
            }, {
                header: i18n._( "Weight" ),
                width: 70,
                dataIndex: "weight",
                editor: {
                    xtype: 'numberfield',
                    allowDecimals: false,
                    minValue: 0,
                    maxValue: 100
                }
            }, {
                header: i18n._( "Resulting Traffic Allocation" ),
                width: 300,
                flex:1,
                dataIndex: "description"
            }]
        });
        
        this.panelTrafficAllocation = Ext.create('Ext.panel.Panel',{
            title: i18n._( "Traffic Allocation" ),
            helpSource: "wan_balancer_traffic_allocation",
            layout: { type: 'vbox', align: 'stretch' },
            cls: "ung-panel",
            items: [{
                xtype: 'fieldset',
                flex: 0,
                html: i18n._( 'Traffic allocation across WAN interfaces is controlled by assigning a relative weight (1-100) to each interface. After entering the weight of each interface the resulting allocation is displayed.<br/>If all WAN interfaces have the same bandwidth it is best to assign the same weight to all WAN interfaces. If the WAN interfaces vary in bandwidth enter numbers that correlate the relative available bandwidth.  For example: 15 for a 1.5Mbit/sec T1, 60 for a 6 mbit link, and 100 for a 10mbit link.' ) 
            }, this.gridInterfaceWeight]
        });
        this.gridInterfaceWeight.getStore().on("update", this.updateDescriptions, this);
    },

    buildRoutingRules: function() {
        var destinationWanList = Ung.Util.getWanList();
        destinationWanList.push( [0, i18n._('Balance')] );
        var destinationWanMap = Ung.Util.createStoreMap(destinationWanList);
        
        this.gridRouteRules = Ext.create( 'Ung.grid.Panel', {
            flex: 1,
            name: 'Route Rules',
            settingsCmp: this,
            hasReorder: true,
            addAtTop: false,
            title: i18n._("Route Rules"),
            dataProperty:'routeRules',
            recordJavaClass: "com.untangle.node.wan_balancer.RouteRule",
            emptyRow: {
                "ruleId": -1,
                "enabled": true,
                "route": true,
                "description": ""
            },
            fields: [{
                name: 'ruleId'
            }, {
                name: 'enabled'
            }, {
                name: 'destinationWan'
            }, {
                name: 'conditions'
            },{
                name: 'description'
            }, {
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Rule Id"),
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
                header: i18n._("Enable"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex:1
            }, {
                header: i18n._("Destination WAN"),
                dataIndex: 'destinationWan',
                resizable: false,
                renderer: function (value) {
                    return destinationWanMap[value] || value;
                },
                width: 100
            }],
            rowEditorInputLines: [{
                xtype:'checkbox',
                name: "Enable Route Rule",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable Route Rule")
            }, {
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            }, {
                xtype:'fieldset',
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.node.wan_balancer.RouteRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getRouteRuleConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items: [{
                    xtype: "combo",
                    name: "destinationWan",
                    allowBlank: false,
                    dataIndex: "destinationWan",
                    fieldLabel: i18n._("Destination WAN"),
                    editable: false,
                    store: destinationWanList,
                    queryMode: 'local'
                }]
            }]
        });

        this.panelRouteRules = Ext.create('Ext.panel.Panel',{
            title: i18n._( "Route Rules" ),
            helpSource: "wan_balancer_route_rules",
            layout: { type: 'vbox', align: 'stretch' },
            cls: "ung-panel",
            items: [{
                xtype: 'fieldset',
                flex: 0,
                html: i18n._( "Route Rules are used to assign specific sessions to a specific WAN interface. Rules are evaluated in order and the WAN interface of the first matching rule is used to route the matching session. If there is no matching rule or the rule is set to <i>Balance</i> the session will be routed according to the <i>Traffic Allocation</i> settings." )
            }, this.gridRouteRules ]
        });
    },
    beforeSave: function(isApply,handler) {
        this.setWeights();
        this.getSettings().routeRules.list = this.gridRouteRules.getList();
        handler.call(this, isApply);
    },
    afterSave: function() {
        this.generateSettings();
        this.updateStatus();
        this.gridInterfaceWeight.reload({data:this.interfaceWeightList});
    },
    updateStatus: function() {
        var i, intf;
        for(i=0;i<this.interfaceWeightList.length;i++) {
            intf = this.interfaceWeightList[i];
            this.panelStatus.down("field[name=interfaceStatus"+intf.interfaceId+"]").setValue(intf.description);
        }
    },
    updateDescriptions: function() {
        Ext.defer(function() {
            var total = 0, store = this.gridInterfaceWeight.getStore();
            store.suspendEvents();
            store.each( function( record ) {
                total += record.get( "weight" );
            });
            store.each( Ext.bind(function( record ) {
                record.set( "description", this.getDescription(this.interfaceWeightList.length, total, record.get( "weight" )));
            }, this));
            store.resumeEvents();
            this.gridInterfaceWeight.getView().refresh();
        }, 200, this);
        
    },
    generateSettings: function() {
        this.interfaceWeightList = [];

        var networkSettings, total = 0, i, weight, intfCount;
        try {
            networkSettings = Ung.Main.getNetworkManager().getNetworkSettings();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }

        var weightArray = this.getSettings().weights || [];
        for (i = 0 ; i < networkSettings.interfaces.list.length ; i++ ) {
            var intf = networkSettings.interfaces.list[i];
            if ( intf.configType != 'ADDRESSED' || !intf.isWan ) {
                continue;
            }
            weight = weightArray[intf.interfaceId-1];
            this.interfaceWeightList.push({
                interfaceId: intf.interfaceId,
                name: intf.name,
                weight: weight,
                description: ""
            });
            total += weight;
        }
        // Calculate weights percent for descriptions.
        intfCount = this.interfaceWeightList.length;
        for(i=0; i<intfCount; i++) {
            this.interfaceWeightList[i].description = this.getDescription(intfCount, total, this.interfaceWeightList[i].weight);
        }
    },
    getDescription: function(intfCount, total, weight) {
        var percent = ( total == 0 ) ? Math.round(( 1 / intfCount ) * 1000) / 10 : Math.round(( weight / total ) * 1000) / 10;
        return Ext.String.format( i18n._( "{0}% of Internet traffic." ), percent);
    },
    // Convert the rules back to the correct form
    setWeights: function() {
        // JSON serializes out empty elements
        var weights = [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                   0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                   0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                   0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                   0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                   0, 0, 0, 0, 0 ];
        this.gridInterfaceWeight.store.each( function( record ) {
            weights[record.get( "interfaceId" ) - 1] = record.get( "weight" );
        });
        this.getSettings().weights = weights;
    }
});
//# sourceURL=wan-balancer-settings.js