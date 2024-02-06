/*
 * File: DateTimeField.js
 *
 * This file requires use of the Ext JS library, under independent license.
 * This is part of the UX for DateTimeField developed by Guilherme Portela
 */
Ext.define('Ung.cmp.DateTimeField', {
    extend: 'Ext.form.field.Date',
    alias: 'widget.datetimefield',

    //<locale>
    /**
     * @cfg {String} format
     * The default date format string which can be overriden for localization support. The format must be valid
     * according to {@link Ext.Date#parse}.
     */
    format: "m/d/Y H:i",
    //</locale>
    /**
     * @cfg {String} altFormats
     * Multiple date formats separated by "|" to try when parsing a user input value and it does not match the defined
     * format.
     */
    altFormats: "m/d/Y H:i:s|c",
    width: 270,

    collapseIf: function(e) {
        var me = this,
            picker = me.picker;

        if (picker.timePicker && !e.within(picker.timePicker.el, false, true)) {
            me.callParent([e]);
        }
    },

    createPicker: function() {
        var me = this,
            parentPicker = this.callParent(),
            parentConfig = Ext.clone(parentPicker.initialConfig),
            initialConfig = Ext.clone(me.initialConfig),
            excludes = ['renderTo', 'width', 'height', 'bind'];

        // Avoiding duplicate ids error
        parentPicker.destroy();

        for (var i=0; i < excludes.length; i++) {
            if (initialConfig.hasOwnProperty([excludes[i]])) {
                delete initialConfig[excludes[i]];
            }
        }

        return Ext.create('Ung.cmp.DateTimePicker', Ext.merge(initialConfig, parentConfig));
    },

    getErrors: function(value) {
        value = arguments.length > 0 ? value : this.formatDate(this.processRawValue(this.getRawValue()));

        var me = this,
            format = Ext.String.format,
            errors = me.superclass.superclass.getErrors.apply(this, arguments),
            disabledDays = me.disabledDays,
            disabledDatesRE = me.disabledDatesRE,
            minValue = me.minValue,
            maxValue = me.maxValue,
            len = disabledDays ? disabledDays.length : 0,
            i = 0,
            svalue,
            fvalue,
            day,
            time;

        if (value === null || value.length < 1) { // if it's blank and textfield didn't flag it then it's valid
             return errors;
        }

        svalue = value;
        value = me.parseDate(value);
        if (!value) {
            errors.push(format(me.invalidText, svalue, Ext.Date.unescapeFormat(me.format)));
            return errors;
        }

        time = value.getTime();
        if (minValue && time < minValue.getTime()) {
            errors.push(format(me.minText, me.formatDate(minValue)));
        }

        if (maxValue && time > maxValue.getTime()) {
            errors.push(format(me.maxText, me.formatDate(maxValue)));
        }

        if (disabledDays) {
            day = value.getDay();

            for(; i < len; i++) {
                if (day === disabledDays[i]) {
                    errors.push(me.disabledDaysText);
                    break;
                }
            }
        }

        fvalue = me.formatDate(value);
        if (disabledDatesRE && disabledDatesRE.test(fvalue)) {
            errors.push(format(me.disabledDatesText, fvalue));
        }

        return errors;
    },

    getRefItems: function() {
        var me = this,
            result = me.callParent();

        if (me.picker && me.picker.timePicker){
            result.push(me.picker.timePicker);
        }

        return result;
    },

    onExpand: function() {
        var me = this,
            timePicker;

        me.callParent();
        timePicker = me.picker && me.picker.timePicker;

        if (timePicker) {
            me.picker.alignTimePicker();
        }
    }
});

/*
 * File: DateTimePicker.js
 *
 * This file requires use of the Ext JS library, under independent license.
 * This is part of the UX for DateTimeField developed by Guilherme Portela
 */

