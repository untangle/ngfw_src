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
        {
            xtype: 'config-local-directory-radius-server',
            tabConfig: {
                bind: {
                    hidden: '{!expertMode}'
                }
            }
        },
        {
            xtype: 'config-local-directory-radius-proxy',
            tabConfig: {
                bind: {
                    hidden: '{!expertMode}'
                }
            }
        },
        {
            xtype: 'config-local-directory-radius-log',
            tabConfig: {
                bind: {
                    hidden: '{!expertMode}'
                }
            }
        }
    ]
});
