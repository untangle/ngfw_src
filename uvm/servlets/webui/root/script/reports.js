Ext.define('Ung.panel.Reports', {
    extend: 'Ext.panel.Panel',
    statics: {
        getColumnHumanReadableName: function(columnName) {
            if(!this.columnsHumanReadableNames) {
                this.columnsHumanReadableNames = {
                    action: i18n._('Action'),
                    ad_blocker_action: 'Ad Blocker ' + i18n._('Action'),
                    ad_blocker_cookie_ident: 'Ad Blocker ' + i18n._('Cookie'),
                    addr: i18n._('Address'),
                    addr_kind: i18n._('Address Kind'),
                    addr_name: i18n._('Address Name'),
                    address: i18n._('Address'),
                    application_control_application: 'Application Control ' + i18n._('Application'),
                    application_control_blocked: 'Application Control ' + i18n._('Blocked'),
                    application_control_confidence: 'Application Control ' + i18n._('Confidence'),
                    application_control_detail: 'Application Control ' + i18n._('Detail'),
                    application_control_flagged: 'Application Control ' + i18n._('Flagged'),
                    application_control_lite_blocked: 'Application Control Lite ' + i18n._('Blocked'),
                    application_control_lite_protocol: 'Application Control Lite ' + i18n._('Protocol'),
                    application_control_protochain: 'Application Control ' + i18n._('Protochain'),
                    application_control_ruleid: 'Application Control ' + i18n._('Rule ID'),
                    auth_type: i18n._('Authorization Type'),
                    bandwidth_control_priority: 'Bandwidth Control ' + i18n._('Priority'),
                    bandwidth_control_rule: 'Bandwidth Control ' + i18n._('Rule ID'),
                    blocked: i18n._('Blocked'),
                    bypasses: i18n._('Bypasses'),
                    c2p_bytes: i18n._('From-Client Bytes'),
                    c2s_content_length: i18n._('Client-to-server Content Length'),
                    c_client_addr: i18n._('Client-side Client Address'),
                    c_client_port: i18n._('Client-side Client Port'),
                    c_server_addr: i18n._('Client-side Server Address'),
                    c_server_port: i18n._('Client-side Server Port'),
                    captive_portal_blocked: 'Captive Portal ' + i18n._('Blocked'),
                    captive_portal_rule_index: 'Captive Portal ' + i18n._('Rule ID'),
                    category: i18n._('Category'),
                    class_id: i18n._('Classtype ID'),
                    classtype: i18n._('Classtype'),
                    client_addr: i18n._('Client Address'),
                    client_address: i18n._('Client Address'),
                    client_intf: i18n._('Client Interface'),
                    client_name: i18n._('Client Name'),
                    client_protocol: i18n._('Client Protocol'),
                    client_username: i18n._('Client Username'),
                    connect_stamp: i18n._('Connect Time'),
                    cpu_system: i18n._('CPU System Utilization'),
                    cpu_user: i18n._('CPU User Utilization'),
                    description: i18n._('Text detail of the event'),
                    dest_addr: i18n._('Destination Address'),
                    dest_port: i18n._('Destination Port'),
                    disk_free: i18n._('Disk Free'),
                    disk_total: i18n._('Disk Size'),
                    domain: i18n._('Domain'),
                    elapsed_time: i18n._('Elapsed Time'),
                    end_time: i18n._('End Time'),
                    event_id: i18n._('Event ID'),
                    event_info: i18n._('Event Type'),
                    firewall_blocked: 'Firewall ' + i18n._('Blocked'),
                    firewall_flagged: 'Firewall ' + i18n._('Flagged'),
                    firewall_rule_index: 'Firewall ' + i18n._('Rule ID'),
                    gen_id: i18n._('Grouping ID'),
                    goodbye_stamp: i18n._('End Time'),
                    hit_bytes: i18n._('Hit Bytes'),
                    hits: i18n._('Hits'),
                    host: i18n._('Host'),
                    hostname: i18n._('Hostname'),
                    interface_id: i18n._('Interface ID'),
                    ipaddr: i18n._('Client Address'),
                    json: i18n._('JSON Text'),
                    key: i18n._('Key'),
                    load_1: i18n._('CPU load (1-min)'),
                    load_15: i18n._('CPU load (15-min)'),
                    load_5: i18n._('CPU load (5-min)'),
                    local: i18n._('Local'),
                    login: i18n._('Login'),
                    login_name: i18n._('Login Name'),
                    mem_buffers: i18n._('Memory Buffers'),
                    mem_cache: i18n._('Memory Cache'),
                    mem_free: i18n._('Memory Free'),
                    method: i18n._('Method'),
                    miss_bytes: i18n._('Miss Bytes'),
                    misses: i18n._('Misses'),
                    msg: i18n._('Message'),
                    msg_id: i18n._('Message ID'),
                    name: i18n._('Interface Name'),
                    net_interface: i18n._('Net Interface'),
                    net_process: i18n._('Net Process'),
                    os_name: i18n._('Interface O/S Name'),
                    p2c_bytes: i18n._('To-Client Bytes'),
                    p2s_bytes: i18n._('To-Server Bytes'),
                    phish_blocker_action: 'Phish Blocker ' + i18n._('Action'),
                    phish_blocker_is_spam: 'Phish Blocker ' + i18n._('Phish'),
                    phish_blocker_score: 'Phish Blocker ' + i18n._('Score'),
                    phish_blocker_tests_string: 'Phish Blocker ' + i18n._('Tests'),
                    policy_id: i18n._('Policy ID'),
                    pool_address: i18n._('Pool Address'),
                    protocol: i18n._('Protocol'),
                    reason: i18n._('Reason'),
                    receiver: i18n._('Receiver'),
                    remote_address: i18n._('Remote Address'),
                    remote_port: i18n._('Remote Port'),
                    request_id: i18n._('Request ID'),
                    rx_bytes: i18n._('Bytes Received'),
                    s2c_content_length: i18n._('Server-to-client Content Length'),
                    s2c_content_type: i18n._('Server-to-client Content Type'),
                    s2p_bytes: i18n._('From-Server Bytes'),
                    s_client_addr: i18n._('Server-side Client Address'),
                    s_client_port: i18n._('Server-side Client Port'),
                    s_server_addr: i18n._('Server-side Server Address'),
                    s_server_port: i18n._('Server-side Server Port'),
                    sender: i18n._('Sender'),
                    server_intf: i18n._('Server Interface'),
                    session_id: i18n._('Session ID'),
                    shield_blocked: 'Shield ' + i18n._('Blocked'),
                    sig_id: i18n._('Signature ID'),
                    size: i18n._('Size'),
                    source_addr: i18n._('Source Address'),
                    source_port: i18n._('Source Port'),
                    spam_blocker_action: 'Spam Blocker ' + i18n._('Action'),
                    spam_blocker_is_spam: 'Spam Blocker ' + i18n._('Spam'),
                    spam_blocker_lite_action: 'Spam Blocker Lite ' + i18n._('Action'),
                    spam_blocker_lite_is_spam: 'Spam Blocker Lite ' + i18n._('Spam'),
                    spam_blocker_lite_score: 'Spam Blocker Lite ' + i18n._('Score'),
                    spam_blocker_lite_tests_string: 'Spam Blocker Lite ' + i18n._('Tests'),
                    spam_blocker_score: 'Spam Blocker ' + i18n._('Score'),
                    spam_blocker_tests_string: 'Spam Blocker ' + i18n._('Tests'),
                    ssl_inspector_detail: 'HTTPS Inspector ' + i18n._('Detail'),
                    ssl_inspector_ruleid: 'HTTPS Inspector ' + i18n._('Rule ID'),
                    ssl_inspector_status: 'HTTPS Inspector ' + i18n._('Status'),
                    start_time: i18n._('Start Time'),
                    subject: i18n._('Subject'),
                    succeeded: i18n._('Succeeded'),
                    success: i18n._('Success'),
                    summary_text: i18n._('Summary Text'),
                    swap_free: i18n._('Swap Free'),
                    swap_total: i18n._('Swap Size'),
                    systems: i18n._('System bypasses'),
                    term: i18n._('Search Term'),
                    time_stamp: i18n._('Timestamp'),
                    tx_bytes: i18n._('Bytes Sent'),
                    type: i18n._('Type'),
                    uri: i18n._('URI'),
                    username: i18n._('Username'),
                    value: i18n._('Value'),
                    vendor_name: i18n._('Vendor Name'),
                    virus_blocker_clean: 'Virus Blocker ' + i18n._('Clean'),
                    virus_blocker_lite_clean: 'Virus Blocker Lite ' + i18n._('Clean'),
                    virus_blocker_lite_name: 'Virus Blocker Lite ' + i18n._('Name'),
                    virus_blocker_name: 'Virus Blocker ' + i18n._('Name'),
                    web_filter_blocked: 'Web Filter ' + i18n._('Blocked'),
                    web_filter_category: 'Web Filter ' + i18n._('Category'),
                    web_filter_flagged: 'Web Filter ' + i18n._('Flagged'),
                    web_filter_lite_blocked: 'Web Filter Lite ' + i18n._('Blocked'),
                    web_filter_lite_category: 'Web Filter Lite ' + i18n._('Category'),
                    web_filter_lite_flagged: 'Web Filter Lite ' + i18n._('Flagged'),
                    web_filter_lite_reason: 'Web Filter Lite ' + i18n._('Reason'),
                    web_filter_reason: 'Web Filter ' + i18n._('Reason')
                };
            }
            var readableName = this.columnsHumanReadableNames[columnName];
            return readableName!=null ? readableName : columnName;
        },
        getColumnsForTable: function(table, store) {
            if(table != null && table.length > 2) {
                Ung.Main.getReportingManagerNew().getColumnsForTable(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var columns = [];
                    for (var i=0; i< result.length; i++) {
                        columns.push({
                            name: result[i],
                            displayName: Ung.panel.Reports.getColumnHumanReadableName(result[i])
                        });
                    }
                    store.loadData(columns);
                }, table);
            }
        }
    },
    name: 'panelReports',
    autoRefreshInterval: 20, //In Seconds
    layout: { type: 'border'},
    extraConditions: null,
    reportEntry: null,
    beforeDestroy: function() {
        Ext.destroy(this.subCmps);
        this.callParent(arguments);
    },
    initComponent: function() {
        this.subCmps = [];
        if(this.category) {
            this.helpSource = this.category.toLowerCase().replace(" ","_") + "_reports";
            if(!this.title) {
                this.title = i18n._('Reports');
            }
            if (!Ung.Main.isReportsAppInstalled()) {
                this.items = [{
                    region: 'center',
                    xtype: 'panel',
                    bodyPadding: 10,
                    html: i18n._("Reports application is required for this feature. Please install and enable the Reports application.")
                }];
                this.callParent(arguments);
                return;
            }
        }
        this.startDateWindow = Ext.create('Ung.window.SelectDateTime', {
            title: i18n._('Start date and time'),
            dateTimeEmptyText: i18n._('start date and time')
        });
        this.endDateWindow = Ext.create('Ung.window.SelectDateTime', {
            title: i18n._('End date and time'),
            dateTimeEmptyText: i18n._('end date and time')
        });
        this.pieLegendHint="<br/>"+i18n._('Hint: Click this label on the legend to hide this slice');
        this.cartesianLegendHint="<br/>"+i18n._('Hint: Click this label on the legend to hide this series');
        this.subCmps.push(this.startDateWindow);
        this.subCmps.push(this.endDateWindow);
        
        this.items = [{
            region: 'east',
            title: i18n._("Current Data"),
            width: 330,
            split: true,
            collapsible: true,
            collapsed: Ung.Main.viewport.getWidth()<1600,
            floatable: false,
            name: 'dataGrid',
            xtype: 'grid',
            store:  Ext.create('Ext.data.Store', {
                fields: [],
                data: []
            }),
            columns: [{
                flex: 1
            }],
            tbar: ['->', {
                xtype: 'button',
                text: i18n._('Export'),
                name: "Export",
                tooltip: i18n._('Export Data to File'),
                iconCls: 'icon-export',
                handler: Ext.bind(this.exportHandler, this)
            }]
        }, {
            region: 'center',
            layout: {type: 'border'},
            items: [{
                region: 'center',
                xtype: "panel",
                name:'chartContainer',
                layout: 'fit',
                html: "",
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'bottom',
                    items: [{
                        xtype: 'button',
                        text: i18n._('One day ago'),
                        initialLabel:  i18n._('One day ago'),
                        width: 132,
                        tooltip: i18n._('Select Start date and time'),
                        handler: Ext.bind(function(button) {
                            this.startDateWindow.buttonObj=button;
                            this.startDateWindow.show();
                        }, this)
                    },{
                        xtype: 'tbtext',
                        text: '-'
                    }, {
                        xtype: 'button',
                        text: i18n._('Present'),
                        initialLabel:  i18n._('Present'),
                        width: 132,
                        tooltip: i18n._('Select End date and time'),
                        handler: Ext.bind(function(button) {
                            this.endDateWindow.buttonObj=button;
                            this.endDateWindow.show();
                        }, this)
                    }, {
                        xtype: 'button',
                        text: i18n._('Refresh'),
                        name: "refresh",
                        tooltip: i18n._('Flush Events from Memory to Database and then Refresh'),
                        iconCls: 'icon-refresh',
                        handler:function () {
                            this.refreshHandler();
                        },
                        scope: this
                    }, {
                        xtype: 'button',
                        name: 'auto_refresh',
                        text: i18n._('Auto Refresh'),
                        enableToggle: true,
                        pressed: false,
                        tooltip: Ext.String.format(i18n._('Auto Refresh every {0} seconds'),this.autoRefreshInterval),
                        iconCls: 'icon-autorefresh',
                        handler: Ext.bind(function(button) {
                            if(button.pressed) {
                                this.startAutoRefresh();
                            } else {
                                this.stopAutoRefresh();
                            }
                        }, this)
                    }, '->', {
                        xtype: 'button',
                        text: i18n._('Customize'),
                        name: "edit",
                        tooltip: i18n._('Advanced report customization'),
                        iconCls: 'icon-edit',
                        handler:function () {
                            this.customizeReport();
                        },
                        scope: this
                    }]
                }] 
            }, this.extraConditionsPanel = Ext.create("Ung.panel.ExtraConditions", {
                region: 'south',
                parentPanel: this,
                listeners: {
                    "expand": {
                        fn: Ext.bind(function() {
                            if(this.reportEntry) {
                                Ung.panel.Reports.getColumnsForTable(this.reportEntry.table, this.extraConditionsPanel.columnsStore);
                            }
                        }, this)
                    }
                }
            })]
        }];
        
        if(this.category) {
            this.entriesStore = Ext.create('Ext.data.Store', {
                fields: ["title"],
                data: []
            });
            this.loadReportEntries();
            
            this.items.push({
                region: 'west',
                title: i18n._("Select Report"),
                width: 250,
                split: true,
                collapsible: true,
                collapsed: false,
                floatable: false,
                name: 'entriesGrid',
                xtype: 'grid',
                hideHeaders: true,
                store:  this.entriesStore,
                columns: [{
                    dataIndex: 'title',
                    flex: 1,
                    renderer: function( value, metaData, record, rowIdx, colIdx, store ) {
                        var description = record.get("description");
                        if(description) {
                            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( description ) + '"';
                        }
                        return value;
                    }
                }],
                listeners: {
                    rowclick: Ext.bind(function( grid, record, tr, rowIndex, e, eOpts ) {
                        this.loadReport(Ext.clone(record.getData()));
                    }, this)
                }
            
            });
        }
        
        this.callParent(arguments);
        this.chartContainer = this.down("panel[name=chartContainer]");
        this.dataGrid = this.down("grid[name=dataGrid]");
    },
    loadReportEntries: function(initialEntryId) {
        Ung.Main.getReportingManagerNew().getReportEntries(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            this.reportEntries = [];
            this.initialEntryIndex = null;
            var reportEntry;
            for(var i=0; i<result.list.length; i++) {
                reportEntry = result.list[i];
                if(reportEntry.enabled) {
                    this.reportEntries.push(reportEntry);
                    if(initialEntryId && reportEntry.uniqueId == initialEntryId) {
                        this.initialEntryIndex = i;
                    }
                    if(this.initialEntryIndex==null && reportEntry.type!="TEXT") {
                        this.initialEntryIndex = i;
                    }
                }
            }
            if(this.initialEntryIndex == null && this.reportEntries.length>0) {
                this.initialEntryIndex = 0;
            }
            this.entriesStore.loadData(this.reportEntries);
            if(initialEntryId && this.initialEntryIndex) {
                this.selectInitialReport();
            }
        }, this), this.category);
    },
    loadReportData: function(data) {
        var i, column;
        if(!this.reportEntry || !this.chartContainer || !this.chartContainer.isVisible()) {
            return;
        }
        var chart = this.chartContainer.down("[name=chart]");
        if(!chart) {
            return;
        }
        if(this.reportEntry.type == 'TEXT') {
            var infos=[], reportData=[];
            if(data.length>0 && this.reportEntry.textColumns!=null) {
                var textColumns=[], value;
                for(i=0; i<this.reportEntry.textColumns.length; i++) {
                    column = this.reportEntry.textColumns[i].split(" ").splice(-1)[0];
                    value = Ext.isEmpty(data[0][column])? 0 : data[0][column];
                    infos.push(value);
                    reportData.push({data: column, value: value});
                }
            }
            
            var sprite = chart.getSurface().get("infos");
            sprite.setAttributes({text:Ext.String.format.apply(Ext.String.format, [i18n._(this.reportEntry.textString)].concat(infos))}, true);
            chart.renderFrame();
            this.dataGrid.getStore().loadData(reportData);
        } else if(this.reportEntry.type == 'PIE_GRAPH') {
            var topData = data;
            if(this.reportEntry.pieNumSlices && data.length>this.reportEntry.pieNumSlices) {
                topData = [];
                var others = {value:0};
                others[this.reportEntry.pieGroupColumn] = i18n._("Others");
                for(i=0; i<data.length; i++) {
                    if(i < this.reportEntry.pieNumSlices) {
                        topData.push(data[i]);
                    } else {
                        others.value+=data[i].value;
                    }
                }
                topData.push(others);
            }
            if(topData.length == 0) {
                this.noDataSprite.show();
            } else {
                this.noDataSprite.hide();
            }
            chart.renderFrame();
            chart.getStore().loadData(topData);
            this.dataGrid.getStore().loadData(data);
        } else if(this.reportEntry.type == 'TIME_GRAPH') {
            chart.getStore().loadData(data);
            this.dataGrid.getStore().loadData(data);
        }
    },
    loadReport: function(reportEntry) {
        var me = this;
        this.reportEntry = reportEntry;
        if(this.autoRefreshEnabled) {
            this.stopAutoRefresh(true);
        }
        this.chartContainer.removeAll();
        this.setLoading(i18n._('Loading report... '));
        
        var i, column;
        
        var data = [];

        var chart, reportData=[];
        this.dataGrid.getStore().loadData([]);
        if(reportEntry.type == 'TEXT') {
            chart = {
                xtype: 'draw',
                name: "chart",
                border: false,
                width: '100%',
                height: '100%',
                tbar: ['->', {
                    xtype: 'button',
                    iconCls: 'icon-export',
                    text: i18n._("Download"),
                    handler: Ext.bind(this.downloadChart, this)
                }],
                sprites: [{
                    type: 'text',
                    text: reportEntry.title,
                    fontSize: 18,
                    width: 100,
                    height: 30,
                    x: 10, // the sprite x position
                    y: 22  // the sprite y position
                }, {
                    type: 'text',
                    text: reportEntry.description,
                    fontSize: 12,
                    x: 10,
                    y: 40
                }, {
                    type: 'text',
                    id: 'infos',
                    text: "",
                    fontSize: 12,
                    x: 10,
                    y: 80
                }]
            };
            this.dataGrid.setColumns([{
                dataIndex: 'data',
                header: i18n._("data"),
                width: 100,
                flex: 1
            },{
                dataIndex: 'value',
                header: i18n._("value"),
                width: 100
            }]);
        } else if(reportEntry.type == 'PIE_GRAPH') {
            var descriptionFn = function(val, record) {
                var title = (record.get(reportEntry.pieGroupColumn)==null)?i18n._("none") : record.get(reportEntry.pieGroupColumn);
                var value = (reportEntry.units == "bytes") ? Ung.Util.bytesRenderer(record.get("value")) : record.get("value") + " " + i18n._(reportEntry.units);
                return title + ": " + value;
            };

            chart = {
                xtype: 'polar',
                name: "chart",
                store: Ext.create('Ext.data.JsonStore', {
                    fields: [{name: "description", convert: descriptionFn }, {name:'value'} ],
                    data: []
                }),
                theme: 'category2',
                border: false,
                width: '100%',
                height: '100%',
                insetPadding: {top: 40, left: 40, right: 10, bottom: 10},
                innerPadding: 20,
                legend: {
                    docked: 'right'
                },
                tbar: ['->', {
                    xtype: 'button',
                    iconCls: 'icon-export',
                    text: i18n._("Download"),
                    handler: Ext.bind(this.downloadChart, this)
                }],
                sprites: [{
                    type: 'text',
                    text: reportEntry.title,
                    fontSize: 18,
                    width: 100,
                    height: 30,
                    x: 10, // the sprite x position
                    y: 22  // the sprite y position
                }, {
                    type: 'text',
                    text: reportEntry.description,
                    fontSize: 12,
                    x: 10,
                    y: 40
                }, this.noDataSprite = Ext.create("Ext.draw.sprite.Text", {
                    type: 'text',
                    hidden: true,
                    text: i18n._("Not enough data to generate the chart."),
                    fontSize: 14,
                    fillStyle: '#FF0000',
                    x: 10,
                    y: 80
                })],
                interactions: ['rotate', 'itemhighlight'],
                series: [{
                    type: 'pie',
                    angleField: 'value',
                    rotation: 45,
                    label: {
                        field: "description",
                        calloutLine: {
                            length: 10,
                            width: 3
                        }
                    },
                    highlight: true,
                    tooltip: {
                        trackMouse: true,
                        style: 'background: #fff',
                        showDelay: 0,
                        dismissDelay: 0,
                        hideDelay: 0,
                        renderer: function(storeItem, item) {
                            this.setHtml(storeItem.get("description")+me.pieLegendHint);
                        }
                    }
                }]
            };

            if ( reportEntry.colors != null && reportEntry.colors.length > 0 ) {
                chart.colors = reportEntry.colors;
            }
            this.dataGrid.setColumns([{
                dataIndex: reportEntry.pieGroupColumn,
                header: reportEntry.pieGroupColumn,
                width: 100,
                flex: 1
            },{
                dataIndex: 'value',
                header: i18n._("value"),
                width: 100
            },{
                xtype: 'actioncolumn',
                menuDisabled: true,
                width: 20,
                items: [{
                    iconCls: 'icon-filter-row',
                    tooltip: i18n._('Add Condition'),
                    handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                        this.buildWindowAddCondition();
                        var data = {
                            column: reportEntry.pieGroupColumn,
                            operator: "=",
                            value: record.get(reportEntry.pieGroupColumn)
                        };
                        this.windowAddCondition.setCondition(data);
                    }, this)
                }]
            }]);
        } else if(reportEntry.type == 'TIME_GRAPH') {
            var axesFields = [], series=[];
            var legendHint = (reportEntry.timeDataColumns.length > 1) ? this.cartesianLegendHint : "";
            var zeroFn = function(val) {
                return (val==null)?0:val;
            };
            var timeFn = function(val) {
                return (val==null || val.time==null)?0:i18n.timestampFormat(val);
            };
            var storeFields =[{name: 'time_trunc', convert: timeFn}];
            var reportDataColumns = [{
                dataIndex: 'time_trunc',
                header: 'time_trunc',
                width: 130,
                flex: reportEntry.timeDataColumns.length>2? 0:1
            }];
            for(i=0; i<reportEntry.timeDataColumns.length; i++) {
                column = reportEntry.timeDataColumns[i].split(" ").splice(-1)[0];
                axesFields.push(column);
                storeFields.push({name: column, convert: zeroFn});
                reportDataColumns.push({
                    dataIndex: column,
                    header: column,
                    width: reportEntry.timeDataColumns.length>2 ? 60 : 90
                });
            }
            if(!this.reportEntry.timeStyle) {
                this.reportEntry.timeStyle = "LINE";
            }
            if(this.reportEntry.timeStyle.indexOf('OVERLAPPED') != -1  && this.reportEntry.timeDataColumns.length <= 1){
                this.reportEntry.timeStyle = this.reportEntry.timeStyle.replace("_OVERLAPPED", "");
            }
            var tbar = [], timeStyle, timeStyles = [
                { name: 'LINE', iconCls: 'icon-line-chart', text: i18n._("Line"), tooltip: i18n._("Switch to Line Chart") },
                { name: 'BAR_3D', iconCls: 'icon-bar3d-chart', text: i18n._("Bar 3D"), tooltip: i18n._("Switch to Bar 3D Chart") },
                { name: 'BAR_3D_OVERLAPPED', iconCls: 'icon-bar3d-overlapped-chart', text: i18n._("Bar 3D Overlapped"), tooltip: i18n._("Switch to Bar 3D Overlapped Chart") },
                { name: 'BAR', iconCls: 'icon-bar-chart', text: i18n._("Bar"), tooltip: i18n._("Switch to Bar Chart") },
                { name: 'BAR_OVERLAPPED', iconCls: 'icon-bar-overlapped-chart', text: i18n._("Bar Overlapped"), tooltip: i18n._("Switch to Bar Overlapped Chart") }
            ];
            
            for(i=0; i<timeStyles.length; i++) {
                timeStyle = timeStyles[i];
                tbar.push({
                    xtype: 'button',
                    pressed: this.reportEntry.timeStyle == timeStyle.name,
                    hidden: (timeStyle.name.indexOf('OVERLAPPED') != -1 ) && (reportEntry.timeDataColumns.length <= 1),
                    name: timeStyle.name,
                    iconCls: timeStyle.iconCls,
                    text: timeStyle.text,
                    tooltip: timeStyle.tooltip,
                    handler: Ext.bind(function(button) {
                        this.reportEntry.timeStyle = button.name;
                        this.loadReport(this.reportEntry);
                    }, this)
                });
            }
            chart = {
                xtype: 'cartesian',
                name: "chart",
                store: Ext.create('Ext.data.JsonStore', {
                    fields: storeFields,
                    data: []
                }),
                theme: 'category2',
                border: false,
                animation: false,
                width: '100%',
                height: '100%',
                insetPadding: {top: 50, left: 10, right: 10, bottom: 10},
                legend: {
                    docked: 'bottom'
                },
                tbar: tbar.concat(['->', {
                    xtype: 'button',
                    iconCls: 'icon-export',
                    text: i18n._("Download"),
                    handler: Ext.bind(this.downloadChart, this)
                }]),
                sprites: [{
                    type: 'text',
                    text: reportEntry.title,
                    fontSize: 18,
                    width: 100,
                    height: 30,
                    x: 10, // the sprite x position
                    y: 22  // the sprite y position
                }, {
                    type: 'text',
                    text: reportEntry.description,
                    fontSize: 12,
                    x: 10,
                    y: 40
                }],
                interactions: ['itemhighlight'],
                axes: [{
                    type: (reportEntry.timeStyle.indexOf('BAR_3D')!=-1) ? 'numeric3d' : 'numeric',
                    fields: axesFields,
                    position: 'left',
                    grid: true,
                    minimum: 0,
                    renderer: function (v) {
                        return (reportEntry.units == "bytes") ? Ung.Util.bytesRenderer(v) : v + " " + i18n._(reportEntry.units);
                    }
                }, {
                    type: (reportEntry.timeStyle.indexOf('BAR_3D')!=-1) ? 'category3d' : 'category',
                    fields: 'time_trunc',
                    position: 'bottom',
                    grid: true,
                    label: {
                        rotate: {
                            degrees: -90
                        }
                    }
                }]
            };

            if ( reportEntry.colors != null && reportEntry.colors.length > 0 ) {
                chart.colors = reportEntry.colors;
            }
            
            if (reportEntry.timeStyle == 'LINE') {
                for(i=0; i<axesFields.length; i++) {
                    series.push({
                        type: 'line',
                        axis: 'left',
                        title: axesFields[i],
                        xField: 'time_trunc',
                        yField: axesFields[i],
                        style: {
                            opacity: 0.90,
                            lineWidth: 3
                        },
                        marker: {
                            radius: 2
                        },
                        highlight: {
                            fillStyle: '#000',
                            radius: 4,
                            lineWidth: 1,
                            strokeStyle: '#fff'
                        },
                        tooltip: {
                            trackMouse: true,
                            style: 'background: #fff',
                            renderer: function(storeItem, item) {
                                var title = item.series.getTitle();
                                this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " +i18n._(reportEntry.units) + legendHint);
                            }
                        }
                    });
                }
                chart.series = series;
            } else if(reportEntry.timeStyle.indexOf('OVERLAPPED') != -1) {
                for(i=0; i<axesFields.length; i++) {
                    series.push({
                        type: (reportEntry.timeStyle.indexOf('BAR_3D')!=-1)?'bar3d': 'bar',
                        axis: 'left',
                        title: axesFields[i],
                        xField: 'time_trunc',
                        yField: axesFields[i],
                        style: (reportEntry.timeStyle.indexOf('BAR_3D') != -1)? { opacity: 0.70, lineWidth: 1+5*i } : {  opacity: 0.60,  maxBarWidth: Math.max(40-2*i, 2) },
                        tooltip: {
                            trackMouse: true,
                            style: 'background: #fff',
                            renderer: function(storeItem, item) {
                                var title = item.series.getTitle();
                                this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " +i18n._(reportEntry.units) + legendHint);
                            }
                        }
                    });
                }
                chart.series = series;
            } else if((reportEntry.timeStyle.indexOf('BAR') != -1 )) {
                chart.series = [{
                    type: (reportEntry.timeStyle.indexOf('BAR_3D')!=-1)?'bar3d': 'bar',
                    axis: 'left',
                    title: axesFields,
                    xField: 'time_trunc',
                    yField: axesFields,
                    stacked: false,
                    style: {
                        opacity: 0.90,
                        inGroupGapWidth: 1
                    },
                    highlight: true,
                    tooltip: {
                        trackMouse: true,
                        style: 'background: #fff',
                        renderer: function(storeItem, item) {
                            var title = item.series.getTitle()[Ext.Array.indexOf(item.series.getYField(), item.field)];
                            this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.field) + " " +i18n._(reportEntry.units) + legendHint);
                        }
                    }
                }];
            }
            this.dataGrid.setColumns(reportDataColumns);
        }
        this.chartContainer.add(chart); 
        Ung.Main.getReportingManagerNew().getDataForReportEntry(Ext.bind(function(result, exception) {
            this.setLoading(false);
            if(Ung.Util.handleException(exception)) return;
            this.loadReportData(result.list);
        }, this), this.reportEntry, this.startDateWindow.date, this.endDateWindow.date, this.extraConditions, -1);
        if(!this.extraConditionsPanel.getCollapsed()) {
            Ung.panel.Reports.getColumnsForTable(this.reportEntry.table, this.extraConditionsPanel.columnsStore);
        }
    },
    refreshHandler: function () {
        if(this.autoRefreshEnabled) {
            return;
        }
        this.refreshReportData();
    },
    autoRefresh: function() {
        if(!this.autoRefreshEnabled) {
            return;
        }
        this.refreshReportData();
    },
    refreshReportData: function() {
        if(!this.reportEntry) {
            return;
        }
        if(!this.autoRefreshEnabled) { this.setLoading(i18n._('Refreshing report... ')); }
        Ung.Main.getNodeReporting().flushEvents(Ext.bind(function(result, exception) {
            Ung.Main.getReportingManagerNew().getDataForReportEntry(Ext.bind(function(result, exception) {
                this.setLoading(false);
                if(Ung.Util.handleException(exception)) return;
                this.loadReportData(result.list);
                if(this!=null && this.rendered && this.autoRefreshEnabled) {
                    Ext.Function.defer(this.autoRefresh, this.autoRefreshInterval*1000, this);
                }
            }, this), this.reportEntry, this.startDateWindow.date, this.endDateWindow.date, this.extraConditions, -1);
            
        }, this));
    },
    autoRefreshEnabled: false,
    startAutoRefresh: function(setButton) {
        if(!this.reportEntry) {
            this.down('button[name=auto_refresh]').toggle(false);
            return;
        }

        this.autoRefreshEnabled=true;
        this.down('button[name=refresh]').disable();
        this.autoRefresh();
    },
    stopAutoRefresh: function(setButton) {
        this.autoRefreshEnabled=false;
        if(setButton) {
            this.down('button[name=auto_refresh]').toggle(false);
        }
        this.down('button[name=refresh]').enable();
    },
    exportHandler: function() {
        if(!this.reportEntry) {
            return;
        }
        var processRow = function (row) {
            var data = [];
            for (var j = 0; j < row.length; j++) {
                var innerValue = row[j] == null ? '' : row[j].toString();
                data.push('"' + innerValue.replace(/"/g, '""') + '"');
            }
            return data.join(",") + '\r\n';
        };

        var records = this.dataGrid.getStore().getRange(), list=[], columns=[], headers=[], i, j, row;
        var gridColumns = this.dataGrid.getColumns();
        for(i=0; i<gridColumns.length;i++) {
            if(gridColumns[i].initialConfig.dataIndex) {
                columns.push(gridColumns[i].initialConfig.dataIndex);
                headers.push(gridColumns[i].initialConfig.header);
            }
        }
        list.push(processRow(headers));
        for(i=0; i<records.length;i++) {
            row = [];
            for(j=0; j<columns.length;j++) {
                row.push(records[i].get(columns[j]));
            }
            list.push(processRow(row));
        }
        var content = list.join("");
        var fileName = this.reportEntry.title.trim().replace(/ /g,"_")+".csv";
        Ung.Util.download(content, fileName, 'text/csv');
    },
    downloadChart: function() {
        if(!this.reportEntry) {
            return;
        }
        var chart = this.chartContainer.down("[name=chart]");
        if(!chart) {
            return;
        }
        var fileName = this.reportEntry.title.trim().replace(/ /g,"_");
        
        if (Ext.os.is.Desktop) {
            chart.download({
                filename: fileName
            });
        } else {
            chart.preview();
        } 
    },
    buildWindowAddCondition: function() {
        var me = this;
        if(!this.windowAddCondition) {
            this.windowAddCondition = Ext.create("Ung.EditWindow", {
                title: i18n._("Add Condition"),
                grid: null,
                height: 150,
                width: 600,
                sizeToRack: false,
                // size to grid on show
                sizeToGrid: false,
                center: true,
                items: [{
                    xtype: "panel",
                    bodyStyle: 'padding:10px 10px 0px 10px;',
                    items: [{
                        xtype: "component",
                        margin: '0 0 10 0',
                        html: i18n._("Add a condition using report data:")
                    }, {
                        xtype: "container",
                        layout: "column",
                        defaults: {
                            margin: '0 10 0 0'
                        },
                        items: [{
                            xtype: "textfield",
                            name: "column",
                            width: 180,
                            readOnly: true
                        }, {
                            xtype: 'combo',
                            width: 90,
                            name: "operator",
                            editable: false,
                            valueField: "name",
                            displayField: "name",
                            queryMode: 'local',
                            value: "=",
                            store: ["=", "!=", "<>", ">", "<", ">=", "<=", "between", "like", "in", "is"]
                        }, {
                            xtype: "textfield",
                            name: "value",
                            emptyText: i18n._("[no value]"),
                            width: 180
                        }]
                    }]
                }],
                updateAction: function() {
                    var data = {
                        column: this.down("[name=column]").getValue(),
                        operator: this.down("[name=operator]").getValue(),
                        value: this.down("[name=value]").getValue()
                    };
                    me.extraConditionsPanel.expand();
                    me.extraConditionsPanel.fillCondition(data);
                    this.cancelAction();
                },
                setCondition: function(data) {
                    this.show();
                    this.down("[name=column]").setValue(data.column);
                    this.down("[name=operator]").setValue(data.operator);
                    this.down("[name=value]").setValue(data.value);
                },
                isDirty: function() {
                    return false;
                },
                closeWindow: function() {
                    this.hide();
                }
            });
            this.subCmps.push(this.windowAddCondition);
        }
    },
    customizeReport: function() {
        if(!this.reportEntry) {
            return;
        }
        if(!this.winReportEditor) {
            var me = this;
            this.winReportEditor = Ext.create('Ung.window.ReportEditor', {
                sizeToComponent: this.chartContainer,
                title: i18n._("Advanced report customization"),
                forReportCustomization: true,
                parentCmp: this,
                grid: {},
                isDirty: function() {
                    return false;
                },
                updateAction: function() {
                    Ung.window.ReportEditor.prototype.updateAction.apply(this, arguments);
                    me.reportEntry = this.record.getData();
                    me.loadReport(me.reportEntry);
                }
            });
            this.subCmps.push(this.winReportEditor);
        }
        var record = Ext.create('Ext.data.Model', this.reportEntry);
        this.winReportEditor.populate(record);
        this.winReportEditor.show();
    },
    isDirty: function() {
        return false;
    },
    selectInitialReport: function() {
        this.down("grid[name=entriesGrid]").getSelectionModel().select(this.initialEntryIndex);
        this.loadReport(Ext.clone(this.reportEntries[this.initialEntryIndex]));
    },
    listeners: {
        "activate": {
            fn: function() {
                if(this.category && !this.reportEntry && this.reportEntries !=null && this.reportEntries.length > 0) {
                    this.selectInitialReport();
                }
            }
        },
        "deactivate": {
            fn: function() {
                if(this.autoRefreshEnabled) {
                    this.stopAutoRefresh(true);
                }
            }
        }
    }
});

