/*global
 Ext, Ung, Webui, rpc:true, i18n:true, setTimeout, clearTimeout, console, window, document, JSONRpcClient, Highcharts
 */
var rpc = {}; // the main json rpc object
var testMode = false;
var interactiveMode = true;

// Main object class
Ext.define("Ung.Main", {
    singleton: true,
    debugMode: false,
    buildStamp: null,
    viewport: null,
    init: function (config) {
        Ext.MessageBox.wait(i18n._("Starting..."), i18n._("Please wait"));
        Ext.apply(this, config);
        if (Ext.isGecko) {
            document.onkeypress = function (e) {
                return e.keyCode !== 27;
            };
        }
        JSONRpcClient.toplevel_ex_handler = Ung.Util.rpcExHandler;
        JSONRpcClient.max_req_active = 25;

        if (Ext.supports.LocalStorage) {
            Ext.state.Manager.setProvider(Ext.create('Ext.state.LocalStorageProvider'));
        }
        rpc = {};
        rpc.jsonrpc = new JSONRpcClient("/reports/JSON-RPC");

        Ext.Deferred.pipeline([
            this.loadSkin,
            this.loadReportsManager,
            this.loadTimezoneOffset,
            this.loadTranslations
        ]).then(Ext.bind(function () {
            if(config.reportChart){
                interactiveMode = false;
                this.startReportChart(config);
            }else{
                this.startApplication();
            }
        }, this), function (exception) {
            Ung.Util.handleException(exception);
        });
    },

    loadSkin: function () {
        var deferred = new Ext.Deferred();
        rpc.jsonrpc.ReportsContext.skinManager(Ext.bind(function (result, exception) {
            if (exception) { deferred.reject(exception); }

            rpc.skinManager = result;
            rpc.skinManager.getSettings(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    deferred.reject(exception);
                }
                rpc.skinSettings = result;
                Ung.Util.loadCss("/skins/" + rpc.skinSettings.skinName + "/css/common.css");
                Ung.Util.loadCss("/skins/" + rpc.skinSettings.skinName + "/css/admin.css");
                deferred.resolve();
            }, this));
        }, this));
        return deferred.promise;
    },

    loadReportsManager: function () {
        var deferred = new Ext.Deferred();
        rpc.jsonrpc.ReportsContext.reportsManager(Ext.bind(function (result, exception) {
            if (exception) { deferred.reject(exception); }

            rpc.reportsManager = result;
            rpc.reportsManager.isReportsEnabled(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    deferred.reject(exception);
                }
                rpc.reportsEnabled = result;
                deferred.resolve();
            }, this));
        }, this));
        return deferred.promise;
    },

    loadTimezoneOffset: function () {
        var deferred = new Ext.Deferred();
        rpc.reportsManager.getTimeZoneOffset(Ext.bind(function (result, exception) {
            if (exception) { deferred.reject(exception); }

            rpc.timeZoneOffset = result;
            Highcharts.setOptions({
                global: {
                    timezoneOffset: -(result / 60000)
                }
            });
            deferred.resolve();
        }, this));
        return deferred.promise;
    },

    loadTranslations: function () {
        var deferred = new Ext.Deferred();
        rpc.jsonrpc.ReportsContext.languageManager(Ext.bind(function (result, exception) {
            if (exception) { deferred.reject(exception); }

            rpc.languageManager = result;
            // get translations for main module

            rpc.languageManager.getTranslations(Ext.bind(function (result, exception) {
                if (exception) { deferred.reject(exception); }

                i18n = Ext.create('Ung.I18N', {
                    map: result.map,
                    timeoffset: (new Date().getTimezoneOffset() * 60000) + rpc.timeZoneOffset
                });

                deferred.resolve();
            }, this), 'untangle');
        }, this));
        return deferred.promise;
    },

    // needed as it is used by reports.js
    getReportsManager: function () {
        return rpc.reportsManager;
    },

    startApplication: function () {
        this.viewport = Ext.create('Ext.container.Viewport', {
            layout: 'border',
            items: [{
                xtype: 'container',
                region: 'north',
                cls: 'main-menu',
                layout: { type: 'hbox', align: 'middle' },
                items: [{
                    xtype: 'component',
                    margin: '3 0 3 10',
                    cls: 'logo',
                    height: 60,
                    width: 100,
                    style: "background-image: url(/images/BrandingLogo.png?" + (new Date()).getTime() + ");"
                }, {
                    xtype: 'component',
                    height: 60,
                    html: i18n._('Reports'),
                    background: 'transparent',
                    style: {
                        color: '#FFF',
                        fontSize: '16px',
                        fontFamily: '"PT Sans", sans-serif',
                        paddingTop: '34px'
                    }
                }, {
                    xtype: 'container',
                    flex: 1
                }, {
                    xtype: 'container',
                    cls: 'user-menu',
                    defaults: {
                        scale: 'large'
                    },
                    items: [{
                        xtype: 'button',
                        html: '<i class="material-icons">exit_to_app</i> <span>' + i18n._('Logout') + '</span>',
                        cls: 'main-menu-btn',
                        href: '/auth/logout?url=/reports&realm=Reports',
                        hrefTarget: '_self'
                    }]
                }]
            }, {
                border: false,
                region: 'center',
                xtype: 'panel',
                layout: 'fit',
                items: [{
                    xtype: 'container',
                    layout: "fit",
                    itemId: 'reports'
                }]
            }]
        });

        if (rpc.reportsEnabled) {
            this.viewport.down("#reports").add(Ext.create("Ung.panel.Reports"));
        } else {
            this.viewport.down("#reports").add({
                xtype: 'container',
                html: '<i class="material-icons" style="font-size: 48px; color: orange;">warning</i><br/>' + i18n._('Reports is not installed into your rack or it is not turned on.'),
                margin: '100 0 0 0',
                style: {
                    fontFamily: '"PT Sans", sans-serif',
                    textAlign: 'center',
                    fontSize: '18px'
                }
            });
        }
        Ext.MessageBox.hide();
    },
    /**
     * Specialized chart-only mode used by fixed reports to generate charts
     * for reports.  Expects the following parameters in config object:
     @param {String}    reportUniqueId - Report's unique id
     @param {Date}      startDate      - Start date
     @param {Date}      startDate      - End date
     */
    startReportChart: function (config) {

        var startDate = new Date(parseInt(decodeURI(config.startDate)));
        var endDate = new Date(parseInt(decodeURI(config.endDate)));

        this.viewport = Ext.create('Ext.container.Viewport', {
            layout: 'border',
            items: [{
                border: false,
                region: 'center',
                xtype: 'panel',
                layout: 'fit',
                items: [{
                    xtype: 'panel',
                    itemId: 'highchart',
                    border: false
                }]
            }]
        });

        if (rpc.reportsEnabled) {
            var entry = {};
            rpc.reportsManager.getReportEntry(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
                this.entry = result;
            if(this.entry != {}){
                rpc.reportsManager.getDataForReportEntry(Ext.bind(function (result, exception) {
                    if (Ung.Util.handleException(exception)) {
                        return;
                    }
                    this.chartData = result.list;

                    // add a new time prop because the datagrid alters the time_trunc, causing charting issues
                    for (i = 0; i < this.chartData.length; i += 1) {
                        if (this.chartData[i].time_trunc) {
                            this.chartData[i].time = this.chartData[i].time_trunc.time;
                        }
                    }

                    switch (this.entry.type) {
                        case 'TEXT':
                            break;
                        case 'TIME_GRAPH':
                        case 'TIME_GRAPH_DYNAMIC':
                            this.chart = Ung.charts.timeSeriesChart(this.entry, result.list, this.viewport.down('#highchart').body, false, true);
                            break;
                        default:
                            this.chart = Ung.charts.categoriesChart(this.entry, result.list, this.viewport.down('#highchart').body, false, true);
                    }
                }, this), this.entry, startDate, endDate, [], -1);
            }
            }, this), decodeURI(config.reportUniqueId));


        } else {
            this.viewport.down("#reports").add({
                xtype: 'container',
                html: '<i class="material-icons" style="font-size: 48px; color: orange;">warning</i><br/>' + i18n._('Reports is not installed into your rack or it is not turned on.'),
                margin: '100 0 0 0',
                style: {
                    fontFamily: '"PT Sans", sans-serif',
                    textAlign: 'center',
                    fontSize: '18px'
                }
            });
        }
        Ext.MessageBox.hide();
    }
});


// Code below needs review as remained from the older version

//Ext overrides used in reports serlvet
Ext.override(Ext.grid.column.Column, {
    defaultRenderer: Ext.util.Format.htmlEncode
});

Ext.apply(Ext.data.SortTypes, {
    // Timestamp sorting
    asTimestamp: function (value) {
        return value.time;
    },
    // Ip address sorting. may contain netmask.
    asIp: function (value) {
        if (Ext.isEmpty(value)) {
            return null;
        }
        var i, len, parts = ('' + value).replace(/\//g, '.').split('.');
        for (i = 0, len = parts.length; i < len; i += 1) {
            parts[i] = Ext.String.leftPad(parts[i], 3, '0');
        }
        return parts.join('.');
    }
});