if (!Ung.hasResource["Ung.SessionMonitor"]) {
    Ung.hasResource["Ung.SessionMonitor"] = true;
    
    Ext.define('Ung.SessionMonitor', {
        extend: 'Ung.StatusWin',
        helpSource: 'session_viewer',
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
                    var testSessionsSize=400 + Math.floor((Math.random()*150));
                    for(var t=0;t<testSessionsSize;t++) {
                        var ii=t+Math.floor((Math.random()*5));
                        sessions.push({
                            "postNatServer": "184.27.239."+(ii%10),
                            "bypassed": ((ii%3)==1),
                            "state": null,
                            "natted": true,
                            "totalKBps": null,
                            "priority": (ii%7)+1,
                            "postNatClient": "50.193.63."+((ii+1)%10),
                            "postNatClientPort": (ii+1000),
                            "preNatClient": "10.0.0."+((t+2)%10),
                            "preNatServer": "184.27.239."+((t+3)%10),
                            "attachments": {
                                "map": {
                                    "sitefilter-best-category-name": "Social Networking",
                                    "protofilter-matched": (ii%3==0),
                                    "sitefilter-best-category-description": "Social Networking",
                                    "sitefilter-best-category-blocked": false,
                                    "sitefilter-flagged": false,
                                    "platform-hostname": "acct07-wxp"+t,
                                    "sitefilter-best-category-flagged": (ii%2==1),
                                    "sitefilter-best-category-id": null,
                                    "http-uri": "/t.gif",
                                    "platform-username": "rbooroojian"+t,
                                    "http-hostname": "p.twitter.com"+t
                                },
                                "javaClass": "java.util.HashMap"
                            },
                            "protocol": (ii%2==1)?"TCP":"UDP",
                            "serverKBps": null,
                            "portForwarded": (ii%2==0),
                            "preNatClientPort": 1471,
                            "preNatServerPort": t+1500,
                            "serverIntf": ii%10,
                            "clientIntf": t%9,
                            "sessionId": 88616525732127+t,
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
            var nodeIds, allNodeProperties, allNodeSettings;
            try {
                nodeIds = rpc.nodeManager.nodeInstancesIds();
                allNodeProperties = rpc.nodeManager.allNodeProperties();
                allNodeSettings = rpc.nodeManager.allNodeSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
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
                header: this.i18n._("Hostname"),
                dataIndex: "platform-hostname",
                width: 100,
                filter: {
                    type: 'string'
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
                dataIndex: "sitefilter-best-category-name",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Category Description") +  this.i18n._(" (Web Filter)"),
                dataIndex: "sitefilter-best-category-description",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Category Flagged") +  this.i18n._(" (Web Filter)"),
                dataIndex: "sitefilter-best-category-flagged",
                width: 50,
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: this.i18n._("Category Blocked") +  this.i18n._(" (Web Filter)"),
                dataIndex: "sitefilter-best-category-blocked",
                width: 50,
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: this.i18n._("Content Type") +  this.i18n._(" (Web Filter)"),
                dataIndex: "sitefilter-content-type",
                width: 50,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: this.i18n._("Flagged") +  this.i18n._(" (Web Filter)"),
                dataIndex: "sitefilter-flagged",
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
                    name: "protofilter-description",
                    type: 'string'
                },{
                    name: "protofilter-matched",
                    type: 'string'
                },{
                    name: "http-hostname"
                },{
                    name: "http-uri"
                },{
                    name: "sitefilter-best-category-name"
                },{
                    name: "sitefilter-best-category-description"
                },{
                    name: "sitefilter-best-category-flagged",
                    type: 'string'
                },{
                    name: "sitefilter-best-category-blocked",
                    type: 'string'
                },{
                    name: "sitefilter-flagged",
                    type: 'string'
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