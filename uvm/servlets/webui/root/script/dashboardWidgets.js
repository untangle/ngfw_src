/*global
 Ext, Ung, i18n, rpc, setTimeout, clearTimeout, console, document
*/
Ext.define('Ung.dashboard', {
    singleton: true,
    widgets: [],
    loadDashboard: function () {
        Ung.dashboard.Queue.reset();
        var loadSemaphore = rpc.reportsEnabled ? 4 : 1;
        var callback = Ext.bind(function () {
            loadSemaphore -= 1;
            if (loadSemaphore === 0) {
                this.setWidgets();
            }
        }, this);
        rpc.dashboardManager.getSettings(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            this.allWidgets = result.widgets.list;
            callback();
        }, this));
        if (rpc.reportsEnabled) {
            this.loadReportEntries(callback);
            this.loadEventEntries(callback);
            Ung.Main.getReportsManager().getUnavailableApplicationsMap(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
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
            grid, gridEl, type, entry, widget;

        for (i = 0; i < this.allWidgets.length; i += 1) {
            widget = this.allWidgets[i];
            type = widget.type;
            if (type !== "ReportEntry" && type !== "EventEntry") {
                widgetsList.push(Ext.create('Ung.dashboard.' + type, {
                    widgetType: type
                }));
            } else {
                if (rpc.reportsEnabled) {
                    if (type === "ReportEntry") {
                        entry = this.reportsMap[widget.entryId];
                        if (!entry && !entry.enabled) {
                            entry = null;
                        }
                    } else {
                        entry = this.eventsMap[widget.entryId];
                    }
                    if (entry && !this.unavailableApplicationsMap[entry.category]) {
                        widgetsList.push(Ext.create('Ung.dashboard.' + widget.type, {
                            widgetType: widget.type,
                            entry: entry,
                            refreshIntervalSec: widget.refreshIntervalSec,
                            timeframe: widget.timeframe || 0,
                            displayColumns: widget.displayColumns
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
    resetReports: function () {
        this.reportEntries = null;
        this.reportsMap = null;
        this.eventEntries = null;
        this.eventsMap = null;
    },
    loadReportEntries: function (handler) {
        if (!this.reportEntries) {
            Ung.Main.getReportsManager().getReportEntries(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
                this.reportEntries = result.list;
                this.reportsMap = Ung.Util.createRecordsMap(this.reportEntries, "uniqueId");
                handler.call(this);
            }, this));
        } else {
            handler.call(this);
        }
    },
    loadEventEntries: function (handler) {
        if (!this.eventEntries) {
            Ung.Main.getReportsManager().getEventEntries(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
                this.eventEntries = result.list;
                this.eventsMap = Ung.Util.createRecordsMap(this.eventEntries, "uniqueId");
                handler.call(this);
            }, this));
        } else {
            handler.call(this);
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
Ext.define('Ung.dashboard.Queue', {
    singleton: true,
    processing: false,
    paused: false,
    queue: [],
    queueMap: {},
    add: function (widget) {
        if (!this.queueMap[widget.id]) {
            this.queue.push(widget);
            //console.log("Adding: "+widget.title);
            this.process();
        } else {
            //console.log("Prevent Double queuing: " + widget.title);
        }
    },
    addFirst: function (widget) {
        if (!this.queueMap[widget.id]) {
            this.queue.unshift(widget);
            //console.log("Adding first: " + widget.title);
            this.process();
        }
    },
    next: function () {
        //console.log("Finish last started widget.");
        this.processing = false;
        this.process();
    },
    process: function () {
        if (!this.paused && !this.processing && this.queue.length > 0) {
            this.processing = true;
            var widget = this.queue.shift();

            delete this.queueMap[widget.id];

            //console.log("Starting: " + widget.title);
            if (widget.hasRefresh) {
                widget.addCls('loading');
            }
            widget.loadData(widget.afterLoad);
        }
    },
    reset: function () {
        this.queue = [];
        this.queueMap = {};
        this.processing = false;
    },
    pause: function () {
        this.paused = true;
    },
    resume: function () {
        this.paused = false;
        this.process();
    },
    inView: function (widget) {
        // checks if the widget is in viewport
        if (!widget.el) { return; }
        var widgetGeometry = widget.getEl().dom.getBoundingClientRect();
        return (widgetGeometry.top + widgetGeometry.height / 2) > 0 && (widgetGeometry.height / 2 + widgetGeometry.top < window.innerHeight);
    }
});


Ext.define('Ung.dashboard.Widget', {
    extend: 'Ext.panel.Panel',
    cls: 'widget small-widget',
    refreshIntervalSec: 0,
    initialLoadedData: false,
    initComponent: function () {
        if (this.hasRefresh) {
            this.loadingMask = new Ext.LoadMask({
                cls: 'widget-loader',
                msg: '<div class="loader">' +
                        '<svg class="circular" viewBox="25 25 50 50">' +
                        '<circle class="path" cx="50" cy="50" r="20" fill="none" stroke-width="3" stroke-miterlimit="10"/>' +
                        '</svg>' +
                     '</div>',
                target: this
            });
            this.tools = [];
            if (this.widgetType === 'EventEntry' || this.widgetType === 'ReportEntry') {
                this.tools.push({
                    type: 'plus',
                    tooltip: i18n._('Open in Reports'),
                    callback: function (panel) {
                        var entry = panel.entry;
                        Ung.Main.target = "reports."+entry.category + (panel.widgetType=="ReportEntry"?".report.":".event.")+panel.entry.uniqueId;
                        Ung.Main.openReports();
                    }
                });
            }
            this.tools.push({
                type: 'refresh',
                tooltip: i18n._('Refresh'),
                margin: '0 5 0 5',
                callback: function (panel) {
                    if (panel.timeoutId) {
                        clearTimeout(panel.timeoutId);
                    }
                    panel.loadingMask.show();
                    Ung.dashboard.Queue.addFirst(panel);
                }
            });
        }
        this.callParent(arguments);
        if (Ext.isFunction(this.loadData)) {
            Ung.dashboard.Queue.add(this);
        }
    },

    afterLoad: function () {
        Ung.dashboard.Queue.next();
        if (this && this.refreshIntervalSec && this.refreshIntervalSec > 0) {
            this.timeoutId = setTimeout(Ext.bind(function () {
                Ung.dashboard.Queue.add(this);
            }, this), this.refreshIntervalSec * 1000);
        }
    },
    beforeDestroy: function () {
        if (this.timeoutId) {
            clearTimeout(this.timeoutId);
        }
        this.callParent(arguments);
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
    refreshIntervalSec: 600,
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
            '<div class="cell">{applianceModel}</div>' +
        '</div>' +
        '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Uptime') + ':</label>' +
            '<div class="cell">{uptime}</div>' +
        '</div>' +
        '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Version') + ':</label>' +
            '<div class="cell">{version}</div>' +
        '</div>' +
        // '<div class="row">' +
        //     '<label style="width: 90px;">' + i18n._('Subscriptions') + ':</label>' +
        //     '<div class="cell">{subscriptions}</div>' +
        // '</div>' +
        '</div>',
    data: {},
    updateStats: function (stats) {
        var numdays = Math.floor((stats.uptime % 31536000) / 86400),
            numhours = Math.floor(((stats.uptime % 31536000) % 86400) / 3600),
            numminutes = Math.floor((((stats.uptime % 31536000) % 86400) % 3600) / 60),
            _uptime = '';

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
            applianceModel: ( rpc.applianceModel == undefined || rpc.applianceModel == null || rpc.applianceModel == "" ? i18n._("custom") : rpc.applianceModel ),
            uptime: _uptime,
            version: rpc.fullVersion,
            subscriptions: this.subscriptions
        });
    },
    loadData: function (handler) {
        rpc.licenseManager.validLicenseCount(Ext.bind(function (result, exception) {
            handler.call(this);
            if (Ung.Util.handleException(exception)) {
                return;
            }
            this.subscriptions = result;
        }, this));
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
            '<button class="wg-button" onclick="Ung.Main.showHosts();">Show Hosts</button> ' +
            '<button class="wg-button" onclick="Ung.Main.showDevices();">Show Devices</button>' +
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
    refreshIntervalSec: 10, // seconds
    initComponent: function () {
        this.title = i18n._("Sessions");
        this.callParent(arguments);
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
    loadData: function (handler) {
        rpc.sessionMonitor.getSessionStats(Ext.bind(function (result, exception) {
            handler.call(this);
            if (Ung.Util.handleException(exception)) {
                return;
            }
            this.update(result);
        }, this));
    }
});

/* Network Widget */
Ext.define('Ung.dashboard.Network', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    height: 190,
    refreshIntervalSec: 180, // refresh timer in seconds
    initComponent: function () {
        this.title = i18n._("Network");
        this.callParent(arguments);
    },
    tpl: '<div class="wg-wrapper">' +
        '<tpl for=".">' +
            '<div class="row">' +
            '<label style="width: 80px;">' + i18n._('Interface') + ' {#}:</label>' +
            '<div class="cell">{name} {physicalDev}<tpl if="isWan"> - WAN</tpl></div>' +
            '</div>' +
        '</tpl>' +
        '</div>',
    data: {},
    loadData: function (handler) {
        rpc.networkManager.getNetworkSettings(Ext.bind(function (result, exception) {
            handler.call(this);
            if (Ung.Util.handleException(exception)) {
                return;
            }
            var allInterfaces = result.interfaces.list,
                addressedInterfaces = [], i;

            for (i = 0; i < allInterfaces.length; i += 1) {
                if (!allInterfaces[i].disabled) {
                    addressedInterfaces.push(allInterfaces[i]);
                }
            }
            this.update(addressedInterfaces);
        }, this));
    }
});

/* CPULoad Widget */
Ext.define('Ung.dashboard.CPULoad', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    height: 190,
    layout: 'fit',
    hasStats: true,
    items: [],
    initComponent: function () {
        this.title = i18n._("CPU Load");
        this.items.push({
            xtype: 'cartesian',
            name: 'chart',
            border: false,
            animation: false,
            width: '100%',
            height: '100%',
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
            data = chart.getStore().getProxy().reader.rawData;

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
            legendHint = (entry.timeDataColumns.length > 1) ? "<br/>" + i18n._('Hint: Click this label on the legend to hide this series') : '',
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
            colors: (entry.colors !== null && entry.colors.length > 0) ? entry.colors : ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'],
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
                            title = item.series.getTitle();
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
                            title = item.series.getTitle();
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
                            title = item.series.getTitle();
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
                        title = item.series.getTitle()[Ext.Array.indexOf(item.series.getYField(), item.field)];
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
        }, noDataSprite = Ext.create("Ext.draw.sprite.Text", {
            type: 'text',
            hidden: true,
            text: i18n._("Not enough data to generate the chart."),
            fontSize: 12,
            fillStyle: '#FF0000',
            x: 10,
            y: 20
        }),
            chart = {
                xtype: 'polar',
                name: 'chart',
                width: '100%',
                height: '100%',
                colors: (entry.colors !== null && entry.colors.length > 0) ? entry.colors : ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'],
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
                sprites: [ noDataSprite],
                noDataSprite: noDataSprite,
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
    createTextReport: function (entry) {
        return {
            xtype: 'component',
            name: "chart",
            margin: 10,
            html: ''
        };
    },
    // creates the chart based on entry report type
    createChart: function (entry) {
        if (entry.type === 'PIE_GRAPH') {
            return this.createPieChart(entry);
        }
        if (entry.type === 'TIME_GRAPH' || entry.type === 'TIME_GRAPH_DYNAMIC') {
            return this.createTimeChart(entry);
        }
        if (entry.type === 'TEXT') {
            return this.createTextReport(entry);
        }
    }
});


/* ReportEntry Widgets */
Ext.define('Ung.dashboard.ReportEntry', {
    extend: 'Ung.dashboard.Widget',
    height: 400,
    cls: 'widget small-widget nopadding',
    layout: 'fit',
    border: false,
    entry: null,
    items: null,
    hasRefresh: true,
    initComponent: function () {
        this.title =  i18n._('Reports') + ' | ' + this.entry.category + ' | ' + this.entry.title;
        this.items = [Ung.dashboard.Util.createChart(this.entry)];
        this.callParent(arguments);
    },
    loadData: function (handler) {
        Ung.Main.getReportsManager().getDataForReportEntry(Ext.bind(function (result, exception) {
            this.loadingMask.hide();
            this.removeCls('loading');

            handler.call(this);
            if (Ung.Util.handleException(exception)) {
                return;
            }
            if (this === null || !this.rendered) {
                return;
            }
            var data = result.list, chart = this.down("[name=chart]"), column;
            if (this.entry.type === 'PIE_GRAPH') {
                var topData = data;
                if (this.entry.pieNumSlices && data.length > this.entry.pieNumSlices) {
                    topData = [];
                    var others = {value: 0};
                    others[this.entry.pieGroupColumn] = i18n._("Others");
                    for (i = 0; i < data.length; i += 1) {
                        if (i < this.entry.pieNumSlices) {
                            topData.push(data[i]);
                        } else {
                            others.value += data[i].value;
                        }
                    }
                    others.value = Math.round(others.value * 10) / 10;
                    topData.push(others);
                }
                if (topData.length === 0) {
                    chart.noDataSprite.show();
                } else {
                    chart.noDataSprite.hide();
                }

                chart.getStore().loadData(topData);
            } else if (this.entry.type === 'TIME_GRAPH_DYNAMIC') {
                var columnsMap = {}, columns = [], values = {};
                for (i = 0; i < data.length; i += 1) {
                    for (column in data[i]) {
                        columnsMap[column] = true;
                    }
                }
                for (column in columnsMap) {
                    if (column !== 'time_trunc') {
                        columns.push(column);
                    }
                }
                this.entry.timeDataColumns = columns;
                this.removeAll();
                this.add(Ung.dashboard.Util.createChart(this.entry));
                this.down("[name=chart]").getStore().loadData(data);
            } else if (this.entry.type === 'TIME_GRAPH') {
                chart.getStore().loadData(data);
            } else if (this.entry.type === 'TEXT') {
                var infos = [], reportData = [];
                if (data.length > 0 && this.entry.textColumns !== null) {
                    var value, i;
                    for (i = 0; i < this.entry.textColumns.length; i += 1) {
                        column = this.entry.textColumns[i].split(" ").splice(-1)[0];
                        value = Ext.isEmpty(data[0][column]) ? 0 : data[0][column];
                        infos.push(value);
                        reportData.push({data: column, value: value});
                    }
                }
                chart.update(Ext.String.format.apply(Ext.String.format, [i18n._(this.entry.textString)].concat(infos)));
            }
        }, this), this.entry, this.timeframe, -1);
    }
});

Ext.define('Ung.dashboard.EventEntry', {
    extend: 'Ung.dashboard.Widget',
    height: 400,
    cls: 'widget small-widget nopadding',
    layout: 'fit',
    border: false,
    entry: null,
    displayColumns: null,
    items: null,
    hasRefresh: true,
    initComponent: function () {
        this.title =  i18n._('Events') + ' | ' + this.entry.category + ' | ' + this.entry.title;
        this.items = [this.buildGrid()];
        this.callParent(arguments);
        this.gridEvents = this.down("grid[name=gridEvents]");
    },
    buildGrid: function () {
        var tableConfig = Ext.clone(Ung.TableConfig.getConfig(this.entry.table)), i;
        if (!tableConfig) {
            console.log('Warning: table "' + this.entry.table + '" is not defined');
            tableConfig = {
                fields: [],
                columns: []
            };
        } else {
            if (!this.displayColumns || this.displayColumns.length === 0) {
                this.displayColumns = this.entry.defaultColumns || [];
            }
            var columnsNames = {}, col;
            for (i = 0; i < tableConfig.columns.length; i += 1) {
                col = tableConfig.columns[i].dataIndex;
                columnsNames[col] = true;
                col = tableConfig.columns[i];
                if (this.displayColumns.length > 0 && this.displayColumns.indexOf(col.dataIndex) < 0) {
                    col.hidden = true;
                }
            }
        }

        return {
            xtype: 'grid',
            name: 'gridEvents',
            reserveScrollbar: true,
            header: false,
            viewConfig: {
                enableTextSelection: true
            },
            store:  Ext.create('Ext.data.Store', {
                fields: tableConfig.fields,
                data: [],
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json'
                    }
                }
            }),
            columns: tableConfig.columns
        };
    },
    loadData: function (handler) {
        var me = this;
        Ung.Main.getReportsManager().getEventsForTimeframeResultSet(Ext.bind(function (result, exception) {
            this.loadingMask.hide();
            this.removeCls('loading');
            handler.call(this);
            if (Ung.Util.handleException(exception)) {
                return;
            }
            if (this === null || !this.rendered) {
                return;
            }
            result.getNextChunk(function (result2, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
                var store = me.gridEvents.getStore();
                if (me === null || !me.rendered) {
                    return;
                }
                store.getProxy().setData(result2.list);
                store.load();
            }, 1000);
        }, this), this.entry, null, this.timeframe, 14);
    }
});
