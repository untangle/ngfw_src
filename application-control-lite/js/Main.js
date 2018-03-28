Ext.define('Ung.apps.applicationcontrollite.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-application-control-lite',
    controller: 'app-application-control-lite',

    viewModel: {

        stores: {
            signatureList: {
                data: '{settings.patterns.list}'
            }
        }
    },

    items: [
        { xtype: 'app-application-control-lite-status' },
        { xtype: 'app-application-control-lite-signatures' }
    ]

});
