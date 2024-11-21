Ext.define('Ung.config.events.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config-events',

    data: {
        title: 'Events'.t(),
        iconName: 'events',

        settings: null,
        record: null

    },

    stores: {
        alertRules: { data: '{settings.alertRules.list}' },
        triggerRules: { data: '{settings.triggerRules.list}' },
        syslogRules: { 
            data: '{settings.syslogRules.list}',
            listeners: {
                update: 'onSyslogRulesGridChange',
                datachanged: 'onSyslogRulesGridChange'
            }
        },
        syslogServers: { 
            data: '{settings.syslogServers.list}',
            listeners: {
                update: 'onSyslogServersGridChange',
                datachanged: 'onSyslogServersGridChange'
            }
        }
    }
});
