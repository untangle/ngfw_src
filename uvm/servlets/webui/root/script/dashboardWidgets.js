/*global
 Ext, Ung, i18n, rpc, setTimeout
*/

Ext.define('Ung.dashboard', {
    widgets: [],
    constructor: function (config) {
        Ext.apply(this, config);
    },
    loadDashboard: function() {
        this.reportsEnabled = Ung.Main.isReportsAppInstalled();
        var loadSemaphore = this.reportsEnabled ? 4 : 1;
        var callback = Ext.bind(function () {
            loadSemaphore--;
            if (loadSemaphore === 0) {
                this.setWidgets();
            }
        }, this);
        rpc.dashboardManager.getSettings(Ext.bind(function(result, exception) {
            if (Ung.Util.handleException(exception)) return;
            this.allWidgets = result.widgets.list;
            callback();
        }, this));
        if(this.reportsEnabled) {
            this.loadReportEntries(callback);
            this.loadEventEntries(callback);
            Ung.Main.getReportsManager().getUnavailableApplicationsMap(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.unavailableApplicationsMap = result.map;
                callback();
            }, this));
        }
    },
    setWidgets: function () {
        this.widgets = [];
        this.dashboardPanel.removeAll();
        var i, j, k,
            widgetsList = [],
            gridList = [],
            grid, gridEl, type, entry;

        for (i = 0; i < this.allWidgets.length; i += 1) {
            type = this.allWidgets[i].type;
            if (type !== "ReportEntry" && type !== "EventEntry") {
                widgetsList.push(Ext.create('Ung.dashboard.' + this.allWidgets[i].type));
            } else {
                if(this.reportsEnabled) {
                    if(type == "ReportEntry") {
                        entry = this.reportsMap[this.allWidgets[i].entryId];
                        if(!entry && !entry.enabled) {
                            entry = null;
                        }
                    } else {
                        entry = this.eventsMap[this.allWidgets[i].entryId];
                    }
                    if(entry && !this.unavailableApplicationsMap[entry.category]) {
                        widgetsList.push(Ext.create('Ung.dashboard.' + this.allWidgets[i].type, {
                            entry: entry,
                            refreshIntervalSec: this.allWidgets[i].refreshIntervalSec
                        }));
                    }
                }
            }
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
    resetReports: function() {
        this.reportEntries = null;
        this.reportsMap = null;
        this.eventEntries = null;
        this.eventsMap = null;
    },
    loadReportEntries: function(handler) {
        if(!this.reportEntries) {
            Ung.Main.getReportsManager().getReportEntries(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.reportEntries = result.list;
                this.reportsMap = Ung.Util.createRecordsMap(this.reportEntries, "uniqueId");
                handler();
            }, this));
        } else {
            handler();
        }
    },
    loadEventEntries: function(handler) {
        if(!this.eventEntries) {
            Ung.Main.getReportsManager().getEventEntries(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.eventEntries = result.list;
                this.eventsMap = Ung.Util.createRecordsMap(this.eventEntries, "uniqueId");
                handler();
            }, this));
        } else {
            handler();
        }
    },
    updateStats: function (stats) {
        var i, widget;
        for (i = 0; i < this.widgets.length; i += 1) {
            widget = this.widgets[i];
            if (widget.hasStats) {
                widget.updateStats(stats);
            }
        }
    }
});

Ext.define('Ung.dashboard.Widget', {
    extend: 'Ext.panel.Panel',
    cls: 'widget small-widget',
    hidden: false,
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
    updateStats: function (stats) {
        this.items.each(function (item) {
            if (item.statsProperty) {
                item.setValue(stats[item.statsProperty]);
            }
            // check if item has updateStatus function, used for parsing data
            if (Ext.isFunction(item.updateStats)) {
                item.updateStats(stats);
            }
        });
        if (Ext.isFunction(this.updateStats)) {
            this.updateStats(stats);
        }
    }
});

