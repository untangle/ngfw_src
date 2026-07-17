Ext.define('Ung.apps.sslinspector.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ssl-inspector',
    controller: 'app-ssl-inspector',

    viewModel: {

        data: {
            autoRefresh: false,
            trustedCertData: [],
            isAddAction: false
        },

        stores: {
            ignoreRules: { data: '{settings.ignoreRules.list}' },
            trustedCertList: { data: '{trustedCertData}' },
            hostnameBypassList: { data: '{settings.hostnameVerificationBypassList.list}' }
        }
    },

    items: [
        { xtype: 'app-ssl-inspector-status' },
        { xtype: 'app-ssl-inspector-configuration' },
        { xtype: 'app-ssl-inspector-rules' }
    ]

});
