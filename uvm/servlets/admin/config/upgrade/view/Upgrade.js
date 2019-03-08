Ext.define('Ung.config.upgrade.view.Upgrade', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-upgrade-upgrade',
    itemId: 'regional',
    viewModel: true,
    scrollable: true,

    title: 'Upgrade Settings'.t(),

    items: [{
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
                xtype: 'combo',
                itemId: 'dayscombo',
                reference: 'dayscombo',
                queryMode: 'local',
                editable: false,
                publishes: 'value',
                store: [['any', 'Any week day'.t()], ['specific', 'Specific days'.t()]],
                listeners: {
                    change: 'onDaysComboChange'
                }
            }, {
                xtype: 'checkboxgroup',
                itemId: 'daysgroup',
                columns: 1,
                vertical: true,
                hidden: true,
                bind: {
                    hidden: '{dayscombo.value !== "specific"}'
                },
                items: [
                    { boxLabel: 'Sunday'.t(), name: 'cb', inputValue: '1' },
                    { boxLabel: 'Monday'.t(), name: 'cb', inputValue: '2' },
                    { boxLabel: 'Tuesday'.t(), name: 'cb', inputValue: '3' },
                    { boxLabel: 'Wednesday'.t(), name: 'cb', inputValue: '4' },
                    { boxLabel: 'Thursday'.t(), name: 'cb', inputValue: '5' },
                    { boxLabel: 'Friday'.t(), name: 'cb', inputValue: '6' },
                    { boxLabel: 'Saturday'.t(), name: 'cb', inputValue: '7' }
                ],
                listeners: {
                    change: 'onDaysGroupChange'
                }
            }, {
                xtype: 'timefield',
                fieldLabel: 'Auto Upgrade Time'.t(),
                labelAlign: 'top',
                editable: false,
                format: 'H:i',
                listeners: {
                    change: 'onUpgradeTimeChange'
                }
            }],
        }]
    }]


});
