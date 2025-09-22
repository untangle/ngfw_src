Ext.define('Ung.config.system.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config-system',

    data: {
        title: 'System'.t(),
        iconName: 'system',

        languageSettings: null,
        languagesList: null,
        systemSettings: null,

        shieldSettings: null
    },
    formulas: {
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
        },

        lastLanguageSync: function (get) {
            // todo: to update setting new date based on timeoffsets
            var ts = get('languageSettings.lastSynchronized');
            return ts ? new Date(ts) : 'Never'.t();
        }

    },
    stores: {
        languages: {
            fields: [
            'code',
            'name',
            'statistics'
            ],
            data: '{languagesList.list}'
        },
        shieldRules: {
            data: '{shieldSettings.rules.list}'
        }
    }

});
