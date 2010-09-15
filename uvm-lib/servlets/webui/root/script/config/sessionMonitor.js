if (!Ung.hasResource["Ung.SessionMonitor"]) {
    Ung.hasResource["Ung.SessionMonitor"] = true;

    Ung.SessionMonitor = Ext.extend(Ung.ConfigWin, {
        fnCallback : null,
        panelPolicyManagement : null,
        gridRacks : null,
        gridRules : null,
        policyStore : null,
        node:null,
        rackKey : 1,
        initComponent : function()
        {
            this.breadcrumbs = [{
                title : this.i18n._('Session Viewer')
            }];

            this.buildGridCurrentSessions();

            this.buildTabPanel([this.gridCurrentSessions]);

            Ung.SessionMonitor.superclass.initComponent.call(this);
        },
        // Current Sessions Grid
        buildGridCurrentSessions : function() {
            this.gridCurrentSessions = new Ung.EditorGrid({
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
                bbar : new Ext.Toolbar({
                    items : [
                        '-',
                        {
                            xtype : 'tbbutton',
                            id: "refresh_"+this.getId(),
                            text : i18n._('Refresh'),
                            name : "Refresh",
                            tooltip : i18n._('Refresh'),
                            iconCls : 'icon-refresh',
                            handler : function() {
                                this.gridCurrentSessions.store.reload();
                            }.createDelegate(this)
                        }
                    ]
                }),
                recordJavaClass : "com.untangle.uvm.SessionMonitorEntry",
                proxyRpcFn : rpc.jsonrpc.RemoteUvmContext.sessionMonitor().getMergedSessionMonitorEntrys,
                fields : [{
                    name : "id"
                },{
                    name : "protocol"
                },{
                    name : "bypassed"
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
                }],
                columns : [{
                    id : "protocol",
                    header : this.i18n._("Protocol"),
                    dataIndex: "protocol",
                    width : 70
                },{
                    id : "bypassed",
                    header : this.i18n._("Bypassed"),
                    dataIndex: "bypassed",
                    width : 70
                },{
                    id : "policy",
                    header : this.i18n._("Policy"),
                    dataIndex: "policy",
                    width : 100
                },{
                    id : "preNatClient",
                    header : this.i18n._("Pre-NAT Client"),
                    dataIndex: "preNatClient",
                    width : 100
                },{
                    id : "preNatServer",
                    header : this.i18n._("Pre-NAT Server"),
                    dataIndex: "preNatServer",
                    width : 100
                },{
                    id : "preNatClientPort",
                    header : this.i18n._("Pre-NAT Client Port"),
                    dataIndex: "preNatClientPort",
                    width : 100
                },{
                    id : "preNatServerPort",
                    header : this.i18n._("Pre-NAT Server Port"),
                    dataIndex: "preNatServerPort",
                    width : 100
                },{
                    id : "postNatClient",
                    header : this.i18n._("Post-NAT Client"),
                    dataIndex: "postNatClient",
                    width : 100
                },{
                    id : "postNatServer",
                    header : this.i18n._("Post-NAT Server"),
                    dataIndex: "postNatServer",
                    width : 100
                },{
                    id : "postNatClientPort",
                    header : this.i18n._("Post-NAT Client Port"),
                    dataIndex: "postNatClientPort",
                    width : 100
                },{
                    id : "postNatServerPort",
                    header : this.i18n._("Post-NAT Server Port"),
                    dataIndex: "postNatServerPort",
                    width : 100
                },{
                    id : "clientIntf",
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
                    id : "serverIntf",
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
                }]
            });
        }

    });

}
