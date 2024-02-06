Ext.define('Ung.cmp.TagTime', {
    extend: 'Ext.form.FieldContainer',
    alias: 'widget.tagtime',

    layout: 'column',
    twoWayBindable: ['value'],

    focusable: false,

    items: [{
        xtype: 'combo',
        margin: '0 0 0 3',
        width: 120,
        store: [
            [-1, 'End of Hour'.t()],
            [-2, 'End of Day'.t()],
            [-3, 'End of Week'.t()],
            [-4, 'End of Month'.t()],
            [0, 'Never'.t()],
            ['', 'Other'.t()] // just a random value to deal with other not special values
        ],
        editable: false,
        focusable: false,
        queryMode: 'local'
    }, {
        xtype: 'datefield',
        margin: '0 0 0 3',
        minValue: new Date(),
        value: new Date(),
        width: 130,
        format: 'date_fmt'.t(),
        editable: false,
        hidden: true
    }, {
        xtype: 'timefield',
        margin: '0 0 0 3',
        value: '11:50 pm',
        width: 110,
        format: 'h:i a',
        increment: 10,
        editable: true,
        hidden: true
    }],

    initComponent: function () {
        var me = this;
        me.callParent();

        me.items.items[0].on({
            scope: me,
            change: me.publishValue
        });
        me.items.items[1].on({
            scope: me,
            change: me.publishValue
        });
        me.items.items[2].on({
            scope: me,
            change: me.publishValue
        });
    },

    publishValue: function () {
        var me = this;
        if (me.rendered) {
            var combo = me.down('combo'), df = me.down('datefield'), tf = me.down('timefield');
            if (combo.getValue() <= 0 && combo.getValue() !== '') {
                me.publishState('value', combo.getValue());
            } else {
                var date = null;
                df.setHidden(false);
                tf.setHidden(false);
                if (df.getValue()) {
                    date = df.getValue();
                    if (tf.getValue()) {
                        date.setHours(tf.getValue().getHours(), tf.getValue().getMinutes(), 0, 0);
                    }
                    me.publishState('value', date.getTime());
                }
            }
        }
    },

    setValue: function (value) {
        var me = this, date, combo = me.items.items[0], df = me.items.items[1], tf = me.items.items[2];

        if (value > 0 || value === '') {
            combo.setValue('');
            df.setHidden(false);
            tf.setHidden(false);

            if (value > 0) {
                date = new Date(value);
            } else {
                date = new Date();
                date.setHours(23, 59, 0, 0); // set custom end of day
            }
            if (date) {
                df.setValue(date);
                tf.setValue(date);
            }
        } else {
            df.setHidden(true);
            combo.setValue(value);
            tf.setHidden(true);
        }
    }
});
