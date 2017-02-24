Ext.define('Ung.view.shd.Devices', {
    extend: 'Ext.panel.Panel',
    // extend: 'Ext.grid.Panel',
    xtype: 'ung.devices',
    // layout: 'border',
    // controller: 'sessions',

    viewModel: {
        data: {
            autoRefresh: false
        }
    },

    layout: 'border',

    defaults: {
        border: false
    },

    title: 'Devices'.t(),

    items: [{
        region: 'center',
        html: 'devices'
    }]
});
