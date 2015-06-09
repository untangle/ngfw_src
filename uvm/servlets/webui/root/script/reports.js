Ext.define('Ung.panel.Reports', {
    extend: 'Ext.panel.Panel',
    statics: {
        getColumnsHumanReadableNames: function() {
            return {
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
            name: 'reportDataGrid',
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
                            this.refreshHandler(true);
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
                            if(this.reportEntry) {
                                this.extraConditionsPanel.getColumnsForTable(this.reportEntry.table);
                            }
                        }, this)
                    }
                }
            })]
        }];
        
        if(this.category) {
            var reportEntriesStore = Ext.create('Ext.data.Store', {
                fields: ["title"],
                data: []
            });
            
            Ung.Main.getReportingManagerNew().getReportEntries(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.reportEntries = [];
                this.initialReportEntryIndex = null;
                var reportEntry;
                for(var i=0; i<result.list.length; i++) {
                    reportEntry = result.list[i];
                    if(reportEntry.enabled) {
                        this.reportEntries.push(reportEntry);
                        if(this.initialReportEntryIndex==null && reportEntry.type!="TEXT") {
                            this.initialReportEntryIndex = i;
                        }
                    }
                }
                if(this.initialReportEntryIndex == null && this.reportEntries.length>0) {
                    this.initialReportEntryIndex = 0;
                }
                reportEntriesStore.loadData(this.reportEntries);
            }, this), this.category);
            this.items.push({
                region: 'west',
                title: i18n._("Select Report"),
                width: 250,
                split: true,
                collapsible: true,
                collapsed: false,
                floatable: false,
                name: 'reportEntriesGrid',
                xtype: 'grid',
                hideHeaders: true,
                store:  reportEntriesStore,
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
        this.reportDataGrid = this.down("grid[name=reportDataGrid]");
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
            this.reportDataGrid.getStore().loadData(reportData);
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
            this.reportDataGrid.getStore().loadData(data);
        } else if(this.reportEntry.type == 'TIME_GRAPH') {
            chart.getStore().loadData(data);
            this.reportDataGrid.getStore().loadData(data);
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
        this.reportDataGrid.getStore().loadData([]);
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
            this.reportDataGrid.setColumns([{
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
                theme: 'green-gradients',
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
            this.reportDataGrid.setColumns([{
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
                theme: 'green-gradients',
                border: false,
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
            this.reportDataGrid.setColumns(reportDataColumns);
        }
        this.chartContainer.add(chart); 
        Ung.Main.getReportingManagerNew().getDataForReportEntry(Ext.bind(function(result, exception) {
            this.setLoading(false);
            if(Ung.Util.handleException(exception)) return;
            this.loadReportData(result.list);
        }, this), this.reportEntry, this.startDateWindow.date, this.endDateWindow.date, this.extraConditions, -1);
        if(!this.extraConditionsPanel.getCollapsed()) {
            this.extraConditionsPanel.getColumnsForTable(this.reportEntry.table);
        }
    },
    refreshHandler: function (forceFlush) {
        if(!this.reportEntry || this.autoRefreshEnabled) {
            return;
        }
        this.setLoading(i18n._('Refreshing report... '));
        Ung.Main.getNodeReporting().flushEvents(Ext.bind(function(result, exception) {
            Ung.Main.getReportingManagerNew().getDataForReportEntry(Ext.bind(function(result, exception) {
                this.setLoading(false);
                if(Ung.Util.handleException(exception)) return;
                this.loadReportData(result.list);
            }, this), this.reportEntry, this.startDateWindow.date, this.endDateWindow.date, this.extraConditions, -1);
            
        }, this));
    },
    autoRefresh: function() {
        if(!this.reportEntry) {
            return;
        }
        Ung.Main.getNodeReporting().flushEvents(Ext.bind(function(result, exception) {
            Ung.Main.getReportingManagerNew().getDataForReportEntry(Ext.bind(function(result, exception) {
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

        var records = this.reportDataGrid.getStore().getRange(), list=[], columns=[], headers=[], i, j, row;
        var gridColumns = this.reportDataGrid.getColumns();
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
    isDirty: function() {
        return false;
    },
    listeners: {
        "activate": {
            fn: function() {
                if(this.category && !this.reportEntry && this.reportEntries !=null && this.reportEntries.length > 0) {
                    this.down("grid[name=reportEntriesGrid]").getSelectionModel().select(this.initialReportEntryIndex);
                    this.loadReport(Ext.clone(this.reportEntries[this.initialReportEntryIndex]));
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
    getColumnsForTable: function(table) {
        if(table != null && table.length > 2) {
            Ung.Main.getReportingManagerNew().getColumnsForTable(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                var columns = [], readableName;
                for (var i=0; i< result.length; i++) {
                    readableName = this.columnsHumanReadableNames[result[i]];
                    columns.push({
                        name: result[i],
                        displayName: readableName!=null ? readableName:result[i]
                    });
                }
                this.columnsStore.loadData(columns);
            }, this), table);
        }
    },
    initComponent: function() {
        this.columnsStore = Ext.create('Ext.data.Store', {
            sorters: "displayName",
            fields: ["name", "displayName"],
            data: []
        });
        this.columnsHumanReadableNames = Ung.panel.Reports.getColumnsHumanReadableNames();
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
                    "javaClass": "com.untangle.uvm.node.SqlCondition",
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
            this.parentPanel.refreshHandler(true);
        }
    }
});