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
            iconName: 'icon_config_upgrade'
        },
        formulas: {
            upgradeTime: {
                get: function (get) {
                    var hour = get('settings.autoUpgradeHour'),
                        minute = get('settings.autoUpgradeMinute') ? get('settings.autoUpgradeMinute') : '00';
                    return hour + ':' + minute;
                }
            }
        }
    },

    items: [
        { xtype: 'config-upgrade-upgrade' },
    ]
});
