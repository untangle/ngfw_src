Ext.define('Ung.apps.firewall.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-firewall',

    items: [
        { xtype: 'app-firewall-status' },
        { xtype: 'app-firewall-rules' }
    ]

});
