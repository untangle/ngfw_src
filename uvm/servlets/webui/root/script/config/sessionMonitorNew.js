if (!Ung.hasResource["Ung.SessionMonitor"]) {
    Ung.hasResource["Ung.SessionMonitor"] = true;

    Ext.define('Ung.SessionMonitor', {
        extend:'Ung.StatusWin',
        helpSource: 'session_monitor',

        initComponent : function()
        {
            this.breadcrumbs = [{
                title : this.i18n._('Session Viewer')
            }];

            this.buildPanel();
            this.buildTabPanel( [this.sessionPanel] );
            Ung.SessionMonitor.superclass.initComponent.call(this);
        },
        getSessions : function(){
            var sessions = rpc.jsonrpc.UvmContext.sessionMonitor().getMergedSessions();
            // iterate through each session and change its attachments map to properties
            for (var i = 0; i < sessions.list.length ; i++) {
                var session = sessions.list[i];
                if (session.attachments != null) {
                    for (var prop in session.attachments.map) {
                        session[prop] = session.attachments.map[prop];
                    }
                }
            }
            return sessions;
        },
        buildPanel : function() {
            this.enabledColumns = {};
            this.columns = [];
            this.groupField = null;
            this.reRenderGrid = Ext.bind(function() {
                Ext.MessageBox.wait(this.i18n._("Refreshing..."), this.i18n._("Please wait"));
                this.columns = [];
                this.enabledColumns = {};
                this.groupField = null;
                for ( var i = 0 ; i < this.panelColumnSelector.items.items.length ; i++ ) {
                    var item = this.panelColumnSelector.items.items[i];
                    if ( item.xtype == "checkbox" ) {
                        this.enabledColumns[item.name] = item.checked;
                        if (item.checked) {
                            var newColumn = {
                                header : item.columnHeader,
                                dataIndex: item.columnDataIndex,
                                width : item.columnWidth
                            };
                            if ( item.columnRenderer !== undefined )
                                newColumn.renderer = item.columnRenderer;
                            this.columns.push( newColumn );
                        }
                    }
                }
                // add grouping if enabled
                for ( var i = 0 ; i < this.panelGroupSelector.items.items.length ; i++ ) {
                    var item = this.panelGroupSelector.items.items[i];
                    if ( item.xtype == "checkbox" ) {
                        if (item.checked) {
                            this.groupField = 'preNatClient';
                        }
                    }
                }
                // if the grid is already rendered it - force re-render it
                if ( this.gridCurrentSessions == undefined ) {
                    this.buildGridCurrentSessions(this.columns, this.groupField);
                 } else {
                        if ( this.groupField) {
                            this.gridCurrentSessions.getStore().group(this.groupField);
                        } else {
                            this.gridCurrentSessions.getStore().clearGrouping();
                        }
                        var headerCt = this.gridCurrentSessions.headerCt;
                        headerCt.suspendLayout = true;
                        headerCt.removeAll();
                        headerCt.add(this.columns);
                        this.gridCurrentSessions.groupField = this.groupField;
                        this.gridCurrentSessions.getView().refresh();
                        headerCt.suspendLayout = false;
                        this.gridCurrentSessions.forceComponentLayout();
                 }
                Ext.MessageBox.hide();
            }, this);
            
            // manually call the renderer for the first render
            this.buildGroupSelectorPanel();
            this.buildColumnSelectorPanel();
            this.reRenderGrid();
            this.buildGridCurrentSessions(this.columns, this.groupField);
            this.buildSessionPanel();
        },
        buildSessionPanel: function() {

            this.sessionPanel = Ext.create('Ext.panel.Panel',{
                title : this.i18n._('Session Viewer'),
                name : 'Session Viewer',
                helpSource : 'session_monitor',
                layout : "anchor",
                defaults: {
                    anchor: '98%',
                    autoScroll: true
                },
                autoScroll : true,
                cls: 'ung-panel',
                items : [this.panelColumnSelector, this.panelGroupSelector, this.gridCurrentSessions]
            });
        },
        buildColumnSelectorPanel : function() {
            this.panelColumnSelector = Ext.create('Ext.panel.Panel',{
                name:'advanced',
                xtype:'fieldset',
                title:i18n._("Column Selection"),
                collapsible : true,
                collapsed : true,
                autoHeight : true,
                bodyStyle : 'padding:5px 5px 5px 5px;',
                items : [{
                    xtype : 'checkbox',
                    checked : true,
                    boxLabel : this.i18n._("Protocol"),
                    columnHeader : this.i18n._("Protocol"),
                    columnDataIndex: "protocol",
                    columnWidth : 70
                },{
                    xtype : 'checkbox',
                    checked : true,
                    boxLabel : this.i18n._("Bypassed"),
                    columnHeader : this.i18n._("Bypassed"),
                    columnDataIndex: "bypassed",
                    columnWidth : 70
                },{
                    xtype : 'checkbox',
                    checked : true,
                    boxLabel : this.i18n._("Policy"),
                    columnHeader : this.i18n._("Policy"),
                    columnDataIndex: "policy",
                    columnWidth : 100
                },{
                    xtype : 'checkbox',
                    checked : true,
                    boxLabel : this.i18n._("Client Interface"),
                    columnHeader : this.i18n._("Client Interface"),
                    columnDataIndex: "clientIntf",
                    columnWidth : 100,
                    columnRenderer : function(value) {
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
                    xtype : 'checkbox',
                    checked : true,
                    boxLabel : this.i18n._("Client (Pre-NAT)"),
                    columnHeader : this.i18n._("Client (Pre-NAT)"),
                    columnDataIndex: "preNatClient",
                    columnWidth : 100
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Server (Pre-NAT)"),
                    columnHeader : this.i18n._("Server (Pre-NAT)"),
                    columnDataIndex: "preNatServer",
                    columnWidth : 100
                },{
                    xtype : 'checkbox',
                    checked : true,
                    boxLabel : this.i18n._("Client Port (Pre-NAT)"),
                    columnHeader : this.i18n._("Client Port (Pre-NAT)"),
                    columnDataIndex: "preNatClientPort",
                    columnWidth : 80
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Server Port (Pre-NAT)"),
                    columnHeader : this.i18n._("Server Port (Pre-NAT)"),
                    columnDataIndex: "preNatServerPort",
                    columnWidth : 80
                },{
                    xtype : 'checkbox',
                    checked : true,
                    boxLabel : this.i18n._("Server Interface"),
                    columnHeader : this.i18n._("Server Interface"),
                    columnDataIndex: "serverIntf",
                    columnWidth : 100,
                    columnRenderer : function(value) {
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
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Client (Post-NAT)"),
                    columnHeader : this.i18n._("Client (Post-NAT)"),
                    columnDataIndex: "postNatClient",
                    columnWidth : 100
                },{
                    xtype : 'checkbox',
                    checked : true,
                    boxLabel : this.i18n._("Server (Post-NAT)"),
                    columnHeader : this.i18n._("Server (Post-NAT)"),
                    columnDataIndex: "postNatServer",
                    columnWidth : 100
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Client Port (Post-NAT)"),
                    columnHeader : this.i18n._("Client Port (Post-NAT)"),
                    columnDataIndex: "postNatClientPort",
                    columnWidth : 80
                },{
                    xtype : 'checkbox',
                    checked : true,
                    boxLabel : this.i18n._("Server Port (Post-NAT)"),
                    columnHeader : this.i18n._("Server Port (Post-NAT)"),
                    columnDataIndex: "postNatServerPort",
                    columnWidth : 80
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Local"),
                    columnHeader : this.i18n._("Local"),
                    columnDataIndex: "localTraffic",
                    columnWidth : 50
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("NATd"),
                    columnHeader : this.i18n._("NATd"),
                    columnDataIndex: "natted",
                    columnWidth : 50
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Port Forwarded"),
                    columnHeader : this.i18n._("Port Forwarded"),
                    columnDataIndex: "portForwarded",
                    columnWidth : 50
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Hostname"),
                    columnHeader : this.i18n._("Hostname"),
                    columnDataIndex : "platform-hostname",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Username"),
                    columnHeader : this.i18n._("Username"),
                    columnDataIndex : "platform-username",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control Lite - Protocol"),
                    columnHeader : this.i18n._("Application Control Lite - Protocol"),
                    columnDataIndex : "protofilter-protocol",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control Lite - Category"),
                    columnHeader : this.i18n._("Application Control Lite - Category"),
                    columnDataIndex : "protofilter-category",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control Lite - Description"),
                    columnHeader : this.i18n._("Application Control Lite - Description"),
                    columnDataIndex : "protofilter-description",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control Lite - Matched?"),
                    columnHeader : this.i18n._("Application Control Lite - Matched?"),
                    columnDataIndex : "protofilter-matched",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("HTTP - Hostname"),
                    columnHeader : this.i18n._("HTTP - Hostname"),
                    columnDataIndex : "http-hostname",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("HTTP - URI"),
                    columnHeader : this.i18n._("HTTP - URI"),
                    columnDataIndex : "http-uri",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Web Filter - Category ID"),
                    columnHeader : this.i18n._("Web Filter - Category ID"),
                    columnDataIndex : "esoft-best-category-id",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Web Filter - Category Name"),
                    columnHeader : this.i18n._("Web Filter - Category Name"),
                    columnDataIndex : "esoft-best-category-name",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Web Filter - Category Description"),
                    columnHeader : this.i18n._("Web Filter - Category Description"),
                    columnDataIndex : "esoft-best-category-description",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Web Filter - Category Flagged"),
                    columnHeader : this.i18n._("Web Filter - Category Flagged"),
                    columnDataIndex : "esoft-best-category-flagged",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Web Filter - Category Blocked"),
                    columnHeader : this.i18n._("Web Filter - Category Blocked"),
                    columnDataIndex : "esoft-best-category-blocked",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Web Filter - Flagged"),
                    columnHeader : this.i18n._("esoft-flagged"),
                    columnDataIndex : "esoft-flagged",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control - Application"),
                    columnHeader : this.i18n._("Application Control - Application"),
                    columnDataIndex : "classd-application",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control - Category"),
                    columnHeader : this.i18n._("Application Control - Category"),
                    columnDataIndex : "classd-category",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control - Protochain"),
                    columnHeader : this.i18n._("Application Control - Protochain"),
                    columnDataIndex : "classd-protochain",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control - Detail"),
                    columnHeader : this.i18n._("Application Control - Detail"),
                    columnDataIndex : "classd-detail",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control - Confidence"),
                    columnHeader : this.i18n._("Application Control - Confidence"),
                    columnDataIndex : "classd-confidence",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control - Productivity"),
                    columnHeader : this.i18n._("Application Control - Productivity"),
                    columnDataIndex : "classd-productivity",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Application Control - Risk"),
                    columnHeader : this.i18n._("Application Control - Risk"),
                    columnDataIndex : "classd-risk",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Bandwidth Control - Client KBps"),
                    columnHeader : this.i18n._("Bandwidth Control - Client KBps"),
                    columnDataIndex : "clientKBps",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Bandwidth Control - Server KBps"),
                    columnHeader : this.i18n._("Bandwidth Control - Server KBps"),
                    columnDataIndex : "serverKBps",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Bandwidth Control - Priority"),
                    columnHeader : this.i18n._("Bandwidth Control - Priority"),
                    columnDataIndex : "priority",
                    columnWidth : 150
                },{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("QoS - Priority"),
                    columnHeader : this.i18n._("QoS - Priority"),
                    columnDataIndex : "qosPriority",
                    columnWidth : 150
                },{
                    xtype : 'button',
                    id: "refresh_columns",
                    text : i18n._('Render'),
                    name : "Render",
                    tooltip : i18n._('Render the grid with the configured view'),
                    iconCls : 'icon-refresh',
                    handler : this.reRenderGrid
                }]
            });
        },
        // build the column selection panel (hidden by default)
        buildGroupSelectorPanel : function() {
            this.panelGroupSelector = Ext.create('Ext.panel.Panel',{
                name:'advanced',
                xtype:'fieldset',
                title:i18n._("Group Selection"),
                collapsible : true,
                collapsed : true,
                autoHeight : true,
                bodyStyle : 'padding:5px 5px 5px 5px;',
                items : [{
                    xtype : 'checkbox',
                    checked : false,
                    boxLabel : this.i18n._("Group by Client IP"),
                    columnHeader : this.i18n._("Group by Client IP"),
                    columnDataIndex: "group",
                    columnWidth : 70
                },{
                    xtype : 'button',
                    id: "refresh_columns",
                    text : i18n._('Render'),
                    name : "Render Columns",
                    tooltip : i18n._('Render the grid with the configured view'),
                    iconCls : 'icon-refresh',
                    handler : this.reRenderGrid
                }]
            });
        },
        // Current Sessions Grid
        buildGridCurrentSessions : function(columns, groupField) {
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
                groupField : groupField,
                columnsDefaultSortable : true,
                title : this.i18n._("Current Sessions"),
                qtip : this.i18n._("This shows all current sessions."),
                paginated : false,
                recordJavaClass : "com.untangle.uvm.SessionMonitorEntry",
                dataFn : this.getSessions,
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
                },{
                    name : "platform-hostname"
                },{
                    name : "platform-username"
                },{
                    name : "protofilter-protocol"
                },{
                    name : "protofilter-category"
                },{
                    name : "protofilter-description"
                },{
                    name : "protofilter-matched"
                },{
                    name : "http-hostname"
                },{
                    name : "http-uri"
                },{
                    name : "esoft-best-category-id"
                },{
                    name : "esoft-best-category-name"
                },{
                    name : "esoft-best-category-description"
                },{
                    name : "esoft-best-category-flagged"
                },{
                    name : "esoft-best-category-blocked"
                },{
                    name : "esoft-flagged"
                },{
                    name : "classd-application"
                },{
                    name : "classd-category"
                },{
                    name : "classd-protochain"
                },{
                    name : "classd-detail"
                },{
                    name : "classd-confidence"
                },{
                    name : "classd-productivity"
                },{
                    name : "classd-risk"
                },{
                    name : "clientKBps"
                },{
                    name : "serverKBps"
                },{
                    name : "priority"
                },{
                    name : "qosPriority"
                }],
                columns : columns,
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
//@ sourceURL=sessionMonitorNew.js
