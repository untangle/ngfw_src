Ext.define('Ung.apps.bandwidthcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-bandwidth-control',
    controller: 'app-bandwidth-control',

    viewModel: {
        stores: {
            rules: { data: '{settings.rules.list}' }
        }
    },

    items: [
        { xtype: 'app-bandwidth-control-status' },
        { xtype: 'app-bandwidth-control-rules' }
    ]

});
