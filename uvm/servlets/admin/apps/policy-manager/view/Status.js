Ext.define('Ung.apps.policymanager.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-policy-manager-status',
    itemId: 'status',
    title: 'Status'.t(),

    viewModel: true,

    items: [{
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/skins/modern-rack/images/admin/apps/untangle-node-policy-manager_80x80.png" width="80" height="80"/>' +
                '<h3>Policy Manager</h3>' +
                '<p>' + 'Policy Manager enables administrators to create different policies and handle different sessions with different policies based on rules.'.t() + '</p>'
        }, {
            xtype: 'appreports',
        }]
    }]
});
