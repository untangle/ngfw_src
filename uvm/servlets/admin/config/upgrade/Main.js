Ext.define('Ung.config.upgrade.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-upgrade',
    scrollable: true,

    /* requires-start */
    requires: [
        'Ung.config.upgrade.MainController',
        'Ung.overrides.form.CheckboxGroup'
    ],
    /* requires-end */
    controller: 'config-upgrade',

    viewModel: {
        data: {
            title: 'Upgrade'.t(),
            iconName: 'upgrade'
        },
    },

    items: [
        { xtype: 'config-upgrade-upgrade' },
    ]
});
