Ext.define('Ung.config.system.Regional', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.system.regional',
    itemId: 'regional',


    viewModel: {
        data: {
            isExpertMode: rpc.isExpertMode,
            time: null,
            languageSettings: rpc.languageManager.getLanguageSettings(),
            languagesList: rpc.languageManager.getLanguagesList(),
            systemSettings: rpc.systemManager.getSettings(),
            tz: rpc.systemManager.getTimeZone(),
            timeZones: null
        },
        formulas: {
            timeSource: function (get) {
                return get('systemSettings.timeSource') === 'manual' ? 'Time was set manually'.t() : 'Time is automatically synchronized via NTP'.t();
            },
            manualDate: function (get) {
                // to fix because rpc.systemManager.getDate() returns an invalid date string
                return get('time') ? new Date(get('time').replace('EET', '(EET)')) : new Date();
            },
            // used for setting the date/time
            manualDateFormat: function (get) { return get('languageSettings.overrideTimestampFmt') || 'timestamp_fmt'.t(); },

            dateFormat: {
                get: function (get) {
                    var fmt = get('languageSettings.overrideDateFmt');
                    return fmt.length === 0 ? 'Y-m-d' : fmt;
                },
                set: function (value) {
                    var fmt = value + ' ' + this.get('timeFormat');
                    this.set('languageSettings.overrideDateFmt', value === 'Y-m-d' ? '' : value);
                    this.set('languageSettings.overrideTimestampFmt', fmt === 'Y-m-d h:i:s a' ? '' : fmt);
                }
            },

            timeFormat: {
                get: function (get) {
                    var tsFmt = get('languageSettings.overrideTimestampFmt');
                    return tsFmt.length > 0 ? tsFmt.substring(6) : 'h:i:s a';
                },
                set: function (value) {
                    var dateFmt = this.get('languageSettings.overrideDateFmt'),
                        fmt = (dateFmt.length === 0 ? 'Y-m-d' : dateFmt) + ' ' + value;
                    this.set('languageSettings.overrideTimestampFmt', fmt === 'Y-m-d h:i:s a' ? '' : fmt);
                }
            }

            // decimalSep: function (get) { return get('languageSettings.overrideDecimalSep') || 'decimal_sep'.t(); },
            // thousandSep: function (get) { return get('languageSettings.overrideThousandSep') || 'thousand_sep'.t(); },
            // dateFmt: function (get) { return get('languageSettings.overrideDateFmt') || 'date_fmt'.t(); },
            // timestampFmt: function (get) { return get('languageSettings.overrideTimestampFmt') || 'timestamp_fmt'.t(); }
        },
        stores: {
            timeZone: {
                fields: ['name', 'value'],
                data: '{timeZones}'
            },
            languages: {
                fields: ['code', 'name', 'statistics', {
                    name: 'cc',
                    calculate: function (r) {
                        return r.code ? r.code.split('-')[1] : r.code;
                    }
                }],
                data: '{languagesList.list}'
            }
        }
    },

    title: 'Regional'.t(),

    bodyPadding: 10,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    items: [{
        title: 'Current Time'.t(),
        items: [{
            xtype: 'component',
            bind: '{timeSource}'
        }, {
            xtype: 'component',
            margin: '10 0 0 0',
            bind: '<i class="fa fa-clock-o"></i> <strong>{time}</strong>'
        }]
    }, {
        title: 'Force Sync Time'.t(),
        hidden: true,
        bind: {
            hidden: '{isExpertMode || systemSettings.timeSource === "manual"}'
        },
        items: [{
            xtype: 'component',
            bind: 'Click to force instant time synchronization.'.t()
        }, {
            xtype: 'button',
            margin: '10 0 0 0',
            text: 'Synchronize Time'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'syncTime'
        }]
    }, {
        title: 'Time Settings'.t(),
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
                margin: '5 25',
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
                    xtype: 'displayfield',
                    value: 'Click to force instant time synchronization.'.t()
                }, {
                    xtype: 'button',
                    margin: '0 0 0 10',
                    text: 'Synchronize Time'.t(),
                    iconCls: 'fa fa-refresh',
                    handler: 'syncTime'
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
                bind: {
                    value: '{manualDate}',
                    format: '{manualDateFormat}',
                    disabled: '{systemSettings.timeSource !== "manual"}',
                    hidden: '{systemSettings.timeSource !== "manual"}'
                }
            }]
        }]
    }, {
        title: 'Timezone'.t(),
        items: [{
            xtype: 'combo',
            width: 350,
            bind: {
                store: '{timeZone}',
                value: '{tz.ID}'
            },
            displayField: 'name',
            valueField: 'value',
            editable: false,
            queryMode: 'local'
        }]
    }, {
        title: 'Language'.t(),
        items: [{
            xtype: 'combo',
            width: 350,
            bind: {
                store: '{languages}',
                value: '{languageSettings.language}'
            },
            displayField: 'name',
            valueField: 'cc',
            editable: false,
            queryMode: 'local',
            listConfig:{
                getInnerTpl: function() {
                    return '<div data-qtip="{statistics}">{name}</div>';
                }
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
                        ["'", "' (APOSTROPHE)"],
                        ['&middot;', "&middot; (MIDDLE DOT)"],
                        ['&#729;', "&#729; (DOT ABOVE)"]
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
                        ["'", "' (APOSTROPHE)"],
                        ['&middot;', "&middot; (MIDDLE DOT)"],
                        ['&#729;', "&#729; (DOT ABOVE)"]
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
                        ['G:i:s', 'G:i:s (e.g. 18:45:35)']
                    ],
                    queryMode: 'local',
                    editable: false
                }]
            }]
        }, {
            xtype: 'button',
            text: 'Synchronize Language'.t(),
            iconCls: 'fa fa-refresh'
        }, {
            xtype: 'component',
            padding: 5,
            bind: 'Last synchronized'.t() + ': {languageSettings.lastSynchronized}'
            // fieldLabel: ,
            // bind: '{languageSettings.lastSynchronized}'
        }]
    }]

});