/* Grid container for the 1 or more widgets */
Ext.define('Ung.dashboard.GridWrapper', {
    extend: 'Ext.container.Container',
    hasStats: true,
    updateStats: function (stats) {
        this.items.each(function (item) {
            if (item.hasStats) {
                item.updateStats(stats);
            }
        });
    }
});

/* Information Widget */
Ext.define('Ung.dashboard.Information', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    height: 190,
    hasStats: true,
    initComponent: function () {
        this.title = i18n._("Information");
        this.callParent(arguments);
    },
    tpl: '<div class="wg-wrapper">' +
        '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Hostname') + ':</label>' +
            '<div class="cell">{hostname}</div>' +
        '</div>' +
        '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Model') + ':</label>' +
            '<div class="cell">unknown</div>' +
        '</div>' +
        '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Uptime') + ':</label>' +
            '<div class="cell">{uptime}</div>' +
        '</div>' +
        '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Version') + ':</label>' +
            '<div class="cell">{version}</div>' +
        '</div>' +
        '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Subscriptions') + ':</label>' +
            '<div class="cell">{subscriptions}</div>' +
        '</div>' +
        '</div>',
    data: {},
    updateStats: function (stats) {
        var numdays = Math.floor((stats.uptime % 31536000) / 86400),
            numhours = Math.floor(((stats.uptime % 31536000) % 86400) / 3600),
            numminutes = Math.floor((((stats.uptime % 31536000) % 86400) % 3600) / 60),
            _uptime = '',
            licenseManager = rpc.licenseManager;

        if (numdays > 0) {
            _uptime += numdays + 'd ';
        }
        if (numhours > 0) {
            _uptime += numhours + 'h ';
        }
        if (numminutes > 0) {
            _uptime += numminutes + 'm';
        }

        this.update({
            hostname: rpc.hostname,
            uptime: _uptime,
            version: rpc.fullVersion,
            subscriptions: licenseManager.validLicenseCount()
        });
    }
});

/* Memory Widget */
Ext.define('Ung.dashboard.Memory', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    height: 190,
    hasStats: true,
    initComponent: function () {
        this.title = i18n._("Memory Resources");
        this.callParent(arguments);
    },
    tpl: '<div class="wg-wrapper">' +
        '<div class="row">' +
        '<label style="width: 60px;">' + i18n._('Memory') + ':</label>' +
        '<div class="cell">' +
        '<div class="wg-progress"><div class="wg-progress-bar"><span style="left: -{percentFree}%;"></span></div><p>{totalMemory} MB</p></div>' +
        '<div class="wg-progress-vals"><span style="color: #D0AE26; font-weight: 600;">{usedMemory} MB</span> used <em>({percentUsed}%)</em></div>' +
        '<div class="wg-progress-vals"><span style="color: #555; font-weight: 600;">{freeMemory} MB</span> free <em>({percentFree}%)</em></div>' +
        '</div>' +
        '</div>' +
        '<div class="row">' +
        '<label style="width: 60px;">' + i18n._('Swap') + ':</label>' +
        '<div class="cell">' +
        '<div class="wg-progress"><div class="wg-progress-bar"><span style="left: -{percentFreeSwap}%;"></span></div><p>{totalSwap} MB</p></div>' +
        '<div class="wg-progress-vals"><span style="color: #D0AE26; font-weight: 600;">{usedSwap} MB</span> used <em>({percentUsedSwap}%)</em></div>' +
        '<div class="wg-progress-vals"><span style="color: #555; font-weight: 600;">{freeSwap} MB</span> free <em>({percentFreeSwap}%)</em></div>' +
        '</div>' +
        '</div>' +
        '</div>',
    data: {},
    updateStats: function (stats) {
        this.update({
            totalMemory: Ung.Util.bytesToMBs(stats.MemTotal),
            usedMemory: Ung.Util.bytesToMBs(stats.MemTotal - stats.MemFree),
            percentUsed: parseFloat((1 - parseFloat(stats.MemFree / stats.MemTotal)) * 100).toFixed(1),
            freeMemory: Ung.Util.bytesToMBs(stats.MemFree),
            percentFree: parseFloat(stats.MemFree / stats.MemTotal * 100).toFixed(1),
            totalSwap: Ung.Util.bytesToMBs(stats.SwapTotal),
            usedSwap: Ung.Util.bytesToMBs(stats.SwapTotal - stats.SwapFree),
            percentUsedSwap: parseFloat((1 - parseFloat(stats.SwapFree / stats.SwapTotal)) * 100).toFixed(1),
            freeSwap: Ung.Util.bytesToMBs(stats.SwapFree),
            percentFreeSwap: parseFloat(stats.SwapFree / stats.SwapTotal * 100).toFixed(1)
        });
    }
});

