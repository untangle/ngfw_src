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
        //console.log(stats);
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
    height: 160,
    hasStats: true,
    defaults: {
        xtype: 'displayfield',
        labelWidth: 100
    },
    items: [{
        fieldLabel: i18n._("Model"),
        statsProperty: 'cpuModel'
    }, {
        fieldLabel: i18n._("Uptime"),
        statsProperty: 'uptime'
    }, {
        name: 'version',
        fieldLabel: i18n._("Version")
    }, {
        name: 'subscriptions',
        fieldLabel: i18n._("Subscriptions")
    }],
    afterRender: function() {
        this.callParent(arguments);
        this.down('displayfield[name=version]').setValue(rpc.fullVersion);
    }
});

Ext.define('Ung.dashboard.Server', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Server"),
    hasStats: true,
    defaults: {
        xtype: 'displayfield',
        labelWidth: 150
    },
    layout: {
       type: 'vbox'
    },
    items: [
        {
            xtype: 'panel',
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            items: [
                { html: i18n._("Memory"), width: 80},
                {
                    xtype: 'progress',
                    name: 'memory',
                    width: 200
                },
                { xtype: 'displayfield', name: 'memory_val', flex: 1}
            ],
            updateStats: function(stats) {
                this.down('progress[name=memory]').setValue(1 - parseFloat(stats.MemFree/stats.MemTotal).toFixed(3));
                this.down('displayfield[name=memory_val]').setValue(parseFloat(stats.MemFree/(1024*1024)).toFixed(2) + 'Mb / ' + parseFloat(stats.MemTotal/(1024*1024)).toFixed(2) + 'Mb');
            }
        },
        {
            xtype: 'panel',
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            items: [
                { html: i18n._("Disk"), width: 80},
                {
                    xtype: 'progress',
                    name: 'disk',
                    width: 200
                },
                { xtype: 'displayfield', name: 'disk_val', flex: 1}
            ],
            updateStats: function(stats) {
                this.down('progress[name=disk]').setValue(1 - parseFloat(stats.freeDiskSpace/stats.totalDiskSpace).toFixed(3));
                this.down('displayfield[name=disk_val]').setValue(parseFloat(stats.freeDiskSpace/(1024*1024)).toFixed(2) + 'Mb / ' + parseFloat(stats.totalDiskSpace/(1024*1024)).toFixed(2) + 'Mb');
            }
        },
        {
            name: 'cpuCount',
            fieldLabel: i18n._("CPU count")
        }, {
            name: 'cpuType',
            fieldLabel: i18n._("CPU type")
        }, {
            name: 'architecture',
            fieldLabel: i18n._("Architecture")
        }, {
            name: 'ethernetNIC',
            fieldLabel: i18n._("Ethernet NIC")
        }, {
            name: 'wirelessNIC',
            fieldLabel: i18n._("Wireless NIC")
        }],
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
            cls: 'widget small-widget'
        }),
        Ext.create('Ung.dashboard.Devices', {
            type: 'Devices',
            cls: 'widget small-widget'
        }),
        Ext.create('Ung.dashboard.Server', {
            type: 'Devices',
            cls: 'widget small-widget'
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

Ext.define('Ung.dashboard.SingleWidget', {
    extend: 'Ung.dashboard.Widget',
    header: false,
    items: [
        Ext.create('Ung.dashboard.Information', {
            type: 'Information',
            cls: 'widget small-widget'
        })
    ]
});