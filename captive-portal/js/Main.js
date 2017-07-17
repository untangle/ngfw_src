Ext.define('Ung.apps.captive-portal.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-captive-portal',
    controller: 'app-captive-portal',

    viewModel: {
        stores: {
            captureRules: { data: '{settings.captureRules.list}' },
            passedClients: { data: '{settings.passedClients.list}' },
            passedServers: { data: '{settings.passedServers.list}' }
        }
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/captive-portal',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            }
        }]
    },

    items: [
        { xtype: 'app-captive-portal-status' },
        { xtype: 'app-captive-portal-capturerules' },
        { xtype: 'app-captive-portal-passedhosts' },
        { xtype: 'app-captive-portal-captivepage' },
        { xtype: 'app-captive-portal-userauthentication' }
    ]

});
