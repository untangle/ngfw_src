if (!Ung.hasResource["Ung.SessionMonitor"]) {
    Ung.hasResource["Ung.SessionMonitor"] = true;
 
    
    // Monitor Grid class
    Ext.define('Ung.MonitorGrid', {
        extend:'Ext.grid.Panel',
        selType: 'rowmodel',
        //reserveScrollbar: true,
        // record per page
        recordsPerPage: 500,
        // the total number of records
        totalRecords: null,
        // settings component
        settingsCmp: null,
        // the list of fields used to by the Store
        fields: null,
        // the default sort field
        sortField: null,
        // the default sort order
        sortOrder: null,
        // the default group field
        groupField: null,
        // the columns are sortable by default, if sortable is not specified
        columnsDefaultSortable: true,
        // paginate the grid by default
        paginated: true,
        // javaClass of the record, used in save function to create correct json-rpc
        // object
        async: true,
        autoRefreshEnabled: false,        
        // the map of changed data in the grid
        // used by rendering functions and by save
        enableColumnHide: false,
        enableColumnMove: false,
        features: [{
            ftype: 'filters',
            encode: false,
            local: true
        }, {ftype: 'groupingsummary'}],
        constructor: function(config) {
            var defaults = {
                data: [],
                plugins: [
                ],
                viewConfig: {
                    enableTextSelection: true,
                    stripeRows: true,
                    loadMask:{
                        msg: i18n._("Loading...")
                    }
                },
                changedData: {},
                subCmps:[]
            };
            Ext.applyIf(config, defaults);
            this.callParent(arguments);
        },
        initComponent: function() {
            for (var i = 0; i < this.columns.length; i++) {
                var col=this.columns[i];
                col.menuDisabled= true;
                if( col.sortable == null) {
                    col.sortable = this.columnsDefaultSortable;
                }
            }    
            
            if(this.dataFn) {
                if(this.dataRoot === undefined) {
                    this.dataRoot="list";
                }
            } else {
                this.async=false;
            }
            
            this.totalRecords = this.data.length;
            this.store=Ext.create('Ext.data.Store',{
                data: [],
                fields: this.fields,
                pageSize: this.paginated?this.recordsPerPage:null,
                proxy: {
                    type: this.paginated?'pagingmemory':'memory',
                    reader: {
                        type: 'json' 
                    }
                },
                autoLoad: false,
                sorters: this.sortField ? {
                    property: this.sortField,
                    direction: this.sortOrder ? this.sortOrder: "ASC"
                }: null,
                groupField: this.groupField,
                remoteSort: this.paginated
            });
            this.bbar = [{
                xtype: 'button',
                id: "refresh_"+this.getId(),
                text: i18n._('Refresh'),
                name: "Refresh",
                tooltip: i18n._('Refresh'),
                iconCls: 'icon-refresh',
                handler: Ext.bind(function() {
                    this.reload();
                }, this)
            },{
                xtype: 'button',
                id: "auto_refresh_"+this.getId(),
                text: i18n._('Auto Refresh'),
                enableToggle: true,
                pressed: false,
                name: "Auto Refresh",
                tooltip: i18n._('Auto Refresh'),
                iconCls: 'icon-autorefresh',
                handler: Ext.bind(function() {
                    var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
                    if(autoRefreshButton.pressed) {
                        this.startAutoRefresh();
                    } else {
                        this.stopAutoRefresh();
                    }
                }, this)
            },'-',{
                text: i18n._('Clear Filters'),
                handler: Ext.bind(function () {
                    this.filters.clearFilters();
                }, this) 
            }];
            if(this.paginated) {
                this.pagingToolbar = Ext.create('Ext.toolbar.Paging',{
                    store: this.getStore(),
                    style: "border:0; top:1px;",
                    displayInfo: true,
                    displayMsg: i18n._('Displaying sessions {0} - {1} of {2}'),
                    emptyMsg: i18n._("No sessions to display")
                });
                this.bbar.push('-',this.pagingToolbar);
            }
            this.callParent(arguments);
        },
        afterRender: function() {
            this.callParent(arguments);
            this.initialLoad();
        },
        initialLoad: function() {
            // load first page initialy
            this.getView().setLoading(true);  
            Ext.defer(function(){
                this.buildData(Ext.bind(function() {
                    this.getStore().loadPage(1, {
                        limit:this.isPaginated() ? this.recordsPerPage: Ung.Util.maxRowCount,
                        callback: function() {
                            this.getView().setLoading(false);
                        },
                        scope: this
                    });
                }, this));
            },10, this);
        },
        getData: function(data) {
            if(!data) {
                if(this.dataFn) {
                    if (this.dataFnArg !== undefined && this.dataFnArg != null) {
                        data = this.dataFn(this.dataFnArg);
                    } else {
                        data = this.dataFn();
                    }
                    this.data = (this.dataRoot!=null && this.dataRoot.length>0) ? data[this.dataRoot]:data;
                }
            } else {
                this.data=(this.dataRoot!=null && this.dataRoot.length>0) ? data[this.dataRoot]:data;;
            }

            if(!this.data) {
                this.data=[];
            }
            return this.data;
        },
        buildData: function(handler) {
            if(this.async) {
                if (this.dataFnArg !== undefined && this.dataFnArg != null) {
                    this.dataFn(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        this.getData(result);
                        this.afterDataBuild(handler);
                    }, this),this.dataFnArg);
                } else {
                    this.dataFn(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        this.getData(result);
                        this.afterDataBuild(handler);
                    }, this));
                }
            } else {
                this.getData();
                this.afterDataBuild(handler);
            }

        },
        afterDataBuild: function(handler) {
            this.getStore().getProxy().data = this.data;
            this.setTotalRecords(this.data.length);
            if(handler) {
                handler();
            }
        },
        // is grid paginated
        isPaginated: function() {
            return  this.paginated && (this.totalRecords != null && this.totalRecords >= this.recordsPerPage);
        },
        beforeDestroy: function() {
            Ext.each(this.subCmps, Ext.destroy);
            this.callParent(arguments);
        },
        // load a page
        loadPage: function(page, callback, scope, arg) {
            this.getStore().loadPage(page, {
                limit:this.isPaginated() ? this.recordsPerPage: Ung.Util.maxRowCount,
                callback: callback,
                scope: scope,
                arg: arg
            });
        },
        reload: function() {
            this.getView().setLoading(true);
            Ext.defer(function(){
                this.buildData(Ext.bind(function() {
                    this.getStore().loadPage(this.getStore().currentPage, {
                        limit:this.isPaginated() ? this.recordsPerPage: Ung.Util.maxRowCount,
                        callback: function() {
                            this.getView().setLoading(false);
                        },
                        scope: this
                    });
                }, this));
            },10, this);
        },
        // Set the total number of records
        setTotalRecords: function(totalRecords) {
            this.totalRecords = totalRecords;
            if(this.paginated) {
                var isPaginated=this.isPaginated();
                this.getStore().pageSize=isPaginated?this.recordsPerPage:Ung.Util.maxRowCount;
                if(!isPaginated) {
                    //Needs to set currentPage to 1 when not using pagination toolbar.
                    this.getStore().currentPage=1;
                }
                var bbar=this.getDockedItems('toolbar[dock="bottom"]')[0];
                if (isPaginated) {
                    this.pagingToolbar.show();
                    this.pagingToolbar.enable();
                } else {
                    this.pagingToolbar.hide();
                    this.pagingToolbar.disable();
                }
            }
        },
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
        autorefreshList: function() {
            if(this!=null && this.autoRefreshEnabled && Ext.getCmp(this.id) != null) {
                this.reload();
                Ext.defer(this.autorefreshList, 9000, this);
            }
        }
    });
    
    Ext.define('Ung.SessionMonitor', {
        extend: 'Ung.StatusWin',
        helpSource: 'session_monitor',
        sortField:'bypassed',
        sortOrder: 'ASC',
        defaultBandwidthColumns: false,
        enableBandwidthColumns: false,
        initComponent: function() {
            this.breadcrumbs = [{
                title: this.i18n._('Session Viewer')
            }];

            this.buildPanel();
            this.items = [this.sessionPanel];
            this.callParent(arguments);
        },
        closeWindow: function() {
            this.gridCurrentSessions.stopAutoRefresh(true);
            this.hide();
        },
        getSessions: function(handler, nodeId) {
            if (!this.isVisible()) {
                 handler({javaClass:"java.util.LinkedList", list:[]});
                 return;
            }
            rpc.sessionMonitor.getMergedSessions(Ext.bind(function(result, exception) {
                if(exception) {
                    handler(result, exception)
                    return;
                }
                var sessions = result.list;
                if(testMode) {
                    var testSessionsSize=450 + Math.floor((Math.random()*100));
                    for(var i=0;i<testSessionsSize;i++) {
                        var ii=i+Math.floor((Math.random()*5));
                        sessions.push({
                            "postNatServer": "184.27.239."+(ii%10),
                            "bypassed": ((ii%3)==1),
                            "state": null,
                            "natted": true,
                            "totalKBps": null,
                            "localTraffic": false,
                            "priority": (ii%7)+1,
                            "postNatClient": "50.193.63."+((ii+1)%10),
                            "postNatClientPort": (ii+1000),
                            "preNatClient": "10.0.0."+((i+2)%10),
                            "preNatServer": "184.27.239."+((i+3)%10),
                            "attachments": {
                                "map": {
                                    "esoft-best-category-name": "Social Networking",
                                    "protofilter-matched": (ii%3==0),
                                    "esoft-best-category-description": "Social Networking",
                                    "esoft-best-category-blocked": false,
                                    "esoft-flagged": false,
                                    "platform-hostname": "acct07-wxp"+i,
                                    "esoft-best-category-flagged": (ii%2==1),
                                    "esoft-best-category-id": null,
                                    "http-uri": "/t.gif",
                                    "platform-username": "rbooroojian"+i,
                                    "http-hostname": "p.twitter.com"+i
                                },
                                "javaClass": "java.util.HashMap"
                            },
                            "protocol": (ii%2==1)?"TCP":"UDP",
                            "serverKBps": null,
                            "portForwarded": (ii%2==0),
                            "preNatClientPort": 1471,
                            "preNatServerPort": i+1500,
                            "serverIntf": ii%10,
                            "clientIntf": i%9,
                            "sessionId": 88616525732127+i,
                            "javaClass": "com.untangle.uvm.SessionMonitorEntry",
                            "qosPriority": (ii%8),
                            "clientKBps": null,
                            "policy": (ii%5==2)?null:(ii%5)+ "",
                            "postNatServerPort": (ii+2000)
                        });                 
                    }
                }
                // iterate through each session and change its attachments map to properties
                for (var i = 0; i < sessions.length ; i++) {
                    var session = sessions[i];
                    if (session.attachments != null) {
                        for (var prop in session.attachments.map) {
                            session[prop] = session.attachments.map[prop];
                        }
                    }
                }
                handler({javaClass:"java.util.LinkedList", list:sessions});
            }, this), nodeId);
        },
        setFilterNodeId: function(nodeId) {
            // set dataFnArg (for node limit) as necessary
            for ( var i = 0 ; i < this.panelNodeSelector.items.items.length ; i++ ) {
                var item = this.panelNodeSelector.items.items[i];
                if ( item.xtype == "radio" ) {
                    if ( item.dataFnArg == nodeId) {
                        item.setValue(true);
                    } else {
                        item.setValue(false);
                    }
                }
            }
            this.reRenderGrid();
        },
        buildPanel: function() {
            this.enabledColumns = {};
            this.columns = [];
            this.groupField = null;
            var nodeStr = "";
            var groupStr = "";
            this.reRenderGrid = Ext.bind(function() {
                this.columns = [];
                this.enabledColumns = {};
                this.groupField = null;
                var nodeName = "xxxxxxxxxxxxxxxxxx";
                // set dataFnArg (for node limit) as necessary
                for ( var i = 0 ; i < this.panelNodeSelector.items.items.length ; i++ ) {
                    var item = this.panelNodeSelector.items.items[i];
                    if ( item.xtype == "radio" ) {
                        this.enabledColumns[item.name] = item.checked;
                        if (item.checked) {
                            this.dataFnArg = item.dataFnArg;
                            nodeName = item.nodeName;
                            // change title
                            nodeStr = " - " + item.boxLabel;
                            if ( this.gridCurrentSessions !== undefined ) 
                                this.gridCurrentSessions.dataFnArg = item.dataFnArg;
                        }
                    }
                }
                // add/remove columns as necessary
                for ( var i = 0 ; i < this.panelColumnSelector.items.items.length ; i++ ) {
                    var item = this.panelColumnSelector.items.items[i];
                    if ( item.xtype == "checkbox" ) {
                        this.enabledColumns[item.name] = item.checked;
                        // if the item is checked, or if we're only showing a specific node's sessions
                        // show the column related to that node
                        if (item.checked || item.gridColumnHeader.indexOf("(" + nodeName+ ")") != -1) {
                            var newColumn = {
                                header: item.gridColumnHeader,
                                dataIndex: item.gridColumnDataIndex,
                                width: item.gridColumnWidth,
                                summaryType: item.gridColumnSummaryType,
                                filter: item.gridColumnFilter
                            };
                            if ( item.gridColumnRenderer !== undefined )
                                newColumn.renderer = item.gridColumnRenderer;
                            this.columns.push( newColumn );
                        }
                    }
                }
                // add grouping if enabled
                for ( var i = 0 ; i < this.panelGroupSelector.items.items.length ; i++ ) {
                    var item = this.panelGroupSelector.items.items[i];
                    if ( item.xtype == "radio" ) {
                        if (item.checked) {
                            this.groupField = item.groupField;
                        }
                    }
                }
                // if the grid is already rendered it - force re-render it
                if ( this.gridCurrentSessions == undefined ) {
                    this.buildGridCurrentSessions(this.columns, this.groupField);
                } else {
                    if ( this.groupField != null ) {
                        groupStr = " - Grouping:" + this.groupField;
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
                if ( this.gridCurrentSessions !== undefined ) {
                    this.gridCurrentSessions.setTitle(i18n._("Current Sessions") + nodeStr + groupStr );
                    this.gridCurrentSessions.reload();

                }

            }, this);
            
            // manually call the renderer for the first render
            this.buildGroupSelectorPanel();
            this.buildNodeSelectorPanel();
            this.buildColumnSelectorPanel();
            this.reRenderGrid();
            this.buildGridCurrentSessions(this.columns, this.groupField);
            this.buildSessionPanel();
        },
        buildSessionPanel: function() {
            this.sessionPanel = Ext.create('Ext.panel.Panel',{
                //title: this.i18n._('Session Viewer'),
                name: 'Session Viewer',
                helpSource: 'session_monitor',
                layout: "anchor",
                defaults: {
                    anchor: '98%',
                    autoScroll: true
                },
                autoScroll: true,
                cls: 'ung-panel',
                items: [this.gridCurrentSessions, this.panelColumnSelector, this.panelNodeSelector, this.panelGroupSelector]
            });
        },
        buildColumnSelectorPanel: function() {
            var interfaceStore=Ung.Util.getInterfaceStore();
            var policyListOptions=[[null, i18n._( "Services" )], ["0", i18n._("No Rack")]];
            for( var i=0 ; i<rpc.policies.length ; i++ ) {
                var policy = rpc.policies[i];
                policyListOptions.push([policy.policyId+"", policy.name]);
            }
            var priorityList=[i18n._("Very High"), i18n._("High"), i18n._("Medium"), i18n._("Low"), i18n._("Limited"), i18n._("Limited More"), i18n._("Limited Severely")];
            var priorityOptions = [[1, i18n._("Very High")], [2, i18n._("High")], [3, i18n._("Medium")] , [4, i18n._("Low")], [5, i18n._("Limited")], [6, i18n._("Limited More")], [7, i18n._("Limited Severely")]];
            this.panelColumnSelector = Ext.create('Ext.panel.Panel',{
                name:'advanced',
                xtype:'fieldset',
                layout: {
                    type: 'table',
                    columns: 7
                },
                title:i18n._("Column Selection"),
                collapsible: true,
                collapsed: true,
                bodyStyle: 'padding:5px 5px 5px 5px;',
                items: [{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Protocol"),
                    gridColumnHeader: this.i18n._("Protocol"),
                    gridColumnDataIndex: "protocol",
                    gridColumnWidth: 60,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Bypassed"),
                    gridColumnHeader: this.i18n._("Bypassed"),
                    gridColumnDataIndex: "bypassed",
                    gridColumnWidth: 60,
                    gridColumnFilter: {
                        type: 'boolean',
                        yesText: 'true',
                        noText: 'false'
                    }
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Policy"),
                    gridColumnHeader: this.i18n._("Policy"),
                    gridColumnDataIndex: "policy",
                    gridColumnWidth: 80,
                    gridColumnRenderer: function(value) {
                        return main.getPolicyName(value);
                    },
                    gridColumnFilter: {
                        type: 'list',
                        options: policyListOptions
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 4
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Client Interface"),
                    gridColumnHeader: this.i18n._("Client Interface"),
                    gridColumnDataIndex: "clientIntf",
                    gridColumnWidth: 85,
                    gridColumnRenderer: function(value) {
                        var record = interfaceStore.findRecord("key", value);
                        return record==null?value==null?"":Ext.String.format( i18n._("Interface {0}"), value ):record.get("name");
                    },
                    gridColumnFilter: {
                        type: 'numeric'
                    }
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Server Interface"),
                    gridColumnHeader: this.i18n._("Server Interface"),
                    gridColumnDataIndex: "serverIntf",
                    gridColumnWidth: 85,
                    gridColumnRenderer: function(value) {
                        var record = interfaceStore.findRecord("key", value);
                        return record==null?value==null?"":Ext.String.format( i18n._("Interface {0}"), value ):record.get("name");
                    },
                    gridColumnFilter: {
                        type: 'numeric'
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 5
                },{
                    xtype: 'checkbox',
                    column: 3,
                    checked: true,
                    boxLabel: this.i18n._("Client (Pre-NAT)"),
                    gridColumnHeader: this.i18n._("Client (Pre-NAT)"),
                    gridColumnDataIndex: "preNatClient",
                    gridColumnWidth: 75,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Server (Pre-NAT)"),
                    gridColumnHeader: this.i18n._("Server (Pre-NAT)"),
                    gridColumnDataIndex: "preNatServer",
                    gridColumnWidth: 75,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Client Port (Pre-NAT)"),
                    gridColumnHeader: this.i18n._("Client Port (Pre-NAT)"),
                    gridColumnDataIndex: "preNatClientPort",
                    gridColumnWidth: 70,
                    gridColumnFilter: {
                        type: 'numeric'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Server Port (Pre-NAT)"),
                    gridColumnHeader: this.i18n._("Server Port (Pre-NAT)"),
                    gridColumnDataIndex: "preNatServerPort",
                    gridColumnWidth: 70,
                    gridColumnFilter: {
                        type: 'numeric'
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 3
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Client (Post-NAT)"),
                    gridColumnHeader: this.i18n._("Client (Post-NAT)"),
                    gridColumnDataIndex: "postNatClient",
                    gridColumnWidth: 75,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Server (Post-NAT)"),
                    gridColumnHeader: this.i18n._("Server (Post-NAT)"),
                    gridColumnDataIndex: "postNatServer",
                    gridColumnWidth: 75,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Client Port (Post-NAT)"),
                    gridColumnHeader: this.i18n._("Client Port (Post-NAT)"),
                    gridColumnDataIndex: "postNatClientPort",
                    gridColumnWidth: 70,
                    gridColumnFilter: {
                        type: 'numeric'
                    }
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Server Port (Post-NAT)"),
                    gridColumnHeader: this.i18n._("Server Port (Post-NAT)"),
                    gridColumnDataIndex: "postNatServerPort",
                    gridColumnWidth: 70,
                    gridColumnFilter: {
                        type: 'numeric'
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 3
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Local"),
                    gridColumnHeader: this.i18n._("Local"),
                    gridColumnDataIndex: "localTraffic",
                    gridColumnWidth: 50,
                    gridColumnFilter: {
                        type: 'boolean',
                        yesText: 'true',
                        noText: 'false'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("NATd"),
                    gridColumnHeader: this.i18n._("NATd"),
                    gridColumnDataIndex: "natted",
                    gridColumnWidth: 50,
                    gridColumnFilter: {
                        type: 'boolean',
                        yesText: 'true',
                        noText: 'false'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Port Forwarded"),
                    gridColumnHeader: this.i18n._("Port Forwarded"),
                    gridColumnDataIndex: "portForwarded",
                    gridColumnWidth: 50,
                    gridColumnFilter: {
                        type: 'boolean',
                        yesText: 'true',
                        noText: 'false'
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 4
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Hostname"),
                    gridColumnHeader: this.i18n._("Hostname"),
                    gridColumnDataIndex: "platform-hostname",
                    gridColumnWidth: 100,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Username"),
                    gridColumnHeader: this.i18n._("Username"),
                    gridColumnDataIndex: "platform-username",
                    gridColumnWidth: 100,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 5
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Application Control Lite - " + this.i18n._("Protocol"),
                    gridColumnHeader: this.i18n._("Protocol") + " (Application Control Lite)",
                    gridColumnDataIndex: "protofilter-protocol",
                    gridColumnWidth: 100,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Application Control Lite - " + this.i18n._("Category"),
                    gridColumnHeader: this.i18n._("Category") + " (Application Control Lite)",
                    gridColumnDataIndex: "protofilter-category",
                    gridColumnWidth: 100,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Application Control Lite - " + this.i18n._("Description"),
                    gridColumnHeader: this.i18n._("Description") + " (Application Control Lite)",
                    gridColumnDataIndex: "protofilter-description",
                    gridColumnWidth: 100,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Application Control Lite - " + this.i18n._("Matched?"),
                    gridColumnHeader: this.i18n._("Matched?") + " (Application Control Lite)",
                    gridColumnDataIndex: "protofilter-matched",
                    gridColumnWidth: 100,
                    gridColumnFilter: {
                        type: 'boolean',
                        yesText: 'true',
                        noText: 'false'
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 3
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "HTTP - " + this.i18n._("Hostname"),
                    gridColumnHeader: this.i18n._("Hostname") + " (HTTP)",
                    gridColumnDataIndex: "http-hostname",
                    gridColumnWidth: 120,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "HTTP - " + this.i18n._("URI"),
                    gridColumnHeader: this.i18n._("URI") + " (HTTP)",
                    gridColumnDataIndex: "http-uri",
                    gridColumnWidth: 120,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 5
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Web Filter - " + this.i18n._("Category Name"),
                    gridColumnHeader: this.i18n._("Category Name") + " (Web Filter)",
                    gridColumnDataIndex: "esoft-best-category-name",
                    gridColumnWidth: 100,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Web Filter - " + this.i18n._("Category Description"),
                    gridColumnHeader: this.i18n._("Category Description") + " (Web Filter)",
                    gridColumnDataIndex: "esoft-best-category-description",
                    gridColumnWidth: 100,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Web Filter - " + this.i18n._("Category Flagged"),
                    gridColumnHeader: this.i18n._("Category Flagged") + " (Web Filter)",
                    gridColumnDataIndex: "esoft-best-category-flagged",
                    gridColumnWidth: 50,
                    gridColumnFilter: {
                        type: 'boolean',
                        yesText: 'true',
                        noText: 'false'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Web Filter - " + this.i18n._("Category Blocked"),
                    gridColumnHeader: this.i18n._("Category Blocked") + " (Web Filter)",
                    gridColumnDataIndex: "esoft-best-category-blocked",
                    gridColumnWidth: 50,
                    gridColumnFilter: {
                        type: 'boolean',
                        yesText: 'true',
                        noText: 'false'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Web Filter - " + this.i18n._("Content Type"),
                    gridColumnHeader: this.i18n._("Content Type") + " (Web Filter)",
                    gridColumnDataIndex: "esoft-content-type",
                    gridColumnWidth: 50,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Web Filter - " + this.i18n._("Flagged"),
                    gridColumnHeader: this.i18n._("Flagged") + " (Web Filter)",
                    gridColumnDataIndex: "esoft-flagged",
                    gridColumnWidth: 50,
                    gridColumnFilter: {
                        type: 'boolean',
                        yesText: 'true',
                        noText: 'false'
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 2
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: "Application Control - " + this.i18n._("Protochain"),
                    gridColumnHeader: this.i18n._("Protochain") + " (Application Control)",
                    gridColumnDataIndex: "classd-protochain",
                    gridColumnWidth: 140,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Application Control - " + this.i18n._("Application"),
                    gridColumnHeader: this.i18n._("Application") + " (Application Control)",
                    gridColumnDataIndex: "classd-application",
                    gridColumnWidth: 100,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Application Control - " + this.i18n._("Category"),
                    gridColumnHeader: this.i18n._("Category") + " (Application Control)",
                    gridColumnDataIndex: "classd-category",
                    gridColumnWidth: 100,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Application Control - " + this.i18n._("Detail"),
                    gridColumnHeader: this.i18n._("Detail") + " (Application Control)",
                    gridColumnDataIndex: "classd-detail",
                    gridColumnWidth: 120,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Application Control - " + this.i18n._("Confidence"),
                    gridColumnHeader: this.i18n._("Confidence") + " (Application Control)",
                    gridColumnDataIndex: "classd-confidence",
                    gridColumnWidth: 50,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Application Control - " + this.i18n._("Productivity"),
                    gridColumnHeader: this.i18n._("Productivity") + " (Application Control)",
                    gridColumnDataIndex: "classd-productivity",
                    gridColumnWidth: 50,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "Application Control - " + this.i18n._("Risk"),
                    gridColumnHeader: this.i18n._("Risk") + " (Application Control)",
                    gridColumnDataIndex: "classd-risk",
                    gridColumnWidth: 50,
                    gridColumnFilter: {
                        type: 'string'
                    }
                },{
                    xtype: 'checkbox',
                    checked: this.defaultBandwidthColumns,
                    disabled: !this.enableBandwidthColumns,
                    boxLabel: "Bandwidth Control - " + this.i18n._("Client KBps"),
                    gridColumnHeader: this.i18n._("Client KB/s"),
                    gridColumnDataIndex: "clientKBps",
                    gridColumnWidth: 80,
                    gridColumnSummaryType: "sum",
                    gridColumnFilter: {
                        type: 'numeric'
                    }
                },{
                    xtype: 'checkbox',
                    checked: this.defaultBandwidthColumns,
                    disabled: !this.enableBandwidthColumns,
                    boxLabel: "Bandwidth Control - " + this.i18n._("Server KBps"),
                    gridColumnHeader: this.i18n._("Server KB/s"),
                    gridColumnDataIndex: "serverKBps",
                    gridColumnWidth: 80,
                    gridColumnSummaryType: "sum",
                    gridColumnFilter: {
                        type: 'numeric'
                    }
                },{
                    xtype: 'checkbox',
                    checked: this.defaultBandwidthColumns,
                    disabled: !this.enableBandwidthColumns,
                    boxLabel: "Bandwidth Control - " + this.i18n._("Total KBps"),
                    gridColumnHeader: this.i18n._("Total KB/s"),
                    gridColumnDataIndex: "totalKBps",
                    gridColumnWidth: 80,
                    gridColumnSummaryType: "sum",
                    gridColumnFilter: {
                        type: 'numeric'
                    }
                },{
                    xtype: 'checkbox',
                    checked: this.defaultBandwidthColumns,
                    boxLabel: "Bandwidth Control - " + this.i18n._("Priority"),
                    gridColumnHeader: this.i18n._("Priority") + " (Bandwidth Control)",
                    gridColumnDataIndex: "priority",
                    gridColumnWidth: 80,
                    gridColumnRenderer: function(value) {
                        return (value < 1 || value > 7)?i18n._("None"):priorityList[value-1];
                    },
                    gridColumnFilter: {
                        type: 'list',
                        options: priorityOptions
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 3
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: "QoS - " + this.i18n._("Priority"),
                    gridColumnHeader: this.i18n._("Priority") + " (QoS)",
                    gridColumnDataIndex: "qosPriority",
                    gridColumnWidth: 100,
                    gridColumnRenderer: function(value) {
                        return (value < 1 || value > 7)?i18n._("None"):priorityList[value-1];
                    },
                    gridColumnFilter: {
                        type: 'list',
                        options: priorityOptions
                    }
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 6
                },{
                    xtype: 'button',
                    id: "refresh_columns",
                    text: i18n._('Render'),
                    name: "Render",
                    tooltip: i18n._('Render the grid with the configured view'),
                    iconCls: 'icon-refresh',
                    handler: this.reRenderGrid
                }]
            });
        },
        // build the panel for node selection
        buildNodeSelectorPanel: function() {
            var items = [{
                xtype: 'radio',
                name: 'nodeRadio',
                checked: true,
                boxLabel: this.i18n._("All Sessions"),
                dataFnArg: 0
            }];
            var nodeIds = rpc.nodeManager.nodeInstancesIds();
            var allNodeProperties = rpc.nodeManager.allNodeProperties();
            var allNodeSettings = rpc.nodeManager.allNodeSettings();
            for (var i = 0 ; i < nodeIds.list.length ; i++) {
                var nodeId = nodeIds.list[i];
                var nodeProperties = allNodeProperties.map[nodeId];
                var nodeSettings = allNodeSettings.map[nodeId];
                if (nodeProperties.viewPosition != null) {
                    items.push({
                        xtype: 'radio',
                        name: 'nodeRadio',
                        checked: false,
                        boxLabel: i18n._('Sessions for') + ' ' + nodeProperties.displayName + " [" + main.getPolicyName(nodeSettings.policyId) + "] ",
                        dataFnArg: nodeSettings.id,
                        nodeName: nodeProperties.displayName
                    });
                }
            }
            items.push({
                xtype: 'button',
                id: "refresh_nodes",
                text: i18n._('Render'),
                name: "Render",
                tooltip: i18n._('Render the grid with the configured view'),
                iconCls: 'icon-refresh',
                handler: this.reRenderGrid
            });

            this.panelNodeSelector = Ext.create('Ext.panel.Panel',{
                name:'advanced',
                xtype:'fieldset',
                layout: 'anchor',
                title:i18n._("App Selection"),
                collapsible: true,
                collapsed: true,
                bodyStyle: 'padding:5px 5px 5px 5px;',
                items: items
            });

        },
        // build the column selection panel (hidden by default)
        buildGroupSelectorPanel: function() {
            this.panelGroupSelector = Ext.create('Ext.panel.Panel',{
                name:'advanced',
                xtype:'fieldset',
                layout: {
                    type: 'table',
                    columns: 7
                },
                title:i18n._("Grouping Field"),
                collapsible: true,
                collapsed: true,
                bodyStyle: 'padding:5px 5px 5px 5px;',
                items: [{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: true,
                    boxLabel: this.i18n._("No Grouping"),
                    groupField: null
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 6
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Protocol"),
                    groupField: "protocol"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Bypassed"),
                    groupField: "bypassed"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Policy"),
                    groupField: "policy"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 4
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Client Interface"),
                    groupField: "clientIntf"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Server Interface"),
                    groupField: "serverIntf"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 5
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Client (Pre-NAT)"),
                    groupField: "preNatClient"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Server (Pre-NAT)"),
                    groupField: "preNatServer"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Client Port (Pre-NAT)"),
                    groupField: "preNatClientPort"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Server Port (Pre-NAT)"),
                    groupField: "preNatServerPort"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 3
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Client (Post-NAT)"),
                    groupField: "postNatClient"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Server (Post-NAT)"),
                    groupField: "postNatServer"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Client Port (Post-NAT)"),
                    groupField: "postNatClientPort"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Server Port (Post-NAT)"),
                    groupField: "postNatServerPort"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 3
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Local"),
                    groupField: "localTraffic"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("NATd"),
                    groupField: "natted"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Port Forwarded"),
                    groupField: "portForwarded"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 4
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Hostname"),
                    groupField: "platform-hostname"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Username"),
                    groupField: "platform-username"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 5
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control Lite - Protocol"),
                    groupField: "protofilter-protocol"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control Lite - Category"),
                    groupField: "protofilter-category"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control Lite - Description"),
                    groupField: "protofilter-description"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control Lite - Matched?"),
                    groupField: "protofilter-matched"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 3
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("HTTP - Hostname"),
                    groupField: "http-hostname"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("HTTP - URI"),
                    groupField: "http-uri"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 5
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Category Name"),
                    groupField: "esoft-best-category-name"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Category Description"),
                    groupField: "esoft-best-category-description"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Category Flagged"),
                    groupField: "esoft-best-category-flagged"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Category Blocked"),
                    groupField: "esoft-best-category-blocked"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Content Type"),
                    groupField: "esoft-content-type"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Flagged"),
                    groupField: "esoft-flagged"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 2
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Application"),
                    groupField: "classd-application"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Category"),
                    groupField: "classd-category"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Protochain"),
                    groupField: "classd-protochain"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Detail"),
                    groupField: "classd-detail"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Confidence"),
                    groupField: "classd-confidence"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Productivity"),
                    groupField: "classd-productivity"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Risk"),
                    groupField: "classd-risk"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Bandwidth Control - Client KBps"),
                    groupField: "clientKBps"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Bandwidth Control - Server KBps"),
                    groupField: "serverKBps"
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("Bandwidth Control - Priority"),
                    groupField: "priority"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 4
                },{
                    xtype: 'radio',
                    name: 'groupingRadio',
                    checked: false,
                    boxLabel: this.i18n._("QoS - Priority"),
                    groupField: "qosPriority"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 6
                },{
                    xtype: 'button',
                    id: "refresh_grouping",
                    text: i18n._('Render'),
                    name: "Render Columns",
                    tooltip: i18n._('Render the grid with the configured view'),
                    iconCls: 'icon-refresh',
                    handler: this.reRenderGrid
                }]
            });
        },
        // Current Sessions Grid
        buildGridCurrentSessions: function(columns, groupField) {
            this.gridCurrentSessions = Ext.create('Ung.MonitorGrid',{
                name: "gridCurrentSessions",
                settingsCmp: this,
                height: 500,
                sortField: this.sortField,
                sortOrder: this.sortOrder,
                groupField: groupField,
                title: this.i18n._("Current Sessions"),
                qtip: this.i18n._("This shows all current sessions."),
                dataFn: Ext.bind(this.getSessions, this),
                dataFnArg: 0,
                columns: columns,
                fields: [{
                    name: "id"
                },{
                    name: "protocol"
                },{
                    name: "bypassed"
                },{
                    name: "localTraffic"
                },{
                    name: "policy"
                },{
                    name: "preNatClient"
                },{
                    name: "preNatServer"
                },{
                    name: "preNatClientPort"
                },{
                    name: "preNatServerPort"
                },{
                    name: "postNatClient"
                },{
                    name: "postNatServer"
                },{
                    name: "postNatClientPort"
                },{
                    name: "postNatServerPort"
                },{
                    name: "clientIntf"
                },{
                    name: "serverIntf"
                },{
                    name: "natted"
                },{
                    name: "portForwarded"
                },{
                    name: "platform-hostname"
                },{
                    name: "platform-username"
                },{
                    name: "protofilter-protocol"
                },{
                    name: "protofilter-category"
                },{
                    name: "protofilter-description"
                },{
                    name: "protofilter-matched"
                },{
                    name: "http-hostname"
                },{
                    name: "http-uri"
                },{
                    name: "esoft-best-category-name"
                },{
                    name: "esoft-best-category-description"
                },{
                    name: "esoft-best-category-flagged"
                },{
                    name: "esoft-best-category-blocked"
                },{
                    name: "esoft-flagged"
                },{
                    name: "classd-application"
                },{
                    name: "classd-category"
                },{
                    name: "classd-protochain"
                },{
                    name: "classd-detail"
                },{
                    name: "classd-confidence"
                },{
                    name: "classd-productivity"
                },{
                    name: "classd-risk"
                },{
                    name: "clientKBps",
                    convert: function(val, rec) {
                        return Math.round(val*10)/10;
                    }
                },{
                    name: "serverKBps",
                    convert: function(val, rec) {
                        return Math.round(val*10)/10;
                    }
                },{
                    name: "totalKBps",
                    convert: function(val, rec) {
                        if ( rec.data.serverKBps == null )
                            return null;
                        if ( rec.data.clientKBps == null )
                            return null;
                        else
                            return (Math.round(rec.data.clientKBps*10))/10 + (Math.round(rec.data.serverKBps*10))/10;
                    }
                },{
                    name: "priority"
                },{
                    name: "qosPriority"
                }]
            });
        }
    });
}
//@ sourceURL=sessionMonitor.js
