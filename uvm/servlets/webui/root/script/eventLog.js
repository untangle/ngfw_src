Ext.define("Ung.grid.EventLog", {
    extend: "Ung.grid.BaseEventLog",
    hasTimestampFilter: true,
    hasAutoRefresh: true,
    hasSelectors: true,
    settingsCmp: null,
    // default is getEventQueries() from settingsCmp
    eventQueriesFn: null,
    initComponent: function() {
        if(this.eventQueriesFn == null && this.settingsCmp.rpcNode != null && this.settingsCmp.rpcNode.getEventQueries != null) {
            this.eventQueriesFn = this.settingsCmp.rpcNode.getEventQueries;
        }
        if(this.hasTimestampFilter) {
            this.startDateWindow = Ext.create('Ung.window.SelectDateTime', {
                title: i18n._('Start date and time'),
                dateTimeEmptyText: i18n._('start date and time')
            });
            this.endDateWindow = Ext.create('Ung.window.SelectDateTime', {
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
                    Ung.Main.getReportingManagerNew().getEventsResultSet(Ext.bind(this.autoRefreshCallback, this),
                                                              selQuery, selPolicy, null, selLimit);
                } else {
                    Ung.Main.getReportingManagerNew().getEventsForDateRangeResultSet(Ext.bind(this.autoRefreshCallback, this),
                                                                          selQuery, selPolicy, null, selLimit, this.startDateWindow.date, this.endDateWindow.date);
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
            this.down('[name=querySelector]').setText(out.join(""));

            displayStyle = "";
            if (this.settingsCmp.nodeProperties != null &&
                this.settingsCmp.nodeProperties.type == "SERVICE") {
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
            this.down('[name=rackSelector]').setText(out.join(""));

            out = [];
            out.push('<select name="Event Limit" id="selectLimit_' + this.getId() + '" width="100px">');
            out.push('<option value="' + 1000 + '" selected>' + '1000 ' + i18n._('Events') + '</option>');
            out.push('<option value="' + 10000 + '">' + '10000 ' + i18n._('Events') + '</option>');
            out.push('<option value="' + 50000 + '">' + '50000 ' + i18n._('Events') + '</option>');
            out.push('</select>');
            this.down('[name=limitSelector]').setText(out.join(""));
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
                Ung.Main.getReportingManagerNew().getEventsResultSet(Ext.bind(this.refreshCallback, this),
                                                          selQuery, selPolicy, null, selLimit);
            } else {
                Ung.Main.getReportingManagerNew().getEventsForDateRangeResultSet(Ext.bind(this.refreshCallback, this),
                                                                      selQuery, selPolicy, null, selLimit, this.startDateWindow.date, this.endDateWindow.date);
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