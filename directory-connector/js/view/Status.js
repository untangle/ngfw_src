Ext.define('Ung.apps.directoryconnector.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-directory-connector-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    viewModel: true,

    items: [{
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/directory-connector.svg" width="80" height="80"/>' +
                '<h3>Directory Connector</h3>' +
                '<p>' + 'Directory Connector allows integration with external directories and services, such as Active Directory, RADIUS, or Google.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appreports'
        }, {
            xtype: 'appremove'
        }]
    }]
});
