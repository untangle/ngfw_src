if (!Ung.hasResource["Ung.SessionViewer"]) {
    Ung.hasResource["Ung.SessionViewer"] = true;

    Ung.SessionViewer = Ext.extend(Ung.ConfigWin, {
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

            Ung.SessionViewer.superclass.initComponent.call(this);
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
                sortField : 'protocol',
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
                recordJavaClass : "com.untangle.uvm.ConntrackSession",
                proxyRpcFn : rpc.jsonrpc.RemoteUvmContext.sessionMonitor().getMergedConntrackSessions,
                fields : [{
                    name : "id"
                },{
                    name : "protocol"
                },{
                    name : "bypassed"
                },{
                    name : "policy"
                },{
                    name : "preNatSrc"
                },{
                    name : "preNatDst"
                },{
                    name : "preNatSrcPort"
                },{
                    name : "preNatDstPort"
                },{
                    name : "postNatSrc"
                },{
                    name : "postNatDst"
                },{
                    name : "postNatSrcPort"
                },{
                    name : "postNatDstPort"
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
                    id : "preNatSrc",
                    header : this.i18n._("Pre-NAT Src"),
                    dataIndex: "preNatSrc",
                    width : 100
                },{
                    id : "preNatDst",
                    header : this.i18n._("Pre-NAT Dst"),
                    dataIndex: "preNatDst",
                    width : 100
                },{
                    id : "preNatSrc",
                    header : this.i18n._("Pre-NAT Src Port"),
                    dataIndex: "preNatSrcPort",
                    width : 100
                },{
                    id : "preNatSrc",
                    header : this.i18n._("Pre-NAT Dst Port"),
                    dataIndex: "preNatDstPort",
                    width : 100
                },{
                    id : "postNatSrc",
                    header : this.i18n._("Post-NAT Src"),
                    dataIndex: "postNatSrc",
                    width : 100
                },{
                    id : "postNatDst",
                    header : this.i18n._("Post-NAT Dst"),
                    dataIndex: "postNatDst",
                    width : 100
                },{
                    id : "postNatSrc",
                    header : this.i18n._("Post-NAT Src Port"),
                    dataIndex: "postNatSrcPort",
                    width : 100
                },{
                    id : "postNatSrc",
                    header : this.i18n._("Post-NAT Dst Port"),
                    dataIndex: "postNatDstPort",
                    width : 100
                }]
            });
        }

    });

}
