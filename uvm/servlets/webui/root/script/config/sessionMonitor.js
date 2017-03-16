Ext.define('Webui.config.sessionMonitor', {
    extend: 'Ung.StatusWin',
    name: 'sessions',
    helpSource: 'sessions',
    sortField:'bypassed',
    sortOrder: 'ASC',
    bandwidthColumns: false,
    displayName: 'Sessions',
    initComponent: function() {
        if(!this.breadcrumbs) {
            this.breadcrumbs = [{
                title: i18n._('Sessions')
            }];
        }
        this.buildGridCurrentSessions();
        this.buildTabPanel([this.gridCurrentSessions]);
        this.callParent(arguments);
    },
    closeWindow: function() {
        this.gridCurrentSessions.stopAutoRefresh(true);
        this.hide();
    },
    getSessions: function(handler, appId) {
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
                sessions = [];
                for(var tt = 0; tt<100; tt++) {
                    sessions = sessions.concat(Ext.decode(Ext.encode(result.list)));
                }
            }
            if(false) {
                var testSessionsSize=5000;//400 + Math.floor((Math.random()*150));
                for(var t=0;t<testSessionsSize;t++) {
                    var ii=t+Math.floor((Math.random()*5));
                    sessions.push({
                        "postNatServer": "184.27.239."+(ii%10),
                        "bypassed": ((ii%3)==1),
                        "state": null,
                        "natted": true,
                        "totalKBps": null,
                        "priority": (ii%7)+1,
                        "pipeline": "str",
                        "postNatClient": "50.193.63."+((ii+1)%10),
                        "postNatClientPort": (ii+1000),
                        "localAddr": "10.0.0."+((t+2)%10),
                        "remoteAddr": "184.27.239."+((t+3)%10),
                        "hostname": "hostname"+t,
                        "preNatClient": "10.0.0."+((t+2)%10),
                        "preNatServer": "184.27.239."+((t+3)%10),
                        "clientCountry" : null,
                        "clientLatitude" : 0,
                        "clientLongitude" : 0,
                        "serverCountry" : "AB",
                        "serverLatitude" : 0,
                        "serverLongitude" : 0,
                        "attachments": {
                            "map": {
                                "web-filter-best-category-name": "Social Networking",
                                "application-control-lite-matched": (ii%3==0),
                                "web-filter-best-category-description": "Social Networking",
                                "web-filter-best-category-blocked": false,
                                "web-filter-flagged": false,
                                "web-filter-best-category-flagged": (ii%2==1),
                                "web-filter-best-category-id": null,
                                "http-uri": "/t.gif",
                                "platform-username": "foobar"+t,
                                "http-hostname": "p.twitter.com"+(t%500)
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
                        "creationTime": 1426011960,
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
            var i, c, prop;
            for (i = 0; i < sessions.length ; i++) {
                var session = sessions[i];
                if (session.attachments) {
                    for (prop in session.attachments.map) {
                        session[prop] = session.attachments.map[prop];
                    }
                }
            }
            handler({javaClass:"java.util.LinkedList", list:sessions});
        }, this), appId);
    },
    getAppList: function() {
        var appList=[{value:0, name: i18n._("All Sessions")}];
        var appIds, allAppProperties, allAppSettings;
        try {
            appIds = rpc.appManager.appInstancesIds();
            allAppProperties = rpc.appManager.allAppProperties();
            allAppSettings = rpc.appManager.allAppSettings();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        for (var i = 0 ; i < appIds.list.length ; i++) {
            var appId = appIds.list[i];
            var appProperties = allAppProperties.map[appId];
            var appSettings = allAppSettings.map[appId];
            if (appProperties.viewPosition != null) {
                appList.push({value: appSettings.id, name: i18n._('Sessions for') + ' ' + appProperties.displayName + " [" + Ung.Main.getPolicyName(appSettings.policyId) + "] "});
            }
        }
        return appList;
    },
    getColumns: function() {
        var policyListOptions=[[null, i18n._( "Service App" )], ["0", i18n._("None")]];
        for( var i=0 ; i<rpc.policies.length ; i++ ) {
            var policy = rpc.policies[i];
            policyListOptions.push([policy.policyId+"", policy.name]);
        }
        var policyListOptionsStore = Ext.create('Ext.data.ArrayStore', {
            fields: [ 'id', 'text' ],
            data: policyListOptions
        });
        var priorityOptionsStore = Ext.create('Ext.data.ArrayStore', {
            fields: [ 'id', 'text' ],
            data: [
                [1, i18n._("Very High")],
                [2, i18n._("High")],
                [3, i18n._("Medium")] ,
                [4, i18n._("Low")],
                [5, i18n._("Limited")],
                [6, i18n._("Limited More")],
                [7, i18n._("Limited Severely")]
            ]
        });
        var priorityList=[i18n._("Very High"), i18n._("High"), i18n._("Medium"), i18n._("Low"), i18n._("Limited"), i18n._("Limited More"), i18n._("Limited Severely")];

        var columns= [{
            hidden: true,
            header: i18n._("Creation Time"),
            dataIndex: "creationTime",
            width: Ung.TableConfig.timestampFieldWidth,
            renderer: function(value) {
                return i18n.timestampFormat(value);
            }
        }, {
            header: i18n._("Protocol"),
            dataIndex: "protocol",
            width: 60,
            filter: {
                type: 'string'
            }
        },{
            header: i18n._("Bypassed"),
            dataIndex: "bypassed",
            width: Ung.TableConfig.booleanFieldWidth,
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            }
        },{
            header: i18n._("Policy"),
            dataIndex: "policy",
            width: 80,
            renderer: function(value) {
                return (value == null || value == "" || value == " " ? " " : Ung.Main.getPolicyName(value) );
            },
            filter: {
                type: 'list',
                store: policyListOptionsStore
            }
        },{
            header: i18n._("Client Interface"),
            dataIndex: "clientIntf",
            width: 85,
            filter: {
                type: 'string'
            }
        },{
            header: i18n._("Server Interface"),
            dataIndex: "serverIntf",
            width: 85,
            filter: {
                type: 'string'
            }
        },{
            header: i18n._("Hostname"),
            dataIndex: "hostname",
            width: 100,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Local Address"),
            dataIndex: "localAddr",
            width: Ung.TableConfig.ipFieldWidth,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Remote Address"),
            dataIndex: "remoteAddr",
            width: Ung.TableConfig.ipFieldWidth,
            filter: {
                type: 'string'
            }
        },{
            header: i18n._("Client (Pre-NAT)"),
            dataIndex: "preNatClient",
            width: Ung.TableConfig.ipFieldWidth,
            filter: {
                type: 'string'
            }
        },{
            header: i18n._("Client Port (Pre-NAT)"),
            dataIndex: "preNatClientPort",
            width: Ung.TableConfig.portFieldWidth,
            filter: {
                type: 'numeric'
            }
        },{
            hidden: true,
            header: i18n._("Client (Post-NAT)"),
            dataIndex: "postNatClient",
            width: Ung.TableConfig.ipFieldWidth,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Client Port (Post-NAT)"),
            dataIndex: "postNatClientPort",
            width: Ung.TableConfig.portFieldWidth,
            filter: {
                type: 'numeric'
            }
        },{
            hidden: true,
            header: i18n._("Client Country"),
            dataIndex: "clientCountry",
            width: 80,
            renderer: function(value) {
                return Ung.Main.getCountryName(value);
            },
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Client Latitude"),
            dataIndex: "clientLatitude",
            width: 100,
            filter: {
                type: 'numeric'
            }
        },{
            hidden: true,
            header: i18n._("Client Longitude"),
            dataIndex: "clientLongitude",
            width: 100,
            filter: {
                type: 'numeric'
            }
        },{
            hidden: true,
            header: i18n._("Server (Pre-NAT)"),
            dataIndex: "preNatServer",
            width: Ung.TableConfig.ipFieldWidth,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Server Port (Pre-NAT)"),
            dataIndex: "preNatServerPort",
            width: Ung.TableConfig.portFieldWidth,
            filter: {
                type: 'numeric'
            }
        },{
            header: i18n._("Server (Post-NAT)"),
            dataIndex: "postNatServer",
            width: Ung.TableConfig.ipFieldWidth,
            filter: {
                type: 'string'
            }
        },{
            header: i18n._("Server Port (Post-NAT)"),
            dataIndex: "postNatServerPort",
            width: Ung.TableConfig.portFieldWidth,
            filter: {
                type: 'numeric'
            }
        },{
            header: i18n._("Server Country"),
            dataIndex: "serverCountry",
            width: 80,
            renderer: function(value) {
                return Ung.Main.getCountryName(value);
            },
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Server Latitude"),
            dataIndex: "serverLatitude",
            width: 100,
            filter: {
                type: 'numeric'
            }
        },{
            hidden: true,
            header: i18n._("Server Longitude"),
            dataIndex: "serverLongitude",
            width: 100,
            filter: {
                type: 'numeric'
            }
        },{
            hidden: true,
            header: i18n._("NATd"),
            dataIndex: "natted",
            width: Ung.TableConfig.booleanFieldWidth,
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            }
        },{
            hidden: true,
            header: i18n._("Port Forwarded"),
            dataIndex: "portForwarded",
            width: Ung.TableConfig.booleanFieldWidth,
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            }
        },{
            header: i18n._("Tags"),
            dataIndex: "tagsString",
            width: 100,
            filter: {
                type: 'string'
            }
        },{
            header: i18n._("Username"),
            dataIndex: "platform-username",
            width: 100,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Protocol") + " " + "(Application Control Lite)",
            dataIndex: "application-control-lite-protocol",
            width: 100,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Category") + " " + "(Application Control Lite)",
            dataIndex: "application-control-lite-category",
            width: 100,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Description") + " " + "(Application Control Lite)",
            dataIndex: "application-control-lite-description",
            width: 100,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Matched?") + " " + "(Application Control Lite)",
            dataIndex: "application-control-lite-matched",
            width: 100,
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            }
        },{
            hidden: true,
            header: i18n._("Hostname") + " " + "(HTTP)",
            dataIndex: "http-hostname",
            width: 120,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("URI") + " " + "(HTTP)",
            dataIndex: "http-uri",
            width: 120,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Category Name") + " " + "(Web Filter)",
            dataIndex: "web_filter-best-category-name",
            width: 100,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Category Description") + " " + "(Web Filter)",
            dataIndex: "web_filter-best-category-description",
            width: 100,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Category Flagged") + " " + "(Web Filter)",
            dataIndex: "web_filter-best-category-flagged",
            width: 50,
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            }
        },{
            hidden: true,
            header: i18n._("Category Blocked") + " " + "(Web Filter)",
            dataIndex: "web_filter-best-category-blocked",
            width: 50,
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            }
        },{
            hidden: true,
            header: i18n._("Content Type") + " " + "(Web Filter)",
            dataIndex: "web_filter-content-type",
            width: 50,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Flagged") + " " + "(Web Filter)",
            dataIndex: "web_filter-flagged",
            width: 50,
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            }
        },{
            header: i18n._("Protochain") + " " + "(Application Control)",
            dataIndex: "application-control-protochain",
            width: 140,
            filter: {
                type: 'string'
            }
        },{
            header: i18n._("Application") + " " + "(Application Control)",
            dataIndex: "application-control-application",
            width: 100,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Category") + " " + "(Application Control)",
            dataIndex: "application-control-category",
            width: 100,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Detail") + " " + "(Application Control)",
            dataIndex: "application-control-detail",
            width: 120,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Confidence") + " " + "(Application Control)",
            dataIndex: "application-control-confidence",
            width: 50,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Productivity") + " " + "(Application Control)",
            dataIndex: "application-control-productivity",
            width: 50,
            filter: {
                type: 'string'
            }
        },{
            hidden: true,
            header: i18n._("Risk") + " " + "(Application Control)",
            dataIndex: "application-control-risk",
            width: 50,
            filter: {
                type: 'string'
            }
        }];

        columns.push({
            header: i18n._("Client KB/s"),
            dataIndex: "clientKBps",
            width: 80,
            gridColumnSummaryType: "sum",
            filter: {
                type: 'numeric'
            }
        },{
            header: i18n._("Server KB/s"),
            dataIndex: "serverKBps",
            width: 80,
            gridColumnSummaryType: "sum",
            filter: {
                type: 'numeric'
            }
        },{
            header: i18n._("Total KB/s"),
            dataIndex: "totalKBps",
            width: 80,
            gridColumnSummaryType: "sum",
            filter: {
                type: 'numeric'
            }
        });

        columns.push({
            hidden: !this.bandwidthColumns,
            header: i18n._("Priority") + " " + "(Bandwidth Control)",
            dataIndex: "priority",
            width: 80,
            renderer: function(value) {
                return (value < 1 || value > 7)?i18n._("None"):priorityList[value-1];
            },
            filter: {
                type: 'list',
                store: priorityOptionsStore
            }
        },{
            hidden: true,
            header: i18n._("Priority") + " (QoS)",
            dataIndex: "qosPriority",
            width: 100,
            renderer: function(value) {
                return (value < 1 || value > 7)?i18n._("None"):priorityList[value-1];
            },
            filter: {
                type: 'list',
                store: priorityOptionsStore
            }
        },{
            hidden: true,
            header: i18n._("Pipeline"),
            dataIndex: "pipeline",
            width: 400,
            filter: {
                type: 'string'
            }
        });

        return columns;
    },
    // Current Sessions Grid
    buildGridCurrentSessions: function() {
        var intfList = Ung.Util.getInterfaceList(false, false);
        var interfaceMap = {};
        for(var i=0; i<intfList.length; i++) {
            interfaceMap[intfList[i][0]]=intfList[i][1];
        }
        this.fieldConvertInterface = function( value, record){
            if (value == null || value < 0) {
                return '';
            }
            if (!interfaceMap[value]) {
                return Ext.String.format(i18n._('Interface [{0}]'), value);
            }
            return interfaceMap[value] + ' [' + value + ']';

        };
        this.gridCurrentSessions = Ext.create('Ung.MonitorGrid',{
            name: this.name+"Grid",
            settingsCmp: this,
            height: 500,
            title: i18n._("Current Sessions"),
            tooltip: i18n._("This shows all current sessions."),
            dataFn: Ext.bind(this.getSessions, this),
            dataFnArg: 0,
            appList: this.getAppList(),
            sortField: this.sortField,
            sortOrder: this.sortOrder,
            groupField: this.groupField,
            columns: this.getColumns(),
            fields: [{
                name: "creationTime",
                sortType: 'asTimestamp',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "id",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "protocol",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "bypassed",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "policy",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "localAddr",
                sortType: 'asIp',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "remoteAddr",
                sortType: 'asIp',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "hostname",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "preNatClient",
                sortType: 'asIp',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "preNatServer",
                sortType: 'asIp',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "preNatClientPort",
                sortType: 'asInt',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "preNatServerPort",
                sortType: 'asInt',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "postNatClient",
                sortType: 'asIp',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "postNatServer",
                sortType: 'asIp',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "postNatClientPort",
                sortType: 'asInt',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "postNatServerPort",
                sortType: 'asInt',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "clientIntf",
                convert: this.fieldConvertInterface
            },{
                name: "serverIntf",
                convert: this.fieldConvertInterface
            },{
                name: "natted",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "portForwarded",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "tagsString",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "platform-username",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-lite-protocol",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-lite-category",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-lite-description",
                type: 'string',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-lite-matched",
                type: 'string',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "http-hostname",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "http-uri",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "web_filter-best-category-name",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "web_filter-best-category-description",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "web_filter-best-category-flagged",
                type: 'string',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "web_filter-best-category-blocked",
                type: 'string',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "web_filter-flagged",
                type: 'string',
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-application",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-category",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-protochain",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-detail",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-confidence",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-productivity",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "application-control-risk",
                convert: Ung.Util.preventEmptyValueConverter
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
                        return Math.round((rec.data.serverKBps+rec.data.clientKBps)*10)/10;
                }
            },{
                name: "priority",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "qosPriority",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "pipeline",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "clientCountry",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "clientLatitude",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "clientLongitude",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "serverCountry",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "serverLatitude",
                convert: Ung.Util.preventEmptyValueConverter
            },{
                name: "serverLongitude",
                convert: Ung.Util.preventEmptyValueConverter
            }]
        });
    },
     // Current Sessions Grid
    buildChartSessions: function() {
        this.chartSessions = Ext.create('Ext.panel.Panel', {
            title: i18n._("Chart"),
            html: '<p>World!</p>'
        });
    }
});
//# sourceURL=sessionMonitor.js
