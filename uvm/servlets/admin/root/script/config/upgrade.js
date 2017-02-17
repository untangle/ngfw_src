Ext.define('Ung.config.upgrade.Upgrade', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.config.upgrade',

    requires: [
        'Ung.config.upgrade.UpgradeController',
        'Ung.overrides.form.CheckboxGroup'
    ],

    controller: 'config.upgrade',

    viewModel: {
        data: {
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

    dockedItems: [{
        xtype: 'toolbar',
        weight: -10,
        border: false,
        items: [{
            text: 'Back',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'tbtext',
            html: '<strong>' + 'Upgrade'.t() + '</strong>'
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        border: false,
        items: ['->', {
            text: 'Apply Changes'.t(),
            scale: 'large',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],

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
                    html: 'If new upgrades are available at the specified upgrade time they will be automatically downloaded and installed. During the install the system may be rebooted resulting in momentary loss of connectivicty.'.t()
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
                    { boxLabel: 'Thursday', name: 'cb', inputValue: '5' },
                    { boxLabel: 'Friday', name: 'cb', inputValue: '6' },
                    { boxLabel: 'Saturday', name: 'cb', inputValue: '7' }
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
Ext.define('Ung.config.upgrade.UpgradeController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.upgrade',

    control: {
        '#': {
            afterrender: 'loadSettings'
        }
    },

    loadSettings: function (view) {

        // view.getViewModel().bind('{settings.autoUpgradeHour}', function (value) {
        //     console.log(value);
        // });

        view.down('progressbar').wait({
            interval: 500,
            text: 'Checking for upgrades...'.t()
        });
        // this.checkUpgrades();
        console.log(view.getViewModel().get('settings'));
    },

    saveSettings: function () {
        var me = this;
        console.log(me.getViewModel().get('settings'));
        me.getView().setLoading('Saving ...');
        rpc.systemManager.setSettings(function (result, ex) {
            me.getView().setLoading(false);
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            Ung.Util.successToast('Upgrade Settings'.t() + ' saved!');
        }, me.getViewModel().get('settings'));

    },

    checkUpgrades: function () {
        var v = this.getView();
        rpc.systemManager.upgradesAvailable(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            console.log(result);
            v.down('progressbar').reset();
            v.down('progressbar').hide();
        });
    },

    onUpgradeTimeChange: function (field, value) {
        this.getViewModel().set('settings.autoUpgradeHour', value.getHours());
        this.getViewModel().set('settings.autoUpgradeMinute', value.getMinutes());
    }

});