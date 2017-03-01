Ext.define('Ung.apps.webfilter.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app.webfilter',

    viewModel: {
        data: {
            nodeName: 'untangle-node-web-filter',
            appName: 'Web Filter'
        }
    },

    items: [
        { xtype: 'app.webfilter.status' },
        { xtype: 'app.webfilter.categories' },
        { xtype: 'app.webfilter.blocksites' },
        { xtype: 'app.webfilter.passsites' },
        { xtype: 'app.webfilter.passclients' },
        { xtype: 'app.webfilter.rules' },
        { xtype: 'app.webfilter.advanced' }
    ]

});
