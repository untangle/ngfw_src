Ext.define('Ung.apps.tunnel-vpn.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-tunnel-vpn-status',
    itemId: 'status',
    title: 'Status'.t(),

    requires: [
        'Ung.cmp.LicenseLoader'
    ],

    items: [{
        title: 'Status'.t(),
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/skins/modern-rack/images/admin/apps/tunnel-vpn_80x80.png" width="80" height="80"/>' +
                '<h3>Tunnel VPN</h3>' +
                '<p>' + 'Tunnel VPN Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appremove'
        }]
    }]

});
