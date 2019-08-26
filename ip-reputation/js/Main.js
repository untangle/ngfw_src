Ext.define('Ung.apps.ipreputation.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ip-reputation',
    controller: 'app-ip-reputation',

    viewModel: {
        stores: {
            passRules: { data: '{settings.passRules.list}' },
        }
    },

    items: [
        { xtype: 'app-ip-reputation-status' },
        { xtype: 'app-ip-reputation-reputation' },
        { xtype: 'app-ip-reputation-pass' }
    ],

    statics: {
        threatLevels: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'color', 'rangeBegin', 'rangeEnd', 'description', 'details' ],
            sorters: [{
                property: 'rangeBegin',
                direction: 'ASC'
            }],
            data: [
            [ 'e51313', 0,  20, 'High Risk'.t(), 'These are high risk IP addresses. There is a high predictive risk that these IPs will deliver attacks - such as malicious payloads, DoS attacks, or others - to your infrastructure and endpoints.'.t() ],
            [ 'e8580c', 21, 40, 'Suspicious'.t(), 'These are suspicious IPs. There is a higher than average predictive risk that these IPs will deliver attacks to your infrastructure and endpoints.'.t() ],
            [ 'ff9a22', 41, 60, 'Moderate Risk'.t(), 'These are generally benign IPs, but have exhibited some potential risk characteristics. There is some predictive risk that these IPs will deliver attacks to our infrastructure and endpoints.'.t() ],
            [ '68af68', 61, 80, 'Low Risk'.t(), 'These are benign IPs and rarely exhibit characteristics that expose your infrastructure and endpoints to security risks. There is a low predictive risk of attack.'.t() ],
            [ '408740', 81, 100, 'Trustworthy'.t(), 'These are clean IPs that have not been tied to a security risk. There is a very low predictive risk that your infrastructure and endpoints will be exposed to attack.'.t() ]
            ]
        }),

        threaLevelsRenderer: function(value, meta, record){
            return Ext.String.format("{0} ({1})".t(), record.get('description'), record.get('details'));
        },

        threats: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'bit', 'description', 'details' ],
            sorters: [{
                property: 'bit',
                direction: 'ASC'
            }],
            data: [
            ]
        }),

        threatsRenderer: function(value, meta, record){
            return record.get('description');
        },

        actions: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'value', 'description'],
            sorters: [{
                property: 'value',
                direction: 'ASC'
            }],
            data: [
                ["block", 'Block'.t()],
                ["pass", 'Pass'.t()]
            ]
        }),
    }

});
