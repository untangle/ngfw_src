if (!Ung.hasResource["Ung.SessionMonitor"]) {
    Ung.hasResource["Ung.SessionMonitor"] = true;
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
        getSessions: function(nodeId) {
            if (!this.isVisible())
                return {javaClass:"java.util.LinkedList", list:[]};
            var sessions = rpc.sessionMonitor.getMergedSessions(nodeId);
            if(testMode) {
                var testSessionsSize=500;
                for(var i=0;i<testSessionsSize;i++) {
                    sessions.list.push({
                        "postNatServer": "184.27.239."+(i%10),
                        "bypassed": ((i%3)==1),
                        "state": null,
                        "natted": true,
                        "totalKBps": null,
                        "localTraffic": false,
                        "priority": 4,
                        "postNatClient": "50.193.63."+((i+1)%10),
                        "postNatClientPort": (i+1000),
                        "preNatClient": "10.0.0."+((i+2)%10),
                        "preNatServer": "184.27.239."+((i+3)%10),
                        "attachments": {
                            "map": {
                                "esoft-best-category-name": "Social Networking",
                                "protofilter-matched": false,
                                "esoft-best-category-description": "Social Networking",
                                "esoft-best-category-blocked": false,
                                "esoft-flagged": false,
                                "platform-hostname": "acct07-wxp",
                                "esoft-best-category-flagged": false,
                                "esoft-best-category-id": null,
                                "http-uri": "/t.gif",
                                "platform-username": "rbooroojian",
                                "http-hostname": "p.twitter.com"
                            },
                            "javaClass": "java.util.HashMap"
                        },
                        "protocol": "TCP",
                        "serverKBps": null,
                        "portForwarded": false,
                        "preNatClientPort": 1471,
                        "preNatServerPort": i+1500,
                        "serverIntf": 5,
                        "clientIntf": 2,
                        "sessionId": 88616525732127+i,
                        "javaClass": "com.untangle.uvm.SessionMonitorEntry",
                        "qosPriority": 4,
                        "clientKBps": null,
                        "policy": (i%5==2)?null:(i%5)+ "",
                        "postNatServerPort": (i+2000)
                    });                 
                }
            }
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
            var policyListOptions=[[null, i18n._( "Services" )], ["0", i18n._("No Rack")]];
            for( var i=0 ; i<rpc.policies.length ; i++ ) {
                var policy = rpc.policies[i];
                policyListOptions.push([policy.policyId+"", policy.name]);
            }
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
                    gridColumnWidth: 85,
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
                        if (value < 1 || value > 7)
                            return i18n._("None");
                        else
                            return [i18n._("Very High"), i18n._("High"), i18n._("Medium"), i18n._("Low"), i18n._("Limited"), i18n._("Limited More"), i18n._("Limited Severely")][value-1];
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
                        if (value < 1 || value > 7)
                            return i18n._("None");
                        else
                            return [i18n._("Very High"), i18n._("High"), i18n._("Medium"), i18n._("Low"), i18n._("Limited"), i18n._("Limited More"), i18n._("Limited Severely")][value-1];
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
            this.gridCurrentSessions = Ext.create('Ung.EditorGrid',{
                name: "gridCurrentSessions",
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
                title: this.i18n._("Current Sessions"),
                qtip: this.i18n._("This shows all current sessions."),
                paginated: false,
                recordJavaClass: "com.untangle.uvm.SessionMonitorEntry",
                
                features: [{
                    ftype: 'filters',
                    encode: false,
                    local: true
                }, {
                    ftype: 'groupingsummary'
                }],
                dataFn: Ext.bind(this.getSessions, this),
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
//@ sourceURL=sessionMonitor.js
