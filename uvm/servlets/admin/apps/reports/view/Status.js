Ext.define('Ung.apps.reports.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-reports-status',
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
            html: '<img src="/skins/modern-rack/images/admin/apps/reports_80x80.png" width="80" height="80"/>' +
                '<h3>Reports</h3>' +
                '<p>' + 'Reports records network events to provide administrators the visibility and data necessary to investigate network activity.'.t() + '</p>'
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'appremove'
        }]
    }]
});