Ext.define("Ung.panel.ExtraConditions", {
    extend: "Ext.panel.Panel",
    title: Ext.String.format( i18n._("Conditions: {0}"), i18n._("None")),
    collapsible: true,
    collapsed: false,
    floatable: false,
    split: true,
    defaultCount: 1,
    autoScroll: true,
    layout: { type: 'vbox'},
    initComponent: function() {
        this.columnsStore = Ext.create('Ext.data.Store', {
            sorters: "displayName",
            fields: ["name", "displayName"],
            data: []
        });
        this.items = [];
        for(var i=0; i<this.defaultCount; i++) {
            this.items.push(this.generateRow());
        }
        
        this.tbar = [{
            text: i18n._("Add Condition"),
            tooltip: i18n._('Add New Condition'),
            iconCls: 'icon-add-row',
            handler: function() {
                this.addRow();
            },
            scope: this
        }, '->', {
            text: i18n._("Delete All"),
            tooltip: i18n._('Delete All Conditions'),
            iconCls: 'cancel-icon',
            handler: function() {
                this.deleteConditions();
            },
            scope: this
        }];
        this.callParent(arguments);
    },
    generateRow: function(data) {
        if(!data) {
            data = {column: "", operator:"=", value: ""};
        }
        return {
            xtype: 'container',
            layout: 'column',
            name: 'condition',
            width: '100%',
            defaults: {
                margin: 3
            },
            items: [{
                xtype: 'combo',
                columnWidth: 0.4,
                emptyText: i18n._("[enter column]"),
                dataIndex: "column",
                typeAhead: true,
                valueField: "name",
                displayField: "displayName",
                queryMode: 'local',
                store: this.columnsStore,
                value: data.column,
                listeners: {
                    change: {
                        fn: function(combo, newValue, oldValue, opts) {
                            var skipReload = Ext.isEmpty(combo.next("[dataIndex=value]").getValue());
                            this.setConditions(skipReload);
                        },
                        scope: this,
                        buffer: 200
                    }
                }
            }, {
                xtype: 'combo',
                width: 100,
                dataIndex: "operator",
                editable: false,
                valueField: "name",
                displayField: "name",
                queryMode: 'local',
                value: data.operator,
                disabled: Ext.isEmpty(data.column),
                store: ["=", "!=", "<>", ">", "<", ">=", "<=", "between", "like", "in", "is"],
                listeners: {
                    change: {
                        fn: function(combo, newValue, oldValue, opts) {
                            var skipReload = Ext.isEmpty(combo.next("[dataIndex=value]").getValue());
                            this.setConditions(skipReload);
                        },
                        scope: this
                    }
                }
            }, {
                xtype: 'textfield',
                dataIndex: "value",
                columnWidth: 0.6,
                disabled: Ext.isEmpty(data.column),
                emptyText: i18n._("[no value]"),
                value: data.value,
                listeners: {
                    change: {
                        fn: function() {
                            this.setConditions();
                        },
                        scope: this,
                        buffer: 1200
                    },
                    specialkey: {
                        fn: function(field, e) {
                            if (e.getKey() == e.ENTER) {
                                this.setConditions();
                            }
                            
                        },
                        scope: this
                    }
                }
            }, {
                xtype: 'button',
                name: "delete",
                text: i18n._("Delete"),
                handler: Ext.bind(function(button) {
                    var skipReload = Ext.isEmpty(button.prev("[dataIndex=column]").getValue());
                    this.remove(button.up("container"));
                    this.setConditions(skipReload);
                }, this)
            }]
        };
    },
    addRow: function(data) {
      this.add(this.generateRow(data));
    },
    fillCondition: function(data) {
        var added = false;
        this.bulkOperation = true;
        Ext.Array.each(this.query("container[name=condition]"), function(item, index, len) {
            if(Ext.isEmpty(item.down("[dataIndex=column]").getValue())) {
                item.down("[dataIndex=column]").setValue(data.column);
                item.down("[dataIndex=operator]").setValue(data.operator);
                item.down("[dataIndex=value]").setValue(data.value);
                added = true;
                return false;
            }
        });
        if(!added) {
            this.addRow(data);
        }
        this.bulkOperation = false;
        this.setConditions();
    },
    deleteConditions: function() {
        var me = this;
        this.bulkOperation = true;
        Ext.Array.each(this.query("container[name=condition]"), function(item, index, len) {
            if(index < me.defaultCount) {
                item.down("[dataIndex=column]").setValue("");
                item.down("[dataIndex=operator]").setValue("=");
                item.down("[dataIndex=value]").setValue("");
            } else {
                me.remove(item);
            }
        });
        this.bulkOperation = false;
        var skipReload = !this.parentPanel.extraConditions || this.parentPanel.extraConditions.length==0;
        this.setConditions(skipReload);
    },
    setConditions: function(skipReload) {
        if(this.bulkOperation) {
            return;
        }
        var conditions = [], columnValue, operator, value, isEmptyColumn;
        Ext.Array.each(this.query("container[name=condition]"), function(item, index, len) {
            columnValue = item.down("[dataIndex=column]").getValue();
            operator = item.down("[dataIndex=operator]");
            value = item.down("[dataIndex=value]");
            isEmptyColumn = Ext.isEmpty(columnValue);
            if(!isEmptyColumn) {
                conditions.push({
                    "javaClass": "com.untangle.node.reporting.SqlCondition",
                    "column": columnValue,
                    "operator": operator.getValue(),
                    "value": value.getValue()
                });
            }
            operator.setDisabled(isEmptyColumn);
            value.setDisabled(isEmptyColumn);
        });
        this.parentPanel.extraConditions = (conditions.length>0)?conditions:null;
        this.setTitle(Ext.String.format( i18n._("Conditions: {0}"), (conditions.length>0)?conditions.length:i18n._("None")));
        if(!skipReload) {
            this.parentPanel.refreshHandler();
        }
    }
});

