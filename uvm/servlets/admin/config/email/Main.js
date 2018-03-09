Ext.define('Ung.config.email.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-email',
    name: 'email',
    /* requires-start */
    requires: [
        'Ung.config.email.MainController',
        'Ung.config.email.EmailTest',

        'Ung.store.Rule',
        'Ung.model.Rule',
        'Ung.cmp.Grid'
    ],
    /* requires-end */

    controller: 'config.email',

    viewModel: {
        data: {
            title: 'Email'.t(),
            iconName: 'email',

            globalSafeList: null,
        }
    },

    items: [
        { xtype: 'config-email-outgoingserver' },
        {
            xtype: 'config-email-safelist',
            tabConfig: {
                hidden: true,
                bind: { hidden: '{!smtp}' }
            }
        },
        {
            xtype: 'config-email-quarantine',
            tabConfig: {
                hidden: true,
                bind: { hidden: '{!smtp}' }
            }
        }
    ]
});
