Ext.define('Ung.config.system.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config-system',

    data: {
        title: 'System'.t(),
        iconName: 'system',

        languageSettings: null,
        systemSettings: null
    },
    formulas: {
        // used for setting the date/time
        manualDateFormat: function (get) { return get('languageSettings.overrideTimestampFmt') || 'timestamp_fmt'.t(); },
    }
});
