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

            this.buildGridCurrentHosts();
            this.buildHostTableEventLog();

            this.buildGridPenaltyBox();
            this.buildPenaltyBoxEventLog();

            this.buildGridQuotaBox();
            this.buildQuotaEventLog();

            this.buildTabPanel([this.gridCurrentHosts, this.gridHostTableEventLog, this.gridPenaltyBox, this.gridPenaltyBoxEventLog, this.gridQuotaBox, this.gridQuotaEventLog]);
            this.callParent(arguments);
        },
        closeWindow: function() {
            this.gridCurrentHosts.stopAutoRefresh(true);
            this.hide();
        },
        getHosts: function(handler) {
            if (!this.isVisible()) {
                handler({javaClass:"java.util.LinkedList", list:[]});
                return;
            }
            rpc.hostTable.getHosts(Ext.bind(function(result, exception) {
                if(testMode && result != null && result.list!=null ) {
                    var testSize = 450 + Math.floor((Math.random()*100));
                    for(var i=0;i<testSize;i++) {
                        var ii=i+Math.floor((Math.random()*10));
                        result.list.push({
                            "address": "184.27.239."+(ii%10),
                            "hostname": "p.twitter.com"+i,
                            "lastAccessTime": 1355998312242+(ii*1000),
                            "username": "testuser"+i,
                            "usernameAdconnector": "uad"+ii,
                            "usernameCapture": "ucap"+(ii%50),
                            "penaltyBoxed":(ii%2)==1,
                            "penaltyBoxEntryTime": 1355998311157-(ii*5000),
                            "penaltyBoxExitTime": 1356000111157+(ii*5000),
                            "quotaSize": ii * 10000,
                            "quotaRemaining": ii * 5000,
                            "quotaIssueTime": 0,
                            "quotaExpirationTime": 0,
                            "httpUserAgent": "MOZFirefox",
                            "httpUserAgentOs": "Win"
                        });
                    }
                }
                handler(result, exception);
            }, this));
        },
        getPenaltyBoxedHosts: function() {
            return rpc.hostTable.getPenaltyBoxedHosts();
        },
        megaByteRenderer: function(bytes) {
            var units = ["bytes","Kbytes","Mbytes","Gbytes"];
            var units_itr = 0;
            
            while ((bytes >= 1000 || bytes <= -1000) && units_itr < 3) {
                bytes = bytes/1000;
                units_itr++;
            }
            
            bytes = Math.round(bytes*100)/100;
            
            return "" + bytes + " " + units[units_itr];
        },
        getQuotaHosts: function() {
            return rpc.hostTable.getQuotaHosts();
        },
        // Current Hosts Grid
        buildGridCurrentHosts: function(columns, groupField) {
            this.gridCurrentHosts = Ext.create('Ung.MonitorGrid',{
                name: "gridCurrentHosts",
                settingsCmp: this,
                height: 500,
                sortField: this.sortField,
                sortOrder: this.sortOrder,
                groupField: this.groupField,
                title: this.i18n._("Current Hosts"),
                tooltip: this.i18n._("This shows all current hosts."),
                dataFn: Ext.bind(this.getHosts, this),
                fields: [{
                    name: "id"
                },{
                    name: "address"
                },{
                    name: "hostname"
                },{
                    name: "lastAccessTime"
                },{
                    name: "username"
                },{
                    name: "usernameAdconnector"
                },{
                    name: "usernameCapture"
                },{
                    name: "penaltyBoxed"
                },{
                    name: "penaltyBoxEntryTime"
                },{
                    name: "penaltyBoxExitTime"
                },{
                    name: "quotaSize"
                },{
                    name: "quotaRemaining"
                },{
                    name: "quotaIssueTime"
                },{
                    name: "quotaExpirationTime"
                },{
                    name: "httpUserAgent"
                },{
                    name: "httpUserAgentOs"
                }],
                columns: [{
                    header: this.i18n._("IP"),
                    dataIndex: "address",
                    width: 100,
                    filter: {
                        type: 'string'
                    }
                }, {
                    hidden: true,
                    header: this.i18n._("Last Access Time"),
                    dataIndex: "lastAccessTime",
                    width: 100,
                    renderer: function(value) {
                        return value == 0 || value == "" ? "" : i18n.timestampFormat(value);
                    }/*,
                    filter: {
                        type: 'date',
                        dateFormat: 'time'
                    }*/
                }, {
                    header: this.i18n._("Hostname"),
                    dataIndex: "hostname",
                    width: 100,
                    filter: {
                        type: 'string'
                    }
                },{
                    header: this.i18n._("Username"),
                    dataIndex: "username",
                    width: 100,
                    filter: {
                        type: 'string'
                    }
                },{
                    boxLabel: this.i18n._("Penalty Boxed"),
                    header: this.i18n._("Penalty Boxed"),
                    dataIndex: "penaltyBoxed",
                    width: 100,
                    filter: {
                        type: 'boolean',
                        yesText: 'true',
                        noText: 'false'
                    }
                },{
                    hidden: true,
                    header: this.i18n._("Penalty Box Entry Time"),
                    dataIndex: "penaltyBoxEntryTime",
                    width: 100,
                    renderer: function(value) {
                        return value == 0 || value == "" ? "" : i18n.timestampFormat(value);
                    }
                },{
                    hidden: true,
                    header: this.i18n._("Penalty Box Exit Time"),
                    dataIndex: "penaltyBoxExitTime",
                    width: 100,
                    renderer: function(value) {
                        return value == 0 || value == "" ? "" : i18n.timestampFormat(value);
                    }
                },{
                    header: this.i18n._("Quota Size"),
                    dataIndex: "quotaSize",
                    width: 100,
                    renderer: function(value) {
                        return value == 0 || value == "" ? "" : value;
                    },
                    filter: {
                        type: 'numeric'
                    }
                },{
                    hidden: true,
                    header: this.i18n._("Quota Remaining"),
                    dataIndex: "quotaRemaining",
                    width: 100,
                    filter: {
                        type: 'numeric'
                    }
                },{
                    hidden: true,
                    header: this.i18n._("Quota Issue Time"),
                    dataIndex: "quotaIssueTime",
                    width: 100,
                    renderer: function(value) {
                        return value == 0 || value == "" ? "" : i18n.timestampFormat(value);
                    }
                },{
                    hidden: true,
                    header: this.i18n._("Quota Expiration Time"),
                    dataIndex: "quotaExpirationTime",
                    width: 100,
                    renderer: function(value) {
                        return value == 0 || value == "" ? "" : i18n.timestampFormat(value);
                    }
                },{
                    hidden: true,
                    header: "HTTP" + " - " + this.i18n._("User Agent"),
                    dataIndex: "httpUserAgent",
                    width: 200,
                    filter: {
                        type: 'string'
                    }
                },{
                    header: "HTTP" + " - " + this.i18n._("User Agent OS"),
                    dataIndex: "httpUserAgentOs",
                    width: 200,
                    filter: {
                        type: 'string'
                    }
                },{
                    hidden: true,
                    header: "Directory Connector" + " - " + this.i18n._("Username"),
                    dataIndex: "usernameAdconnector",
                    width: 100,
                    filter: {
                        type: 'string'
                    }
                },{
                    hidden: true,
                    header: "Captive Portal" + " - " + this.i18n._("Username"),
                    dataIndex: "usernameCapture",
                    width: 100,
                    filter: {
                        type: 'string'
                    }
                }]
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
                fields: [{
                    name: "address"
                },{
                    name: "penaltyBoxEntryTime"
                },{
                    name: "penaltyBoxExitTime"
                },{
                    name: "id"
                }],
                columns: [{
                    header: this.i18n._("IP Address"),
                    dataIndex: 'address',
                    width: 150
                },{
                    header: this.i18n._("Entry Time"),
                    dataIndex: 'penaltyBoxEntryTime',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                },{
                    header: this.i18n._("Planned Exit Time"),
                    dataIndex: 'penaltyBoxExitTime',
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
                        }, this), record.data.address );
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
        buildGridQuotaBox: function() {
            this.gridQuotaBox = Ext.create('Ung.EditorGrid',{
                anchor: '100% -60',
                name: "gridQuotaBox",
                settingsCmp: this,
                parentId: this.getId(),
                hasAdd: false,
                hasEdit: false,
                hasDelete: false,
                columnsDefaultSortable: true,
                title: this.i18n._("Current Quotas"),
                qtip: this.i18n._("This shows all hosts currently with quotas."),
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
                                this.gridQuotaBox.reload();
                            }, this)
                        }
                    ]
                }),
                recordJavaClass: "com.untangle.node.bandwidth.QuotaBoxEntry",
                dataFn: Ext.bind(this.getQuotaHosts, this),
                fields: [{
                    name: "address"
                },{
                    name: "quotaSize"
                },{
                    name: "quotaRemaining"
                },{
                    name: "quotaIssueTime"
                },{
                    name: "quotaExpirationTime"
                },{
                    name: "id"
                }],
                columns: [{
                    header: this.i18n._("IP Address"),
                    dataIndex: 'address',
                    width: 150
                },{
                    header: this.i18n._("Quota Size"),
                    dataIndex: 'quotaSize',
                    width: 100,
                    renderer: Ext.bind(this.megaByteRenderer, this)
                },{
                    header: this.i18n._("Quota Remaining"),
                    dataIndex: 'quotaRemaining',
                    width: 100,
                    renderer: Ext.bind(this.megaByteRenderer, this)
                },{
                    header: this.i18n._("Allocated"),
                    dataIndex: 'quotaIssueTime',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                },{
                    header: this.i18n._("Expires"),
                    dataIndex: 'quotaExpirationTime',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                }, Ext.create('Ext.grid.column.Action',{
                    width: 80,
                    header: this.i18n._("Refill Quota"),
                    dataIndex: null,
                    handler: Ext.bind(function(view, rowIndex, colIndex) {
                        var record = view.getStore().getAt(rowIndex);
                        Ext.MessageBox.wait(this.i18n._("Refilling..."), this.i18n._("Please wait"));
                        rpc.hostTable.refillQuota(Ext.bind(function(result,exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            this.gridQuotaBox.reload();
                        }, this), record.data.address );
                    }, this ),
                    renderer: Ext.bind(function(value, metadata, record) {
                        var out= '';
                        if(record.data.internalId>=0) {
                            //adding the x-action-col-0 class to force the processing of click event
                            out= '<div class="x-action-col-0 ung-button button-column" style="text-align:center;" >' + this.i18n._("Refill") + '</div>';
                        }
                        return out;
                    }, this)
                }), Ext.create('Ext.grid.column.Action',{
                    width: 80,
                    header: this.i18n._("Drop Quota"),
                    dataIndex: null,
                    handler: Ext.bind(function(view, rowIndex, colIndex) {
                        var record = view.getStore().getAt(rowIndex);
                        Ext.MessageBox.wait(this.i18n._("Removing Quota..."), this.i18n._("Please wait"));
                        rpc.hostTable.removeQuota(Ext.bind(function(result,exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            this.gridQuotaBox.reload();
                        }, this), record.data.address );
                    }, this ),
                    renderer: Ext.bind(function(value, metadata, record) {
                        var out= '';
                        if(record.data.internalId>=0) {
                            //adding the x-action-col-0 class to force the processing of click event
                            out= '<div class="x-action-col-0 ung-button button-column" style="text-align:center;" >' + this.i18n._("Drop") + '</div>';
                        }
                        return out;
                    }, this)
                })]
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
                }, {
                    name: 'reason'
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
                    width: Ung.Util.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'address'
                },{
                    header: this.i18n._("Reason"),
                    width: 100,
                    flex: 1,
                    dataIndex: 'reason'
                }]
            });
        },
        buildQuotaEventLog: function() {
            this.gridQuotaEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp: this,
                eventQueriesFn: rpc.hostTable.getQuotaEventQueries,
                title: this.i18n._("Quota Event Log"),
                fields: [{
                    name: 'time_stamp',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'action'
                }, {
                    name: 'address'
                }, {
                    name: 'size'
                }, {
                    name: 'reason'
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
                    header: this.i18n._("Action"),
                    width: 150,
                    sortable: true,
                    dataIndex: 'action',
                    renderer: Ext.bind(function(value, metadata, record) {
                        switch (value) {
                            case 0: return "";
                            case 1: return "Quota Given";
                            case 2: return "Quota Exceeded";
                            default: return "Unknown";
                        }
                    }, this)
                },{
                    header: this.i18n._("Quota Size"),
                    width: 150,
                    dataIndex: 'size',
                    renderer: Ext.bind(this.megaByteRenderer, this)
                },{
                    header: this.i18n._("Reason"),
                    width: 100,
                    flex: 1,
                    dataIndex: 'reason'
                }]
            });
        }
    });
}
//@ sourceURL=hostMonitor.js
