if (!Ung.hasResource["Ung.SessionMonitor"]) {
    Ung.hasResource["Ung.SessionMonitor"] = true;

    Ext.define('Ung.SessionMonitor', {
        extend:'Ung.ConfigWin',
        fnCallback : null,
        panelPolicyManagement : null,
        gridRacks : null,
        gridRules : null,
        policyStore : null,
        node:null,
        rackKey: 1,
        helpSource: 'session_monitor',

        initComponent : function()
        {
            this.breadcrumbs = [{
                title : this.i18n._('Session Viewer')
            }];

            this.buildGridCurrentSessions();

            this.buildTabPanel([this.gridCurrentSessions]);
            Ung.SessionMonitor.superclass.initComponent.call(this);
        },
        saveAction : function(){
            this.cancelAction();
        },
        applyAction : function(){
            this.cancelAction();
        },
        // Current Sessions Grid
        buildGridCurrentSessions : function() {
            this.gridCurrentSessions = Ext.create('Ung.EditorGrid',{
                name : "gridCurrentSessions",
                settingsCmp : this,
                height : 500,
                paginated: true,
                hasAdd : false,
                configAdd : null,
                hasEdit : false,
                configEdit : null,
                hasDelete : false,
                configDelete : null,
                sortField : 'bypassed',
                columnsDefaultSortable : true,
                title : this.i18n._("Current Sessions"),
                qtip : this.i18n._("This shows all current sessions."),
                paginated : false,
                recordJavaClass : "com.untangle.uvm.SessionMonitorEntry",
                dataFn : rpc.jsonrpc.UvmContext.sessionMonitor().getMergedSessions,
                fields : [{
                    name : "id"
                },{
                    name : "protocol"
                },{
                    name : "bypassed"
                },{
                    name : "localTraffic"
                },{
                    name : "policy"
                },{
                    name : "preNatClient"
                },{
                    name : "preNatServer"
                },{
                    name : "preNatClientPort"
                },{
                    name : "preNatServerPort"
                },{
                    name : "postNatClient"
                },{
                    name : "postNatServer"
                },{
                    name : "postNatClientPort"
                },{
                    name : "postNatServerPort"
                },{
                    name : "clientIntf"
                },{
                    name : "serverIntf"
                },{
                    name : "natted"
                },{
                    name : "portForwarded"
                }],
                columns : [{
                    header : this.i18n._("Protocol"),
                    dataIndex: "protocol",
                    width : 70
                },{
                    header : this.i18n._("Bypassed"),
                    dataIndex: "bypassed",
                    width : 70
                },{
                    header : this.i18n._("Policy"),
                    dataIndex: "policy",
                    width : 100
                },{
                    header : this.i18n._("Client Interface"),
                    dataIndex: "clientIntf",
                    width : 100,
                    renderer : function(value) {
                        var result = "";
                        var store = Ung.Util.getInterfaceStore();
                        if (store) {
                            var index = store.find("key", value);
                            if (index >= 0) {
                                result = store.getAt(index).get("name");
                            }
                        }
                        return result;
                    }
                },{
                    header : this.i18n._("Client (Pre-NAT)"),
                    dataIndex: "preNatClient",
                    width : 100
                },{
                    header : this.i18n._("Server (Pre-NAT)"),
                    dataIndex: "preNatServer",
                    width : 100
                },{
                    header : this.i18n._("Client Port (Pre-NAT)"),
                    dataIndex: "preNatClientPort",
                    width : 80
                },{
                    header : this.i18n._("Server Port (Pre-NAT)"),
                    dataIndex: "preNatServerPort",
                    width : 80
                },{
                    header : this.i18n._("Server Interface"),
                    dataIndex: "serverIntf",
                    width : 100,
                    renderer : function(value) {
                        var result = "";
                        var store = Ung.Util.getInterfaceStore();
                        if (store) {
                            var index = store.find("key", value);
                            if (index >= 0) {
                                result = store.getAt(index).get("name");
                            }
                        }
                        return result;
                    }
                },{
                    header : this.i18n._("Client (Post-NAT)"),
                    dataIndex: "postNatClient",
                    width : 100
                },{
                    header : this.i18n._("Server (Post-NAT)"),
                    dataIndex: "postNatServer",
                    width : 100
                },{
                    header : this.i18n._("Client Port (Post-NAT)"),
                    dataIndex: "postNatClientPort",
                    width : 80
                },{
                    header : this.i18n._("Server Port (Post-NAT)"),
                    dataIndex: "postNatServerPort",
                    width : 80
                },{
                    header : this.i18n._("Local"),
                    dataIndex: "localTraffic",
                    width : 50
                },{
                    header : this.i18n._("NATd"),
                    dataIndex: "natted",
                    width : 50
                },{
                    header : this.i18n._("Port Forwarded"),
                    dataIndex: "portForwarded",
                    width : 50
                }],
                initComponent : function() {
                    this.bbar = ['-',
                        {
                            xtype : 'button',
                            id: "refresh_"+this.getId(),
                            text : i18n._('Refresh'),
                            name : "Refresh",
                            tooltip : i18n._('Refresh'),
                            iconCls : 'icon-refresh',
                            handler : Ext.bind(function() {
                                Ext.MessageBox.wait(this.settingsCmp.i18n._("Refreshing..."), this.settingsCmp.i18n._("Please wait"));
                                this.reloadGrid();
                                Ext.MessageBox.hide();
                            },this)
                        },{
                            xtype : 'button',
                            id: "auto_refresh_"+this.getId(),
                            text : i18n._('Auto Refresh'),
                            enableToggle: true,
                            pressed: false,
                            name : "Auto Refresh",
                            tooltip : i18n._('Auto Refresh'),
                            iconCls : 'icon-autorefresh',
                            handler : Ext.bind(function() {
                                var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
                                if(autoRefreshButton.pressed) {
                                    this.startAutoRefresh();
                                } else {
                                    this.stopAutoRefresh();
                                }
                            },this)
                        }
                    ];
                    Ung.EditorGrid.prototype.initComponent.call(this);
                    this.loadMask=null;
                },                
                autoRefreshEnabled:true,
                startAutoRefresh: function(setButton) {
                    this.autoRefreshEnabled=true;
                    if(setButton) {
                        var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
                        autoRefreshButton.toggle(true);
                    }
                    var refreshButton=Ext.getCmp("refresh_"+this.getId());
                    refreshButton.disable();
                    this.autorefreshList();

                },
                stopAutoRefresh: function(setButton) {
                    this.autoRefreshEnabled=false;
                    if(setButton) {
                        var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
                        autoRefreshButton.toggle(false);
                    }
                    var refreshButton=Ext.getCmp("refresh_"+this.getId());
                    refreshButton.enable();
                },
                autorefreshList : function() {
                    if(this!=null && this.autoRefreshEnabled && Ext.getCmp(this.id) != null) {
                    	this.reloadGrid();
                        Ext.defer(this.autorefreshList,5000,this);
                    }
                }
            });
        }

    });

}
