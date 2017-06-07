Ext.define('Ung.config.upgrade.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-upgrade',

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
            iconName: 'icon_config_upgrade',

            settings: rpc.systemManager.getSettings(),
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

    items: [{
        title: 'Upgrade Settings'.t(),

        bodyPadding: 10,

        tbar: [{
            xtype: 'progressbar',
            width: '100%',
            border: false,
            text: 'Checking for upgrades...'.t()
        }],

        defaults: {
            xtype: 'fieldset',
            padding: 10
        },

        items: [{
            xtype: "component",
            name: 'upgradeText',
            padding: 5,
            margin: 20,
            html: 'No upgrades available.'.t(),
            hidden: true
        }, {
            xtype: "button",
            name: 'upgradeButton',
            padding: 5,
            margin: 20,
            hidden: true,
            text: "Upgrade Now".t(),
            iconCls: 'fa fa-play',
            handler: 'downloadUpgrades'
        }, {
            title: 'Automatic Upgrade'.t(),
            items: [{
                xtype: 'radiogroup',
                columns: 1,
                vertical: true,
                simpleValue: true,
                bind: '{settings.autoUpgrade}',
                items: [{
                    boxLabel: '<strong>' + 'Automatically Install Upgrades'.t() + '</strong>',
                    inputValue: true
                }, {
                    xtype: 'component',
                    margin: '0 0 10 20',
                    html: 'If new upgrades are available at the specified upgrade time they will be automatically downloaded and installed. During the install the system may be rebooted resulting in momentary loss of connectivity.'.t()
                }, {
                    boxLabel: '<strong>' + 'Do Not Automatically Install Upgrades'.t() + '</strong>',
                    inputValue: false
                }, {
                    xtype: 'component',
                    margin: '0 0 10 20',
                    html: 'If new upgrades are available at the specified upgrade time they will be not be installed. All upgrades must be manually installed using the button on the Upgrade tab.'.t() + '<br/>' +
                        '<i class="fa fa-info-circle"></i> <em>' + 'Note: Turning off Automatic Upgrades does not disable signature & list updates'.t() + '</em>'
                }]
            }]
        }, {
            title: 'Automatic Upgrade Schedule'.t(),
            hidden: true,
            bind: {
                hidden: '{!settings.autoUpgrade}'
            },
            items: [{
                xtype: 'checkboxgroup',
                columns: 1,
                vertical: true,
                bind: '{settings.autoUpgradeDays}',
                items: [
                    { boxLabel: 'Sunday'.t(), name: 'cb', inputValue: '1' },
                    { boxLabel: 'Monday'.t(), name: 'cb', inputValue: '2' },
                    { boxLabel: 'Tuesday'.t(), name: 'cb', inputValue: '3' },
                    { boxLabel: 'Wednesday'.t(), name: 'cb', inputValue: '4' },
                    { boxLabel: 'Thursday'.t(), name: 'cb', inputValue: '5' },
                    { boxLabel: 'Friday'.t(), name: 'cb', inputValue: '6' },
                    { boxLabel: 'Saturday'.t(), name: 'cb', inputValue: '7' }
                ]
            }, {
                xtype: 'timefield',
                fieldLabel: 'Auto Upgrade Time'.t(),
                labelAlign: 'top',
                editable: false,
                // format: 'H:i',
                bind: {
                    value: '{upgradeTime}',
                    disabled: '{settings.autoUpgradeDays.length === 0}'
                },
                listeners: {
                    change: 'onUpgradeTimeChange'
                }
            }],
        }]
    }]

});
