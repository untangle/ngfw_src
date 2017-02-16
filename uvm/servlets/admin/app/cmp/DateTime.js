Ext.define('Ung.cmp.DateTime', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.xdatetime',

    viewModel: {
        data: {
            value: ''
        },
        formulas: {

            test: {
                get: function (get) {
                    console.log('get');
                    return get('value');
                },
                set: function (value) {
                    console.log('set');
                    this.set('value', value);
                }
            }
        }
    },

    items: [{
        xtype: 'textfield',
        bind: '{test}'
        // value: this.date,
    }]

    // reference: 'field',

    // publishes: ['value'],
    // twoWayBindable: ['value'],

    // twoWayBindable: ['date'],

    // items: [{
    //     xtype: 'textfield'
    // }],
    // defaultBindProperty: 'value',

});