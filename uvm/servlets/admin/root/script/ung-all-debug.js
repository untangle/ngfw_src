Ext.define('Ung.rpc.Rpc', {
    alternateClassName: 'Rpc',
    singleton: true,

    // loadWebUi: function() {
    //     var deferred = new Ext.Deferred(), me = this;
    //     this.getWebuiStartupInfo(function (result, exception) {
    //         if (exception) { deferred.reject(exception); }
    //         Ext.apply(me, result);
    //         console.log('hahaha');


    //         String.prototype.translate = function() {
    //             var record = Rpc.rpc.translations[this.valueOf()], value;
    //             if (record === null) {
    //                 alert('Missing translation for: ' + this.valueOf()); // Key is not found in the corresponding messages_<locale>.properties file.
    //                 return this.valueOf(); // Return key name as placeholder
    //             }
    //             return value;
    //         };

    //         //console.log('Dashboard'.translate());

    //         if (me.nodeManager.node('untangle-node-reports')) {
    //             me.reportsManager = me.nodeManager.node('untangle-node-reports').getReportsManager();
    //         }
    //         deferred.resolve();
    //     });
    //     return deferred.promise;
    // },



    getDashboardSettings: function () {
        console.time('dashboardSettings');
        var deferred = new Ext.Deferred();
        this.rpc.dashboardManager.getSettings(function (settings, exception) {
            console.timeEnd('dashboardSettings');
            if (exception) { deferred.reject(exception); }
            //Ung.app.dashboardSettings = result;

            //Ung.app.getStore('Widgets').loadRawData(result.widgets);
            //Ext.get('app-loader-text').setHtml('Loading widgets ...');
            deferred.resolve(settings);
        });
        return deferred.promise;
    },

    getReports: function () {
        console.time('loadReports');
        var deferred = new Ext.Deferred();
        if (this.rpc.nodeManager.node('untangle-node-reports')) {
            this.rpc.reportsManager = this.rpc.nodeManager.node('untangle-node-reports').getReportsManager();
            this.rpc.reportsManager.getReportEntries(function (result, exception) {
                console.timeEnd('loadReports');
                if (exception) { deferred.reject(exception); }
                // deferred.reject('aaa');
                deferred.resolve(result);
            });

        } else {
            deferred.resolve(null);
        }
        return deferred.promise;
    },

    getUnavailableApps: function () {
        console.time('unavailApps');
        var deferred = new Ext.Deferred();
        if (rpc.reportsManager) {
            rpc.reportsManager.getUnavailableApplicationsMap(function (result, exception) {
                console.timeEnd('unavailApps');
                if (exception) { deferred.reject(exception); }
                deferred.resolve(result);
                // Ext.getStore('unavailableApps').loadRawData(result.map);
                // Ext.getStore('widgets').loadData(settings.widgets.list);
                // me.loadWidgets();
            });
        } else {
            // Ext.getStore('widgets').loadData(settings.widgets.list);
            // me.loadWidgets();
            deferred.resolve(null);
        }
        return deferred.promise;
    },




    getReportData: function (entry, timeframe) {
        var deferred = new Ext.Deferred();
        this.rpc.reportsManager.getDataForReportEntry(function(result, exception) {
            if (exception) { deferred.reject(exception); }
            deferred.resolve(result);
        }, entry, timeframe, -1);
        return deferred.promise;
    },

    getRpcNode: function (nodeId) {
        var deferred = new Ext.Deferred();
        this.rpc.nodeManager.node(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, nodeId);
        return deferred.promise;
    },

    getNodeSettings: function (node) {
        var deferred = new Ext.Deferred();
        node.getSettings(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        });
        return deferred.promise;
    },

    setNodeSettings: function (node, settings) {
        console.log(settings);
        var deferred = new Ext.Deferred();
        node.setSettings(function (result, ex) {
            console.log(result);
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, settings);
        return deferred.promise;
    },




    readRecords: function () {
        console.log('read');
        return {};
    }


});
Ext.define('Ung.util.Util', {
    alternateClassName: 'Ung.Util',
    singleton: true,

    // node name to class mapping
    nodeClassMapping: {
        'untangle-node-web-filter': 'Ung.node.WebFilter',
        'untangle-node-web-filter-lite': 'Ung.node.WebFilterLite',
        'untangle-node-virus-blocker': 'Ung.node.VirusBlocker',
        'untangle-node-virus-blocker-lite': 'Ung.node.VirusBlockerLite',
        'untangle-node-spam-blocker': 'Ung.node.SpamBlocker',
        'untangle-node-spam-blocker-lite': 'Ung.node.SpamBlockerLite',
        'untangle-node-phish-blocker': 'Ung.node.PhishBlocker',
        'untangle-node-web-cache': 'Ung.node.WebCache',
        'untangle-node-bandwidth-control': 'Ung.node.BandwidthControl',
        'untangle-casing-ssl-inspector': 'Ung.node.SslInspector',
        'untangle-node-application-control': 'Ung.node.ApplicationControl',
        'untangle-node-application-control-lite': 'Ung.node.ApplicationControlLite',
        'untangle-node-captive-portal': 'Ung.node.CaptivePortal',
        'untangle-node-firewall': 'Ung.node.Firewall',
        'untangle-node-ad-blocker': 'Ung.node.AdBlocker',

        'untangle-node-reports': 'Ung.node.Reports'
    },

    iconReportTitle: function (report) {
        var icon;
        switch (report.get('type')) {
        case 'TEXT':
            icon = 'subject';
            break;
        case 'EVENT_LIST':
            icon = 'format_list_bulleted';
            break;
        case 'PIE_GRAPH':
            icon = 'pie_chart';
            if (report.get('pieStyle') === 'COLUMN' || report.get('pieStyle') === 'COLUMN_3D') {
                icon = 'insert_chart';
            } else {
                if (report.get('pieStyle') === 'DONUT' || report.get('pieStyle') === 'DONUT_3D') {
                    icon = 'donut_large';
                }
            }
            break;
        case 'TIME_GRAPH':
        case 'TIME_GRAPH_DYNAMIC':
            if (report.get('timeStyle').indexOf('BAR') >= 0) {
                icon = 'insert_chart';
            } else {
                icon = 'show_chart';
            }
            break;
        default:
            icon = 'subject';
        }
        return '<i class="material-icons" style="font-size: 18px">' + icon + '</i>';
    },

    bytesToHumanReadable: function (bytes, si) {
        var thresh = si ? 1000 : 1024;
        if(Math.abs(bytes) < thresh) {
            return bytes + ' B';
        }
        var units = si ? ['kB','MB','GB','TB','PB','EB','ZB','YB'] : ['KiB','MiB','GiB','TiB','PiB','EiB','ZiB','YiB'];
        var u = -1;
        do {
            bytes /= thresh;
            ++u;
        } while(Math.abs(bytes) >= thresh && u < units.length - 1);
        return bytes.toFixed(1)+' '+units[u];
    },

    formatBytes: function (bytes, decimals) {
        if (bytes === 0) {
            return '0';
        }
        //bytes = bytes * 1000;
        var k = 1000, // or 1024 for binary
            dm = decimals || 3,
            sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'],
            i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    },

    iconTitle: function (text, icon) {
        var icn = icon.split('-') [0],
            size = icon.split('-') [1] || 24;
        return '<i class="material-icons" style="font-size: ' + size + 'px">' +
                icn + '</i> <span style="vertical-align: middle;">' +
                text + '</span>';
    },

    successToast: function (message) {
        Ext.toast({
            html: '<i class="fa fa-check fa-lg"></i> ' + message,
            // minWidth: 200,
            bodyPadding: '12 12 12 40',
            baseCls: 'toast',
            border: false,
            bodyBorder: false,
            // align: 'b',
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 10,
            paddingY: 50
        });
    },

    exceptionToast: function (message) {
        var msg = [];
        if (typeof message === 'object') {
            if (message.name && message.code) {
                msg.push('<strong>Name:</strong> ' + message.name + ' (' + message.code + ')');
            }
            if (message.message) {
                msg.push('<strong>Error:</strong> ' + message.message);
            }
        } else {
            msg = [message];
        }
        Ext.toast({
            html: '<i class="fa fa-exclamation-triangle fa-lg"></i> <span style="font-weight: bold; color: yellow;">Exception!</span><br/>' + msg.join('<br/>'),
            bodyPadding: '10 10 10 45',
            baseCls: 'toast',
            cls: 'exception',
            border: false,
            bodyBorder: false,
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 10,
            paddingY: 50
        });
    },

    invalidFormToast: function (fields) {
        if (!fields || fields.length === 0) {
            return;
        }

        var str = [];
        fields.forEach(function (field) {
            str.push('<span class="field-name">' + field.label + '</span>: <br/> <span class="field-error">' + field.error.replace(/<\/?[^>]+(>|$)/g, '') + '</span>');
        });
        Ext.toast({
            html: '<i class="fa fa-exclamation-triangle fa-lg"></i> <span style="font-weight: bold; font-size: 14px; color: yellow;">Check invalid fields!</span><br/><br/>' + str.join('<br/>'),
            bodyPadding: '10 10 10 45',
            baseCls: 'toast-invalid-frm',
            border: false,
            bodyBorder: false,
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 10,
            paddingY: 50
        });
    },

    getInterfaceListSystemDev: function (wanMatchers, anyMatcher, systemDev) {
        var networkSettings = rpc.networkSettings,
            data = [], intf, i;

        // Note: using strings as keys instead of numbers, needed for the checkboxgroup column widget component to function

        for (i = 0; i < networkSettings.interfaces.list.length; i += 1) {
            intf = networkSettings.interfaces.list[i];
            data.push([systemDev ? intf.systemDev.toString() : intf.interfaceId.toString(), intf.name]);
        }

        if (systemDev) {
            data.push(['tun0', 'OpenVPN']);
        } else {
            data.push(['250', 'OpenVPN']); // 0xfa
            data.push(['251', 'L2TP']); // 0xfb
            data.push(['252', 'Xauth']); // 0xfc
            data.push(['253', 'GRE']); // 0xfd
        }
        if (wanMatchers) {
            data.unshift(['wan', 'Any WAN'.t()]);
            data.unshift(['non_wan', 'Any Non-WAN'.t()]);
        }
        if (anyMatcher) {
            data.unshift(['any', 'Any'.t()]);
        }
        return data;
    },
    getInterfaceList: function (wanMatchers, anyMatcher) {
        return Ung.Util.getInterfaceListSystemDev(wanMatchers, anyMatcher, false);
    },

    // used for render purposes
    interfacesListNamesMap: function () {
        var map = {
            'wan': 'Any WAN'.t(),
            'non_wan': 'Any Non-WAN'.t(),
            'any': 'Any'.t(),
            '250': 'OpenVPN',
            '251': 'L2TP',
            '252': 'Xauth',
            '253': 'GRE'
        };
        var i, intf;

        for (i = 0; i < rpc.networkSettings.interfaces.list.length; i += 1) {
            intf = rpc.networkSettings.interfaces.list[i];
            map[intf.systemDev] = intf.name;
            map[intf.interfaceId] = intf.name;
        }
        return map;
    },




    urlValidator: function (val) {
        var res = val.match(/(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/g);
        return res ? true : 'Url missing or in wrong format!'.t();
    },

    /**
     * Helper method that lists the order in which classes are loaded
     */
    getClassOrder: function () {
        var classes = [], extClasses = [];

        Ext.Loader.history.forEach(function (cls) {
            if (cls.indexOf('Ung') === 0) {
                classes.push(cls.replace('Ung', 'app').replace(/\./g, '/') + '.js');
            } else {
                extClasses.push(cls);
            }
        });

        classes.pop();

        Ext.create('Ext.Window', {
            title: 'Untangle Classes Load Order',
            width: 600,
            height: 600,

            // Constraining will pull the Window leftwards so that it's within the parent Window
            modal: true,
            draggable: false,
            resizable: false,
            layout: {
                type: 'hbox',
                align: 'stretch',
                pack: 'end'
            },
            items: [
            // {
            //     xtype: 'component',
            //     padding: 10,
            //     html: 'Copy this list into <strong>.buildorder</strong> file!'
            // }, {
                {
                xtype: 'textarea',
                border: false,
                flex: 1,
                editable: false,
                fieldStyle: {
                    background: '#FFF',
                    fontSize: '11px'
                },
                value: classes.join('\r\n')
            }, {
                xtype: 'textarea',
                border: false,
                flex: 1,
                editable: false,
                fieldStyle: {
                    background: '#FFF',
                    fontSize: '11px'
                },
                value: extClasses.join('\r\n')
            }]
        }).show();
    },

    getV4NetmaskList: function(includeNull) {
        var data = [];
        if (includeNull) {
            data.push( [null,"\u00a0"] );
        }
        data.push( [32,"/32 - 255.255.255.255"] );
        data.push( [31,"/31 - 255.255.255.254"] );
        data.push( [30,"/30 - 255.255.255.252"] );
        data.push( [29,"/29 - 255.255.255.248"] );
        data.push( [28,"/28 - 255.255.255.240"] );
        data.push( [27,"/27 - 255.255.255.224"] );
        data.push( [26,"/26 - 255.255.255.192"] );
        data.push( [25,"/25 - 255.255.255.128"] );
        data.push( [24,"/24 - 255.255.255.0"] );
        data.push( [23,"/23 - 255.255.254.0"] );
        data.push( [22,"/22 - 255.255.252.0"] );
        data.push( [21,"/21 - 255.255.248.0"] );
        data.push( [20,"/20 - 255.255.240.0"] );
        data.push( [19,"/19 - 255.255.224.0"] );
        data.push( [18,"/18 - 255.255.192.0"] );
        data.push( [17,"/17 - 255.255.128.0"] );
        data.push( [16,"/16 - 255.255.0.0"] );
        data.push( [15,"/15 - 255.254.0.0"] );
        data.push( [14,"/14 - 255.252.0.0"] );
        data.push( [13,"/13 - 255.248.0.0"] );
        data.push( [12,"/12 - 255.240.0.0"] );
        data.push( [11,"/11 - 255.224.0.0"] );
        data.push( [10,"/10 - 255.192.0.0"] );
        data.push( [9,"/9 - 255.128.0.0"] );
        data.push( [8,"/8 - 255.0.0.0"] );
        data.push( [7,"/7 - 254.0.0.0"] );
        data.push( [6,"/6 - 252.0.0.0"] );
        data.push( [5,"/5 - 248.0.0.0"] );
        data.push( [4,"/4 - 240.0.0.0"] );
        data.push( [3,"/3 - 224.0.0.0"] );
        data.push( [2,"/2 - 192.0.0.0"] );
        data.push( [1,"/1 - 128.0.0.0"] );
        data.push( [0,"/0 - 0.0.0.0"] );

        return data;
    },

});
Ext.define('Ung.util.Metrics', {
    singleton: true,
    frequency: 10000,
    interval: null,
    running: false,

    start: function () {
        var me = this;
        me.stop();
        me.run();
        me.interval = window.setInterval(function () {
            me.run();
        }, me.frequency);
    },

    stop: function () {
        if (this.interval !== null) {
            window.clearInterval(this.interval);
        }
    },

    run: function () {
        var data = [];
        rpc.metricManager.getMetricsAndStats(Ext.bind(function(result, exception) {
            if (exception) { console.log(exception); }

            data = [];

            Ext.getStore('stats').loadRawData(result.systemStats);
            // console.log(result.systemStats);

            for (var nodeId in result.metrics) {
                if (result.metrics.hasOwnProperty(nodeId)) {
                    data.push({
                        nodeId: nodeId,
                        metrics: result.metrics[nodeId]
                    });
                }
            }

            Ext.getStore('metrics').loadData(data);

            //Ext.getStore('metrics').loadData([result.metrics]);
        }));
    }

});
Ext.define('Ung.overrides.form.field.VTypes', {
    override: 'Ext.form.field.VTypes',

    // init: function() {
    //     this.initBundleLoader();
    // },

    mask: {
        macAddrMaskRe: /^[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}$/,
        ip4AddrMaskRe: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
        ip6AddrMaskRe: /^\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?\s*$/,
        email: /^(")?(?:[^\."])(?:(?:[\.])?(?:[\w\-!#$%&'*+/=?^_`{|}~]))*\1@(\w[\-\w]*\.){1,5}([A-Za-z]){2,63}$/,
        ipAddrRange: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)-(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
        cidrRange: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/[0-3]?[0-9]$/,
        ipNetmask: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
    },

    isSinglePortValid: function(val) {
        /* check for values between 0 and 65536 */
        if (val < 0 || val > 65536) { return false; }
        /* verify its an integer (not a float) */
        if (!/^\d{1,5}$/.test(val)) { return false; }
        return true;
    },
    isPortRangeValid: function(val) {
        var portRange = val.split('-');
        if (portRange.length !== 2) { return false; }
        return this.isSinglePortValid(portRange[0]) && this.isSinglePortValid(portRange[1]);
    },
    isPortListValid: function(val) {
        var portList = val.split(','),
            retVal = true, i;
        for (i = 0; i < portList.length; i++) {
            if (portList[i].indexOf('-') !== -1) {
                retVal = retVal && this.isPortRangeValid(portList[i]);
            } else {
                retVal = retVal && this.isSinglePortValid(portList[i]);
            }
            if (!retVal) {
                return false;
            }
        }
        return true;
    },
    isSingleIpValid: function(val) {
        return this.mask.ip4AddrMaskRe.test(val);
    },
    isIpRangeValid: function(val) {
        return this.mask.ipAddrRange.test(val);
    },
    isCIDRValid: function(val) {
        return this.mask.cidrRange.test(val);
    },
    isIpNetmaskValid:function(val) {
        return this.mask.ipNetmask.test(val);
    },
    isIpListValid: function(val) {
        var ipList = val.split(','),
            retVal = true, i;
        for (i = 0; i < ipList.length; i += 1) {
            if (ipList[i].indexOf('-') !== -1) {
                retVal = retVal && this.isIpRangeValid(ipList[i]);
            } else {
                if (ipList[i].indexOf('/') !== -1) {
                    retVal = retVal && ( this.isCIDRValid(ipList[i]) || this.isIpNetmaskValid(ipList[i]));
                } else {
                    retVal = retVal && this.isSingleIpValid(ipList[i]);
                }
            }
            if (!retVal) {
                return false;
            }
        }
        return true;
    },


    // matchers
    email: function (v) {
        return this.mask.email.test(v);
    },
    ipMatcher: function(val) {
        if (val.indexOf('/') === -1 && val.indexOf(',') === -1 && val.indexOf('-') === -1) {
            switch (val) {
                case 'any':
                return true;
            default:
                return this.isSingleIpValid(val);
            }
        }
        if (val.indexOf(',') !== -1) {
            return this.isIpListValid(val);
        } else {
            if (val.indexOf('-') !== -1) {
                return this.isIpRangeValid(val);
            }
            if (val.indexOf('/') !== -1) {
                var cidrValid = this.isCIDRValid(val);
                var ipNetmaskValid = this.isIpNetmaskValid(val);
                return cidrValid || ipNetmaskValid;
            }
            console.log('Unhandled case while handling vtype for ipAddr:', val, ' returning true !');
            return true;
        }
    },
    ipMatcherText: 'Invalid IP Address.'.t(),


    ip4Address: function (val) {
        return this.mask.ip4AddrMaskRe.test(val);
    },
    ip4AddressText: 'Invalid IPv4 Address.'.t(),


    ip4AddressList:  function (v) {
        var addr = v.split(','), i;
        for (i = 0 ; i < addr.length ; i += 1) {
            if (!this.mask.ip4AddrMaskRe.test(addr[i])) {
                return false;
            }
        }
        return true;
    },
    ip4AddressListText: 'Invalid IPv4 Address(es).'.t(),

    ip6Address: function (val) {
        return this.mask.ip6AddrMaskRe.test(val);
    },
    ip6AddressText: 'Invalid IPv6 Address.'.t(),

    ipAddress: function (val) {
        return this.mask.ip4AddrMaskRe.test(val) || this.mask.ip6AddrMaskRe.test(val);
    },
    ipAddressText: 'Invalid IP Address.'.t(),

    macAddress: function (val) {
        return this.mask.macAddrMaskRe.test(val);
    },
    macAddressText: 'Invalid Mac Address.'.t(),

    cidrBlock:  function (v) {
        return (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(v));
    },
    cidrBlockText: 'Must be a network in CIDR format.'.t() + ' ' + '(192.168.123.0/24)',

    cidrBlockList: function (v) {
        var blocks = v.split(','), i;
        for (i = 0 ; i < blocks.length; i += 1) {
            if (!(/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(blocks[i]))) {
                return false;
            }
        }
        return true;
    },
    cidrBlockListText: 'Must be a comma seperated list of networks in CIDR format.'.t() + ' ' + '(192.168.123.0/24,1.2.3.4/24)',

    cidrBlockArea:  function (v) {
        var blocks = v.split('\n'), i;
        for (i = 0 ; i < blocks.length; i += 1) {
            if (!(/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(blocks[i]))) {
                return false;
            }
        }
        return true;
    },
    cidrBlockAreaText: 'Must be a one-per-line list of networks in CIDR format.'.t() + ' ' + '(192.168.123.0/24)',


    portMatcher: function (val) {
        switch (val) {
            case 'any':
            return true;
        default:
            if (val.indexOf('>') !== -1 && val.indexOf(',') === -1) {
                return this.isSinglePortValid( val.substring( val.indexOf('>') + 1 ));
            }
            if (val.indexOf('<') !== -1 && val.indexOf(',') === -1) {
                return this.isSinglePortValid( val.substring( val.indexOf('<') + 1 ));
            }
            if (val.indexOf('-') === -1 && val.indexOf(',') === -1) {
                return this.isSinglePortValid(val);
            }
            if (val.indexOf('-') !== -1 && val.indexOf(',') === -1) {
                return this.isPortRangeValid(val);
            }
            return this.isPortListValid(val);
        }
    },
    portMatcherText: Ext.String.format('The port must be an integer number between {0} and {1}.'.t(), 1, 65535),

    port: function (val) {
        var minValue = 1,
            maxValue = 65535;
        return (minValue <= val && val <= maxValue);
    },
    portText: Ext.String.format('The port must be an integer number between {0} and {1} or one of the following values: any, all, n/a, none.'.t(), 1, 65535)
});
Ext.define('Ung.view.main.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.main',

    control: {
        '#': {
            beforerender: 'onBeforeRender'
        }
    },

    init: function (view) {
        var vm = view.getViewModel();
        //view.getViewModel().set('widgets', Ext.getStore('widgets'));
        vm.set('reports', Ext.getStore('reports'));
        vm.set('policyId', 1);
    },

    // routes: {
    //     '': 'onDashboard',
    //     'apps': 'onApps',
    //     'apps/:policyId': 'onApps',
    //     'apps/:policyId/:node': 'onApps',
    //     'config': 'onConfig',
    //     'config/:configName': 'onConfig',
    //     'reports': 'onReports',
    //     'sessions': 'onSessions',
    //     'hosts': 'onHosts',
    //     'devices': 'onDevices'
    // },

    onBeforeRender: function(view) {
        var vm = view.getViewModel();

        // vm.bind('{reportsEnabled}', function(enabled) {
        //     if (enabled) {
        //         view.down('#main').insert(3, {
        //             xtype: 'ung.reports',
        //             itemId: 'reports'
        //         });
        //     } else {
        //         view.down('#main').remove('reports');
        //     }
        // });

        vm.set('reportsInstalled', rpc.nodeManager.node('untangle-node-reports') !== null);
        if (rpc.nodeManager.node('untangle-node-reports')) {
            vm.set('reportsRunning', rpc.nodeManager.node('untangle-node-reports').getRunState() === 'RUNNING');
        }
        vm.notify();
        /*
        setTimeout(function () {
            vm.set('reportsInstalled', false);
        }, 5000);
        */

        view.getViewModel().set('policies', Ext.getStore('policies'));
        view.getViewModel().set('policy', Ext.getStore('policies').findRecord('policyId', 1));
        //this.getViewModel().set('activeItem', Ext.util.History.getHash());
    },

    // afterRender: function () {
    //     this.redirectTo(Ext.util.History.getHash(), true);
    // },

    onDashboard: function () {
        console.log('on dashboard');
        this.getViewModel().set('activeItem', 'dashboard');
    },

    onApps: function (policyId, node) {
        console.log('on apps');
        var vm = this.getViewModel();
        var _policyId = policyId || 1,
            _currentPolicy = vm.get('policyId'),
            _newPolicy;

        //if (!_currentPolicy || _currentPolicy.get('policyId') !== policyId) {
            //_newPolicy = Ext.getStore('policies').findRecord('policyId', _policyId) || Ext.getStore('policies').findRecord('policyId', 1);

        vm.set('policyId', policyId);
        //}
        // var view = 'Ung.view.apps.Apps';
        // var ctrl = Ung.app.getController('Ung.view.apps.AppsController');

        if (node) {
            if (node === 'install') {
                vm.set('activeItem', 'appsinstall');
            } else {
                vm.set('nodeName', node);
                vm.set('activeItem', 'settings');
            }
        } else {
            vm.set('activeItem', 'apps');
        }
    },

    onConfig: function (configName) {
        this.getViewModel().set('activeItem', 'config');
        var view = this.getView();
        this.getViewModel().set('activeItem', 'config');
        if (configName) {
            Ext.require('Ung.view.config.network.Network', function () {
                view.down('#config').add({
                    xtype: 'ung.config.network'
                });
                view.down('#config').setActiveItem(1);
            });
        } else {
            view.down('#config').setActiveItem(0);
        }
    },

    onReports: function () {
        this.getViewModel().set('activeItem', 'reports');
    },


    // sessions, hosts, devices

    onSessions: function () {
        // this.setShd('sessions');
        this.getViewModel().set('activeItem', 'sessions');
    },

    onHosts: function () {
        this.setShd('hosts');
    },

    onDevices: function () {
        this.setShd('devices');
    },

    setShd: function (viewName) {
        this.getViewModel().set('activeItem', 'shd');
        this.getViewModel().set('shdActiveItem', viewName);
        this.getView().down('#shdcenter').setActiveItem(viewName);
    }

});

Ext.define('Ung.view.main.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.main',

    data: {
        reportsInstalled: false,
        reportsRunning: false
    },
    formulas: {
        // selected: function () {
        //     return 'dashboard';
        // },
        // reports are enabled only if are installed and has running state
        reportsEnabled: function (get) {
            return (get('reportsInstalled') && get('reportsRunning'));
        },
        isDashboard: function(get) {
            return get('activeItem') === 'dashboard';
        },
        isApps: function(get) {
            return get('activeItem') === 'apps';
        },
        isConfig: function(get) {
            return get('activeItem') === 'config';
        },
        isReports: function(get) {
            return get('activeItem') === 'reports';
        },
        isSessions: function(get) {
            return get('shdActiveItem') === 'sessions';
        },
        isHosts: function(get) {
            return get('shdActiveItem') === 'hosts';
        },
        isDevices: function(get) {
            return get('shdActiveItem') === 'devices';
        }
    }
});

/**
 * Dashboard Controller which displays and manages the Dashboard Widgets
 * Widgets can be affected by following actions:
 * - remove/add/modify widget entry itself;
 * - install/uninstall Reports or start/stop Reports service
 * - install/uninstall Apps which can lead in a report widget to be available or not;
 * - modifying a report that is used by a widget, which requires reload of that affected widget
 */
Ext.define('Ung.view.dashboard.DashboardController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.dashboard',
    viewModel: true,
    control: {
        '#': {
            // afterrender: 'loadWidgets'
        }
    },

    listen: {
        global: {
            init: 'loadWidgets',
            nodeinstall: 'onNodeInstall',
            removewidget: 'onRemoveWidget',
            addwidget: 'onAddWidget',
            reportsInstall: 'loadWidgets'
        },
        store: {
            '#stats': {
                datachanged: 'onStatsUpdate'
            },
            '#widgets': {
                // datachanged: 'loadWidgets'
            },
            '#reports': {
                // datachanged: 'loadWidgets'
            }
        }
    },

    init: function (view) {
        var me = this, vm = view.getViewModel();

        // update dashboard when Reports app is installed/removed or enabled/disabled
        // vm.bind('{reportsEnabled}', function() {
        //     // me.loadWidgets();
        // });
        vm.set('managerOpen', false);
    },

    onInit: function () {
        console.log('oninit');
    },

    /**
     * before rendering the Dashboard the settings are fetched form the server
     */
    initDashboard: function () {
        var me = this,
            vm = me.getViewModel();
        //me.populateMenus();
        // load the dashboard settings

        // Rpc.loadDashboardSettings().then(function(settings) {
        //     me.getView().setSettings(settings);
        //     if (vm.get('reportsInstalled')) {
        //         // load unavailable apps needed for showing the widgets
        //         console.time('unavailApps');
        //         rpc.reportsManager.getUnavailableApplicationsMap(function (result, ex) {
        //             if (ex) { Ung.Util.exceptionToast(ex); return false; }

        //             Ext.getStore('unavailableApps').loadRawData(result.map);
        //             Ext.getStore('widgets').loadData(settings.widgets.list);
        //             console.timeEnd('unavailApps');
        //             me.loadWidgets();
        //         });
        //     } else {
        //         Ext.getStore('widgets').loadData(settings.widgets.list);
        //         me.loadWidgets();
        //     }
        //     me.populateMenus();
        // });
        /*
        if (vm.get('reportsInstalled')) {
            // load unavailable apps needed for showing the widgets
            console.time('unavailApps');
            rpc.reportsManager.getUnavailableApplicationsMap(function (result, ex) {
                if (ex) { Ung.Util.exceptionToast(ex); return false; }

                Ext.getStore('unavailableApps').loadRawData(result.map);
                //Ext.getStore('widgets').loadData(settings.widgets.list);
                console.timeEnd('unavailApps');
                me.loadWidgets();
            });
        } else {
            //Ext.getStore('widgets').loadData(settings.widgets.list);
            me.loadWidgets();
        }
        */

    },


    /**
     * Load initial dashboard widgets
     */
    loadWidgets: function() {
        // console.log('loadWidgets');
        var vm = this.getViewModel(),
            dashboard = this.getView().lookupReference('dashboard'),
            widgets = Ext.getStore('widgets').getRange(),
            i, widget, widgetComponents = [], entry;

        // refresh the dashboard manager grid if the widgets were affected
        this.getView().lookupReference('dashboardNav').getView().refresh();

        dashboard.removeAll(true);

        for (i = 0; i < widgets.length; i += 1 ) {
            widget = widgets[i];

            if (widget.get('type') !== 'ReportEntry') {
                dashboard.add({
                    xtype: widget.get('type').toLowerCase() + 'widget',
                    itemId: widget.get('type'),
                    viewModel: {
                        data: {
                            widget: widget
                        }
                    }
                });
            }
            else {
                if (vm.get('reportsEnabled')) {
                    entry = Ext.getStore('reports').findRecord('uniqueId', widget.get('entryId'));
                    if (entry && !Ext.getStore('unavailableApps').first().get(entry.get('category')) && widget.get('enabled')) {
                        dashboard.add({
                            xtype: 'reportwidget',
                            itemId: widget.get('entryId'),
                            refreshIntervalSec: widget.get('refreshIntervalSec'),
                            viewModel: {
                                data: {
                                    widget: widget,
                                    entry: entry
                                }
                            }
                        });
                    } else {
                        dashboard.add({
                            xtype: 'component',
                            itemId: widget.get('entryId'),
                            hidden: true
                        });
                    }
                } else {
                    dashboard.add({
                        xtype: 'component',
                        itemId: widget.get('entryId'),
                        hidden: true
                    });
                }
            }
        }
        // dashboard.add(widgetComponents);
        console.timeEnd('all');
        // this.populateMenus();
    },

    /**
     * when a node is installed or removed apply changes to dashboard
     */
    onNodeInstall: function (action, node) {
        // refresh dashboard manager grid
        this.getView().lookupReference('dashboardNav').getView().refresh();

        var dashboard = this.getView().lookupReference('dashboard'),
            widgets = Ext.getStore('widgets').getRange(), widget, entry, i;

        // traverse all widgets and add/remove those with report category as the passed node
        for (i = 0; i < widgets.length; i += 1 ) {
            widget = widgets[i];
            entry = Ext.getStore('reports').findRecord('uniqueId', widget.get('entryId'));
            if (entry && entry.get('category') === node.displayName) {
                // remove widget placeholder
                dashboard.remove(widget.get('entryId'));
                if (action === 'install') {
                    // add real widget
                    dashboard.insert(i, {
                        xtype: 'reportwidget',
                        itemId: widget.get('entryId'),
                        refreshIntervalSec: widget.get('refreshIntervalSec'),
                        viewModel: {
                            data: {
                                widget: widget,
                                entry: entry
                            }
                        }
                    });
                } else {
                    // add widget placeholder
                    dashboard.insert(i, {
                        xtype: 'component',
                        itemId: widget.get('entryId'),
                        hidden: true
                    });
                }
            }
        }
    },

    enableRenderer: function (value, meta, record) {
        var vm = this.getViewModel();
        if (record.get('type') !== 'ReportEntry') {
            return '<i class="fa ' + (value ? 'fa-check-circle-o' : 'fa-circle-o') + ' fa-lg"></i>';
        }
        var entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));

        if (!entry || Ext.getStore('unavailableApps').first().get(entry.get('category')) || !vm.get('reportsRunning')) {
            return '<i class="fa fa-info-circle fa-lg" style="color: #a91f1f;"></i>';
        }
        return '<i class="fa ' + (value ? 'fa-check-circle-o' : 'fa-circle-o') + ' fa-lg"></i>';
    },

    settingsRenderer: function () {

    },

    /**
     * renders the title of the widget in the dashboard manager grid, based on various conditions
     */
    widgetTitleRenderer: function (value, metaData, record) {
        var vm = this.getViewModel(), entry, title, unavailApp, enabled;
        enabled = record.get('enabled');

        if (!value) {
            return '<span style="font-weight: 700; ' + (!enabled ? 'color: #999;' : '') + '">' + record.get('type') + '</span>'; // <br/><span style="font-size: 10px; color: #777;">Common</span>';
        }
        if (vm.get('reportsInstalled')) {
            entry = Ext.getStore('reports').findRecord('uniqueId', value);
            if (entry) {
                unavailApp = Ext.getStore('unavailableApps').first().get(entry.get('category'));
                title = '<span style="font-weight: 700; ' + ((unavailApp || !enabled) ? 'color: #999;' : '') + '">' + (entry.get('readOnly') ? entry.get('title').t() : entry.get('title')) + '</span>';

                if (entry.get('timeDataInterval') && entry.get('timeDataInterval') !== 'AUTO') {
                    title += '<span style="text-transform: lowercase; color: #999; font-weight: 300;"> per ' + entry.get('timeDataInterval') + '</span>';
                }
                // if (unavailApp) {
                //     title += '<br/><span style="font-size: 10px; color: #777;">' + entry.get('category') + '</span>';
                // } else {
                //     title += '<br/><span style="font-size: 10px; color: #777;">' + entry.get('category') + '</span>';
                // }
                /*
                if (entry.get('readOnly')) {
                    title += ' <i class="material-icons" style="font-size: 14px; color: #999; vertical-align: top;">lock</i>';
                }
                */
                return title;
            } else {
                return 'Some ' + 'Widget'.t();
            }
        } else {
            return '<span style="color: #999; line-height: 26px;">' + 'App Widget'.t() + '</span>';
        }
    },


    /**
     * Method which sends modified dashboard settings to backend to be saved
     */
    applyChanges: function () {
        console.log('apply');
        // because of the drag/drop reorder the settins widgets are updated to respect new ordering
        Ung.dashboardSettings.widgets.list = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');
        rpc.dashboardManager.setSettings(function (result, ex) {
            if (ex) { Ung.Util.exceptionToast(ex); return; }
            Ung.Util.successToast('<span style="color: yellow; font-weight: 600;">Dashboard Saved!</span>');
            Ext.getStore('widgets').sync();
        }, Ung.dashboardSettings);

    },

    managerHandler: function () {
        var state = this.getViewModel().get('managerOpen');
        this.getViewModel().set('managerOpen', !state);
    },

    onItemClick: function (cell, td, cellIndex, record, tr, rowIndex) {
        var me = this,
            dashboard = me.getView().lookupReference('dashboard'),
            vm = this.getViewModel(),
            entry, widgetCmp;

        if (cellIndex === 1) {
            // toggle visibility or show alerts

            if (record.get('type') !== 'ReportEntry') {
                record.set('enabled', !record.get('enabled'));
            } else {
                if (!vm.get('reportsInstalled')) {
                    Ext.Msg.alert('Install required'.t(), 'To enable App Widgets please install Reports first!'.t());
                    return;
                }
                if (!vm.get('reportsRunning')) {
                    Ext.Msg.alert('Reports'.t(), 'To view App Widgets enable the Reports App first!'.t());
                    return;
                }

                entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));
                widgetCmp = dashboard.down('#' + record.get('entryId'));
                if (entry && widgetCmp) {
                    if (!Ext.getStore('unavailableApps').first().get(entry.get('category'))) {
                        widgetCmp.destroy();
                        if (!record.get('enabled')) {
                            dashboard.insert(rowIndex, {
                                xtype: 'reportwidget',
                                itemId: record.get('entryId'),
                                refreshIntervalSec: record.get('refreshIntervalSec'),
                                viewModel: {
                                    data: {
                                        widget: record,
                                        entry: entry
                                    }
                                }
                            });
                            widgetCmp = dashboard.down('#' + record.get('entryId'));
                            setTimeout(function () {
                                dashboard.scrollTo(0, dashboard.getEl().getScrollTop() + widgetCmp.getEl().getY() - 121, {duration: 300 });
                            }, 100);
                        } else {
                            dashboard.insert(rowIndex, {
                                xtype: 'component',
                                itemId: record.get('entryId'),
                                hidden: true
                            });
                        }
                        record.set('enabled', !record.get('enabled'));
                    } else {
                        Ext.Msg.alert('Install required'.t(), Ext.String.format('To enable this Widget please install <strong>{0}</strong> app first!'.t(), entry.get('category')));
                    }
                } else {
                    Ung.Util.exceptionToast('This entry is not available and it should be removed!');
                }

            }
        }

        if (cellIndex === 2) {
            // highlights in the dashboard the widget which receives click event in the manager grid
            widgetCmp = dashboard.down('#' + record.get('entryId')) || dashboard.down('#' + record.get('type'));
            if (widgetCmp && !widgetCmp.isHidden()) {
                dashboard.addCls('highlight');
                widgetCmp.addCls('highlight-item');
                dashboard.scrollTo(0, dashboard.getEl().getScrollTop() + widgetCmp.getEl().getY() - 121, {duration: 500});
            }
        }

        // if (cellIndex === 3) {
        //     // remove widget
        //     record.drop();
        // }
    },

    removeWidget: function (table, rowIndex, colIndex, item, e, record) {
        record.drop();
        // console.log(record);
    },

    /**
     * removes the above set highlight
     */
    onItemLeave: function (view, record) {
        if (this.tout) {
            window.clearTimeout(this.tout);
        }
        var dashboard = this.getView().lookupReference('dashboard'), widgetCmp;
        if (record.get('type') !== 'ReportEntry') {
            widgetCmp = dashboard.down('#' + record.get('type'));
        } else {
            widgetCmp = dashboard.down('#' + record.get('entryId'));
        }
        if (widgetCmp) {
            dashboard.removeCls('highlight');
            widgetCmp.removeCls('highlight-item');
        }
    },


    /**
     * todo: after drag sort event
     */
    onDrop: function (node, data, overModel, dropPosition) {
        var dashboard = this.getView().lookupReference('dashboard');
        //console.log(data.view.getStore().findExact('entryId', data.records[0].get('entryId')));
        //console.log(data.records);

        var widgetMoved = this.getView().down('#' + data.records[0].get('entryId')) || this.getView().down('#' + data.records[0].get('type'));
        var widgetDropped = this.getView().down('#' + overModel.get('entryId')) || this.getView().down('#' + overModel.get('type'));

        /*
        widgetMoved.addCls('moved');

        window.setTimeout(function () {
            widgetMoved.removeCls('moved');
        }, 300);
        */

        if (dropPosition === 'before') {
            dashboard.moveBefore(widgetMoved, widgetDropped);
        } else {
            dashboard.moveAfter(widgetMoved, widgetDropped);
        }


    },

    resetDashboard: function () {
        var me = this,
            vm = this.getViewModel();
        Ext.MessageBox.confirm('Warning'.t(),
            'This will overwrite the current dashboard settings with the defaults.'.t() + '<br/><br/>' +
            'Do you want to continue?'.t(),
            function (btn) {
                if (btn === 'yes') {
                    rpc.dashboardManager.resetSettingsToDefault(function (result, ex) {
                        if (ex) { Ung.Util.exceptionToast(ex); return; }
                        Ung.Util.successToast('Dashboard reset done!');
                        Rpc.getDashboardSettings().then(function(settings) {
                            Ung.dashboardSettings = settings;
                            if (vm.get('reportsInstalled')) {
                                // load unavailable apps needed for showing the widgets
                                rpc.reportsManager.getUnavailableApplicationsMap(function (result, ex) {
                                    if (ex) { Ung.Util.exceptionToast(ex); return false; }

                                    Ext.getStore('unavailableApps').loadRawData(result.map);
                                    Ext.getStore('widgets').loadData(settings.widgets.list);
                                    me.loadWidgets();
                                });
                            } else {
                                Ext.getStore('widgets').loadData(settings.widgets.list);
                                me.loadWidgets();
                            }
                            me.populateMenus();
                        });
                    });
                }
            });
    },


    onRemoveWidget: function (id) {
        var dashboard = this.getView().lookupReference('dashboard');
        if (dashboard.down('#' + id)) {
            dashboard.remove(id);
        }
    },

    onAddWidget: function (widget, entry) {
        var dashboard = this.getView().lookupReference('dashboard');
        if (entry) {
            dashboard.add({
                xtype: 'reportwidget',
                itemId: widget.get('entryId'),
                refreshIntervalSec: widget.get('refreshIntervalSec'),
                viewModel: {
                    data: {
                        widget: widget,
                        entry: entry
                    }
                }
            });
        } else {
            console.log(widget);
            dashboard.add({
                xtype: widget.get('type').toLowerCase() + 'widget',
                itemId: widget.get('type'),
                viewModel: {
                    data: {
                        widget: widget
                    }
                }
            });
        }
    },

    onStatsUpdate: function() {
        var vm = this.getViewModel();
        vm.set('stats', Ext.getStore('stats').first());

        // get devices
        // @todo: review this based on oler implementation
        rpc.deviceTable.getDevices(function (result, ex) {
            if (ex) { Ung.Util.exceptionToast(ex); return false; }
            vm.set('deviceCount', result.list.length);
        });
    },

    populateMenus: function () {
        var addWidgetBtn = this.getView().down('#addWidgetBtn'), categories, categoriesMenu = [], reportsMenu = [];

        if (addWidgetBtn.getMenu()) {
            addWidgetBtn.getMenu().remove();
        }

        categoriesMenu.push({
            text: 'Common',
            icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_hosts.png',
            iconCls: 'menu-icon',
            menu: {
                plain: true,
                items: [{
                    text: 'Information',
                    type: 'Information'
                }, {
                    text: 'Resources',
                    type: 'Resources'
                }, {
                    text: 'CPU Load',
                    type: 'CPULoad'
                }, {
                    text: 'Network Information',
                    type: 'NetworkInformation'
                }, {
                    text: 'Network Layout',
                    type: 'NetworkLayout'
                }, {
                    text: 'Map Distribution',
                    type: 'MapDistribution'
                }],
                listeners: {
                    click: function (menu, item) {
                        if (Ext.getStore('widgets').findRecord('type', item.type)) {
                            Ung.Util.successToast('<span style="color: yellow; font-weight: 600;">' + item.text + '</span>' + ' is already in Dashboard!');
                            return;
                        }
                        var newWidget = Ext.create('Ung.model.Widget', {
                            displayColumns: null,
                            enabled: true,
                            entryId: null,
                            javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                            refreshIntervalSec: 0,
                            timeframe: null,
                            type: item.type
                        });
                        Ext.getStore('widgets').add(newWidget);
                        Ext.GlobalEvents.fireEvent('addwidget', newWidget, null);
                    }
                }
            }
        });

        if (rpc.reportsManager) {
            rpc.reportsManager.getCurrentApplications(function (result, ex) {
                categories = [
                    { displayName: 'Hosts', icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_hosts.png' },
                    { displayName: 'Devices', icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_devices.png' },
                    { displayName: 'Network', icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_network.png' },
                    { displayName: 'Administration', icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_admin.png' },
                    { displayName: 'System', icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_system.png' },
                    { displayName: 'Shield', icon: resourcesBaseHref + '/skins/modern-rack/images/admin/apps/untangle-node-shield_17x17.png' }
                ];
                result.list.forEach(function (app) {
                    categories.push({
                        displayName: app.displayName,
                        icon: resourcesBaseHref + '/skins/modern-rack/images/admin/apps/' + app.name + '_17x17.png'
                    });
                });

                categories.forEach(function (category) {
                    reportsMenu = [];
                    Ext.getStore('reports').filter({
                        property: 'category',
                        value: category.displayName,
                        exactMatch: true
                    });
                    Ext.getStore('reports').getRange().forEach(function(report) {
                        reportsMenu.push({
                            text: Ung.Util.iconReportTitle(report) + ' ' + report.get('title'),
                            report: report
                        });
                    });

                    Ext.getStore('reports').clearFilter();
                    categoriesMenu.push({
                        text: category.displayName,
                        icon: category.icon,
                        iconCls: 'menu-icon',
                        menu: {
                            plain: true,
                            items: reportsMenu,
                            listeners: {
                                click: function (menu, item) {
                                    if (Ext.getStore('widgets').findRecord('entryId', item.report.get('uniqueId'))) {
                                        Ung.Util.successToast('<span style="color: yellow; font-weight: 600;">' + item.report.get('title') + '</span>' + ' is already in Dashboard!');
                                        return;
                                    }
                                    var newWidget = Ext.create('Ung.model.Widget', {
                                        displayColumns: item.report.get('displayColumns'),
                                        enabled: true,
                                        entryId: item.report.get('uniqueId'),
                                        javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                                        refreshIntervalSec: 60,
                                        timeframe: 3600,
                                        type: 'ReportEntry'
                                    });
                                    Ext.getStore('widgets').add(newWidget);
                                    Ext.GlobalEvents.fireEvent('addwidget', newWidget, item.report);
                                }
                            }
                        }
                    });
                });
                addWidgetBtn.setMenu({
                    items: categoriesMenu,
                    mouseLeaveDelay: 0
                });
            });
        } else {
            addWidgetBtn.setMenu({
                items: categoriesMenu,
                mouseLeaveDelay: 0
            });
        }
    }
});

Ext.define('Ung.view.dashboard.Queue', {
    alternateClassName: 'DashboardQueue',
    singleton: true,
    processing: false,
    paused: false,
    queue: [],
    queueMap: {},
    tout: null,
    add: function (widget) {
        if (!this.queueMap[widget.id]) {
            this.queue.push(widget);
            // console.log('Adding: ' + widget.itemId);
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
    remove: function (widget) {
        console.log('removing');
        if (this.processing) {
            this.processing = false;
        }
    },
    process: function () {
        var me = this;
        // ensure processing is false in some cases when ir remains true
        clearTimeout(this.tout);
        this.tout = setTimeout(function () {
            me.processing = false;
            me.queue = [];
        }, 3000);

        //console.log(this.queue);
        if (!this.paused && !this.processing && this.queue.length > 0) {
            this.processing = true;
            var widget = this.queue.shift();

            delete this.queueMap[widget.id];

            //if (this.inView(widget)) {
            if (!widget.isHidden()) {
                widget.fetchData();
            } else {
                widget.toQueue = true;
                Ung.view.dashboard.Queue.next();
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
    },

    inView: function (widget) {
        // checks if the widget is visible
        if (!widget.getEl()) {
            return false;
        }
        var widgetGeometry = widget.getEl().dom.getBoundingClientRect();
        return (widgetGeometry.top + widgetGeometry.height / 2) > 0 && (widgetGeometry.height / 2 + widgetGeometry.top < window.innerHeight);
    }

});
Ext.define('Ung.widget.WidgetController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.widget',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            afterdata: 'onAfterData',
            beforedestroy: 'onBeforeRemove'
            // show: 'onShow'
        },
        '#header': {
            render: 'headerRender'
        }
    },

    listen: {
        // store: {
        //     '#stats': {
        //         datachanged: 'onStatsUpdate'
        //     }
        // }
    },

    headerRender: function (cmp) {
        var me = this;
        cmp.getEl().on({
            click: function (e) {
                if (e.target.dataset.action === 'refresh') {
                    me.addToQueue();
                }
            }
        });
    },

    init: function (view) {
        var vm = view.getViewModel(), entryType;
        if (vm.get('entry')) {
            entryType = vm.get('entry.type');
            if (entryType === 'TIME_GRAPH' || entryType === 'TIME_GRAPH_DYNAMIC') {
                view.add({ xtype: 'timechart', reference: 'chart', height: 250 });
            }

            if (entryType === 'PIE_GRAPH') {
                view.add({ xtype: 'piechart', reference: 'chart',  height: 250 });
            }

            if (entryType === 'EVENT_LIST') {
                view.add({ xtype: 'component', html: 'Not Implemented',  height: 250 });
            }
        }
    },

    onAfterRender: function (widget) {
        setTimeout(function () {
            widget.removeCls('adding');
        }, 100);


        widget.getViewModel().bind('{widget.enabled}', function (enabled) {
            if (enabled && Ext.isFunction(widget.fetchData)) {
                Ung.view.dashboard.Queue.add(widget);
            }
        });
        widget.getViewModel().notify();
    },

    onAfterData: function () {
        var widget = this.getView();
        Ung.view.dashboard.Queue.next();
        if (widget.refreshIntervalSec && widget.refreshIntervalSec > 0) {
            widget.refreshTimeoutId = setTimeout(function () {
                Ung.view.dashboard.Queue.add(widget);
            }, widget.refreshIntervalSec * 1000);
        }
    },

    onBeforeRemove: function (widget) {
        // remove widget from queue if; important if removal is happening while fetching data
        Ung.view.dashboard.Queue.remove(widget);
    },

    onShow: function (widget) {
        console.log('onShow');
        widget.removeCls('adding');
        // console.log('on show', widget.getViewModel().get('widget.type'));
        // if (Ext.isFunction(widget.fetchData)) {
        //     Ung.view.dashboard.Queue.add(widget);
        // }
    },

    addToQueue: function () {
        console.log('fetch data');
        var widget = this.getView();
        if (widget.refreshTimeoutId) {
            clearTimeout(widget.refreshTimeoutId);
        }
        Ung.view.dashboard.Queue.addFirst(widget);
    },


    // not used
    resizeWidget: function () {
        var view = this.getView();
        if (view.hasCls('small')) {
            view.removeCls('small').addCls('medium');
        } else {
            if (view.hasCls('medium')) {
                view.removeCls('medium').addCls('large');
            } else {
                if (view.hasCls('large')) {
                    view.removeCls('large').addCls('x-large');
                } else {
                    if (view.hasCls('x-large')) {
                        view.removeCls('x-large').addCls('small');
                    }
                }
            }
        }
        view.updateLayout();
    }

});

Ext.define('Ung.widget.Information', {
    extend: 'Ext.container.Container',
    alias: 'widget.informationwidget',

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget small info-widget adding',

    bind: {
        hidden: '{!widget.enabled}'
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    items: [{
        xtype: 'component',
        cls: 'header',
        html: '<h1>' + 'Information'.t() + '</h1>'
    }, {
        xtype: 'component',
        cls: 'info-host',
        padding: 5,
        bind: {
            html: '<p class="hostname">{stats.hostname}</p><p class="version">{stats.version}</p>'
        }
    }, {
        xtype: 'component',
        margin: '10 0',
        bind : {
            html: '<span class="info-lbl">' + 'uptime'.t() + ':</span><span class="info-val">{stats.uptimeFormatted}</span><br/>' +
                '<span class="info-lbl">' + 'Server'.t() + ':</span><span class="info-val">{stats.appliance}</span><br/>' +
                '<span class="info-lbl">' + 'CPU Count'.t() + ':</span><span class="info-val">{stats.numCpus}</span><br/>' +
                '<span class="info-lbl">' + 'CPU Type'.t() + ':</span><span class="info-val">{stats.cpuModel}</span><br/>' +
                '<span class="info-lbl">' + 'Architecture'.t() + ':</span><span class="info-val">{stats.architecture}</span><br/>' +
                '<span class="info-lbl">' + 'Memory'.t() + ':</span><span class="info-val">{stats.totalMemory}</span><br/>' +
                '<span class="info-lbl">' + 'Disk'.t() + ':</span><span class="info-val">{stats.totalDisk}</span>'
        }
    }]
});
Ext.define('Ung.widget.Resources', {
    extend: 'Ext.container.Container',
    alias: 'widget.resourceswidget',

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget',
    cls: 'small adding',

    bind: {
        hidden: '{!widget.enabled}'
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    items: [{
        xtype: 'component',
        cls: 'header',
        html: '<h1>' + 'Resources'.t() + '</h1>'
    }, {
        xtype: 'container',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            flex: 1,
            padding: '5 10',
            bind: {
                html: '<div>' +
                        '<p style="margin: 2px; text-align: left; font-weight: bold; font-size: 14px;">' + 'Memory'.t() + '</p>' +
                        '<div class="load-bar"><div class="load-bar-inner" style="left: -{stats.freeMemoryPercent}%;"></div><p>{stats.totalMemory}</p></div>' +
                        '<div class="load-bar-values">' +
                            '<div class="load-used"><strong>{stats.usedMemoryPercent}%</strong> used<br/><span>{stats.usedMemory}</span></div>' +
                            '<div class="load-free">free <strong>{stats.freeMemoryPercent}%</strong><br/><span>{stats.freeMemory}</span></div>' +
                        '</div>' +
                      '</div>'
            }
        }, {
            xtype: 'component',
            flex: 1,
            padding: '5 10',
            bind: {
                html: '<div>' +
                        '<p style="margin: 2px; text-align: left; font-weight: bold; font-size: 14px;">' + 'Swap'.t() + '</p>' +
                        '<div class="load-bar"><div class="load-bar-inner" style="left: -{stats.freeSwapPercent}%;"></div><p>{stats.totalSwap}</p></div>' +
                        '<div class="load-bar-values">' +
                            '<div class="load-used"><strong>{stats.usedSwapPercent}%</strong> used<br/><span>{stats.usedSwap}</span></div>' +
                            '<div class="load-free">free <strong>{stats.freeSwapPercent}%</strong><br/><span>{stats.freeSwap}</span></div>' +
                        '</div>' +
                      '</div>'
            }
        }, {
            xtype: 'component',
            flex: 1,
            padding: '5 10',
            bind: {
                html: '<div>' +
                        '<p style="margin: 2px; text-align: left; font-weight: bold; font-size: 14px;">' + 'Disk'.t() + '</p>' +
                        '<div class="load-bar"><div class="load-bar-inner" style="left: -{stats.freeDiskPercent}%;"></div><p>{stats.totalDisk}</p></div>' +
                        '<div class="load-bar-values">' +
                            '<div class="load-used"><strong>{stats.usedDiskPercent}%</strong> used<br/><span>{stats.usedDisk}</span></div>' +
                            '<div class="load-free">free <strong>{stats.freeDiskPercent}%</strong><br/><span>{stats.freeDisk}</span></div>' +
                        '</div>' +
                      '</div>'
            }
        }]
    }]
});
Ext.define('Ung.widget.NetworkInformation', {
    extend: 'Ext.container.Container',
    alias: 'widget.networkinformationwidget',

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget small adding',

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    bind: {
        hidden: '{!widget.enabled}'
    },

    viewModel: true,

    refreshIntervalSec: 3,

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        html: '<h1>' + 'Network Information'.t() + '</h1>' +
            '<button class="action-btn"><i class="material-icons" data-action="refresh">refresh</i></button>'
    }, {
        xtype: 'container',
        //cls: 'wg-wrapper flex',
        items: [{
            xtype: 'component',
            bind: {
                html: '<div class="info-box" style="border-bottom: 1px #EEE solid;">' +
                '<div class="info-item">' + 'Currently Active'.t() + '<br/><span>{stats.activeHosts}</span></div>' +
                '<div class="info-item">' + 'Maximum Active'.t() + '<br/><span>{stats.maxActiveHosts}</span></div>' +
                '<div class="info-item">' + 'Known Devices'.t() + '<br/><span>{stats.knownDevices}</span></div>' +
                '<div class="info-actions">' +
                '<a class="wg-button" href="#hosts" style="flex: 1;">' + 'Hosts'.t() + '</a>' +
                '<a class="wg-button" href="#devices" style="flex: 1;">' + 'Devices'.t() + '</a>' +
                '</div>' +
                '</div>'
            }
        }, {
            xtype: 'component',
            bind: {
                html: '<div class="info-box">' +
                '<div class="info-item">' + 'Total Sessions'.t() + '<br/><span>{sessions.totalSessions}</span></div>' +
                '<div class="info-item">' + 'Scanned Sessions'.t() + '<br/><span>{sessions.scannedSessions}</span></div>' +
                '<div class="info-item">' + 'Bypassed Sessions'.t() + '<br/><span>{sessions.bypassedSessions}</span></div>' +
                '<div class="info-actions">' +
                '<a class="wg-button" href="#sessions" style="flex: 1;">' + 'Sessions'.t() + '</a> ' +
                '</div>' +
                '</div>'
            }
        }]
    }],

    fetchData: function () {
        var me = this,
            vm = this.getViewModel();

        if (vm) {
            rpc.sessionMonitor.getSessionStats(function (result, exception) {
                vm.set('sessions', result);
                //console.log(result);
                me.fireEvent('afterdata');
            });
        }
    }
});
Ext.define('Ung.widget.MapDistribution', {
    extend: 'Ext.container.Container',
    alias: 'widget.mapdistributionwidget',

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget',
    cls: 'adding',

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    bind: {
        hidden: '{!widget.enabled}'
    },

    refreshIntervalSec: 5,

    items: [{
        xtype: 'container',
        layout: {
            type: 'hbox',
            align: 'top'
        },
        cls: 'header',
        style: {
            height: '50px'
        },
        items: [{
            xtype: 'component',
            flex: 1,
            html: '<h1>' + 'Map Distribution'.t() + '</h1>'
        }, {
            xtype: 'container',
            margin: '10 5 0 0',
            layout: {
                type: 'hbox',
                align: 'middle'
            },
            items: [{
                xtype: 'button',
                baseCls: 'action',
                text: '<i class="material-icons">refresh</i>',
                listeners: {
                    click: 'fetchData'
                }
            }]
        }]
    }, {
        xtype: 'container',
        html: 'Under construction'
    }],

    fetchData: function () {
        var me = this;
        me.fireEvent('afterdata');
    }
});
Ext.define('Ung.view.apps.AppsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.apps',

    control: {
        '#': { activate: 'getPolicies' },
        '#installedApps': { activate: 'filterInstalled' },
        '#installableApps': {
            activate: 'filterInstallable',
            select: 'onInstallNode'
        }
    },

    nodeDesc: {
        'untangle-node-web-filter': 'Web Filter scans and categorizes web traffic to monitor and enforce network usage policies.'.t(),
        'untangle-node-web-monitor': 'Web monitor scans and categorizes web traffic to monitor and enforce network usage policies.'.t(),
        'untangle-node-virus-blocker': 'Virus Blocker detects and blocks malware before it reaches users desktops or mailboxes.'.t(),
        'untangle-node-virus-blocker-lite': 'Virus Blocker Lite detects and blocks malware before it reaches users desktops or mailboxes.'.t(),
        'untangle-node-spam-blocker': 'Spam Blocker detects, blocks, and quarantines spam before it reaches users mailboxes.'.t(),
        'untangle-node-spam-blocker-lite': 'Spam Blocker Lite detects, blocks, and quarantines spam before it reaches users mailboxes.'.t(),
        'untangle-node-phish-blocker': 'Phish Blocker detects and blocks phishing emails using signatures.'.t(),
        'untangle-node-web-cache': 'Web Cache stores and serves web content from local cache for increased speed and reduced bandwidth usage.'.t(),
        'untangle-node-bandwidth-control': 'Bandwidth Control monitors, manages, and shapes bandwidth usage on the network'.t(),
        'untangle-casing-ssl-inspector': 'SSL Inspector allows for full decryption of HTTPS and SMTPS so that other applications can process the encrytped streams.'.t(),
        'untangle-node-application-control': 'Application Control scans sessions and identifies the associated applications allowing each to be flagged and/or blocked.'.t(),
        'untangle-node-application-control-lite': 'Application Control Lite identifies, logs, and blocks sessions based on the session content using custom signatures.'.t(),
        'untangle-node-captive-portal': 'Captive Portal allows administrators to require network users to complete a defined process, such as logging in or accepting a network usage policy, before accessing the internet.'.t(),
        'untangle-node-firewall': 'Firewall is a simple application that flags and blocks sessions based on rules.'.t(),
        'untangle-node-ad-blocker': 'Ad Blocker blocks advertising content and tracking cookies for scanned web traffic.'.t(),
        'untangle-node-reports': 'Reports records network events to provide administrators the visibility and data necessary to investigate network activity.'.t(),
        'untangle-node-policy-manager': 'Policy Manager enables administrators to create different policies and handle different sessions with different policies based on rules.'.t(),
        'untangle-node-directory-connector': 'Directory Connector allows integration with external directories and services, such as Active Directory, RADIUS, or Google.'.t(),
        'untangle-node-wan-failover': 'WAN Failover detects WAN outages and re-routes traffic to any other available WANs to maximize network uptime.'.t(),
        'untangle-node-wan-balancer': 'WAN Balancer spreads network traffic across multiple internet connections for better performance.'.t(),
        'untangle-node-ipsec-vpn': 'IPsec VPN provides secure network access and tunneling to remote users and sites using IPsec, GRE, L2TP, Xauth, and IKEv2 protocols.'.t(),
        'untangle-node-openvpn': 'OpenVPN provides secure network access and tunneling to remote users and sites using the OpenVPN protocol.'.t(),
        'untangle-node-intrusion-prevention': 'Intrusion Prevention blocks scans, detects, and blocks attacks and suspicious traffic using signatures.'.t(),
        'untangle-node-configuration-backup': 'Configuration Backup automatically creates backups of settings uploads them to My Account and Google Drive.'.t(),
        'untangle-node-branding-manager': 'The Branding Settings are used to set the logo and contact information that will be seen by users (e.g. reports).'.t(),
        'untangle-node-live-support': 'Live Support provides on-demand help for any technical issues.'.t()
    },

    refs: {
        installedApps: '#installedApps',
        installableApps: '#installableApps'
    },

    // listen: {
    //     global: {
    //         nodestatechange: 'updateNodes'
    //     },
    //     store: {
    //         '#policies': {
    //             datachanged: 'updateNodes'
    //         }
    //     }
    // },

    getPolicies: function () {
        console.log('on activate');
        var me = this;
        var vm = this.getViewModel();
        rpc.nodeManager.getAppsViews(function(result, ex) {
            console.log(result);
            var nodes = [];
            vm.getStore('apps').removeAll();

            Ext.Array.each(result[0].nodeProperties.list, function (node) {
                nodes.push({
                    name: node.name,
                    displayName: node.displayName,
                    type: node.type,
                    viewPosition: node.viewPosition,
                    status: null,
                    targetState: result[0].instances.list.filter(function (instance) {
                        return node.name === instance.nodeName;
                    })[0].targetState
                });

                // var tState = result[0].instances.list.filter(function (instance) {
                //     return node.name === instance.nodeName;
                // });
                // console.log(tState[0]);
            });

            Ext.Array.each(result[0].installable.list, function (node) {
                nodes.push({
                    name: node.name,
                    displayName: node.displayName,
                    type: node.type,
                    viewPosition: node.viewPosition,
                    desc: me.nodeDesc[node.name],
                    status: 'available'
                });
            });
            // Ext.toast('Data loaded');
            vm.getStore('apps').loadData(nodes);
        });
    },

    /**
     * Based on which view is activated (Apps or Install Apps)
     * the nodes store is filtered to reflect current applications
     */
    filterInstalled: function () {
        this.getViewModel().set('onInstalledApps', true);
        console.log('filter installed');
        var appsStore = this.getViewModel().getStore('apps');

        appsStore.clearFilter();
        appsStore.filterBy(function (rec) {
            return !rec.get('status') || rec.get('status') === 'installing' || rec.get('status') === 'installed';
        });
    },

    filterInstallable: function () {
        this.getViewModel().set('onInstalledApps', false);
        var appsStore = this.getViewModel().getStore('apps');

        // initially, after install the nide item is kept on the Install Apps, having status 'installed'
        // when activating 'Install Apps', the 'installed' status is set as null so that app will not be shown
        appsStore.each(function (rec) {
            if (rec.get('status') === 'installed') {
                rec.set('status', null);
            }
        });
        appsStore.clearFilter();
        appsStore.filterBy(function (rec) {
            return Ext.Array.contains(['available', 'installing', 'installed'], rec.get('status'));
        });
    },

    // init: function (view) {
    //     view.getViewModel().bind({
    //         bindTo: '{policyId}'
    //     }, this.onPolicy, this);
    // },

    // onNodeStateChange: function (state, instance) {
    //     console.log(instance);
    // },

    // updateNodes: function () {
    //     var vm = this.getViewModel(),
    //         nodeInstance, i;

    //     rpc.nodeManager.getAppsViews(function(result, exception) {
    //         var policy = result.filter(function (p) {
    //             return parseInt(p.policyId) === parseInt(vm.get('policyId'));
    //         })[0];

    //         var nodes = policy.nodeProperties.list,
    //             instances = policy.instances.list;

    //         for (i = 0; i < nodes.length; i += 1) {
    //             nodeInstance = instances.filter(function (instance) {
    //                 return instance.nodeName === nodes[i].name;
    //             })[0];
    //             // console.log(nodeInstance.targetState);
    //             nodes[i].policyId = vm.get('policyId');
    //             nodes[i].state = nodeInstance.targetState.toLowerCase();
    //         }
    //         vm.set('nodes', nodes);
    //     });
    // },

    // onPolicy: function () {
    //     // this.getView().lookupReference('filters').removeAll();
    //     // this.getView().lookupReference('services').removeAll();
    //     // this.updateNodes();
    // },

    setPolicy: function (combo, newValue, oldValue) {
        if (oldValue !== null) {
            this.redirectTo('#apps/' + newValue, false);
            this.updateNodes();
        }
    },

    // onItemAfterRender: function (item) {
    //     Ext.defer(function () {
    //         item.removeCls('insert');
    //     }, 50);
    // },

    /**
     * method which initialize the node installation
     */
    onInstallNode: function (view, record) {
        record.set('status', 'installing');
        rpc.nodeManager.instantiate(function (result, ex) {
            if (ex) {
                record.set('status', 'available');
                console.log(ex);
                return;
            }
            record.set('status', 'installed');
        }, record.get('name'), 1);
    }

});

Ext.define('Ung.view.apps.Apps', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.apps',
    itemId: 'apps',
    layout: 'card',
    requires: [
        'Ung.view.apps.AppsController'
    ],

    controller: 'apps',
    viewModel: {
        data: {
            onInstalledApps: false
        },
        stores: {
            apps: {
                // data: '{appsData}',
                fields: ['name', 'displayName', 'type', 'status'],
                sorters: [{
                    property: 'viewPosition',
                    direction: 'ASC'
                }]
            }
        }
    },

    config: {
        policy: undefined
    },

    defaults: {
        border: false
    },

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        border: false,
        items: [{
            xtype: 'combobox',
            editable: false,
            multiSelect: false,
            queryMode: 'local',
            hidden: true,
            bind: {
                value: '{policyId}',
                store: '{policies}',
                hidden: '{!onInstalledApps}'
            },
            valueField: 'policyId',
            displayField: 'displayName',
            listeners: {
                change: 'setPolicy'
            }
        }, {
            xtype: 'button',
            html: 'Install Apps'.t(),
            iconCls: 'fa fa-download',
            hrefTarget: '_self',
            hidden: true,
            bind: {
                href: '#apps/{policyId}/install',
                hidden: '{!onInstalledApps}'
            }
        }, {
            xtype: 'button',
            html: 'Back to Apps'.t(),
            iconCls: 'fa fa-arrow-circle-left',
            hrefTarget: '_self',
            hidden: true,
            bind: {
                href: '#apps/{policyId}',
                hidden: '{onInstalledApps}'
            }
        }]
    }],

    items: [{
        xtype: 'dataview',
        scrollable: true,
        itemId: 'installedApps',
        bind: '{apps}',
        tpl: '<p class="apps-title">' + 'Apps'.t() + '</p>' +
            '<tpl for=".">' +
                '<tpl if="type === \'FILTER\'">' +
                '<a href="#config" class="app-item">' +
                '<span class="state {targetState}"><i class="fa fa-power-off"></i></span>' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<span class="app-name">{displayName}</span>' +
                '</a>' +
                '</tpl>' +
            '</tpl>' +
            '<p class="apps-title">' + 'Service Apps'.t() + '</p>' +
            '<tpl for=".">' +
                '<tpl if="type === \'SERVICE\'">' +
                '<a href="#config" class="app-item">' +
                '<span class="state {targetState}"><i class="fa fa-power-off"></i></span>' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<span class="app-name">{displayName}</span>' +
                '</a>' +
                '</tpl>' +
            '</tpl>',
        itemSelector: 'a'
    }, {
        xtype: 'dataview',
        scrollable: true,
        itemId: 'installableApps',
        bind: {
            store: '{apps}'
        },
        tpl: '<p class="apps-title">' + 'Apps'.t() + '</p>' +'<tpl for=".">' +
            '<tpl if="type === \'FILTER\'">' +
                '<div class="node-install-item {status}">' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<i class="fa fa-download fa-3x"></i>' +
                '<i class="fa fa-check fa-3x"></i>' +
                '<span class="loader">Loading...</span>' +
                '<h3>{displayName}</h3>' + '<p>{desc}</p>' +
                '</div>' +
                '</tpl>' +
            '</tpl>' +
            '<p class="apps-title">' + 'Service Apps'.t() + '</p>' +
            '<tpl for=".">' +
                '<tpl if="type === \'SERVICE\'">' +
                '<div class="node-install-item {status}">' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
                '<i class="fa fa-download fa-3x"></i>' +
                '<i class="fa fa-check fa-3x"></i>' +
                '<span class="loader">Loading...</span>' +
                '<h3>{displayName}</h3>' + '<p>{desc}</p>' +
                '</div>' +
                '</tpl>' +
            '</tpl>',
        itemSelector: 'div'
    }]
});
Ext.define('Ung.view.config.ConfigController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config',

    control: {
        '#': {
            deactivate: 'onDeactivate'
        },
        '#subNav': {
            selectionchange: 'onSelect'
        }
    },

    listen: {
        global: {
            loadconfig: 'onLoadConfig'
        }
    },

    onSelect: function (el, sel) {
        // console.log(selected);
        sel.selected = true;
    },


    onLoadConfig: function (configName, configTab) {
        var view = this.getView();
        if (!configName) {
            view.setActiveItem(1);
            return;
        }

        if (view.down('#configCard')) {
            view.down('#configCard').destroy();
        }

        var cfgName = configName.charAt(0).toUpperCase() + configName.slice(1).toLowerCase();

        view.down('#subNav').getStore().each(function (item) {
            if (item.get('url') === configName) {
                item.set('selected', 'x-item-selected');
            } else {
                item.set('selected', '');
            }
        });


        view.setLoading('Loading ' + cfgName.t() + '...');
        Ext.require('Ung.view.config.' + cfgName.toLowerCase() + '.' + cfgName, function () {
            view.down('#configWrapper').add({
                xtype: 'ung.config.' + cfgName.toLowerCase(),
                region: 'center',
                itemId: 'configCard'
            });
            view.setLoading(false);
            view.setActiveItem(2);
            console.log(configTab);
            if (configTab) {
                view.down('#configCard').setActiveItem(configTab);
            }
            // view.getViewModel().set('currentView', cfgName.toLowerCase());
            // console.log(view.down('#subNav'));
        });
    },

    onDeactivate: function (view) {
        console.log('here');
        // if (view.down('#configCard')) {
        //     view.setActiveItem(0);
        //     view.down('#configCard').destroy();
        // }
        // view.remove('configCard');
    }


});
Ext.define('Ung.view.config.Config', {
    extend: 'Ext.container.Container',
    xtype: 'ung.config',
    itemId: 'config',

    requires: [
        'Ung.view.config.ConfigController',
        'Ung.cmp.EditorFields'
        // 'Ung.view.config.ConfigModel'
    ],

    controller: 'config',

    items: [{
        xtype: 'dataview',
        store: {data: [
                { name: 'Network'.t(), url: 'network', icon: 'icon_config_network.png' },
                { name: 'Administration'.t(), url: 'administration', icon: 'icon_config_admin.png' },
                { name: 'Email'.t(), url: 'email', icon: 'icon_config_email.png' },
                { name: 'Local Directory'.t(), url: 'localdirectory', icon: 'icon_config_directory.png' },
                { name: 'Upgrade'.t(), url: 'upgrade', icon: 'icon_config_upgrade.png' },
                { name: 'System'.t(), url: 'system', icon: 'icon_config_system.png' },
                { name: 'About'.t(), url: 'about', icon: 'icon_config_about.png' }
        ]},
        tpl: '<p class="apps-title">' + 'Configuration'.t() + '</p>' +
             '<tpl for=".">' +
                '<a href="#config/{url}" class="app-item">' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/config/{icon}" width=80 height=80/>' +
                '<span class="app-name">{name}</span>' +
                '</a>' +
            '</tpl>',
        itemSelector: 'a'
    }, {
        xtype: 'dataview',
        store: {data: [
                { name: 'Sessions'.t(), url: 'sessions', icon: 'icon_config_sessions.png' },
                { name: 'Hosts'.t(), url: 'hosts', icon: 'icon_config_hosts.png' },
                { name: 'Devices'.t(), url: 'devices', icon: 'icon_config_devices.png' }
        ]},
        tpl: '<p class="apps-title">' + 'Tools'.t() + '</p>' +
             '<tpl for=".">' +
                '<a href="#{url}" class="app-item">' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/config/{icon}" width=80 height=80/>' +
                '<span class="app-name">{name}</span>' +
                '</a>' +
            '</tpl>',
        itemSelector: 'a'
    }]
});
Ext.define('Ung.widget.ReportModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.reportwidget',

    formulas: {
        title: {
            get: function(get) {
                return '<h1>' +
                    //(get('entry.readOnly') ? ' <i class="material-icons" style="color: #ec3610; font-size: 16px;">lock</i>' : '') +
                    get('entry.title') +
                    (get('entry.timeDataInterval') ? ' <span style="text-transform: lowercase; color: #777; font-weight: 100;">per ' + get('entry.timeDataInterval') + '</span>' : '') +
                    '</h1><p>since ' + get('timeframe') + ' ago</p></h1>';
            }
        },
        timeframe: {
            get: function(get) {
                return get('widget.timeframe');
                //return Ung.util.Services.secondsToString(get('widget.timeframe'));
            }
        }
    }

});
Ext.define('Ung.widget.CpuLoadController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.cpuload',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            resize: 'onResize'
        }
    },

    listen: {
        store: {
            '#stats': {
                datachanged: 'addPoint'
            }
        }
    },

    onAfterRender: function (view) {
        setTimeout(function () {
            view.removeCls('adding');
        }, 100);

        this.lineChart = new Highcharts.Chart({
            chart: {
                type: 'areaspline',
                renderTo: view.lookupReference('cpulinechart').getEl().dom,
                marginBottom: 15,
                marginTop: 20,
                //padding: [0, 0, 0, 0],
                backgroundColor: 'transparent',
                animation: false,
                style: {
                    fontFamily: 'Source Sans Pro',
                    fontSize: '12px'
                }
            },
            title: null,
            credits: {
                enabled: false
            },
            exporting: {
                enabled: false
            },
            xAxis: [{
                type: 'datetime',
                crosshair: {
                    width: 1,
                    dashStyle: 'ShortDot',
                    color: 'rgba(100, 100, 100, 0.3)'
                },
                lineColor: '#C0D0E0',
                lineWidth: 1,
                tickLength: 3,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                labels: {
                    style: {
                        fontFamily: 'Source Sans Pro',
                        color: '#999',
                        fontSize: '11px',
                        fontWeight: 600
                    },
                    y: 12
                },
                maxPadding: 0,
                minPadding: 0
            }],
            yAxis: {
                min: 0,
                minRange: 2,
                lineColor: '#C0D0E0',
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                tickAmount: 4,
                tickLength: 5,
                tickWidth: 1,
                tickPosition: 'inside',
                opposite: false,
                labels: {
                    align: 'left',
                    useHTML: true,
                    padding: 0,
                    style: {
                        fontFamily: 'Source Sans Pro',
                        color: '#999',
                        fontSize: '11px',
                        fontWeight: 600,
                        background: 'rgba(255, 255, 255, 0.6)',
                        padding: '0 1px',
                        borderRadius: '2px',
                        //textShadow: '1px 1px 1px #000'
                        lineHeight: '11px'
                    },
                    x: 1,
                    y: -2
                },
                title: null
            },
            legend: {
                enabled: false
            },
            tooltip: {
                shared: true,
                animation: true,
                followPointer: true,
                backgroundColor: 'rgba(255, 255, 255, 0.7)',
                borderWidth: 1,
                borderColor: 'rgba(0, 0, 0, 0.1)',
                style: {
                    textAlign: 'right',
                    fontFamily: 'Source Sans Pro',
                    padding: '5px',
                    fontSize: '10px',
                    marginBottom: '40px'
                },
                //useHTML: true,
                hideDelay: 0,
                shadow: false,
                headerFormat: '<span style="font-size: 11px; line-height: 1.5; font-weight: bold;">{point.key}</span><br/>',
                pointFormatter: function () {
                    var str = '<span>' + this.series.name + '</span>';
                    str += ': <span style="color: ' + this.color + '; font-weight: bold;">' + this.y + '</span>';
                    return str + '<br/>';
                }
            },
            plotOptions: {
                areaspline: {
                    fillOpacity: 0.15,
                    lineWidth: 2
                },
                series: {
                    marker: {
                        enabled: true,
                        radius: 0,
                        states: {
                            hover: {
                                enabled: true,
                                lineWidthPlus: 2,
                                radius: 4,
                                radiusPlus: 2
                            }
                        }
                    },
                    states: {
                        hover: {
                            enabled: true,
                            lineWidthPlus: 0,
                            halo: {
                                size: 2
                            }
                        }
                    }
                }
            },
            series: [{
                name: 'load',
                data: (function () {
                    var data = [], time = Date.now(), i;
                    try {
                        time = rpc.systemManager.getMilliseconds();
                    } catch (e) {
                        console.log('Unable to get current millis.');
                    }
                    time = Math.round(time/1000) * 1000;
                    for (i = -19; i <= 0; i += 1) {
                        data.push({
                            x: time + i * 3000,
                            y: 0
                        });
                    }
                    return data;
                }())
            }]
        });

        this.gaugeChart = new Highcharts.Chart({
            chart: {
                type: 'gauge',
                renderTo: view.lookupReference('cpugaugechart').getEl().dom,
                height: 140,
                margin: [-20, 0, 0, 0],
                backgroundColor: 'transparent',
                style: {
                    margin: '0 auto'
                }
            },
            credits: {
                enabled: false
            },
            title: null,
            exporting: {
                enabled: false
            },
            pane: [{
                startAngle: -45,
                endAngle: 45,
                background: null,
                center: ['50%', '135%'],
                size: 280
            }],

            tooltip: {
                enabled: false
            },

            yAxis: [{
                min: 0,
                max: 50,
                minorTickPosition: 'outside',
                tickPosition: 'outside',
                tickColor: '#555',
                minorTickColor: '#999',
                labels: {
                    rotation: 'auto',
                    distance: 20,
                    step: 1
                },
                plotBands: [{
                    from: 0,
                    to: 3,
                    color: 'rgba(112, 173, 112, 1)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }, {
                    from: 3,
                    to: 6,
                    color: 'rgba(255, 255, 0, 1)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }, {
                    from: 6,
                    to: 7,
                    color: 'rgba(255, 0, 0, 1)',
                    innerRadius: '100%',
                    outerRadius: '105%'
                }],
                title: null
            }],

            plotOptions: {
                gauge: {
                    dataLabels: {
                        enabled: false
                    },
                    dial: {
                        radius: '99%',
                        backgroundColor: '#999'
                    }
                }
            },
            series: [{
                data: [0]
            }]
        });
        this.addPoint();
    },

    onResize: function () {
        this.lineChart.reflow();
        this.gaugeChart.reflow();
    },

    addPoint: function () {
        if (!this.gaugeChart || !this.lineChart) {
            return;
        }
        var store = Ext.getStore('stats');
        var vm = this.getViewModel(),
            stats = store.first().getData(),
            medLimit = stats.numCpus + 1,
            highLimit = stats.numCpus + 4;

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

        this.lineChart.series[0].addPoint({
            x: Date.now(),
            y: store.first().getData().oneMinuteLoadAvg
        }, true, true);

        this.gaugeChart.series[0].points[0].update(stats.oneMinuteLoadAvg <= highLimit + 1 ? stats.oneMinuteLoadAvg : highLimit + 1, true);

        if (stats.oneMinuteLoadAvg < medLimit) {
            vm.set('loadLabel', 'low'.t());
        }
        if (stats.oneMinuteLoadAvg > medLimit) {
            vm.set('loadLabel', 'medium'.t());
        }
        if (stats.oneMinuteLoadAvg > highLimit) {
            vm.set('loadLabel', 'high'.t());
        }

    }
});
Ext.define('Ung.widget.CpuLoad', {
    extend: 'Ext.container.Container',
    alias: 'widget.cpuloadwidget',

    requires: [
        'Ung.widget.CpuLoadController'
    ],

    controller: 'cpuload',
    viewModel: true,

    hidden: true,
    border: false,
    baseCls: 'widget',
    cls: 'small adding',

    bind: {
        hidden: '{!widget.enabled}'
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    items: [{
        xtype: 'component',
        cls: 'header',
        html: '<h1>' + 'CPU Load'.t() + '</h1>'
    }, {
        xtype: 'container',
        /*
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        */
        items: [{
            xtype: 'component',
            height: 120,
            reference: 'cpulinechart'
        }, {
            xtype: 'component',
            reference: 'cpugaugechart',
            height: 140
        }, {
            xtype: 'component',
            cls: 'cpu-gauge',
            bind: {
                html: '{stats.oneMinuteLoadAvg}<br/><span>{loadLabel}</span>'
            }
        }]
    }]
});
Ext.define('Ung.widget.InterfaceItem', {
    extend: 'Ext.Component',
    alias: 'widget.interfaceitem',

    bind: {
        html: '<p class="name" style="display: {displayWan};">{iface.name}</p>' +
            '<div class="speeds">' +
            '<div class="speed_up"><i class="fa fa-caret-down fa-lg"></i> <span>{tx} kB/s</span></div>' +
            '<div class="speed_down"><i class="fa fa-caret-up fa-lg"></i> <span>{rx} kB/s</span></div>' +
            '</div>' +
            '<p class="name" style="display: {displayNotWan};">{iface.name}</p>' +
            '<i class="fa fa-caret-down fa-lg pointer"></i>'
    },

    viewModel: {
        formulas: {
            displayWan: function (get) {
                return get('iface.isWan') ? 'block' : 'none';
            },
            displayNotWan: function (get) {
                return get('iface.isWan') ? 'none' : 'block';
            },
            tx: function (get) {
                var stats = get('stats').getData(),
                    propStr = 'interface_' + get('iface.interfaceId') + '_txBps';
                if (stats.hasOwnProperty(propStr)) {
                    return (stats[propStr]/1024).toFixed(2);
                } else {
                    return '-';
                }
            },
            rx: function (get) {
                var stats = get('stats').getData(),
                    propStr = 'interface_' + get('iface.interfaceId') + '_rxBps';
                if (stats.hasOwnProperty(propStr)) {
                    return (stats[propStr]/1024).toFixed(2);
                } else {
                    return '-';
                }
            }
        }
    }
});
Ext.define('Ung.widget.NetworkLayout', {
    extend: 'Ext.container.Container',
    alias: 'widget.networklayoutwidget',

    requires: [
        'Ung.widget.InterfaceItem'
    ],

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget adding',

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    bind: {
        hidden: '{!widget.enabled}'
    },

    refreshIntervalSec: 0,

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        html: '<h1>' + 'Network Layout'.t() + '</h1>' +
            '<button class="action-btn"><i class="fa fa-refresh" data-action="refresh"></i></button>'
    }, {
        //xtype: 'container',
        cls: 'net-layout',
        margin: 10,
        layout: {
            type: 'vbox',
            align: 'stretch'
            //pack: 'middle'
        },
        border: false,
        defaults: {
            xtype: 'component'
        },
        items: [{
            html: '<img src="' + resourcesBaseHref + '/skins/default/images/admin/icons/interface-cloud.png" style="margin: 0 auto; display: block; height: 30px;"/>'
        }, {
            xtype: 'container',
            cls: 'ifaces',
            height: 69,
            itemId: 'externalInterface'
        }, {
            xtype: 'component',
            cls: 'line'
        }, {
            xtype: 'container',
            cls: 'ifaces',
            height: 80,
            itemId: 'internalInterface'
        }, {
            xtype: 'component',
            cls: 'devices',
            margin: '5 0 0 0',
            height: 40,
            bind: {
                html: '<img src="' + resourcesBaseHref + '/skins/default/images/admin/icons/interface-devices.png"><br/>{deviceCount}'
            }
        }]
    }],

    fetchData: function () {
        var me = this;
        rpc.networkManager.getNetworkSettings(function (result, ex) {
            me.fireEvent('afterdata');
            if (ex) { Ung.Util.exceptionToast(ex); return false; }
            me.down('#externalInterface').removeAll();
            me.down('#internalInterface').removeAll();
            Ext.each(result.interfaces.list, function (iface) {
                if (!iface.disabled) {
                    if (iface.isWan) {
                        me.down('#externalInterface').add({
                            xtype: 'interfaceitem',
                            cls: 'iface wan',
                            viewModel: {
                                data: {
                                    iface: iface
                                }
                            }
                        });
                    } else {
                        me.down('#internalInterface').add({
                            xtype: 'interfaceitem',
                            cls: 'iface',
                            viewModel: {
                                data: {
                                    iface: iface
                                }
                            }
                        });
                    }
                }
            });
        });
    }

    // fetchData: function () {
    //     var me = this;
    //     rpc.networkManager.getNetworkSettings(function (result, exception) {
    //         me.fireEvent('afterdata');
    //         //handler.call(this);

    //         // Ext.each(result.interfaces.list, function (iface) {
    //         //     if (!iface.disabled) {
    //         //         if (iface.isWan) {
    //         //             me.data.externalInterfaces.push({
    //         //                 id: iface.interfaceId,
    //         //                 name: iface.name,
    //         //                 rx: 0,
    //         //                 tx: 0
    //         //             });
    //         //         } else {
    //         //             me.data.internalInterfaces.push({
    //         //                 id: iface.interfaceId,
    //         //                 name: iface.name,
    //         //                 rx: 0,
    //         //                 tx: 0
    //         //             });
    //         //         }
    //         //     }
    //         // });
    //         // this.interfacesLoaded = true;
    //         // this.update(me.data);
    //     });
    // }
});
Ext.define('Ung.chart.TimeChartController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.timechart',

    control: {
        '#': {
            resize: 'onResize'
        }
    },

    styles: {
        'LINE': { styleType: 'spline' },
        'AREA': { styleType: 'areaspline' },
        'AREA_STACKED': {styleType: 'areaspline', stacking: true },
        'BAR': {styleType: 'column', grouping: true },
        'BAR_OVERLAPPED': {styleType: 'column', overlapped: true },
        'BAR_STACKED': {styleType: 'column', stacking: true }
    },
    init: function () {
        this.defaultColors = ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'];
    },
    setChartType: function (timeStyle) {
        var type = 'areaspline';
        switch (timeStyle) {
        case 'LINE':
            type = 'spline';
            break;
        case 'AREA':
        case 'AREA_STACKED':
            type = 'areaspline';
            break;
        case 'BAR':
        case 'BAR_3D':
        case 'BAR_OVERLAPPED':
        case 'BAR_3D_OVERLAPPED':
        case 'BAR_STACKED':
            type = 'column';
            break;
        default:
            type = 'areaspline';
        }
        return type;
    },

    onAfterRender: function (view) {
        var me = this;
        this.entry = view.getViewModel().get('entry') || this.getView().getEntry();

        this.chart = new Highcharts.StockChart({
            chart: {
                type: me.setChartType(me.entry.get('timeStyle')),
                zoomType: 'x',
                renderTo: view.lookupReference('timechart').getEl().dom,
                //marginBottom: !forDashboard ? 40 : 50,
                marginTop: 10,
                //marginRight: 0,
                //marginLeft: 0,
                padding: [0, 0, 0, 0],
                backgroundColor: 'transparent',
                animation: false,
                style: {
                    fontFamily: 'Source Sans Pro',
                    fontSize: '12px'
                }
            },
            title: null,
            lang: {
                noData: ''
            },
            noData: {
                style: {
                    fontSize: '12px',
                    fontWeight: 'normal',
                    color: '#CCC'
                }
            },
            colors: (me.entry.get('colors') !== null && me.entry.get('colors') > 0) ? me.entry.get('colors') : me.defaultColors,
            navigator: {
                enabled: false
            },
            rangeSelector : {
                enabled: false,
                inputEnabled: false
            },
            scrollbar: {
                enabled: false
            },
            credits: {
                enabled: false
            },
            navigation: {
                buttonOptions: {
                    enabled: false
                }
            },
            xAxis: [{
                type: 'datetime',
                alternateGridColor: 'rgba(220, 220, 220, 0.1)',
                crosshair: {
                    width: 1,
                    dashStyle: 'ShortDot',
                    color: 'rgba(100, 100, 100, 0.3)'
                },
                lineColor: '#C0D0E0',
                lineWidth: 1,
                tickLength: 3,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                labels: {
                    style: {
                        fontFamily: 'Open Sans Condensed',
                        color: '#555',
                        fontSize: '10px',
                        fontWeight: 700
                    },
                    y: 12
                },
                maxPadding: 0,
                minPadding: 0
            }],
            yAxis: {
                allowDecimals: true,
                min: 0,
                minRange: me.entry.get('units') === 'percent' ? 100 : 0.4,
                maxRange: me.entry.get('units') === 'percent' ? 100 : undefined,
                lineColor: '#C0D0E0',
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                //tickPixelInterval: 50,
                tickLength: 5,
                tickWidth: 1,
                //tickPosition: 'inside',
                showFirstLabel: false,
                showLastLabel: true,
                endOnTick: me.entry.get('units') !== 'percent',
                tickInterval: me.entry.get('units') === 'percent' ? 20 : undefined,
                maxPadding: 0,
                opposite: false,
                labels: {
                    align: 'right',
                    useHTML: true,
                    padding: 0,
                    style: {
                        fontFamily: 'Open Sans Condensed',
                        color: '#555',
                        fontSize: '10px',
                        fontWeight: 600,
                        background: 'rgba(255, 255, 255, 0.6)',
                        padding: '0 1px',
                        borderRadius: '2px',
                        //textShadow: '1px 1px 1px #000'
                        lineHeight: '11px'
                    },
                    x: -10,
                    y: 5,
                    formatter: function() {
                        var finalVal = this.value;

                        if (me.entry.get('units') === 'bytes/s') {
                            finalVal = Ung.Util.bytesToHumanReadable(this.value, true);
                            /*
                            if (this.isLast) {
                                return '<span style="color: #555; font-size: 12px;"><strong>' + finalVal + '</strong> (per second)</span>';
                            }
                            */
                        } else {
                            /*
                            if (this.isLast) {
                                return '<span style="color: #555; font-size: 12px;"><strong>' + this.value + '</strong> (' + entry.get('units') + ')</span>';
                            }
                            */
                        }
                        return finalVal;
                    }
                },
                title: {
                    align: 'high',
                    offset: -10,
                    y: 3,
                    //rotation: 0,
                    //text: entry.units,
                    text: me.entry.get('units'),
                    //textAlign: 'left',
                    style: {
                        fontFamily: 'Source Sans Pro',
                        color: '#555',
                        fontSize: '10px',
                        fontWeight: 600
                    }
                }
            },
            legend: {
                enabled: true,
                padding: 5,
                margin: 0,
                y: 10,
                lineHeight: 12,
                itemDistance: 10,
                itemStyle: {
                    fontFamily: 'Source Sans Pro',
                    color: '#555',
                    fontSize: '12px',
                    fontWeight: 600
                },
                symbolHeight: 7,
                symbolWidth: 7,
                symbolRadius: 3
            },
            tooltip: {
                shared: true,
                animation: true,
                followPointer: true,
                backgroundColor: 'rgba(255, 255, 255, 0.7)',
                borderWidth: 1,
                borderColor: 'rgba(0, 0, 0, 0.1)',
                style: {
                    textAlign: 'right',
                    fontFamily: 'Source Sans Pro',
                    padding: '5px',
                    fontSize: '10px',
                    marginBottom: '40px'
                },
                //useHTML: true,
                hideDelay: 0,
                shadow: false,
                headerFormat: '<span style="font-size: 11px; line-height: 1.5; font-weight: bold;">{point.key}</span><br/>',
                pointFormatter: function () {
                    var str = '<span>' + this.series.name + '</span>';
                    if (me.entry.get('units') === 'bytes' || me.entry.get('units') === 'bytes/s') {
                        str += ': <span style="color: ' + this.color + '; font-weight: bold;">' + this.y + '</span>';
                    } else {
                        str += ': <spanstyle="color: ' + this.color + '; font-weight: bold;">' + this.y + '</span> ' + me.entry.get('units');
                    }
                    return str + '<br/>';
                }

            },
            plotOptions: {
                column: {
                    edgeWidth: 0,
                    borderWidth: 0,
                    pointPadding: 0,
                    groupPadding: 0.2,
                    dataGrouping: {
                        groupPixelWidth: 40
                    },
                    pointPlacement: 'on',
                    dataLabels: {
                        enabled: false,
                        align: 'left',
                        rotation: -45,
                        x: 0,
                        y: -2,
                        style: {
                            fontSize: '9px',
                            color: '#999',
                            textShadow: false
                        }
                    }
                },
                areaspline: {
                    lineWidth: 0
                },
                spline: {
                    lineWidth: 2,
                    softThreshold: false
                },
                series: {
                    animation: false,
                    marker: {
                        enabled: true,
                        radius: 0,
                        states: {
                            hover: {
                                enabled: true,
                                lineWidthPlus: 2,
                                radius: 4,
                                radiusPlus: 2
                            }
                        }
                    },
                    states: {
                        hover: {
                            enabled: true,
                            lineWidthPlus: 0,
                            halo: {
                                size: 2
                            }
                        }
                    }
                }
            }
        });
    },

    onResize: function () {
        this.chart.reflow();
    },

    onSetSeries: function (data) {
        //var newData = [], me = this;
        var me = this, newData = [], i, j, _column,
            timeDataColumns = Ext.clone(this.entry.get('timeDataColumns')),
            style = this.entry.get('timeStyle'),
            colors = this.entry.get('colors') || this.defaultColors;

            /*
            timeDataColumns = Ext.clone(me.getViewModel().get('entry.timeDataColumns')),
            style = me.getViewModel().get('entry.timeStyle'),
            colors = me.getViewModel().get('entry.colors') || this.defaultColors;
            */

        this.getView().lookupReference('loader').hide(); // hide chart loader


        if (!timeDataColumns) {
            timeDataColumns = [];
            for (j = 0; j < data.length; j += 1) {
                for (_column in data[j]) {
                    if (data[j].hasOwnProperty(_column) && _column !== 'time_trunc' && _column !== 'time' && timeDataColumns.indexOf(_column) < 0) {
                        timeDataColumns.push(_column);
                    }
                }
            }
        } else {
            for (i = 0; i < timeDataColumns.length; i += 1) {
                timeDataColumns[i] = timeDataColumns[i].split(' ').splice(-1)[0];
            }
        }

        for (i = 0; i < timeDataColumns.length; i += 1) {
            newData = [];
            for (j = 0; j < data.length; j += 1) {
                newData.push([
                    data[j].time_trunc.time,
                    data[j][timeDataColumns[i]] || 0
                ]);
            }
            if (this.chart.get('series-' + timeDataColumns[i])) {
                this.chart.get('series-' + timeDataColumns[i]).update({
                    data: newData,
                    type: me.styles[style].styleType,
                    color: colors[i] || undefined,
                    fillOpacity: (style === 'AREA_STACKED' || style === 'BAR_STACKED') ? 1 : 0.5,
                    grouping: me.styles[style].grouping || false,
                    stacking: me.styles[style].stacking || undefined,
                    pointPadding: me.styles[style].overlapped ? (timeDataColumns.length <= 3 ? 0.10 : 0.075) * i : 0.1,
                    visible: !(timeDataColumns[i] === 'total' && me.styles[style].stacking)
                });
            } else {
                this.chart.addSeries({
                    id: 'series-' + timeDataColumns[i],
                    name: timeDataColumns[i],
                    data: newData,
                    type: me.styles[style].styleType,
                    color: colors[i] || undefined,
                    fillOpacity: (style === 'AREA_STACKED' || style === 'BAR_STACKED') ? 1 : 0.5,
                    grouping: me.styles[style].grouping || false,
                    stacking: me.styles[style].stacking || undefined,
                    pointPlacement: 0,
                    pointPadding: me.styles[style].overlapped ? (timeDataColumns.length <= 3 ? 0.12 : 0.075) * (i + 1) : 0.1,
                    visible: !(timeDataColumns[i] === 'total' && me.styles[style].stacking)
                }, false, false);
            }
        }
        this.chart.redraw();

    },

    onSetStyle: function () {
        var me = this,
            style = me.getViewModel().get('entry.timeStyle'),
            colors = me.getViewModel().get('entry.colors') || this.defaultColors;
        if (this.chart) {
            var seriesLength = this.chart.series.length;
            this.chart.series.forEach( function (series, idx) {
                series.update({
                    type: me.styles[style].styleType,
                    color: colors[idx] || undefined,
                    fillOpacity: (style === 'AREA_STACKED' || style === 'BAR_STACKED') ? 1 : 0.5,
                    grouping: me.styles[style].grouping || false,
                    stacking: me.styles[style].stacking || undefined,
                    pointPadding: me.styles[style].overlapped ? (seriesLength <= 3 ? 0.15 : 0.075) * idx : 0.1,
                    visible: !(series.name === 'total' && me.styles[style].stacking)
                });
            });
        }
    },

    onBeginFetchData: function() {
        this.getView().lookupReference('loader').show();
    }

});
Ext.define('Ung.chart.TimeChart', {
    extend: 'Ext.container.Container',
    alias: 'widget.timechart',
    requires: [
        'Ung.chart.TimeChartController'
    ],

    controller: 'timechart',
    viewModel: true,

    config: {
        widget: null,
        entry: null
    },

    listeners: {
        afterrender: 'onAfterRender',
        resize: 'onResize',
        setseries: 'onSetSeries',
        //setstyle: 'onSetStyle',
        //setcolors: 'onSetColors',
        beginfetchdata: 'onBeginFetchData'
    },

    items: [{
        xtype: 'component',
        reference: 'timechart',
        cls: 'chart'
    }, {
        xtype: 'component',
        reference: 'loader',
        cls: 'loader',
        hideMode: 'visibility'
    }]
});
Ext.define('Ung.chart.PieChartController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.piechart',
    styles: {
        'LINE': { styleType: 'spline' },
        'AREA': { styleType: 'areaspline' },
        'AREA_STACKED': {styleType: 'areaspline', stacking: true },
        'BAR': {styleType: 'column', grouping: true },
        'BAR_OVERLAPPED': {styleType: 'column', overlapped: true },
        'BAR_STACKED': {styleType: 'column', stacking: true }
    },
    init: function () {
        this.defaultColors = ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'];
    },

    onAfterRender: function (view) {
        var me = this;
        //console.log(cmp.viewModel.get('timeStyle'));
        this.entry = view.getViewModel().get('entry') || this.getView().getEntry();

        this.chart =  new Highcharts.Chart({
            chart: {
                type: me.entry.get('pieStyle').indexOf('COLUMN') >= 0 ? 'column' : 'pie',
                renderTo: view.lookupReference('piechart').getEl().dom,
                //margin: (entry.chartType === 'pie' && !forDashboard) ? [80, 20, 50, 20] : undefined,
                marginTop: 10,
                marginRight: 0,
                marginLeft: 0,
                //spacing: [10, 10, 20, 10],
                backgroundColor: 'transparent',
                style: {
                    fontFamily: 'Source Sans Pro', // default font
                    fontSize: '12px'
                },
                options3d: {
                    enabled: false,
                    alpha: 45,
                    beta: 5
                }
            },
            title: null,
            lang: {
                noData: ''
            },
            noData: {
                style: {
                    fontSize: '12px',
                    fontWeight: 'normal',
                    color: '#CCC'
                }
            },
            colors: (me.entry.get('colors') !== null && me.entry.get('colors') > 0) ? me.entry.get('colors') : this.defaultColors,
            credits: {
                enabled: false
            },
            navigation: {
                buttonOptions: {
                    enabled: false
                }
            },
            /*
            exporting: {
                chartOptions: {
                    title: {
                        text: entry.category + ' - ' + entry.title,
                        style: {
                            fontSize: '12px'
                        }
                    }
                },
                type: 'image/jpeg'
            },
            */
            xAxis: {
                type: 'category',
                crosshair: true,
                alternateGridColor: 'rgba(220, 220, 220, 0.1)',
                labels: {
                    style: {
                        fontSize: '11px'
                    }
                },
                lineColor: '#C0D0E0',
                lineWidth: 1,
                tickLength: 3,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                /*
                title: {
                    align: 'middle',
                    text: 'some name',
                    style: {
                        //fontSize: !forDashboard ? '14px' : '12px',
                        fontWeight: 'bold'
                    }
                },
                */
                maxPadding: 0,
                minPadding: 0
            },
            yAxis: {
                lineColor: '#C0D0E0',
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                tickLength: 5,
                tickWidth: 1,
                tickPosition: 'inside',
                showFirstLabel: false,
                showLastLabel: true,
                endOnTick: true,
                labels: {
                    align: 'left',
                    useHTML: true,
                    padding: 0,
                    style: {
                        fontFamily: 'Source Sans Pro',
                        color: '#555',
                        fontSize: '11px',
                        fontWeight: 600,
                        background: 'rgba(255, 255, 255, 0.6)',
                        padding: '0 1px',
                        borderRadius: '2px',
                        //textShadow: '1px 1px 1px #000'
                        lineHeight: '11px'
                    },
                    x: 9,
                    y: 6,
                    formatter: function() {
                        if (this.isLast) {
                            return '<span style="color: #555; font-size: 12px;"><strong>' + this.value + '</strong> (' + me.entry.get('units') + ')</span>';
                        }
                        return this.value;
                    }
                }
            },
            tooltip: {
                headerFormat: '<span style="font-size: 14px; font-weight: bold;">aaa : {point.key}</span><br/>',
                hideDelay: 0
                //pointFormat: '{series.name}: <b>{point.y}</b>' + (entry.pieStyle.indexOf('COLUMN') < 0 ? ' ({point.percentage:.1f}%)' : '')
            },
            plotOptions: {
                pie: {
                    allowPointSelect: true,
                    cursor: 'pointer',
                    center: ['50%', '50%'],
                    showInLegend: true,
                    colorByPoint: true,
                    //depth: 0,
                    minSize: 150,
                    borderWidth: 1,
                    borderColor: '#EEE',
                    dataLabels: {
                        enabled: true,
                        distance: 5,
                        padding: 0,
                        reserveSpace: false,
                        style: {
                            fontSize: '11px',
                            color: '#777',
                            fontFamily: 'Source Sans Pro',
                            fontWeight: 400
                        },
                        formatter: function () {
                            if (this.point.percentage < 3) {
                                return null;
                            }
                            if (this.point.name.length > 25) {
                                return this.point.name.substring(0, 25) + '...';
                            }
                            return this.point.name;
                        }
                        //color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || '#555'
                    }
                },
                column: {
                    borderWidth: 0,
                    colorByPoint: true,
                    depth: 25,
                    dataLabels: {
                        enabled: false,
                        align: 'center'
                    }
                },
                series: {
                    animation: false
                }
            },
            legend: {
                enabled: false,
                backgroundColor: '#EEE',
                borderRadius: 3,
                padding: 15,
                style: {
                    overflow: 'hidden'
                },
                title: {
                    text: 'aaa',
                    style: {
                        fontSize: '14px'
                    }
                },
                itemStyle: {
                    fontSize: '11px',
                    width: '120px',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                },
                //itemWidth: 120,
                useHTML: true,
                layout: 'vertical',
                align: 'left',
                verticalAlign: 'top',
                symbolHeight: 8,
                symbolWidth: 8,
                symbolRadius: 4
            }
        });

    },

    onResize: function () {
        this.chart.reflow();
    },

    onSetSeries: function (data) {
        this.getView().lookupReference('loader').hide();

        while(this.chart.series.length > 0) {
            this.chart.series[0].remove(true);
        }

        //var entry = this.getViewModel().get('entry');
        var _mainData = [], _otherCumulateVal = 0, i;

        for (i = 0; i < data.length; i += 1) {
            if (i < this.entry.get('pieNumSlices')) {
                _mainData.push({
                    name: data[i][this.entry.get('pieGroupColumn')] !== undefined ? data[i][this.entry.get('pieGroupColumn')] : 'None',
                    y: data[i].value
                });
            } else {
                _otherCumulateVal += data[i].value;
            }
        }

        if (_otherCumulateVal > 0) {
            _mainData.push({
                name: 'Other',
                color: '#DDD',
                y: _otherCumulateVal
            });
        }

        //this.chart.series[0].setData(_mainData, true, true);
        this.chart.addSeries({
            name: 'aaa',
            type: this.entry.get('pieStyle').indexOf('COLUMN') >= 0 ? 'column' : 'pie',
            colors: this.entry.get('colors') || this.defaultColors,
            innerSize: this.entry.get('pieStyle').indexOf('DONUT') >= 0 ? '50%' : 0,
            data: _mainData
        }, true, true);
    },

    onSetStyle: function () {
        var me = this,
            style = me.getViewModel().get('entry.pieStyle'),
            colors = me.getViewModel().get('entry.colors') || this.defaultColors;
        if (this.chart) {
            this.chart.series[0].update({
                type: style.indexOf('COLUMN') >= 0 ? 'column' : 'pie',
                colors: colors,
                innerSize: style.indexOf('DONUT') >= 0 ? '50%' : 0
            });
        }
    },

    onBeginFetchData: function() {
        this.getView().lookupReference('loader').show();
    }

});
Ext.define('Ung.chart.PieChart', {
    extend: 'Ext.container.Container',
    alias: 'widget.piechart',
    requires: [
        'Ung.chart.PieChartController'
    ],

    controller: 'piechart',
    viewModel: true,

    config: {
        widget: null,
        entry: null
    },

    listeners: {
        afterrender: 'onAfterRender',
        resize: 'onResize',
        setseries: 'onSetSeries',
        //setstyle: 'onSetStyle',
        beginfetchdata: 'onBeginFetchData'
    },

    items: [{
        xtype: 'component',
        reference: 'piechart',
        cls: 'chart'
    }, {
        xtype: 'component',
        reference: 'loader',
        cls: 'loader',
        hideMode: 'visibility',
        html: '<div class="spinner"><div class="bounce1"></div><div class="bounce2"></div><div class="bounce3"></div></div>'
    }]
});
Ext.define('Ung.chart.EventChartController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.eventchart',

    control: {
        '#': { setdata: 'onSetData', beginfetchdata: 'onBeforeData' }
    },

    onBeforeData: function () {
        this.getView().setLoading('Querying Database...'.t());
    },

    onSetData: function (data) {
        this.getView().setLoading(false);
        this.getViewModel().set('customData', data);
        //console.log(data);
    }
});
Ext.define('Ung.util.TableConfig', {
    alternateClassName: 'Ung.TableConfig',
    singleton: true,

    columnWidth: {
        hostname: 120,
        ip: 100,
        port: 70,
        timestamp: 135,
        username: 120
    },

    table: {
        sessions: ['session_id', 'time_stamp', 'end_time', 'bypassed', 'entitled', 'protocol', 'icmp_type', 'hostname', 'username', 'policy_id', 'policy_rule_id', 'c_client_addr', 'c_client_port', 'c_server_addr', 'c_server_port', 's_client_addr', 's_client_port', 's_server_addr', 's_server_port', 'client_intf', 'server_intf', 'client_country', 'client_latitude', 'client_longitude', 'server_country', 'server_latitude', 'server_longitude', "c2p_bytes", "p2c_bytes", "s2p_bytes", "p2s_bytes", 'filter_prefix', 'firewall_blocked', 'firewall_flagged', 'firewall_rule_index', 'application_control_lite_blocked', 'application_control_lite_protocol', 'captive_portal_rule_index', 'captive_portal_blocked', 'application_control_application', 'application_control_protochain', 'application_control_category', 'application_control_flagged', 'application_control_blocked', 'application_control_confidence', 'application_control_detail', 'application_control_ruleid', 'bandwidth_control_priority', 'bandwidth_control_rule', 'ssl_inspector_status', 'ssl_inspector_detail', 'ssl_inspector_ruleid'],
        session_minutes: ['session_id', 'time_stamp', 'end_time', 'bypassed', 'entitled', 'protocol', 'icmp_type', 'hostname', 'username', 'policy_id', 'policy_rule_id', 'c_client_addr', 'c_client_port', 'c_server_addr', 'c_server_port', 's_client_addr', 's_client_port', 's_server_addr', 's_server_port', 'client_intf', 'server_intf', 'client_country', 'client_latitude', 'client_longitude', 'server_country', 'server_latitude', 'server_longitude', "c2p_bytes", "p2c_bytes", "s2p_bytes", "p2s_bytes", 'filter_prefix', 'firewall_blocked', 'firewall_flagged', 'firewall_rule_index', 'application_control_lite_blocked', 'application_control_lite_protocol', 'captive_portal_rule_index', 'captive_portal_blocked', 'application_control_application', 'application_control_protochain', 'application_control_category', 'application_control_flagged', 'application_control_blocked', 'application_control_confidence', 'application_control_detail', 'application_control_ruleid', 'bandwidth_control_priority', 'bandwidth_control_rule', 'ssl_inspector_status', 'ssl_inspector_detail', 'ssl_inspector_ruleid'],
        http_events: ['request_id', 'policy_id', 'time_stamp', 'session_id', 'client_intf', 'server_intf', 'c_client_addr', 'c_client_port', 'c_server_addr', 'c_server_port', 's_client_addr', 's_client_port', 's_server_addr', 's_server_port', 'username', 'hostname', 'method', 'domain', 'host', 'uri', 'referer', 'c2s_content_length', 's2c_content_length', 's2c_content_type', 'web_filter_lite_blocked', 'web_filter_blocked', 'web_filter_lite_flagged', 'web_filter_flagged', 'web_filter_lite_category', 'web_filter_category', 'web_filter_lite_reason', 'web_filter_reason', 'ad_blocker_action', 'ad_blocker_cookie_ident', 'virus_blocker_clean', 'virus_blocker_name', 'virus_blocker_lite_clean', 'virus_blocker_lite_name']
    },

    getColumns: function () {
        return {
            c_client_port: {
                header: 'Client Port'.t(),
                width: this.columnWidth.port,
                renderer: function (value) { return Ung.TableConfig.protocols[value] || value; }
            },
            hostname: {
                header: 'Hostname'.t(),
                width: this.columnWidth.hostname
            },
            protocol: {
                header: 'Protocol'.t(),
                width: this.columnWidth.port,
                renderer: function (value) { return Ung.TableConfig.protocols[value] || value; }
            },
            s_server_addr: {
                header: 'Server'.t(),
                width: this.columnWidth.ip
            },
            s_server_port: {
                header: 'Server Port'.t(),
                width: this.columnWidth.port
            },
            session_id: {
                header: 'Session Id'.t()
            },
            time_stamp: {
                header: 'Timestamp'.t(),
                width: this.columnWidth.timestamp,
                renderer: function (value) {
                    var date = new Date(value.time);
                    return Ext.Date.format(date, 'M d, H:i:s.u');
                }
            },
            username: {
                header: 'Username'.t(),
                width: this.columnWidth.username
            }
        };
    },

    protocols: {
        0: 'HOPOPT (0)',
        1: 'ICMP (1)',
        2: 'IGMP (2)',
        3: 'GGP (3)',
        4: 'IP-in-IP (4)',
        5: 'ST (5)',
        6: 'TCP (6)',
        7: 'CBT (7)',
        8: 'EGP (8)',
        9: 'IGP (9)',
        10: 'BBN-RCC-MON (10)',
        11: 'NVP-II (11)',
        12: 'PUP (12)',
        13: 'ARGUS (13)',
        14: 'EMCON (14)',
        15: 'XNET (15)',
        16: 'CHAOS (16)',
        17: 'UDP (17)',
        18: 'MUX (18)',
        19: 'DCN-MEAS (19)',
        20: 'HMP (20)',
        21: 'PRM (21)',
        22: 'XNS-IDP (22)',
        23: 'TRUNK-1 (23)',
        24: 'TRUNK-2 (24)',
        25: 'LEAF-1 (25)',
        26: 'LEAF-2 (26)',
        27: 'RDP (27)',
        28: 'IRTP (28)',
        29: 'ISO-TP4 (29)',
        30: 'NETBLT (30)',
        31: 'MFE-NSP (31)',
        32: 'MERIT-INP (32)',
        33: 'DCCP (33)',
        34: '3PC (34)',
        35: 'IDPR (35)',
        36: 'XTP (36)',
        37: 'DDP (37)',
        38: 'IDPR-CMTP (38)',
        39: 'TP++ (39)',
        40: 'IL (40)',
        41: 'IPv6 (41)',
        42: 'SDRP (42)',
        43: 'IPv6-Route (43)',
        44: 'IPv6-Frag (44)',
        45: 'IDRP (45)',
        46: 'RSVP (46)',
        47: 'GRE (47)',
        48: 'MHRP (48)',
        49: 'BNA (49)',
        50: 'ESP (50)',
        51: 'AH (51)',
        52: 'I-NLSP (52)',
        53: 'SWIPE (53)',
        54: 'NARP (54)',
        55: 'MOBILE (55)',
        56: 'TLSP (56)',
        57: 'SKIP (57)',
        58: 'IPv6-ICMP (58)',
        59: 'IPv6-NoNxt (59)',
        60: 'IPv6-Opts (60)',
        62: 'CFTP (62)',
        64: 'SAT-EXPAK (64)',
        65: 'KRYPTOLAN (65)',
        66: 'RVD (66)',
        67: 'IPPC (67)',
        69: 'SAT-MON (69)',
        70: 'VISA (70)',
        71: 'IPCU (71)',
        72: 'CPNX (72)',
        73: 'CPHB (73)',
        74: 'WSN (74)',
        75: 'PVP (75)',
        76: 'BR-SAT-MON (76)',
        77: 'SUN-ND (77)',
        78: 'WB-MON (78)',
        79: 'WB-EXPAK (79)',
        80: 'ISO-IP (80)',
        81: 'VMTP (81)',
        82: 'SECURE-VMTP (82)',
        83: 'VINES (83)',
        84: 'TTP (84)',
        85: 'NSFNET-IGP (85)',
        86: 'DGP (86)',
        87: 'TCF (87)',
        88: 'EIGRP (88)',
        89: 'OSPF (89)',
        90: 'Sprite-RPC (90)',
        91: 'LARP (91)',
        92: 'MTP (92)',
        93: 'AX.25 (93)',
        94: 'IPIP (94)',
        95: 'MICP (95)',
        96: 'SCC-SP (96)',
        97: 'ETHERIP (97)',
        98: 'ENCAP (98)',
        100: 'GMTP (100)',
        101: 'IFMP (101)',
        102: 'PNNI (102)',
        103: 'PIM (103)',
        104: 'ARIS (104)',
        105: 'SCPS (105)',
        106: 'QNX (106)',
        107: 'A/N (107)',
        108: 'IPComp (108)',
        109: 'SNP (109)',
        110: 'Compaq-Peer (110)',
        111: 'IPX-in-IP (111)',
        112: 'VRRP (112)',
        113: 'PGM (113)',
        115: 'L2TP (115)',
        116: 'DDX (116)',
        117: 'IATP (117)',
        118: 'STP (118)',
        119: 'SRP (119)',
        120: 'UTI (120)',
        121: 'SMP (121)',
        122: 'SM (122)',
        123: 'PTP (123)',
        124: 'IS-IS (124)',
        125: 'FIRE (125)',
        126: 'CRTP (126)',
        127: 'CRUDP (127)',
        128: 'SSCOPMCE (128)',
        129: 'IPLT (129)',
        130: 'SPS (130)',
        131: 'PIPE (131)',
        132: 'SCTP (132)',
        133: 'FC (133)',
        134: 'RSVP-E2E-IGNORE (134)',
        135: 'Mobility (135)',
        136: 'UDPLite (136)',
        137: 'MPLS-in-IP (137)',
        138: 'manet (138)',
        139: 'HIP (139)',
        140: 'Shim6 (140)',
        141: 'WESP (141)',
        142: 'ROHC (142)'
    }
});
Ext.define('Ung.chart.EventChart', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.eventchart',
    requires: [
        'Ung.chart.EventChartController',
        'Ung.util.TableConfig'
    ],

    controller: 'eventchart',
    viewModel: {
        stores: {
            store: {
                data: '{customData}'
            }
        }
    },

    plugins: 'gridfilters',

    config: {
        entry: null
    },

    bind: {
        store: '{store}'
    },

    border: false,

    initComponent: function () {
        var me = this,
            col, columns = [],
            columnsConfig = Ung.TableConfig.getColumns();

        Ung.TableConfig.table[me.getEntry().get('table')].forEach(function (column) {
            // add columns from TableConfig and apply dataIndex
            col = columnsConfig[column] || { header: column };
            Ext.apply(col, {
                dataIndex: column,
                hidden: !Ext.Array.contains(me.getEntry().get('defaultColumns'), column)
            });
            columns.push(col);
        });

        // me.getEntry().get('defaultColumns')

        //var filterFeature = Ext.create('Ung.chart.EventFilter', {});


        Ext.apply(this, {
            columns: columns
        });
        this.callParent(arguments);

        //this.getStore().addFilter(filterFeature.globalFilter);
    }

});
Ext.define('Ung.widget.Report', {
    extend: 'Ext.container.Container',
    alias: 'widget.reportwidget',
    requires: [
        //'Ung.widget.report.ReportController',
        'Ung.widget.ReportModel',
        'Ung.chart.TimeChart',
        'Ung.chart.PieChart',
        'Ung.chart.EventChart'
    ],

    controller: 'widget',
    viewModel: {
        type: 'reportwidget'
    },
    config: {
        widget: null,
        entry: null
    },

    hidden: true,
    bind: {
        hidden: '{!widget.enabled}'
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    border: false,
    baseCls: 'widget adding',

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        bind: {
            html: '{title}' +
                '<button class="action-btn"><i class="fa fa-refresh" data-action="refresh"></i></button>'
        }
    }],
    // {
    //     xtype: 'container',
    //     layout: {
    //         type: 'hbox',
    //         align: 'top'
    //     },
    //     cls: 'header',
    //     style: {
    //         height: '50px'
    //     },
    //     items: [{
    //         xtype: 'component',
    //         flex: 1,
    //         bind: {
    //             html: '{title}'
    //         }
    //     }, {
    //         xtype: 'container',
    //         margin: '10 5 0 0',
    //         layout: {
    //             type: 'hbox',
    //             align: 'middle'
    //         },
    //         items: [/*{
    //             xtype: 'button',
    //             baseCls: 'action',
    //             text: '<i class="material-icons">settings_ethernet</i>',
    //             listeners: {
    //                 click: 'resizeWidget'
    //             }
    //         }, {
    //             xtype: 'button',
    //             baseCls: 'action',
    //             text: '<i class="material-icons">settings</i>',
    //             listeners: {
    //                 click: 'showEditor'
    //             }
    //         },*/ {
    //             xtype: 'button',
    //             baseCls: 'action',
    //             text: '<i class="material-icons">refresh</i>',
    //             listeners: {
    //                 click: 'fetchData'
    //             }
    //         }, {
    //             xtype: 'button',
    //             baseCls: 'action',
    //             text: '<i class="material-icons">call_made</i>',
    //             bind: {
    //                 href: '#reports/{widget.entryId}'
    //             },
    //             hrefTarget: '_self'
    //         }]
    //     }]
    // }],

    fetchData: function () {
        var me = this,
            vm = this.getViewModel();

        if (vm) {
            var entry = vm.get('entry'),
                timeframe = vm.get('widget.timeframe');

            // console.log('fetch data - ', entry.get('title'));

            if (entry.get('type') === 'EVENT_LIST') {
                // fetch event data
                //console.log('Event List');
                me.fireEvent('afterdata');
            } else {
                // fetch chart data
                this.lookupReference('chart').fireEvent('beginfetchdata');
                Rpc.getReportData(entry.getData(), timeframe)
                    .then(function (response) {
                        me.fireEvent('afterdata');
                        me.lookupReference('chart').fireEvent('setseries', response.list);
                    }, function (exception) {
                        me.fireEvent('afterdata');
                        me.lookupReference('chart').lookupReference('loader').hide();
                        console.log(exception);
                        // Ung.Util.exceptionToast(exception);

                        if (me.down('#exception')) {
                            me.remove('exception');
                        }
                        me.add({
                            xtype: 'component',
                            itemId: 'exception',
                            cls: 'exception',
                            scrollable: true,
                            html: '<h3><i class="material-icons">error</i> <span>' + 'Widget error'.t() + '</span></h3>' +
                                '<p>' + 'There was an issue while fetching data!'.t() + '</p>'
                        });
                    });
            }
        }
    }
});
/**
 * Dashboard view which holds the widgets and manager
 */
