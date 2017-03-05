Ext.define('Ung.apps.bandwidthcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-bandwidthcontrol',
    controller: 'app-bandwidthcontrol',

    viewModel: {
        stores: {
            rules: { data: '{settings.rules.list}' }
        }
    },

    items: [
        { xtype: 'app-bandwidthcontrol-status' },
        { xtype: 'app-bandwidthcontrol-rules' }
    ]

});
