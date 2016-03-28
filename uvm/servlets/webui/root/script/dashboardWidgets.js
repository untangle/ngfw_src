/*global
 Ext, Ung, i18n, rpc, setTimeout, clearTimeout, console, window, document, Highcharts
 */
Ext.define('Ung.dashboard', {
    singleton: true,
    widgetsList: [],
    widgetsGrid: [],
    reportEntriesModified: false,
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
            var i;
            if (Ung.Util.handleException(exception)) {
                return;
            }
            this.allWidgets = [];
            for (i = 0; i < result.widgets.list.length; i += 1) {
                if (result.widgets.list[i].enabled) {
                    this.allWidgets.push(result.widgets.list[i]);
                }
            }
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
        this.widgetsGrid = [];
        this.widgetsList = [];
        this.dashboardPanel.removeAll();
        var i, j, type, entry, widget;

        for (i = 0; i < this.allWidgets.length; i += 1) {
            widget = this.allWidgets[i];
            type = widget.type;
            if (type !== "ReportEntry" && type !== "EventEntry") {
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
                    } else {
                        entry = this.eventsMap[widget.entryId];
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
                    this.widgetsList[j].cls = 'medium';
                } else {
                    this.widgetsList[j].cls = 'large';
                }
            }
        }
        this.dashboardPanel.add(this.widgetsList);

        if (!this.scrollInitialized) {
            this.scrollInitialized = true;
            this.dashboardContainer.getEl().on('scroll', this.debounce(this.onScrollChange, 500));
            Ext.on('resize', this.debounce(this.onScrollChange, 500));
        }

        var timer;
        Ext.on('resize', function () {
            clearTimeout(timer);
            timer = setTimeout(function () {
                Highcharts.charts.forEach(function (chart) {
                    if (chart) {
                        chart.reflow();
                    }
                });
            }, 100);
        });
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
        for (i = 0; i < this.widgetsList.length; i += 1) {
            widget = this.widgetsList[i];
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
                        Ung.Main.target = "reports." + entry.category + (panel.widgetType === "ReportEntry" ? ".report." : ".event.") + panel.entry.uniqueId;
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
                    panel.dataFirstLoaded = false; // to force loading data when manual refresh
                    panel.loadingMask.show();
                    Ung.dashboard.Queue.addFirst(panel);
                }
            });
            /*
            this.tools.push({
                type: 'refresh',
                tooltip: i18n._('Refresh'),
                margin: '0 5 0 5',
                callback: function (panel) {
                    var dimm = document.createElement('div');
                    dimm.className = 'dimm';
                    document.body.appendChild(dimm);

                    panel.addCls('enlarged');
                    console.log(panel.chart);
                    setTimeout(function () {
                        panel.chart.reflow();
                    }, 500);

                }
            });
            */
        }
        this.callParent(arguments);
    },
    afterRender: function () {
        if (Ext.isFunction(this.loadData)) {
            Ung.dashboard.Queue.add(this);
        }
        this.callParent(arguments);
    },

    afterLoad: function () {
        if (this.hasRefresh) {
            this.loadingMask.hide();
        }
        //console.log(this.getHeader());
        this.header = true;
        try {
            this.removeCls('init');
            this.removeCls('loading');
        } catch (ignore) {
            //console.log('null widget conf');
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
        this.callParent(arguments);
    },
    data: {},
    tpl: '<div class="wg-wrapper information">' +
            '<p class="info-hostname">{hostname}<br/> <span>version: {version}</span></p>' +
            '<p class="info-uptime"><i class="material-icons" style="vertical-align: middle; font-size: 16px; margin-right: 5px;">access_time</i>' + i18n._('uptime') + ': {uptime}</p>' +
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
         '<div class="init-mask"><i class="material-icons">widgets</i><p>' + i18n._("Information") + '</p></div>',
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

/* Resources Widget */
Ext.define('Ung.dashboard.Resources', {
    extend: 'Ung.dashboard.Widget',
    hasStats: true,
    initComponent: function () {
        this.title = '<h3>' + i18n._("Resources") + '</h3>';
        this.callParent(arguments);
    },
    tpl:
        /*
        '<div class="wg-wrapper flex resources" style="padding: 0 30px;">' +
        '<div class="info-box" style="padding: 0;">' +
            '<p>Memory<br/><span style="font-size: 20px; font-weight: 400;">{totalMemory} Mb</span></p>' +
            '<div class="donut"><div class="chart res1"></div><span>50</span></div>' +
        '</div>' +
        '<div class="info-box" style="padding: 0;">' +
            '<p>Swap<br/><span style="font-size: 20px; font-weight: 400;">23456 Mb</span></p>' +
            '<div class="donut"><div class="chart res2"></div><span>50</span></div>' +
        '</div>' +
        '<div class="info-box" style="padding: 0;">' +
            '<p>Disk<br/><span style="font-size: 20px; font-weight: 400;">23456 Mb</span></p>' +
            '<div class="donut"><div class="chart res3"></div><span>50</span></div>' +
        '</div>' +
        */
        '<div class="wg-wrapper flex" style="padding: 0 30px; align-items: initial;">' +
        '<p style="margin: 5px 0; font-weight: 600;">' + i18n._('Memory') + '</p>' +
        '<div>' +
            '<div class="wg-progress"><div class="wg-progress-bar"><span style="left: -{percentFreeMemory}%;"></span></div><p>{totalMemory} MB</p></div>' +
            '<div class="wg-progress-vals"><span style="color: #BB9600; font-weight: 600;">{usedMemory} MB</span> used <em>({percentUsedMemory}%)</em></div>' +
            '<div class="wg-progress-vals"><span style="color: #555; font-weight: 600;">{freeMemory} MB</span> free <em>({percentFreeMemory}%)</em></div>' +
        '</div>' +
        '<div style="border-top: 1px #EEE solid; border-bottom: 1px #EEE solid; margin: 5px 0; padding: 0 0 10px 0;">' +
            '<p style="margin: 5px 0; font-weight: 600;">' + i18n._('Swap') + '</p>' +
            '<div>' +
                '<div class="wg-progress"><div class="wg-progress-bar"><span style="left: -{percentFreeSwap}%;"></span></div><p>{totalSwap} MB</p></div>' +
                '<div class="wg-progress-vals"><span style="color: #BB9600; font-weight: 600;">{usedSwap} MB</span> used <em>({percentUsedSwap}%)</em></div>' +
                '<div class="wg-progress-vals"><span style="color: #555; font-weight: 600;">{freeSwap} MB</span> free <em>({percentFreeSwap}%)</em></div>' +
            '</div>' +
        '</div>' +
        '<p style="margin: 5px 0; font-weight: 600;">' + i18n._('Disk') + '</p>' +
        '<div>' +
        '<div class="wg-progress"><div class="wg-progress-bar"><span style="left: -{percentFreeDisk}%;"></span></div><p>{totalDisk} GB</p></div>' +
        '<div class="wg-progress-vals"><span style="color: #BB9600; font-weight: 600;">{usedDisk} GB</span> used <em>({percentUsedDisk}%)</em></div>' +
        '<div class="wg-progress-vals"><span style="color: #555; font-weight: 600;">{freeDisk} GB</span> free <em>({percentFreeDisk}%)</em></div>' +
        '</div>' +
        '</div>' +
        '<div class="init-mask"><i class="material-icons">widgets</i><p>' + i18n._("Resources") + '</p></div>',
    data: {},
    listeners: {
        /*
        'afterrender': function (widget) {
            for (var i=1; i<=3; i++) {
                new Highcharts.Chart({
                    chart: {
                        type: 'solidgauge',
                        renderTo: widget.getEl().query('.chart.res' + i)[0],
                        margin: [0,0,0,0],
                        padding: 0,
                        backgroundColor: 'transparent'
                    },
                    credits: {
                        enabled: false
                    },
                    exporting: {
                        enabled: false
                    },
                    title: null,

                    tooltip: {
                        enabled: false
                    },

                    pane: {
                        startAngle: 0,
                        endAngle: 360,
                        background: [{
                            outerRadius: '100%',
                            innerRadius: '90%',
                            backgroundColor: '#EEE',
                            borderWidth: 0
                        }]
                    },

                    yAxis: {
                        min: 0,
                        max: 100,
                        lineWidth: 0,
                        tickPositions: []
                    },

                    plotOptions: {
                        solidgauge: {
                            //borderWidth: '8px',
                            dataLabels: {
                                enabled: false
                            },
                            //linecap: 'round',
                            stickyTracking: false
                        }
                    },


                    series: [{
                        name: 'Move',
                        data: [{
                            color: 'coral',
                            radius: '100%',
                            innerRadius: '90%',
                            y: 60
                        }]
                    }]
                });       
            }
        }
        */
    },
    updateStats: function (stats) {
        //if (!this.loaded) {
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
            //this.loaded = true;
        //}
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
        '<div class="init-mask"><i class="material-icons">widgets</i><p>' + i18n._("CPU Load") + '</p></div>',
    data: {},
    lineChart: null,
    gaugeChart: null,
    listeners: {
        'afterrender': function (widget) {
            widget.lineChart = Ung.charts.cpuLineChart(widget.getEl().query('.cpu-line-chart')[0]);
            widget.gaugeChart = Ung.charts.cpuGaugeChart(widget.getEl().query('.cpu-gauge-chart')[0]);
        }
    },
    updateStats: function (stats) {
        var medLimit = stats.numCpus + 1;
        var highLimit = stats.numCpus + 4;
        var loadLabel = 'low';

        if (!this.loaded && this.lineChart !== null && this.chart2 !== null) {
            this.lineChart.yAxis[0].update({
                minRange: stats.numCpus
            });

            this.gaugeChart.yAxis[0].update({
                max: highLimit + 1,
                plotBands: [{
                    from: 0,
                    to: medLimit,
                    color: 'rgba(112, 173, 112, 0.5)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }, {
                    from: medLimit,
                    to: highLimit,
                    color: 'rgba(255, 255, 0, 0.5)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }, {
                    from: highLimit,
                    to: highLimit + 1,
                    color: 'rgba(255, 0, 0, 0.5)',
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
            this.loaded = true;
        }


        if (stats.oneMinuteLoadAvg > medLimit) {
            loadLabel = 'medium';
        }
        if (stats.oneMinuteLoadAvg > highLimit) {
            loadLabel = 'high';
        }
        if (Ext.select('.cpuLoadVal', this).elements[0]) {
            Ext.select('.cpuLoadVal', this).elements[0].addCls(loadLabel).setHtml(stats.oneMinuteLoadAvg + '<br/><span>' + loadLabel + '</span>');
        }

        if (this.lineChart !== null && this.gaugeChart !== null) {
            this.lineChart.series[0].addPoint([(new Date()).getTime(), stats.oneMinuteLoadAvg], true, true);
            this.gaugeChart.series[0].points[0].update(stats.oneMinuteLoadAvg <= 7 ? stats.oneMinuteLoadAvg : 7, true);
        }

    }
});

/* Network Information Widget */
Ext.define('Ung.dashboard.NetworkInformation', {
    extend: 'Ung.dashboard.Widget',
    displayMode: 'small',
    hasStats: true,
    initComponent: function () {
        this.title = '<h3 style="padding: 5px 0;">' + i18n._("Network Information") + '</h3>';
        this.callParent(arguments);
    },
    tpl: '<div class="wg-wrapper no-padding flex">' +
            '<div class="info-box" style="border-bottom: 1px #EEE solid;">' +
                '<div class="info-item">' + i18n._("Currently Active") + '<br/><span>{hosts.activeHosts}</span></div>' +
                '<div class="info-item">' + i18n._("Maximum Active") + '<br/><span>{hosts.maxActiveHosts}</span></div>' +
                '<div class="info-item">' + i18n._("Known Devices") + '<br/><span>{hosts.knownDevices}</span></div>' +
                '<div class="info-actions">' +
                    '<button class="wg-button" onclick="Ung.Main.showHosts();" style="flex: 1;">View Hosts</button>' +
                    '<button class="wg-button" onclick="Ung.Main.showDevices();" style="flex: 1;">View Devices</button>' +
                '</div>' +
            '</div>' +
            '<div class="info-box">' +
                '<div class="info-item">' + i18n._("Total Sessions") + '<br/><span>{sessions.totalSessions}</span></div>' +
                '<div class="info-item">' + i18n._("Scanned Sessions") + '<br/><span>{sessions.scannedSessions}</span></div>' +
                '<div class="info-item">' + i18n._("Bypassed Sessions") + '<br/><span>{sessions.bypassedSessions}</span></div>' +
                '<div class="info-actions">' +
                    '<button class="wg-button" onclick="Ung.Main.showSessions();" style="flex: 1;">View Sessions</button> ' +
                '</div>' +
            '</div>' +
        '</div>' +
        '<div class="init-mask"><i class="material-icons">widgets</i><p>' + i18n._("Network Information") + '</p></div>',
    data: {
        hosts: {},
        sessions: {}
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
    data: {},
    initComponent: function () {
        this.title = '<h3>' + i18n._("Network Layout") + '</h3>';
        this.callParent(arguments);
    },
    tpl: '<div class="wg-wrapper network-intf">' +
        '<button id="fake-interface-add" class="wg-button" style="position: absolute; right: 10px; top: 0;">Add Fake Interface</button>' +
        '<div class="external">' +
        '<div class="iface" id="interface_{externalInterface.id}">' +
        '<img src="/skins/default/images/admin/icons/interface-cloud.png" style="margin-bottom: 5px; height: 30px;"/>' +
        '<p class="name">{externalInterface.name}</p>' +
        '<div class="speeds" style="display: inline-block; text-align: left;">' +
        '<span class="up">{externalInterface.tx} kb/s</span>' +
        '<span class="down">{externalInterface.rx} kb/s</span>' +
        '</div>' +
        '<br/><span class="connection ext"></span>' +
        '</div>' +
        '</div>' +
        '<div class="wire"></div>' +
        '<div class="wrap">' +
        '<div class="internal">' +
        '<tpl for="internalInterfaces">' +
        '<div class="iface" id="interface_{id}">' +
        '<span class="connection int"></span><br/>' +
        '<div class="speeds" style="display: inline-block; text-align: left;">' +
        '<span class="up">??? kb/s</span>' +
        '<span class="down">??? kb/s</span>' +
        '</div>' +
        '<p class="name">{name}</p>' +
        '<p class="devs">23</p>' +
        '</div>' +
        '</tpl>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '<div class="init-mask"><i class="material-icons">widgets</i><p>' + i18n._("Network Layout") + '</p></div>',
    updateStats: function (stats) {
        //console.log(stats);
        if (this.data.externalInterface) {
            var speedElExt = document.querySelector('#interface_' + this.data.externalInterface.id);
            if (speedElExt) {
                speedElExt.querySelector('.up').innerHTML = Math.round(stats['interface_' + this.data.externalInterface.id + '_txBps'] / 1024) + ' kb/s';
                speedElExt.querySelector('.down').innerHTML = Math.round(stats['interface_' + this.data.externalInterface.id + '_rxBps'] / 1024) + ' kb/s';
            }
        }

        if (this.data.internalInterfaces) {
            var speedsEl, i;
            for (i = 0; i < this.data.internalInterfaces.length; i += 1) {
                speedsEl = document.querySelector('#interface_' + this.data.internalInterfaces[i].id);
                if (speedsEl) {
                    speedsEl.querySelector('.up').innerHTML = Math.round(stats['interface_' + this.data.internalInterfaces[i].id + '_txBps'] / 1024) + ' kb/s';
                    speedsEl.querySelector('.down').innerHTML = Math.round(stats['interface_' + this.data.internalInterfaces[i].id + '_rxBps'] / 1024) + ' kb/s';
                }
            }
        }

    },
    loadData: function (handler) {
        this.data.externalInterface = {};
        this.data.internalInterfaces = [];

        var me = this;

        rpc.networkManager.getNetworkSettings(Ext.bind(function (result, exception) {
            handler.call(this);

            if (Ung.Util.handleException(exception)) {
                return;
            }
            var allInterfaces = result.interfaces.list, i;

            this.data.externalInterface = {
                id: allInterfaces[0].interfaceId,
                name: allInterfaces[0].name,
                //physicalDev: allInterfaces[0].physicalDev,
                //disabled: allInterfaces[0].disabled,
                rx: '???',
                tx: '???'
            };

            for (i = 1; i < allInterfaces.length; i += 1) {
                if (!allInterfaces[i].disabled) {
                    this.data.internalInterfaces.push({
                        id: allInterfaces[i].interfaceId,
                        name: allInterfaces[i].name,
                        //physicalDev: allInterfaces[i].physicalDev,
                        //disabled: allInterfaces[i].disabled,
                        rx: '???',
                        tx: '???'
                    });
                }
            }

            this.update(this.data);

            Ext.get('fake-interface-add').on('click', function () {
                me.addFakeInterface();
            });

        }, this));
    },
    addFakeInterface: function () {
        var fakeIntf = {
            name: 'Fake interface',
            disabled: false,
            rx: 100,
            tx: 10
        };
        this.data.internalInterfaces.push(fakeIntf);
        this.update(this.data);
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
        '<div class="chart-types" style="height: 20px;">' +
        '</div>' +
        '<div class="chart" style="height: 240px; position: absolute; left: 0; bottom: 0; right: 0;">' +
        '</div>' +
        '</div>' +
        '<div class="mask init-mask"><i class="material-icons">widgets</i><p>Loading ...</p></div>' +
        '<div class="mask nodata-mask"><i class="material-icons">not_interested</i><p>' + i18n._('No data available yet!') + '</p></div>',
    data: {},
    chart: null,
    chartData: null,
    initComponent: function () {
        this.title = '<h3>' + this.entry.category + ' &bull; ' + this.entry.title + '</h3><p>' + this.entry.description + '</p>';
        switch (this.entry.timeStyle) {
        case 'LINE':
            this.chartType = 'spline';
            break;
        case 'AREA':
            this.chartType = 'areaspline';
            break;
        case 'BAR_3D_OVERLAPPED':
            this.chartType = 'column';
            break;
        default:
            this.chartType = 'areaspline';
        }
        this.callParent(arguments);
    },

    listeners: {
        'afterrender': function (widget) {
            var chartButtons = widget.getEl().query('.chart-types')[0];
            widget.getEl().query('.init-mask p')[0].innerHTML = this.entry.category + ' &bull; ' + this.entry.title;

            switch (widget.entry.type) {
            case 'TIME_GRAPH':
            case 'TIME_GRAPH_DYNAMIC':
                var i;
                chartButtons.innerHTML =
                    '<button data-type="spline" class="selected">' + i18n._('Line') + '</button>' +
                    '<button data-type="areaspline">' + i18n._('Area') + '</button>' +
                    '<button data-type="column">' + i18n._('Grouped Columns') + '</button>' +
                    '<button data-type="column" data-overlapped>' + i18n._('Overlapped Columns') + '</button>';
                chartButtons.addEventListener('click', function (evt) {
                    for (i = 0; i < chartButtons.querySelectorAll('button').length; i += 1) {
                        chartButtons.querySelectorAll('button')[i].removeAttribute('class');
                    }
                    evt.target.className = 'selected';
                    widget.entry.columnOverlapped = evt.target.dataset.overlapped !== undefined;
                    Ung.charts.updateSeriesType(widget.entry, widget.chart, evt.target.dataset.type);
                });
                break;
            default:
                chartButtons.innerHTML =
                    '<button data-type="pie" class="selected">' + i18n._('Pie') + '</button>' +
                    '<button data-type="pie" data-donut>' + i18n._('Donut') + '</button>' +
                    '<button data-type="column">' + i18n._('Column') + '</button>';
                chartButtons.addEventListener('click', function (evt) {
                    for (i = 0; i < chartButtons.querySelectorAll('button').length; i += 1) {
                        chartButtons.querySelectorAll('button')[i].removeAttribute('class');
                    }
                    evt.target.className = 'selected';
                    widget.entry.chartType = evt.target.dataset.type;
                    widget.entry.isDonut = evt.target.dataset.donut !== undefined;
                    widget.chart.destroy();
                    widget.chart = Ung.charts.categoriesChart(widget.entry, widget.chartData, widget.getEl().query('.chart')[0], true);
                });
            }
        }
    },

    loadData: function (handler) {
        Ung.Main.getReportsManager().getDataForReportEntry(Ext.bind(function (result, exception) {
            handler.call(this);

            if (Ung.Util.handleException(exception)) {
                return;
            }
            if (this === null || !this.rendered) {
                return;
            }

            this.chartData = result.list;

            if (!this.chart || this.chart.series.length === 0) {
                if (this.entry.type === 'TIME_GRAPH' || this.entry.type === 'TIME_GRAPH_DYNAMIC') {
                    this.chart = Ung.charts.timeSeriesChart(this.entry, result.list, this.getEl().query('.chart')[0], true);
                } else {
                    this.chart = Ung.charts.categoriesChart(this.entry, result.list, this.getEl().query('.chart')[0], true);
                }
            } else {
                if (this.entry.type === 'TIME_GRAPH' || this.entry.type === 'TIME_GRAPH_DYNAMIC') {
                    Ung.charts.setTimeSeries(this.entry, result.list, this.chart);
                } else {
                    Ung.charts.setCategoriesSeries(this.entry, result.list, this.chart);
                }
            }

            //console.log(this.chart.series[0].data);

            /*
            if (this.chart.series[0].data.length === 0) {
                this.addCls('nodata');
                return false;
            }
            */
        }, this), this.entry, this.timeframe, -1);
    }
});

/* EventEntry Widget */
Ext.define('Ung.dashboard.EventEntry', {
    extend: 'Ung.dashboard.Widget',
    cls: 'widget',
    layout: 'fit',
    border: false,
    entry: null,
    displayColumns: null,
    items: null,
    hasRefresh: true,
    initComponent: function () {
        this.title = '<h3>' + this.entry.category + ' &bull; ' + this.entry.title + '</h3>';
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
    },
    loadData: function (handler) {
        var me = this;
        Ung.Main.getReportsManager().getEventsForTimeframeResultSet(Ext.bind(function (result, exception) {
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


