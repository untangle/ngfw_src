/*global
 Ext, Ung, i18n, rpc
*/

Ext.define('Ung.dashboard', {
    widgets: [],
    constructor: function (config) {
        Ext.apply(this, config);
    },
    setWidgets: function (widgets) {
        this.widgets = [];
        this.dashboardPanel.removeAll();

        var i, j, k,
            widgetsList = [],
            gridList = [],
            grid, gridEl;

        for (i = 0; i < widgets.length; i += 1) {
            widgetsList.push(Ext.create('Ung.dashboard.' + widgets[i].type));
        }

        for (j = 0; j < widgetsList.length; j += 1) {

            if (gridList.length > 0) {
                grid = gridList[gridList.length - 1];

                if (grid.type === 'small' && widgetsList[j].displayMode === 'small') {
                    if (grid.items.length < 4) {
                        grid.items.push(widgetsList[j]);
                    } else {
                        gridList.push({type: 'small', items: [widgetsList[j]]});
                    }
                } else {
                    gridList.push({type: widgetsList[j].displayMode, items: [widgetsList[j]]});
                }

            } else {
                grid = {type: widgetsList[j].displayMode, items: [widgetsList[j]]};
                gridList.push(grid);
            }
        }

        for (k = 0; k < gridList.length; k += 1) {
            gridEl = Ext.create('Ung.dashboard.GridWrapper');

            if (gridList[k].type === 'small') {
                gridEl.cls = 'grid-cell small-' + gridList[k].items.length;
            } else {
                gridEl.cls = 'grid-cell big';
            }
            gridEl.add(gridList[k].items);
            this.widgets.push(gridEl);
        }
        this.dashboardPanel.add(this.widgets);
    },
    updateFromStats: function (stats) {
        //console.log(stats);
        var i, widget;
        for (i = 0; i < this.widgets.length; i += 1) {
            widget = this.widgets[i];
            if (widget.hasStats) {
                widget.updateFromStats(stats);
            }
        }
    }
});

Ext.define('Ung.dashboard.Widget', {
    extend: 'Ext.panel.Panel',
    cls: 'widget small-widget',
    hidden: false,
    /*
    tools: [{
        type:'refresh',
        callback: function() {
            this.refresh();
        },
        scope: this
    }],
    */
    initComponent: function () {
        this.callParent(arguments);
    },
    listeners: {
        beforeclose: {
            fn: function (panel, eOpts) {
                if (panel.removeConfirmed) {
                    return true;
                }
                Ext.MessageBox.confirm(i18n._("Remove widget"),
                        i18n._("Do you want to remove this widget from dashboard?"),
                        function (btn) {
                        if (btn === 'yes') {
                            //TODO: remove this widget from dashboard.
                            panel.removeConfirmed = true;
                            panel.close();
                        }
                    }, this);
                return false;
            },
            scope: this
        }
    },
    updateFromStats: function (stats) {
        this.items.each(function (item) {
            if (item.statsProperty) {
                item.setValue(stats[item.statsProperty]);
            }
            // check if item has updateStatus function, used for parsing data
            if (Ext.isFunction(item.updateStats)) {
                item.updateStats(stats);
            }
        });

    }
});

Ext.define('Ung.dashboard.Information', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Information"),
    displayMode: 'small',
    hasStats: true,
    height: 190,
    defaults: {
        xtype: 'displayfield',
        labelWidth: 100
    },
    items: [{
        name: 'hostname',
        fieldLabel: i18n._("Hostname")
    }, {
        fieldLabel: i18n._("Model"),
        value: 'unknown'
    },
        /*{
        fieldLabel: i18n._("Model"),
        statsProperty: 'cpuModel'
    },*/ {
            name: 'uptime',
            fieldLabel: i18n._("Uptime"),
            updateStats: function (stats) {
                // display humab readable uptime
                var numdays = Math.floor((stats.uptime % 31536000) / 86400),
                    numhours = Math.floor(((stats.uptime % 31536000) % 86400) / 3600),
                    numminutes = Math.floor((((stats.uptime % 31536000) % 86400) % 3600) / 60),
                    output = '';

                if (numdays > 0) {
                    output += numdays + 'd ';
                }

                if (numhours > 0) {
                    output += numhours + 'h ';
                }

                if (numminutes > 0) {
                    output += numminutes + 'm';
                }
                this.setValue(output);
            }
        }, {
        name: 'version',
        fieldLabel: i18n._("Version")
    }, {
        name: 'subscriptions',
        fieldLabel: i18n._("Subscriptions")
    }],
    afterRender: function () {
        this.callParent(arguments);
        this.down('displayfield[name=hostname]').setValue(rpc.hostname);
        this.down('displayfield[name=version]').setValue(rpc.fullVersion);
    }
});

