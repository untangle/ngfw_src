Ext.define('Ung.config.administration.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-administration',
    name: 'administration',
    /* requires-start */
    requires: [
        'Ung.config.administration.MainController',
        'Ung.config.administration.MainModel',

        'Ung.store.Rule',
        'Ung.model.Rule',
        'Ung.cmp.Grid'
    ],
    /* requires-end */

    controller: 'config-administration',
    viewModel: {
        type: 'config-administration'
    },

    items: [
        { xtype: 'config-administration-admin' },
        { xtype: 'config-administration-certificates' },
        { xtype: 'config-administration-snmp' },
        { xtype: 'config-administration-skins' }
    ]
});
