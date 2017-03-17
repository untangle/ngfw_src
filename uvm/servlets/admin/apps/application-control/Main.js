Ext.define('Ung.apps.applicationcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-application-control',
    controller: 'app-application-control',

    viewModel: {
        stores: {
            protoRules: { data: '{settings.protoRules.list}' },
            logicRules: { data: '{settings.logicRules.list}' }
        }
    },

    items: [
        { xtype: 'app-application-control-status' },
        { xtype: 'app-application-control-applications' },
        { xtype: 'app-application-control-rules' }
    ]

});
