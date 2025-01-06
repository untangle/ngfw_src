Ext.define('Ung.apps.dynamic-lists.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-dynamic-lists',
    controller: 'app-dynamic-lists',
    viewModel: {
        stores: {
        },
        data: {
        },
        formulas: {
        }
    },
    items: [
        { xtype: 'app-dynamic-lists-status' },
        { xtype: 'app-dynamic-lists-configuration' }

    ]
});