Ext.define('Ung.overrides.form.CheckboxGroup', {
    override: 'Ext.form.CheckboxGroup',

    setValue: function(value) {
        var me    = this,
            boxes = me.getBoxes(),
            b,
            bLen  = boxes.length,
            box,
            cbValue;

        me.batchChanges(function() {
            Ext.suspendLayouts();
            for (b = 0; b < bLen; b++) {
                box = boxes[b];
                cbValue = false;

                if (value) {
                    var arr_val = value;
                    if(!Ext.isArray(value)){
                        arr_val = value.split(',');
                    }
                    if (Ext.isArray(arr_val)) {
                        cbValue = Ext.Array.contains(arr_val, box.inputValue);
                    } else {
                        // single value, let the checkbox's own setValue handle conversion
                        cbValue = arr_val;
                    }
                }
                box.setValue(cbValue);
            }
            Ext.resumeLayouts(true);
        });
        return me;
    },

    getValue: function() {
        var values = [],
            boxes  = this.getBoxes(),
            b,
            bLen   = boxes.length,
            box, name, inputValue, bucket;

        for (b = 0; b < bLen; b++) {
            box        = boxes[b];
            name       = box.getName();
            inputValue = box.inputValue;

            if (box.getValue()) {
                if (values.hasOwnProperty(name)) {
                    bucket = values[name];
                    if (!Ext.isArray(bucket)) {
                        bucket = values[name] = [bucket];
                    }
                    bucket.push(inputValue);
                } else {
                    values.push(inputValue);
                }
            }
        }

        return values.join(',');
    },

});
