Ext.define('Webui.untangle-node-wan-failover.settings', {
    extend:'Ung.NodeWin',
    panelStatus: null,
    panelTests: null,
    gridWanStatus: null,
    gridTests: null,
    gridTestEventLog: null,
    gridEventLog: null,
    initComponent: function() {
        this.buildWanStatus();
        this.buildStatus();
        this.buildTests();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelStatus, this.panelTests]);
        this.callParent(arguments);
    },
    getMissingTestsWarning: function() {
        var i,
            testMap={},
            warningArr=[];
        for(i=0; i<this.settings.tests.list.length; i++) {
            var test=this.settings.tests.list[i];
            if(test.enabled) {
                testMap[test.interfaceId]=true;
            }
        }
        var interfaceList = Ung.Util.getWanList();
        for ( i = 0 ; i < interfaceList.length ; i++ ) {
            if(!testMap[interfaceList[i][0]]) {
                warningArr.push(Ext.String.format( this.i18n._( "Warning: The <i>{0}</i> needs a test configured!" ), interfaceList[i][1]));
            }
        }
        return warningArr.join("<br/>");
    },
    // active connections/sessions grid
    buildWanStatus: function() {
        this.gridWanStatus = Ext.create('Ung.grid.Panel',{
            name: "gridWanStatus",
            settingsCmp: this,
            flex: 1,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            hasRefresh: true,
            title: this.i18n._("WAN Status"),
            qtip: this.i18n._("This shows the current status of each WAN Interface."),
            recordJavaClass: "com.untangle.node.wan_failover.WanStatus",
            dataFn: this.getRpcNode().getWanStatus,
            fields: [{
                name: "interfaceId"
            },{
                name: "interfaceName"
            },{
                name: "systemName"
            },{
                name: "online"
            },{
                name: "totalTestsRun"
            },{
                name: "totalTestsPassed"
            },{
                name: "totalTestsFailed"
            }],
            columns: [{
                dataIndex: "interfaceId",
                header: this.i18n._("Interface ID"),
                width: 100
            },{
                dataIndex: "interfaceName",
                header: this.i18n._("Interface Name"),
                width: 130
            },{
                dataIndex: "systemName",
                header: this.i18n._("System Name"),
                width: 100
            },{
                dataIndex: "online",
                header: this.i18n._("Online Status"),
                width: 100
            },{
                dataIndex: "totalTestsRun",
                header: this.i18n._("Current Tests Count"),
                width: 150
            },{
                dataIndex: "totalTestsPassed",
                header: this.i18n._("Tests Passed"),
                width: 100
            },{
                dataIndex: "totalTestsFailed",
                header: this.i18n._("Tests Failed"),
                width: 100
            }]
        });
    },
    // Status Panel
    refreshTestsWarning: function() {
        var txt = this.getMissingTestsWarning();
        var missingTestsWarningContainer = this.panelStatus.down('component[name="missingTestsWarning"]');
        missingTestsWarningContainer.update(txt);
        missingTestsWarningContainer.setVisible(txt.length >0);
        noTestsWarningContainer = this.panelStatus.down('component[name="noTestsWarning"]');
        noTestsWarningContainer.setVisible(this.settings.tests.list.length == 0);
    },
    buildStatus: function() {
        var missingTestsWarning = this.getMissingTestsWarning();
        this.panelStatus = Ext.create('Ext.panel.Panel', {
            name: 'Status',
            helpSource: 'wan_failover_status',
            title: this.i18n._("Status"),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: this.i18n._('WAN Failover'),
                flex: 0,
                items: [{
                    xtype: "component",
                    html: this.i18n._("WAN Failover maximizes your network uptime. It monitors the status of your WAN interfaces and re-routes traffic any available WANs.") + "<br/>" +
                        this.i18n._("Tests must be configured using the <i>Tests</i> tab to determine the connectivity of each WAN.")
                }, {
                    xtype: "component",
                    name: "noTestsWarning",
                    html: this.i18n._("WARNING") + ": " + this.i18n._("There are currently no tests configured. A test must be configured for each WAN."),
                    cls: 'warning',
                    margin: '5 0 0 0',
                    hidden: (this.settings.tests.list.length != 0)
                }, {
                    xtype: "component",
                    name: "missingTestsWarning",
                    html: missingTestsWarning,
                    cls: 'warning',
                    margin: '5 0 0 0',
                    hidden: (missingTestsWarning.length == 0)
                }]
            }, this.gridWanStatus]
        });
    },
    //Tests panel
    buildTests: function() {
        this.interfaceStore = Ung.Util.getWanList();
        this.interfaceMap = Ung.Util.createStoreMap(this.interfaceStore);
        
        this.failureThresholdStore = [];
        for ( c = 1 ; c <= 10 ; c+= 1 ) {
            this.failureThresholdStore.push([ c, Ext.String.format( this.i18n._( "{0} of 10" ), c)]);
        }

        this.typeStore = [["ping", this.i18n._( "Ping" )], ["arp", this.i18n._( "ARP" )], ["dns", this.i18n._( "DNS" )], ["http", this.i18n._( "HTTP" )]];
        this.typeMap = Ung.Util.createStoreMap(this.typeStore);

        this.patchTests();
        this.gridTests = Ext.create('Ung.grid.Panel', {
            flex: 1,
            settingsCmp: this,
            dataProperty: "tests",
            title: this.i18n._( "Failure Detection Tests" ),
            recordJavaClass: "com.untangle.node.wan_failover.WanTestSettings",
            emptyRow: {
                "enabled": true,
                "description": "",
                "type": "ping",
                "interfaceId": 1,
                "timeoutMilliseconds": 2000,
                "delayMilliseconds": 5000,
                "timeoutSeconds": 2,
                "delaySeconds": 5,
                "failureThreshold": 3,
                "pingHostname": "8.8.8.8",
                "httpUrl": "http://1.2.3.4/",
                "testHistorySize": 10
            },
            fields: [{
                name: "enabled"
            },{
                name: "description"
            },{
                name: "javaClass"
            },{
                name: "interfaceId"
            },{
                name: "type"
            },{
                name: "timeoutMilliseconds"
            },{
                name: "delayMilliseconds"
            },{
                name: "timeoutSeconds"
            },{
                name: "delaySeconds"
            },{
                name: "testHistorySize"
            },{
                name: "failureThreshold"
            },{
                name: "pingHostname"
            },{
                name: "httpUrl"
            }],
            columns: [{
                header: this.i18n._( "Interface" ),
                width: 170,
                dataIndex: "interfaceId",
                renderer: Ext.bind(function( value ) {
                    var name = this.interfaceMap[value];
                    return ( name == null ) ? (Ext.String.format( this.i18n._("Interface {0}"), value )) : name;
                }, this)
            },{
                header: this.i18n._( "Test Type" ),
                width: 70,
                dataIndex: "type",
                renderer: Ext.bind(function( value ) {
                    var name = this.typeMap[value];
                    return ( name == null ) ? (Ext.String.format( this.i18n._("Type {0}"), value )) : name;
                }, this)
            },{
                header: this.i18n._( "Description" ),
                width: 200,
                dataIndex: "description",
                flex:1,
                editor: {
                    xtype: 'textfield',
                    emptyText: this.i18n._("[no description]")
                }
            }]
        });
        this.buildTestsRowEditor();

        this.panelTests = Ext.create('Ext.panel.Panel',{
            title: this.i18n._( "Tests" ),
            helpSource: "wan_failover_tests",
            layout: { type: 'vbox', align: 'stretch' },
            cls: "ung-panel",
            items: [{
                xtype: 'fieldset',
                flex: 0,
                title: this.i18n._('Note'),
                html: this.i18n._( 'These tests control how each WAN interface is tested to ensure that it has connectivity to the Internet.')  + "<br/>" +
                    this.i18n._('There should be one configured test per WAN interface.') + "<br/>" +
                    this.i18n._('These rules require careful configuration. Poorly chosen tests will greatly reduce the effectiveness of WAN Failover.') + "<br/>" +
                    this.i18n._('Press Help to see a further discussion about Failure Detection Tests.' )
            }, this.gridTests ]
        });
    },
    buildTestsRowEditor: function() {
        var rowEditor = Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
            helpSource: "wan_failover_tests",
            inputLines: [{
                xtype: "component",
                margin: '0 0 10 0',
                html: this.i18n._( 'Test configuration:' ) + "<br/>" +
                    this.i18n._('Design a test that likely indicates the connectivity of the chosen WAN interface.' )
            },{
                xtype: "combo",
                store: this.interfaceStore,
                dataIndex: "interfaceId",
                fieldLabel: this.i18n._( "Interface" ),
                name: "interfaceId",
                width: 300,
                queryMode: 'local',
                editable: false
            },{
                xtype: "textfield",
                fieldLabel: this.i18n._( "Description" ),
                emptyText: this.i18n._("[no description]"),
                dataIndex: "description",
                width: 450
            }, {
                xtype: "component",
                margin: '10 0 5 0',
                html: this.i18n._( '<b>Testing Interval</b> specifies how often this test will run. (Default: 5)' )
            }, {
                xtype: 'container',
                layout: 'column',
                items: [{
                    xtype: "numberfield",
                    width: 210,
                    fieldLabel: this.i18n._( "Testing Interval" ),
                    dataIndex: "delaySeconds",
                    hideTrigger:true
                },{
                    xtype: 'label',
                    html: this.i18n._( "(seconds)" ),
                    cls: 'boxlabel'
                }]
            },{
                xtype: "component",
                margin: '5 0 5 0',
                html: this.i18n._( '<b>Timeout</b> specifies how long the system waits for a response before considering the test failed. (Default: 2)' )
            }, {
                xtype: 'container',
                layout: 'column',
                items: [{
                    xtype: "numberfield",
                    width: 210,
                    fieldLabel: this.i18n._( "Timeout" ),
                    dataIndex: "timeoutSeconds",
                    hideTrigger:true
                },{
                    xtype: 'label',
                    html: this.i18n._( "(seconds)" ),
                    cls: 'boxlabel'
                }]
            }, {
                xtype: "component",
                margin: '5 0 5 0',
                html: this.i18n._( '<b>Failure Threshold</b> specifies how many tests failures (out of 10) are required for this WAN to be considered offline.' )
            }, {
                xtype: "combo",
                store: this.failureThresholdStore,
                dataIndex: "failureThreshold",
                fieldLabel: this.i18n._( "Failure Threshold" ),
                name: "failureThreshold",
                width: 180,
                queryMode: 'local',
                editable: false
            }, {
                xtype: "combo",
                style: {marginTop: '15px'},
                fieldLabel: this.i18n._( "Test Type" ),
                dataIndex: "type",
                store: this.typeStore,
                width: 180,
                queryMode: 'local',
                editable: false,
                listeners: {
                    "select": {
                        fn: Ext.bind(function(combo, records, eOpts) {
                            this.gridTests.rowEditor.syncComponents();
                        }, this)
                    }
                }
            }, {
                //Ping container
                xtype: "container",
                testDetails: true,
                showValue: "ping",
                items: [{
                    xtype: "component",
                    html: this.i18n._( 'Choose a destination that likely indicates the connectivity of the chosen WAN interface.' ) + "<br/>" +
                        this.i18n._( 'The IP of an upstream router at your ISP is recommended.' )
                }, {
                    xtype: 'container',
                    layout: 'column',
                    margin: '5 0 0 0',
                    items: [{
                        xtype: "combo",
                        dataIndex: "pingHostname",
                        fieldLabel: this.i18n._( "IP" ),
                        store: [],
                        queryMode: 'local',
                        editable: true,
                        width: 240,
                        listWidth: 100
                    }, {
                        xtype: "button",
                        margin: '0 0 0 15',
                        text: this.i18n._( "Generate Suggestions" ),
                        handler: this.generateSuggestions,
                        scope: this
                    }]
                }]
            }, {
                //ARP component
                xtype: "component",
                testDetails: true,
                showValue: "arp",
                html: this.i18n._( "ARP the default gateway for this interface." )
            }, {
                //DNS component
                xtype: "component",
                testDetails: true,
                showValue: "dns",
                html: this.i18n._( "Generate DNS requests to the upstream DNS servers." )
            }, {
                //HTTP container
                xtype: "container",
                testDetails: true,
                showValue: "http",
                items: [{
                    xtype: "component",
                    margin: '0 0 5 0',
                    html: this.i18n._( 'Choose a URL that likely indicates the connectivity of the chosen WAN interface.' ) + "<br/>" +
                        this.i18n._( 'The IP URL of an upstream website near your ISP is recommended.' ) + "<br/>" +
                        this.i18n._( 'Using a hostname instead of an IP address URL is <b>not recommended</b> as it requires DNS which may be down.')
                },{
                    xtype: "textfield",
                    fieldLabel: this.i18n._( "URL" ),
                    dataIndex: "httpUrl",
                    width: 300
                }]
            }, {
                xtype: "button",
                text: this.i18n._( "Run Test" ),
                margin: '5 0 5 0',
                handler: this.runTest,
                scope: this
            }],
            syncComponents: function() {
                var combo = this.down('combo[dataIndex="type"]');
                var selectedType = combo.getValue();

                var fields = this.query('component[testDetails="true"]');
                for ( var c = 0 ; c < fields.length ; c++ ) {
                    var field = fields[c];
                    field.setVisible( field.showValue == selectedType );
                }
            },
            getCurrentData: function() {
                if (this.validate()!==true) {
                    return null;
                }
                if (this.record !== null) {
                    var record =this.record.copy();
                    var data = {};
                    this.updateActionRecursive(this.items, data, 0);
                    record.set(data);
                    return record.data;
                }
                return null;
            }
        });

        this.gridTests.setRowEditor(rowEditor);
    },
    patchTests: function() {
        var c,rule;
        // Patch the rules
        for ( c = 0 ; c < this.settings.tests.list.length ; c++ ) {
            rule = this.settings.tests.list[c];
            rule.delaySeconds = Math.round( rule.delayMilliseconds / 1000 );
            rule.timeoutSeconds = Math.round( rule.timeoutMilliseconds / 1000 );
        }
    },
    beforeSave: function(isApply, handler) {
        this.settings.tests.list=this.gridTests.getList();
        var rules=this.settings.tests.list;
        for (var c = 0 ; c < rules.length ; c++ ) {
            var rule = rules[c];
            rule.delayMilliseconds = rule.delaySeconds * 1000;
            rule.timeoutMilliseconds = rule.timeoutSeconds * 1000;
            delete rule.timeoutSeconds;
            delete rule.delaySeconds;
        }
        handler.call(this, isApply);
    },
    afterSave: function() {
        this.patchTests();
        this.gridTests.reload();
        this.refreshTestsWarning();
    },
    generateSuggestions: function() {
        var interfaceId = this.gridTests.rowEditor.down('combo[name="interfaceId"]').getValue();
        var pingHostnameCombo = this.gridTests.rowEditor.down('combo[dataIndex="pingHostname"]');
        Ext.MessageBox.wait(this.i18n._("Querying Pingable Hosts..."), this.i18n._("Please wait"));
        this.getRpcNode().getPingableHosts( Ext.bind(function( result, exception ) {
            if ( exception ) {
                Ext.MessageBox.show({
                    title: this.i18n._( "Unable to find a pingable host."),
                    msg: this.i18n._( "Please try again later." ),
                    buttons: Ext.MessageBox.OK
                });
                pingHostnameCombo.getStore().loadData([]);
                console.log("getPingableHosts", exception);
                return;
            }
            var comboData = [];
            for(var i=0; i<result.list.length; i++) {
                comboData.push([result.list[i],result.list[i]]);
            }
            pingHostnameCombo.getStore().loadData(comboData);
            Ext.MessageBox.show({
                title: this.i18n._( "Completed host list."),
                msg: this.i18n._( "Select one of the recommended host<br/>from the 'IP' dropdown." ),
                buttons: Ext.MessageBox.OK
            });
        }, this ), interfaceId );
    },
    runTest: function() {
        var rule = this.gridTests.rowEditor.getCurrentData();
        if(!rule) {
            return;
        }
        Ext.MessageBox.wait(this.i18n._("Running Test..."), this.i18n._("Please wait"));
        this.getRpcNode().runTest( Ext.bind(function( result, exception ) {
            if ( exception ) {
                Ext.MessageBox.show({
                    title: this.i18n._( "Unable to complete test."),
                    msg: this.i18n._( "Please try again later." ),
                    buttons: Ext.MessageBox.OK
                });
                return;
            }
            var message = this.i18n._( result );

            Ext.MessageBox.show({
                title: this.i18n._( "Test Results" ),
                msg: message,
                buttons: Ext.MessageBox.OK
            });
        }, this ), rule );
    }
});
//# sourceURL=wan-failover-settings.js