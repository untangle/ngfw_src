Ext.define('Ung.panel.Reports', {
    extend: 'Ext.panel.Panel',

    name: 'panelReports',
    autoRefreshInterval: 10, //In Seconds
    layout: { type: 'border'},
    border: false,
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
            rpc.reportsManager = Ung.Main.getReportsManager();
        }
        
        this.filterFeature = Ext.create('Ung.grid.feature.GlobalFilter', {});
        this.items = [{
            region: 'east',
            title: i18n._("Current Data"),
            width: 330,
            hidden: true,
            split: true,
            collapsible: true,
            collapsed: Ung.Main.viewport.getWidth()<1200,
            floatable: true,
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
            xtype: "container",
            layout: {type: 'border'},
            items: [{
                region: 'center',
                xtype: "panel",
                name: 'cardsContainer',
                layout: 'card',
                items: [{
                    xtype: 'component',
                    itemId: 'pleaseSelectEntryContrainer',
                    name: 'pleaseSelectEntryContrainer',
                    padding: '25 10 0 20',
                    style: 'font-size: 16px;',
                    html: i18n._('Please select an entry to view.')
                }, {
                    xtype: 'container',
                    itemId: 'chartContainer',
                    name: 'chartContainer',
                    layout: 'fit',
                    items: []
                }, {
                    region: 'center',
                    xtype: 'grid',
                    itemId: 'gridEvents',
                    name:'gridEvents',
                    reserveScrollbar: true,
                    title: ".",
                    stateful: true,
                    stateId: "eventGrid",
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
                        hidden: true,
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
                    },{
                        text: i18n._('Reset View'),
                        name: "resetView",
                        hidden: true,
                        tooltip: i18n._('Restore default columns positions, widths and visibility'),
                        handler: Ext.bind(function () {
                            if(!this.entry || !Ung.panel.Reports.isEvent(this.entry)) {
                                return;
                            }
                            var gridEvents = this.down("grid[name=gridEvents]"); 
                            Ext.state.Manager.clear(gridEvents.stateId);
                            gridEvents.reconfigure(undefined, gridEvents.defaultTableConfig.columns);
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
        this.resetView = this.down("button[name=resetView]");
        
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
            itemId: 'panelEntries',
            layout: {type: 'vbox', align: 'stretch'},
            width: 250,
            split: true,
            collapsible: true,
            collapsed: Ung.Main.viewport.getWidth()<800,
            floatable: true,
            items:[{
                name: 'reportEntriesGrid',
                title: i18n._("Select Report"),
                xtype: 'grid',
                border: false,
                margin: '1 0 10 0',
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
        this.cardsContainer.setActiveItem("pleaseSelectEntryContrainer");
        this.reportDataGrid.hide();
        this.loadEntries();
    },
    loadEntries: function() {
        
        this.loadReportEntries(null);
        this.loadEventEntries();
    },
    loadReportEntries: function(initialEntryId) {
        if(!this.reportEntriesGrid) {
            this.reportEntriesGrid = this.down("grid[name=reportEntriesGrid]");
        }
        rpc.reportsManager.getReportEntries(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var reportEntries = [];
            var entry;
            for(var i=0; i<result.list.length; i++) {
                entry = result.list[i];
                if(entry.enabled) {
                    reportEntries.push(entry);
                }
            }
            this.reportEntriesGrid.getStore().loadData(reportEntries);
            this.reportEntriesGrid.setHidden(reportEntries.length == 0);
            if(initialEntryId) {
                this.reportEntriesGrid.getSelectionModel().select(initialEntryId);
            }
        }, this), this.category);
    },
    loadEventEntries: function() {
        if(!this.eventEntriesGrid) {
            this.eventEntriesGrid = this.down("grid[name=eventEntriesGrid]");
        }
        rpc.reportsManager.getEventEntries(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            this.eventEntriesGrid.getStore().loadData(result.list);
            this.eventEntriesGrid.setHidden(result.list.length == 0);
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
        } else if(this.entry.type == 'TIME_GRAPH_DYNAMIC') {
            var columnsMap = {}, columns=[], values={};
            for(i=0; i<data.length; i++) {
                for(column in data[i]) {
                    columnsMap[column]=true;
                }
            }
            for(column in columnsMap) {
                if(column!='time_trunc') {
                    columns.push(column);
                }
            }
            this.entry.timeDataColumns = columns;
            this.buildReportEntry(this.entry);
            chart = this.chartContainer.down("[name=chart]");
            chart.getStore().loadData(data);
            this.reportDataGrid.getStore().loadData(data);
        }
    },
    buildReportEntry: function(entry) {
        var me = this;
        this.entry = entry;
        this.cardsContainer.setActiveItem("chartContainer");
        this.reportDataGrid.show();
        this.limitSelector.hide();
        this.resetView.hide();
        
        this.chartContainer.removeAll();
        
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
                var value = Ung.panel.Reports.renderValue(record.get("value"), entry);
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
                        renderer: function(tooltip, storeItem, item) {
                            tooltip.setHtml(storeItem.get("description")+me.pieLegendHint);
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
                    iconCls: 'icon-row icon-filter',
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
        } else if(entry.type == 'TIME_GRAPH' || entry.type == 'TIME_GRAPH_DYNAMIC') {
            if(!entry.timeDataColumns) {
                entry.timeDataColumns = [];
            }
            var axesFields = [], axesFieldsTitles=[], series=[];
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
                header: i18n._("Timestamp"),
                width: 130,
                flex: entry.timeDataColumns.length>2? 0:1
            }];
            var seriesRenderer = null, title;
            if(!Ext.isEmpty(entry.seriesRenderer)) {
                seriesRenderer =  Ung.panel.Reports.getColumnRenderer(entry.seriesRenderer);
            }
            for(i=0; i<entry.timeDataColumns.length; i++) {
                column = entry.timeDataColumns[i].split(" ").splice(-1)[0];
                title = seriesRenderer?seriesRenderer(column):column;
                axesFields.push(column);
                axesFieldsTitles.push(title);
                storeFields.push({name: column, convert: zeroFn, type: 'integer'});
                reportDataColumns.push({
                    dataIndex: column,
                    header: title,
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
                    renderer: function (axis, v) {
                        var significantValue = Ung.panel.Reports.significantFigures(v, 3);
                        return Ung.panel.Reports.renderValue(significantValue, entry);
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
                        title: axesFieldsTitles[i],
                        xField: 'time_trunc',
                        yField: axesFields[i],
                        style: {
                            opacity: 0.90,
                            lineWidth: 3
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
                            renderer: function(tooltip, storeItem, item) {
                                var title = item.series.getTitle();
                                tooltip.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " +i18n._(entry.units) + legendHint);
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
                        title: axesFieldsTitles[i],
                        xField: 'time_trunc',
                        yField: axesFields[i],
                        style: {
                            opacity: 0.60,
                            lineWidth: 3
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
                            renderer: function(tooltip, storeItem, item) {
                                var title = item.series.getTitle();
                                tooltip.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " +i18n._(entry.units) + legendHint);
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
                        title: axesFieldsTitles[i],
                        xField: 'time_trunc',
                        yField: axesFields[i],
                        style: (entry.timeStyle.indexOf('BAR_3D') != -1)? { opacity: 0.70, lineWidth: 1+5*i } : {  opacity: 0.60,  maxBarWidth: Math.max(40-2*i, 2) },
                        tooltip: {
                            trackMouse: true,
                            style: 'background: #fff',
                            renderer: function(tooltip, storeItem, item) {
                                var title = item.series.getTitle();
                                tooltip.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " +i18n._(entry.units) + legendHint);
                            }
                        }
                    });
                }
                chart.series = series;
            } else if((entry.timeStyle.indexOf('BAR') != -1 )) {
                chart.series = [{
                    type: (entry.timeStyle.indexOf('BAR_3D')!=-1)?'bar3d': 'bar',
                    axis: 'left',
                    title: axesFieldsTitles,
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
                        renderer: function(tooltip, storeItem, item) {
                            var title = item.series.getTitle()[Ext.Array.indexOf(item.series.getYField(), item.field)];
                            tooltip.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.field) + " " +i18n._(entry.units) + legendHint);
                        }
                    }
                }];
            }
            this.reportDataGrid.setColumns(reportDataColumns);
        }
        
        if(chart.xtype!= 'draw') {
            if ( entry.colors != null && entry.colors.length > 0 ) {
                chart.colors = entry.colors;
            } else {
                chart.colors = ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'];
            }
        }
        
        this.chartContainer.add(chart);
    },
    loadReportEntry: function(entry) {
        this.setLoading(i18n._('Loading report... '));
        if(this.autoRefreshEnabled) {
            this.stopAutoRefresh(true);
        }
        this.buildReportEntry(entry);
        rpc.reportsManager.getDataForReportEntry(Ext.bind(function(result, exception) {
            this.setLoading(false);
            if(Ung.Util.handleException(exception)) return;
            this.loadReportData(result.list);
        }, this), entry, this.startDateWindow.serverDate, this.endDateWindow.serverDate, this.extraConditions, -1);
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
        this.resetView.show();
        
        this.gridEvents.setTitle(entry.title);
        this.gridEvents.stateId = "eventGrid-" + (entry.category? (entry.category.toLowerCase().replace(" ","_") + "-"):"") + entry.table;
        
        var tableConfig = Ext.clone(Ung.TableConfig.getConfig(entry.table));
        var state = Ext.state.Manager.get(this.gridEvents.stateId);
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
                if( col.stateId === undefined ){
                    col.stateId = col.dataIndex;
                }
            }
            for(i=0; i<entry.defaultColumns.length; i++) {
                col = entry.defaultColumns[i];
                if(!columnsNames[col]) {
                    console.log("Warning: column '"+col+"' is not defined in the tableConfig for "+entry.table);
                }
            }
            this.gridEvents.defaultTableConfig = tableConfig;
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
        state = Ext.state.Manager.get(this.gridEvents.stateId);
        if(state != null && state.columns != undefined){
            // Performing a state restore to a dynamic grid is very picky.
            // If you decide to revisit, see the test procedures in
            // https://bugzilla.untangle.com/show_bug.cgi?id=12594
            Ext.suspendLayouts();
            this.gridEvents.getView().getHeaderCt().purgeCache();
            this.gridEvents.applyState(state);
            this.gridEvents.updateLayout();
            Ext.Array.each(this.gridEvents.getColumns(), function(column, index) {
                if(column.hidden == true){
                    column.setVisible(true);
                    column.setVisible(false);
                }else{
                    column.setVisible(false);
                    column.setVisible(true);
                }
            });
            Ext.resumeLayouts(true);
        }

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
        rpc.reportsManager.getDataForReportEntry(Ext.bind(function(result, exception) {
            this.setLoading(false);
            if(Ung.Util.handleException(exception)) return;
            this.loadReportData(result.list);
            if(this!=null && this.rendered && this.autoRefreshEnabled) {
                Ext.Function.defer(this.autoRefresh, this.autoRefreshInterval*1000, this);
            }
        }, this), this.entry, this.startDateWindow.serverDate, this.endDateWindow.serverDate, this.extraConditions, -1);
    },
    refreshEvents: function() {
        if(!this.entry) {
            return;
        }
        var limit = this.limitSelector.getValue();
            if(!this.autoRefreshEnabled) { this.setLoading(i18n._('Querying Database...')); }
            rpc.reportsManager.getEventsForDateRangeResultSet(Ext.bind(function(result, exception) {
                this.setLoading(false);
                if(Ung.Util.handleException(exception)) return;
                this.loadResultSet(result);
            }, this), this.entry, this.extraConditions, limit, this.startDateWindow.serverDate, this.endDateWindow.serverDate);
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
    },
    exportEventsHandler: function() {
        if(!this.entry) {
            return;
        }
        var startDate = this.startDateWindow.serverDate;
        var endDate = this.endDateWindow.serverDate;
        
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
                            store: ["=", "!=", ">", "<", ">=", "<=", "like", "not like", "is","is not", "in", "not in"]
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
                grid: {
                    reconfigure: function(){}
                },
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
    listeners: {
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
        renderValue: function(value, entry) {
            var showValue;
            if(entry.units == "bytes" || entry.units == "bytes/s") {
                showValue = Ung.Util.bytesRenderer(value, entry.units == "bytes/s");
            } else {
                showValue = value + (Ext.isEmpty(entry.units)? "" : " " + i18n._(entry.units));
            }
            return showValue;
        },
        getColumnRenderer: function(columnName) {
            if(!this.columnRenderers) {
                this.columnRenderers = {
                    "policy_id": function(value) {
                        var name = Ung.Main.getPolicyName(value);
                        if ( name != null )
                            return name + " (" + value + ")";
                        else
                            return value;
                    },
                    "protocol": function(value) {
                        if(!Ung.panel.Reports.protocolMap) {
                            Ung.panel.Reports.protocolMap = {
                                0: "HOPOPT (0)",
                                1: "ICMP (1)",
                                2: "IGMP (2)",
                                3: "GGP (3)",
                                4: "IP-in-IP (4)",
                                5: "ST (5)",
                                6: "TCP (6)",
                                7: "CBT (7)",
                                8: "EGP (8)",
                                9: "IGP (9)",
                                10: "BBN-RCC-MON (10)",
                                11: "NVP-II (11)",
                                12: "PUP (12)",
                                13: "ARGUS (13)",
                                14: "EMCON (14)",
                                15: "XNET (15)",
                                16: "CHAOS (16)",
                                17: "UDP (17)",
                                18: "MUX (18)",
                                19: "DCN-MEAS (19)",
                                20: "HMP (20)",
                                21: "PRM (21)",
                                22: "XNS-IDP (22)",
                                23: "TRUNK-1 (23)",
                                24: "TRUNK-2 (24)",
                                25: "LEAF-1 (25)",
                                26: "LEAF-2 (26)",
                                27: "RDP (27)",
                                28: "IRTP (28)",
                                29: "ISO-TP4 (29)",
                                30: "NETBLT (30)",
                                31: "MFE-NSP (31)",
                                32: "MERIT-INP (32)",
                                33: "DCCP (33)",
                                34: "3PC (34)",
                                35: "IDPR (35)",
                                36: "XTP (36)",
                                37: "DDP (37)",
                                38: "IDPR-CMTP (38)",
                                39: "TP++ (39)",
                                40: "IL (40)",
                                41: "IPv6 (41)",
                                42: "SDRP (42)",
                                43: "IPv6-Route (43)",
                                44: "IPv6-Frag (44)",
                                45: "IDRP (45)",
                                46: "RSVP (46)",
                                47: "GRE (47)",
                                48: "MHRP (48)",
                                49: "BNA (49)",
                                50: "ESP (50)",
                                51: "AH (51)",
                                52: "I-NLSP (52)",
                                53: "SWIPE (53)",
                                54: "NARP (54)",
                                55: "MOBILE (55)",
                                56: "TLSP (56)",
                                57: "SKIP (57)",
                                58: "IPv6-ICMP (58)",
                                59: "IPv6-NoNxt (59)",
                                60: "IPv6-Opts (60)",
                                62: "CFTP (62)",
                                64: "SAT-EXPAK (64)",
                                65: "KRYPTOLAN (65)",
                                66: "RVD (66)",
                                67: "IPPC (67)",
                                69: "SAT-MON (69)",
                                70: "VISA (70)",
                                71: "IPCU (71)",
                                72: "CPNX (72)",
                                73: "CPHB (73)",
                                74: "WSN (74)",
                                75: "PVP (75)",
                                76: "BR-SAT-MON (76)",
                                77: "SUN-ND (77)",
                                78: "WB-MON (78)",
                                79: "WB-EXPAK (79)",
                                80: "ISO-IP (80)",
                                81: "VMTP (81)",
                                82: "SECURE-VMTP (82)",
                                83: "VINES (83)",
                                84: "TTP (84)",
                                85: "NSFNET-IGP (85)",
                                86: "DGP (86)",
                                87: "TCF (87)",
                                88: "EIGRP (88)",
                                89: "OSPF (89)",
                                90: "Sprite-RPC (90)",
                                91: "LARP (91)",
                                92: "MTP (92)",
                                93: "AX.25 (93)",
                                94: "IPIP (94)",
                                95: "MICP (95)",
                                96: "SCC-SP (96)",
                                97: "ETHERIP (97)",
                                98: "ENCAP (98)",
                                100: "GMTP (100)",
                                101: "IFMP (101)",
                                102: "PNNI (102)",
                                103: "PIM (103)",
                                104: "ARIS (104)",
                                105: "SCPS (105)",
                                106: "QNX (106)",
                                107: "A/N (107)",
                                108: "IPComp (108)",
                                109: "SNP (109)",
                                110: "Compaq-Peer (110)",
                                111: "IPX-in-IP (111)",
                                112: "VRRP (112)",
                                113: "PGM (113)",
                                115: "L2TP (115)",
                                116: "DDX (116)",
                                117: "IATP (117)",
                                118: "STP (118)",
                                119: "SRP (119)",
                                120: "UTI (120)",
                                121: "SMP (121)",
                                122: "SM (122)",
                                123: "PTP (123)",
                                124: "IS-IS (124)",
                                125: "FIRE (125)",
                                126: "CRTP (126)",
                                127: "CRUDP (127)",
                                128: "SSCOPMCE (128)",
                                129: "IPLT (129)",
                                130: "SPS (130)",
                                131: "PIPE (131)",
                                132: "SCTP (132)",
                                133: "FC (133)",
                                134: "RSVP-E2E-IGNORE (134)",
                                135: "Mobility (135)",
                                136: "UDPLite (136)",
                                137: "MPLS-in-IP (137)",
                                138: "manet (138)",
                                139: "HIP (139)",
                                140: "Shim6 (140)",
                                141: "WESP (141)",
                                142: "ROHC (142)"
                            };
                        }
                        return (value!=null)?Ung.panel.Reports.protocolMap[value] || value.toString():"";
                    },
                    "interface": function(value) {
                        if(!Ung.panel.Reports.interfaceMap) {
                            var interfacesList = [];
                            try {
                                interfacesList = rpc.reportsManager.getInterfacesInfo().list;
                            } catch (e) {
                                Ung.Util.rpcExHandler(e);
                            }
                            interfacesList.push({ interfaceId: 250, name: "OpenVPN" } ); // 0xfa
                            interfacesList.push({ interfaceId: 251, name: "L2TP" } ); // 0xfb
                            interfacesList.push({ interfaceId: 252, name: "Xauth" } ); // 0xfc
                            interfacesList.push({ interfaceId: 253, name: "GRE" } ); // 0xfd
                            Ung.panel.Reports.interfaceMap = {};
                            for(var i=0;i<interfacesList.length;i++) {
                                Ung.panel.Reports.interfaceMap[interfacesList[i].interfaceId]=interfacesList[i].name;
                            }
                        }
                        return (value!=null) ? Ung.panel.Reports.interfaceMap[value] || value.toString() : "";
                    }
                };
            }
            return this.columnRenderers[columnName];
        }
    }
});



Ext.define("Ung.panel.ExtraConditions", {
    extend: "Ext.panel.Panel",
    collapsible: true,
    collapsed: false,
    floatable: false,
    split: true,
    defaultCount: 1,
    autoScroll: true,
    layout: { type: 'vbox'},
    initComponent: function() {
        this.title = Ext.String.format( i18n._("Conditions: {0}"), i18n._("None"));
        this.collapsed = Ung.Main.viewport.getHeight()<500;
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
        rpc.reportsManager.getConditionQuickAddHints(Ext.bind(function(result, exception) {
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
                queryMode: 'local',
                store: this.columnsStore,
                value: data.column,
                listConfig: {
                    minWidth: 520
                },
                tpl: Ext.create('Ext.XTemplate',
                    '<ul class="x-list-plain"><tpl for=".">',
                        '<li role="option" class="x-boundlist-item"><b>{header}</b> <span style="float: right;">[{dataIndex}]</span></li>',
                    '</tpl></ul>'
                ),
                // template for the content inside text field
                displayTpl: Ext.create('Ext.XTemplate',
                    '<tpl for=".">',
                        '{header} [{dataIndex}]',
                    '</tpl>'
                ),
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
                store: ["=", "!=", ">", "<", ">=", "<=", "like", "not like", "is","is not", "in", "not in"],
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
        
        this.grid.on("afterrender", Ext.bind(function() {
            this.grid.getStore().addFilter(this.globalFilter);
        }, this));
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
        this.serverDate = (this.date) ? (new Date(this.date.getTime() - i18n.timeoffset)) : null;
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
