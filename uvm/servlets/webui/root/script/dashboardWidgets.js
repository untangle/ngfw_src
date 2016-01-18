Ext.define('Ung.Main.dashboard', {
    constructor: function(config) {
        var widget;
        this.widgets = [];
        for(var i=0; i < config.widgets.length; i++) {
            widget = config.widgets[i];
            this.widgets.push(Ext.create('Ung.dashboard.' + widget.type, widget));
        }
    },
    updateFromStats: function(stats) {
        console.log(stats);
        for(var i=0; i < this.widgets.length; i++) {
            var widget = this.widgets[i];
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
    //closable: true,
    tools: [{
        type:'refresh',
        callback: function() {
            this.refresh();
        },
        scope: this
    }],
    //padding: '5 0 0 5',
    //bodyPadding: 5,
    initComponent: function() {
        this.callParent(arguments);
    },
    listeners: {
        beforeclose: {
            fn: function(panel, eOpts) {
                if(panel.removeConfirmed) {
                    return true;
                }
                Ext.MessageBox.confirm(i18n._("Remove widget"),
                        i18n._("Do you want to remove this widget from dashboard?"),
                        function(btn) {
                            if(btn == 'yes') {
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
    updateFromStats: function(stats) {
        this.items.each(function(item) {
            if(item.statsProperty) {
                item.setValue(stats[item.statsProperty]);
            }
            // check if item has updateStatus function, used for parsing data
            if(Ext.isFunction(item.updateStats)) {
                item.updateStats(stats);
            }
        });

    }
});

Ext.define('Ung.dashboard.Information', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Information"),
    hasStats: true,
    defaults: {
        xtype: 'displayfield',
        labelWidth: 100
    },
    items: [{
        name: 'hostname',
        fieldLabel: i18n._("Hostname")
    },{
        fieldLabel: i18n._("Model"),
        value: 'unknown'
    },
        /*{
        fieldLabel: i18n._("Model"),
        statsProperty: 'cpuModel'
    },*/ {
        name: 'uptime',
        fieldLabel: i18n._("Uptime"),
        updateStats: function(stats) {
            // display humab readable uptime
            var numdays = Math.floor((stats.uptime % 31536000) / 86400);
            var numhours = Math.floor(((stats.uptime % 31536000) % 86400) / 3600);
            var numminutes = Math.floor((((stats.uptime % 31536000) % 86400) % 3600) / 60);

            var output = '';

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
    afterRender: function() {
        this.callParent(arguments);
        this.down('displayfield[name=hostname]').setValue(rpc.hostname);
        this.down('displayfield[name=version]').setValue(rpc.fullVersion);
    }
});

Ext.define('Ung.dashboard.Server', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Server"),
    hasStats: true,
    layout: {
        type: 'vbox',
    },
    items: [
        {
            xtype: 'panel',
            layout: 'hbox',
            cls: 'nopadding',
            items: [
                {
                    xtype: 'displayfield',
                    fieldLabel: i18n._("CPU"),
                    width: 60
                },
                {
                    xtype: 'panel',
                    layout: 'vbox',
                    items: [
                        {
                            xtype: 'displayfield',
                            name: 'cpu',
                            value: 'LOW'
                        },
                        {
                            xtype: 'displayfield',
                            name: 'cpuload'
                        }
                    ]
                }
            ],
            updateStats: function(stats) {
                this.down('displayfield[name=cpuload]').setValue('<strong>' + stats.oneMinuteLoadAvg + '</strong> 1-min load');
            }
        },
        {
            xtype: 'panel',
            layout: 'hbox',
            cls: 'nopadding',
            items: [
                {
                    xtype: 'displayfield',
                    fieldLabel: i18n._("Disk"),
                    width: 60
                },
                {
                    xtype: 'panel',
                    layout: 'vbox',
                    items: [
                        {
                            xtype: 'progress',
                            name: 'disk',
                            width: 200,
                            style: {
                                marginBottom: '7px'
                            }
                        },
                        {
                            xtype: 'displayfield',
                            name: 'usedDisk',
                            style: {
                                minHeight: '18px'
                            }
                        },
                        {
                            xtype: 'displayfield',
                            name: 'freeDisk',
                            style: {
                                minHeight: '18px'
                            }
                        }
                    ]
                }
            ],
            updateStats: function(stats) {
                this.down('progress[name=disk]').setValue(1 - parseFloat(stats.freeDiskSpace/stats.totalDiskSpace).toFixed(3));

                var usedDisk = parseInt((stats.totalDiskSpace-stats.freeDiskSpace)/8/1048576,10);
                var usedPercent = parseFloat((1 - parseFloat(stats.freeDiskSpace/stats.totalDiskSpace)) * 100).toFixed(1);

                var freeDisk = parseInt(stats.freeDiskSpace/8/1048576,10);
                //console.log(parseFloat(stats.freeDiskSpace/stats.totalDiskSpace));
                this.down('displayfield[name=usedDisk]').setValue('<strong>' + usedDisk + ' MB</strong> used <em>(' + usedPercent + '%)</em>');
                this.down('displayfield[name=freeDisk]').setValue('<strong>' + freeDisk + ' MB</strong> free <em>(' + (100 - usedPercent) + '%)</em>');
            }
        }
    ],
    refresh: function () {

    }
});

Ext.define('Ung.dashboard.Sessions', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Sessions"),
    defaults: {
        xtype: 'displayfield',
        labelWidth: 150
            
    },
    items: [{
        name: 'totalSessions',
        fieldLabel: i18n._("Total Sessions")
    }, {
        name: 'scannedSessions',
        fieldLabel: i18n._("Scanned Sessions")
    }, {
        name: 'scannedTCPSessions',
        fieldLabel: i18n._("Scanned TCP Sessions")
    }, {
        name: 'scannedUDPSessions',
        fieldLabel: i18n._("Scanned UDP Sessions")
    }, {
        name: 'bypassedSessions',
        fieldLabel: i18n._("Bypasswd Sessions")
    }],
    refresh: function () {
        
    }
});

Ext.define('Ung.dashboard.Devices', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Devices"),
    height: 160,
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
    }],
    refresh: function () {
        
    }
});

Ext.define('Ung.dashboard.Hardware', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Hardware"),
    hasStats: true,
    height: 160,
    defaults: {
        xtype: 'displayfield',
        labelWidth: 100

    },
    items: [{
        fieldLabel: i18n._("CPU Count"),
        statsProperty: 'numCpus'
    }, {
        fieldLabel: i18n._("CPU Type"),
        statsProperty: 'cpuModel'
    },{
        fieldLabel: i18n._("Architecture"),
        statsProperty: 'architecture'
    },{
        fieldLabel: i18n._("Memory"),
        updateStats: function(stats) {
            this.setValue(Math.round(stats.MemTotal/8/1048576) + ' MB');
        }
    },{
        fieldLabel: i18n._("Disk"),
        updateStats: function(stats) {
            this.setValue(Math.round(stats.totalDiskSpace/8/1048576) + ' MB');
        }
    }],
    refresh: function () {

    }
});

Ext.define('Ung.dashboard.Memory', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Memory Resources"),
    hasStats: true,
    layout: {
        type: 'vbox'
    },
    items: [
        {
            xtype: 'panel',
            layout: 'hbox',
            cls: 'nopadding',
            items: [
                {
                    xtype: 'displayfield',
                    fieldLabel: i18n._("Memory"),
                    width: 60
                },
                {
                    xtype: 'panel',
                    layout: 'vbox',
                    items: [
                        {
                            xtype: 'progress',
                            name: 'memory',
                            width: 200,
                            style: {
                                marginBottom: '7px'
                            }
                        },
                        {
                            xtype: 'displayfield',
                            name: 'usedMemory',
                            style: {
                                minHeight: '18px'
                            }
                        },
                        {
                            xtype: 'displayfield',
                            name: 'freeMemory',
                            style: {
                                minHeight: '18px'
                            }
                        }
                    ]
                }
            ],
            updateStats: function(stats) {
                this.down('progress[name=memory]').setValue(1 - parseFloat(stats.MemFree/stats.MemTotal).toFixed(3));

                var usedMemory = parseInt((stats.MemTotal-stats.MemFree)/8/1048576,10);
                var usedMemoryPercent = parseFloat((1 - parseFloat(stats.MemFree/stats.MemTotal)) * 100).toFixed(1);

                var freeMemory = parseInt(stats.MemFree/8/1048576,10);

                this.down('displayfield[name=usedMemory]').setValue('<strong>' + usedMemory + ' MB</strong> used <em>(' + usedMemoryPercent + '%)</em>');
                this.down('displayfield[name=freeMemory]').setValue('<strong>' + freeMemory + ' MB</strong> free <em>(' + (100 - usedMemoryPercent) + '%)</em>');
            }
        },
        {
            xtype: 'container',
            layout: 'hbox',
            cls: 'nopadding',
            items: [
                {
                    xtype: 'component',
                    html: i18n._("Swap"),
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
                            width: 200,
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
            updateStats: function(stats) {
                this.down('progress[name=swap]').setValue(1 - parseFloat(stats.SwapFree/stats.SwapTotal).toFixed(3));

                var usedSwap = parseInt((stats.SwapTotal-stats.SwapFree)/8/1048576,10);
                var usedSwapPercent = parseFloat((1 - parseFloat(stats.SwapFree/stats.SwapTotal)) * 100).toFixed(1);

                var freeSwap = parseInt(stats.SwapFree/8/1048576,10);


                this.down('component[name=usedSwap]').update('<strong>' + usedSwap + ' MB</strong> used <em>(' + usedSwapPercent + '%)</em>');
                this.down('component[name=freeSwap]').update('<strong>' + freeSwap + ' MB</strong> free <em>(' + (100 - usedSwapPercent) + '%)</em>');
            }
        }
    ],
    refresh: function () {

    }
});


Ext.define('Ung.dashboard.EventEntry', {
    extend: 'Ung.dashboard.Widget',
    width: 500,
    initComponent: function(conf) {
        this.items= {
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
    },
    refresh: function () {
        
    }
});

Ext.define('Ung.dashboard.GroupWidget', {
    extend: 'Ung.dashboard.Widget',
    header: false,
    items: [
        Ext.create('Ung.dashboard.Information', {
            type: 'Information',
            cls: 'widget small-widget',
            height: 180
        }),
        Ext.create('Ung.dashboard.Server', {
            type: 'Server',
            cls: 'widget small-widget',
            height: 180
        }),
        Ext.create('Ung.dashboard.Hardware', {
            type: 'Hardware',
            cls: 'widget small-widget',
            height: 212
        }),
        Ext.create('Ung.dashboard.Memory', {
            type: 'Memory',
            cls: 'widget small-widget',
            height: 212
        })
    ],
    initComponent: function() {
        var me = this;
        this.callParent(arguments);

        this.items.each(function(item) {
            if (item.hasStats) {
                me.hasStats = true;
                return false;
            }
        });
    },
    updateFromStats: function(stats) {
        this.items.each(function(item) {
            if (item.hasStats) {
                item.updateFromStats(stats);
            }
        });
    }
});