Ext.syncRequire('Ung.common.threatprevention');
Ext.define('Ung.apps.threatprevention.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-threat-prevention',
    controller: 'app-threat-prevention',

    viewModel: {
        type: 'app-threat-prevention'
    },

    items: [
        { xtype: 'app-threat-prevention-status' },
        { xtype: 'app-threat-prevention-threats' },
        { xtype: 'app-threat-prevention-rules' }
    ]
});