/* Server Widget */
Ext.define('Ung.dashboard.Server', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    height: 190,
    hasStats: true,
    initComponent: function () {
        this.title = i18n._("Server Resources");
        this.callParent(arguments);
    },
    tpl: '<div class="wg-wrapper">' +
        '<div class="row">' +
            '<label style="width: 40px;">' + i18n._('CPU') + ':</label>' +
            '<div class="cell">' +
                '<div style="line-height: 20px;">{cpuLoadText}</div>' +
                '<div style="line-height: 20px;">{avgCpuLoad}</div>' +
            '</div>' +
        '</div>' +
        '<div class="row">' +
            '<label style="width: 40px;">' + i18n._('Disk') + ':</label>' +
            '<div class="cell">' +
                '<div class="wg-progress"><div class="wg-progress-bar"><span style="left: -{freePercent}%;"></span></div><p>{totalDisk} GB</p></div>' +
                '<div class="wg-progress-vals"><span style="color: #D0AE26; font-weight: 600;">{usedDisk} GB</span> used <em>({usedPercent}%)</em></div>' +
                '<div class="wg-progress-vals"><span style="color: #555; font-weight: 600;">{freeDisk} GB</span> free <em>({freePercent}%)</em></div>' +
            '</div>' +
        '</div>' +
        '</div>',
    data: {},
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


        this.update({
            cpuLoadText: loadText,
            avgCpuLoad: '<strong>' + stats.oneMinuteLoadAvg + '</strong> 1-min load',
            totalDisk: Math.round(stats.totalDiskSpace / 10000000) / 100,
            usedDisk: Math.round((stats.totalDiskSpace - stats.freeDiskSpace) / 10000000) / 100,
            usedPercent: parseFloat((1 - parseFloat(stats.freeDiskSpace / stats.totalDiskSpace)) * 100).toFixed(1),
            freeDisk: Math.round(stats.freeDiskSpace / 10000000) / 100,
            freePercent: parseFloat(stats.freeDiskSpace / stats.totalDiskSpace * 100).toFixed(1)
        });
    }
});

/* Hardware Widget */
Ext.define('Ung.dashboard.Hardware', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    height: 190,
    hasStats: true,
    initComponent: function () {
        this.title = i18n._("Hardware");
        this.callParent(arguments);
    },
    tpl: '<div class="wg-wrapper">' +
            '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('CPU Count') + ':</label>' +
            '<div class="cell">{cpuCount}</div>' +
            '</div>' +
            '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('CPU Type') + ':</label>' +
            '<div class="cell">{cpuType}</div>' +
            '</div>' +
            '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Architecture') + ':</label>' +
            '<div class="cell">{architecture}</div>' +
            '</div>' +
            '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Memory') + ':</label>' +
            '<div class="cell">{totalMemory} MB</div>' +
            '</div>' +
            '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Disk') + ':</label>' +
            '<div class="cell">{totalDisk} GB</div>' +
            '</div>' +
        '</div>',
    data: {},
    dataSet: false,
    updateStats: function (stats) {
        // set values just once
        if (!this.dataSet) {
            this.update({
                cpuCount: stats.numCpus,
                cpuType: stats.cpuModel,
                architecture: stats.architecture,
                totalMemory: Ung.Util.bytesToMBs(stats.MemTotal),
                totalDisk: Math.round(stats.totalDiskSpace / 10000000) / 100
            });
            this.dataSet = true;
        }
    }
});

