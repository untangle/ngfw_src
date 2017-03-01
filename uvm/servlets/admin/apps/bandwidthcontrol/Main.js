Ext.define('Ung.apps.bandwidthcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.bandwidthcontrol',

    viewModel: {
        data: {
            nodeName: 'untangle-node-bandwidth-control',
            appName: 'Bandwidth Control'
        }
    },

    items: [
        { xtype: 'app.bandwidthcontrol.status' },
        { xtype: 'app.bandwidthcontrol.rules' }
    ]

});
