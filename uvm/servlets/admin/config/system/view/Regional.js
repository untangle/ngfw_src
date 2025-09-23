Ext.define('Ung.config.system.view.Regional', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-system-regional',
    itemId: 'regional',
    viewModel: true,
    scrollable: true,
    withValidation: false,
    title: 'Regional'.t(),

    bodyPadding: 10,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    items: [{
        title: 'Time Settings'.t(),
        hidden: true,
        bind: {
            hidden: '{!(isExpertMode || systemSettings.timeSource === "manual" || !time)}'
        },
        items: [{
            xtype: 'radiogroup',
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{systemSettings.timeSource}',
            items: [{
                boxLabel: '<strong>' + 'Synchronize time automatically via NTP'.t() + '</strong>',
                inputValue: 'ntp'
            }, {
                xtype: 'fieldset',
                margin: 5,
                border: false,
                disabled: true,
                hidden: true,
                layout: {
                    type: 'hbox'
                },
                bind: {
                    disabled: '{systemSettings.timeSource !== "ntp"}',
                    hidden: '{systemSettings.timeSource !== "ntp"}'
                },
                items: [{
                    xtype: 'button',
                    margin: '0 10 0 0',
                    text: 'Synchronize Time'.t(),
                    iconCls: 'fa fa-refresh',
                    handler: 'syncTime'
                }, {
                    xtype: 'displayfield',
                    value: 'Click to force instant time synchronization.'.t()
                }]
            }, {
                boxLabel: '<strong>' + 'Set system clock manually'.t() + '</strong>',
                inputValue: 'manual'
            }, {
                xtype: 'datefield',
                width: 180,
                margin: '5 25',
                disabled: true,
                hidden: true,
                value: new Date(),
                format: 'timestamp_fmt'.t(),
                bind: {
                    value: '{manualDate}',
                    format: '{manualDateFormat}',
                    disabled: '{systemSettings.timeSource !== "manual"}',
                    hidden: '{systemSettings.timeSource !== "manual"}'
                }
            }]
        }]
    }, {
        title: 'Language'.t(),
        hidden: true,
        bind: {
            hidden: '{!languagesList || !languageSettings}'
        },
        items: [{
            xtype: 'combo',
            width: 350,
            bind: {
                store: '{languages}',
                value: '{languageSettings.language}'
            },
            displayField: 'name',
            valueField: 'code',
            editable: false,
            queryMode: 'local',
            listConfig:{
                getInnerTpl: function() {
                    return '<div data-qtip="{statistics}">{name}</div>';
                }
            },
            listeners: {
                change: 'languageChange'
            }
        }, {
            xtype: 'fieldset',
            title: 'Regional Formats'.t(),
            border: 0,
            margin: 10,
            items: [{
                xtype: 'radiogroup',
                columns: 1,
                vertical: true,
                simpleValue: true,
                bind: '{languageSettings.regionalFormats}',
                items: [{
                    boxLabel: 'Use defaults'.t(),
                    inputValue: 'default'
                }, {
                    boxLabel: 'Override'.t(),
                    inputValue: 'override'
                }]
            }, {
                xtype: 'fieldset',
                disabled: true,
                hidden: true,
                border: false,
                bind: {
                    disabled: '{languageSettings.regionalFormats === "default"}',
                    hidden: '{languageSettings.regionalFormats === "default"}'
                },
                defaults: {
                    xtype: 'combo',
                    labelAlign: 'right',
                    labelWidth: 150
                },
                items: [{
                    fieldLabel: 'Decimal Separator'.t(),
                    bind: '{languageSettings.overrideDecimalSep}',
                    queryMode: 'local',
                    editable: false,
                    store: [
                        ['', '. (DOT)'], // DOT is by default so value is set as ''
                        [',', ', (COMMA)'],
                        [' ', '&nbsp; (SPACE)'],
                        ['\'', '\' (APOSTROPHE)'],
                        ['&middot;', '&middot; (MIDDLE DOT)'],
                        ['&#729;', '&#729; (DOT ABOVE)']
                    ]
                }, {
                    fieldLabel: 'Thousand Separator'.t(),
                    bind: '{languageSettings.overrideThousandSep}',
                    queryMode: 'local',
                    editable: false,
                    store: [
                        ['.', '. (DOT)'],
                        ['', ', (COMMA)'], // COMMA is by default so value is set as ''
                        [' ', '&nbsp; (SPACE)'],
                        ['\'', '\' (APOSTROPHE)'],
                        ['&middot;', '&middot; (MIDDLE DOT)'],
                        ['&#729;', '&#729; (DOT ABOVE)']
                    ],
                }, {
                    fieldLabel: 'Date Format'.t(),
                    width: 400,
                    bind: '{dateFormat}',
                    // bind: '{dateFormat}',
                    queryMode: 'local',
                    editable: false,
                    store: [
                        ['Y-m-d', 'Y-m-d (e.g. 2017-05-23) - default'], // 'Y-m-d' is by default
                        ['m-d-Y', 'm-d-Y (e.g. 05-23-2017)'],
                        ['d-m-Y', 'd-m-Y (e.g. 23-05-2017)'],
                        ['Y/m/d', 'Y/m/d (e.g. 2017/05/23)'],
                        ['m/d/Y', 'm/d/Y (e.g. 05/23/2017)'],
                        ['d/m/Y', 'd/m/Y (e.g. 23/05/2017)'],
                        ['d.m.Y', 'd.m.Y (e.g. 23.05.2017)']
                    ]
                }, {
                    fieldLabel: 'Time Format'.t(),
                    width: 400,
                    bind: '{timeFormat}',
                    store: [
                        ['h:i:s a', 'h:i:s a (e.g. 06:45:35 PM) - default'],
                        ['H:i:s', 'H:i:s (e.g. 18:45:35)']
                    ],
                    queryMode: 'local',
                    editable: false
                }]
            }]
        }, {
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'button',
                margin: '0 10 0 0',
                text: 'Synchronize Language'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'syncLanguage'
            }, {
                xtype: 'displayfield',
                bind: '<strong>' + 'Last synchronized'.t() + '</strong>: {lastLanguageSync}'
                // fieldLabel: ,
                // bind: '{languageSettings.lastSynchronized}'
            }]
        }]
    }]

});