Ext.define("Ung.window.ReportEditor", {
    extend: "Ung.RowEditorWindow",
    rowEditorLabelWidth: 150,
    parentCmp: null,
    initComponent: function() {
        if(!this.forReportCustomization) {
            this.tbar = [{
                xtype: 'button',
                text: i18n._('View Report'),
                iconCls: 'icon-play',
                handler: function() {
                    if (this.validate()!==true) {
                        return;
                    }
                    if (this.record !== null) {
                        var data = Ext.clone(this.record.getData());
                        this.updateActionRecursive(this.items, data, 0);
                        this.parentCmp.viewReport(data);
                    }
                },
                scope: this
            }, {
                xtype: 'button',
                text: i18n._('Copy Report'),
                iconCls: 'action-icon',
                handler: function() {
                    var data = Ext.clone(this.grid.emptyRow);
                    this.updateActionRecursive(this.items, data, 0);
                    Ext.apply(data, {
                        uniqueId: this.getUniqueId(),
                        title: Ext.String.format("Copy of {0}", data.title)
                    });
                    this.closeWindow();
                    this.grid.addHandler(null, null, data);
                    Ext.MessageBox.alert(i18n._("Copy Report"), Ext.String.format(i18n._("You are now editing the copied report: '{0}'"), data.title));
                },
                scope: this
            }];
        } else {
            this.tbar = [{
                xtype: 'button',
                text: i18n._('Save as New Report'),
                iconCls: 'save-icon',
                handler: function() {
                    if (this.validate()!==true) {
                        return false;
                    }
                    if (this.record !== null) {
                        var data = {};
                        this.updateActionRecursive(this.items, data, 0);
                        this.record.set(data);
                        this.record.set("readOnly", false);
                        this.record.set("uniqueId", this.getUniqueId());
                        var reportEntry = this.record.getData();
                        
                        Ung.Main.getReportingManagerNew().saveReportEntry(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            this.closeWindow();
                            this.parentCmp.loadReportEntries(reportEntry.uniqueId);
                        }, this), reportEntry);
                    }
                },
                scope: this
            }];
        }
        var categoryStore = Ext.create('Ext.data.Store', {
            sorters: "displayName",
            fields: ["displayName"],
            data: []
        });
        rpc.nodeManager.getAllNodeProperties(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var data=[{displayName: 'System'}];
            var nodeProperties = result.list;
            for (var i=0; i< nodeProperties.length; i++) {
                if(!nodeProperties[i].invisible || nodeProperties[i].displayName == 'Shield') {
                    data.push(nodeProperties[i]);
                }
            }
            categoryStore.loadData(data);
        }, this));

        var tablesStore = Ext.create('Ext.data.Store', {
            sorters: "name",
            fields: ["name"],
            data: []
        });
        Ung.Main.getReportingManagerNew().getTables(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var tables = [];
            for (var i=0; i< result.length; i++) {
                tables.push({ name: result[i]});
            }
            tablesStore.loadData(tables);
        }, this));
        
        this.columnsStore = Ext.create('Ext.data.Store', {
            sorters: "displayName",
            fields: ["name", "displayName"],
            data: []
        });
        var chartTypes = [["TEXT", i18n._("Text")],["PIE_GRAPH", i18n._("Pie Graph")],["TIME_GRAPH", i18n._("Time Graph")]];
        
        var gridSqlConditionsEditor = Ext.create('Ung.grid.Panel',{
            name: 'Sql Conditions',
            height: 180,
            width: '100%',
            settingsCmp: this,
            addAtTop: false,
            hasImportExport: false,
            dataIndex: 'conditions',
            columnsDefaultSortable: false,
            recordJavaClass: "com.untangle.node.reporting.SqlCondition",
            emptyRow: {
                "column": "",
                "operator": "=",
                "value": ""
                
            },
            fields: ["column", "value", "operator"],
            columns: [{
                header: i18n._("Column"),
                dataIndex: 'column',
                width: 200
            }, {
                header: i18n._("Operator"),
                dataIndex: 'operator',
                width: 100
            }, {
                header: i18n._("Value"),
                dataIndex: 'value',
                flex: 1,
                width: 200
            }],
            rowEditorInputLines: [{
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype: 'combo',
                    emptyText: i18n._("[enter column]"),
                    dataIndex: "column",
                    fieldLabel: i18n._("Column"),
                    typeAhead:true,
                    allowBlank: false,
                    valueField: "name",
                    displayField: "displayName",
                    queryMode: 'local',
                    width: 350,
                    store: this.columnsStore
                }, {
                    xtype: 'label',
                    html: i18n._("(Columns list is loaded for the entered Table')"),
                    cls: 'boxlabel'
                }]
            }, {
                xtype: 'combo',
                emptyText: i18n._("[select operator]"),
                dataIndex: "operator",
                fieldLabel: i18n._("Operator"),
                editable: false,
                allowBlank: false,
                valueField: "name",
                displayField: "name",
                queryMode: 'local',
                store: ["=", "!=", "<>", ">", "<", ">=", "<=", "between", "like", "in", "is"]
            }, {
                xtype: 'textfield',
                dataIndex: "value",
                fieldLabel: i18n._("Value"),
                emptyText: i18n._("[no value]"),
                width: '90%'
            }],
            setValue: function (val) {
                var data = val || [];
                this.reload({data:data});
            },
            getValue: function () {
                var val = this.getList();
                return val.length == 0 ? null: val;
            }
        });
        
        this.inputLines = [{
            xtype: 'combo',
            name: 'Category',
            dataIndex: "category",
            allowBlank: false,
            editable: false,
            valueField: 'displayName',
            displayField: 'displayName',
            fieldLabel: i18n._('Category'),
            emptyText: i18n._("[select category]"),
            queryMode: 'local',
            width: 500,
            readOnly: this.forReportCustomization,
            store: categoryStore
        }, {
            xtype:'textfield',
            name: "Title",
            dataIndex: "title",
            allowBlank: false,
            fieldLabel: i18n._("Title"),
            emptyText: i18n._("[enter title]"),
            width: '100%'
        }, {
            xtype:'textfield',
            name: "Description",
            dataIndex: "description",
            fieldLabel: i18n._("Description"),
            emptyText: i18n._("[no description]"),
            width: '100%'
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '5 0 5 0',
            items: [{
                xtype:'checkbox',
                name: "Enabled",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enabled"),
                labelWidth: 150
            }, {
                xtype: 'numberfield',
                name: 'Display Order',
                fieldLabel: i18n._('Display Order'),
                dataIndex: "displayOrder",
                allowDecimals: false,
                minValue: 0,
                maxValue: 100000,
                allowBlank: false,
                width: 282,
                style: { marginLeft: '50px'}
            }]
        }, {
            xtype:'textfield',
            name: "Units",
            dataIndex: "units",
            fieldLabel: i18n._("Units"),
            emptyText: i18n._("[no units]"),
            width: 500
        }, {
            xtype: 'combo',
            name: "Table",
            dataIndex: "table",
            allowBlank: false,
            fieldLabel: i18n._("Table"),
            emptyText: i18n._("[enter table]"),
            valueField: "name",
            displayField: "name",
            queryMode: 'local',
            width: 500,
            store: tablesStore,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, newValue) {
                        Ung.panel.Reports.getColumnsForTable(newValue, this.columnsStore);
                    }, this),
                    buffer: 600
                }
            }
        }, {
            xtype: 'combo',
            name: 'Type',
            margin: '10 0 10 0',
            dataIndex: "type",
            allowBlank: false,
            editable: false,
            fieldLabel: i18n._('Type'),
            queryMode: 'local',
            width: 350,
            store: chartTypes,
            listeners: {
                "select": {
                    fn: Ext.bind(function(combo, records, eOpts) {
                        this.syncComponents();
                    }, this)
                }
            }
        }, {
            xtype: "container",
            dataIndex: "textColumns",
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype:'textareafield',
                name: "textColumns",
                grow: true,
                labelWidth: 150,
                fieldLabel: i18n._("Text Columns"),
                width: 500
            }, {
                xtype: 'label',
                html: i18n._("(enter one column per row)"),
                cls: 'boxlabel'
            }],
            setValue: function(value) {
                var textColumns  = this.down('textfield[name="textColumns"]');
                textColumns.setValue((value||[]).join("\n"));
            },
            getValue: function() {
                var textColumns = [];
                var val  = this.down('textfield[name="textColumns"]').getValue();
                if(!Ext.isEmpty(val)) {
                    var valArr = val.split("\n");
                    var colVal;
                    for(var i = 0; i< valArr.length; i++) {
                        colVal = valArr[i].trim();
                        if(!Ext.isEmpty(colVal)) {
                            textColumns.push(colVal);
                        }
                    }
                }
                
                return textColumns.length==0 ? null : textColumns;
            },
            setReadOnly: function(val) {
                this.down('textfield[name="textColumns"]').setReadOnly(val);
            }
        }, {
            xtype:'textfield',
            name: "textString",
            dataIndex: "textString",
            alowBlank: false,
            fieldLabel: i18n._("Text String"),
            width: '100%'
        }, {
            xtype:'textfield',
            name: "pieGroupColumn",
            dataIndex: "pieGroupColumn",
            fieldLabel: i18n._("Pie Group Column"),
            width: 500
        }, {
            xtype:'textfield',
            name: "pieSumColumn",
            dataIndex: "pieSumColumn",
            fieldLabel: i18n._("Pie Sum Column"),
            width: 500
        }, {
            xtype: 'numberfield',
            name: 'pieNumSlices',
            fieldLabel: i18n._('Pie Slices Number'),
            dataIndex: "pieNumSlices",
            allowDecimals: false,
            minValue: 0,
            maxValue: 1000,
            allowBlank: false,
            width: 350
        }, {
            xtype: 'combo',
            name: 'timeStyle',
            dataIndex: "timeStyle",
            editable: false,
            fieldLabel: i18n._('Time Chart Style'),
            queryMode: 'local',
            allowBlank: false,
            width: 350,
            store: [
                ["LINE", i18n._("Line")],
                ["BAR_3D", i18n._("Bar 3D")],
                ["BAR_3D_OVERLAPPED", i18n._("Bar 3D Overlapped")],
                ["BAR", i18n._("Bar")],
                ["BAR_OVERLAPPED", i18n._("Bar Overlapped")]
            ]
        }, {
            xtype: 'combo',
            name: 'timeDataInterval',
            dataIndex: "timeDataInterval",
            editable: false,
            fieldLabel: i18n._('Time Data Interval'),
            queryMode: 'local',
            allowBlank: false,
            width: 350,
            store: [
                ["AUTO", i18n._("Auto")],
                ["SECOND", i18n._("Second")],
                ["MINUTE", i18n._("Minute")],
                ["HOUR", i18n._("Hour")],
                ["DAY", i18n._("Day")],
                ["WEEK", i18n._("Week")],
                ["MONTH", i18n._("Month")]
            ]
        }, {
            xtype: "container",
            dataIndex: "timeDataColumns",
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype:'textareafield',
                name: "timeDataColumns",
                grow: true,
                labelWidth: 150,
                fieldLabel: i18n._("Time Data Columns"),
                width: 500
            }, {
                xtype: 'label',
                html: i18n._("(enter one column per row)"),
                cls: 'boxlabel'
            }],
            setValue: function(value) {
                var timeDataColumns  = this.down('textfield[name="timeDataColumns"]');
                timeDataColumns.setValue((value||[]).join("\n"));
            },
            getValue: function() {
                var timeDataColumns = [];
                var val  = this.down('textfield[name="timeDataColumns"]').getValue();
                if(!Ext.isEmpty(val)) {
                    var valArr = val.split("\n");
                    var colVal;
                    for(var i = 0; i< valArr.length; i++) {
                        colVal = valArr[i].trim();
                        if(!Ext.isEmpty(colVal)) {
                            timeDataColumns.push(colVal);
                        }
                    }
                }
                
                return timeDataColumns.length==0 ? null : timeDataColumns;
            },
            setReadOnly: function(val) {
                this.down('textfield[name="timeDataColumns"]').setReadOnly(val);
            }
        }, {
            xtype: "container",
            dataIndex: "colors",
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype:'textareafield',
                name: "colors",
                grow: true,
                labelWidth: 150,
                fieldLabel: i18n._("Colors"),
                width: 500
            }, {
                xtype: 'label',
                html: i18n._("(enter one color per row)"),
                cls: 'boxlabel'
            }],
            setValue: function(value) {
                var timeDataColumns  = this.down('textfield[name="colors"]');
                timeDataColumns.setValue((value||[]).join("\n"));
            },
            getValue: function() {
                var colors = [];
                var val  = this.down('textfield[name="colors"]').getValue();
                if(!Ext.isEmpty(val)) {
                    var valArr = val.split("\n");
                    var colVal;
                    for(var i = 0; i< valArr.length; i++) {
                        colVal = valArr[i].trim();
                        if(!Ext.isEmpty(colVal)) {
                            colors.push(colVal);
                        }
                    }
                }
                
                return colors.length==0 ? null : colors;
            },
            setReadOnly: function(val) {
                this.down('textfield[name="colors"]').setReadOnly(val);
            }
        }, {
            xtype: 'container',
            layout: 'column',
            margin: '10 0 10 0',
            items: [{
                xtype:'textfield',
                name: "orderByColumn",
                dataIndex: "orderByColumn",
                fieldLabel: i18n._("Order By Column"),
                labelWidth: 150,
                width: 350
            },{
                xtype: 'combo',
                name: 'orderDesc',
                dataIndex: "orderDesc",
                editable: false,
                fieldLabel: i18n._('Order Direction'),
                queryMode: 'local',
                width: 300,
                style: { marginLeft: '10px'},
                store: [[null, ""], [false, i18n._("Ascending")], [true, i18n._("Descending")]]
            }]
        }, {
            xtype:'fieldset',
            title: i18n._("Sql Conditions:"),
            items:[gridSqlConditionsEditor]
        }];
        this.callParent(arguments);
    },
    getUniqueId: function() {
        return "report-"+Math.random().toString(36).substr(2);
    },
    populate: function(record, addMode) {
        Ung.panel.Reports.getColumnsForTable(record.get("table"), this.columnsStore);
        if(!record.get("uniqueId")) {
            record.set("uniqueId", this.getUniqueId());
        }
        if(this.forReportCustomization && record.get("title").indexOf(i18n._("Custom")) == -1) {
            record.set("title", record.get("title") + " - " + i18n._("Custom"));
        }
        this.callParent(arguments);
    },
    syncComponents: function () {
        var type=this.down('combo[dataIndex=type]').getValue();
        var cmps = {
            textColumns: this.down('[dataIndex=textColumns]'),
            textString: this.down('[dataIndex=textString]'),
            pieGroupColumn: this.down('[dataIndex=pieGroupColumn]'),
            pieSumColumn: this.down('[dataIndex=pieSumColumn]'),
            pieNumSlices: this.down('[dataIndex=pieNumSlices]'),
            timeStyle: this.down('[dataIndex=timeStyle]'),
            timeDataInterval: this.down('[dataIndex=timeDataInterval]'),
            timeDataColumns: this.down('[dataIndex=timeDataColumns]'),
            colors: this.down('[dataIndex=colors]')
        };
        
        cmps.textColumns.setVisible(type=="TEXT");
        cmps.textColumns.setDisabled(type!="TEXT");

        cmps.textString.setVisible(type=="TEXT");
        cmps.textString.setDisabled(type!="TEXT");

        cmps.pieGroupColumn.setVisible(type=="PIE_GRAPH");
        cmps.pieGroupColumn.setDisabled(type!="PIE_GRAPH");

        cmps.pieSumColumn.setVisible(type=="PIE_GRAPH");
        cmps.pieSumColumn.setDisabled(type!="PIE_GRAPH");
        
        cmps.pieNumSlices.setVisible(type=="PIE_GRAPH");
        cmps.pieNumSlices.setDisabled(type!="PIE_GRAPH");

        cmps.timeStyle.setVisible(type=="TIME_GRAPH");
        cmps.timeStyle.setDisabled(type!="TIME_GRAPH");

        cmps.timeDataInterval.setVisible(type=="TIME_GRAPH");
        cmps.timeDataInterval.setDisabled(type!="TIME_GRAPH");
        
        cmps.timeDataColumns.setVisible(type=="TIME_GRAPH");
        cmps.timeDataColumns.setDisabled(type!="TIME_GRAPH");
        
        cmps.colors.setVisible(type!="TEXT");
        cmps.colors.setDisabled(type=="TEXT");
    }
});

