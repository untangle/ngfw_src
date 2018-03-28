Ext.define('Ung.apps.firewall.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-firewall',
    controller: 'app-firewall',

    viewModel: {
        stores: {
            rules: { data: '{settings.rules.list}' },
        }
    },

    items: [
        { xtype: 'app-firewall-status' },
        { xtype: 'app-firewall-rules' }
    ]

});
