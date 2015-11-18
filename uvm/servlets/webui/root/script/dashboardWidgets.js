Ext.define('Ung.dashboard.Widget', {
    extend: 'Ext.panel.Panel',
    closable: true,
    tools: [{
        type:'refresh',
        callback: function() {
            this.refresh();
        },
        scope: this
    }],
    padding: '5 0 0 5',
    bodyPadding: 5,
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
    }
});

Ext.define('Ung.dashboard.Information', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Information"),
    width: 300,
    defaults: {
        xtype: 'displayfield',
        labelWidth: 150
            
    },
    items: [{
        name: 'model',
        fieldLabel: i18n._("Model")
    }, {
        name: 'uptime',
        fieldLabel: i18n._("Uptime")
    }, {
        name: 'version',
        fieldLabel: i18n._("Version")
    }, {
        name: 'subscriptions',
        fieldLabel: i18n._("Subscriptions")
    }],
    refresh: function () {
        
    }
});

Ext.define('Ung.dashboard.Server', {
    extend: 'Ung.dashboard.Widget',
    title: i18n._("Server"),
    width: 300,
    defaults: {
        xtype: 'displayfield',
        labelWidth: 150
            
    },
    items: [{
        name: 'cpuCount',
        fieldLabel: i18n._("CPU count")
    }, {
        name: 'cpuType',
        fieldLabel: i18n._("CPU type")
    }, {
        name: 'architecture',
        fieldLabel: i18n._("Architecture")
    }, {
        name: 'memory',
        fieldLabel: i18n._("Memory")
    }, {
        name: 'disk',
        fieldLabel: i18n._("Disk")
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
    width: 300,
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
    width: 300,
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