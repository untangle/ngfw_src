Ext.define('Ung.apps.intrusionprevention.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.intrusionprevention',

    viewModel: {
        data: {
            nodeName: 'untangle-node-intrusion-prevention',
            appName: 'Intrusion Prevention'
        }
    },

    items: [
        { xtype: 'app.intrusionprevention.status' },
        { xtype: 'app.intrusionprevention.rules' },
        { xtype: 'app.intrusionprevention.variables' }
    ]

});
