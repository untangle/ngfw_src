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
        passSites: { data: '{settings.passSites.list}' },
        threatLookupInfo: {
            model: 'Ung.apps.threatprevention.ThreatLookupInfo'
        }
    }
});
/**
 * ThreatLookupInfo is a model to represent the threat info that should be displayed on the Lookup Threat Tab
 */
Ext.define('Ung.apps.threatprevention.ThreatLookupInfo', {
    extend: 'Ext.data.Model',
    fields: [
         {name: 'inputVal', type: 'string'},
         {name: 'address', type: 'string'},
         {name: 'recentCount', type: 'string'},
         {name: 'level', type: 'int'},
         {name: 'popularity', type: 'int'},
         {name: 'threathistory', type: 'string'},
         {name: 'country', type: 'string'},
         {name: 'age', type: 'int'}
    ],

    hasMany: [{
        model: 'ThreatCategories',
        name: 'categories'
    },{
        model: 'ThreatHistory',
        name: 'history'
    }]

});

/**
 * ThreatCategories model contains confidence% and category ID of a threat that shows up in the Threat History or in the Threat Lookup Info
 */
Ext.define('Ung.apps.threatprevention.ThreatCategories', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'catid', type: 'int'},
        {name: 'confidence', type: 'int'}
    ]
});

/**
 * ThreatHistory contains a timestamp and any categories associated with historical data about the threat
 */
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