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
        threatLookupInfo: {
            model: 'Ung.apps.threatprevention.ThreatLookupInfo'
        }
    }
});

Ext.define('Ung.apps.threatprevention.ThreatLookupInfo', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'inputVal', type: 'string'},
        {name: 'address', type: 'string'},
        {name: 'category', type: 'string'},
        {name: 'score', type: 'string'},
        {name: 'level', type: 'string'},
        {name: 'levelDetails', type: 'string'}
    ]

});