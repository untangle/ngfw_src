/*global
 Ext, Ung, i18n, rpc, setTimeout, clearTimeout, console, window, document, Highcharts
 */
Ext.define('Ung.dashboard', {
    singleton: true,
    widgetsList: [],
    reportEntriesModified: false,
    timeZoneChanged: false,
    loadDashboard: function () {
        Ung.dashboard.Queue.reset();

        Ext.Deferred.pipeline([
            this.getSettings,
            this.getReportEntries,
            this.getApps
        ]).then(Ext.bind(function () {
            this.setWidgets();
        }, this), function (exception) {
            Ung.Util.handleException(exception);
        });
    },
    getSettings: function () {
        var deferred = new Ext.Deferred();
        rpc.dashboardManager.getSettings(function (result, exception) {
            if (exception) { deferred.reject(exception); }
            Ung.dashboard.allWidgets = result.widgets.list.filter(function (widget) { return widget.enabled; });
            // if dashboard has map widget enabled and map not loaded then load the map
            if (Ung.dashboard.allWidgets.filter(function (widget) { return widget.type === 'MapDistribution'; }).length === 1 && !Highcharts.maps['custom/world-highres']) {
                Ext.Ajax.request({
                    url: '/highcharts/maps/world-highres.js',
                    success: function (response, opts) {
                        Ext.util.JSON.decode(response.responseText);
                        deferred.resolve();
                    },
                    failure: function (response, opts) {
                        deferred.reject(response);
                    }
                });
            } else {
                deferred.resolve();
            }
        });
        return deferred.promise;
    },
    getReportEntries: function () {
        var deferred = new Ext.Deferred();
        if (rpc.reportsEnabled && !Ung.dashboard.reportEntries) {
            Ung.Main.getReportsManager().getReportEntries(function (result, exception) {
                if (exception) { deferred.reject(exception); }
                Ung.dashboard.reportEntries = result.list;
                Ung.dashboard.reportsMap = Ung.Util.createRecordsMap(result.list, 'uniqueId');
                deferred.resolve();
            });
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    },
    getApps: function () {
        var deferred = new Ext.Deferred();
        if (rpc.reportsEnabled) {
            Ung.Main.getReportsManager().getUnavailableApplicationsMap(function (result, exception) {
                if (exception) { deferred.reject(exception); }
                Ung.dashboard.unavailableApplicationsMap = result.map;
                deferred.resolve();
            });
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    },

    onScrollChange: function () {
        var i, widget;
        for (i = 0; i < this.widgetsList.length; i += 1) {
            widget = this.widgetsList[i];
            if (Ext.isFunction(widget.loadData)) {
                if (widget.toQueue && this.inView(widget)) {
                    //console.log('FOUND PENDING QUEUE', widget.title);
                    widget.toQueue = false;
                    Ung.dashboard.Queue.add(widget);
                }
            }
        }
    },
    inView: function (widget) {
        // checks if the widget is in viewport
        if (!widget.getEl()) {
            return false;
        }
        var widgetGeometry = widget.getEl().dom.getBoundingClientRect();
        return (widgetGeometry.top + widgetGeometry.height / 2) > 0 && (widgetGeometry.height / 2 + widgetGeometry.top < window.innerHeight);
    },
    debounce: function (fn, delay) {
        var timer = null;
        var me = this;
        return function () {
            var context = me, args = arguments;
            clearTimeout(timer);
            timer = setTimeout(function () {
                fn.apply(context, args);
            }, delay);
        };
    },
    setWidgets: function () {
        this.widgetsList = [];
        this.dashboardPanel.removeAll();
        var i, j, type, entry, widget;

        //this.widgetsList.push(Ext.create('Ung.dashboard.Map'));

        for (i = 0; i < this.allWidgets.length; i += 1) {
            widget = this.allWidgets[i];
            type = widget.type;
            if (type !== "ReportEntry") {
                this.widgetsList.push(Ext.create('Ung.dashboard.' + type, {
                    widgetType: type,
                    entryId: widget.entryId
                }));
            } else {
                if (rpc.reportsEnabled) {
                    if (type === "ReportEntry") {
                        entry = this.reportsMap[widget.entryId];
                        if (entry && !entry.enabled) {
                            entry = null;
                        }
                    }
                    if (entry && !this.unavailableApplicationsMap[entry.category]) {
                        this.widgetsList.push(Ext.create('Ung.dashboard.' + widget.type, {
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

        for (j = 0; j < this.widgetsList.length; j += 1) {
            this.widgetsList[j].cls = 'small';
            if (this.widgetsList[j].widgetType === 'NetworkLayout') {
                this.widgetsList[j].cls = 'large';
            }
            if (this.widgetsList[j].entry) {
                if (this.widgetsList[j].entry.type === 'PIE_GRAPH') {
                    if (this.widgetsList[j].entry.pieStyle.indexOf('COLUMN') >= 0) {
                        this.widgetsList[j].cls = 'large';
                    } else {
                        this.widgetsList[j].cls = 'medium';
                    }
                } else {
                    this.widgetsList[j].cls = 'large';
                }
            }
        }
        // do not fully extend last widget if the widget is not large
        if (this.widgetsList[this.widgetsList.length - 1] && !this.widgetsList[this.widgetsList.length - 1].hasCls('large')) {
            this.dashboardPanel.addCls('last-not-extended');
        }

        this.dashboardPanel.add(this.widgetsList);

        if (!this.scrollInitialized) {
            this.scrollInitialized = true;
            this.dashboardContainer.getEl().on('scroll', this.debounce(this.onScrollChange, 500));
            Ext.on('resize', this.debounce(this.onScrollChange, 500));
        }
    },
    resetReports: function () {
        this.reportEntries = null;
        this.reportsMap = null;
    },
    updateStats: function () {
        var i, widget;
        for (i = 0; i < this.widgetsList.length; i += 1) {
            widget = this.widgetsList[i];
            if (widget.hasStats) {
                widget.updateStats(Ung.Main.stats);
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
        } /* else { console.log("Prevent Double queuing: " + widget.title); } */
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

            if (!widget.dataFirstLoaded || Ung.dashboard.inView(widget)) {
                widget.dataFirstLoaded = true;
                widget.addCls('loading');

                //console.log('PROCESS...', widget.title);
                widget.loadData(widget.afterLoad);
            } else {
                //console.log('DELAYED', widget.title);
                widget.toQueue = true;
                Ung.dashboard.Queue.next();
            }
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
    }
});

Ext.define('Ung.dashboard.Widget', {
    extend: 'Ext.panel.Panel',
    cls: 'widget init',
    refreshIntervalSec: 0,
    height: 320,
    initComponent: function () {
        if (this.hasRefresh) {
            this.tools = [];
            if (this.widgetType === 'ReportEntry') {
                if (this.entry.type !== 'EVENT_LIST') {
                    this.tools.push({
                        type: 'widget-tool',
                        cls: 'widget-tool open',
                        renderTpl: '<i class="material-icons">file_download</i>',
                        callback: function (panel) {
                            if (!panel.chart) {
                                return;
                            }
                            panel.chart.exportChart();
                        }
                    });
                }
                this.tools.push({
                    type: 'widget-tool',
                    cls: 'widget-tool open',
                    renderTpl: '<i class="material-icons">call_made</i>',
                    callback: function (panel) {
                        Ung.Main.openReports(panel.entry);
                    }
                });
            }
            this.tools.push({
                type: 'widget-tool',
                cls: 'widget-tool refresh',
                renderTpl: '<i class="material-icons">refresh</i>',
                margin: '0 5 0 5',
                callback: function (panel) {
                    if (panel.timeoutId) {
                        clearTimeout(panel.timeoutId);
                    }
                    panel.dataFirstLoaded = false; // to force loading data when manual refresh
                    Ung.dashboard.Queue.addFirst(panel);
                }
            });
        }
        this.callParent(arguments);
    },
    afterRender: function () {
        if (Ext.isFunction(this.loadData)) {
            Ung.dashboard.Queue.add(this);
        }
        this.callParent(arguments);
        // update stats only after widget is rendered to avoid errors
        if (this.hasStats && Ung.Main.stats) {
            this.updateStats(Ung.Main.stats);
        }
    },

    afterLoad: function () {
        this.header = true;
        try {
            this.removeCls('init');
            this.removeCls('loading');
        } catch (ignore) {
        }

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

/* Information Widget */
Ext.define('Ung.dashboard.Information', {
    extend: 'Ung.dashboard.Widget',
    hasStats: true,
    refreshIntervalSec: 600,
    /*
    loader: {
        url: 'script/widget-template/information.tpl',
        autoLoad: true,
        renderer: function (loader, response, active) {
            this.tpl = new Ext.XTemplate(response.responseText);
            this.update();
            return true;
        }
    },
    */
    initComponent: function () {
        this.title = '<h3>' + i18n._('Information') + '</h3>';
        this.tpl = '<div class="wg-wrapper information">' +
            '<p class="info-hostname">{hostname}<br/> <span>' + i18n._('version') + ': {version}</span></p>' +
            '<p class="info-uptime"><i class="material-icons" style="vertical-align: middle; font-size: 16px; margin-right: 5px;">access_time</i> <span style="vertical-align: middle;">' + i18n._('uptime') + ': {uptime}</span></p>' +
            '<div class="info-hardware">' +
            '<div class="row">' +
            '<label style="width: 90px;">' + i18n._('Server') + ':</label>' +
            '<div class="cell">{applianceModel}</div>' +
            '</div>' +
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
            '</div>' +
            '</div>' +
            '<div class="mask init-mask"><i class="material-icons">widgets</i><p>' + i18n._("Information") + '</p></div>';
        this.callParent(arguments);
    },
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
            applianceModel: (rpc.applianceModel === undefined || rpc.applianceModel === null || rpc.applianceModel === '' ? i18n._('custom') : rpc.applianceModel),
            uptime: _uptime,
            version: rpc.fullVersion,

            cpuCount: stats.numCpus,
            cpuType: stats.cpuModel,
            architecture: stats.architecture,
            totalMemory: Ung.Util.bytesToMBs(stats.MemTotal),
            totalDisk: Math.round(stats.totalDiskSpace / 10000000) / 100
        });

        this.removeCls('init');
    }
});

/* Map Widget */
Ext.define('Ung.dashboard.MapDistribution', {
    extend: 'Ung.dashboard.Widget',
    hasRefresh: true,
    refreshIntervalSec: 20,
    userCls: 'large',
    initComponent: function () {
        this.title = '<h3>' + i18n._('Map Distribution') + '</h3>';
        this.callParent(arguments);
    },
    data: {},
    tpl: '<div class="wg-wrapper no-padding">' +
        '<div class="map-chart" style="height: 100%; width: 100%;"></div>' +
        '</div>' +
        '<div class="mask init-mask"><i class="material-icons">widgets</i><p>' + i18n._("Map distribution") + '</p></div>',
    listeners: {
        afterrender: function (widget) {
            widget.chart = Ung.charts.mapChart(widget.getEl().query('.map-chart')[0]);
        }
    },
    loadData: function (handler) {
        var data = [], i;
        Ung.Main.getGeographyManager().getGeoSessionStats(Ext.bind(function (result, exception) {
            handler.call(this);
            if (Ung.Util.handleExceptionToast(exception)) {
                return;
            }
            for (i = 0; i < result.length; i += 1) {
                data.push({
                    lat: result[i].latitude,
                    lon: result[i].longitude,
                    z: Math.round(result[i].kbps * 100) / 100,
                    country: result[i].country,
                    sessionCount: result[i].sessionCount
                });
            }
            this.chart.series[1].setData(data, true, false);
        }, this));
    }
});


/* Resources Widget */
Ext.define('Ung.dashboard.Resources', {
    extend: 'Ung.dashboard.Widget',
    hasStats: true,
    initComponent: function () {
        this.title = '<h3>' + i18n._('Resources') + '</h3>';
        this.tpl = '<div class="wg-wrapper flex" style="padding: 0 30px; align-items: initial;">' +
            '<p style="margin: 5px 0; font-weight: 600;">' + i18n._('Memory') + '</p>' +
            '<div>' +
            '<div class="wg-progress"><div class="wg-progress-bar"><span style="left: -{percentFreeMemory}%;"></span></div><p>{totalMemory} MB</p></div>' +
            '<div class="wg-progress-vals"><span style="color: #BB9600; font-weight: 600;">{usedMemory} MB</span> ' + i18n._('used') + ' <em>({percentUsedMemory}%)</em></div>' +
            '<div class="wg-progress-vals"><span style="color: #555; font-weight: 600;">{freeMemory} MB</span> ' + i18n._('free') + ' <em>({percentFreeMemory}%)</em></div>' +
            '</div>' +
            '<div class="swap-usage" style="border-top: 1px #EEE solid; border-bottom: 1px #EEE solid; margin: 5px 0; padding: 0 0 10px 0;">' +
            '<p style="margin: 5px 0; font-weight: 600;">' + i18n._('Swap') + '</p>' +
            '<div>' +
            '<div class="wg-progress"><div class="wg-progress-bar"><span style="left: -{percentFreeSwap}%;"></span></div><p>{totalSwap} MB</p></div>' +
            '<div class="wg-progress-vals"><span style="color: #BB9600; font-weight: 600;">{usedSwap} MB</span> ' + i18n._('used') + ' <em>({percentUsedSwap}%)</em></div>' +
            '<div class="wg-progress-vals"><span style="color: #555; font-weight: 600;">{freeSwap} MB</span> ' + i18n._('free') + ' <em>({percentFreeSwap}%)</em></div>' +
            '</div>' +
            '</div>' +
            '<p style="margin: 5px 0; font-weight: 600;">' + i18n._('Disk') + '</p>' +
            '<div>' +
            '<div class="wg-progress"><div class="wg-progress-bar"><span style="left: -{percentFreeDisk}%;"></span></div><p>{totalDisk} GB</p></div>' +
            '<div class="wg-progress-vals"><span style="color: #BB9600; font-weight: 600;">{usedDisk} GB</span> ' + i18n._('used') + ' <em>({percentUsedDisk}%)</em></div>' +
            '<div class="wg-progress-vals"><span style="color: #555; font-weight: 600;">{freeDisk} GB</span> ' + i18n._('free') + ' <em>({percentFreeDisk}%)</em></div>' +
            '</div>' +
            '</div>' +
            '<div class="mask init-mask"><i class="material-icons">widgets</i><p>' + i18n._('Resources') + '</p></div>';
        this.callParent(arguments);
    },
    data: {},
    updateStats: function (stats) {
        this.update({
            totalMemory: Ung.Util.bytesToMBs(stats.MemTotal),
            usedMemory: Ung.Util.bytesToMBs(stats.MemTotal - stats.MemFree),
            freeMemory: Ung.Util.bytesToMBs(stats.MemFree),
            percentUsedMemory: parseFloat((1 - parseFloat(stats.MemFree / stats.MemTotal)) * 100).toFixed(1),
            percentFreeMemory: parseFloat(stats.MemFree / stats.MemTotal * 100).toFixed(1),

            totalSwap: Ung.Util.bytesToMBs(stats.SwapTotal),
            usedSwap: Ung.Util.bytesToMBs(stats.SwapTotal - stats.SwapFree),
            freeSwap: Ung.Util.bytesToMBs(stats.SwapFree),
            percentUsedSwap: parseFloat((1 - parseFloat(stats.SwapFree / stats.SwapTotal)) * 100).toFixed(1),
            percentFreeSwap: parseFloat(stats.SwapFree / stats.SwapTotal * 100).toFixed(1),

            totalDisk: Math.round(stats.totalDiskSpace / 10000000) / 100,
            usedDisk: Math.round((stats.totalDiskSpace - stats.freeDiskSpace) / 10000000) / 100,
            freeDisk: Math.round(stats.freeDiskSpace / 10000000) / 100,
            percentUsedDisk: parseFloat((1 - parseFloat(stats.freeDiskSpace / stats.totalDiskSpace)) * 100).toFixed(1),
            percentFreeDisk: parseFloat(stats.freeDiskSpace / stats.totalDiskSpace * 100).toFixed(1)
        });
        this.removeCls('init');
        // assuming total swap = 0 // e.g. ARM based
        if (this.getEl() && !stats.SwapTotal) {
            this.getEl().query('.swap-usage')[0].remove();
        }
    }
});

/* CPULoad Widget */
Ext.define('Ung.dashboard.CPULoad', {
    extend: 'Ung.dashboard.Widget',
    layout: 'fit',
    hasStats: true,
    cpuDataStorage: [], // used to keep data after dashboard reloads
    initComponent: function () {
        this.title = '<h3>' + i18n._("CPU Load") + '</h3>';
        this.callParent(arguments);
    },
    tpl: '<div class="wg-wrapper no-padding cpuload">' +
            '<div class="cpu-line-chart" style="height: 135px; width:100%; margin: 0 auto;"></div>' +
            '<div class="cpu-gauge-chart" style="height: 135px; width:100%; margin: 0 auto;"></div>' +
            '<div class="cpuLoadVal"></div>' +
        '</div>' +
        '<div class="mask init-mask"><i class="material-icons">widgets</i><p>' + i18n._("CPU Load") + '</p></div>',
    data: {},
    lineChart: null,
    gaugeChart: null,
    listeners: {
        'afterrender': function (widget) {
            widget.lineChart = Ung.charts.cpuLineChart(widget.getEl().query('.cpu-line-chart')[0]);
            widget.gaugeChart = Ung.charts.cpuGaugeChart(widget.getEl().query('.cpu-gauge-chart')[0]);
            if (Ung.Main.stats) {
                widget.updateStats(Ung.Main.stats);
            }
        }
    },
    updateStats: function (stats) {
        var medLimit = stats.numCpus + 1;
        var highLimit = stats.numCpus + 4;
        var loadLabel = i18n._('low');

        if (this.lineChart !== null && this.gaugeChart !== null) {
            this.lineChart.yAxis[0].update({
                minRange: stats.numCpus
            });

            this.gaugeChart.yAxis[0].update({
                max: highLimit + 1,
                plotBands: [{
                    from: 0,
                    to: medLimit,
                    color: 'rgba(112, 173, 112, 1)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }, {
                    from: medLimit,
                    to: highLimit,
                    color: 'rgba(255, 255, 0, 1)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }, {
                    from: highLimit,
                    to: highLimit + 1,
                    color: 'rgba(255, 0, 0, 1)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }]
            });

            this.removeCls('init');

            var me = this;
            setTimeout(function () {
                me.lineChart.reflow();
                me.gaugeChart.reflow();
            }, 100);
            //this.loaded = true;
        }


        if (stats.oneMinuteLoadAvg > medLimit) {
            loadLabel = i18n._('medium');
        }
        if (stats.oneMinuteLoadAvg > highLimit) {
            loadLabel = i18n._('high');
        }
        if (Ext.select('.cpuLoadVal', this).elements[0]) {
            var loadValElement = Ext.select('.cpuLoadVal', this).elements[0];
            loadValElement.removeCls('high');
            loadValElement.removeCls('medium');
            loadValElement.addCls(loadLabel).setHtml(stats.oneMinuteLoadAvg + '<br/><span>' + loadLabel + '</span>');
        }

        if (this.lineChart !== null && this.gaugeChart !== null) {
            this.lineChart.series[0].addPoint([this.lineChart.series[0].data[this.lineChart.series[0].data.length - 1].x + 3000, stats.oneMinuteLoadAvg], true, true);
            this.gaugeChart.series[0].points[0].update(stats.oneMinuteLoadAvg <= highLimit + 1 ? stats.oneMinuteLoadAvg : highLimit + 1, true);
        }

    }
});

/* Network Information Widget */
Ext.define('Ung.dashboard.NetworkInformation', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    refreshIntervalSec: 10,
    hasStats: true,
    initComponent: function () {
        this.title = '<h3 style="padding: 5px 0;">' + i18n._('Network Information') + '</h3>';
        this.tpl = '<div class="wg-wrapper no-padding flex">' +
            '<div class="info-box" style="border-bottom: 1px #EEE solid;">' +
            '<div class="info-item">' + i18n._('Currently Active') + '<br/><span>{hosts.activeHosts}</span></div>' +
            '<div class="info-item">' + i18n._('Maximum Active') + '<br/><span>{hosts.maxActiveHosts}</span></div>' +
            '<div class="info-item">' + i18n._('Known Devices') + '<br/><span>{hosts.knownDevices}</span></div>' +
            '<div class="info-actions">' +
            '<button class="wg-button" onclick="Ung.Main.showHosts();" style="flex: 1;">' + i18n._('Hosts') + '</button>' +
            '<button class="wg-button" onclick="Ung.Main.showDevices();" style="flex: 1;">' + i18n._('Devices') + '</button>' +
            '</div>' +
            '</div>' +
            '<div class="info-box">' +
            '<div class="info-item">' + i18n._('Total Sessions') + '<br/><span>{sessions.totalSessions}</span></div>' +
            '<div class="info-item">' + i18n._('Scanned Sessions') + '<br/><span>{sessions.scannedSessions}</span></div>' +
            '<div class="info-item">' + i18n._('Bypassed Sessions') + '<br/><span>{sessions.bypassedSessions}</span></div>' +
            '<div class="info-actions">' +
            '<button class="wg-button" onclick="Ung.Main.showSessions();" style="flex: 1;">' + i18n._('Sessions') + '</button> ' +
            '</div>' +
            '</div>' +
            '</div>' +
            '<div class="mask init-mask"><i class="material-icons">widgets</i><p>' + i18n._('Network Information') + '</p></div>';
        this.data = {
            hosts: {
                activeHosts: 0,
                maxActiveHosts: 0,
                knownDevices: 0
            },
            sessions: {
                totalSessions: 0,
                scannedSessions: 0,
                bypassedSessions: 0
            }
        };
        this.callParent(arguments);
    },
    updateStats: function (stats) {
        this.data.hosts = {
            activeHosts: stats.activeHosts,
            maxActiveHosts: stats.maxActiveHosts,
            knownDevices: stats.knownDevices
        };
        this.update(this.data);
    },
    loadData: function (handler) {
        rpc.sessionMonitor.getSessionStats(Ext.bind(function (result, exception) {
            handler.call(this);
            if (Ung.Util.handleException(exception)) {
                return;
            }
            if (this === null || !this.rendered) {
                return;
            }
            this.data.sessions = result;
            this.update(this.data);
        }, this));
    }
});

/* Network Layout Widget */
Ext.define('Ung.dashboard.NetworkLayout', {
    extend: 'Ung.dashboard.Widget',
    hasStats: true,
    refreshIntervalSec: 0,
    hasRefresh: true,
    interfacesLoaded: false,
    initComponent: function () {
        this.title = '<h3>' + i18n._("Network Layout") + '</h3>';
        this.data = {
            externalInterfaces: null,
            internalInterfaces: null
        };
        this.tpl = '<div class="wg-wrapper network-intf">' +
            '<div class="wrap">' +
            '<div class="external">' +
            '<img src="/skins/default/images/admin/icons/interface-cloud.png" style="margin: 5px auto; height: 30px; display: block;"/>' +
            '<tpl for="externalInterfaces">' +
            '<div class="iface" id="interface_{id}">' +
            '<p class="name">{name}</p>' +
            '<div class="speeds" style="display: inline-block; text-align: left;">' +
            '<span class="up">{tx} kB/s</span>' +
            '<span class="down">{rx} kB/s</span>' +
            '</div>' +
            '<br/><span class="connection ext"></span>' +
            '</div>' +
            '</tpl>' +
            '</div>' +
            '</div>' +
            '<div class="wire"></div>' +
            '<div class="wrap">' +
            '<div class="internal">' +
            '<tpl for="internalInterfaces">' +
            '<div class="iface" id="interface_{id}">' +
            '<span class="connection int"></span><br/>' +
            '<div class="speeds" style="display: inline-block; text-align: left;">' +
            '<span class="up">{tx} kB/s</span>' +
            '<span class="down">{rx} kB/s</span>' +
            '</div>' +
            '<p class="name">{name}</p>' +
            '<p class="devs"></p>' +
            '</div>' +
            '</tpl>' +
            '</div>' +
            '</div>' +
            '</div>' +
            '<div class="mask init-mask"><i class="material-icons">widgets</i><p>' + i18n._('Network Layout') + '</p></div>';
        this.callParent(arguments);
    },
    updateStats: function (stats) {
        if (!this.interfacesLoaded) {
            return;
        }

        var me = this;
        var interfaceEl, i, interfaceDevicesMap = [], device;

        if (me.data && me.data.externalInterfaces) {
            for (i = 0; i < this.data.externalInterfaces.length; i += 1) {
                interfaceEl = document.querySelector('#interface_' + this.data.externalInterfaces[i].id);
                if (interfaceEl && stats['interface_' + this.data.externalInterfaces[i].id + '_txBps']) {
                    interfaceEl.querySelector('.up').innerHTML = Math.round(stats['interface_' + this.data.externalInterfaces[i].id + '_txBps'] / 1024) + ' kB/s';
                    interfaceEl.querySelector('.down').innerHTML = Math.round(stats['interface_' + this.data.externalInterfaces[i].id + '_rxBps'] / 1024) + ' kB/s';
                }
            }
        }

        if (me.data && me.data.internalInterfaces) {
            rpc.deviceTable.getDevices(Ext.bind(function (res, ex) {
                if (Ung.Util.handleException(ex)) {
                    return;
                }
                for (i = 0; i < res.list.length; i += 1) {
                    device = res.list[i];
                    if (interfaceDevicesMap[device.lastSeenInterfaceId] >= 0) {
                        interfaceDevicesMap[device.lastSeenInterfaceId] += 1;
                    } else {
                        interfaceDevicesMap[device.lastSeenInterfaceId] = 1;
                    }
                }

                for (i = 0; i < me.data.internalInterfaces.length; i += 1) {
                    interfaceEl = document.querySelector('#interface_' + me.data.internalInterfaces[i].id);
                    if (interfaceEl) {
                        interfaceEl.querySelector('.up').innerHTML = Math.round(stats['interface_' + me.data.internalInterfaces[i].id + '_rxBps'] / 1024) + ' kB/s';
                        interfaceEl.querySelector('.down').innerHTML = Math.round(stats['interface_' + me.data.internalInterfaces[i].id + '_txBps'] / 1024) + ' kB/s';
                        interfaceEl.querySelector('.devs').innerHTML = interfaceDevicesMap[me.data.internalInterfaces[i].id] || 0;
                    }
                }
            }));
        }

    },
    loadData: function (handler) {
        var me = this;
        this.data.externalInterfaces = [];
        this.data.internalInterfaces = [];
        rpc.networkManager.getNetworkSettings(Ext.bind(function (result, exception) {
            handler.call(this);

            if (Ung.Util.handleException(exception)) {
                return;
            }

            Ext.each(result.interfaces.list, function (iface) {
                if (!iface.disabled) {
                    if (iface.isWan) {
                        me.data.externalInterfaces.push({
                            id: iface.interfaceId,
                            name: iface.name,
                            rx: 0,
                            tx: 0
                        });
                    } else {
                        me.data.internalInterfaces.push({
                            id: iface.interfaceId,
                            name: iface.name,
                            rx: 0,
                            tx: 0
                        });
                    }
                }
            });
            this.interfacesLoaded = true;
            this.update(me.data);
        }, this));
    }
});

/* ReportEntry Widget */
Ext.define('Ung.dashboard.ReportEntry', {
    extend: 'Ung.dashboard.Widget',
    layout: 'fit',
    border: false,
    entry: null,
    hasRefresh: true,

    tpl: '<div class="wg-wrapper no-padding">' +
        '</div>' +
        '<div class="chart" style="height: 260px; position: absolute; left: 0; bottom: 0; right: 0;">' +
        '</div>' +
        '</div>' +
        '<div class="mask init-mask"><i class="material-icons">widgets</i><p>' + i18n._('Loading') + '...</p></div>' +
        '<div class="mask nodata-mask"><i class="material-icons">not_interested</i><p>' + i18n._('No data available yet!') + '</p></div>',
    data: {},
    chart: null,
    chartData: null,
    initComponent: function () {
        this.title = '<h3>' + i18n._(this.entry.category) + ' &bull; ' + i18n._(this.entry.title) + '</h3><p>' + i18n._(this.entry.description) + '</p>';
        if (this.entry.type === 'EVENT_LIST') {
            this.items = [this.buildGrid()];
            this.callParent(arguments);
            this.gridEvents = this.down("grid[name=gridEvents]");
        } else if (this.entry.type === 'TEXT') {
            this.callParent(arguments);
        } else {
            this.tpl = new Ext.XTemplate(this.tpl);
            this.callParent(arguments);
        }
    },

    listeners: {
        afterrender: function (widget) {
            widget.getEl().query('.init-mask p')[0].innerHTML = this.entry.category + ' &bull; ' + this.entry.title;
        }
    },
    loadData: function (handler) {
        if (this.entry.type === 'EVENT_LIST') {
            this.renderEventData(handler);
        } else {
            this.renderReportData(handler);
        }
    },
    renderReportData: function (handler) {
        Ung.Main.getReportsManager().getDataForReportEntry(Ext.bind(function (result, exception) {
            handler.call(this);

            if (Ung.Util.handleExceptionToast(exception)) {
                return;
            }
            if (this === null || !this.rendered) {
                return;
            }

            var i;
            if (this.entry.type === 'TEXT') {
                var infos = [], column, value, data = result.list;
                if (data.length > 0 && this.entry.textColumns != null) {
                    for (i = 0; i < this.entry.textColumns.length; i += 1) {
                        column = this.entry.textColumns[i].split(" ").splice(-1)[0];
                        value = Ext.isEmpty(data[0][column]) ? 0 : data[0][column];
                        infos.push(value);
                    }
                }
                this.getEl().query('.chart')[0].innerHTML = '<p class="text-report">' + Ext.String.format.apply(Ext.String.format, [i18n._(this.entry.textString)].concat(infos)) + '</p>';

            } else {
                this.chartData = result.list;
                // add a new time prop because the datagrid alters the time_trunc, causing charting issues
                for (i = 0; i < this.chartData.length; i += 1) {
                    if (this.chartData[i].time_trunc) {
                        this.chartData[i].time = this.chartData[i].time_trunc.time;
                    }
                }

                if (this.entry.type === 'TIME_GRAPH' || this.entry.type === 'TIME_GRAPH_DYNAMIC') {
                    this.chart = Ung.charts.timeSeriesChart(this.entry, this.chartData, this, true);
                } else {
                    this.chart = Ung.charts.categoriesChart(this.entry, this.chartData, this, true);
                }
            }
        }, this), this.entry, this.timeframe, -1);
    },

    renderEventData: function (handler) {
        var me = this;
        Ung.Main.getReportsManager().getEventsForTimeframeResultSet(Ext.bind(function (result, exception) {
            handler.call(this);

            if (Ung.Util.handleExceptionToast(exception)) {
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
            store: Ext.create('Ext.data.Store', {
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
    }
});

Ext.define('Ung.dashboard.Util', {
    singleton: true,

    buildInterfaces: function () {
        var c, intf, name, key;
        if (!this.interfaces) {
            this.interfaces = [];
            this.interfaceMap = {};
            var networkSettings = Ung.Main.getNetworkSettings();
            for (c = 0; c < networkSettings.interfaces.list.length; c += 1) {
                intf = networkSettings.interfaces.list[c];
                name = intf.name;
                key = intf.interfaceId;
                this.interfaces.push([key, name]);
                this.interfaceMap[key] = name;
            }
        }
    },
    getInterfaces: function () {
        this.buildInterfaces();
        return this.interfaces;
    },
    getInterfaceMap: function () {
        this.buildInterfaces();
        return this.interfaceMap;
    },

    createTextReport: function (entry, widget) {
        return {
            xtype: 'component',
            name: "chart",
            margin: 10,
            html: ''
        };
    }
});


