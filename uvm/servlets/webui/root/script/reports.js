Ext.define('Ung.panel.Reports', {
    extend: 'Ext.panel.Panel',
    settingsCmp: null,
    layout: { type: 'vbox', align: 'stretch' },
    initComponent: function() {
        if(!this.title) {
            this.title = i18n._('Reports');
        }
        if (!this.isReportsAppInstalled()) {
            this.items = [{
                xtype: 'component',
                html: i18n._("Event Logs require the Reports application. Please install and enable the Reports application.")
            }];
            this.callParent(arguments);
            return;
        }
        var reportEntries = this.getReportingManagerNew().getReportEntries().list;
        var reportsList = [];
        for(var i=0; i<reportEntries.length; i++) {
            reportsList.push([i, reportEntries[i].title]);
        }
        this.items = [{
            xtype: 'combo',
            margin: 10,
            fieldLabel: i18n._("Select Report"),
            width: 500,
            store: reportsList,
            queryMode: 'local',
            editable: false,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, newValue) {
                        this.loadReport(reportEntries[newValue]);
                    }, this)
                }
            }
        }, {
            xtype: 'container',
            name:'chartContainer',
            flex: 1,
            layout: 'fit',
            html: ""
        }];
        this.callParent(arguments);
        this.chartContainer = this.down("container[name=chartContainer]");
    },
    loadReport: function(rep) {
        this.chartContainer.removeAll();
        this.chartContainer.add({
            xtype: 'container',
            margin: 10,
            html: i18n._("Loading report data...")
        });
        this.getReportingManagerNew().getDataForReportEntry(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            if(!this.chartContainer || !this.chartContainer.isVisible()) {
                return;
            }
            this.chartContainer.removeAll();
            var data = result.list;
            console.log(rep);
            console.log(data);
            var chart = {xtype: 'component', html: ""}, dataStore;
            if(rep.type == 'PIE_GRAPH') {
                var othersFn = function(val) {
                    return (val==null)?i18n._("others"):val;
                };
                dataStore = Ext.create('Ext.data.JsonStore', {
                    fields: [{name: rep.pieGroupColumn, convert: othersFn}, {name:'value'} ],
                    data: data
                }); 

                chart = {
                    xtype: 'polar',
                    theme: 'default-gradients',
                    border: false,
                    width: '100%',
                    height: '100%',
                    store: dataStore,
                    insetPadding: 50,
                    innerPadding: 20,
                    legend: {
                        docked: 'right'
                    },
                    interactions: ['rotate', 'itemhighlight'],
                    sprites: [{
                        type: 'text',
                        text: rep.title,
                        fontSize: 22,
                        width: 100,
                        height: 30,
                        x: 10, // the sprite x position
                        y: 20  // the sprite y position
                    }, {
                        type: 'text',
                        text: rep.description,
                        fontSize: 12,
                        x: 10,
                        y: 35
                    }],
                    series: [{
                        type: 'pie',
                        angleField: 'value',
                        label: {
                            field: rep.pieGroupColumn,
                            calloutLine: {
                                length: 60,
                                width: 3
                                // specifying 'color' is also possible here
                            }
                        },
                        highlight: true,
                        tooltip: {
                            trackMouse: true,
                            renderer: function(storeItem, item) {
                                this.setHtml(storeItem.get(rep.pieGroupColumn) + ": "+ storeItem.get('value') +" Hits");
                            }
                        }
                    }]
                };
            } else if(rep.type == 'TIME_GRAPH') {
                var axesFields = [];
                var series = [], column;
                var markerFx = {
                    duration: 200,
                    easing: 'backOut'
                };
                var zeroFn = function(val) {
                    return (val==null)?0:val;
                };
                var timeFn = function(val) {
                    return (val==null || val.time==null)?0:i18n.timestampFormat(val);
                };
                var storeFields =[{name: 'time_trunc', convert: timeFn}];
                for(var i=0; i<rep.timeDataColumns.length; i++) {
                    column = rep.timeDataColumns[i].split(" ").splice(-1)[0];
                    axesFields.push(column);
                    storeFields.push({name: column, convert: zeroFn});
                    series.push({
                        type: 'line',
                        axis: 'left',
                        title: column,
                        xField: 'time_trunc',
                        yField: column,
                        marker: {
                            type: 'square',
                            fx: markerFx
                        },
                        highlightCfg: {
                            scaling: 2
                        },
                        tooltip: {
                            trackMouse: true,
                            style: 'background: #fff',
                            renderer: function(storeItem, item) {
                                var title = item.series.getTitle();
                                this.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + ' Hits');
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
                    width: '100%',
                    height: '100%',
                    border: false,
                    legend: {
                        docked: 'right'
                    },
                    store: dataStore,
                    insetPadding: 40,
                    sprites: [{
                        type: 'text',
                        text: rep.title,
                        fontSize: 22,
                        width: 100,
                        height: 30,
                        x: 10, // the sprite x position
                        y: 20  // the sprite y position
                    }, {
                        type: 'text',
                        text: rep.description,
                        fontSize: 12,
                        x: 10,
                        y: 35
                    }],
                    axes: [{
                        type: 'numeric',
                        fields: axesFields,
                        position: 'left',
                        grid: true,
                        minimum: 0,
                        renderer: function (v) {
                            return v + ' Hits';
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
            this.chartContainer.add(chart); 
        }, this), rep, null, null, -1);
    },
    isDirty: function() {
        return false;
    },
    getReportingManagerNew: function(forceReload) {
        if (forceReload || rpc.reportingManagerNew === undefined) {
            try {
                rpc.reportingManagerNew = this.getNodeReporting().getReportingManagerNew();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.reportingManagerNew;
    },
    
    // get node reporting
    getNodeReporting: function(forceReload) {
        if (forceReload || this.nodeReporting === undefined) {
            try {
                this.nodeReporting = rpc.nodeManager.node("untangle-node-reporting");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.nodeReporting;
    },
    // is reports node installed
    isReportsAppInstalled: function(forceReload) {
        if (forceReload || this.reportsAppInstalledAndEnabled === undefined) {
            try {
                if (!this.getNodeReporting()) {
                    this.reportsAppInstalledAndEnabled = false;
                } else {
                    if (this.nodeReporting.getRunState() == "RUNNING"){
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