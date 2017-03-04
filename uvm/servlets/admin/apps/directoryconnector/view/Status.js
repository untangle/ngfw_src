Ext.define('Ung.apps.directoryconnector.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-directoryconnector-status',
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
            html: '<img src="/skins/modern-rack/images/admin/apps/untangle-node-directory-connector_80x80.png" width="80" height="80"/>' +
                '<h3>Directory Connector</h3>' +
                '<p>' + 'Directory Connector allows integration with external directories and services, such as Active Directory, RADIUS, or Google.'.t() + '</p>'
        }, {
            xtype: 'appreports'
        }, {
            xtype: 'appremove'
        }]
    }]
});