Ext.define('Ung.panel.Events', {
    extend: 'Ext.panel.Panel',
    name: 'panelEvents',
    autoRefreshInterval: 5, //In Seconds
    layout: { type: 'border'},
    extraConditions: null,
    eventEntry: null,
    beforeDestroy: function() {
        Ext.destroy(this.subCmps);
        this.callParent(arguments);
    },
    initComponent: function() {
        this.subCmps = [];
        if(this.category) {
            this.helpSource = this.category.toLowerCase().replace(" ","_") + "_events";
            if(!this.title) {
                this.title = i18n._('Events');
            }
            if (!Ung.Main.isReportsAppInstalled()) {
                this.items = [{
                    region: 'center',
                    xtype: 'panel',
                    bodyPadding: 10,
                    html: i18n._("Reports application is required for this feature. Please install and enable the Reports application.")
                }];
                this.callParent(arguments);
                return;
            }
        }
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
        
        var policyStore = Ext.create('Ext.data.Store', {
            fields: ["policyId", "name"],
            data: [{policyId: -1, name: i18n._('All Racks')}].concat(rpc.policies)
        });
        var limitStore = Ext.create('Ext.data.Store', {
            fields: ["value", "name"],
            data: [{value: 1000, name: "1000 " + i18n._('Events')}, {value: 10000, name: "10000 " + i18n._('Events')}, {value: 50000, name: "50000 " + i18n._('Events')}]
        });
        this.filterFeature = Ext.create('Ung.grid.feature.GlobalFilter', {});
        this.items = [{
            region: 'west',
            title: i18n._("Select Event Log"),
            width: 200,
            hidden: !this.category,
            split: true,
            collapsible: true,
            collapsed: false,
            floatable: false,
            name: 'entriesGrid',
            xtype: 'grid',
            hideHeaders: true,
            store:  Ext.create('Ext.data.Store', {
                sorters: "displayOrder",
                fields: ["title", "displayOrder"],
                data: []
            }),
            columns: [{
                dataIndex: 'title',
                flex: 1
            }],
            listeners: {
                rowclick: Ext.bind(function( grid, record, tr, rowIndex, e, eOpts ) {
                    this.loadEventEntry(Ext.clone(record.getData()));
                }, this)
            }
        
        }, {
            region: 'center',
            layout: {type: 'border'},
            items: [{
                region: 'center',
                xtype: 'grid',
                name:'gridEvents',
                reserveScrollbar: true,
                title: ".",
                viewConfig: {
                    enableTextSelection: true
                },
                store:  Ext.create('Ext.data.Store', {
                    fields: [],
                    data: [],
                    proxy: {
                        type: 'memory',
                        reader: {
                            type: 'json'
                        }
                    }
                }),
                columns: [{
                    flex: 1
                }],
                plugins: ['gridfilters'],
                features: [this.filterFeature],
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'top',
                    items: [i18n._('Filter:'), {
                        xtype: 'textfield',
                        name: 'searchField',
                        hideLabel: true,
                        width: 130,
                        listeners: {
                            change: {
                                fn: function() {
                                    this.filterFeature.updateGlobalFilter(this.searchField.getValue(), this.caseSensitive.getValue());
                                },
                                scope: this,
                                buffer: 600
                            }
                        }
                    }, {
                        xtype: 'checkbox',
                        name: 'caseSensitive',
                        hideLabel: true,
                        margin: '0 4px 0 4px',
                        boxLabel: i18n._('Case sensitive'),
                        handler: function() {
                            this.filterFeature.updateGlobalFilter(this.searchField.getValue(),this.caseSensitive.getValue());
                        },
                        scope: this
                    }, {
                        xtype: 'button',
                        iconCls: 'icon-clear-filter',
                        text: i18n._('Clear Filters'),
                        tooltip: i18n._('Filters can be added by clicking on column headers arrow down menu and using Filters menu'),
                        handler: Ext.bind(function () {
                            this.gridEvents.clearFilters();
                            this.searchField.setValue("");
                        }, this)
                    }, {
                        text: i18n._('Reset View'),
                        tooltip: i18n._('Restore default columns positions, widths and visibility'),
                        handler: Ext.bind(function () {
                            Ext.state.Manager.clear(this.stateId);
                            this.reconfigure(null, this.getInitialConfig("columns"));
                        }, this)
                    },'->',{
                        xtype: 'button',
                        text: i18n._('Export'),
                        name: "Export",
                        tooltip: i18n._('Export Events to File'),
                        iconCls: 'icon-export',
                        handler: Ext.bind(this.exportHandler, this)
                    }]
                }, {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    items: [{
                        xtype: 'combo',
                        hidden: this.settingsCmp !=null && this.settingsCmp.nodeProperties != null && this.settingsCmp.nodeProperties.type == "SERVICE",
                        width: 120,
                        style: {marginRight: "5px"},
                        name: "policySelector",
                        editable: false,
                        valueField: "policyId",
                        displayField: "name",
                        queryMode: 'local',
                        value: rpc.currentPolicy.policyId,
                        store: policyStore
                    }, {
                        xtype: 'combo',
                        width: 100,
                        name: "limitSelector",
                        editable: false,
                        valueField: "value",
                        displayField: "name",
                        queryMode: 'local',
                        value: 1000,
                        store: limitStore
                    }, {
                        xtype: 'button',
                        text: i18n._('From'),
                        initialLabel:  i18n._('From'),
                        width: 132,
                        tooltip: i18n._('Select Start date and time'),
                        handler: Ext.bind(function(button) {
                            this.startDateWindow.buttonObj=button;
                            this.startDateWindow.show();
                        }, this)
                    },{
                        xtype: 'tbtext',
                        text: '-'
                    }, {
                        xtype: 'button',
                        text: i18n._('To'),
                        initialLabel:  i18n._('To'),
                        width: 132,
                        tooltip: i18n._('Select End date and time'),
                        handler: Ext.bind(function(button) {
                            this.endDateWindow.buttonObj=button;
                            this.endDateWindow.show();
                        }, this)
                    }, {
                        xtype: 'button',
                        text: i18n._('Refresh'),
                        name: "refresh",
                        tooltip: i18n._('Flush Events from Memory to Database and then Refresh'),
                        iconCls: 'icon-refresh',
                        handler:function () {
                            this.refreshHandler();
                        },
                        scope: this
                    }, {
                        xtype: 'button',
                        name: 'auto_refresh',
                        text: i18n._('Auto Refresh'),
                        enableToggle: true,
                        pressed: false,
                        tooltip: Ext.String.format(i18n._('Auto Refresh every {0} seconds'),this.autoRefreshInterval),
                        iconCls: 'icon-autorefresh',
                        handler: Ext.bind(function(button) {
                            if(button.pressed) {
                                this.startAutoRefresh();
                            } else {
                                this.stopAutoRefresh();
                            }
                        }, this)
                    }]
                }] 
            }, this.extraConditionsPanel = Ext.create("Ung.panel.ExtraConditions", {
                region: 'south',
                parentPanel: this,
                listeners: {
                    "expand": {
                        fn: Ext.bind(function() {
                            if(this.eventEntry) {
                                Ung.panel.Reports.getColumnsForTable(this.eventEntry.table, this.extraConditionsPanel.columnsStore);
                            }
                        }, this)
                    }
                }
            })]
        }];
        

        this.callParent(arguments);
        
        this.entriesGrid = this.down("grid[name=entriesGrid]");
        this.gridEvents = this.down("grid[name=gridEvents]");
        this.searchField=this.down('textfield[name=searchField]');
        this.caseSensitive = this.down('checkbox[name=caseSensitive]');
        this.policySelector = this.down("combo[name=policySelector]");
        this.limitSelector = this.down("combo[name=limitSelector]");
        if(this.category) {
            this.loadEventEntries();
        }
    },
    loadEventEntries: function() {
        Ung.Main.getReportingManagerNew().getEventEntries(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            this.entriesGrid.getStore().loadData(result.list);
            this.entriesGrid.setHidden(result.list.length <= 1);
        }, this), this.category);
    },
    
    loadEventEntry: function(eventEntry) {
        var i, col;
        this.eventEntry = eventEntry;
        if(!eventEntry.defaultColumns) {
            eventEntry.defaultColumns = [];
        }
        this.gridEvents.setTitle(eventEntry.title);
        var tableConfig = Ung.panel.Events.getTableConfig(this.eventEntry.table);
        if(!tableConfig) {
            tableConfig = {
                fields: [],
                columns: []
            };
            for(i=0; i< eventEntry.defaultColumns.length; i++) {
                col = eventEntry.defaultColumns[i];
                tableConfig.columns.push({
                    header: col.replace(/_/g," "),
                    dataIndex: col,
                    sortable: true,
                    flex: 1
                });
            }
        } else {
            for(i=0; i< tableConfig.columns.length; i++) {
                col = tableConfig.columns[i];
                col.hidden = eventEntry.defaultColumns.indexOf(col.dataIndex) < 0; 
            }
        }
        this.gridEvents.getStore().setFields(tableConfig.fields);
        this.gridEvents.setColumns(tableConfig.columns);
        this.refreshHandler();
        if(!this.extraConditionsPanel.getCollapsed()) {
            Ung.panel.Reports.getColumnsForTable(eventEntry.table, this.extraConditionsPanel.columnsStore);
        }
    },
    refreshHandler: function () {
        if(this.autoRefreshEnabled) {
            return;
        }
        this.refreshEvents();
    },
    autoRefresh: function() {
        if(!this.autoRefreshEnabled) {
            return;
        }
        this.refreshEvents();
    },
    refreshEvents: function() {
        if(!this.eventEntry) {
            return;
        }
        var policyId = this.policySelector.getValue();
        var limit = this.limitSelector.getValue();
        if(!this.autoRefreshEnabled) { this.setLoading(i18n._('Syncing events to Database... ')); }
        Ung.Main.getNodeReporting().flushEvents(Ext.bind(function(result, exception) {
            if(!this.autoRefreshEnabled) { this.setLoading(i18n._('Querying Database...')); }
            Ung.Main.getReportingManagerNew().getEventsForDateRangeResultSet(Ext.bind(function(result, exception) {
                this.setLoading(false);
                if(Ung.Util.handleException(exception)) return;
                this.loadResultSet(result);
            }, this), this.eventEntry, policyId, this.extraConditions, limit, this.startDateWindow.date, this.endDateWindow.date);
            
        }, this));
        
    },
    //Used to get dummy records in testing
    getTestRecord:function(index, fields) {
        var rec= {};
        var property;
        for (var i=0; i<fields.length ; i++) {
            property = (fields[i].mapping != null)?fields[i].mapping:fields[i].name;
            rec[property]=
                (property=='id')?index+1:
                (property=='time_stamp')?{javaClass:"java.util.Date", time: (new Date(Math.floor((Math.random()*index*12345678)))).getTime()}:
                (property.indexOf('_addr') != -1)?Math.floor((Math.random()*255))+"."+Math.floor((Math.random()*255))+"."+Math.floor((Math.random()*255))+"."+Math.floor((Math.random()*255))+"/"+Math.floor((Math.random()*32)):
                (property.indexOf('_port') != -1)?Math.floor((Math.random()*65000)):
            property+"_"+(i*index)+"_"+Math.floor((Math.random()*10));
        }
        return rec;
    },
    loadNextChunkCallback: function(result, exception) {
        if(Ung.Util.handleException(exception)) return;
        var newEventEntries = result;
        // If we got results append them to the current events list, and make another call for more
        if ( newEventEntries != null && newEventEntries.list != null && newEventEntries.list.length != 0 ) {
            this.eventEntries.push.apply( this.eventEntries, newEventEntries.list );
            if(!this.autoRefreshEnabled) { this.setLoading(i18n._('Fetching Events...') + ' (' + this.eventEntries.length + ')'); }
            this.reader.getNextChunk(Ext.bind(this.loadNextChunkCallback, this), 1000);
            return;
        }
        // If we got here, then we either reached the end of the resultSet or ran out of room display the results
        if (this.settingsCmp != null && this.gridEvents!=null && this.gridEvents.getStore() != null) {
            this.gridEvents.getStore().getProxy().setData(this.eventEntries);
            this.gridEvents.getStore().load();
        }
        this.setLoading(false);
        
        if(this!=null && this.rendered && this.autoRefreshEnabled) {
            if(this == this.settingsCmp.tabs.getActiveTab()) {
                Ext.Function.defer(this.autoRefresh, this.autoRefreshInterval*1000, this);
            } else {
                this.stopAutoRefresh(true);
            }
        }
    },
    // Refresh the events list
    loadResultSet: function(result) {
        this.eventEntries = [];

        if( testMode ) {
            var emptyRec={};
            var length = Math.floor((Math.random()*5000));
            var fields = this.entriesGrid.getStore().getFields();
            for(var i=0; i<length; i++) {
                this.eventEntries.push(this.getTestRecord(i, fields));
            }
            this.loadNextChunkCallback(null);
        }

        this.reader = result;
        if(this.reader) {
            if(!this.autoRefreshEnabled) { this.setLoading(i18n._('Fetching Events...')); }
            this.reader.getNextChunk(Ext.bind(this.loadNextChunkCallback, this), 1000);
        } else {
            this.loadNextChunkCallback(null);
        }
    },
    autoRefreshEnabled: false,
    startAutoRefresh: function(setButton) {
        if(!this.eventEntry) {
            this.down('button[name=auto_refresh]').toggle(false);
            return;
        }

        this.autoRefreshEnabled=true;
        this.down('button[name=refresh]').disable();
        this.autoRefresh();
    },
    stopAutoRefresh: function(setButton) {
        this.autoRefreshEnabled=false;
        if(setButton) {
            this.down('button[name=auto_refresh]').toggle(false);
        }
        this.down('button[name=refresh]').enable();
    },
    exportHandler: function() {
        if(!this.eventEntry) {
            return;
        }
        var processRow = function (row) {
            var data = [];
            for (var j = 0; j < row.length; j++) {
                var innerValue = row[j] == null ? '' : row[j].toString();
                data.push('"' + innerValue.replace(/"/g, '""') + '"');
            }
            return data.join(",") + '\r\n';
        };

        var records = this.gridEvents.getStore().getRange(), list=[], columns=[], headers=[], i, j, row;
        var gridColumns = this.gridEvents.getColumns();
        for(i=0; i<gridColumns.length;i++) {
            if(gridColumns[i].initialConfig.dataIndex) {
                columns.push(gridColumns[i].initialConfig.dataIndex);
                headers.push(gridColumns[i].initialConfig.header);
            }
        }
        list.push(processRow(headers));
        for(i=0; i<records.length;i++) {
            row = [];
            for(j=0; j<columns.length;j++) {
                row.push(records[i].get(columns[j]));
            }
            list.push(processRow(row));
        }
        var content = list.join("");
        var fileName = this.eventEntry.title.trim().replace(/ /g,"_")+".csv";
        Ung.Util.download(content, fileName, 'text/csv');
    },
    isDirty: function() {
        return false;
    },
    selectInitialEvent: function() {
        this.entriesGrid.getSelectionModel().select(0);
        var record = this.entriesGrid.getSelectionModel().getSelection()[0];
        this.loadEventEntry(record.getData());
    },
    listeners: {
        "activate": {
            fn: function() {
                if(this.category && !this.eventEntry && this.entriesGrid.getStore().getCount() > 0) {
                    this.selectInitialEvent();
                }
            }
        },
        "deactivate": {
            fn: function() {
                if(this.autoRefreshEnabled) {
                    this.stopAutoRefresh(true);
                }
            }
        }
    },
    statics: {
        getTableConfig: function(table) {
            if(!this.tableConfig) {
                this.tableConfig = {
                    sessions: {
                        fields: [{
                            name: 'time_stamp',
                            sortType: 'asTimestamp'
                        }, {
                            name: 'bandwidth_control_priority'
                        }, {
                            name: 'bandwidth_control_rule'
                        }, {
                            name: 'protocol'
                        }, {
                            name: 'username'
                        }, {
                            name: 'hostname'
                        }, {
                            name: 'c_client_addr',
                            sortType: 'asIp'
                        }, {
                            name: 'c_client_port',
                            sortType: 'asInt'
                        }, {
                            name: 'c_server_addr',
                            sortType: 'asIp'
                        }, {
                            name: 'c_server_port',
                            sortType: 'asInt'
                        }, {
                            name: 's_server_addr',
                            sortType: 'asIp'
                        }, {
                            name: 's_server_port',
                            sortType: 'asInt'
                        }, {
                            name: 'application_control_application',
                            type: 'string'
                        }, {
                            name: 'application_control_protochain',
                            type: 'string'
                        }, {
                            name: 'application_control_flagged',
                            type: 'boolean'
                        }, {
                            name: 'application_control_blocked',
                            type: 'boolean'
                        }, {
                            name: 'application_control_confidence'
                        }, {
                            name: 'application_control_detail'
                        }, {
                            name: 'application_control_lite_blocked'
                        }, {
                            name: 'application_control_lite_protocol',
                            type: 'string'
                        }, {
                            name: 'application_control_ruleid'
                        }, {
                            name: 'ssl_inspector_status'
                        }, {
                            name: 'ssl_inspector_detail'
                        }, {
                            name: 'ssl_inspector_ruleid'
                        }, {
                            name: 'policy_id'
                        }, {
                            name: 'firewall_blocked'
                        }, {
                            name: 'firewall_flagged'
                        }, {
                            name: 'firewall_rule_index'
                        }, {
                            name: 'ips_blocked'
                        }, {
                            name: 'ips_ruleid'
                        }, {
                            name: 'ips_description',
                            type: 'string'
                        }, {
                            name: "captive_portal_rule_index"
                        }, {
                            name: "captive_portal_blocked"
                        }],
                        columns: [{
                            header: i18n._("Timestamp"),
                            width: Ung.Util.timestampFieldWidth,
                            sortable: true,
                            dataIndex: 'time_stamp',
                            renderer: function(value) {
                                return i18n.timestampFormat(value);
                            }
                        }, {
                            header: i18n._("Protocol"),
                            width: Ung.Util.portFieldWidth,
                            sortable: true,
                            dataIndex: 'protocol',
                            renderer: function(value) {
                                if (value == 17)
                                    return "UDP";
                                if (value == 6)
                                    return "TCP";
                                return value;
                            }
                        }, {
                            header: i18n._("Client"),
                            width: Ung.Util.ipFieldWidth,
                            sortable: true,
                            dataIndex: 'c_client_addr'
                        }, {
                            header: i18n._("Client port"),
                            width: Ung.Util.portFieldWidth,
                            sortable: true,
                            dataIndex: 'c_client_port',
                            filter: {
                                type: 'numeric'
                            }
                        }, {
                            header: i18n._("Username"),
                            width: Ung.Util.usernameFieldWidth,
                            sortable: true,
                            dataIndex: 'username'
                        }, {
                            header: i18n._("Hostname"),
                            width: Ung.Util.hostnameFieldWidth,
                            sortable: true,
                            dataIndex: 'hostname'
                        }, {
                            header: i18n._("Server"),
                            width: Ung.Util.ipFieldWidth,
                            sortable: true,
                            dataIndex: 'c_server_addr'
                        }, {
                            header: i18n._("Server Port"),
                            width: Ung.Util.portFieldWidth,
                            sortable: true,
                            dataIndex: 'c_server_port',
                            filter: {
                                type: 'numeric'
                            }
                        }, {
                            header: i18n._("Rule ID"),
                            width: 70,
                            sortable: true,
                            dataIndex: 'application_control_ruleid',
                            filter: {
                                type: 'numeric'
                            }
                        }, {
                            header: i18n._("Priority"),
                            width: 120,
                            sortable: true,
                            dataIndex: 'bandwidth_control_priority',
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
                            header: i18n._("Rule"),
                            width: 120,
                            sortable: true,
                            dataIndex: 'bandwidth_control_rule',
                            renderer: function(value) {
                                return Ext.isEmpty(value) ? i18n._("none") : value;
                            }
                        }, {
                            header: i18n._("Application"),
                            width: 120,
                            sortable: true,
                            dataIndex: 'application_control_application'
                        }, {
                            header: i18n._("ProtoChain"),
                            width: 180,
                            sortable: true,
                            dataIndex: 'application_control_protochain'
                        }, {
                            header: i18n._("Blocked (Application Control)"),
                            width: Ung.Util.booleanFieldWidth,
                            sortable: true,
                            dataIndex: 'application_control_blocked',
                            filter: {
                                type: 'boolean',
                                yesText: 'true',
                                noText: 'false'
                            }
                        }, {
                            header: i18n._("Flagged (Application Control)"),
                            width: Ung.Util.booleanFieldWidth,
                            sortable: true,
                            dataIndex: 'application_control_flagged',
                            filter: {
                                type: 'boolean',
                                yesText: 'true',
                                noText: 'false'
                            }
                        }, {
                            header: i18n._("Confidence"),
                            width: Ung.Util.portFieldWidth,
                            sortable: true,
                            dataIndex: 'application_control_confidence',
                            filter: {
                                type: 'numeric'
                            }
                        }, {
                            header: i18n._("Detail"),
                            width: 200,
                            sortable: true,
                            dataIndex: 'application_control_detail'
                        },{
                            header: i18n._("Protocol (Application Control Lite)"),
                            width: 120,
                            sortable: true,
                            dataIndex: 'application_control_lite_protocol'
                        }, {
                            header: i18n._("Blocked (Application Control Lite)"),
                            width: Ung.Util.booleanFieldWidth,
                            sortable: true,
                            dataIndex: 'application_control_lite_blocked',
                            filter: {
                                type: 'boolean',
                                yesText: 'true',
                                noText: 'false'
                            }
                        }, {
                            header: i18n._("Rule ID (HTTPS Inspector)"),
                            width: 70,
                            sortable: true,
                            dataIndex: 'ssl_inspector_ruleid',
                            filter: {
                                type: 'numeric'
                            }
                        }, {
                            header: i18n._("Status (HTTPS Inspector)"),
                            width: 100,
                            sortable: true,
                            dataIndex: 'ssl_inspector_status'
                        }, {
                            header: i18n._("Detail (HTTPS Inspector)"),
                            width: 250,
                            sortable: true,
                            dataIndex: 'ssl_inspector_detail'
                        }, {
                            header: i18n._('Policy Id'),
                            width: 60,
                            sortable: true,
                            flex:1,
                            dataIndex: 'policy_id',
                            renderer: Ung.Main.getPolicyName
                        }, {
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
                            header: i18n._('Rule Id (Firewall)'),
                            width: 60,
                            sortable: true,
                            flex:1,
                            dataIndex: 'firewall_rule_index',
                            filter: {
                                type: 'numeric'
                            }
                        }, {
                            header: i18n._("Server") ,
                            width: Ung.Util.ipFieldWidth + 40, // +40 for column header
                            sortable: true,
                            dataIndex: 's_server_addr'
                        }, {
                            header: i18n._("Server Port"),
                            width: Ung.Util.portFieldWidth + 40, // +40 for column header
                            sortable: true,
                            dataIndex: 's_server_port',
                            filter: {
                                type: 'numeric'
                            }
                        }, {
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
                            header: i18n._('Rule Id (Intrusion Prevention)'),
                            width: 60,
                            sortable: true,
                            dataIndex: 'ips_ruleid'
                        }, {
                            header: i18n._('Rule Description (Intrusion Prevention)'),
                            width: 150,
                            sortable: true,
                            flex:1,
                            dataIndex: 'ips_description'
                        }, {
                            header: i18n._("Rule ID (Captive Portal)"),
                            width: 80,
                            dataIndex: 'captive_portal_rule_index'
                        }, {
                            header: i18n._("Captured"),
                            width: 100,
                            sortable: true,
                            dataIndex: "captive_portal_blocked",
                            filter: {
                                type: 'boolean',
                                yesText: 'true',
                                noText: 'false'
                            }
                        }]
                    },
                    http_events: {
                        fields: [{
                            name: 'time_stamp',
                            sortType: 'asTimestamp'
                        }, {
                            name: 'web_filter_lite_blocked',
                            type: 'boolean'
                        }, {
                            name: 'web_filter_blocked',
                            type: 'boolean'
                        }, {
                            name: 'web_filter_lite_flagged',
                            type: 'boolean'
                        }, {
                            name: 'web_filter_flagged',
                            type: 'boolean'
                        }, {
                            name: 'web_filter_lite_category',
                            type: 'string'
                        }, {
                            name: 'web_filter_category',
                            type: 'string'
                        }, {
                            name: 'c_client_addr',
                            sortType: 'asIp'
                        }, {
                            name: 'username'
                        }, {
                            name: 'hostname'
                        }, {
                            name: 'c_server_addr',
                            sortType: 'asIp'
                        }, {
                            name: 's_server_port',
                            sortType: 'asInt'
                        }, {
                            name: 'host'
                        }, {
                            name: 'uri'
                        }, {
                            name: 'web_filter_lite_reason',
                            type: 'string',
                            convert: Ung.CustomEventLog.httpEventConvertReason
                        }, {
                            name: 'web_filter_reason',
                            type: 'string',
                            convert: Ung.CustomEventLog.httpEventConvertReason
                        }, {
                            name: 'ad_blocker_action',
                            type: 'string',
                            convert: function(value) {
                                return (value == 'B')?i18n._("block") : i18n._("pass");
                            }
                        }, {
                            name: 'ad_blocker_cookie_ident'
                        }, {
                            name: 'virus_blocker_name'
                        }, {
                            name: 'virus_blocker_lite_name'
                        }],
                        columns: [{
                            header: i18n._("Timestamp"),
                            width: Ung.Util.timestampFieldWidth,
                            sortable: true,
                            dataIndex: 'time_stamp',
                            renderer: function(value) {
                                return i18n.timestampFormat(value);
                            }
                        }, {
                            header: i18n._("Hostname"),
                            width: Ung.Util.hostnameFieldWidth,
                            sortable: true,
                            dataIndex: 'hostname'
                        }, {
                            header: i18n._("Client"),
                            width: Ung.Util.ipFieldWidth,
                            sortable: true,
                            dataIndex: 'c_client_addr'
                        }, {
                            header: i18n._("Username"),
                            width: Ung.Util.usernameFieldWidth,
                            sortable: true,
                            dataIndex: 'username'
                        }, {
                            header: i18n._("Host"),
                            width: Ung.Util.hostnameFieldWidth,
                            sortable: true,
                            dataIndex: 'host'
                        }, {
                            header: i18n._("Uri"),
                            flex:1,
                            width: Ung.Util.uriFieldWidth,
                            sortable: true,
                            dataIndex: 'uri'
                        }, {
                            header: i18n._("Blocked (Webfilter Lite)"),
                            width: Ung.Util.booleanFieldWidth,
                            sortable: true,
                            dataIndex: 'web_filter_lite_blocked',
                            filter: {
                                type: 'boolean',
                                yesText: 'true',
                                noText: 'false'
                            }
                        }, {
                            header: i18n._("Flagged (Webfilter Lite)"),
                            width: Ung.Util.booleanFieldWidth,
                            dataIndex: 'web_filter_lite_flagged',
                            filter: {
                                type: 'boolean',
                                yesText: 'true',
                                noText: 'false'
                            }
                        }, {
                            header: i18n._("Reason For Action (Webfilter Lite)"),
                            width: 150,
                            sortable: true,
                            dataIndex: 'web_filter_lite_reason'
                        }, {
                            header: i18n._("Category (Webfilter Lite)"),
                            width: 120,
                            sortable: true,
                            dataIndex: 'web_filter_lite_category'
                        }, {
                            header: i18n._("Blocked  (Webfilter)"),
                            width: Ung.Util.booleanFieldWidth,
                            sortable: true,
                            dataIndex: 'web_filter_blocked',
                            filter: {
                                type: 'boolean',
                                yesText: 'true',
                                noText: 'false'
                            }
                        }, {
                            header: i18n._("Flagged (Webfilter)"),
                            width: Ung.Util.booleanFieldWidth,
                            sortable: true,
                            dataIndex: 'web_filter_flagged',
                            filter: {
                                type: 'boolean',
                                yesText: 'true',
                                noText: 'false'
                            }
                        }, {
                            header: i18n._("Reason For Action (Webfilter)"),
                            width: 150,
                            sortable: true,
                            dataIndex: 'web_filter_reason'
                        }, {
                            header: i18n._("Category (Webfilter)"),
                            width: 120,
                            sortable: true,
                            dataIndex: 'web_filter_category'
                        }, {
                            header: i18n._("Server"),
                            width: Ung.Util.ipFieldWidth,
                            sortable: true,
                            dataIndex: 'c_server_addr'
                        }, {
                            header: i18n._("Server Port"),
                            width: Ung.Util.portFieldWidth,
                            sortable: true,
                            dataIndex: 's_server_port',
                            filter: {
                                type: 'numeric'
                            }
                        }, {
                            header: i18n._("Action (Ad Blocker)"),
                            width: 120,
                            sortable: true,
                            dataIndex: 'ad_blocker_action'
                        }, {
                            header: i18n._("Cookie"),
                            width: 100,
                            sortable: true,
                            dataIndex: 'ad_blocker_cookie_ident'
                        }, {
                            header: i18n._("Virus Name (Virus Blocker Lite)"),
                            width: 140,
                            sortable: true,
                            dataIndex: 'virus_blocker_lite_name'
                        }, {
                            header: i18n._("Virus Name (Virus Blocker)"),
                            width: 140,
                            sortable: true,
                            dataIndex: 'virus_blocker_name'
                        }]
                    }
                };
                var key, columns, i;
                for(key in this.tableConfig) {
                    columns = this.tableConfig[key].columns;
                    for(i=0; i<columns.length; i++) {
                        columns[i].filter = { type: 'string' };
                    }
                }
            }
            return this.tableConfig[table];
        }
    }
});