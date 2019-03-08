Ext.define('Ung.config.system.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-system',

    /* requires-start */
    requires: [
        'Ung.config.system.MainController',
        'Ung.config.system.MainModel',
    ],
    /* requires-end */
    controller: 'config-system',

    viewModel: {
        type: 'config-system',
        localizationChanged: false
    },

    items: [
        { xtype: 'config-system-regional' },
        { xtype: 'config-system-support' },
        { xtype: 'config-system-backup' },
        { xtype: 'config-system-restore' },
        { xtype: 'config-system-protocols' },
        { xtype: 'config-system-shield' }
    ]

});
