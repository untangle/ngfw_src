Ext.define('Ung.config.events.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config-events',
    name: 'events',

    /* requires-start */
    requires: [
        'Ung.config.events.MainController',
        'Ung.config.events.MainModel'
    ],
    /* requires-end */

    controller: 'config-events',

    viewModel: {
        type: 'config-events',
        formulas: {
            templateEmailSubject: {
                bind: '{settings.emailSubject}',
                get: function(get){
                    return this.get('settings.emailSubject') || this.get('templateDefaults.emailSubject');
                },
                set: function(value){
                    this.set('settings.emailSubject', (value == this.get('templateDefaults.emailSubject')) ? null : value);
                }
            },
            templateEmailBody: {
                bind: '{settings.emailBody}',
                get: function(get){
                    return this.get('settings.emailBody') || this.get('templateDefaults.emailBody');
                },
                set: function(value){
                    this.set('settings.emailBody', (value.replace(/(\r\n|\n)/gm, '\n') == this.get('templateDefaults.emailBody').replace(/(\r\n|\n)/gm, '\n')) ? null : value);
                }
            }
        }
    },

    items: [
        { xtype: 'config-events-alerts' },
        { xtype: 'config-events-triggers' },
        { xtype: 'config-events-syslog' },
        { xtype: 'config-events-email-template' }
    ]
});