Ext.define('Ung.cmp.DateTimePicker', {
    extend: 'Ext.picker.Date',
    alias: 'widget.datetimepicker',
    requires: [
        'Ext.picker.Date',
        'Ext.slider.Single',
        'Ext.form.field.Time',
        'Ext.form.Label'
    ],
    // <locale>
    /**
     * @cfg {String} todayText
     * The default text that will be displayed in the calendar to pick the curent date.
     */
    todayText: 'Current Date',
    // </locale>
    // <locale>
    /**
     * @cfg {String} hourText
     * The default text displayed above the hour slider
     */
    hourText: 'Hour',
    // </locale>
    // <locale>
    /**
     * @cfg {String} minuteText
     * The default text displayed above the minute slider
     */
    minuteText : 'Minutes',
    // </locale>

    initEvents: function() {
        var me = this,
            eDate = Ext.Date,
            day = eDate.DAY;

        Ext.apply(me.keyNavConfig,{
            up: function(e) {
                if (e.ctrlKey) {
                    if (e.shiftKey) {
                        me.minuteSlider.setValue(me.minuteSlider.getValue() + 1);
                    } else {
                        me.showNextYear();
                    }
                } else {
                    if (e.shiftKey) {
                        me.hourSlider.setValue(me.hourSlider.getValue() + 1);
                    } else {
                        me.update(eDate.add(me.activeDate, day, - 7));
                    }
                }
            },

            down: function(e) {
                if (e.ctrlKey) {
                    if (e.shiftKey) {
                        me.minuteSlider.setValue(me.minuteSlider.getValue() - 1);
                    } else {
                        me.showPrevYear();
                    }
                } else {
                    if (e.shiftKey) {
                        me.hourSlider.setValue(me.hourSlider.getValue() - 1);
                    } else {
                        me.update(eDate.add(me.activeDate, day, 7));
                    }
                }
            }
        });
        me.callParent();
    },

    initComponent: function() {
        var me = this,
            dtAux;

        if (typeof me.value === 'string') {
            me.value = Ext.Date.parse(me.value, me.format);
        } else if (!me.value) {
            me.value = new Date();
        }

        dtAux = me.value;

        dtAux.setSeconds(0);

        me.timeFormat = me.format.indexOf("h") !== -1 ? 'h' : 'H';
        me.hourSlider = new Ext.slider.Single({
            fieldLabel: me.hourText,
            labelAlign: 'top',
            labelSeparator: ' ',
            padding: '0 0 10 17',
            focusable : false,
            value: 0,
            minValue: 0,
            maxValue: 23,
            vertical: true,
            tipText: function(thumb){
                var value = thumb.value;

                if (me.timeFormat === 'H') {
                    return value || '0';
                } else {
                    return (value && value - 12 <= 0) ? value : Math.abs(value - 12);
                }
            },
            listeners: {
                change: me.changeTimeValue,
                scope: me
            }
        });

        me.minuteSlider = new Ext.slider.Single({
            fieldLabel: me.minuteText,
            labelAlign: 'top',
            labelSeparator: ' ',
            padding: '0 10 10 0',
            focusable : false,
            value: 0,
            increment: 1,
            minValue: 0,
            maxValue: 59,
            vertical: true,
            listeners: {
                change: me.changeTimeValue,
                scope: me
            }
        });

        me.timePicker = new Ext.panel.Panel({
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            border: false,
            defaults: {
                flex: 1
            },
            width: 130,
            floating: true,
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'top',
                ui: 'footer',
                items: [
                    '->', {
                        xtype: 'label',
                        text: me.timeFormat == 'h' ? '12:00 AM' : '00:00'
                    },
                    '->'
                ]
            }],
            items: [me.hourSlider, me.minuteSlider],
            onMouseDown: function(e) {
                e.preventDefault();
            }
        });

        me.callParent();
        me.ownerCt = me.up('[floating]');
        me.timePicker.ownerCt = me.ownerCt;
        me.registerWithOwnerCt();
        me.timePicker.registerWithOwnerCt();
        me.setValue(new Date(dtAux));
    },

    handleTabClick: function (e) {
        this.handleDateClick(e, this.activeCell.firstChild, true);
    },

    getSelectedDate: function (date) {
        var me = this,
            t = Ext.Date.clearTime(date,true).getTime(),
            cells = me.cells,
            cls = me.selectedCls,
            cellItems = cells.elements,
            cLen = cellItems.length,
            cell, c;

        cells.removeCls(cls);

        for (c = 0; c < cLen; c++) {
            cell = cellItems[c].firstChild;
            if (cell.dateValue === t) {
                return cell;
            }
        }
        return null;
    },

    changeTimeValue: function(slider, e, eOpts) {
        var me = this,
            label = me.timePicker.down('label'),
            minutePrefix = me.minuteSlider.getValue() < 10 ? '0' : '',
            hourDisplay = me.hourSlider.getValue(),
            pickerValue, hourPrefix, timeSufix, auxValue;

        if (me.timeFormat == 'h') {
            timeSufix = me.hourSlider.getValue() < 12 ? ' AM' : ' PM';
            hourDisplay = me.hourSlider.getValue() < 13 ? hourDisplay : hourDisplay - 12;
            hourDisplay = hourDisplay || '12';
        }

        hourPrefix = hourDisplay < 10 ? '0' : '';

        label.setText(hourPrefix + hourDisplay + ':' + minutePrefix + me.minuteSlider.getValue() + (timeSufix || ''));

        if (me.pickerField && (pickerValue = me.pickerField.getValue())) {
            auxValue = new Date(pickerValue[slider == me.hourSlider ? 'setHours' : 'setMinutes'](slider.getValue()));
            me.pickerField.setValue(auxValue);
            me.pickerField.fireEvent('select', me.pickerField, auxValue);
        }
    },

    afterShow: function(animateTarget, callback, scope) {
        var me = this,
            timePickerToolbarEl, backgroundColor;

        me.callParent([animateTarget, callback, scope]);
        me.timePicker.show();

        // this is a workaround for the classic theme, where the time
        // panel would have a transparent background with the classic theme.
        timePickerToolbarEl = me.timePicker.down('toolbar').getEl();
        backgroundColor = timePickerToolbarEl.getStyle('background-color');
        if (backgroundColor == 'transparent') {
            timePickerToolbarEl.setStyle('background-color', timePickerToolbarEl.getStyle('border-color'));
        }
    },

    afterSetPosition: function(x, y) {
        this.callParent([x, y]);
        this.alignTimePicker();
    },

    alignTimePicker: function() {
        var me = this,
            el = me.el,
            alignTo = me.getTimePickerSide(),
            xPos = alignTo == 'tl' ? -135 : 5;

        me.timePicker.setHeight(el.getHeight());
        me.timePicker.showBy(me, alignTo, [xPos, 0]);

    },

    onHide: function() {
        var me = this;
        me.timePicker.hide();
        me.callParent();
    },

    beforeDestroy: function() {
        var me = this;

        if (me.rendered) {
            Ext.destroy(
                me.timePicker,
                me.minuteSlider,
                me.hourSlider
            );
        }
        me.callParent();
    },

    getTimePickerSide: function() {
        var el = this.el,
            body = Ext.getBody(),
            bodyWidth = body.getViewSize().width;

        return (bodyWidth < (el.getX() + el.getWidth() + 140)) ? 'tl' : 'tr';
    },

    setValue: function(value) {
        value = value || new Date();

        value.setSeconds(0);
        this.value = new Date(value);
        return this.update(this.value);
    },

    selectToday: function() {
        var me = this,
            btn = me.todayBtn,
            handler = me.handler,
            auxDate = new Date();

        if (btn && !btn.disabled) {
            me.setValue(new Date(auxDate.setSeconds(0)));
            me.fireEvent('select', me, me.value);
            if (handler) {
                handler.call(me.scope || me, me, me.value);
            }
            me.onSelect();
        }
        return me;
    },

    handleDateClick: function(e, t, /*private*/ blockStopEvent) {
        var me = this,
            handler = me.handler,
            hourSet = me.timePicker.items.items[0].getValue(),
            minuteSet = me.timePicker.items.items[1].getValue(),
            auxDate = new Date(t.dateValue);

        if(blockStopEvent !== true) {
            e.stopEvent();
        }

        if (!me.disabled && t.dateValue && !Ext.fly(t.parentNode).hasCls(me.disabledCellCls)) {
            me.doCancelFocus = me.focusOnSelect === false;
            auxDate.setHours(hourSet, minuteSet, 0);
            me.setValue(new Date(auxDate));
            delete me.doCancelFocus;
            me.fireEvent('select', me, me.value);
            if (handler) {
                handler.call(me.scope || me, me, me.value);
            }
            me.onSelect();
        }
    },

    selectedUpdate: function(date) {
        var me = this,
            dateOnly = Ext.Date.clearTime(date, true);

        this.callParent([dateOnly]);
        me.updateSliders();

    },

    fullUpdate: function(date) {
        var me = this,
            dateOnly = Ext.Date.clearTime(date, true);

        this.callParent([dateOnly]);
        me.updateSliders();
    },

    updateSliders: function() {
        var me = this,
            currentDate = (me.pickerField && me.pickerField.getValue()) || new Date();
        if (me.timePicker.rendered) {
            me.hourSlider.setValue(currentDate.getHours());
            me.minuteSlider.setValue(currentDate.getMinutes());
        }
    }
});