Ext.define('Ung.dashboard.Server', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Server"),
    displayMode: 'small',
    height: 190,
    hasStats: true,
    layout: {
        type: 'vbox'
    },
    items: [
        {
            xtype: 'container',
            layout: 'hbox',
            cls: 'nopadding',
            items: [
                {
                    xtype: 'displayfield',
                    fieldLabel: i18n._("CPU"),
                    width: 60
                },
                {
                    xtype: 'container',
                    layout: 'vbox',
                    items: [
                        {
                            xtype: 'component',
                            name: 'cpu',
                            padding: '3px 0'
                        },
                        {
                            xtype: 'component',
                            name: 'cpuload',
                            padding: '3px 0'
                        }
                    ]
                }
            ],
            updateStats: function (stats) {
                var oneMinuteLoadAvg = stats.oneMinuteLoadAvg,
                    oneMinuteLoadAvgAdjusted = oneMinuteLoadAvg - stats.numCpus,
                    loadText = '<font color="#55BA47">' + i18n._('LOW') + '</font>';
                if (oneMinuteLoadAvgAdjusted > 1.0) {
                    loadText = '<font color="orange">' + i18n._('MEDIUM') + '</font>';
                }
                if (oneMinuteLoadAvgAdjusted > 4.0) {
                    loadText = '<font color="red">' + i18n._('HIGH') + '</font>';
                }

                this.down('component[name=cpu]').update(loadText);
                this.down('component[name=cpuload]').update('<strong>' + stats.oneMinuteLoadAvg + '</strong> 1-min load');

            }
        },
        {
            xtype: 'container',
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            cls: 'nopadding',
            items: [
                {
                    xtype: 'displayfield',
                    fieldLabel: i18n._("Disk"),
                    width: 60
                },
                {
                    xtype: 'container',
                    layout: 'vbox',
                    flex: 1,
                    items: [
                        {
                            xtype: 'progress',
                            name: 'disk',
                            width: 160,
                            margin: '3px 0'
                        },
                        {
                            xtype: 'component',
                            name: 'usedDisk',
                            padding: '3px 0'
                        },
                        {
                            xtype: 'component',
                            name: 'freeDisk',
                            padding: '3px 0'
                        }
                    ]
                }
            ],
            updateStats: function (stats) {
                var usedDisk = Math.round((stats.totalDiskSpace - stats.freeDiskSpace) / 10000000) / 100,
                    usedPercent = parseFloat((1 - parseFloat(stats.freeDiskSpace / stats.totalDiskSpace)) * 100).toFixed(1),
                    freeDisk = Math.round(stats.freeDiskSpace / 10000000) / 100;

                this.down('progress[name=disk]').setValue(1 - parseFloat(stats.freeDiskSpace / stats.totalDiskSpace).toFixed(3));
                this.down('component[name=usedDisk]').update('<strong>' + usedDisk + ' GB</strong> used <em>(' + usedPercent + '%)</em>');
                this.down('component[name=freeDisk]').update('<strong>' + freeDisk + ' GB</strong> free <em>(' + (100 - usedPercent) + '%)</em>');
            }
        }
    ]
});

Ext.define('Ung.dashboard.Sessions', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Sessions"),
    displayMode: 'big',
    height: 400,
    defaults: {
        xtype: 'displayfield',
        labelWidth: 150
    },
    items: []
});

Ext.define('Ung.dashboard.Devices', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Devices"),
    height: 190,
    defaults: {
        xtype: 'displayfield',
        labelWidth: 150
    },
    items: [{
        name: 'totalDevices',
        fieldLabel: i18n._("Total Devices")
    }, {
        name: 'activeDevices',
        fieldLabel: i18n._("Active Devices")
    }, {
        name: 'highesActiveDevices',
        fieldLabel: i18n._("Highest Active Devices")
    }, {
        name: 'knownDevices',
        fieldLabel: i18n._("Known Devices")
    }]
});

Ext.define('Ung.dashboard.Hardware', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Hardware"),
    displayMode: 'small',
    hasStats: true,
    height: 190,
    defaults: {
        xtype: 'displayfield',
        labelWidth: 100
    },
    items: [{
        fieldLabel: i18n._("CPU Count"),
        statsProperty: 'numCpus'
    }, {
        fieldLabel: i18n._("CPU Type"),
        statsProperty: 'cpuModel',
        cls: 'ellipsis'
    }, {
        fieldLabel: i18n._("Architecture"),
        statsProperty: 'architecture'
    }, {
        fieldLabel: i18n._("Memory"),
        updateStats: function (stats) {
            this.setValue(Ung.Util.bytesToMBs(stats.MemTotal) + ' MB');
        }
    }, {
        fieldLabel: i18n._("Disk"),
        updateStats: function (stats) {
            this.setValue(Math.round(stats.totalDiskSpace / 10000000) / 100 + ' GB');
        }
    }]
});

