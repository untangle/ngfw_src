Ext.define('Ung.apps.dynamic-blocklists.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-dynamic-blocklists',
    viewModel: {
        stores: {
        },
        data: {
        },
        formulas: {
        }
    },
    items: [
        { xtype: 'app-dynamic-blocklists-configuration' }
    ]
});