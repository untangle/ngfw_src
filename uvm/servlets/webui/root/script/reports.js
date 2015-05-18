Ext.define('Ung.panel.Reports', {
    extend: 'Ext.panel.Panel',
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
        
        var reportEntriesStore = Ext.create('Ext.data.Store', {
            fields: ["title"],
            data: []
        });
        
        Ung.Main.getReportingManagerNew().getReportEntries(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            this.reportEntries = result.list;
            reportEntriesStore.loadData(this.reportEntries);
        }, this), this.category);
        
        this.items = [{
            region: 'west',
            title: i18n._("Select Report"),
            width: 250,
            split: true,
            collapsible: true,
            collapsed: false,
            floatable: false,
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
                    this.loadReport(record.getData());
                }, this)
            }
        
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
                        name: "extraConditions",
                        text: i18n._('Add Conditions'),
                        initialLabel:  i18n._('Add Conditions'),
                        width: 132,
                        tooltip: i18n._('Add extra SQL conditions to report'),
                        handler: Ext.bind(function(button) {
                            if(!this.reportEntry) {
                                return;
                            }
                            this.extraConditionsPanel.getColumnsForTable(this.reportEntry.table);
                            this.extraConditionsPanel.expand();
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
                parentPanel: this
            })]
        }];
        this.callParent(arguments);
        this.chartContainer = this.down("panel[name=chartContainer]");
    },
    loadReport: function(reportEntry) {
        this.reportEntry = reportEntry;
        this.chartContainer.removeAll();
        Ung.Main.getReportingManagerNew().getDataForReportEntry(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            if(!this.chartContainer || !this.chartContainer.isVisible()) {
                return;
            }
            
            var data = result.list;
            var chart = {xtype: 'component', html: ""}, dataStore;
            if(reportEntry.type == 'PIE_GRAPH') {
                var descriptionFn = function(val, record) {
                    var title = (record.get(reportEntry.pieGroupColumn)==null)?i18n._("none") : record.get(reportEntry.pieGroupColumn);
                    var value = (reportEntry.units == "bytes") ? Ung.Util.bytesRenderer(record.get("value")) : record.get("value") + " " + i18n._(reportEntry.units);
                    return title + ": " + value;
                };
                dataStore = Ext.create('Ext.data.JsonStore', {
                    fields: [{name: "description", convert: descriptionFn }, {name:'value'} ],
                    data: data
                }); 

                chart = {
                    xtype: 'polar',
                    name: "chart",
                    store: dataStore,
                    theme: 'default-gradients',
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
            } else if(reportEntry.type == 'TIME_GRAPH') {
                var axesFields = [],column;
                var zeroFn = function(val) {
                    return (val==null)?0:val;
                };
                var timeFn = function(val) {
                    return (val==null || val.time==null)?0:i18n.timestampFormat(val);
                };
                var storeFields =[{name: 'time_trunc', convert: timeFn}];
                for(var i=0; i<reportEntry.timeDataColumns.length; i++) {
                    column = reportEntry.timeDataColumns[i].split(" ").splice(-1)[0];
                    axesFields.push(column);
                    storeFields.push({name: column, convert: zeroFn});
                }
                dataStore = Ext.create('Ext.data.JsonStore', {
                    fields: storeFields,
                    data: data
                });
                
                chart = {
                    xtype: 'cartesian',
                    name: "chart",
                    store: dataStore,
                    theme: 'default-gradients',
                    border: false,
                    width: '100%',
                    height: '100%',
                    insetPadding: {top: 50, left: 10, right: 10, bottom: 10},
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
                    series: [{
                        type: 'bar3d',
                        axis: 'left',
                        title: axesFields,
                        xField: 'time_trunc',
                        yField: axesFields,
                        stacked: false,
                        style: {
                            opacity: 0.90
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

                if ( reportEntry.timeColors != null ) {
                    chart.colors = reportEntry.timeColors;
                }
            }
            this.chartContainer.add(chart); 
        }, this), this.reportEntry, this.startDateWindow.date, this.endDateWindow.date, this.extraConditions, -1);
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
                if(!this.chartContainer || !this.chartContainer.isVisible()) {
                    return;
                }
                var chart = this.chartContainer.down("[name=chart]");
                if(chart) {
                    chart.getStore().loadData(result.list);
                }
                
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
                if(!this.chartContainer || !this.chartContainer.isVisible()) {
                    return;
                }
                var chart = this.chartContainer.down("[name=chart]");
                if(chart) {
                    chart.getStore().loadData(result.list);
                }
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
    isDirty: function() {
        return false;
    },
    listeners: {
        "activate": {
            fn: function() {
                if(!this.reportEntry && this.reportEntries !=null && this.reportEntries.length > 0) {
                    this.loadReport(this.reportEntries[0]);
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
    title: i18n._('Extra Conditions'),
    collapsible: true,
    collapsed: true,
    split: true,
    autoScroll: true,
    count: 5,
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
            items.push.apply(items, [{
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
                            var isEmpty = Ext.isEmpty(newValue);
                            combo.next("[dataIndex=operator]").setDisabled(isEmpty);
                            combo.next("[dataIndex=value]").setDisabled(isEmpty);
                            combo.next("[dataIndex=clear]").setDisabled(isEmpty);
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
                store: ["=", "!=", "<>", ">", "<", ">=", "<=", "between", "like", "in", "is"]
            }, {
                xtype: 'textfield',
                dataIndex: "value",
                name: "value"+i,
                width: 400,
                margin: 3,
                disabled: true,
                emptyText: i18n._("[no value]")
            }, {
                xtype: 'button',
                dataIndex: "clear",
                margin: 3,
                name: "clear"+i,
                text: i18n._("Clear"),
                disabled: true,
                handler: function() {
                    this.prev("[dataIndex=column]").setValue("");
                    this.prev("[dataIndex=operator]").setValue("=");
                    this.prev("[dataIndex=value]").setValue("");
                }
            }]);
        }
        this.items = items;
        
        this.bbar = ['->',{
            text: i18n._("Done"),
            iconCls: 'save-icon',
            handler: function() {
                this.setConditions();
                this.collapse();
                var refreshButton = this.parentPanel.down("button[name=refresh]");
                if(!refreshButton.isDisabled()) {
                    this.parentPanel.refreshHandler(true);
                }
                
            },
            scope: this
        }, {
            name: 'Clear',
            text: i18n._("Clear All"),
            iconCls: 'cancel-icon',
            handler: function() {
                this.clearConditions();
                this.collapse();
                var refreshButton = this.parentPanel.down("button[name=refresh]");
                if(!refreshButton.isDisabled()) {
                    this.parentPanel.refreshHandler(true);
                }
            },
            scope: this
        }];
        this.callParent(arguments);
    },
    clearConditions: function() {
        for(var i=0; i<this.count; i++) {
            this.down("[name=column"+i+"]").setValue("");
            this.down("[name=operator"+i+"]").setValue("=");
            this.down("[name=value"+i+"]").setValue("");
        }
        this.setConditions();
    },
    setConditions: function() {
        var conditions = [], column;
        for(var i=0; i<this.count; i++) {
            column = this.down("[name=column"+i+"]").getValue();
            if(!Ext.isEmpty(column)) {
                conditions.push({
                    "javaClass": "com.untangle.uvm.node.SqlCondition",
                    "column": column,
                    "operator": this.down("[name=operator"+i+"]").getValue(),
                    "value": this.down("[name=value"+i+"]").getValue()
                });
            }
        }
        if(!this.buttonObj){
            this.buttonObj = this.parentPanel.down("button[name=extraConditions]");
        }
            
        this.parentPanel.extraConditions = (conditions.length>0)?conditions:null;
        this.buttonObj.setText((conditions.length>0)?Ext.String.format( i18n._("{0} Condition(s)"), conditions.length):this.buttonObj.initialLabel);
    }
});