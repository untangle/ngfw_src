if (!Ung.hasResource["Ung.SessionMonitor"]) {
    Ung.hasResource["Ung.SessionMonitor"] = true;
 
    
    // Monitor Grid class
    Ext.define('Ung.MonitorGrid', {
        extend:'Ext.grid.Panel',
        selType: 'rowmodel',
        // record per page
        recordsPerPage: 500,
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
        async: true,
        //an applicaiton selector
        appList: null,
        // the total number of records
        totalRecords: null,
        autoRefreshEnabled: false,        
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
            this.bbar=[];
            if(this.appList!=null) {
                this.bbar.push({
                    xtype: 'tbtext',
                    id: "appSelectorBox_"+this.getId(),
                    text: ''
                });
            }
            this.bbar.push({
                xtype: 'tbtext',
                id: "appSelector_"+this.getId(),
                text: ''
            }, {
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
                tooltip: i18n._('Filters can be added by clicking on column headers arrow down menu and using Filters menu'),
                handler: Ext.bind(function () {
                    this.filters.clearFilters();
                }, this) 
            },{
                text: i18n._('Clear Grouping'),
                tooltip: i18n._('Grouping be can be used by clicking on column headers arrow down menu and clicking Group by this field'),
                handler: Ext.bind(function () {
                    this.getStore().clearGrouping();
                }, this) 
            });
            if(this.paginated) {
                this.pagingToolbar = Ext.create('Ext.toolbar.Paging',{
                    store: this.getStore(),
                    style: "border:0; top:1px;",
                    displayInfo: true,
                    displayMsg: i18n._('{0} - {1} of {2}'),
                    emptyMsg: i18n._("No topics to display")
                });
                this.bbar.push('-',this.pagingToolbar);
            }
            this.callParent(arguments);
        },
        afterRender: function() {
            this.callParent(arguments);
            if(this.appList!=null) {
                out = [];
                out.push('<select name="appSelector" id="appSelector_' + this.getId() + '" onchange="Ext.getCmp(\''+this.getId()+'\').changeApp()">');
                for (i = 0; i < this.appList.length; i++) {
                    var app = this.appList[i];
                    var selOpt = (app.value === this.dataFnArg) ? "selected": "";
                    out.push('<option value="' + app.value + '" ' + selOpt + '>' + app.name + '</option>');
                }
                out.push('</select>');
                Ext.getCmp('appSelectorBox_' + this.getId()).setText(out.join(""));
            }
            
            this.initialLoad();
        },
        setSelectedApp: function(dataFnArg) {
            this.dataFnArg=dataFnArg;
            var selObj = document.getElementById('appSelector_' + this.getId());
            for(var i=0; i< selObj.options.length; i++) {
                if(selObj.options[i].value==dataFnArg) {
                    selObj.selectedIndex=i;
                    this.reload();
                    return;
                }
            }
        },
        getSelectedApp: function() {
            var selObj = document.getElementById('appSelector_' + this.getId());
            var result = null;
            if (selObj !== null && selObj.selectedIndex >= 0) {
                result = selObj.options[selObj.selectedIndex].value;
            }
            return result;
        },
        changeApp: function() {
            this.dataFnArg=this.getSelectedApp();
            this.reload();
        },
        initialLoad: function() {
            this.getView().setLoading(true);  
            this.getData({list:[]}); //Inital load with empty data
            this.afterDataBuild(Ext.bind(function() {
                this.getStore().loadPage(1, {
                    limit:this.isPaginated() ? this.recordsPerPage: Ung.Util.maxRowCount,
                    callback: function() {
                        this.getView().setLoading(false);
                    },
                    scope: this
                });
            }, this));
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
        },
        isDirty: function() {
            return false;
        }
    });
    
    Ext.define('Ung.SessionMonitor', {
        extend: 'Ung.StatusWin',
        helpSource: 'session_monitor',
        sortField:'bypassed',
        sortOrder: 'ASC',
        bandwidthColumns: false,
        initComponent: function() {
            this.breadcrumbs = [{
                title: this.i18n._('Session Viewer')
            }];

            this.buildGridCurrentSessions();
            this.items = [this.gridCurrentSessions];
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
                    handler(result, exception);
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
        getAppList: function() {
            var appList=[{value:0, name: this.i18n._("All Sessions")}];
            var nodeIds = rpc.nodeManager.nodeInstancesIds();
            var allNodeProperties = rpc.nodeManager.allNodeProperties();
            var allNodeSettings = rpc.nodeManager.allNodeSettings();
            for (var i = 0 ; i < nodeIds.list.length ; i++) {
                var nodeId = nodeIds.list[i];
                var nodeProperties = allNodeProperties.map[nodeId];
                var nodeSettings = allNodeSettings.map[nodeId];
                if (nodeProperties.viewPosition != null) {
                    appList.push({value: nodeSettings.id, name: i18n._('Sessions for') + ' ' + nodeProperties.displayName + " [" + main.getPolicyName(nodeSettings.policyId) + "] "});
                }
            }
            return appList;
        },
        getColumns: function() {
            var interfaceStore=Ung.Util.getInterfaceStore();
            var policyListOptions=[[null, i18n._( "Services" )], ["0", i18n._("No Rack")]];
            for( var i=0 ; i<rpc.policies.length ; i++ ) {
                var policy = rpc.policies[i];
                policyListOptions.push([policy.policyId+"", policy.name]);
            }
            var priorityList=[i18n._("Very High"), i18n._("High"), i18n._("Medium"), i18n._("Low"), i18n._("Limited"), i18n._("Limited More"), i18n._("Limited Severely")];
            var priorityOptions = [[1, i18n._("Very High")], [2, i18n._("High")], [3, i18n._("Medium")] , [4, i18n._("Low")], [5, i18n._("Limited")], [6, i18n._("Limited More")], [7, i18n._("Limited Severely")]];

            var columns= [{
                header: this.i18n._("Protocol"),
                dataIndex: "protocol",
                width: 60,
                filter: {
                    type: 'string'
                }
            },{
                header: this.i18n._("Bypassed"),
                dataIndex: "bypassed",
                width: 60,
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                header: this.i18n._("Policy"),
                dataIndex: "policy",
                width: 80,
                renderer: function(value) {
                    return (value == null || value == "" ? "" : main.getPolicyName(value) );
                },
                filter: {
                    type: 'list',
                    options: policyListOptions
                }
            },{
                header: this.i18n._("Client Interface"),
                dataIndex: "clientIntf",
                width: 85,
                renderer: function(value) {
                    var record = interfaceStore.findRecord("key", value);
                    return record==null ? ( value==null || value<0 ? "" : Ext.String.format( i18n._("Interface {0}"), value ) ) : record.get("name");
                },
                filter: {
                    type: 'numeric'
                }
            },{
                header: this.i18n._("Server Interface"),
                dataIndex: "serverIntf",
                width: 85,
                renderer: function(value) {
                    var record = interfaceStore.findRecord("key", value);
                    return record==null ? ( value==null || value<0 ? "" : Ext.String.format( i18n._("Interface {0}"), value ) ) : record.get("name");
                },
                filter: {
                    type: 'numeric'
                }
            },{
                header: this.i18n._("Client (Pre-NAT)"),
                dataIndex: "preNatClient",
                width: 75,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Server (Pre-NAT)"),
                dataIndex: "preNatServer",
                width: 75,
                filter: {
                    type: 'string'
                }
            },{
                header: this.i18n._("Client Port (Pre-NAT)"),
                dataIndex: "preNatClientPort",
                width: 70,
                filter: {
                    type: 'numeric'
                }
            },{
                hidden: true,
                header: this.i18n._("Server Port (Pre-NAT)"),
                dataIndex: "preNatServerPort",
                width: 70,
                filter: {
                    type: 'numeric'
                }
            },{
                hidden: true,
                header: this.i18n._("Client (Post-NAT)"),
                dataIndex: "postNatClient",
                width: 75,
                filter: {
                    type: 'string'
                }
            },{
                header: this.i18n._("Server (Post-NAT)"),
                dataIndex: "postNatServer",
                width: 75,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Client Port (Post-NAT)"),
                dataIndex: "postNatClientPort",
                width: 70,
                filter: {
                    type: 'numeric'
                }
            },{
                header: this.i18n._("Server Port (Post-NAT)"),
                dataIndex: "postNatServerPort",
                width: 70,
                filter: {
                    type: 'numeric'
                }
            },{
                hidden: true,
                header: this.i18n._("Local"),
                dataIndex: "localTraffic",
                width: 50,
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: this.i18n._("NATd"),
                dataIndex: "natted",
                width: 50,
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: this.i18n._("Port Forwarded"),
                dataIndex: "portForwarded",
                width: 50,
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: this.i18n._("Hostname"),
                dataIndex: "platform-hostname",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                header: this.i18n._("Username"),
                dataIndex: "platform-username",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Protocol") + this.i18n._(" (Application Control Lite)"),
                dataIndex: "protofilter-protocol",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Category") + this.i18n._(" (Application Control Lite)"),
                dataIndex: "protofilter-category",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Description") + this.i18n._(" (Application Control Lite)"),
                dataIndex: "protofilter-description",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Matched?") + this.i18n._(" (Application Control Lite)"),
                dataIndex: "protofilter-matched",
                width: 100,
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: this.i18n._("Hostname") +  this.i18n._(" (HTTP)"),
                dataIndex: "http-hostname",
                width: 120,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("URI") +  this.i18n._(" (HTTP)"),
                dataIndex: "http-uri",
                width: 120,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Category Name") +  this.i18n._(" (Web Filter)"),
                dataIndex: "esoft-best-category-name",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Category Description") +  this.i18n._(" (Web Filter)"),
                dataIndex: "esoft-best-category-description",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Category Flagged") +  this.i18n._(" (Web Filter)"),
                dataIndex: "esoft-best-category-flagged",
                width: 50,
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: this.i18n._("Category Blocked") +  this.i18n._(" (Web Filter)"),
                dataIndex: "esoft-best-category-blocked",
                width: 50,
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: this.i18n._("Content Type") +  this.i18n._(" (Web Filter)"),
                dataIndex: "esoft-content-type",
                width: 50,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Flagged") +  this.i18n._(" (Web Filter)"),
                dataIndex: "esoft-flagged",
                width: 50,
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                header: this.i18n._("Protochain") +  this.i18n._(" (Application Control)"),
                dataIndex: "classd-protochain",
                width: 140,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Application") +  this.i18n._(" (Application Control)"),
                dataIndex: "classd-application",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Category") +  this.i18n._(" (Application Control)"),
                dataIndex: "classd-category",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Detail") +  this.i18n._(" (Application Control)"),
                dataIndex: "classd-detail",
                width: 120,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Confidence") +  this.i18n._(" (Application Control)"),
                dataIndex: "classd-confidence",
                width: 50,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Productivity") +  this.i18n._(" (Application Control)"),
                dataIndex: "classd-productivity",
                width: 50,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Risk") +  this.i18n._(" (Application Control)"),
                dataIndex: "classd-risk",
                width: 50,
                filter: {
                    type: 'string'
                }
            }];
            if(this.bandwidthColumns) {
                columns.push({
                    header: this.i18n._("Client KB/s") + this.i18n._(" (Bandwidth Control)"),
                    dataIndex: "clientKBps",
                    width: 80,
                    gridColumnSummaryType: "sum",
                    filter: {
                        type: 'numeric'
                    }
                },{
                    header: this.i18n._("Server KB/s") + this.i18n._(" (Bandwidth Control)"),
                    dataIndex: "serverKBps",
                    width: 80,
                    gridColumnSummaryType: "sum",
                    filter: {
                        type: 'numeric'
                    }
                },{
                    header: this.i18n._("Total KB/s") + this.i18n._(" (Bandwidth Control)"),
                    dataIndex: "totalKBps",
                    width: 80,
                    gridColumnSummaryType: "sum",
                    filter: {
                        type: 'numeric'
                    }
                });
            }
            
            columns.push({
                hidden: !this.bandwidthColumns,
                header: this.i18n._("Priority") + this.i18n._(" (Bandwidth Control)"),
                dataIndex: "priority",
                width: 80,
                renderer: function(value) {
                    return (value < 1 || value > 7)?i18n._("None"):priorityList[value-1];
                },
                filter: {
                    type: 'list',
                    options: priorityOptions
                }
            },{
                hidden: true,
                header: this.i18n._("Priority") + " (QoS)",
                dataIndex: "qosPriority",
                width: 100,
                renderer: function(value) {
                    return (value < 1 || value > 7)?i18n._("None"):priorityList[value-1];
                },
                filter: {
                    type: 'list',
                    options: priorityOptions
                }
            });
            return columns;
        },
        // Current Sessions Grid
        buildGridCurrentSessions: function() {
            this.gridCurrentSessions = Ext.create('Ung.MonitorGrid',{
                name: "gridCurrentSessions",
                settingsCmp: this,
                height: 500,
                sortField: this.sortField,
                sortOrder: this.sortOrder,
                groupField: this.groupField,
                title: this.i18n._("Current Sessions"),
                tooltip: this.i18n._("This shows all current sessions."),
                dataFn: Ext.bind(this.getSessions, this),
                dataFnArg: 0,
                appList: this.getAppList(),
                columns: this.getColumns(),
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
