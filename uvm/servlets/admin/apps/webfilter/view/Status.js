Ext.define('Ung.apps.webfilter.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app.webfilter.status',
    title: 'Status'.t(),

    viewModel: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/skins/modern-rack/images/admin/apps/untangle-node-web-filter_80x80.png" width="80" height="80"/><br/>' +
                'Web Filter scans and categorizes web traffic to monitor and enforce network usage policies.'.t()
        }, {
            xtype: 'fieldset',
            title: 'Power'.t(),
            border: false,
            padding: 0,
            margin: 0,
            items: [{
                xtype: 'button',
                iconCls: 'fa fa-toggle-on fa-2x fa-green'
            }]
        }]
    }, {
        region: 'east',
        border: false,
        width: 300,
        split: true,
        layout: 'fit',
        // layout: {
        //     type: 'hbox'
        // },
        items: [{
            xtype: 'appmetrics',
            disabled: true,
            sourceConfig: {
                // attachments:       { displayName: 'Attachments'.t() }
            },
        }]
    }]
});
