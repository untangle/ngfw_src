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
        if (Ext.isFunction(this.updateStats)) {
            this.updateStats(stats);
        }
    }
});

/* Grid container for the 1 or more widgets */
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
            '<div class="cell">???</div>' +
        '</div>' +
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
            uptime: _uptime,
            version: rpc.fullVersion
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


Ext.define('Ung.dashboard.Sessions', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    height: 190,
    hasStats: true,
    initComponent: function () {
        this.title = i18n._("Sessions");
        this.callParent(arguments);
    },
    tpl: '<div class="wg-wrapper">' +
        '<div class="row">' +
        '<label style="width: 150px;">' + i18n._('Total Sessions') + ':</label>' +
        '<div class="cell">???</div>' +
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
        '<div class="cell">???</div>' +
        '</div>' +
        '</div>',
    data: {},
    updateStats: function (stats) {
        this.update({
            scannedSessions: stats.uvmSessions,
            scannedTCPSessions: stats.uvmTCPSessions,
            scannedUDPSessions: stats.uvmUDPSessions
        });
    }
});

Ext.define('Ung.dashboard.ReportEntry', {
    extend: 'Ung.dashboard.Widget',
    height: 400,
    displayMode: 'big',
    initComponent: function () {
        this.title =  i18n._("Report");
        this.items = [];
        this.callParent(arguments);
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