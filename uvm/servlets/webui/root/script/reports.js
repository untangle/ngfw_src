Ext.define('Ung.panel.Reports', {
    extend: 'Ext.panel.Panel',
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
            listeners: {
                rowclick: Ext.bind(function( grid, record, tr, rowIndex, e, eOpts ) {
                    //TODO: add extra condition
                }, this)
            },
            tbar: ['->',{
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
                        name: "extraConditions",
                        text: i18n._('Toggle Conditions'),
                        width: 132,
                        tooltip: i18n._('Add extra SQL conditions to report'),
                        handler: Ext.bind(function(button) {
                            this.extraConditionsPanel.toggleCollapse();
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
                var textColumns=[];
                for(i=0; i<this.reportEntry.textColumns.length; i++) {
                    column = this.reportEntry.textColumns[i].split(" ").splice(-1)[0];
                    infos.push(data[0][column]);
                    reportData.push({data: column, value: data[0][column]});
                }
            }
            chart.update(Ext.String.format.apply(Ext.String.format, [i18n._(this.reportEntry.textString)].concat(infos)));
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
            chart.getStore().loadData(topData);
            this.reportDataGrid.getStore().loadData(data);
        } else if(this.reportEntry.type == 'TIME_GRAPH') {
            chart.getStore().loadData(data);
            this.reportDataGrid.getStore().loadData(data);
        }
    },
    loadReport: function(reportEntry) {
        this.reportEntry = reportEntry;
        if(this.autoRefreshEnabled) {
            this.stopAutoRefresh(true);
        }
        this.chartContainer.removeAll();
        this.setLoading(i18n._('Loading report... '));
        Ung.Main.getReportingManagerNew().getDataForReportEntry(Ext.bind(function(result, exception) {
            var i, column;
            if(Ung.Util.handleException(exception)) return;
            if(!this.chartContainer || !this.chartContainer.isVisible()) {
                return;
            }
            
            var data = result.list;
            var chart, dataStore, reportData=[];
            this.reportDataGrid.getStore().loadData([]);
            if(reportEntry.type == 'TEXT') {
                var infos=[];
                if(data.length>0 && reportEntry.textColumns!=null) {
                    var textColumns=[];
                    for(i=0; i<reportEntry.textColumns.length; i++) {
                        column = reportEntry.textColumns[i].split(" ").splice(-1)[0];
                        infos.push(data[0][column]);
                        reportData.push({data: column, value: data[0][column]});
                    }
                }
                chart = {
                    xtype: 'component',
                    name: "chart",
                    margin: 15,
                    //TODO: get data in the right format now is an array with a single element {scanned: x, flagged: y, blocked: z} or make the sting use the properties instead of {0}, {1}, {2}
                    html: Ext.String.format.apply(Ext.String.format, [i18n._(reportEntry.textString)].concat(infos))
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
                this.reportDataGrid.getStore().loadData(reportData);
                
            } else if(reportEntry.type == 'PIE_GRAPH') {
                var topData = data;
                if(reportEntry.pieNumSlices && data.length>reportEntry.pieNumSlices) {
                    topData = [];
                    var others = {value:0};
                    others[reportEntry.pieGroupColumn] = i18n._("Others");
                    for(i=0; i<data.length; i++) {
                        if(i < reportEntry.pieNumSlices) {
                            topData.push(data[i]);
                        } else {
                            others.value+=data[i].value;
                        }
                        
                    }
                    topData.push(others);
                }

                var descriptionFn = function(val, record) {
                    var title = (record.get(reportEntry.pieGroupColumn)==null)?i18n._("none") : record.get(reportEntry.pieGroupColumn);
                    var value = (reportEntry.units == "bytes") ? Ung.Util.bytesRenderer(record.get("value")) : record.get("value") + " " + i18n._(reportEntry.units);
                    return title + ": " + value;
                };
                dataStore = Ext.create('Ext.data.JsonStore', {
                    fields: [{name: "description", convert: descriptionFn }, {name:'value'} ],
                    data: topData
                }); 

                chart = {
                    xtype: 'polar',
                    name: "chart",
                    store: dataStore,
                    theme: 'green-gradients',
                    border: false,
                    width: '100%',
                    height: '100%',
                    insetPadding: {top: 50, left: 10, right: 10, bottom: 10},
                    innerPadding: 20,
                    legend: {
                        docked: 'right'
                    },
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
                    interactions: ['rotate', 'itemhighlight'],
                    series: [{
                        type: 'pie',
                        angleField: 'value',
                        label: {
                            field: "description",
                            calloutLine: {
                                length: 60,
                                width: 3
                                // specifying 'color' is also possible here
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
                                this.setHtml(storeItem.get("description"));
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
                }]);
                this.reportDataGrid.getStore().loadData(data);
            } else if(reportEntry.type == 'TIME_GRAPH') {
                var axesFields = [], series=[];
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
                if(reportEntry.timeStyle == 'BAR') {
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
                    
                    dataStore = Ext.create('Ext.data.JsonStore', {
                        fields: storeFields,
                        data: data
                    });
                    chart = {
                        xtype: 'cartesian',
                        name: "chart",
                        store: dataStore,
                        theme: 'green-gradients',
                        border: false,
                        width: '100%',
                        height: '100%',
                        insetPadding: {top: 50, left: 10, right: 10, bottom: 10},
                        legend: {
                            docked: 'bottom'
                        },
                        tbar: ['->', {
                            xtype: 'button',
                            text: i18n._("Switch to Line Chart"),
                            handler: Ext.bind(function() {
                                this.reportEntry.timeStyle = 'LINE';
                                this.loadReport(this.reportEntry);
                            }, this)
                        }, {
                            xtype: 'button',
                            hidden: reportEntry.timeDataColumns.length <= 1,
                            text: i18n._("Switch to Overlapped Bar Chart"),
                            handler: Ext.bind(function() {
                                this.reportEntry.timeStyle = 'BAR_OVERLAP';
                                this.loadReport(this.reportEntry);
                            }, this)
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
                        }],
                        interactions: ['itemhighlight'],
                        axes: [{
                            type: 'numeric3d',
                            fields: axesFields,
                            position: 'left',
                            grid: true,
                            minimum: 0,
                            renderer: function (v) {
                                return (reportEntry.units == "bytes") ? Ung.Util.bytesRenderer(v) : v + " " + i18n._(reportEntry.units);
                            }
                        }, {
                            type: 'category3d',
                            fields: 'time_trunc',
                            position: 'bottom',
                            grid: true,
                            label: {
                                rotate: {
                                    degrees: -45
                                }
                            }
                        }],
                        series: [{
                            type: 'bar3d',
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
                                    this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.field) + " " +i18n._(reportEntry.units));
                                }
                            }
                        }]
                    };                    
                } else if (reportEntry.timeStyle == 'BAR_OVERLAP') {
                    for(i=0; i<reportEntry.timeDataColumns.length; i++) {
                        column = reportEntry.timeDataColumns[i].split(" ").splice(-1)[0];
                        axesFields.push(column);
                        storeFields.push({name: column, convert: zeroFn});
                        reportDataColumns.push({
                            dataIndex: column,
                            header: column,
                            width: reportEntry.timeDataColumns.length>2? 60:90
                        });
                        console.log(column);
                        series.push({
                            type: 'bar3d',
                            axis: 'left',
                            title: column,
                            xField: 'time_trunc',
                            yField: column,
                            smooth: true,
                            style: {
                                opacity: 0.70,
                                lineWidth: (i+1)*5
                            },
                            tooltip: {
                                trackMouse: true,
                                style: 'background: #fff',
                                renderer: function(storeItem, item) {
                                    var title = item.series.getTitle();
                                    this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " +i18n._(reportEntry.units));
                                }
                            }
                        });
                    }
                    
                    dataStore = Ext.create('Ext.data.JsonStore', {
                        fields: storeFields,
                        data: data
                    });
                    chart = {
                        xtype: 'cartesian',
                        name: "chart",
                        store: dataStore,
                        theme: 'green-gradients',
                        border: false,
                        width: '100%',
                        height: '100%',
                        insetPadding: {top: 50, left: 10, right: 10, bottom: 10},
                        legend: {
                            docked: 'bottom'
                        },
                        tbar: ['->', {
                            xtype: 'button',
                            text: i18n._("Switch to Bar Chart"),
                            handler: Ext.bind(function() {
                                this.reportEntry.timeStyle = 'BAR';
                                this.loadReport(this.reportEntry);
                            }, this)
                        }, {
                            xtype: 'button',
                            text: i18n._("Switch to Line Chart"),
                            handler: Ext.bind(function() {
                                this.reportEntry.timeStyle = 'LINE';
                                this.loadReport(this.reportEntry);
                            }, this)
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
                        }],
                        interactions: ['itemhighlight'],
                        axes: [{
                            type: 'numeric3d',
                            fields: axesFields,
                            position: 'left',
                            grid: true,
                            minimum: 0,
                            renderer: function (v) {
                                return (reportEntry.units == "bytes") ? Ung.Util.bytesRenderer(v) : v + " " + i18n._(reportEntry.units);
                            }
                        }, {
                            type: 'category3d',
                            fields: 'time_trunc',
                            position: 'bottom',
                            grid: true,
                            label: {
                                rotate: {
                                    degrees: -45
                                }
                            }
                        }],
                        series: series
                    };
                } else if (reportEntry.timeStyle == 'LINE') {
                    for(i=0; i<reportEntry.timeDataColumns.length; i++) {
                        column = reportEntry.timeDataColumns[i].split(" ").splice(-1)[0];
                        axesFields.push(column);
                        storeFields.push({name: column, convert: zeroFn});
                        reportDataColumns.push({
                            dataIndex: column,
                            header: column,
                            width: reportEntry.timeDataColumns.length>2? 60:90
                        });
                        series.push({
                            type: 'line',
                            axis: 'left',
                            title: column,
                            xField: 'time_trunc',
                            yField: column,
                            smooth: true,
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
                                    this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " +i18n._(reportEntry.units));
                                }
                            }
                        });
                    }
                    
                    dataStore = Ext.create('Ext.data.JsonStore', {
                        fields: storeFields,
                        data: data
                    });
                    chart = {
                        xtype: 'cartesian',
                        name: "chart",
                        store: dataStore,
                        theme: 'green-gradients',
                        border: false,
                        width: '100%',
                        height: '100%',
                        insetPadding: {top: 50, left: 10, right: 10, bottom: 10},
                        legend: {
                            docked: 'bottom'
                        },
                        tbar: ['->', {
                            xtype: 'button',
                            text: i18n._("Switch to Bar Chart"),
                            handler: Ext.bind(function() {
                                this.reportEntry.timeStyle = 'BAR';
                                this.loadReport(this.reportEntry);
                            }, this)
                        }, {
                            xtype: 'button',
                            hidden: reportEntry.timeDataColumns.length <= 1,
                            text: i18n._("Switch to Overlapped Bar Chart"),
                            handler: Ext.bind(function() {
                                this.reportEntry.timeStyle = 'BAR_OVERLAP';
                                this.loadReport(this.reportEntry);
                            }, this)
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
                        }],
                        interactions: ['itemhighlight'],
                        axes: [{
                            type: 'numeric',
                            fields: axesFields,
                            position: 'left',
                            grid: true,
                            minimum: 0,
                            renderer: function (v) {
                                return (reportEntry.units == "bytes") ? Ung.Util.bytesRenderer(v) : v + " " + i18n._(reportEntry.units);
                            }
                        }, {
                            type: 'category',
                            fields: 'time_trunc',
                            position: 'bottom',
                            grid: true,
                            label: {
                                rotate: {
                                    degrees: -45
                                }
                            }
                        }],
                        series: series
                    };
                }


                if ( reportEntry.colors != null && reportEntry.colors.length > 0 ) {
                    chart.colors = reportEntry.colors;
                }
                this.reportDataGrid.setColumns(reportDataColumns);
                this.reportDataGrid.getStore().loadData(data);
            }
            this.chartContainer.add(chart); 
            this.setLoading(false);
        }, this), this.reportEntry, this.startDateWindow.date, this.endDateWindow.date, this.extraConditions, -1);
        if(!this.extraConditionsPanel.getCollapsed()) {
            this.extraConditionsPanel.getColumnsForTable(this.reportEntry.table);
        }
            
    },
    refreshHandler: function (forceFlush) {
        if(!this.reportEntry) {
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
        //TODO: implement export
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
    title: i18n._('Extra conditions: None'),
    collapsible: true,
    collapsed: true,
    floatable: false,
    split: true,
    autoScroll: true,
    count: 3,
    layout: { type: 'table', columns: 4 },
    getColumnsForTable: function(table) {
        if(table != null && table.length > 2) {
            Ung.Main.getReportingManagerNew().getColumnsForTable(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                var columns = [];
                for (var i=0; i< result.length; i++) {
                    columns.push({ name: result[i]});
                }
                this.columnsStore.loadData(columns);
            }, this), table);
        }
    },
    initComponent: function() {
        this.columnsStore = Ext.create('Ext.data.Store', {
            sorters: "name",
            fields: ["name"],
            data: []
        });

        var items = [{
            title: i18n._("Column"),
            padding: 0,
            width: 256
        }, {
            title: i18n._("Operator"),
            padding: 0,
            width: 106
        }, {
            title: i18n._("Value"),
            padding: 0,
            width: 406
        }, {
            title: "&nbsp;",
            width: 100
        }];
        for(var i=0; i<this.count; i++) {
            items.push.apply(items, this.generateRow(i));
        }
        this.items = items;
        
        this.tbar = [{
            text: i18n._("Add Condition"),
            tooltip: i18n._('Add New Condition'),
            iconCls: 'icon-add-row',
            handler: function() {
                this.addRow();
            },
            scope: this
        }, '->', {
            text: i18n._("Clear All"),
            tooltip: i18n._('Clear All Conditions'),
            iconCls: 'cancel-icon',
            handler: function() {
                this.clearConditions();
            },
            scope: this
        }];
        this.callParent(arguments);
    },
    generateRow: function(i) {
        return [{
            xtype: 'combo',
            width: 250,
            margin: 3,
            emptyText: i18n._("[enter column]"),
            dataIndex: "column",
            name: "column"+i,
            typeAhead: true,
            valueField: "name",
            displayField: "name",
            queryMode: 'local',
            store: this.columnsStore,
            value: "",
            listeners: {
                change: {
                    fn: function(combo, newValue, oldValue, opts) {
                        this.setConditions();
                    },
                    scope: this
                }
            }
        }, {
            xtype: 'combo',
            width: 100,
            margin: 3,
            dataIndex: "operator",
            name: "operator"+i,
            editable: false,
            valueField: "name",
            displayField: "name",
            queryMode: 'local',
            value: "=",
            disabled: true,
            store: ["=", "!=", "<>", ">", "<", ">=", "<=", "between", "like", "in", "is"],
            listeners: {
                change: {
                    fn: function(combo, newValue, oldValue, opts) {
                        this.setConditions();
                    },
                    scope: this
                }
            }
        }, {
            xtype: 'textfield',
            dataIndex: "value",
            name: "value"+i,
            width: 400,
            margin: 3,
            disabled: true,
            emptyText: i18n._("[no value]"),
            listeners: {
                change: {
                    fn: function() {
                        this.setConditions();
                    },
                    scope: this,
                    buffer: 500
                }
            }
        }, {
            xtype: 'button',
            dataIndex: "clear",
            margin: 3,
            name: "clear"+i,
            text: i18n._("Clear"),
            disabled: true,
            handler: Ext.bind(function(button) {
                button.prev("[dataIndex=column]").setRawValue("");
                button.prev("[dataIndex=operator]").setRawValue("=");
                button.prev("[dataIndex=value]").setRawValue("");
                this.setConditions();
            }, this)
        }];
    },
    addRow: function() {
      this.add(this.generateRow(this.count));
      this.count++;
    },
    clearConditions: function() {
        for(var i=0; i<this.count; i++) {
            this.down("[name=column"+i+"]").setRawValue("");
            this.down("[name=operator"+i+"]").setRawValue("=");
            this.down("[name=value"+i+"]").setRawValue("");
        }
        this.setConditions();
    },
    setConditions: function() {
        var conditions = [], columnValue, operator, value, clearBtn, isEmptyColumn;
        for(var i=0; i<this.count; i++) {
            columnValue = this.down("combo[name=column"+i+"]").getValue();
            operator = this.down("combo[name=operator"+i+"]");
            value = this.down("textfield[name=value"+i+"]");
            clearBtn = this.down("button[name=clear"+i+"]");
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
            clearBtn.setDisabled(isEmptyColumn);
            
        }
        this.parentPanel.extraConditions = (conditions.length>0)?conditions:null;
        this.setTitle((conditions.length>0)?Ext.String.format( i18n._("Extra conditions: {0}"), conditions.length):i18n._("Extra conditions: None"));
    }
});