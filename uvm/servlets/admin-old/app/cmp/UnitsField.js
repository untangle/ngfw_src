Ext.define('Ung.cmp.UnitsField', {
    extend: 'Ext.form.FieldContainer',
    alias: 'widget.unitsfield',

    layout: 'column',
    twoWayBindable: ['value'],

    // publishes: 'value',

    initComponent: function () {
        var me = this;

        me.items = [{
            xtype: 'numberfield',
            width: 100,
            minValue: this.minValue,
            maxValue: this.maxValue
        }, {
            xtype: 'combo',
            margin: '0 5',
            width: 100,
            store: this.units,
            editable: false,
            value: 1
        }];
        me.callParent();

        me.items.items[0].on({
            scope: me,
            change: me.publishValue
        });
        me.items.items[1].on({
            scope: me,
            change: me.publishValue
        });
    },

    publishValue: function () {
        var me = this, nrfield = me.items.items[0], combo = me.items.items[1];
        if (me.rendered) {
            me.publishState('value', nrfield.value * combo.value);
        }
    },

    setValue: function (value) {
        var me = this, nrfield = me.items.items[0], combo = me.items.items[1], comboVal = 1;
        for (var i = 0; i < me.units.length; i += 1) {
            if (me.units[i+1]) {
                if (me.units[i][0] <= value && value < me.units[i+1][0]) {
                    comboVal = me.units[i][0];
                }
            }
        }
        if (value > me.units[me.units.length-1][0]) {
            comboVal = me.units[me.units.length-1][0];
        }
        combo.setValue(comboVal);
        nrfield.setValue(Math.round(value/combo.getValue()));
    }
});
