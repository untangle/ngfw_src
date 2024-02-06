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
                return Rpc.directData('rpc.hostname');
            }
        }, {
            name: 'version',
            calculate: function () {
                return Rpc.directData('rpc.fullVersion');
            }
        }, {
            name: 'appliance',
            calculate: function() {
                var applianceModel = Rpc.directData('rpc.applianceModel');
                return (applianceModel === undefined || applianceModel === null || applianceModel === '' ? 'custom'.t() : applianceModel);
            }
        }, {
            name: 'totalMemory',
            calculate: function (data) {
                return Util.formatBytes(data.MemTotal, 2);
            }
        }, {
            name: 'freeMemory',
            calculate: function (data) {
                return Util.formatBytes(data.MemFree, 2);
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
                return Util.formatBytes(data.MemTotal - data.MemFree, 2);
            }
        }, {
            name: 'usedMemoryPercent',
            calculate: function (data) {
                return ((1 - data.MemFree / data.MemTotal) * 100).toFixed(1);
            }
        }, {
            name: 'totalSwap',
            calculate: function (data) {
                if (data.SwapTotal) {
                    return Util.formatBytes(data.SwapTotal, 2);
                }
                return 0;
            }
        }, {
            name: 'freeSwap',
            calculate: function (data) {
                if (data.SwapTotal) {
                    return Util.formatBytes(data.SwapFree, 2);
                }
                return 0;
            }
        }, {
            name: 'freeSwapPercent',
            calculate: function (data) {
                if (data.SwapTotal) {
                    return (data.SwapFree / data.SwapTotal * 100).toFixed(1);
                }
                return 0;
            }
        }, {
            name: 'usedSwap',
            calculate: function (data) {
                if (data.SwapTotal) {
                    return Util.formatBytes(data.SwapTotal - data.SwapFree, 2);
                }
                return 0;
            }
        }, {
            name: 'usedSwapPercent',
            calculate: function (data) {
                if (data.SwapTotal) {
                    return ((1 - data.SwapFree / data.SwapTotal) * 100).toFixed(1);
                }
                return 0;
            }
        }, {
            name: 'totalDisk',
            calculate: function (data) {
                return Util.formatBytes(data.totalDiskSpace, 2);
            }
        }, {
            name: 'freeDisk',
            calculate: function (data) {
                return Util.formatBytes(data.freeDiskSpace, 2);
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
                return Util.formatBytes(data.totalDiskSpace - data.freeDiskSpace, 2);
            }
        }, {
            name: 'usedDiskPercent',
            calculate: function (data) {
                return ((1 - data.freeDiskSpace / data.totalDiskSpace) * 100).toFixed(1);
            }
        }, {
            name: 'uptimeFormatted',
            calculate: function (data) {
                var numyears = Math.floor(data.uptime / 31536000),
                    numdays = Math.floor((data.uptime % 31536000) / 86400),
                    numhours = Math.floor(((data.uptime % 31536000) % 86400) / 3600),
                    numminutes = Math.floor((((data.uptime % 31536000) % 86400) % 3600) / 60),
                    uptime = '';

                if (numyears > 0) {
                    uptime += numyears + 'y ';
                }
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
