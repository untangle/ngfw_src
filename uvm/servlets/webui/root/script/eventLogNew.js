Ext.define("Ung.GridEventLog", {
    extend: "Ung.GridEventLogBase",
    hasTimestampFilter: true,
    hasAutoRefresh: true,
    hasSelectors: true,
    settingsCmp: null,
    // default is getEventQueries() from settingsCmp
    eventQueriesFn: null,
    initComponent: function() {
        if(this.eventQueriesFn == null && this.settingsCmp.node !== null && this.settingsCmp.node.rpcNode !== null && this.settingsCmp.node.rpcNode.getEventQueries !== null) {
            this.eventQueriesFn = this.settingsCmp.node.rpcNode.getEventQueries;
        }
        if(this.hasTimestampFilter) {
            this.startDateWindow = Ext.create('Ung.SelectDateTimeWindow', {
                title: i18n._('Start date and time'),
                dateTimeEmptyText: i18n._('start date and time')
            });
            this.endDateWindow = Ext.create('Ung.SelectDateTimeWindow', {
                title: i18n._('End date and time'),
                dateTimeEmptyText: i18n._('end date and time')
            });
            this.subCmps.push(this.startDateWindow);
            this.subCmps.push(this.endDateWindow);
        }
        this.callParent(arguments);
    },
    refreshHandler: function (forceFlush) {
        if (!this.isReportsAppInstalled()) {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._("Event Logs require the Reports application. Please install and enable the Reports application."));
        } else {
            if (!forceFlush) {
                this.refreshList();
            } else {
                this.setLoading(i18n._('Syncing events to Database... '));
                this.getUntangleNodeReporting().flushEvents(Ext.bind(function(result, exception) {
                    this.refreshList();
                }, this));
            }
        }
    },
    autoRefreshNextChunkCallback: function(result, exception) {
        if(Ung.Util.handleException(exception)) return;
        var newEventEntries = result;
        //If we got results append them to the current events list, and make another call for more
        if ( newEventEntries != null && newEventEntries.list != null && newEventEntries.list.length != 0 ) {
            this.eventEntries.push.apply( this.eventEntries, newEventEntries.list );
            this.reader.getNextChunk(Ext.bind(this.autoRefreshNextChunkCallback, this), 1000);
            return;
        }

        //If we got here, then we either reached the end of the resultSet or ran out of room. Display the results
        if (this.settingsCmp !== null) {
            this.getStore().getProxy().setData(this.eventEntries);
            this.getStore().load();
        }
        if(this!=null && this.rendered && this.autoRefreshEnabled) {
            if(this == this.settingsCmp.tabs.getActiveTab()) {
                Ext.Function.defer(this.autoRefreshList, 5000, this);
            } else {
                this.stopAutoRefresh(true);
            }
        }
    },
    autoRefreshCallback: function(result, exception) {
        if(Ung.Util.handleException(exception)) return;

        this.eventEntries = [];
        if( testMode ) {
            var emptyRec={};
            for(var j=0; j<30; j++) {
                this.eventEntries.push(this.getTestRecord(j, this.fields));
            }
            this.autoRefreshNextChunkCallback(null);
        }

        this.reader = result;
        this.reader.getNextChunk(Ext.bind(this.autoRefreshNextChunkCallback, this), 1000);
    },
    autoRefreshList: function() {
        this.getUntangleNodeReporting().flushEvents(Ext.bind(function(result, exception) {
            var selQuery = this.getSelectedQuery();
            var selPolicy = this.getSelectedPolicy();
            var selLimit = this.getSelectedLimit();
            if ( selQuery != null && selPolicy != null && selLimit != null ) {
                if (!this.hasTimestampFilter) {
                    rpc.jsonrpc.UvmContext.getEventsResultSet(Ext.bind(this.autoRefreshCallback, this),
                                                              selQuery, selPolicy, selLimit);
                } else {
                    rpc.jsonrpc.UvmContext.getEventsForDateRangeResultSet(Ext.bind(this.autoRefreshCallback, this),
                                                                          selQuery, selPolicy, selLimit, this.startDateWindow.date, this.endDateWindow.date);
                }
            }
        }, this));
    },
    exportHandler: function() {
        var selQuery = this.getSelectedQuery();
        var selQueryName = this.getSelectedQueryName();
        var selPolicy = this.getSelectedPolicy();
        var startDate = this.startDateWindow.date;
        var endDate = this.endDateWindow.date;
        if (selQuery != null && selPolicy != null) {
            Ext.MessageBox.wait(i18n._("Exporting Events..."), i18n._("Please wait"));
            var name = ( (this.name!=null) ? this.name: i18n._("Event Log") ) + " " +selQueryName;
            name=name.trim().replace(/ /g,"_");
            var downloadForm = document.getElementById('downloadForm');
            downloadForm["type"].value="eventLogExport";
            downloadForm["arg1"].value=name;
            downloadForm["arg2"].value=selQuery;
            downloadForm["arg3"].value=selPolicy;
            downloadForm["arg4"].value=this.getColumnList();
            downloadForm["arg5"].value=startDate?startDate.getTime():-1;
            downloadForm["arg6"].value=endDate?endDate.getTime():-1;
            downloadForm.submit();
            Ext.MessageBox.hide();
        }
    },
    // called when the component is rendered
    afterRender: function() {
        this.callParent(arguments);

        if (this.eventQueriesFn != null) {
            this.rpc.eventLogQueries=this.eventQueriesFn();
            var queryList = this.rpc.eventLogQueries;
            var displayStyle;
            var out =[];
            var i;
            var selOpt;
            out.push('<select name="Event Type" id="selectQuery_' + this.getId() + '">');
            for (i = 0; i < queryList.length; i++) {
                var queryDesc = queryList[i];
                selOpt = (i === 0) ? "selected": "";
                out.push('<option value="' + queryDesc.query + '" ' + selOpt + '>' + i18n._(queryDesc.name) + '</option>');
            }
            out.push('</select>');
            Ext.getCmp('querySelector_' + this.getId()).setText(out.join(""));

            displayStyle = "";
            if (this.settingsCmp.node != null &&
                this.settingsCmp.node.nodeProperties != null &&
                this.settingsCmp.node.nodeProperties.type == "SERVICE") {
                displayStyle = "display:none;"; //hide rack selector for services
            }
            out = [];
            out.push('<select name="Rack" id="selectPolicy_' + this.getId() + '" style="'+displayStyle+'">');
            out.push('<option value="-1">' + i18n._('All Racks') + '</option>');
            for (i = 0; i < rpc.policies.length; i++) {
                var policy = rpc.policies[i];
                selOpt = ( policy == rpc.currentPolicy ) ? "selected": "";
                out.push('<option value="' + policy.policyId + '" ' + selOpt + '>' + policy.name + '</option>');
            }
            out.push('</select>');
            Ext.getCmp('rackSelector_' + this.getId()).setText(out.join(""));

            out = [];
            out.push('<select name="Event Limit" id="selectLimit_' + this.getId() + '" width="100px">');
            out.push('<option value="' + 1000 + '" selected>' + '1000 ' + i18n._('Events') + '</option>');
            out.push('<option value="' + 10000 + '">' + '10000 ' + i18n._('Events') + '</option>');
            out.push('<option value="' + 50000 + '">' + '50000 ' + i18n._('Events') + '</option>');
            out.push('</select>');
            Ext.getCmp('limitSelector_' + this.getId()).setText(out.join(""));

        }
    },
    // get selected query value
    getSelectedQuery: function() {
        var selObj = document.getElementById('selectQuery_' + this.getId());
        var result = null;
        if (selObj !== null && selObj.selectedIndex >= 0) {
            result = selObj.options[selObj.selectedIndex].value;
        }
        return result;
    },
    // get selected query name
    getSelectedQueryName: function() {
        var selObj = document.getElementById('selectQuery_' + this.getId());
        var result = "";
        if (selObj !== null && selObj.selectedIndex >= 0) {
            result = selObj.options[selObj.selectedIndex].label;
        }
        return result;
    },
    // get selected policy
    getSelectedPolicy: function() {
        var selObj = document.getElementById('selectPolicy_' + this.getId());
        var result = "";
        if (selObj !== null && selObj.selectedIndex >= 0) {
            result = selObj.options[selObj.selectedIndex].value;
        }
        return result;
    },
    // get selected limit
    getSelectedLimit: function() {
        var selObj = document.getElementById('selectLimit_' + this.getId());
        var result = "";
        if (selObj !== null && selObj.selectedIndex >= 0) {
            result = selObj.options[selObj.selectedIndex].value;
        }
        return result;
    },
    refreshList: function() {
        this.setLoading(i18n._('Querying Database...'));
        var selQuery = this.getSelectedQuery();
        var selPolicy = this.getSelectedPolicy();
        var selLimit = this.getSelectedLimit();
        if ( selQuery != null && selPolicy != null && selLimit != null ) {
            if (!this.hasTimestampFilter) {
                rpc.jsonrpc.UvmContext.getEventsResultSet(Ext.bind(this.refreshCallback, this),
                                                          selQuery, selPolicy, selLimit);
            } else {
                rpc.jsonrpc.UvmContext.getEventsForDateRangeResultSet(Ext.bind(this.refreshCallback, this),
                                                                      selQuery, selPolicy, selLimit, this.startDateWindow.date, this.endDateWindow.date);
            }
        } else {
            this.setLoading(false);
        }
    },
    // get untangle node reporting
    getUntangleNodeReporting: function(forceReload) {
        if (forceReload || this.untangleNodeReporting === undefined) {
            try {
                this.untangleNodeReporting = rpc.nodeManager.node("untangle-node-reporting");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.untangleNodeReporting;
    },
    // is reports node installed
    isReportsAppInstalled: function(forceReload) {
        if (forceReload || this.reportsAppInstalledAndEnabled === undefined) {
            try {
                var reportsNode = this.getUntangleNodeReporting();
                if (this.untangleNodeReporting == null) {
                    this.reportsAppInstalledAndEnabled = false;
                }
                else {
                    if (reportsNode.getRunState() == "RUNNING"){
                        this.reportsAppInstalledAndEnabled = true;
                    } else {
                        this.reportsAppInstalledAndEnabled = false;
                    }
                }
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.reportsAppInstalledAndEnabled;
    }
});

Ung.CustomEventLog = {
    buildSessionEventLog: function(settingsCmpParam, nameParam, titleParam, helpSourceParam, visibleColumnsParam, eventQueriesFnParam) {
        var grid = Ext.create('Ung.GridEventLog',{
            name: nameParam,
            settingsCmp: settingsCmpParam,
            helpSource: helpSourceParam,
            eventQueriesFn: eventQueriesFnParam,
            title: titleParam,
            fields: [{
                name: 'time_stamp',
                sortType: Ung.SortTypes.asTimestamp
            }, {
                name: 'bandwidth_priority'
            }, {
                name: 'bandwidth_rule'
            }, {
                name: 'username'
            }, {
                name: 'hostname'
            }, {
                name: 'c_client_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'c_client_port',
                sortType: 'asInt'
            }, {
                name: 'c_server_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'c_server_port',
                sortType: 'asInt'
            }, {
                name: 's_server_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 's_server_port',
                sortType: 'asInt'
            }, {
                name: 'classd_application',
                type: 'string'
            }, {
                name: 'classd_protochain',
                type: 'string'
            }, {
                name: 'classd_flagged',
                type: 'boolean'
            }, {
                name: 'classd_blocked',
                type: 'boolean'
            }, {
                name: 'classd_confidence'
            }, {
                name: 'classd_detail'
            }, {
                name: 'protofilter_blocked'
            }, {
                name: 'protofilter_protocol',
                type: 'string'
            }, {
                name: 'classd_ruleid'
            }, {
                name: 'https_status'
            }, {
                name: 'https_detail'
            }, {
                name: 'https_ruleid'
            }, {
                name: 'policy_id'
            }, {
                name: 'firewall_blocked'
            }, {
                name: 'firewall_flagged'
            }, {
                name: 'firewall_rule_index',
                convert: Ung.CustomEventLog.mailEventConvertAction
            }, {
                name: 'ips_blocked'
            }, {
                name: 'ips_ruleid'
            }, {
                name: 'ips_description',
                type: 'string'
            }, {
                name: "capture_rule_index"
            }, {
                name: "capture_blocked"
            }],
            columns: [{
                hidden: visibleColumnsParam.indexOf('time_stamp') < 0,
                header: i18n._("Timestamp"),
                width: Ung.Util.timestampFieldWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                renderer: function(value) {
                    return i18n.timestampFormat(value);
                }
            }, {
                hidden: visibleColumnsParam.indexOf('c_client_addr') < 0,
                header: i18n._("Client"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('c_client_port') < 0,
                header: i18n._("Client port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 'c_client_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('username') < 0,
                header: i18n._("Username"),
                width: Ung.Util.usernameFieldWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                hidden: visibleColumnsParam.indexOf('hostname') < 0,
                header: i18n._("Hostname"),
                width: Ung.Util.hostnameFieldWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                hidden: visibleColumnsParam.indexOf('c_server_addr') < 0,
                header: i18n._("Server"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('c_server_port') < 0,
                header: i18n._("Server Port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 'c_server_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('classd_ruleid') < 0,
                header: i18n._("Rule ID"),
                width: 70,
                sortable: true,
                dataIndex: 'classd_ruleid',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('bandwidth_priority') < 0,
                header: i18n._("Priority"),
                width: 120,
                sortable: true,
                dataIndex: 'bandwidth_priority',
                renderer: function(value) {
                    if (Ext.isEmpty(value)) {
                        return "";
                    }
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
            }, {
                hidden: visibleColumnsParam.indexOf('bandwidth_rule') < 0,
                header: i18n._("Rule"),
                width: 120,
                sortable: true,
                dataIndex: 'bandwidth_rule',
                renderer: function(value) {
                    return Ext.isEmpty(value) ? i18n._("none") : value;
                }
            }, {
                hidden: visibleColumnsParam.indexOf('classd_application') < 0,
                header: i18n._("Application"),
                width: 120,
                sortable: true,
                dataIndex: 'classd_application'
            }, {
                hidden: visibleColumnsParam.indexOf('classd_protochain') < 0,
                header: i18n._("ProtoChain"),
                width: 180,
                sortable: true,
                dataIndex: 'classd_protochain'
            }, {
                hidden: visibleColumnsParam.indexOf('classd_blocked') < 0,
                header: i18n._("Blocked (Application Control)"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'classd_blocked',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('classd_flagged') < 0,
                header: i18n._("Flagged (Application Control)"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'classd_flagged',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('classd_confidence') < 0,
                header: i18n._("Confidence"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 'classd_confidence',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('classd_detail') < 0,
                header: i18n._("Detail"),
                width: 200,
                sortable: true,
                dataIndex: 'classd_detail'
            },{
                hidden: visibleColumnsParam.indexOf('protofilter_protocol') < 0,
                header: i18n._("Protocol"),
                width: 120,
                sortable: true,
                dataIndex: 'protofilter_protocol'
            }, {
                hidden: visibleColumnsParam.indexOf('protofilter_blocked') < 0,
                header: i18n._("Blocked (Application Control Lite)"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'protofilter_blocked',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('https_ruleid') < 0,
                header: i18n._("Rule ID (HTTPS Inspector)"),
                width: 70,
                sortable: true,
                dataIndex: 'https_ruleid',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('https_status') < 0,
                header: i18n._("Status (HTTPS Inspector)"),
                width: 100,
                sortable: true,
                dataIndex: 'https_status'
            }, {
                hidden: visibleColumnsParam.indexOf('https_detail') < 0,
                header: i18n._("Detail (HTTPS Inspector)"),
                width: 250,
                sortable: true,
                dataIndex: 'https_detail'
            }, {
                hidden: visibleColumnsParam.indexOf('policy_id') < 0,
                header: i18n._('Policy Id'),
                width: 60,
                sortable: true,
                flex:1,
                dataIndex: 'policy_id',
                renderer: main.getPolicyName
            }, {
                hidden: visibleColumnsParam.indexOf('firewall_blocked') < 0,
                header: i18n._("Blocked (Firewall)"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'firewall_blocked',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('firewall_flagged') < 0,
                header: i18n._("Flagged (Firewall)"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'firewall_flagged',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('firewall_rule_index') < 0,
                header: i18n._('Rule Id (Firewall)'),
                width: 60,
                sortable: true,
                flex:1,
                dataIndex: 'firewall_rule_index'
            }, {
                hidden: visibleColumnsParam.indexOf('s_server_addr') < 0,
                header: i18n._("Server") ,
                width: Ung.Util.ipFieldWidth + 40, // +40 for column header
                sortable: true,
                dataIndex: 's_server_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('s_server_port') < 0,
                header: i18n._("Server Port"),
                width: Ung.Util.portFieldWidth + 40, // +40 for column header
                sortable: true,
                dataIndex: 's_server_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('ips_blocked') < 0,
                header: i18n._("Blocked (Intrusion Prevention)"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'ips_blocked',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('ips_ruleid') < 0,
                header: i18n._('Rule Id (Intrusion Prevention)'),
                width: 60,
                sortable: true,
                dataIndex: 'ips_ruleid'
            }, {
                hidden: visibleColumnsParam.indexOf('ips_description') < 0,
                header: i18n._('Rule Description (Intrusion Prevention)'),
                width: 150,
                sortable: true,
                flex:1,
                dataIndex: 'ips_description'
            }, {
                hidden: visibleColumnsParam.indexOf('capture_rule_index') < 0,
                header: i18n._("Rule ID (Captive Portal)"),
                width: 80,
                dataIndex: 'capture_rule_index'
            }, {
                hidden: visibleColumnsParam.indexOf('capture_blocked') < 0,
                header: i18n._("Captured"),
                width: 100,
                sortable: true,
                dataIndex: "capture_blocked",
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }]
        });
        return grid;
    },
    buildHttpEventLog: function(settingsCmpParam, nameParam, titleParam, helpSourceParam, visibleColumnsParam, eventQueriesFnParam) {
        var grid = Ext.create('Ung.GridEventLog',{
            name: nameParam,
            settingsCmp: settingsCmpParam,
            helpSource: helpSourceParam,
            eventQueriesFn: eventQueriesFnParam,
            title: titleParam,
            fields: [{
                name: 'time_stamp',
                sortType: Ung.SortTypes.asTimestamp
            }, {
                name: 'webfilter_blocked',
                type: 'boolean'
            }, {
                name: 'sitefilter_blocked',
                type: 'boolean'
            }, {
                name: 'webfilter_flagged',
                type: 'boolean'
            }, {
                name: 'sitefilter_flagged',
                type: 'boolean'
            }, {
                name: 'webfilter_category',
                type: 'string'
            }, {
                name: 'sitefilter_category',
                type: 'string'
            }, {
                name: 'c_client_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'username'
            }, {
                name: 'hostname'
            }, {
                name: 'c_server_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 's_server_port',
                sortType: 'asInt'
            }, {
                name: 'host'
            }, {
                name: 'uri'
            }, {
                name: 'webfilter_reason',
                type: 'string',
                convert: Ung.CustomEventLog.httpEventConvertReason
            }, {
                name: 'sitefilter_reason',
                type: 'string',
                convert: Ung.CustomEventLog.httpEventConvertReason
            }, {
                name: 'adblocker_action',
                type: 'string',
                convert: function(value) {
                    return (value == 'B')?i18n._("block") : i18n._("pass");
                }
            }, {
                name: 'adblocker_cookie_ident'
            }, {
                name: 'virusblocker_name'
            }, {
                name: 'clam_name'
            }],
            columns: [{
                hidden: visibleColumnsParam.indexOf('time_stamp') < 0,
                header: i18n._("Timestamp"),
                width: Ung.Util.timestampFieldWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                renderer: function(value) {
                    return i18n.timestampFormat(value);
                }
            }, {
                hidden: visibleColumnsParam.indexOf('hostname') < 0,
                header: i18n._("Hostname"),
                width: Ung.Util.hostnameFieldWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                hidden: visibleColumnsParam.indexOf('c_client_addr') < 0,
                header: i18n._("Client"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('username') < 0,
                header: i18n._("Username"),
                width: Ung.Util.usernameFieldWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                hidden: visibleColumnsParam.indexOf('host') < 0,
                header: i18n._("Host"),
                width: Ung.Util.hostnameFieldWidth,
                sortable: true,
                dataIndex: 'host'
            }, {
                hidden: visibleColumnsParam.indexOf('uri') < 0,
                header: i18n._("Uri"),
                flex:1,
                width: Ung.Util.uriFieldWidth,
                sortable: true,
                dataIndex: 'uri'
            }, {
                hidden: visibleColumnsParam.indexOf('webfilter_blocked') < 0,
                header: i18n._("Blocked (Webfilter Lite)"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'webfilter_blocked',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('webfilter_flagged') < 0,
                header: i18n._("Flagged (Webfilter Lite)"),
                width: Ung.Util.booleanFieldWidth,
                dataIndex: 'webfilter_flagged',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('webfilter_reason') < 0,
                header: i18n._("Reason For Action (Webfilter Lite)"),
                width: 150,
                sortable: true,
                dataIndex: 'webfilter_reason'
            }, {
                hidden: visibleColumnsParam.indexOf('webfilter_category') < 0,
                header: i18n._("Category (Webfilter Lite)"),
                width: 120,
                sortable: true,
                dataIndex: 'webfilter_category'
            }, {
                hidden: visibleColumnsParam.indexOf('sitefilter_blocked') < 0,
                header: i18n._("Blocked  (Webfilter)"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'sitefilter_blocked',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('sitefilter_flagged') < 0,
                header: i18n._("Flagged (Webfilter)"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'sitefilter_flagged',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('sitefilter_reason') < 0,
                header: i18n._("Reason For Action (Webfilter)"),
                width: 150,
                sortable: true,
                dataIndex: 'sitefilter_reason'
            }, {
                hidden: visibleColumnsParam.indexOf('sitefilter_category') < 0,
                header: i18n._("Category (Webfilter)"),
                width: 120,
                sortable: true,
                dataIndex: 'sitefilter_category'
            }, {
                hidden: visibleColumnsParam.indexOf('c_server_addr') < 0,
                header: i18n._("Server"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('s_server_port') < 0,
                header: i18n._("Server Port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 's_server_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('adblocker_action') < 0,
                header: i18n._("Action (Ad Blocker)"),
                width: 120,
                sortable: true,
                dataIndex: 'adblocker_action'
            }, {
                hidden: visibleColumnsParam.indexOf('adblocker_cookie_ident') < 0,
                header: i18n._("Cookie"),
                width: 100,
                sortable: true,
                dataIndex: 'adblocker_cookie_ident'
            }, {
                hidden: visibleColumnsParam.indexOf('clam_name') < 0,
                header: i18n._("Virus Name (Virus Blocker Lite)"),
                width: 140,
                sortable: true,
                dataIndex: 'clam_name'
            }, {
                hidden: visibleColumnsParam.indexOf('virusblocker_name') < 0,
                header: i18n._("Virus Name (Virus Blocker)"),
                width: 140,
                sortable: true,
                dataIndex: 'virusblocker_name'
            }]
        });
        return grid;
    },
    buildHttpQueryEventLog: function(settingsCmpParam, nameParam, titleParam, helpSourceParam, visibleColumnsParam, eventQueriesFnParam) {
        var grid = Ext.create('Ung.GridEventLog',{
            name: nameParam,
            settingsCmp: settingsCmpParam,
            helpSource: helpSourceParam,
            eventQueriesFn: eventQueriesFnParam,
            title: titleParam,
            fields: [{
                name: 'time_stamp',
                sortType: Ung.SortTypes.asTimestamp
            }, {
                name: 'c_client_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'username'
            }, {
                name: 'hostname'
            }, {
                name: 'c_server_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 's_server_port',
                sortType: 'asInt'
            }, {
                name: 'host'
            }, {
                name: 'uri'
            }, {
                name: 'term'
            }],
            columns: [{
                hidden: visibleColumnsParam.indexOf('time_stamp') < 0,
                header: i18n._("Timestamp"),
                width: Ung.Util.timestampFieldWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                renderer: function(value) {
                    return i18n.timestampFormat(value);
                }
            }, {
                hidden: visibleColumnsParam.indexOf('hostname') < 0,
                header: i18n._("Hostname"),
                width: Ung.Util.hostnameFieldWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                hidden: visibleColumnsParam.indexOf('c_client_addr') < 0,
                header: i18n._("Client"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('username') < 0,
                header: i18n._("Username"),
                width: Ung.Util.usernameFieldWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                hidden: visibleColumnsParam.indexOf('host') < 0,
                header: i18n._("Host"),
                width: Ung.Util.hostnameFieldWidth,
                sortable: true,
                dataIndex: 'host'
            }, {
                hidden: visibleColumnsParam.indexOf('uri') < 0,
                header: i18n._("Uri"),
                flex:1,
                width: Ung.Util.uriFieldWidth,
                sortable: true,
                dataIndex: 'uri'
            }, {
                hidden: visibleColumnsParam.indexOf('term') < 0,
                header: i18n._("Query Term"),
                flex:1,
                width: Ung.Util.uriFieldWidth,
                sortable: true,
                dataIndex: 'term'
            }, {
                hidden: visibleColumnsParam.indexOf('c_server_addr') < 0,
                header: i18n._("Server"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('s_server_port') < 0,
                header: i18n._("Server Port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 's_server_port',
                filter: {
                    type: 'numeric'
                }
            }]
        });
        return grid;
    },
    buildMailEventLog: function(settingsCmpParam, nameParam, titleParam, helpSourceParam, visibleColumnsParam, eventQueriesFnParam) {
        var grid = Ext.create('Ung.GridEventLog',{
            name: nameParam,
            settingsCmp: settingsCmpParam,
            helpSource: helpSourceParam,
            eventQueriesFn: eventQueriesFnParam,
            title: titleParam,
            fields: [{
                name: 'time_stamp',
                sortType: Ung.SortTypes.asTimestamp
            }, {
                name: 'hostname'
            }, {
                name: 'c_client_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'username'
            }, {
                name: 'c_server_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 's_server_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'virusblocker_name'
            }, {
                name: 'clam_name'
            }, {
                name: 'subject',
                type: 'string'
            }, {
                name: 'addr',
                type: 'string'
            }, {
                name: 'sender',
                type: 'string'
            }, {
                name: 'vendor'
            }, {
                name:  'spamassassin_action',
                type: 'string',
                convert: Ung.CustomEventLog.mailEventConvertAction
            }, {
                name: 'spamassassin_score'
            }, {
                name: 'spamassassin_tests_string'
            }, {
                name:  'spamblocker_action',
                type: 'string',
                convert: Ung.CustomEventLog.mailEventConvertAction
            }, {
                name: 'spamblocker_score'
            }, {
                name: 'spamblocker_tests_string'
            }, {
                name:  'phish_action',
                type: 'string',
                convert: Ung.CustomEventLog.mailEventConvertAction
            }, {
                name: 'phish_score'
            }, {
                name: 'phish_tests_string'
            }],
            columns: [{
                hidden: visibleColumnsParam.indexOf('time_stamp') < 0,
                header: i18n._("Timestamp"),
                width: Ung.Util.timestampFieldWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                renderer: function(value) {
                    return i18n.timestampFormat(value);
                }
            }, {
                hidden: visibleColumnsParam.indexOf('hostname') < 0,
                header: i18n._("Hostname"),
                width: Ung.Util.hostnameFieldWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                hidden: visibleColumnsParam.indexOf('c_client_addr') < 0,
                header: i18n._("Client"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('c_server_addr') < 0,
                header: i18n._("Server"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('s_server_addr') < 0,
                header: i18n._("Server"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 's_server_addr'
            }, {
                hidden: visibleColumnsParam.indexOf('clam_name') < 0,
                header: i18n._("Virus Name (Virus Blocker Lite)"),
                width: 140,
                sortable: true,
                dataIndex: 'clam_name'
            }, {
                hidden: visibleColumnsParam.indexOf('virusblocker_name') < 0,
                header: i18n._("Virus Name (Virus Blocker)"),
                width: 140,
                sortable: true,
                dataIndex: 'virusblocker_name'
            }, {
                hidden: visibleColumnsParam.indexOf('addr') < 0,
                header: i18n._("Receiver"),
                width: Ung.Util.emailFieldWidth,
                sortable: true,
                dataIndex: 'addr'
            }, {
                hidden: visibleColumnsParam.indexOf('sender') < 0,
                header: i18n._("Sender"),
                width: Ung.Util.emailFieldWidth,
                sortable: true,
                dataIndex: 'sender'
            }, {
                hidden: visibleColumnsParam.indexOf('subject') < 0,
                header: i18n._("Subject"),
                flex:1,
                width: 150,
                sortable: true,
                dataIndex: 'subject'
            }, {
                hidden: visibleColumnsParam.indexOf('spamassassin_action') < 0,
                header: i18n._("Action (Spam Blocker Lite)"),
                width: 125,
                sortable: true,
                dataIndex: 'spamassassin_action'
            }, {
                hidden: visibleColumnsParam.indexOf('spamassassin_score') < 0,
                header: i18n._("Spam Score (Spam Blocker Lite)"),
                width: 70,
                sortable: true,
                dataIndex: 'spamassassin_score',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('spamassassin_tests_string') < 0,
                header: i18n._("Detail (Spam Blocker Lite)"),
                width: 125,
                sortable: true,
                dataIndex: 'spamassassin_tests_string'
            }, {
                hidden: visibleColumnsParam.indexOf('spamblocker_action') < 0,
                header: i18n._("Action (Spam Blocker)"),
                width: 125,
                sortable: true,
                dataIndex: 'spamblocker_action'
            }, {
                hidden: visibleColumnsParam.indexOf('spamblocker_score') < 0,
                header: i18n._("Spam Score (Spam Blocker)"),
                width: 70,
                sortable: true,
                dataIndex: 'spamblocker_score',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumnsParam.indexOf('spamblocker_tests_string') < 0,
                header: i18n._("Detail (Spam Blocker)"),
                width: 125,
                sortable: true,
                dataIndex: 'spamblocker_tests_string'
            }, {
                hidden: visibleColumnsParam.indexOf('phish_action') < 0,
                header: i18n._("Action (Phish Blocker)"),
                width: 125,
                sortable: true,
                dataIndex: 'phish_action'
            }, {
                hidden: visibleColumnsParam.indexOf('phish_tests_string') < 0,
                header: i18n._("Detail (Phish Blocker)"),
                width: 125,
                sortable: true,
                dataIndex: 'phish_tests_string'
            }]
        });
        return grid;
    },
    httpEventConvertReason: function(value) {
        if(Ext.isEmpty(value)) {
            return null;
        }
        switch (value) {
          case 'D': return i18n._("in Categories Block list");
          case 'U': return i18n._("in URLs Block list");
          case 'E': return i18n._("in File Extensions Block list");
          case 'M': return i18n._("in MIME Types Block list");
          case 'H': return i18n._("Hostname is an IP address");
          case 'I': return i18n._("in URLs Pass list");
          case 'R': return i18n._("in URLs Pass list (via referer)");
          case 'C': return i18n._("in Clients Pass list");
          case 'B': return i18n._("Client Bypass");
          case 'DEFAULT':
            return i18n._("no rule applied");
          default:
            return i18n._("no rule applied");
        }
    },
    mailEventConvertAction: function(value) {
        if(Ext.isEmpty(value)) {
            return "";
        }
        switch (value) {
            case 'P': return i18n._("pass message");
            case 'M': return i18n._("mark message");
            case 'D': return i18n._("drop message");
            case 'B': return i18n._("block message");
            case 'Q': return i18n._("quarantine message");
            case 'S': return i18n._("pass safelist message");
            case 'Z': return i18n._("pass oversize message");
            case 'O': return i18n._("pass outbound message");
            case 'F': return i18n._("block message (scan failure)");
            case 'G': return i18n._("pass message (scan failure)");
            case 'Y': return i18n._("block message (greylist)");
            default: return i18n._("unknown action");
        }
    }
};