Ext.define('Ung.panel.Reports', {
    extend: 'Ext.panel.Panel',

    name: 'panelReports',
    autoRefreshInterval: 10, //In Seconds
    layout: { type: 'border'},
    extraConditions: null,
    reportsManager: null,
    hasEntriesSection: true,
    entry: null,
    beforeDestroy: function() {
        Ext.destroy(this.subCmps);
        this.callParent(arguments);
    },
    initComponent: function() {
        this.subCmps = [];
        if(Ung.Main.webuiMode) {
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
            rpc.reportsManagerNew = Ung.Main.getReportsManagerNew();
        }
        
        this.filterFeature = Ext.create('Ung.grid.feature.GlobalFilter', {});
        this.items = [{
            region: 'east',
            title: i18n._("Current Data"),
            width: 330,
            split: true,
            collapsible: true,
            collapsed: false,
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
                handler: Ext.bind(this.exportReportDataHandler, this)
            }]
        }, {
            region: 'center',
            layout: {type: 'border'},
            items: [{
                region: 'center',
                xtype: "panel",
                name: 'cardsContainer',
                layout: 'card',
                items: [{
                    xtype: 'container',
                    itemId: 'chartContainer',
                    name: 'chartContainer',
                    layout: 'fit',
                    html: ""
                }, {
                    region: 'center',
                    xtype: 'grid',
                    itemId: 'gridEvents',
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
                        }, '->',{
                            xtype: 'button',
                            text: i18n._('Export'),
                            name: "Export",
                            tooltip: i18n._('Export Events to File'),
                            iconCls: 'icon-export',
                            handler: Ext.bind(this.exportEventsHandler, this)
                        }]
                    }] 
                }],
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'bottom',
                    items: [{
                        xtype: 'combo',
                        width: 100,
                        name: "limitSelector",
                        editable: false,
                        valueField: "value",
                        displayField: "name",
                        queryMode: 'local',
                        value: 1000,
                        store: Ext.create('Ext.data.Store', {
                            fields: ["value", "name"],
                            data: [{value: 1000, name: "1000 " + i18n._('Events')}, {value: 10000, name: "10000 " + i18n._('Events')}, {value: 50000, name: "50000 " + i18n._('Events')}]
                        })
                    }, {
                        xtype: 'button',
                        name: 'startDateButton',
                        text: i18n._('One day ago'),
                        initialLabel:  i18n._('One day ago'),
                        width: 132,
                        tooltip: i18n._('Select Start date and time'),
                        handler: Ext.bind(function(button) {
                            this.startDateWindow.show();
                        }, this)
                    },{
                        xtype: 'tbtext',
                        text: '-'
                    }, {
                        xtype: 'button',
                        name: 'endDateButton',
                        text: i18n._('Present'),
                        initialLabel:  i18n._('Present'),
                        width: 132,
                        tooltip: i18n._('Select End date and time'),
                        handler: Ext.bind(function(button) {
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
                parentPanel: this
            })]
        }];

        if(this.hasEntriesSection) {
            this.items.push(this.buildEntriesSection());
        }

        this.callParent(arguments);
        
        this.cardsContainer = this.down("container[name=cardsContainer]");
        
        this.chartContainer = this.down("container[name=chartContainer]");
        this.reportDataGrid = this.down("grid[name=reportDataGrid]");

        this.gridEvents = this.down("grid[name=gridEvents]");
        this.searchField=this.down('textfield[name=searchField]');
        this.caseSensitive = this.down('checkbox[name=caseSensitive]');
        this.limitSelector = this.down("combo[name=limitSelector]");
        
        this.startDateWindow = Ext.create('Ung.window.SelectDateTime', {
            title: i18n._('Start date and time'),
            dateTimeEmptyText: i18n._('start date and time'),
            buttonObj: this.down("button[name=startDateButton]")
        });
        this.endDateWindow = Ext.create('Ung.window.SelectDateTime', {
            title: i18n._('End date and time'),
            dateTimeEmptyText: i18n._('end date and time'),
            buttonObj: this.down("button[name=endDateButton]")
        });
        this.subCmps.push(this.startDateWindow);
        this.subCmps.push(this.endDateWindow);

        this.pieLegendHint="<br/>"+i18n._('Hint: Click this label on the legend to hide this slice');
        this.cartesianLegendHint="<br/>"+i18n._('Hint: Click this label on the legend to hide this series');
        
        if(this.category) {
            this.loadEntries();
        }
    },
    buildEntriesSection: function () {
        var entriesSection = {
            region: 'west',
            layout: {type: 'vbox', align: 'stretch'},
            width: 250,
            split: true,
            collapsible: true,
            collapsed: false,
            floatable: false,
            items:[{
                name: 'reportEntriesGrid',
                title: i18n._("Select Report"),
                xtype: 'grid',
                border: false,
                margin: '0 0 10 0',
                flex: 5,
                hideHeaders: true,
                store:  Ext.create('Ext.data.Store', {
                    fields: ["title"],
                    data: []
                }),
                reserveScrollbar: true,
                columns: [{
                    dataIndex: 'title',
                    flex: 1,
                    renderer: function( value, metaData, record, rowIdx, colIdx, store ) {
                        var description = record.get("description");
                        if(description) {
                            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( i18n._(description) ) + '"';
                        }
                        return i18n._(value);
                    }
                }],
                selModel: {
                    selType: 'rowmodel',
                    listeners: {
                        select: Ext.bind(function( rowModel, record, rowIndex, eOpts ) {
                            this.loadReportEntry(Ext.clone(record.getData()));
                            this.eventEntriesGrid.getSelectionModel().deselectAll();
                        }, this)
                    }
                }
            }, {
                name: 'eventEntriesGrid',
                xtype: 'grid',
                title: i18n._("Select Events"),
                border: false,
                flex: 4,
                hideHeaders: true,
                store:  Ext.create('Ext.data.Store', {
                    sorters: "displayOrder",
                    fields: ["title", "displayOrder"],
                    data: []
                }),
                reserveScrollbar: true,
                columns: [{
                    dataIndex: 'title',
                    flex: 1,
                    renderer: function( value, metaData, record, rowIdx, colIdx, store ) {
                        var description = record.get("description");
                        if(description) {
                            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( i18n._(description) ) + '"';
                        }
                        return i18n._(value);
                    }
                }],
                selModel: {
                    selType: 'rowmodel',
                    listeners: {
                        select: Ext.bind(function( rowModel, record, rowIndex, eOpts ) {
                            this.loadEventEntry(Ext.clone(record.getData()));
                            this.reportEntriesGrid.getSelectionModel().deselectAll();
                        }, this)
                    }
                }
            }]
        };
        return entriesSection;
    },
    setCategory: function(category) {
        this.category = category;
        var selectInitialEntrySemaphore = 2;
        var loadEntriesHandler = Ext.bind(function() {
            selectInitialEntrySemaphore--;
            if(selectInitialEntrySemaphore==0) {
                this.selectInitialEntry();
            }
        }, this);
        this.loadEntries(loadEntriesHandler);
        
    },
    loadEntries: function(handler) {
        this.loadReportEntries(null, handler);
        this.loadEventEntries(handler);
    },
    loadReportEntries: function(initialEntryId, handler) {
        if(!this.reportEntriesGrid) {
            this.reportEntriesGrid = this.down("grid[name=reportEntriesGrid]");
        }
        rpc.reportsManagerNew.getReportEntries(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var reportEntries = [];
            this.initialEntryIndex = null;
            var entry;
            for(var i=0; i<result.list.length; i++) {
                entry = result.list[i];
                if(entry.enabled) {
                    reportEntries.push(entry);
                    if(initialEntryId && entry.uniqueId == initialEntryId) {
                        this.initialEntryIndex = i;
                    }
                    if(this.initialEntryIndex==null && entry.type!="TEXT") {
                        this.initialEntryIndex = i;
                    }
                }
            }
            if(this.initialEntryIndex == null && reportEntries.length>0) {
                this.initialEntryIndex = 0;
            }
            this.reportEntriesGrid.getStore().loadData(reportEntries);
            this.reportEntriesGrid.setHidden(reportEntries.length == 0);
            if(initialEntryId && this.initialEntryIndex) {
                this.reportEntriesGrid.getSelectionModel().select(this.initialEntryIndex);
            }
            if(Ext.isFunction(handler)) {
                handler();
            }
        }, this), this.category);
    },
    loadEventEntries: function(handler) {
        if(!this.eventEntriesGrid) {
            this.eventEntriesGrid = this.down("grid[name=eventEntriesGrid]");
        }
        rpc.reportsManagerNew.getEventEntries(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            this.eventEntriesGrid.getStore().loadData(result.list);
            this.eventEntriesGrid.setHidden(result.list.length == 0);
            if(Ext.isFunction(handler)) {
                handler();
            }
        }, this), this.category);
    },
    
    loadReportData: function(data) {
        var i, column;
        if(!this.entry || !this.chartContainer || !this.chartContainer.isVisible()) {
            return;
        }
        var chart = this.chartContainer.down("[name=chart]");
        if(!chart) {
            return;
        }
        if(this.entry.type == 'TEXT') {
            var infos=[], reportData=[];
            if(data.length>0 && this.entry.textColumns!=null) {
                var textColumns=[], value;
                for(i=0; i<this.entry.textColumns.length; i++) {
                    column = this.entry.textColumns[i].split(" ").splice(-1)[0];
                    value = Ext.isEmpty(data[0][column])? 0 : data[0][column];
                    infos.push(value);
                    reportData.push({data: column, value: value});
                }
            }
            
            var sprite = chart.getSurface().get("infos");
            sprite.setAttributes({text:Ext.String.format.apply(Ext.String.format, [i18n._(this.entry.textString)].concat(infos))}, true);
            chart.renderFrame();
            this.reportDataGrid.getStore().loadData(reportData);
        } else if(this.entry.type == 'PIE_GRAPH') {
            var topData = data;
            if(this.entry.pieNumSlices && data.length>this.entry.pieNumSlices) {
                topData = [];
                var others = {value:0};
                others[this.entry.pieGroupColumn] = i18n._("Others");
                for(i=0; i<data.length; i++) {
                    if(i < this.entry.pieNumSlices) {
                        topData.push(data[i]);
                    } else {
                        others.value+=data[i].value;
                    }
                }
                others.value = Math.round(others.value*10)/10;
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
        } else if(this.entry.type == 'TIME_GRAPH') {
            chart.getStore().loadData(data);
            this.reportDataGrid.getStore().loadData(data);
        }
    },
    loadReportEntry: function(entry) {
        var me = this;
        this.entry = entry;
        if(this.autoRefreshEnabled) {
            this.stopAutoRefresh(true);
        }
        this.cardsContainer.setActiveItem("chartContainer");
        this.reportDataGrid.show();
        this.limitSelector.hide();
        
        this.chartContainer.removeAll();
        this.setLoading(i18n._('Loading report... '));
        
        var i, column;
        
        var data = [];
        var tbar = [{
            xtype: 'button',
            text: i18n._('Customize'),
            hidden: !Ung.Main.webuiMode,
            name: "edit",
            tooltip: i18n._('Advanced report customization'),
            iconCls: 'icon-edit',
            handler:function () {
                this.customizeReport();
            },
            scope: this
        }, '->' , {
            xtype: 'button',
            text: i18n._('View Events'),
            name: "edit",
            tooltip: i18n._('View events for this report'),
            iconCls: 'icon-edit',
            handler:Ext.bind(this.viewEventsForReport, this)
        }, {
            xtype: 'button',
            iconCls: 'icon-export',
            text: i18n._("Download"),
            handler: Ext.bind(this.downloadChart, this)
        }]; 
        var chart, reportData=[];
        this.reportDataGrid.getStore().loadData([]);
        if(entry.type == 'TEXT') {
            chart = {
                xtype: 'draw',
                name: "chart",
                border: false,
                width: '100%',
                height: '100%',
                tbar: tbar,
                sprites: [{
                    type: 'text',
                    text: i18n._(entry.title),
                    fontSize: 18,
                    width: 100,
                    height: 30,
                    x: 10, // the sprite x position
                    y: 22  // the sprite y position
                }, {
                    type: 'text',
                    text: i18n._(entry.description),
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
        } else if(entry.type == 'PIE_GRAPH') {
            var descriptionFn = function(val, record) {
                var title = (record.get(entry.pieGroupColumn)==null)?i18n._("none") : record.get(entry.pieGroupColumn);
                var value = (entry.units == "bytes") ? Ung.Util.bytesRenderer(record.get("value")) : record.get("value") + " " + i18n._(entry.units);
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
                tbar: tbar,
                sprites: [{
                    type: 'text',
                    text: i18n._(entry.title),
                    fontSize: 18,
                    width: 100,
                    height: 30,
                    x: 10, // the sprite x position
                    y: 22  // the sprite y position
                }, {
                    type: 'text',
                    text: i18n._(entry.description),
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

            this.reportDataGrid.setColumns([{
                dataIndex: entry.pieGroupColumn,
                header: entry.pieGroupColumn,
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
                            column: entry.pieGroupColumn,
                            operator: "=",
                            value: record.get(entry.pieGroupColumn)
                        };
                        this.windowAddCondition.setCondition(data);
                    }, this)
                }]
            }]);
        } else if(entry.type == 'TIME_GRAPH') {
            var axesFields = [], series=[];
            var legendHint = (entry.timeDataColumns.length > 1) ? this.cartesianLegendHint : "";
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
                flex: entry.timeDataColumns.length>2? 0:1
            }];
            for(i=0; i<entry.timeDataColumns.length; i++) {
                column = entry.timeDataColumns[i].split(" ").splice(-1)[0];
                axesFields.push(column);
                storeFields.push({name: column, convert: zeroFn, type: 'integer'});
                reportDataColumns.push({
                    dataIndex: column,
                    header: column,
                    width: entry.timeDataColumns.length>2 ? 60 : 90
                });
            }
            if(!entry.timeStyle) {
                entry.timeStyle = "LINE";
            }
            if(entry.timeStyle.indexOf('OVERLAPPED') != -1  && entry.timeDataColumns.length <= 1){
                entry.timeStyle = entry.timeStyle.replace("_OVERLAPPED", "");
            }
            var timeStyleButtons = [], timeStyle, timeStyles = [
                { name: 'LINE', iconCls: 'icon-line-chart', text: i18n._("Line"), tooltip: i18n._("Switch to Line Chart") },
                { name: 'AREA', iconCls: 'icon-area-chart', text: i18n._("Area"), tooltip: i18n._("Switch to Area Chart") },
                { name: 'BAR_3D', iconCls: 'icon-bar3d-chart', text: i18n._("Bar 3D"), tooltip: i18n._("Switch to Bar 3D Chart") },
                { name: 'BAR_3D_OVERLAPPED', iconCls: 'icon-bar3d-overlapped-chart', text: i18n._("Bar 3D Overlapped"), tooltip: i18n._("Switch to Bar 3D Overlapped Chart") },
                { name: 'BAR', iconCls: 'icon-bar-chart', text: i18n._("Bar"), tooltip: i18n._("Switch to Bar Chart") },
                { name: 'BAR_OVERLAPPED', iconCls: 'icon-bar-overlapped-chart', text: i18n._("Bar Overlapped"), tooltip: i18n._("Switch to Bar Overlapped Chart") }
            ];
            
            for(i=0; i<timeStyles.length; i++) {
                timeStyle = timeStyles[i];
                timeStyleButtons.push({
                    xtype: 'button',
                    pressed: entry.timeStyle == timeStyle.name,
                    hidden: (timeStyle.name.indexOf('OVERLAPPED') != -1 ) && (entry.timeDataColumns.length <= 1),
                    name: timeStyle.name,
                    iconCls: timeStyle.iconCls,
                    text: timeStyle.text,
                    tooltip: timeStyle.tooltip,
                    handler: Ext.bind(function(button) {
                        entry.timeStyle = button.name;
                        this.loadReportEntry(entry);
                    }, this)
                });
            }
            timeStyleButtons.push("-");
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
                tbar: timeStyleButtons.concat(tbar),
                sprites: [{
                    type: 'text',
                    text: i18n._(entry.title),
                    fontSize: 18,
                    width: 100,
                    height: 30,
                    x: 10, // the sprite x position
                    y: 22  // the sprite y position
                }, {
                    type: 'text',
                    text: i18n._(entry.description),
                    fontSize: 12,
                    x: 10,
                    y: 40
                }],
                interactions: ['itemhighlight'],
                axes: [{
                    type: (entry.timeStyle.indexOf('BAR_3D')!=-1) ? 'numeric3d' : 'numeric',
                    fields: axesFields,
                    position: 'left',
                    grid: true,
                    minimum: 0,
                    renderer: function (v) {
                        var significantValue = Ung.panel.Reports.significantFigures(v, 3);
                        return (entry.units == "bytes") ? Ung.Util.bytesRenderer(significantValue) : significantValue + " " + i18n._(entry.units);
                    }
                }, {
                    type: (entry.timeStyle.indexOf('BAR_3D')!=-1) ? 'category3d' : 'category',
                    fields: 'time_trunc',
                    position: 'bottom',
                    grid: true,
                    label: {
                        'fontSize': '11px',
                        'text-anchor': 'right',
                        x: 20,
                        rotate: {
                            degrees: -45
                        }
                    }
                }]
            };

            if (entry.timeStyle == 'LINE') {
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
                                this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " +i18n._(entry.units) + legendHint);
                            }
                        }
                    });
                }
                chart.series = series;
            } else if (entry.timeStyle == 'AREA') {
                for(i=0; i<axesFields.length; i++) {
                    series.push({
                        type: 'area',
                        axis: 'left',
                        title: axesFields[i],
                        xField: 'time_trunc',
                        yField: axesFields[i],
                        style: {
                            opacity: 0.60,
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
                                this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " +i18n._(entry.units) + legendHint);
                            }
                        }
                    });
                }
                chart.series = series;
            } else if(entry.timeStyle.indexOf('OVERLAPPED') != -1) {
                for(i=0; i<axesFields.length; i++) {
                    series.push({
                        type: (entry.timeStyle.indexOf('BAR_3D')!=-1)?'bar3d': 'bar',
                        axis: 'left',
                        title: axesFields[i],
                        xField: 'time_trunc',
                        yField: axesFields[i],
                        style: (entry.timeStyle.indexOf('BAR_3D') != -1)? { opacity: 0.70, lineWidth: 1+5*i } : {  opacity: 0.60,  maxBarWidth: Math.max(40-2*i, 2) },
                        tooltip: {
                            trackMouse: true,
                            style: 'background: #fff',
                            renderer: function(storeItem, item) {
                                var title = item.series.getTitle();
                                this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " +i18n._(entry.units) + legendHint);
                            }
                        }
                    });
                }
                chart.series = series;
            } else if((entry.timeStyle.indexOf('BAR') != -1 )) {
                chart.series = [{
                    type: (entry.timeStyle.indexOf('BAR_3D')!=-1)?'bar3d': 'bar',
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
                            this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.field) + " " +i18n._(entry.units) + legendHint);
                        }
                    }
                }];
            }
            this.reportDataGrid.setColumns(reportDataColumns);
        } else if(entry.type == 'TIME_GRAPH_DYNAMIC') { 
            chart = {
                    xtype: 'draw',
                    name: "chart",
                    border: false,
                    width: '100%',
                    height: '100%',
                    tbar: tbar,
                    sprites: [{
                        type: 'text',
                        text: i18n._(entry.title),
                        fontSize: 18,
                        width: 100,
                        height: 30,
                        x: 10, // the sprite x position
                        y: 22  // the sprite y position
                    }, {
                        type: 'text',
                        text: i18n._(entry.description),
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
                this.reportDataGrid.setColumns([]);
        }
        
        if(chart.xtype!= 'draw') {
            if ( entry.colors != null && entry.colors.length > 0 ) {
                chart.colors = entry.colors;
            } else {
                chart.colors = ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'];
            }
        }
        
        this.chartContainer.add(chart);
        rpc.reportsManagerNew.getDataForReportEntry(Ext.bind(function(result, exception) {
            this.setLoading(false);
            if(Ung.Util.handleException(exception)) return;
            this.loadReportData(result.list);
        }, this), entry, this.startDateWindow.date, this.endDateWindow.date, this.extraConditions, -1);
        Ung.TableConfig.getColumnsForTable(entry.table, this.extraConditionsPanel.columnsStore);
    },
    loadEventEntry: function(entry) {
        var i, col, sortType, config;
        this.entry = entry;
        if(this.autoRefreshEnabled) {
            this.stopAutoRefresh(true);
        }
        if(!entry.defaultColumns) {
            entry.defaultColumns = [];
        }
        this.cardsContainer.setActiveItem("gridEvents");
        this.reportDataGrid.hide();
        this.limitSelector.show();
        
        this.gridEvents.setTitle(entry.title);
        var tableConfig = Ext.clone(Ung.TableConfig.getConfig(entry.table));
        if(!tableConfig) {
            console.log("Warning: table '"+entry.table+"' is not defined");
            tableConfig = {
                fields: [],
                columns: []
            };
        } else {
            var columnsNames = {};
            for(i=0; i< tableConfig.columns.length; i++) {
                col = tableConfig.columns[i].dataIndex;
                columnsNames[col]=true;
                col = tableConfig.columns[i];
                if((entry.defaultColumns.length>0) && (entry.defaultColumns.indexOf(col.dataIndex) < 0)) {
                    col.hidden = true;
                }
            }
            for(i=0; i<entry.defaultColumns.length; i++) {
                col = entry.defaultColumns[i];
                if(!columnsNames[col]) {
                    console.log("Warning: column '"+col+"' is not defined in the tableConfig for "+entry.table);
                }
            }
        }
        var store = Ext.create('Ext.data.Store', {
            fields: tableConfig.fields,
            data: [],
            proxy: {
                type: 'memory',
                reader: {
                    type: 'json'
                }
            }
        });
        this.gridEvents.reconfigure(store, tableConfig.columns);
        this.gridEvents.getStore().addFilter(this.filterFeature.globalFilter);
        this.refreshHandler();
        Ung.TableConfig.getColumnsForTable(entry.table, this.extraConditionsPanel.columnsStore);
    },
    refreshHandler: function () {
        if(this.autoRefreshEnabled) {
            return;
        }
        this.refresheEntry();
    },
    autoRefresh: function() {
        if(!this.autoRefreshEnabled) {
            return;
        }
        this.refresheEntry();
    },
    refresheEntry: function() {
        if(!this.entry) {
            return;
        }
        if(Ung.panel.Reports.isEvent(this.entry)) {
            this.refreshEvents();
        } else {
            this.refreshReportData();
        }
    },
    refreshReportData: function() {
        if(!this.entry) {
            return;
        }
        if(!this.autoRefreshEnabled) { this.setLoading(i18n._('Refreshing report... ')); }
        rpc.reportsManagerNew.getDataForReportEntry(Ext.bind(function(result, exception) {
            this.setLoading(false);
            if(Ung.Util.handleException(exception)) return;
            this.loadReportData(result.list);
            if(this!=null && this.rendered && this.autoRefreshEnabled) {
                Ext.Function.defer(this.autoRefresh, this.autoRefreshInterval*1000, this);
            }
        }, this), this.entry, this.startDateWindow.date, this.endDateWindow.date, this.extraConditions, -1);
    },
    refreshEvents: function() {
        if(!this.entry) {
            return;
        }
        var limit = this.limitSelector.getValue();
            if(!this.autoRefreshEnabled) { this.setLoading(i18n._('Querying Database...')); }
            rpc.reportsManagerNew.getEventsForDateRangeResultSet(Ext.bind(function(result, exception) {
                this.setLoading(false);
                if(Ung.Util.handleException(exception)) return;
                this.loadResultSet(result);
            }, this), this.entry, this.extraConditions, limit, this.startDateWindow.date, this.endDateWindow.date);
    },
    autoRefreshEnabled: false,
    startAutoRefresh: function(setButton) {
        if(!this.entry) {
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
                (property=="spam_blocker_action")?'P':
            property+"_"+(i*index)+"_"+Math.floor((Math.random()*10));
        }
        return rec;
    },
    loadNextChunkCallback: function(result, exception) {
        if(Ung.Util.handleException(exception)) return;
        var newEvents = result;
        // If we got results append them to the current events list, and make another call for more
        if ( newEvents != null && newEvents.list != null && newEvents.list.length != 0 ) {
            this.events.push.apply( this.events, newEvents.list );
            if(!this.autoRefreshEnabled) { this.setLoading(i18n._('Fetching Events...') + ' (' + this.events.length + ')'); }
            this.reader.getNextChunk(Ext.bind(this.loadNextChunkCallback, this), 1000);
            return;
        }
        // If we got here, then we either reached the end of the resultSet or ran out of room display the results
        if (this.gridEvents!=null && this.gridEvents.getStore() != null) {
            this.gridEvents.getStore().getProxy().setData(this.events);
            this.gridEvents.getStore().load();
        }
        this.setLoading(false);
        
        if(this!=null && this.rendered && this.autoRefreshEnabled) {
            Ext.Function.defer(this.autoRefresh, this.autoRefreshInterval*1000, this);
        }
    },
    // Refresh the events list
    loadResultSet: function(result) {
        this.events = [];

        if( testMode ) {
            var emptyRec={};
            var length = Math.floor((Math.random()*5000));
            var fields = this.gridEvents.getStore().getModel().getFields();
            for(var i=0; i<length; i++) {
                this.events.push(this.getTestRecord(i, fields));
            }
        }

        this.reader = result;
        if(this.reader) {
            if(!this.autoRefreshEnabled) { this.setLoading(i18n._('Fetching Events...')); }
            this.reader.getNextChunk(Ext.bind(this.loadNextChunkCallback, this), 1000);
        } else {
            this.loadNextChunkCallback(null);
        }
    },
    getColumnList: function() {
        var columns= this.gridEvents.getColumns(), columnList = "";
        for (var i=0; i<columns.length ; i++) {
            if (i !== 0) {
                columnList += ",";
            }
            if (columns[i].dataIndex != null) {
                columnList += columns[i].dataIndex;
            }
        }
        return columnList;
    },
    exportReportDataHandler: function() {
        if(!this.entry) {
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
        var fileName = this.entry.title.trim().replace(/ /g,"_")+".csv";
        Ung.Util.download(content, fileName, 'text/csv');
    },
    downloadChart: function() {
        if(!this.entry) {
            return;
        }
        var chart = this.chartContainer.down("[name=chart]");
        if(!chart) {
            return;
        }
        var fileName = this.entry.title.trim().replace(/ /g,"_")+".png";
        Ext.MessageBox.wait(i18n._("Downloading Chart..."), i18n._("Please wait"));
        var downloadForm = document.getElementById('downloadForm');
        downloadForm["type"].value="imageDownload";
        downloadForm["arg1"].value=fileName;
        downloadForm["arg2"].value=chart.getImage().data;
        downloadForm["arg3"].value="";
        downloadForm["arg4"].value="";
        downloadForm["arg5"].value="";
        downloadForm["arg6"].value="";
        downloadForm.submit();
        Ext.MessageBox.hide();
        /*
        if (Ext.os.is.Desktop) {
            chart.download({
                filename: fileName
            });
        } else {
            chart.preview();
        } */
    },
    exportEventsHandler: function() {
        if(!this.entry) {
            return;
        }
        var startDate = this.startDateWindow.date;
        var endDate = this.endDateWindow.date;
        
        Ext.MessageBox.wait(i18n._("Exporting Events..."), i18n._("Please wait"));
        var name=this.entry.title.trim().replace(/ /g,"_");
        var downloadForm = document.getElementById('downloadForm');
        downloadForm["type"].value="eventLogExport";
        downloadForm["arg1"].value=name;
        downloadForm["arg2"].value=Ext.encode(this.entry);
        downloadForm["arg3"].value=Ext.encode(this.extraConditions);
        downloadForm["arg4"].value=this.getColumnList();
        downloadForm["arg5"].value=startDate?startDate.getTime():-1;
        downloadForm["arg6"].value=endDate?endDate.getTime():-1;
        downloadForm.submit();
        Ext.MessageBox.hide();
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
        if(!this.entry) {
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
                    me.entry = this.record.getData();
                    me.loadReportEntry(me.entry);
                }
            });
            this.subCmps.push(this.winReportEditor);
        }
        var record = Ext.create('Ext.data.Model', this.entry);
        this.winReportEditor.populate(record);
        this.winReportEditor.show();
    },
    viewEventsForReport: function() {
        if(!this.entry) {
            return;
        }
        
        var entry = {
            javaClass: "com.untangle.node.reports.EventEntry",
            category: this.entry.category,
            conditions: this.entry.conditions,
            displayOrder: 1,
            table: this.entry.table,
            title: Ext.String.format(i18n._('Events for report: {0}'), this.entry.title)
        };
        this.loadEventEntry(entry);
        if(this.reportEntriesGrid){
            this.reportEntriesGrid.getSelectionModel().deselectAll();
        }
    },
    isDirty: function() {
        return false;
    },
    selectInitialEntry: function() {
        if(this.reportEntriesGrid && this.reportEntriesGrid.getStore().getCount() > 0) {
            this.reportEntriesGrid.getSelectionModel().select(this.initialEntryIndex);
        } else if (this.eventEntriesGrid && this.eventEntriesGrid.getStore().getCount() > 0) {
            this.eventEntriesGrid.getSelectionModel().select(0);
        }
    },
    listeners: {
        "activate": {
            fn: function() {
                if(this.category && !this.entry) {
                    this.selectInitialEntry();
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
        isEvent: function(entry) {
            return "com.untangle.node.reports.EventEntry" == entry.javaClass;
        },
        significantFigures: function(n, sig) {
            if(isNaN(n)) {
                return n;
            }
            if(n==0) {
                return 0;
            }
            var isNegative = n<0;
            if(isNegative) n = -n;
            var mult = Math.pow(10, sig - Math.floor(Math.log(n) / Math.LN10) - 1);
            var result =  Math.round(n * mult) / mult;
            return isNegative? -result : result;
        },
        getColumnRenderer: function(columnName) {
            if(!this.columnRenderers) {
                this.columnRenderers = {
                    policy_id: function(value) {
                        var name = Ung.Main.getPolicyName(value);
                        if ( name != null )
                            return name + " (" + value + ")";
                        else
                            return value;
                    },
                    protocol: function(value) {
                        switch (value) {
                          case 0: return "HOPOPT (0)";
                          case 1: return "ICMP (1)";
                          case 2: return "IGMP (2)";
                          case 3: return "GGP (3)";
                          case 4: return "IP-in-IP (4)";
                          case 5: return "ST (5)";
                          case 6: return "TCP (6)";
                          case 7: return "CBT (7)";
                          case 8: return "EGP (8)";
                          case 9: return "IGP (9)";
                          case 10: return "BBN-RCC-MON (10)";
                          case 11: return "NVP-II (11)";
                          case 12: return "PUP (12)";
                          case 13: return "ARGUS (13)";
                          case 14: return "EMCON (14)";
                          case 15: return "XNET (15)";
                          case 16: return "CHAOS (16)";
                          case 17: return "UDP (17)";
                          case 18: return "MUX (18)";
                          case 19: return "DCN-MEAS (19)";
                          case 20: return "HMP (20)";
                          case 21: return "PRM (21)";
                          case 22: return "XNS-IDP (22)";
                          case 23: return "TRUNK-1 (23)";
                          case 24: return "TRUNK-2 (24)";
                          case 25: return "LEAF-1 (25)";
                          case 26: return "LEAF-2 (26)";
                          case 27: return "RDP (27)";
                          case 28: return "IRTP (28)";
                          case 29: return "ISO-TP4 (29)";
                          case 30: return "NETBLT (30)";
                          case 31: return "MFE-NSP (31)";
                          case 32: return "MERIT-INP (32)";
                          case 33: return "DCCP (33)";
                          case 34: return "3PC (34)";
                          case 35: return "IDPR (35)";
                          case 36: return "XTP (36)";
                          case 37: return "DDP (37)";
                          case 38: return "IDPR-CMTP (38)";
                          case 39: return "TP++ (39)";
                          case 40: return "IL (40)";
                          case 41: return "IPv6 (41)";
                          case 42: return "SDRP (42)";
                          case 43: return "IPv6-Route (43)";
                          case 44: return "IPv6-Frag (44)";
                          case 45: return "IDRP (45)";
                          case 46: return "RSVP (46)";
                          case 47: return "GRE (47)";
                          case 48: return "MHRP (48)";
                          case 49: return "BNA (49)";
                          case 50: return "ESP (50)";
                          case 51: return "AH (51)";
                          case 52: return "I-NLSP (52)";
                          case 53: return "SWIPE (53)";
                          case 54: return "NARP (54)";
                          case 55: return "MOBILE (55)";
                          case 56: return "TLSP (56)";
                          case 57: return "SKIP (57)";
                          case 58: return "IPv6-ICMP (58)";
                          case 59: return "IPv6-NoNxt (59)";
                          case 60: return "IPv6-Opts (60)";
                          case 62: return "CFTP (62)";
                          case 64: return "SAT-EXPAK (64)";
                          case 65: return "KRYPTOLAN (65)";
                          case 66: return "RVD (66)";
                          case 67: return "IPPC (67)";
                          case 69: return "SAT-MON (69)";
                          case 70: return "VISA (70)";
                          case 71: return "IPCU (71)";
                          case 72: return "CPNX (72)";
                          case 73: return "CPHB (73)";
                          case 74: return "WSN (74)";
                          case 75: return "PVP (75)";
                          case 76: return "BR-SAT-MON (76)";
                          case 77: return "SUN-ND (77)";
                          case 78: return "WB-MON (78)";
                          case 79: return "WB-EXPAK (79)";
                          case 80: return "ISO-IP (80)";
                          case 81: return "VMTP (81)";
                          case 82: return "SECURE-VMTP (82)";
                          case 83: return "VINES (83)";
                          case 84: return "TTP (84)";
                          case 84: return "IPTM (84)";
                          case 85: return "NSFNET-IGP (85)";
                          case 86: return "DGP (86)";
                          case 87: return "TCF (87)";
                          case 88: return "EIGRP (88)";
                          case 89: return "OSPF (89)";
                          case 90: return "Sprite-RPC (90)";
                          case 91: return "LARP (91)";
                          case 92: return "MTP (92)";
                          case 93: return "AX.25 (93)";
                          case 94: return "IPIP (94)";
                          case 95: return "MICP (95)";
                          case 96: return "SCC-SP (96)";
                          case 97: return "ETHERIP (97)";
                          case 98: return "ENCAP (98)";
                          case 100: return "GMTP (100)";
                          case 101: return "IFMP (101)";
                          case 102: return "PNNI (102)";
                          case 103: return "PIM (103)";
                          case 104: return "ARIS (104)";
                          case 105: return "SCPS (105)";
                          case 106: return "QNX (106)";
                          case 107: return "A/N (107)";
                          case 108: return "IPComp (108)";
                          case 109: return "SNP (109)";
                          case 110: return "Compaq-Peer (110)";
                          case 111: return "IPX-in-IP (111)";
                          case 112: return "VRRP (112)";
                          case 113: return "PGM (113)";
                          case 115: return "L2TP (115)";
                          case 116: return "DDX (116)";
                          case 117: return "IATP (117)";
                          case 118: return "STP (118)";
                          case 119: return "SRP (119)";
                          case 120: return "UTI (120)";
                          case 121: return "SMP (121)";
                          case 122: return "SM (122)";
                          case 123: return "PTP (123)";
                          case 124: return "IS-IS (124)";
                          case 125: return "FIRE (125)";
                          case 126: return "CRTP (126)";
                          case 127: return "CRUDP (127)";
                          case 128: return "SSCOPMCE (128)";
                          case 129: return "IPLT (129)";
                          case 130: return "SPS (130)";
                          case 131: return "PIPE (131)";
                          case 132: return "SCTP (132)";
                          case 133: return "FC (133)";
                          case 134: return "RSVP-E2E-IGNORE (134)";
                          case 135: return "Mobility (135)";
                          case 136: return "UDPLite (136)";
                          case 137: return "MPLS-in-IP (137)";
                          case 138: return "manet (138)";
                          case 139: return "HIP (139)";
                          case 140: return "Shim6 (140)";
                          case 141: return "WESP (141)";
                          case 142: return "ROHC (142)";
                          default: return value.toString();
                        }
                    }
                };
            }
            return this.columnRenderers[columnName];
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
            sorters: "header",
            fields: ["dataIndex", "header"],
            data: []
        });
        this.items = [];
        for(var i=0; i<this.defaultCount; i++) {
            this.items.push(this.generateRow());
        }
        var quickAddMenu = Ext.create('Ext.menu.Menu');
        var addQuickCondition = Ext.bind(function(item) {
            this.fillCondition({
                column: item.column,
                operator: "=",
                value: item.value
            });
        }, this);
        rpc.reportsManagerNew.getConditionQuickAddHints(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var column, hintMenus = [], columnItems, i, text, value;
            for(column in result) {
                var values = result[column];
                if(values.length > 0) {
                    columnItems = [];
                    columnRenderer = Ung.panel.Reports.getColumnRenderer(column);
                    for(i=0; i<values.length; i++) {
                        columnItems.push({
                            text: Ext.isFunction(columnRenderer) ? columnRenderer(values[i]) : values[i],
                            column: column,
                            value: values[i],
                            handler: addQuickCondition
                        });
                    }
                    hintMenus.push({
                        text: Ung.TableConfig.getColumnHumanReadableName(column),
                        menu: {
                            items: columnItems
                        }
                    });
                }
            }
            quickAddMenu.add(hintMenus);
            
        }, this));
        this.tbar = [{
            text: i18n._("Add Condition"),
            tooltip: i18n._('Add New Condition'),
            iconCls: 'icon-add-row',
            handler: function() {
                this.addRow();
            },
            scope: this
        }, {
            text:i18n._('Quick Add'),
            iconCls: 'icon-add-row',
            menu: quickAddMenu
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
                valueField: "dataIndex",
                displayField: "header",
                queryMode: 'local',
                store: this.columnsStore,
                value: data.column,
                listeners: {
                    change: {
                        fn: function(combo, newValue, oldValue, opts) {
                            this.setConditions(true);
                        },
                        scope: this
                    },
                    blur: {
                        fn: function(field, e) {
                            var skipReload = Ext.isEmpty(field.next("[dataIndex=value]").getValue());
                            this.setConditions(skipReload);
                        },
                        scope: this
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
                    blur: {
                        fn: function(field, e) {
                            this.setConditions();
                        },
                        scope: this
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
                    "javaClass": "com.untangle.node.reports.SqlCondition",
                    "column": columnValue,
                    "operator": operator.getValue(),
                    "value": value.getValue()
                });
            }
            operator.setDisabled(isEmptyColumn);
            value.setDisabled(isEmptyColumn);
        });
        if(!skipReload) {
            var encodedConditions = Ext.encode(conditions);
            if(this.currentConditions != encodedConditions) {
                this.currentConditions = encodedConditions;
                this.parentPanel.extraConditions = (conditions.length>0)?conditions:null;
                this.setTitle(Ext.String.format( i18n._("Conditions: {0}"), (conditions.length>0)?conditions.length:i18n._("None")));
                this.parentPanel.refreshHandler();
            }
        }

    },
    setValue: function(extraConditions) {
        var me = this, i;
        this.bulkOperation = true;
        Ext.Array.each(this.query("container[name=condition]"), function(item, index, len) {
            me.remove(item);
        });
        if(!extraConditions) {
            for(i=0; i<this.defaultCount; i++) {
                this.addRow();
            }
        } else {
            for(i=0; i<extraConditions.length; i++) {
                this.addRow(extraConditions[i]);
            }
        }
        this.bulkOperation = false;
    }
    
});

