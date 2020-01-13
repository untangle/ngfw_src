Ext.define('Ung.apps.threatprevention.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.app-threat-prevention',

    data: {
        title: 'Threat Prevention'.t(),
        iconName: 'threatprevention',

        threatMeter: null,
        currentThreatDescription: null,
        threatList: null
    },
    stores: {
        rules: { data: '{settings.rules.list}' },
    }
});
