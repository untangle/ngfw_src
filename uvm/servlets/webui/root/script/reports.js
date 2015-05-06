Ext.define('Ung.panel.Reports', {
    extend: 'Ext.panel.Panel',
    autoRefreshInterval: 20, //In Seconds
    layout: { type: 'border'},
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
        }];
        this.callParent(arguments);
        this.chartContainer = this.down("container[name=chartContainer]");
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
                    tbar: [ '->', {
                        xtype: 'segmentedbutton',
                        width: 200,
                        items: [{
                            text: 'Stack'
                        }, {
                            text: 'Group',
                            pressed: true
                        }],
                        listeners: {
                            toggle: Ext.bind(function(segmentedButton, button, pressed) {
                                var chart = this.chartContainer.down("[name=chart]"),
                                series = chart.getSeries()[0],
                                value = segmentedButton.getValue();
                                series.setStacked(value === 0);
                                chart.redraw(); 
                            }, this)
                        }
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
                        type: 'bar',
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
            }
            this.chartContainer.add(chart); 
        }, this), this.reportEntry, this.startDateWindow.date, this.endDateWindow.date, -1);
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
                this.chartContainer.down("[name=chart]").getStore().loadData(result.list);
            }, this), this.reportEntry, this.startDateWindow.date, this.endDateWindow.date, -1);
            
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
                this.chartContainer.down("[name=chart]").getStore().loadData(result.list);
                if(this!=null && this.rendered && this.autoRefreshEnabled) {
                    Ext.Function.defer(this.autoRefresh, this.autoRefreshInterval*1000, this);
                }
            }, this), this.reportEntry, this.startDateWindow.date, this.endDateWindow.date, -1);
            
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