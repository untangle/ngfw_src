Ext.define('Ung.config.local-directory.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-local-directory',

    /* requires-start */
    requires: [
        'Ung.config.local-directory.MainController',
        'Ung.config.local-directory.MainModel'
    ],
    /* requires-end */
    controller: 'config-local-directory',

    viewModel: {
        type: 'config-local-directory'
    },

    items: [
        { xtype: 'config-local-directory-users' },
        { xtype: 'config-local-directory-radius' }
    ]
});