/* Hosts&Devices Widget */
Ext.define('Ung.dashboard.HostsDevices', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    height: 190,
    hasStats: true,
    initComponent: function () {
        this.title = i18n._("Hosts & Devices");
        this.callParent(arguments);
    },
    tpl: '<div class="wg-wrapper">' +
            '<div class="row">' +
            '<label style="width: 110px;">' + i18n._('Currently Active') + ':</label>' +
            '<div class="cell">{activeHosts}</div>' +
            '</div>' +
            '<div class="row">' +
            '<label style="width: 110px;">' + i18n._('Maximum Active') + ':</label>' +
            '<div class="cell">{maxActiveHosts}</div>' +
            '</div>' +
            '<div class="row">' +
            '<label style="width: 110px;">' + i18n._('Known Devices') + ':</label>' +
            '<div class="cell">{knownDevices}</div>' +
            '</div>' +
        '</div>' +
        '<div style="text-align: center; margin-top: 20px;">' +
            '<button class="wg-button">Show Hosts</button>' +
            '<button class="wg-button">Show Devices</button>' +
        '</div>',
    data: {},
    updateStats: function (stats) {
        this.update({
            activeHosts: stats.activeHosts,
            maxActiveHosts: stats.maxActiveHosts,
            knownDevices: stats.knownDevices
        });
    }
});

/* Sessions Widget */
Ext.define('Ung.dashboard.Sessions', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    height: 190,
    refreshTime: 3, // seconds
    initComponent: function () {
        this.title = i18n._("Sessions");
        this.callParent(arguments);
        this.loadData();
    },
    tpl: '<div class="wg-wrapper">' +
        '<div class="row">' +
        '<label style="width: 150px;">' + i18n._('Total Sessions') + ':</label>' +
        '<div class="cell">{totalSessions}</div>' +
        '</div>' +
        '<div class="row">' +
        '<label style="width: 150px;">' + i18n._('Scanned Sessions') + ':</label>' +
        '<div class="cell">{scannedSessions}</div>' +
        '</div>' +
        '<div class="row">' +
        '<label style="width: 150px;">' + i18n._('Scanned TCP Sessions') + ':</label>' +
        '<div class="cell">{scannedTCPSessions}</div>' +
        '</div>' +
        '<div class="row">' +
        '<label style="width: 150px;">' + i18n._('Scanned UDP Sessions') + ':</label>' +
        '<div class="cell">{scannedUDPSessions}</div>' +
        '</div>' +
        '<div class="row">' +
        '<label style="width: 150px;">' + i18n._('Bypassed Sessions') + ':</label>' +
        '<div class="cell">{bypassedSessions}</div>' +
        '</div>' +
        '</div>',
    loadData: function () {
        var me = this;
        rpc.sessionMonitor.getSessionStats(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            this.update(result);
            setTimeout(function () {
                me.loadData();
            }, this.refreshTime * 1000);
        }, this));
    }
});

/* Network Widget */
Ext.define('Ung.dashboard.Network', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    height: 190,
    refreshTime: 10, // refresh timer in seconds
    initComponent: function () {
        this.title = i18n._("Network");
        this.callParent(arguments);
        this.loadData();
    },
    tpl: '<div class="wg-wrapper">' +
        '<tpl for=".">' +
            '<div class="row">' +
            '<label style="width: 80px;">' + i18n._('Interface') + ' {#}:</label>' +
            '<div class="cell">{name} ({physicalDev})<tpl if="isWan"> - WAN</tpl></div>' +
            '</div>' +
        '</tpl>' +
        '</div>',
    data: {},
    loadData: function () {
        var me = this;
        this.update(rpc.networkSettings.interfaces.list);
        setTimeout(function () {
            me.loadData();
        }, this.refreshTime * 1000);
    }
});

