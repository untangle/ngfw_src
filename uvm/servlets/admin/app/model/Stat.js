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
                return Util.formatBytes(data.SwapTotal, 2);
            }
        }, {
            name: 'freeSwap',
            calculate: function (data) {
                return Util.formatBytes(data.SwapFree, 2);
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
                return Util.formatBytes(data.SwapTotal - data.SwapFree, 2);
            }
        }, {
            name: 'usedSwapPercent',
            calculate: function (data) {
                return ((1 - data.SwapFree / data.SwapTotal) * 100).toFixed(1);
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