Ext.define("Ung.grid.feature.GlobalFilter", {
    extend: "Ext.grid.feature.Feature",
    useVisibleColumns: true,
    useFields: null,
    init: function (grid) {
        this.grid=grid;

        this.globalFilter = Ext.create('Ext.util.Filter', {
            regExpProtect: /\\|\/|\+|\\|\.|\[|\]|\{|\}|\?|\$|\*|\^|\|/gm,
            disabled: true,
            regExpMode: false,
            caseSensitive: false,
            regExp: null,
            stateId: 'globalFilter',
            searchFields: {},
            filterFn: function(record) {
                if(!this.regExp) {
                    return true;
                }
                var datas = record.getData(), key, val;
                for(key in this.searchFields) {
                    if(datas[key] !== undefined){
                        val = datas[key];
                        if(val == null) {
                            continue;
                        }
                        if(typeof val == 'boolean' || typeof val == 'number') {
                            val=val.toString();
                        } else if(typeof val == 'object') {
                            if(val.time != null) {
                                val = i18n.timestampFormat(val);
                            }
                        }
                        if(typeof val == 'string') {
                            if(this.regExp.test(val)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            },
            getSearchValue: function(value) {
                if (value === '' || value === '^' || value === '$') {
                    return null;
                }
                if (!this.regExpMode) {
                    value = value.replace(this.regExpProtect, function(m) {
                        return '\\' + m;
                    });
                } else {
                    try {
                        new RegExp(value);
                    } catch (error) {
                        return null;
                    }
                }
                return value;
            },
            buildSearch: function(value, caseSensitive, searchFields) {
                this.searchFields = searchFields;
                this.setCaseSensitive(caseSensitive);
                var searchValue = this.getSearchValue(value);
                this.regExp = searchValue==null? null:new RegExp(searchValue, 'g' + (caseSensitive ? '' : 'i'));
                this.setDisabled(this.regExp==null);
            }
        });
        
        this.grid.on("beforedestroy", Ext.bind(function() {
            this.grid.getStore().removeFilter(this.globalFilter);
            Ext.destroy(this.globalFilter);
        }, this));
        this.callParent(arguments);
    },
    updateGlobalFilter: function(value, caseSensitive) {
        var searchFields = {}, i, col;
        if(this.useVisibleColumns) {
            var visibleColumns = this.grid.getVisibleColumns(); 
            for(i=0; i<visibleColumns.length; i++) {
                col = visibleColumns[i];
                if(col.dataIndex) {
                    searchFields[col.dataIndex] = true;
                }
            }
        } else if(this.searchFields!=null) {
            for(i=0; i<this.searchFields.length; i++) {
                searchFields[this.searchFields[i]] = true;
            }
        }
        this.globalFilter.buildSearch(value, caseSensitive, searchFields);
        this.grid.getStore().getFilters().notify('endupdate');
    }
});

Ext.define("Ung.window.SelectDateTime", {
    extend: "Ext.window.Window",
    date: null,
    buttonObj: null,
    modal:true,
    closeAction: 'hide',
    initComponent: function() {
        this.items = [{
            xtype: 'textfield',
            name: 'dateAndTime',
            readOnly: true,
            hideLabel: true,
            width: 180,
            emptyText: this.dateTimeEmptyText
        }, {
            xtype: 'datepicker',
            name: 'date',
            handler: function(picker, date) {
                var timeValue = this.down("timefield[name=time]").getValue();
                if(timeValue != null) {
                    date.setHours(timeValue.getHours());
                    date.setMinutes(timeValue.getMinutes());
                }
                this.setDate(date);
            },
            scope: this
        }, {
            xtype: 'timefield',
            name: 'time',
            hideLabel: true,
            margin: '5px 0 0 0',
            increment: 30,
            width: 180,
            emptyText: i18n._('Time'),
            value: Ext.Date.parse('12:00 AM','h:i A'),
            listeners: {
                change: {
                    fn: function(combo, newValue, oldValue, opts) {
                        if(!this.buttonObj) {
                            return;
                        }
                        var comboValue = combo.getValue(); 
                        if (comboValue!=null) {
                            if(!this.date) {
                                var selDate=this.down("datepicker[name=date]").getValue();
                                if(!selDate) {
                                    selDate=new Date();
                                    selDate.setHours(0,0,0,0);
                                }
                                this.date = new Date(selDate.getTime());
                            }
                            this.date.setHours(comboValue.getHours());
                            this.date.setMinutes(comboValue.getMinutes());
                            this.setDate(this.date);
                        }
                    },
                    scope: this
                }
            }
        }];
        this.buttons = [{
            text: i18n._("Done"),
            handler: function() {
                this.hide();
            },
            scope: this
        }, '->', {
            name: 'Clear',
            text: i18n._("Clear Value"),
            handler: function() {
                this.setDate(null);
                this.hide();
                },
            scope: this
        }];
        this.callParent(arguments);
    },
    setDate: function(date) {
        this.date = date;
        var dateStr ="";
        var buttonLabel = null;
        if(this.date) {
            var displayTime = this.date.getTime()-i18n.timeoffset;
            dateStr = i18n.timestampFormat({time: displayTime});
            buttonLabel = i18n.timestampFormat({time: displayTime});
        }
        this.down("textfield[name=dateAndTime]").setValue(dateStr);
        if(this.buttonObj) {
            this.buttonObj.setText(buttonLabel!=null?buttonLabel:this.buttonObj.initialLabel);
        }
    }
});