/* CPULoad Widget */
Ext.define('Ung.dashboard.CPULoad', {
    extend: 'Ext.panel.Panel',
    displayMode: 'small',
    height: 190,
    cls: 'widget small-widget nopadding',
    layout: 'fit',
    hasStats: true,
    items: [],
    initComponent: function () {
        this.title = i18n._("CPU Load");
        this.items.push({
            xtype: 'cartesian',
            name: 'chart',
            layout: 'fit',
            animation: false,
            store: {
                fields: ['minutes1', 'time'],
                data: []
            },
            axes: [{
                type: 'numeric',
                position: 'left',
                grid: {
                    lineDash: [3, 3]
                },
                minimum: 0,
                fields: ['minutes1'],
                style : {
                    strokeStyle: '#CCC'
                },
                label: {
                    fontSize: 11,
                    color: '#999'
                },
                limits: [{
                    value: 2,
                    line: {
                        strokeStyle: '#999'
                    }
                }]
            }, {
                type: 'category',
                position: 'bottom',
                //maximum: 30,
                hidden: true,
                fields: ['time']
            }],
            series: [{
                type: 'area',
                xField: 'time',
                yField: ['minutes1'],
                style: {
                    stroke: '#666666',
                    lineWidth: 0,
                    fillOpacity: 0.8
                }
            }]
        });
        this.callParent(arguments);
    },
    updateStats: function (stats) {
        var d = new Date(),
            chart = this.down("[name=chart]"),
            data = chart.store.proxy.reader.rawData;

        // set the max value of the chart based on CPU count
        if (stats.oneMinuteLoadAvg < stats.numCpus) {
            chart.getAxes()[0].setMaximum(stats.numCpus + 0.5);
        } else {
            chart.getAxes()[0].setMaximum(stats.oneMinuteLoadAvg + 0.5);
        }

        if (data.length > 30) {
            data.shift();
        }
        data.push({
            time: d.getTime(),
            minutes1: stats.oneMinuteLoadAvg,
            minutes5: stats.fiveMinuteLoadAvg,
            minutes15: stats.fifteenMinuteLoadAvg
        });
        chart.store.loadData(data);
    }
});



