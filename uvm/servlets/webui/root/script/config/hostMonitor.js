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

            this.buildPanel();
            this.items = [this.hostsPanel];
            this.callParent(arguments);
        },
        closeWindow: function() {
            this.gridCurrentHosts.stopAutoRefresh(true);
            this.hide();
        },
        getHosts: function() {
            if (!this.isVisible())
                return {javaClass:"java.util.LinkedList", list:[]};

            var hosts = rpc.jsonrpc.UvmContext.hostTable().getHosts();
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
        buildPanel: function() {
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
                    this.gridCurrentHosts.setTitle(i18n._("Current Hosts") + groupStr );
                    this.gridCurrentHosts.reload();

                }

            }, this);
            
            // manually call the renderer for the first render
            this.buildGroupSelectorPanel();
            this.buildColumnSelectorPanel();
            this.reRenderGrid();
            this.buildGridCurrentHosts(this.columns, this.groupField);
            this.buildHostsPanel();
        },
        buildHostsPanel: function() {
            this.hostsPanel = Ext.create('Ext.panel.Panel',{
                name: 'Host Viewer',
                helpSource: 'host_monitor',
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
                    checked: true,
                    boxLabel: this.i18n._("Hostname"),
                    gridColumnHeader: this.i18n._("Hostname"),
                    gridColumnDataIndex: "platform-hostname",
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Username"),
                    gridColumnHeader: this.i18n._("Username"),
                    gridColumnDataIndex: "platform-username",
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Directory Connector Username"),
                    gridColumnHeader: this.i18n._("Directory Connector Username"),
                    gridColumnDataIndex: "adconnector-username",
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Captive Portal Username"),
                    gridColumnHeader: this.i18n._("Captive Portal Username"),
                    gridColumnDataIndex: "capture-username",
                    gridColumnWidth: 100
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
                    name: "platform-hostname"
                },{
                    name: "platform-username"
                },{
                    name: "adconnector-username"
                },{
                    name: "capture-username"
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
        }
    });
}
//@ sourceURL=hostMonitor.js