Ext.define('Ung.view.dashboard.Dashboard', {
    extend: 'Ext.container.Container',
    xtype: 'ung.dashboard',
    itemId: 'dashboard',
    requires: [
        'Ung.view.dashboard.DashboardController',
        'Ung.view.dashboard.Queue',
        'Ung.widget.Report',

        'Ung.widget.WidgetController',
        'Ung.widget.Information',
        'Ung.widget.Resources',
        'Ung.widget.CpuLoad',
        'Ung.widget.NetworkInformation',
        'Ung.widget.NetworkLayout',
        'Ung.widget.MapDistribution'
    ],

    controller: 'dashboard',
    viewModel: true,
    //viewModel: 'dashboard',

    config: {
        settings: null // the dashboard settings object
    },

    layout: 'border',

    defaults: {
        border: false
    },

    items: [{
        region: 'west',
        title: 'Manage Widgets'.t(),
        // weight: 30,
        width: 300,
        collapsible: true,
        bodyBorder: true,
        // shadow: false,
        animCollapse: false,
        collapsed: true,
        // collapseMode: 'mini',
        titleCollapse: true,
        floatable: false,
        cls: 'widget-manager',
        split: true,
        xtype: 'grid',
        reference: 'dashboardNav',
        // forceFit: true,
        hideHeaders: true,
        // disableSelection: true,
        // trackMouseOver: false,

        store: 'widgets',

        bodyStyle: {
            border: 0
        },
        viewConfig: {
            plugins: {
                ptype: 'gridviewdragdrop',
                dragText: 'Drag and drop to reorganize'.t(),
                dragZone: {
                    onBeforeDrag: function (data, e) {
                        return Ext.get(e.target).hasCls('drag-handle');
                    }
                }
            },
            stripeRows: false,
            getRowClass: function (record) {
                return !record.get('enabled') ? 'disabled' : '';
            },
            listeners: {
                drop: 'onDrop'
            }
        },
        columns: [{
            width: 14,
            align: 'center',
            sortable: false,
            hideable: false,
            resizable: false,
            menuDisabled: true,
            tdCls: 'drag-handle'
        }, {
            width: 30,
            align: 'center',
            sortable: false,
            hideable: false,
            resizable: false,
            menuDisabled: true,
            //handler: 'toggleWidgetEnabled',
            dataIndex: 'enabled',
            renderer: 'enableRenderer'
        }, {
            dataIndex: 'entryId',
            renderer: 'widgetTitleRenderer',
            flex: 1
        }, {
            xtype: 'actioncolumn',
            width: 30,
            align: 'center',
            sortable: false,
            hideable: false,
            resizable: false,
            menuDisabled: true,
            handler: 'removeWidget',
            renderer: function (value, meta, record) {
                return '<i class="fa fa-times" style="color: #999; font-size: 20px;"></i>';
            }
        }/*, {
            xtype: 'actioncolumn',
            align: 'center',
            width: 25,
            sortable: false,
            hideable: false,
            resizable: false,
            menuDisabled: true,
            renderer: function (value, meta, record) {
                if (record.get('type') !== 'ReportEntry') {
                    return '';
                }
                return '<i style="font-size: 16px; color: #777; padding-top: 4px;" class="material-icons">settings</i>';
            }
        }*/],
        listeners: {
            itemmouseleave : 'onItemLeave',
            cellclick: 'onItemClick'
        },
        tbar: [{
            itemId: 'addWidgetBtn',
            text: 'Add'.t(),
            iconCls: 'fa fa-plus-circle'
            // menu: Ext.create('Ext.menu.Menu', {
            //     mouseLeaveDelay: 0
            // })
        }, '->', {
            text: 'Import'.t(),
            iconCls: 'fa fa-download'
            // handler: 'applyChanges'
        }, {
            text: 'Export'.t(),
            iconCls: 'fa fa-upload'
            //handler: 'applyChanges'
        }],
        bbar: [{
            text: 'Reset'.t(),
            iconCls: 'fa fa-rotate-left',
            handler: 'resetDashboard'
        }, '->', {
            text: 'Apply'.t(),
            iconCls: 'fa fa-floppy-o',
            handler: 'applyChanges'
        }]
    }, {
        xtype: 'container',
        region: 'center',
        reference: 'dashboard',
        cls: 'dashboard',
        padding: 8,
        scrollable: true
        // dockedItems: [{
        //     xtype: 'toolbar',
        //     dock: 'top',
        //     border: false,
        //     items: [{
        //         xtype: 'button',
        //         text: 'Manage Widgets'.t()
        //     }]
        // }]
    }],
    listeners: {
        //afterrender: 'onAfterRender',
        showwidgeteditor: 'showWidgetEditor'
    }
});
Ext.define('Ung.view.main.Main', {
    extend: 'Ext.panel.Panel',
    itemId: 'main',
    //xtype: 'ung-main',

    // plugins: [
    //     'viewport'
    // ],

    requires: [
        // 'Ext.plugin.Viewport',
        'Ung.view.main.MainController',
        'Ung.view.main.MainModel',
        'Ung.view.dashboard.Dashboard',
        'Ung.view.apps.Apps',
        'Ung.view.config.Config',
        'Ung.view.reports.Reports',
        // 'Ung.view.node.Settings',

        // 'Ung.view.shd.Sessions',
        // 'Ung.view.shd.Hosts',
        // 'Ung.view.shd.Devices'
    ],


    controller: 'main',
    // viewModel: true,
    viewModel: {
        type: 'main'
    },

    layout: 'card',
    border: false,

    bind: {
        activeItem: '{activeItem}'
    },

    items: [{
        xtype: 'ung.dashboard'
    }, {
        xtype: 'ung.apps'
    }, {
        xtype: 'ung.config'
    }, {
        xtype: 'ung.reports'
    }, {
        // xtype: 'ung.hosts',
        // itemId: 'hosts'
    }, {
        // xtype: 'ung.devices',
        // itemId: 'devices'
    }],

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        defaults: {
            xtype: 'button',
            border: false,
            enableToggle: true,
            iconAlign: 'top',
            // border: '0 0 5 0',
            hrefTarget: '_self'
        },
        items: [
            { html: '<img src="' + resourcesBaseHref + '/images/BrandingLogo.png" style="height: 40px;"/>', cls: 'logo' },
            { text: 'Dashboard'.t(), iconCls: 'fa fa-home', cls: 'upper', href: '#', bind: { pressed: '{selectedNavItem === "dashboard"}' } },
            { text: 'Apps'.t(), iconCls: 'fa fa-th', cls: 'upper', bind: { href: '#apps/{policyId}', pressed: '{selectedNavItem === "apps"}' } },
            { text: 'Config'.t(), iconCls: 'fa fa-sliders', cls: 'upper', href: '#config', bind: { pressed: '{selectedNavItem === "config"}' } },
            { text: 'Reports'.t(), iconCls: 'fa fa-line-chart', cls: 'upper', href: '#reports', bind: { pressed: '{selectedNavItem === "reports"}' } },
            '->',
            { text: 'Sessions'.t(), iconCls: 'fa fa-list', href: '#sessions', bind: { pressed: '{selectedNavItem === "sessions"}' } },
            { text: 'Hosts'.t(), iconCls: 'fa fa-th-list', href: '#hosts', bind: { pressed: '{selectedNavItem === "hosts"}' } },
            { text: 'Devices'.t(), iconCls: 'fa fa-desktop', href: '#devices', bind: { pressed: '{selectedNavItem === "devices"}' } },
            '-',
            { text: 'Help'.t(), iconCls: 'fa fa-question-circle' },
            { text: 'Account'.t(), iconCls: 'fa fa-user-circle' }
        ]
    }]


    // items: [{
    //     region: 'north',
    //     layout: { type: 'hbox', align: 'middle' },
    //     border: false,
    //     height: 66,
    //     // ui: 'navigation',
    //     items: [{
    //         xtype: 'container',
    //         layout: { type: 'hbox', align: 'middle' },
    //         defaults: {
    //             xtype: 'button',
    //             enableToggle: true,
    //             baseCls: 'nav-item',
    //             height: 30,
    //             hrefTarget: '_self'
    //         },
    //         items: [{
    //             enableToggle: false,
    //             html: '<img src="' + resourcesBaseHref + '/images/BrandingLogo.png" style="height: 40px;"/>',
    //             width: 100,
    //             height: 40,
    //             href: '#'
    //         }, {
    //             text: 'Dashboard',
    //             iconCls: 'fa fa-home',
    //             href: '#',
    //             bind: {
    //                 pressed: '{isDashboard}'
    //             }
    //         }, {
    //             html: Ung.Util.iconTitle('Apps'.t(), 'apps-16'),
    //             bind: {
    //                 href: '#apps/{policyId}',
    //                 pressed: '{isApps}'
    //             }
    //         }, {
    //             html: Ung.Util.iconTitle('Config'.t(), 'tune-16'),
    //             href: '#config',
    //             bind: {
    //                 pressed: '{isConfig}'
    //             }
    //         }, {
    //             html: Ung.Util.iconTitle('Reports'.t(), 'show_chart-16'),
    //             href: '#reports',
    //             bind: {
    //                 //html: '{reportsEnabled}',
    //                 hidden: '{!reportsEnabled}',
    //                 pressed: '{isReports}'
    //             }
    //         }]
    //     }]
    // }, {
    //     xtype: 'container',
    //     region: 'center',
    //     layout: 'card',
    //     itemId: 'main',
    //     border: false,
    //     bind: {
    //         activeItem: '{activeItem}'
    //     },
    //     items: [{
    //         xtype: 'ung.dashboard',
    //         itemId: 'dashboard'
    //     },
    //     {
    //         xtype: 'ung.apps',
    //         itemId: 'apps'
    //     }, {
    //         xtype: 'ung.config',
    //         itemId: 'config'
    //     }, {
    //         xtype: 'ung.configsettings',
    //         itemId: 'configsettings'
    //     }, {
    //         xtype: 'ung.appsinstall',
    //         itemId: 'appsinstall'
    //     }, {
    //         xtype: 'ung.nodesettings',
    //         itemId: 'settings'
    //     },
    //     {
    //         layout: 'border',
    //         itemId: 'shd', // sessions hosts devices
    //         border: false,
    //         items: [{
    //             region: 'north',
    //             weight: 20,
    //             border: false,
    //             height: 44,
    //             bodyStyle: {
    //                 background: '#555',
    //                 padding: '0 5px'
    //             },
    //             layout: {
    //                 type: 'hbox',
    //                 align: 'middle'
    //             },
    //             defaults: {
    //                 xtype: 'button',
    //                 enableToggle: true,
    //                 baseCls: 'heading-btn',
    //                 hrefTarget: '_self'
    //             },
    //             items: [{
    //                 html: Ung.Util.iconTitle('Back to Dashboard', 'keyboard_arrow_left-16'),
    //                 enableToggle: false,
    //                 href: '#',
    //                 hrefTarget: '_self'
    //             }, {
    //                 xtype: 'component',
    //                 flex: 1
    //             }, {
    //                 html: 'Sessions'.t(),
    //                 href: '#sessions',
    //                 bind: {
    //                     pressed: '{isSessions}'
    //                 }
    //             }, {
    //                 html: 'Hosts'.t(),
    //                 href: '#hosts',
    //                 bind: {
    //                     pressed: '{isHosts}'
    //                 }
    //             }, {
    //                 html: 'Devices'.t(),
    //                 href: '#devices',
    //                 bind: {
    //                     pressed: '{isDevices}'
    //                 }
    //             }]
    //         }, {
    //             region: 'center',
    //             layout: 'card',
    //             itemId: 'shdcenter',
    //             items: [{
    //                 xtype: 'ung.sessions',
    //                 itemId: 'sessions'
    //             }, {
    //                 xtype: 'ung.hosts',
    //                 itemId: 'hosts'
    //             }, {
    //                 xtype: 'ung.devices',
    //                 itemId: 'devices'
    //             }]
    //         }]
    //     }
    //     ]
    // }]
});

