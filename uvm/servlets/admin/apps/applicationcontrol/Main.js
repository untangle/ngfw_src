Ext.define('Ung.apps.applicationcontrol.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-applicationcontrol',
    controller: 'app-applicationcontrol',

    viewModel: {
        stores: {
            protoRules: { data: '{settings.protoRules.list}' },
            logicRules: { data: '{settings.logicRules.list}' }
        }
    },

    items: [
        { xtype: 'app-applicationcontrol-status' },
        { xtype: 'app-applicationcontrol-applications' },
        { xtype: 'app-applicationcontrol-rules' }
    ]

});
