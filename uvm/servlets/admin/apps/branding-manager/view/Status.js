Ext.define('Ung.apps.brandingmanager.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-branding-manager-status',
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
            html: '<img src="/skins/modern-rack/images/admin/apps/branding-manager_80x80.png" width="80" height="80"/>' +
                '<h3>Branding Manager</h3>' +
                '<p>' + 'The Branding Settings are used to set the logo and contact information that will be seen by users (e.g. reports).'.t() + '</p>'
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
