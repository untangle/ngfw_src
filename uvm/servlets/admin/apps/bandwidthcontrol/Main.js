Ext.define('Ung.apps.bandwidthcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-bandwidthcontrol',
    controller: 'app-bandwidthcontrol',

    items: [
        { xtype: 'app-bandwidthcontrol-status' },
        { xtype: 'app-bandwidthcontrol-rules' }
    ]

});
