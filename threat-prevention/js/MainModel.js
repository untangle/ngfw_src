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
         {name: 'score', type: 'string'},
         {name: 'level', type: 'string'},
         {name: 'levelDetails', type: 'string'},
         {name: 'popularity', type: 'string'},
         {name: 'threathistory', type: 'string'},
         {name: 'country', type: 'string'},
         {name: 'age', type: 'string'}
    ],

    hasMany: [{
        model: 'ThreatCategories',
        name: 'categories'
    },{
        model: 'ThreatHistory',
        name: 'history'
    }]

});

Ext.define('Ung.apps.threatprevention.ThreatCategories', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'catid', type: 'int'},
        {name: 'confidence', type: 'int'}
    ]
});

Ext.define('Ung.apps.threatprevention.ThreatHistory', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'timestamp', type: 'int'}
    ],
    hasMany: [{
        model: 'ThreatCategories',
        name: 'categories'
    }]
});