Ext.define('Ung.dashboard.Memory', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Memory Resources"),
    displayMode: 'small',
    height: 190,
    hasStats: true,
    layout: {
        type: 'vbox'
    },
    items: [
        {
            xtype: 'container',
            layout: 'hbox',
            cls: 'nopadding',
            items: [
                {
                    xtype: 'displayfield',
                    fieldLabel: i18n._("Memory"),
                    width: 60
                },
                {
                    xtype: 'container',
                    layout: 'vbox',
                    items: [
                        {
                            xtype: 'progress',
                            name: 'memory',
                            width: 160,
                            style: {
                                marginBottom: '7px'
                            }
                        },
                        {
                            xtype: 'component',
                            name: 'usedMemory',
                            style: {
                                minHeight: '18px'
                            }
                        },
                        {
                            xtype: 'component',
                            name: 'freeMemory',
                            style: {
                                minHeight: '18px'
                            }
                        }
                    ]
                }
            ],
            updateStats: function (stats) {
                var usedMemory = Ung.Util.bytesToMBs(stats.MemTotal - stats.MemFree),
                    usedMemoryPercent = parseFloat((1 - parseFloat(stats.MemFree / stats.MemTotal)) * 100).toFixed(1),
                    freeMemory = Ung.Util.bytesToMBs(stats.MemFree);

                this.down('progress[name=memory]').setValue(1 - parseFloat(stats.MemFree / stats.MemTotal).toFixed(3));
                this.down('component[name=usedMemory]').update('<strong>' + usedMemory + ' MB</strong> used <em>(' + usedMemoryPercent + '%)</em>');
                this.down('component[name=freeMemory]').update('<strong>' + freeMemory + ' MB</strong> free <em>(' + (100 - usedMemoryPercent) + '%)</em>');
            }
        },
        {
            xtype: 'container',
            layout: 'hbox',
            cls: 'nopadding',
            items: [
                {
                    xtype: 'displayfield',
                    fieldLabel: i18n._("Swap"),
                    width: 60
                },
                {
                    xtype: 'container',
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },

                    items: [
                        {
                            xtype: 'progress',
                            name: 'swap',
                            width: 160,
                            style: {
                                marginBottom: '7px'
                            }
                        },
                        {
                            xtype: 'component',
                            name: 'usedSwap',
                            style: {
                                minHeight: '18px'
                            }
                        },
                        {
                            xtype: 'component',
                            name: 'freeSwap',
                            style: {
                                minHeight: '18px'
                            }
                        }
                    ]
                }
            ],
            updateStats: function (stats) {
                var usedSwap = Ung.Util.bytesToMBs(stats.SwapTotal - stats.SwapFree),
                    usedSwapPercent = parseFloat((1 - parseFloat(stats.SwapFree / stats.SwapTotal)) * 100).toFixed(1),
                    freeSwap = Ung.Util.bytesToMBs(stats.SwapFree);

                this.down('progress[name=swap]').setValue(1 - parseFloat(stats.SwapFree / stats.SwapTotal).toFixed(3));
                this.down('component[name=usedSwap]').update('<strong>' + usedSwap + ' MB</strong> used <em>(' + usedSwapPercent + '%)</em>');
                this.down('component[name=freeSwap]').update('<strong>' + freeSwap + ' MB</strong> free <em>(' + (100 - usedSwapPercent) + '%)</em>');
            }
        }
    ]
});

Ext.define('Ung.dashboard.ReportEntry', {
    extend: 'Ung.dashboard.Widget',
    width: 500,
    initComponent: function (conf) {
        this.title =  i18n._("Report");
        this.items = {
            xtype: 'grid',
            header: false,
            store:  Ext.create('Ext.data.Store', {
                fields: [],
                data: []
            }),
            columns: [{
                flex: 1
            }]
        };
    }
});

Ext.define('Ung.dashboard.EventEntry', {
    extend: 'Ung.dashboard.Widget',
    width: 500,
    initComponent: function (conf) {
        this.title =  i18n._("Events");
        this.items = {
            xtype: 'grid',
            header: false,
            store:  Ext.create('Ext.data.Store', {
                fields: [],
                data: []
            }),
            columns: [{
                flex: 1
            }]
        };
    }
});

Ext.define('Ung.dashboard.GridWrapper', {
    extend: 'Ext.container.Container',
    hasStats: true,
    updateFromStats: function (stats) {
        this.items.each(function (item) {
            if (item.hasStats) {
                item.updateFromStats(stats);
            }
        });
    }
});