Ext.define('Ung.dashboard.Util', {
    singleton: true,
    createTimeChart: function (entry) {
        if (!entry.timeDataColumns) {
            entry.timeDataColumns = [];
        }

        var chart, axesFields = [], axesFieldsTitles = [], series = [],
            legendHint = (entry.timeDataColumns.length > 1) ? this.cartesianLegendHint : '',
            zeroFn = function (val) {
                return (val === null) ? 0 : val;
            },
            timeFn = function (val) {
                return (val === null || val.time === null) ? 0 : i18n.timestampFormat(val);
            },
            storeFields = [{name: 'time_trunc', convert: timeFn}],
            reportDataColumns = [{
                dataIndex: 'time_trunc',
                header: i18n._("Timestamp"),
                width: 130,
                flex: entry.timeDataColumns.length > 2 ? 0 : 1
            }],
            seriesRenderer = null, title, column, i,

            timeStyleButtons = [], timeStyle,
            timeStyles = [
                { name: 'LINE', iconCls: 'icon-line-chart', text: i18n._("Line"), tooltip: i18n._("Switch to Line Chart") },
                { name: 'AREA', iconCls: 'icon-area-chart', text: i18n._("Area"), tooltip: i18n._("Switch to Area Chart") },
                { name: 'BAR_3D', iconCls: 'icon-bar3d-chart', text: i18n._("Bar 3D"), tooltip: i18n._("Switch to Bar 3D Chart") },
                { name: 'BAR_3D_OVERLAPPED', iconCls: 'icon-bar3d-overlapped-chart', text: i18n._("Bar 3D Overlapped"), tooltip: i18n._("Switch to Bar 3D Overlapped Chart") },
                { name: 'BAR', iconCls: 'icon-bar-chart', text: i18n._("Bar"), tooltip: i18n._("Switch to Bar Chart") },
                { name: 'BAR_OVERLAPPED', iconCls: 'icon-bar-overlapped-chart', text: i18n._("Bar Overlapped"), tooltip: i18n._("Switch to Bar Overlapped Chart") }
            ];

        if (!Ext.isEmpty(entry.seriesRenderer)) {
            seriesRenderer =  Ung.panel.Reports.getColumnRenderer(entry.seriesRenderer);
        }

        for (i = 0; i < entry.timeDataColumns.length; i += 1) {
            column = entry.timeDataColumns[i].split(' ').splice(-1)[0];
            title = seriesRenderer ? seriesRenderer(column) : column;
            axesFields.push(column);
            axesFieldsTitles.push(title);
            storeFields.push({name: column, convert: zeroFn, type: 'integer'});
            reportDataColumns.push({
                dataIndex: column,
                header: title,
                width: entry.timeDataColumns.length > 2 ? 60 : 90
            });
        }
        if (!entry.timeStyle) {
            entry.timeStyle = "LINE";
        }
        if (entry.timeStyle.indexOf('OVERLAPPED') !== -1  && entry.timeDataColumns.length <= 1) {
            entry.timeStyle = entry.timeStyle.replace("_OVERLAPPED", "");
        }

        for (i = 0; i < timeStyles.length; i += 1) {
            timeStyle = timeStyles[i];
            timeStyleButtons.push({
                xtype: 'button',
                pressed: entry.timeStyle === timeStyle.name,
                hidden: (timeStyle.name.indexOf('OVERLAPPED') !== -1) && (entry.timeDataColumns.length <= 1),
                name: timeStyle.name,
                iconCls: timeStyle.iconCls,
                text: timeStyle.text,
                tooltip: timeStyle.tooltip,
                handler: Ext.bind(function (button) {
                    entry.timeStyle = button.name;
                    this.loadReportEntry(entry);
                }, this)
            });
        }
        timeStyleButtons.push('-');

        chart = {
            xtype: 'cartesian',
            name: 'chart',
            store: Ext.create('Ext.data.JsonStore', {
                fields: storeFields,
                data: []
            }),
            theme: 'category2',
            border: false,
            animation: false,
            width: '100%',
            height: '100%',
            legend: {
                docked: 'bottom'
            },
            axes: [{
                type: (entry.timeStyle.indexOf('BAR_3D') !== -1) ? 'numeric3d' : 'numeric',
                fields: axesFields,
                position: 'left',
                grid: {
                    lineDash: [3, 3]
                },
                style : {
                    strokeStyle: '#CCC'
                },
                minimum: 0,
                label: {
                    fontSize: 11,
                    color: '#999'
                }
            }, {
                type: (entry.timeStyle.indexOf('BAR_3D') !== -1) ? 'category3d' : 'category',
                fields: 'time_trunc',
                position: 'bottom',
                hidden: true,
                style : {
                    strokeStyle: '#CCC'
                },
                label: {
                    fontSize: 11,
                    color: '#999',
                    x: 0,
                    rotate: {
                        degrees: -90
                    }
                }
            }]
        };

        if (entry.timeStyle === 'LINE') {
            for (i = 0; i < axesFields.length; i += 1) {
                series.push({
                    type: 'line',
                    axis: 'left',
                    title: axesFieldsTitles[i],
                    xField: 'time_trunc',
                    yField: axesFields[i],
                    style: {
                        opacity: 0.90,
                        lineWidth: 1
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
                        renderer: function (tooltip, storeItem, item) {
                            var title = item.series.getTitle();
                            tooltip.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " + i18n._(entry.units) + legendHint);
                        }
                    }
                });
            }
            chart.series = series;
        } else if (entry.timeStyle === 'AREA') {
            for (i = 0; i < axesFields.length; i += 1) {
                series.push({
                    type: 'area',
                    axis: 'left',
                    title: axesFieldsTitles[i],
                    xField: 'time_trunc',
                    yField: axesFields[i],
                    style: {
                        opacity: 0.60,
                        lineWidth: 1
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
                        renderer: function (tooltip, storeItem, item) {
                            var title = item.series.getTitle();
                            tooltip.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " + i18n._(entry.units) + legendHint);
                        }
                    }
                });
            }
            chart.series = series;
        } else if (entry.timeStyle.indexOf('OVERLAPPED') !== -1) {
            for (i = 0; i < axesFields.length; i += 1) {
                series.push({
                    type: (entry.timeStyle.indexOf('BAR_3D') !== -1) ? 'bar3d' : 'bar',
                    axis: 'left',
                    title: axesFieldsTitles[i],
                    xField: 'time_trunc',
                    yField: axesFields[i],
                    style: (entry.timeStyle.indexOf('BAR_3D') !== -1) ? { opacity: 0.70, lineWidth: 1 + 5 * i } : {  opacity: 0.60,  maxBarWidth: Math.max(40 - 2 * i, 2) },
                    tooltip: {
                        trackMouse: true,
                        style: 'background: #fff',
                        renderer: function (tooltip, storeItem, item) {
                            var title = item.series.getTitle();
                            tooltip.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.series.getYField()) + " " + i18n._(entry.units) + legendHint);
                        }
                    }
                });
            }
            chart.series = series;
        } else if (entry.timeStyle.indexOf('BAR') !== -1) {
            chart.series = [{
                type: (entry.timeStyle.indexOf('BAR_3D') !== -1) ? 'bar3d' : 'bar',
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
                    renderer: function (tooltip, storeItem, item) {
                        var title = item.series.getTitle()[Ext.Array.indexOf(item.series.getYField(), item.field)];
                        tooltip.setHtml(title + ' for ' + storeItem.get('time_trunc') + ': ' + storeItem.get(item.field) + " " + i18n._(entry.units) + legendHint);
                    }
                }
            }];
        }
        return chart;
    },
    createPieChart: function (entry) {
        var descriptionFn = function (val, record) {
            var title = (record.get(entry.pieGroupColumn) === null) ? i18n._("none") : record.get(entry.pieGroupColumn),
                value = Ung.panel.Reports.renderValue(record.get("value"), entry);
            return title + ": " + value;
        },
            chart = {
                xtype: 'polar',
                name: 'chart',
                width: '100%',
                height: '100%',
                colors: ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'],
                store: Ext.create('Ext.data.JsonStore', {
                    fields: [
                        {name: 'description', convert: descriptionFn },
                        {name: 'value'}
                    ],
                    data: []
                }),
                border: false,
                insetPadding: {top: 40, left: 40, right: 10, bottom: 10},
                innerPadding: 20,
                legend: {
                    docked: 'right'
                },
                interactions: ['rotate', 'itemhighlight'],
                series: [{
                    type: 'pie',
                    angleField: 'value',
                    rotation: 45,
                    label: {
                        field: 'description',
                        calloutLine: {
                            length: 10,
                            width: 3
                        }
                    }
                }]
            };
        return chart;
    },
    // creates the chart based on entry report type
    createChart: function (entry) {
        if (entry.type === 'PIE_GRAPH') {
            return this.createPieChart(entry);
        }
        if (entry.type === 'TIME_GRAPH' || entry.type === 'TIME_GRAPH_DYNAMIC') {
            return this.createTimeChart(entry);
        }
    }
});


/* ReportEntry Widgets */
Ext.define('Ung.dashboard.ReportEntry', {
    extend: 'Ext.panel.Panel',
    height: 400,
    cls: 'widget small-widget nopadding',
    layout: 'fit',
    border: false,
    entry: null,
    items: null,
    initComponent: function () {
        this.title =  i18n._('Reports') + ' | ' + this.entry.category + ' | ' + this.entry.title;
        this.items = [Ung.dashboard.Util.createChart(this.entry)];

        this.callParent(arguments);

        this.loadData();
    },
    loadData: function () {
        var me = this,
            chart = this.down("[name=chart]");

        Ung.Main.getReportsManager().getDataForReportEntry(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }

            chart.store.loadData(result.list);

            if (this.refreshIntervalSec > 0) {
                setTimeout(function () {
                    me.loadData();
                }, this.refreshIntervalSec * 1000);
            }
        }, this), this.entry, null, null, -1);
    }
});

Ext.define('Ung.dashboard.EventEntry', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'big',
    height: 400,
    initComponent: function () {
        this.title =  i18n._("Events");
        this.items = [];
        this.callParent(arguments);
    }
});
