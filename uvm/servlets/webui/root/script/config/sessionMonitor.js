if (!Ung.hasResource["Ung.SessionMonitor"]) {
    Ung.hasResource["Ung.SessionMonitor"] = true;

    Ext.define('Ung.SessionMonitor', {
        extend: 'Ung.StatusWin',
        helpSource: 'session_monitor',
        initComponent: function() {
            this.breadcrumbs = [{
                title: this.i18n._('Session Viewer')
            }];

            this.buildPanel();
            this.items = [this.sessionPanel];
            //this.buildTabPanel( [this.sessionPanel] );
            this.callParent(arguments);
        },
        closeWindow: function() {
            this.gridCurrentSessions.stopAutoRefresh(true);
            this.hide();
        },
        getSessions: function(nodeId) {
            var sessions = rpc.jsonrpc.UvmContext.sessionMonitor().getMergedSessions(nodeId);
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
        buildPanel: function() {
            this.enabledColumns = {};
            this.columns = [];
            this.groupField = null;
            this.reRenderGrid = Ext.bind(function() {
                Ext.MessageBox.wait(this.i18n._("Refreshing..."), this.i18n._("Please wait"));
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
                                width: item.gridColumnWidth
                            };
                            if ( item.gridColumnRenderer !== undefined )
                                newColumn.renderer = item.gridColumnRenderer;
                            this.columns.push( newColumn );
                        }
                    }
                }
                // set dataFnArg (for node limit) as necessary
                for ( var i = 0 ; i < this.panelNodeSelector.items.items.length ; i++ ) {
                    var item = this.panelNodeSelector.items.items[i];
                    if ( item.xtype == "radio" ) {
                        this.enabledColumns[item.name] = item.checked;
                        if (item.checked) {
                            this.dataFnArg = item.dataFnArg;
                            if ( this.gridCurrentSessions !== undefined ) 
                                this.gridCurrentSessions.dataFnArg = item.dataFnArg;
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
                if ( this.gridCurrentSessions !== undefined ) {
                    this.gridCurrentSessions.reloadGrid();
                }

                Ext.MessageBox.hide();
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
                items: [this.panelColumnSelector, this.panelNodeSelector, this.panelGroupSelector, this.gridCurrentSessions]
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
                autoHeight: true,
                bodyStyle: 'padding:5px 5px 5px 5px;',
                items: [{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Protocol"),
                    gridColumnHeader: this.i18n._("Protocol"),
                    gridColumnDataIndex: "protocol",
                    gridColumnWidth: 70
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Bypassed"),
                    gridColumnHeader: this.i18n._("Bypassed"),
                    gridColumnDataIndex: "bypassed",
                    gridColumnWidth: 70
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Policy"),
                    gridColumnHeader: this.i18n._("Policy"),
                    gridColumnDataIndex: "policy",
                    gridColumnWidth: 100
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
                    gridColumnWidth: 100,
                    gridColumnRenderer: function(value) {
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
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Server Interface"),
                    gridColumnHeader: this.i18n._("Server Interface"),
                    gridColumnDataIndex: "serverIntf",
                    gridColumnWidth: 100,
                    gridColumnRenderer: function(value) {
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
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Server (Pre-NAT)"),
                    gridColumnHeader: this.i18n._("Server (Pre-NAT)"),
                    gridColumnDataIndex: "preNatServer",
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Client Port (Pre-NAT)"),
                    gridColumnHeader: this.i18n._("Client Port (Pre-NAT)"),
                    gridColumnDataIndex: "preNatClientPort",
                    gridColumnWidth: 80
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Server Port (Pre-NAT)"),
                    gridColumnHeader: this.i18n._("Server Port (Pre-NAT)"),
                    gridColumnDataIndex: "preNatServerPort",
                    gridColumnWidth: 80
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
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Server (Post-NAT)"),
                    gridColumnHeader: this.i18n._("Server (Post-NAT)"),
                    gridColumnDataIndex: "postNatServer",
                    gridColumnWidth: 100
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Client Port (Post-NAT)"),
                    gridColumnHeader: this.i18n._("Client Port (Post-NAT)"),
                    gridColumnDataIndex: "postNatClientPort",
                    gridColumnWidth: 80
                },{
                    xtype: 'checkbox',
                    checked: true,
                    boxLabel: this.i18n._("Server Port (Post-NAT)"),
                    gridColumnHeader: this.i18n._("Server Port (Post-NAT)"),
                    gridColumnDataIndex: "postNatServerPort",
                    gridColumnWidth: 80
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
                    gridColumnWidth: 50
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("NATd"),
                    gridColumnHeader: this.i18n._("NATd"),
                    gridColumnDataIndex: "natted",
                    gridColumnWidth: 50
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Port Forwarded"),
                    gridColumnHeader: this.i18n._("Port Forwarded"),
                    gridColumnDataIndex: "portForwarded",
                    gridColumnWidth: 50
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
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Username"),
                    gridColumnHeader: this.i18n._("Username"),
                    gridColumnDataIndex: "platform-username",
                    gridColumnWidth: 150
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 5
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control Lite - Protocol"),
                    gridColumnHeader: this.i18n._("Application Control Lite - Protocol"),
                    gridColumnDataIndex: "protofilter-protocol",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control Lite - Category"),
                    gridColumnHeader: this.i18n._("Application Control Lite - Category"),
                    gridColumnDataIndex: "protofilter-category",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control Lite - Description"),
                    gridColumnHeader: this.i18n._("Application Control Lite - Description"),
                    gridColumnDataIndex: "protofilter-description",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control Lite - Matched?"),
                    gridColumnHeader: this.i18n._("Application Control Lite - Matched?"),
                    gridColumnDataIndex: "protofilter-matched",
                    gridColumnWidth: 150
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 3
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("HTTP - Hostname"),
                    gridColumnHeader: this.i18n._("HTTP - Hostname"),
                    gridColumnDataIndex: "http-hostname",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("HTTP - URI"),
                    gridColumnHeader: this.i18n._("HTTP - URI"),
                    gridColumnDataIndex: "http-uri",
                    gridColumnWidth: 150
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 5
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Category ID"),
                    gridColumnHeader: this.i18n._("Web Filter - Category ID"),
                    gridColumnDataIndex: "esoft-best-category-id",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Category Name"),
                    gridColumnHeader: this.i18n._("Web Filter - Category Name"),
                    gridColumnDataIndex: "esoft-best-category-name",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Category Description"),
                    gridColumnHeader: this.i18n._("Web Filter - Category Description"),
                    gridColumnDataIndex: "esoft-best-category-description",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Category Flagged"),
                    gridColumnHeader: this.i18n._("Web Filter - Category Flagged"),
                    gridColumnDataIndex: "esoft-best-category-flagged",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Category Blocked"),
                    gridColumnHeader: this.i18n._("Web Filter - Category Blocked"),
                    gridColumnDataIndex: "esoft-best-category-blocked",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Web Filter - Flagged"),
                    gridColumnHeader: this.i18n._("esoft-flagged"),
                    gridColumnDataIndex: "esoft-flagged",
                    gridColumnWidth: 150
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 2
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Application"),
                    gridColumnHeader: this.i18n._("Application Control - Application"),
                    gridColumnDataIndex: "classd-application",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Category"),
                    gridColumnHeader: this.i18n._("Application Control - Category"),
                    gridColumnDataIndex: "classd-category",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Protochain"),
                    gridColumnHeader: this.i18n._("Application Control - Protochain"),
                    gridColumnDataIndex: "classd-protochain",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Detail"),
                    gridColumnHeader: this.i18n._("Application Control - Detail"),
                    gridColumnDataIndex: "classd-detail",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Confidence"),
                    gridColumnHeader: this.i18n._("Application Control - Confidence"),
                    gridColumnDataIndex: "classd-confidence",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Productivity"),
                    gridColumnHeader: this.i18n._("Application Control - Productivity"),
                    gridColumnDataIndex: "classd-productivity",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Application Control - Risk"),
                    gridColumnHeader: this.i18n._("Application Control - Risk"),
                    gridColumnDataIndex: "classd-risk",
                    gridColumnWidth: 150
                    //                 },{
                    //                     border: false,
                    //                     html: '&nbsp;',
                    //                     colspan: 0
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Bandwidth Control - Client KBps"),
                    gridColumnHeader: this.i18n._("Bandwidth Control - Client KBps"),
                    gridColumnDataIndex: "clientKBps",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Bandwidth Control - Server KBps"),
                    gridColumnHeader: this.i18n._("Bandwidth Control - Server KBps"),
                    gridColumnDataIndex: "serverKBps",
                    gridColumnWidth: 150
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("Bandwidth Control - Priority"),
                    gridColumnHeader: this.i18n._("Bandwidth Control - Priority"),
                    gridColumnDataIndex: "priority",
                    gridColumnWidth: 150
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 4
                },{
                    xtype: 'checkbox',
                    checked: false,
                    boxLabel: this.i18n._("QoS - Priority"),
                    gridColumnHeader: this.i18n._("QoS - Priority"),
                    gridColumnDataIndex: "qosPriority",
                    gridColumnWidth: 150
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
            var nodes = rpc.nodeManager.nodeInstances();
            for (var i = 0 ; i < nodes.list.length ; i++) {
                var nodeProperties = nodes.list[i].nodeProperties;
                var nodeSettings = nodes.list[i].nodeSettings;
                if (nodeProperties.viewPosition != null) {
                    items.push({
                        xtype: 'radio',
                        name: 'nodeRadio',
                        checked: false,
                        boxLabel: i18n._('Sessions for') + ' ' + nodeProperties.displayName + " [" + main.getPolicyName(nodeSettings.policyId) + "] ",
                        dataFnArg: nodeSettings.id
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
                autoHeight: true,
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
                autoHeight: true,
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
                    boxLabel: this.i18n._("Web Filter - Category ID"),
                    groupField: "esoft-best-category-id"
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
                    boxLabel: this.i18n._("Web Filter - Flagged"),
                    groupField: "esoft-flagged"
                },{
                    border: false,
                    html: '&nbsp;',
                    colspan: 1
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
                    //                 },{
                    //                     border: false,
                    //                     html: '&nbsp;',
                    //                     colspan: 0
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
            this.gridCurrentSessions = Ext.create('Ung.EditorGrid',{
                name: "gridCurrentSessions",
                settingsCmp: this,
                height: 500,
                paginated: true,
                hasAdd: false,
                configAdd: null,
                hasEdit: false,
                configEdit: null,
                hasDelete: false,
                configDelete: null,
                sortField: 'bypassed',
                groupField: groupField,
                columnsDefaultSortable: true,
                title: this.i18n._("Current Sessions"),
                qtip: this.i18n._("This shows all current sessions."),
                paginated: false,
                recordJavaClass: "com.untangle.uvm.SessionMonitorEntry",
                dataFn: this.getSessions,
                dataFnArg: 0,
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
                    name: "esoft-best-category-id"
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
                    name: "clientKBps"
                },{
                    name: "serverKBps"
                },{
                    name: "priority"
                },{
                    name: "qosPriority"
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
                            Ext.MessageBox.wait(this.settingsCmp.i18n._("Refreshing..."), this.settingsCmp.i18n._("Please wait"));
                            this.reloadGrid();
                            Ext.MessageBox.hide();
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
                    	this.reloadGrid();
                        Ext.defer(this.autorefreshList, 5000, this);
                    }
                }
            });
        }
    });
}
//@ sourceURL=sessionMonitor.js
