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
                        name: "extraConditions",
                        text: i18n._('Toggle Conditions'),
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
        var me = this;
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
            var chart, reportData=[];
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

                chart = {
                    xtype: 'polar',
                    name: "chart",
                    store: Ext.create('Ext.data.JsonStore', {
                        fields: [{name: "description", convert: descriptionFn }, {name:'value'} ],
                        data: topData
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
                            this.extraConditionsPanel.expand();
                            var data = {
                                column: reportEntry.pieGroupColumn,
                                operator: "=",
                                value: record.get(reportEntry.pieGroupColumn)
                            };
                            this.extraConditionsPanel.fillCondition(data);
                        }, this)
                    }]
                }]);
                this.reportDataGrid.getStore().loadData(data);
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
                
                chart = {
                    xtype: 'cartesian',
                    name: "chart",
                    store: Ext.create('Ext.data.JsonStore', {
                        fields: storeFields,
                        data: data
                    }),
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
                        hidden: this.reportEntry.timeStyle == 'LINE',
                        text: i18n._("Switch to Line Chart"),
                        handler: Ext.bind(function() {
                            this.reportEntry.timeStyle = 'LINE';
                            this.loadReport(this.reportEntry);
                        }, this)
                    }, {
                        xtype: 'button',
                        hidden: this.reportEntry.timeStyle == 'BAR',
                        text: i18n._("Switch to Bar Chart"),
                        handler: Ext.bind(function() {
                            this.reportEntry.timeStyle = 'BAR';
                            this.loadReport(this.reportEntry);
                        }, this)
                    }, {
                        xtype: 'button',
                        hidden: this.reportEntry.timeStyle == 'BAR_OVERLAP' || reportEntry.timeDataColumns.length <= 1,
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
                        type: (reportEntry.timeStyle == 'LINE') ? 'numeric' : 'numeric3d',
                        fields: axesFields,
                        position: 'left',
                        grid: true,
                        minimum: 0,
                        renderer: function (v) {
                            return (reportEntry.units == "bytes") ? Ung.Util.bytesRenderer(v) : v + " " + i18n._(reportEntry.units);
                        }
                    }, {
                        type: (reportEntry.timeStyle == 'LINE') ? 'category' : 'category3d',
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
                
                if(reportEntry.timeStyle == 'BAR') {
                    chart.series = [{
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
                                this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.field) + " " +i18n._(reportEntry.units) + legendHint);
                            }
                        }
                    }];
                } else if (reportEntry.timeStyle == 'BAR_OVERLAP') {
                    for(i=0; i<axesFields.length; i++) {
                        series.push({
                            type: 'bar3d',
                            axis: 'left',
                            title: axesFields[i],
                            xField: 'time_trunc',
                            yField: axesFields[i],
                            style: {
                                opacity: 0.70,
                                lineWidth: (i+1)*5
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
                } else if (reportEntry.timeStyle == 'LINE') {
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
    defaultCount: 3,
    autoScroll: true,
    layout: { type: 'vbox'},
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
                displayField: "name",
                queryMode: 'local',
                store: this.columnsStore,
                value: data.column,
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
                            this.setConditions();
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
                        buffer: 500
                    }
                }
            }, {
                xtype: 'button',
                name: "clear",
                text: i18n._("Clear"),
                disabled: Ext.isEmpty(data.column),
                handler: Ext.bind(function(button) {
                    this.bulkOperation = true;
                    button.prev("[dataIndex=column]").setValue("");
                    button.prev("[dataIndex=operator]").setValue("=");
                    button.prev("[dataIndex=value]").setValue("");
                    this.bulkOperation = false;
                    this.setConditions();
                }, this)
            }, {
                xtype: 'button',
                name: "delete",
                text: i18n._("Delete"),
                handler: Ext.bind(function(button) {
                    this.remove(button.up("container"));
                    this.setConditions();
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
    clearConditions: function() {
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
        this.setConditions();
    },
    setConditions: function() {
        if(this.bulkOperation) {
            return;
        }
        var conditions = [], columnValue, operator, value, clearBtn, isEmptyColumn;
        Ext.Array.each(this.query("container[name=condition]"), function(item, index, len) {
            columnValue = item.down("[dataIndex=column]").getValue();
            operator = item.down("[dataIndex=operator]");
            value = item.down("[dataIndex=value]");
            clearBtn = item.down("button[name=clear]");
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
        });

        this.parentPanel.extraConditions = (conditions.length>0)?conditions:null;
        this.setTitle((conditions.length>0)?Ext.String.format( i18n._("Extra conditions: {0}"), conditions.length):i18n._("Extra conditions: None"));
    }
});