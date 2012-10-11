if (!Ung.hasResource["Ung.HostMonitor"]) {
    Ung.hasResource["Ung.HostMonitor"] = true;

    Ext.define('Ung.HostMonitor', {
        extend: 'Ung.StatusWin',
        helpSource: 'host_monitor',
        sortField:'bypassed',
        sortOrder: 'ASC',
        defaultBandwidthColumns: false,
        enableBandwidthColumns: false,
        initComponent: function() {
            this.breadcrumbs = [{
                title: this.i18n._('Host Viewer')
            }];

            this.buildHostsPanel();
            this.buildGridPenaltyBox();
            this.buildPenaltyBoxEventLog();
            this.buildHostTableEventLog();

            var pageTabs = [this.hostsPanel, this.gridHostTableEventLog, this.gridPenaltyBox, this.gridPenaltyBoxEventLog];
            this.buildTabPanel(pageTabs);
            this.callParent(arguments);
        },
        closeWindow: function() {
            this.gridCurrentHosts.stopAutoRefresh(true);
            this.hide();
        },
        getHosts: function() {
            if (!this.isVisible())
                return {javaClass:"java.util.LinkedList", list:[]};

            var hosts = rpc.hostTable.getHosts();
            // iterate through each host and change its attachments map to properties
            for (var i = 0; i < hosts.list.length ; i++) {
                var host = hosts.list[i];
                if (host.attachments != null) {
                    for (var prop in host.attachments.map) {
                        host[prop] = host.attachments.map[prop];
                    }
                }
            }
            return hosts;
        },
        getPenaltyBoxedHosts: function() {
            var hosts = rpc.hostTable.getPenaltyBoxedHosts();
            // iterate through each host and change its attachments map to properties
            for (var i = 0; i < hosts.list.length ; i++) {
                var host = hosts.list[i];
                if (host.attachments != null) {
                    for (var prop in host.attachments.map) {
                        host[prop] = host.attachments.map[prop];
                    }
                }
            }
            return hosts;
        },
        buildHostsPanel: function() {
            this.enabledColumns = {};
            this.columns = [];
            this.groupField = null;
            var groupStr = "";
            this.reRenderGrid = Ext.bind(function() {
                this.columns = [];
                this.enabledColumns = {};
                this.groupField = null;
                // add/remove columns as necessary
                for ( var i = 0 ; i < this.panelColumnSelector.items.items.length ; i++ ) {
                    var item = this.panelColumnSelector.items.items[i];
                    if ( item.xtype == "checkbox" ) {
                        this.enabledColumns[item.name] = item.checked;
                        if (item.checked) {
                            var newColumn = {
                                header: item.gridColumnHeader,
                                dataIndex: item.gridColumnDataIndex,
                                width: item.gridColumnWidth,
                                summaryType: item.gridColumnSummaryType
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
                if ( this.gridCurrentHosts == undefined ) {
                    this.buildGridCurrentHosts(this.columns, this.groupField);
                } else {
                    if ( this.groupField != null ) {
                        groupStr = " - Grouping:" + this.groupField;
                        this.gridCurrentHosts.getStore().group(this.groupField);
                    } else {
                        this.gridCurrentHosts.getStore().clearGrouping();
                    }
                    var headerCt = this.gridCurrentHosts.headerCt;
                    headerCt.suspendLayout = true;
                    headerCt.removeAll();
                    headerCt.add(this.columns);
                    this.gridCurrentHosts.groupField = this.groupField;
                    this.gridCurrentHosts.getView().refresh();
                    headerCt.suspendLayout = false;
                    this.gridCurrentHosts.forceComponentLayout();
                }
                if ( this.gridCurrentHosts !== undefined ) {
                    this.gridCurrentHosts.setTitle(i18n._("Host Table") + groupStr );
                    this.gridCurrentHosts.reload();

                }

            }, this);
            
            // manually call the renderer for the first render
            this.buildGroupSelectorPanel();
            this.buildColumnSelectorPanel();
            this.reRenderGrid();
            this.buildGridCurrentHosts(this.columns, this.groupField);

            this.hostsPanel = Ext.create('Ext.panel.Panel',{
                name: 'Host Table',
                helpSource: 'host_monitor',
                parentId: this.getId(),
                title: this.i18n._('Host Table'),
                cls: 'ung-panel',
                layout: "anchor",
                defaults: {
                    anchor: '98%',
                    autoScroll: true
                },
                autoScroll: true,
                cls: 'ung-panel',
                items: [this.gridCurrentHosts, this.panelColumnSelector, this.panelGroupSelector]
            });
        },
        buildColumnSelectorPanel: function() {
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
                    boxLabel: this.i18n._("IP"),
                    gridColumnHeader: this.i18n._("IP"),
                    gridColumnDataIndex: "addr",
                    gridColumnWidth: 100
                }, {
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Last Access Time"),
                    gridColumnHeader: this.i18n._("Last Access Time"),
                    gridColumnDataIndex: "lastAccessTime",
                    gridColumnWidth: 100,
                    gridColumnRenderer: function(value) {
                        return value == null || value == "" ? "" : i18n.timestampFormat(value);
                    }
                }, {
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Hostname"),
                    gridColumnHeader: this.i18n._("Hostname"),
                    gridColumnDataIndex: "hostname",
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Username"),
                    gridColumnHeader: this.i18n._("Username"),
                    gridColumnDataIndex: "username",
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Penalty Boxed"),
                    gridColumnHeader: this.i18n._("Penalty Boxed"),
                    gridColumnDataIndex: "penaltybox",
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Penalty Box Entry Time"),
                    gridColumnHeader: this.i18n._("Penalty Box Entry Time"),
                    gridColumnDataIndex: "penaltybox-entry-time",
                    gridColumnWidth: 100,
                    gridColumnRenderer: function(value) {
                        return value == null || value == "" ? "" : i18n.timestampFormat(value);
                    }
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Penalty Box Exit Time"),
                    gridColumnHeader: this.i18n._("Penalty Box Exit Time"),
                    gridColumnDataIndex: "penaltybox-exit-time",
                    gridColumnWidth: 100,
                    gridColumnRenderer: function(value) {
                        return value == null || value == "" ? "" : i18n.timestampFormat(value);
                    }
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: "Directory Connector" + " - " + this.i18n._("Username"),
                    gridColumnHeader: "Directory Connector" + " - " + this.i18n._("Username"),
                    gridColumnDataIndex: "adconnector-username",
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: "Captive Portal" + " - " + this.i18n._("Username"),
                    gridColumnHeader: "Captive Portal" + " - " + this.i18n._("Username"),
                    gridColumnDataIndex: "capture-username",
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: "Bandwidth Control" + " - " + this.i18n._("Penalty Box Priority"),
                    gridColumnHeader: "Bandwidth Control" + " - " + this.i18n._("Penalty Box Priority"),
                    gridColumnDataIndex: "penaltybox-priority",
                    gridColumnWidth: 100,
                    gridColumnRenderer: function(value) {
                        if (value == null || value == "")
                            return "";
                        
                        switch(value) {
                          case 0: return "";
                          case 1: return i18n._("Very High");
                          case 2: return i18n._("High");
                          case 3: return i18n._("Medium");
                          case 4: return i18n._("Low");
                          case 5: return i18n._("Limited");
                          case 6: return i18n._("Limited More");
                          case 7: return i18n._("Limited Severely");
                        default: return Ext.String.format(i18n._("Unknown Priority: {0}"), value);
                        }
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
                    boxLabel: this.i18n._("Address"),
                    groupField: "addr"
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
        // Current Hosts Grid
        buildGridCurrentHosts: function(columns, groupField) {
            this.gridCurrentHosts = Ext.create('Ung.EditorGrid',{
                name: "gridCurrentHosts",
                settingsCmp: this,
                height: 500,
                paginated: true,
                hasAdd: false,
                hasEdit: false,
                hasDelete: false,
                sortField: this.sortField,
                sortOrder: this.sortOrder,
                groupField: groupField,
                columnsDefaultSortable: true,
                title: this.i18n._("Current Hosts"),
                qtip: this.i18n._("This shows all current hosts."),
                paginated: false,
                recordJavaClass: "com.untangle.uvm.HostMonitorEntry",
                features: [{
                    ftype: 'groupingsummary'
                }],
                dataFn: Ext.bind(this.getHosts, this),
                dataFnArg: 0,
                fields: [{
                    name: "id"
                },{
                    name: "addr"
                },{
                    name: "hostname"
                },{
                    name: "lastAccessTime"
                },{
                    name: "username"
                },{
                    name: "penaltybox"
                },{
                    name: "penaltybox-entry-time"
                },{
                    name: "penaltybox-exit-time"
                },{
                    name: "adconnector-username"
                },{
                    name: "capture-username"
                },{
                    name: "penaltybox-priority"
                }],
                columns: columns,
                initComponent: function() {
                    this.bbar = ['-', {
                        xtype: 'button',
                        id: "refresh_"+this.getId(),
                        text: i18n._('Refresh'),
                        name: "Refresh",
                        tooltip: i18n._('Refresh'),
                        iconCls: 'icon-refresh',
                        handler: Ext.bind(function() {
                            this.setLoading(true);
                            this.reload();
                            this.setLoading(false);
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
                    }];
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
                autorefreshList: function() {
                    if(this!=null && this.autoRefreshEnabled && Ext.getCmp(this.id) != null) {
                        this.reload();
                        Ext.defer(this.autorefreshList, 5000, this);
                    }
                }
            });
        },
        buildGridPenaltyBox: function() {
            this.gridPenaltyBox = Ext.create('Ung.EditorGrid',{
                anchor: '100% -60',
                name: "gridPenaltyBox",
                settingsCmp: this,
                parentId: this.getId(),
                hasAdd: false,
                hasEdit: false,
                hasDelete: false,
                columnsDefaultSortable: true,
                title: this.i18n._("Penalty Box Hosts"),
                qtip: this.i18n._("This shows all hosts currently in the Penalty Box."),
                paginated: false,
                bbar: Ext.create('Ext.toolbar.Toolbar',{
                    items: [
                        '-',
                        {
                            xtype: 'button',
                            text: i18n._('Refresh'),
                            name: "Refresh",
                            tooltip: i18n._('Refresh'),
                            iconCls: 'icon-refresh',
                            handler: Ext.bind(function() {
                                this.gridPenaltyBox.reload();
                            }, this)
                        }
                    ]
                }),
                recordJavaClass: "com.untangle.uvm.HostTable.HostTableEntry",
                dataFn: Ext.bind(this.getPenaltyBoxedHosts, this),
                //testData: [{address:"aaa",priority:1, entryTime: {time:1},exitTime:{time:4654324}}, {address:"1.2.3.4",priority:2, entryTime: {time:36434},exitTime:{time:56534}}],
                fields: [{
                    name: "addr"
                },{
                    name: "penaltybox-priority"
                },{
                    name: "penaltybox-entry-time"
                },{
                    name: "penaltybox-exit-time"
                },{
                    name: "id"
                }],
                columns: [{
                    header: this.i18n._("IP Address"),
                    dataIndex: 'addr',
                    width: 150
                },{
                    header: this.i18n._("Penalty Priority"),
                    dataIndex: 'penaltybox-priority',
                    width: 200,
                    renderer: function(value) {
                        if (value == null || value == "") return "";
                        switch(value) {
                          case 0: return "";
                          case 1: return i18n._("Very High");
                          case 2: return i18n._("High");
                          case 3: return i18n._("Medium");
                          case 4: return i18n._("Low");
                          case 5: return i18n._("Limited");
                          case 6: return i18n._("Limited More");
                          case 7: return i18n._("Limited Severely");
                        default: return Ext.String.format(i18n._("Unknown Priority: {0}"), value);
                        }
                    }
                },{
                    header: this.i18n._("Entry Time"),
                    dataIndex: 'penaltybox-entry-time',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                },{
                    header: this.i18n._("Planned Exit Time"),
                    dataIndex: 'penaltybox-exit-time',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                }, Ext.create('Ext.grid.column.Action', {
                    width: 80,
                    header: this.i18n._("Control"),
                    dataIndex: null,
                    handler: Ext.bind(function(view, rowIndex, colIndex) {
                        var record = view.getStore().getAt(rowIndex);
                        Ext.MessageBox.wait(this.i18n._("Releasing host..."), this.i18n._("Please wait"));
                        rpc.hostTable.releaseHostFromPenaltyBox(Ext.bind(function(result,exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            this.gridPenaltyBox.reload();
                        }, this), record.data.addr );
                    }, this ),
                    renderer: Ext.bind(function(value, metadata, record,rowIndex,colIndex,store,view) {
                        var out= '';
                        if(record.data.internalId>=0) {
                            //adding the x-action-col-0 class to force the processing of click event
                            out= '<div class="x-action-col-0 ung-button button-column" style="text-align:center;">' + this.i18n._("Release") + '</div>';
                        }
                        return out;
                    }, this)
                })]
            });

        },
        buildPenaltyBoxEventLog: function() {
            this.gridPenaltyBoxEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp: this,
                eventQueriesFn: rpc.hostTable.getPenaltyBoxEventQueries,
                title: this.i18n._("Penalty Box Event Log"),
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'start_time',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'end_time',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'address'
                }],
                columns: [{
                    header: this.i18n._("Start Time"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'start_time',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: this.i18n._("End Time"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'end_time',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: this.i18n._("Address"),
                    flex:1,
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'address'
                }]
            });
        },
        buildHostTableEventLog: function() {
            this.gridHostTableEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp: this,
                eventQueriesFn: rpc.hostTable.getHostTableEventQueries,
                title: this.i18n._("Host Table Event Log"),
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'address'
                }, {
                    name: 'key'
                }, {
                    name: 'value'
                }],
                columns: [{
                    header: this.i18n._("Timestamp"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: this.i18n._("Address"),
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'address'
                }, {
                    header: this.i18n._("Key"),
                    width: 250,
                    sortable: true,
                    dataIndex: 'key'
                }, {
                    header: this.i18n._("Value"),
                    width: 300,
                    sortable: true,
                    dataIndex: 'value'
                }]
            });
        }
    });
}
//@ sourceURL=hostMonitor.js