Ext.define('Ung.store.Metrics', {
    extend: 'Ext.data.Store',
    storeId: 'metrics',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
            //rootProperty: 'users'
        }
    }
});
Ext.define('Ung.store.Hosts', {
    extend: 'Ext.data.Store',
    storeId: 'hosts',
    // model: 'Ung.model.Session'
    fields: [
        { name: 'address', type: 'string' }
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
            // rootProperty: 'list'
        }
    }

});
Ext.define('Ung.store.Conditions', {
    extend: 'Ext.data.Store',
    storeId: 'conditions',
    fields: ['name', 'displayName', 'type'],
    data: [
        {name:'DST_ADDR', displayName: 'Destination Address'.t(), editorType: 'textfield', vtype:'ipall', visible: true },
        {name:'DST_PORT',displayName: 'Destination Port'.t(), editorType: 'textfield', vtype:'port', visible: true },
        {name:'DST_INTF',displayName: 'Destination Interface'.t(), editorType: 'checkgroup', /*values: Ung.Util.getInterfaceList(true, false),*/ visible: true},
        {name:'SRC_ADDR',displayName: 'Source Address'.t(), editorType: 'textfield', visible: true, vtype:'ipall'},
        {name:'SRC_PORT',displayName: 'Source Port'.t(), editorType: 'textfield', vtype:'portMatcher', visible: rpc.isExpertMode},
        {name:'SRC_INTF',displayName: 'Source Interface'.t(), editorType: 'checkgroup', /*values: Ung.Util.getInterfaceList(true, false),*/ visible: true},
        {name:'PROTOCOL',displayName: 'Protocol'.t(), editorType: 'checkgroup', values: [['TCP','TCP'],['UDP','UDP'],['any','any']], visible: true},
        {name:'USERNAME',displayName: 'Username'.t(), editorType: 'userselection', /*editor: Ext.create('Ung.UserEditorWindow',{}),*/ visible: true},
        {name:'CLIENT_HOSTNAME',displayName: 'Client Hostname'.t(), editorType: 'textfield', visible: true},
        {name:'SERVER_HOSTNAME',displayName: 'Server Hostname'.t(), editorType: 'textfield', visible: rpc.isExpertMode},
        {name:'SRC_MAC', displayName: 'Client MAC Address'.t(), editorType: 'textfield', visible: true },
        {name:'DST_MAC', displayName: 'Server MAC Address'.t(), editorType: 'textfield', visible: true },
        {name:'CLIENT_MAC_VENDOR',displayName: 'Client MAC Vendor'.t(), editorType: 'textfield', visible: true},
        {name:'SERVER_MAC_VENDOR',displayName: 'Server MAC Vendor'.t(), editorType: 'textfield', visible: true},
        {name:'CLIENT_IN_PENALTY_BOX',displayName: 'Client in Penalty Box'.t(), editorType: 'boolean', visible: true},
        {name:'SERVER_IN_PENALTY_BOX',displayName: 'Server in Penalty Box'.t(), editorType: 'boolean', visible: true},
        {name:'CLIENT_HAS_NO_QUOTA',displayName: 'Client has no Quota'.t(), editorType: 'boolean', visible: true},
        {name:'SERVER_HAS_NO_QUOTA',displayName: 'Server has no Quota'.t(), editorType: 'boolean', visible: true},
        {name:'CLIENT_QUOTA_EXCEEDED',displayName: 'Client has exceeded Quota'.t(), editorType: 'boolean', visible: true},
        {name:'SERVER_QUOTA_EXCEEDED',displayName: 'Server has exceeded Quota'.t(), editorType: 'boolean', visible: true},
        {name:'CLIENT_QUOTA_ATTAINMENT',displayName: 'Client Quota Attainment'.t(), editorType: 'textfield', visible: true},
        {name:'SERVER_QUOTA_ATTAINMENT',displayName: 'Server Quota Attainment'.t(), editorType: 'textfield', visible: true},
        {name:'HTTP_HOST',displayName: 'HTTP: Hostname'.t(), editorType: 'textfield', visible: true},
        {name:'HTTP_REFERER',displayName: 'HTTP: Referer'.t(), editorType: 'textfield', visible: true},
        {name:'HTTP_URI',displayName: 'HTTP: URI'.t(), editorType: 'textfield', visible: true},
        {name:'HTTP_URL',displayName: 'HTTP: URL'.t(), editorType: 'textfield', visible: true},
        {name:'HTTP_CONTENT_TYPE',displayName: 'HTTP: Content Type'.t(), editorType: 'textfield', visible: true},
        {name:'HTTP_CONTENT_LENGTH',displayName: 'HTTP: Content Length'.t(), editorType: 'textfield', visible: true},
        {name:'HTTP_USER_AGENT',displayName: 'HTTP: Client User Agent'.t(), editorType: 'textfield', visible: true},
        {name:'HTTP_USER_AGENT_OS',displayName: 'HTTP: Client User OS'.t(), editorType: 'textfield', visible: false},
        {name:'APPLICATION_CONTROL_APPLICATION',displayName: 'Application Control: Application'.t(), editorType: 'textfield', visible: true},
        {name:'APPLICATION_CONTROL_CATEGORY',displayName: 'Application Control: Application Category'.t(), editorType: 'textfield', visible: true},
        {name:'APPLICATION_CONTROL_PROTOCHAIN',displayName: 'Application Control: Protochain'.t(), editorType: 'textfield', visible: true},
        {name:'APPLICATION_CONTROL_DETAIL',displayName: 'Application Control: Detail'.t(), editorType: 'textfield', visible: true},
        {name:'APPLICATION_CONTROL_CONFIDENCE',displayName: 'Application Control: Confidence'.t(), editorType: 'textfield', visible: true},
        {name:'APPLICATION_CONTROL_PRODUCTIVITY',displayName: 'Application Control: Productivity'.t(), editorType: 'textfield', visible: true},
        {name:'APPLICATION_CONTROL_RISK',displayName: 'Application Control: Risk'.t(), editorType: 'textfield', visible: true},
        {name:'PROTOCOL_CONTROL_SIGNATURE',displayName: 'Application Control Lite: Signature'.t(), editorType: 'textfield', visible: true},
        {name:'PROTOCOL_CONTROL_CATEGORY',displayName: 'Application Control Lite: Category'.t(), editorType: 'textfield', visible: true},
        {name:'PROTOCOL_CONTROL_DESCRIPTION',displayName: 'Application Control Lite: Description'.t(), editorType: 'textfield', visible: true},
        {name:'WEB_FILTER_CATEGORY',displayName: 'Web Filter: Category'.t(), editorType: 'textfield', visible: true},
        {name:'WEB_FILTER_CATEGORY_DESCRIPTION',displayName: 'Web Filter: Category Description'.t(), editorType: 'textfield', visible: true},
        {name:'WEB_FILTER_FLAGGED',displayName: 'Web Filter: Website is Flagged'.t(), editorType: 'boolean', visible: true},
        {name:'DIRECTORY_CONNECTOR_GROUP',displayName: 'Directory Connector: User in Group'.t(), type: 'editor', /*editor: Ext.create('Ung.GroupEditorWindow',{}),*/ visible: true},
        {name:'CLIENT_COUNTRY',displayName: 'Client Country'.t(), editorType: 'countryselector', /*editor: Ext.create('Ung.CountryEditorWindow',{}),*/ visible: true},
        {name:'SERVER_COUNTRY',displayName: 'Server Country'.t(), editorType: 'countryselector', /*editor: Ext.create('Ung.CountryEditorWindow',{}),*/ visible: true}
    ]


});
Ext.define('Ung.store.Countries', {
    extend: 'Ext.data.Store',
    storeId: 'countries',
    data: [
        { code: 'AF', name: 'Afghanistan'.t() },
        { code: 'AX', name: 'Aland Islands'.t() },
        { code: 'AL', name: 'Albania'.t() },
        { code: 'DZ', name: 'Algeria'.t() },
        { code: 'AS', name: 'American Samoa'.t() },
        { code: 'AD', name: 'Andorra'.t() },
        { code: 'AO', name: 'Angola'.t() },
        { code: 'AI', name: 'Anguilla'.t() },
        { code: 'AQ', name: 'Antarctica'.t() },
        { code: 'AG', name: 'Antigua and Barbuda'.t() },
        { code: 'AR', name: 'Argentina'.t() },
        { code: 'AM', name: 'Armenia'.t() },
        { code: 'AW', name: 'Aruba'.t() },
        { code: 'AU', name: 'Australia'.t() },
        { code: 'AT', name: 'Austria'.t() },
        { code: 'AZ', name: 'Azerbaijan'.t() },
        { code: 'BS', name: 'Bahamas'.t() },
        { code: 'BH', name: 'Bahrain'.t() },
        { code: 'BD', name: 'Bangladesh'.t() },
        { code: 'BB', name: 'Barbados'.t() },
        { code: 'BY', name: 'Belarus'.t() },
        { code: 'BE', name: 'Belgium'.t() },
        { code: 'BZ', name: 'Belize'.t() },
        { code: 'BJ', name: 'Benin'.t() },
        { code: 'BM', name: 'Bermuda'.t() },
        { code: 'BT', name: 'Bhutan'.t() },
        { code: 'BO', name: 'Bolivia, Plurinational State of'.t() },
        { code: 'BQ', name: 'Bonaire, Sint Eustatius and Saba'.t() },
        { code: 'BA', name: 'Bosnia and Herzegovina'.t() },
        { code: 'BW', name: 'Botswana'.t() },
        { code: 'BV', name: 'Bouvet Island'.t() },
        { code: 'BR', name: 'Brazil'.t() },
        { code: 'IO', name: 'British Indian Ocean Territory'.t() },
        { code: 'BN', name: 'Brunei Darussalam'.t() },
        { code: 'BG', name: 'Bulgaria'.t() },
        { code: 'BF', name: 'Burkina Faso'.t() },
        { code: 'BI', name: 'Burundi'.t() },
        { code: 'KH', name: 'Cambodia'.t() },
        { code: 'CM', name: 'Cameroon'.t() },
        { code: 'CA', name: 'Canada'.t() },
        { code: 'CV', name: 'Cape Verde'.t() },
        { code: 'KY', name: 'Cayman Islands'.t() },
        { code: 'CF', name: 'Central African Republic'.t() },
        { code: 'TD', name: 'Chad'.t() },
        { code: 'CL', name: 'Chile'.t() },
        { code: 'CN', name: 'China'.t() },
        { code: 'CX', name: 'Christmas Island'.t() },
        { code: 'CC', name: 'Cocos (Keeling) Islands'.t() },
        { code: 'CO', name: 'Colombia'.t() },
        { code: 'KM', name: 'Comoros'.t() },
        { code: 'CG', name: 'Congo'.t() },
        { code: 'CD', name: 'Congo, the Democratic Republic of the'.t() },
        { code: 'CK', name: 'Cook Islands'.t() },
        { code: 'CR', name: 'Costa Rica'.t() },
        { code: 'CI', name: 'Cote d\'Ivoire'.t() },
        { code: 'HR', name: 'Croatia'.t() },
        { code: 'CU', name: 'Cuba'.t() },
        { code: 'CW', name: 'Curacao'.t() },
        { code: 'CY', name: 'Cyprus'.t() },
        { code: 'CZ', name: 'Czech Republic'.t() },
        { code: 'DK', name: 'Denmark'.t() },
        { code: 'DJ', name: 'Djibouti'.t() },
        { code: 'DM', name: 'Dominica'.t() },
        { code: 'DO', name: 'Dominican Republic'.t() },
        { code: 'EC', name: 'Ecuador'.t() },
        { code: 'EG', name: 'Egypt'.t() },
        { code: 'SV', name: 'El Salvador'.t() },
        { code: 'GQ', name: 'Equatorial Guinea'.t() },
        { code: 'ER', name: 'Eritrea'.t() },
        { code: 'EE', name: 'Estonia'.t() },
        { code: 'ET', name: 'Ethiopia'.t() },
        { code: 'FK', name: 'Falkland Islands (Malvinas)'.t() },
        { code: 'FO', name: 'Faroe Islands'.t() },
        { code: 'FJ', name: 'Fiji'.t() },
        { code: 'FI', name: 'Finland'.t() },
        { code: 'FR', name: 'France'.t() },
        { code: 'GF', name: 'French Guiana'.t() },
        { code: 'PF', name: 'French Polynesia'.t() },
        { code: 'TF', name: 'French Southern Territories'.t() },
        { code: 'GA', name: 'Gabon'.t() },
        { code: 'GM', name: 'Gambia'.t() },
        { code: 'GE', name: 'Georgia'.t() },
        { code: 'DE', name: 'Germany'.t() },
        { code: 'GH', name: 'Ghana'.t() },
        { code: 'GI', name: 'Gibraltar'.t() },
        { code: 'GR', name: 'Greece'.t() },
        { code: 'GL', name: 'Greenland'.t() },
        { code: 'GD', name: 'Grenada'.t() },
        { code: 'GP', name: 'Guadeloupe'.t() },
        { code: 'GU', name: 'Guam'.t() },
        { code: 'GT', name: 'Guatemala'.t() },
        { code: 'GG', name: 'Guernsey'.t() },
        { code: 'GN', name: 'Guinea'.t() },
        { code: 'GW', name: 'Guinea-Bissau'.t() },
        { code: 'GY', name: 'Guyana'.t() },
        { code: 'HT', name: 'Haiti'.t() },
        { code: 'HM', name: 'Heard Island and McDonald Islands'.t() },
        { code: 'VA', name: 'Holy See (Vatican City State)'.t() },
        { code: 'HN', name: 'Honduras'.t() },
        { code: 'HK', name: 'Hong Kong'.t() },
        { code: 'HU', name: 'Hungary'.t() },
        { code: 'IS', name: 'Iceland'.t() },
        { code: 'IN', name: 'India'.t() },
        { code: 'ID', name: 'Indonesia'.t() },
        { code: 'IR', name: 'Iran, Islamic Republic of'.t() },
        { code: 'IQ', name: 'Iraq'.t() },
        { code: 'IE', name: 'Ireland'.t() },
        { code: 'IM', name: 'Isle of Man'.t() },
        { code: 'IL', name: 'Israel'.t() },
        { code: 'IT', name: 'Italy'.t() },
        { code: 'JM', name: 'Jamaica'.t() },
        { code: 'JP', name: 'Japan'.t() },
        { code: 'JE', name: 'Jersey'.t() },
        { code: 'JO', name: 'Jordan'.t() },
        { code: 'KZ', name: 'Kazakhstan'.t() },
        { code: 'KE', name: 'Kenya'.t() },
        { code: 'KI', name: 'Kiribati'.t() },
        { code: 'KP', name: 'Korea, Democratic People\'s Republic of'.t() },
        { code: 'KR', name: 'Korea, Republic of'.t() },
        { code: 'KW', name: 'Kuwait'.t() },
        { code: 'KG', name: 'Kyrgyzstan'.t() },
        { code: 'LA', name: 'Lao People\'s Democratic Republic'.t() },
        { code: 'LV', name: 'Latvia'.t() },
        { code: 'LB', name: 'Lebanon'.t() },
        { code: 'LS', name: 'Lesotho'.t() },
        { code: 'LR', name: 'Liberia'.t() },
        { code: 'LY', name: 'Libya'.t() },
        { code: 'LI', name: 'Liechtenstein'.t() },
        { code: 'LT', name: 'Lithuania'.t() },
        { code: 'LU', name: 'Luxembourg'.t() },
        { code: 'MO', name: 'Macao'.t() },
        { code: 'MK', name: 'Macedonia, the Former Yugoslav Republic of'.t() },
        { code: 'MG', name: 'Madagascar'.t() },
        { code: 'MW', name: 'Malawi'.t() },
        { code: 'MY', name: 'Malaysia'.t() },
        { code: 'MV', name: 'Maldives'.t() },
        { code: 'ML', name: 'Mali'.t() },
        { code: 'MT', name: 'Malta'.t() },
        { code: 'MH', name: 'Marshall Islands'.t() },
        { code: 'MQ', name: 'Martinique'.t() },
        { code: 'MR', name: 'Mauritania'.t() },
        { code: 'MU', name: 'Mauritius'.t() },
        { code: 'YT', name: 'Mayotte'.t() },
        { code: 'MX', name: 'Mexico'.t() },
        { code: 'FM', name: 'Micronesia, Federated States of'.t() },
        { code: 'MD', name: 'Moldova, Republic of'.t() },
        { code: 'MC', name: 'Monaco'.t() },
        { code: 'MN', name: 'Mongolia'.t() },
        { code: 'ME', name: 'Montenegro'.t() },
        { code: 'MS', name: 'Montserrat'.t() },
        { code: 'MA', name: 'Morocco'.t() },
        { code: 'MZ', name: 'Mozambique'.t() },
        { code: 'MM', name: 'Myanmar'.t() },
        { code: 'NA', name: 'Namibia'.t() },
        { code: 'NR', name: 'Nauru'.t() },
        { code: 'NP', name: 'Nepal'.t() },
        { code: 'NL', name: 'Netherlands'.t() },
        { code: 'NC', name: 'New Caledonia'.t() },
        { code: 'NZ', name: 'New Zealand'.t() },
        { code: 'NI', name: 'Nicaragua'.t() },
        { code: 'NE', name: 'Niger'.t() },
        { code: 'NG', name: 'Nigeria'.t() },
        { code: 'NU', name: 'Niue'.t() },
        { code: 'NF', name: 'Norfolk Island'.t() },
        { code: 'MP', name: 'Northern Mariana Islands'.t() },
        { code: 'NO', name: 'Norway'.t() },
        { code: 'OM', name: 'Oman'.t() },
        { code: 'PK', name: 'Pakistan'.t() },
        { code: 'PW', name: 'Palau'.t() },
        { code: 'PS', name: 'Palestine, State of'.t() },
        { code: 'PA', name: 'Panama'.t() },
        { code: 'PG', name: 'Papua New Guinea'.t() },
        { code: 'PY', name: 'Paraguay'.t() },
        { code: 'PE', name: 'Peru'.t() },
        { code: 'PH', name: 'Philippines'.t() },
        { code: 'PN', name: 'Pitcairn'.t() },
        { code: 'PL', name: 'Poland'.t() },
        { code: 'PT', name: 'Portugal'.t() },
        { code: 'PR', name: 'Puerto Rico'.t() },
        { code: 'QA', name: 'Qatar'.t() },
        { code: 'RE', name: 'Reunion'.t() },
        { code: 'RO', name: 'Romania'.t() },
        { code: 'RU', name: 'Russian Federation'.t() },
        { code: 'RW', name: 'Rwanda'.t() },
        { code: 'BL', name: 'Saint Barthelemy'.t() },
        { code: 'SH', name: 'Saint Helena, Ascension and Tristan da Cunha'.t() },
        { code: 'KN', name: 'Saint Kitts and Nevis'.t() },
        { code: 'LC', name: 'Saint Lucia'.t() },
        { code: 'MF', name: 'Saint Martin (French part)'.t() },
        { code: 'PM', name: 'Saint Pierre and Miquelon'.t() },
        { code: 'VC', name: 'Saint Vincent and the Grenadines'.t() },
        { code: 'WS', name: 'Samoa'.t() },
        { code: 'SM', name: 'San Marino'.t() },
        { code: 'ST', name: 'Sao Tome and Principe'.t() },
        { code: 'SA', name: 'Saudi Arabia'.t() },
        { code: 'SN', name: 'Senegal'.t() },
        { code: 'RS', name: 'Serbia'.t() },
        { code: 'SC', name: 'Seychelles'.t() },
        { code: 'SL', name: 'Sierra Leone'.t() },
        { code: 'SG', name: 'Singapore'.t() },
        { code: 'SX', name: 'Sint Maarten (Dutch part)'.t() },
        { code: 'SK', name: 'Slovakia'.t() },
        { code: 'SI', name: 'Slovenia'.t() },
        { code: 'SB', name: 'Solomon Islands'.t() },
        { code: 'SO', name: 'Somalia'.t() },
        { code: 'ZA', name: 'South Africa'.t() },
        { code: 'GS', name: 'South Georgia and the South Sandwich Islands'.t() },
        { code: 'SS', name: 'South Sudan'.t() },
        { code: 'ES', name: 'Spain'.t() },
        { code: 'LK', name: 'Sri Lanka'.t() },
        { code: 'SD', name: 'Sudan'.t() },
        { code: 'SR', name: 'Suriname'.t() },
        { code: 'SJ', name: 'Svalbard and Jan Mayen'.t() },
        { code: 'SZ', name: 'Swaziland'.t() },
        { code: 'SE', name: 'Sweden'.t() },
        { code: 'CH', name: 'Switzerland'.t() },
        { code: 'SY', name: 'Syrian Arab Republic'.t() },
        { code: 'TW', name: 'Taiwan, Province of China'.t() },
        { code: 'TJ', name: 'Tajikistan'.t() },
        { code: 'TZ', name: 'Tanzania, United Republic of'.t() },
        { code: 'TH', name: 'Thailand'.t() },
        { code: 'TL', name: 'Timor-Leste'.t() },
        { code: 'TG', name: 'Togo'.t() },
        { code: 'TK', name: 'Tokelau'.t() },
        { code: 'TO', name: 'Tonga'.t() },
        { code: 'TT', name: 'Trinidad and Tobago'.t() },
        { code: 'TN', name: 'Tunisia'.t() },
        { code: 'TR', name: 'Turkey'.t() },
        { code: 'TM', name: 'Turkmenistan'.t() },
        { code: 'TC', name: 'Turks and Caicos Islands'.t() },
        { code: 'TV', name: 'Tuvalu'.t() },
        { code: 'UG', name: 'Uganda'.t() },
        { code: 'UA', name: 'Ukraine'.t() },
        { code: 'AE', name: 'United Arab Emirates'.t() },
        { code: 'GB', name: 'United Kingdom'.t() },
        { code: 'US', name: 'United States'.t() },
        { code: 'UM', name: 'United States Minor Outlying Islands'.t() },
        { code: 'UY', name: 'Uruguay'.t() },
        { code: 'UZ', name: 'Uzbekistan'.t() },
        { code: 'VU', name: 'Vanuatu'.t() },
        { code: 'VE', name: 'Venezuela, Bolivarian Republic of'.t() },
        { code: 'VN', name: 'Viet Nam'.t() },
        { code: 'VG', name: 'Virgin Islands, British'.t() },
        { code: 'VI', name: 'Virgin Islands, U.S.'.t() },
        { code: 'WF', name: 'Wallis and Futuna'.t() },
        { code: 'EH', name: 'Western Sahara'.t() },
        { code: 'YE', name: 'Yemen'.t() },
        { code: 'ZM', name: 'Zambia'.t() },
        { code: 'ZW', name: 'Zimbabwe'.t() }
    ]
});
Ext.define('Ung.store.UnavailableApps', {
    extend: 'Ext.data.Store',
    storeId: 'unavailableApps',
    alias: 'store.unavailableApps',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
Ext.define ('Ung.model.Policy', {
    extend: 'Ext.data.Model' ,
    fields: [
        {name: 'policyId', type: 'int'},
        {name: 'displayName', type: 'string', convert: function (value, record) {
            return 'Policy ' + record.get('policyId');
        }}
    ],
    hasMany: {
        model: 'Ung.model.NodeProperty',
        name: 'nodeProperties'
    },
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});

Ext.define('Ung.store.Policies', {
    extend: 'Ext.data.Store',
    alias: 'store.policies',
    storeId: 'policies',
    model: 'Ung.model.Policy'
});
Ext.define ('Ung.model.Stat', {
    extend: 'Ext.data.Model' ,
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: ['numCpus', 'cpuModel', 'MemFree', 'MemTotal', 'SwapFree', 'SwapTotal', 'freeDiskSpace', 'totalDiskSpace', 'uptime',
        {
            name: 'hostname',
            calculate: function () {
                return rpc.hostname;
            }
        }, {
            name: 'version',
            calculate: function () {
                return rpc.fullVersion;
            }
        }, {
            name: 'appliance',
            calculate: function() {
                return (rpc.applianceModel === undefined || rpc.applianceModel === null || rpc.applianceModel === '' ? 'custom'.t() : rpc.applianceModel);
            }
        }, {
            name: 'totalMemory',
            calculate: function (data) {
                return Ung.Util.formatBytes(data.MemTotal, 2);
            }
        }, {
            name: 'freeMemory',
            calculate: function (data) {
                return Ung.Util.formatBytes(data.MemFree, 2);
            }
        }, {
            name: 'freeMemoryPercent',
            calculate: function (data) {
                // return (Math.random() * 100).toFixed(2);
                return (data.MemFree / data.MemTotal * 100).toFixed(1);
            }
        }, {
            name: 'usedMemory',
            calculate: function (data) {
                return Ung.Util.formatBytes(data.MemTotal - data.MemFree, 2);
            }
        }, {
            name: 'usedMemoryPercent',
            calculate: function (data) {
                return ((1 - data.MemFree / data.MemTotal) * 100).toFixed(1);
            }
        }, {
            name: 'totalSwap',
            calculate: function (data) {
                return Ung.Util.formatBytes(data.SwapTotal, 2);
            }
        }, {
            name: 'freeSwap',
            calculate: function (data) {
                return Ung.Util.formatBytes(data.SwapFree, 2);
            }
        }, {
            name: 'freeSwapPercent',
            calculate: function (data) {
                // return (Math.random() * 100).toFixed(2);
                return (data.SwapFree / data.SwapTotal * 100).toFixed(1);
            }
        }, {
            name: 'usedSwap',
            calculate: function (data) {
                return Ung.Util.formatBytes(data.SwapTotal - data.SwapFree, 2);
            }
        }, {
            name: 'usedSwapPercent',
            calculate: function (data) {
                return ((1 - data.SwapFree / data.SwapTotal) * 100).toFixed(1);
            }
        }, {
            name: 'totalDisk',
            calculate: function (data) {
                return Ung.Util.formatBytes(data.totalDiskSpace, 2);
            }
        }, {
            name: 'freeDisk',
            calculate: function (data) {
                return Ung.Util.formatBytes(data.freeDiskSpace, 2);
            }
        }, {
            name: 'freeDiskPercent',
            calculate: function (data) {
                // return (Math.random() * 100).toFixed(2);
                return (data.freeDiskSpace / data.totalDiskSpace * 100).toFixed(1);
            }
        }, {
            name: 'usedDisk',
            calculate: function (data) {
                return Ung.Util.formatBytes(data.totalDiskSpace - data.freeDiskSpace, 2);
            }
        }, {
            name: 'usedDiskPercent',
            calculate: function (data) {
                return ((1 - data.freeDiskSpace / data.totalDiskSpace) * 100).toFixed(1);
            }
        }, {
            name: 'uptimeFormatted',
            calculate: function (data) {
                var numdays = Math.floor((data.uptime % 31536000) / 86400),
                    numhours = Math.floor(((data.uptime % 31536000) % 86400) / 3600),
                    numminutes = Math.floor((((data.uptime % 31536000) % 86400) % 3600) / 60),
                    uptime = '';

                if (numdays > 0) {
                    uptime += numdays + 'd ';
                }
                if (numhours > 0) {
                    uptime += numhours + 'h ';
                }
                if (numminutes > 0) {
                    uptime += numminutes + 'm';
                }
                return uptime;
            }
        }]
});
Ext.define('Ung.store.Stats', {
    extend: 'Ext.data.Store',
    alias: 'store.stats',
    storeId: 'stats',
    model: 'Ung.model.Stat'
});
Ext.define ('Ung.model.Report', {
    extend: 'Ext.data.Model' ,
    fields: [
        'category', 'colors', 'conditions', 'defaultColumns', 'description',
        'displayOrder', 'enabled',
        'javaClass',
        'orderByColumn',
        'orderDesc',

        'pieGroupColumn',
        'pieNumSlices',
        'pieStyle',
        'pieSumColumn',

        'readOnly',
        'seriesRenderer',
        'table',
        'textColumns',
        'textString',

        'timeDataColumns',
        'timeDataDynamicAggregationFunction',
        'timeDataDynamicAllowNull',
        'timeDataDynamicColumn',
        'timeDataDynamicLimit',
        'timeDataDynamicValue',
        'timeDataInterval',
        'timeStyle',
        'title',
        'type',
        'uniqueId',
        'units',


        {
            name: 'icon',
            calculate: function (entry) {
                var icon;
                switch (entry.type) {
                case 'TEXT':
                    icon = 'fa-align-left';
                    break;
                case 'EVENT_LIST':
                    icon = 'fa-list-ul';
                    break;
                case 'PIE_GRAPH':
                    icon = 'fa-pie-chart';
                    if (entry.pieStyle === 'COLUMN' || entry.pieStyle === 'COLUMN_3D') {
                        icon = 'fa-bar-chart';
                    } else {
                        if (entry.pieStyle === 'DONUT' || entry.pieStyle === 'DONUT_3D') {
                            icon = 'fa-pie-chart';
                        }
                    }
                    break;
                case 'TIME_GRAPH':
                case 'TIME_GRAPH_DYNAMIC':
                    icon = 'fa-line-chart';
                    if (entry.timeStyle.indexOf('BAR') >= 0) {
                        icon = 'fa-bar-chart';
                    } else {
                        if (entry.timeStyle.indexOf('AREA') >= 0) {
                            icon = 'fa-area-chart';
                        }
                    }
                    break;
                default:
                    icon = 'fa-align-left';
                }
                return icon;
            }
        }
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
            //rootProperty: 'list'
        }
    }
});

Ext.define('Ung.store.Reports', {
    extend: 'Ext.data.Store',
    storeId: 'reports',
    model: 'Ung.model.Report',
    sorters: [{
        property: 'displayOrder',
        direction: 'ASC'
    }]
});
Ext.define ('Ung.model.Widget', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'displayColumns', type: 'auto', default: null },
        { name: 'enabled', type: 'auto', default: true },
        { name: 'entryId', type: 'auto', default: null },
        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.uvm.DashboardWidgetSettings' },
        { name: 'refreshIntervalSec', type: 'auto', default: null },
        { name: 'timeframe', type: 'auto', default: null },
        { name: 'type', type: 'string' }
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json',
            rootProperty: 'list'
        }
    }
});
Ext.define('Ung.store.Widgets', {
    extend: 'Ext.data.Store',
    alias: 'store.widgets',
    storeId: 'widgets',
    model: 'Ung.model.Widget'
});
Ext.define ('Ung.model.Session', {
    extend: 'Ext.data.Model',
    fields: [
        { name: 'protocol', type: 'string' },
        { name: 'clientKBps', type: 'number', convert: function (val) { return val ? Math.round(val*1000)/1000 : null; } },
        { name: 'serverKBps', convert: function (val) { return val ? Math.round(val*1000)/1000 : null; } },
        { name: 'totalKBps', convert: function (val) { return val ? Math.round(val*1000)/1000 : null ; } },
        { name: 'clientIntf', convert: function (val) {
            if (!val || val < 0) {
                return '';
            }
            return val;
        } },
        { name: 'serverIntf', convert: function (val) {
            if (!val || val < 0) {
                return '';
            }
            return val;
        } }
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
            // rootProperty: 'list'
        }
    }
});
Ext.define('Ung.store.Sessions', {
    extend: 'Ext.data.Store',
    storeId: 'sessions',
    model: 'Ung.model.Session'
});
Ext.define ('Ung.model.Category', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'name', type: 'string' },
        { name: 'displayName', type: 'string' },
        { name: 'icon', type: 'string' }
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
Ext.define('Ung.store.Categories', {
    extend: 'Ext.data.Store',
    storeId: 'categories',
    model: 'Ung.model.Category'
});
Ext.define('Ung.controller.Global', {
    extend: 'Ext.app.Controller',
    namespace: 'Ung',

    requires: [
        'Ung.rpc.Rpc',
        'Ung.util.Util',
        'Ung.util.Metrics',
        'Ung.view.main.Main',
        'Ung.overrides.form.field.VTypes',
        'Ung.view.shd.Sessions',
        'Ung.view.shd.Hosts',
        'Ung.view.shd.Devices',
        'Ung.config.network.Network'
    ],


    stores: [
        'Policies',
        'Metrics',
        'Stats',
        'Reports',
        'Widgets',
        'Sessions',
        'Hosts',
        'Conditions',
        'Countries',
        'Categories',
        'UnavailableApps',
        'Rule'
    ],

    config: {
        control: {
            '#main': {
                beforerender: 'onBeforeRender'
            },
            '#apps': {
                activate: 'onActivate'
            }
        },

        refs: {
            mainView: '#main',
            dashboardView: '#dashboard',
            appsView: '#apps',
            reportsView: '#reports',
        },

        routes: {
            '': 'onDashboard',
            // 'apps': 'onApps',
            'apps/:policyId': 'onApps',
            'apps/:policyId/:node': 'onApps',
            'config': 'onConfig',
            'config/:configName': 'onConfig',
            'config/:configName/:configView': 'onConfig',
            'reports': 'onReports',
            'reports/:category': 'onReports',
            'reports/:category/:entry': 'onReports',
            'sessions': 'onSessions',
            'hosts': 'onHosts',
            'devices': 'onDevices'
        },

        reportsEnabled: true
    },

    onBeforeRender: function () {
        // console.log('init');
    },

    onActivate: function () {
        console.log('activate');
    },

    onDashboard: function () {
        this.getMainView().setActiveItem('dashboard');
        this.getMainView().getViewModel().set('selectedNavItem', 'dashboard');
        // this.getMainView().setActiveItem('#dashboard');
        // this.getViewModel().set('activeItem', 'dashboard');
    },

    onApps: function (policyId, node) {
        console.log(node);
        this.getMainView().getViewModel().set('selectedNavItem', 'apps');
        this.getMainView().setActiveItem('apps');

        if (node) {
            if (node === 'install') {
                console.log(this.getAppsView());
                this.getAppsView().setActiveItem('installableApps');
            } else {
                // vm.set('nodeName', node);
                // vm.set('activeItem', 'settings');
            }
        } else {
            this.getAppsView().setActiveItem('installedApps');
            // vm.set('activeItem', 'apps');
        }

        // console.log(this);
        // console.log(this.getAppsView());
    },

    onConfig: function (configName, configView) {
        var me = this;
        if (!configName) {
            this.getMainView().getViewModel().set('selectedNavItem', 'config');
            this.getMainView().setActiveItem('config');
        } else {
            me.getMainView().setLoading(true);
            Ext.Loader.loadScript({
                url: 'root/script/config/' + configName + '.js',
                onLoad: function () {
                    me.getMainView().setLoading(false);
                    me.getMainView().add({
                        xtype: 'config.' + configName,
                        region: 'center',
                        itemId: 'configCard'
                    });
                    me.getMainView().setActiveItem('configCard');

                    // if (configView) {
                    //     console.log('here');
                    //     me.getMainView().down('#configCard').setActiveItem(configView);
                    // }

                    // console.log('loaded');
                    // Ext.require('Ung.config.network.Network', function () {
                    //     console.log('require');
                    // });
                    // setTimeout(function() {
                    //     me.getMainView().add({
                    //         xtype: 'ung.config.network',
                    //         region: 'center',
                    //         itemId: 'configCard'
                    //     });
                    // }, 1000);
                }
            });
        }


        // if (!configName) {
        //     this.getMainView().getViewModel().set('selectedNavItem', 'config');
        //     this.getMainView().setActiveItem('config');
        // } else {
        //     this.getMainView().add({
        //         xtype: 'ung.config.network',
        //         region: 'center',
        //         itemId: 'configCard'
        //     });
        //     this.getMainView().setActiveItem('configCard');
        // }
        // this.getMainView().setActiveItem('#dashboard');
        // this.getViewModel().set('activeItem', 'dashboard');
    },

    onReports: function (category) {
        if (category) {
            this.getReportsView().getViewModel().set('category', category.replace(/-/g, ' '));
        } else {
            this.getReportsView().getViewModel().set('category', null);
        }
        this.getMainView().getViewModel().set('selectedNavItem', 'reports');
        this.getMainView().setActiveItem('reports');
        // console.log(this.getReportsView().getViewModel());
    },

    onSessions: function () {
        // var shd = this.getMainView().down('#shd');
        // if (shd) {
        //     // this.getMainView().remove('#shd', true);
        //     shd.destroy();
        // }
        this.getMainView().add({
            xtype: 'ung.sessions',
            itemId: 'sessions'
        });
        this.getMainView().getViewModel().set('selectedNavItem', 'sessions');
        this.getMainView().setActiveItem('sessions');
    },

    onHosts: function () {
        // var shd = this.getMainView().down('#shd');
        // if (shd) {
        //     // this.getMainView().remove('#shd', true);
        //     shd.destroy();
        // }
        this.getMainView().add({
            xtype: 'ung.hosts',
            itemId: 'hosts'
        });
        this.getMainView().getViewModel().set('selectedNavItem', 'hosts');
        this.getMainView().setActiveItem('hosts');
    },

    onDevices: function () {
        // var shd = this.getMainView().down('#shd');
        // if (shd) {
        //     // this.getMainView().remove('#shd', true);
        //     shd.destroy();
        // }
        this.getMainView().add({
            xtype: 'ung.devices',
            itemId: 'devices'
        });
        this.getMainView().getViewModel().set('selectedNavItem', 'devices');
        this.getMainView().setActiveItem('devices');
    }

});
// test
Ext.define('Ung.Application', {
    extend: 'Ext.app.Application',
    namespace: 'Ung',

    autoCreateViewport: false,
    name: 'Ung',

    rpc: null,

    controllers: ['Global'],

    defaultToken : '',

    mainView: 'Ung.view.main.Main',

    init: function () {
        console.timeEnd('resources');
        // Ext.get('app-loader').destroy();
    },

    launch: function () {
        var me = this;
        Rpc.rpc = me.rpc;

        Ext.getStore('policies').loadData(me.rpc.appsViews);

        Ung.util.Metrics.start();

        Ext.get('app-loader').destroy();

        Ext.Deferred.parallel([
            Rpc.getDashboardSettings,
            Rpc.getReports,
            Rpc.getUnavailableApps
        ]).then(function (result) {
            // Ext.get('app-loader').destroy();
            Ung.dashboardSettings = result[0];
            Ext.getStore('widgets').loadData(result[0].widgets.list);
            if (result[1]) {
                Ext.getStore('reports').loadData(result[1].list);
            }
            if (result[2]) {
                Ext.getStore('unavailableApps').loadRawData(result[2].map);
            }

            Ext.fireEvent('init');
            // me.loadMainView();
            //console.log(reports);
            //this.setWidgets();
        }, function (exception) {
            console.log(exception);
        });



        // Ext.get('app-message').setHtml('Reports ...');

        // need to check if reports enabled an load it if so
        // console.time('dash');
        // Rpc.loadDashboardSettings().then(function(settings) {
        //     console.timeEnd('dash');
        //     Ext.getStore('widgets').loadData(settings.widgets.list);

        //     if (me.rpc.nodeManager.node('untangle-node-reports')) {
        //         Rpc.loadReports().then(function (reports) {
        //             Ext.getStore('reports').loadData(reports.list);
        //             me.loadMainView();
        //         });
        //     } else {
        //         me.loadMainView();
        //     }
        //     // me.loadMainView();
        //     // me.getView().setSettings(settings);
        //     // if (vm.get('reportsInstalled')) {
        //     //     // load unavailable apps needed for showing the widgets
        //     //     console.time('unavailApps');
        //     //     rpc.reportsManager.getUnavailableApplicationsMap(function (result, ex) {
        //     //         if (ex) { Ung.Util.exceptionToast(ex); return false; }

        //     //         Ext.getStore('unavailableApps').loadRawData(result.map);
        //     //         Ext.getStore('widgets').loadData(settings.widgets.list);
        //     //         console.timeEnd('unavailApps');
        //     //         me.loadWidgets();
        //     //     });
        //     // } else {
        //     //     Ext.getStore('widgets').loadData(settings.widgets.list);
        //     //     me.loadWidgets();
        //     // }
        //     // me.populateMenus();
        // });

        // uncomment this to retreive the class load order inside browser
        // Ung.Util.getClassOrder();
    },

    loadMainView: function () {
        Ung.util.Metrics.start();
        try {
            Ung.app.setMainView('Ung.view.main.Main');
        } catch (ex) {
            console.error(ex);
            Ung.Util.exceptionToast(ex);
            return;
        }

        // start metrics
        // Ext.get('app-loader').destroy();

        // destroy app loader
        // Ext.get('app-loader').addCls('removing');
        // Ext.Function.defer(function () {
        //     Ext.get('app-loader').destroy();
        // }, 500);
    }
});
