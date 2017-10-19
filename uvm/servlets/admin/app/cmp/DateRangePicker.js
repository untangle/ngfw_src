Ext.define('Ung.cmp.DateRangePicker', {
    extend: 'Ext.form.field.Picker',
    alias: 'widget.daterange',

    twoWayBindable: ['startDate', 'endDate'],
    publishes: ['startDate', 'endDate'],

    width: 320,

    matchFieldWidth: false,
    displayField: false,
    valueField: false,

    startDate: null,
    endDate: null,

    editable: false,

    initComponent: function() {
        this.callParent(arguments);
    },

    createPicker: function() {
        var me = this;
        me.picker = Ext.create('Ext.panel.Panel', {
            title: '<i class="fa fa-calendar"></i> ' + 'Select Date and Time Range'.t(),
            renderTo: Ext.getBody(),
            // width: 400,
            // width: this.bodyEl.getWidth(),
            // height: 200,
            floating: true,
            // modal: true,
            layout: { type: 'hbox' },
            items: [{
                xtype: 'container',
                layout: { type: 'vbox', align: 'stretch' },
                items: [{
                    xtype: 'datepicker',
                    itemId: 'startDate',
                    value: me.startDate,
                    showToday: false,
                    margin: '5 5 0 5',
                    maxDate: Util.serverToClientDate(new Date(rpc.systemManager.getMilliseconds())),
                    listeners: {
                        select: function (el, date) {
                            me.startDate.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());
                            me.setValue();
                        }
                    }
                }, {
                    xtype: 'timefield',
                    itemId: 'startTime',
                    value: me.startDate,
                    width: 100,
                    margin: '5 5 0 5',
                    increment: 10,
                    editable: false,
                    fieldLabel: 'Start Time'.t(),
                    labelWidth: 'auto',
                    labelAlign: 'right',
                    format: 'h:i A',
                    listeners: {
                        change: function (el, date) {
                            me.startDate.setHours(date.getHours(), date.getMinutes(), 0, 0);
                            me.setValue();
                        }
                    }
                }]
            }, {
                xtype: 'container',
                layout: { type: 'vbox', align: 'stretch' },
                items: [{
                    xtype: 'datepicker',
                    itemId: 'endDate',
                    value: me.endDate,
                    showToday: false,
                    margin: '5 5 0 0',
                    maxDate: Util.serverToClientDate(new Date(rpc.systemManager.getMilliseconds())),
                    listeners: {
                        select: function (el, date) {
                            me.endDate.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());
                            if (me.endDate.getTime() < me.startDate.getTime()) {
                                var sd = Ext.Date.subtract(date, Ext.Date.DAY, 1);
                                me.picker.down('#startDate').setValue(sd);
                                me.startDate.setFullYear(sd.getFullYear(), sd.getMonth(), sd.getDate());
                            }
                            // me.picker.down('#startDate').setMaxDate(date);
                            me.setValue();
                        }
                    }
                }, {
                    xtype: 'timefield',
                    itemId: 'endTime',
                    value: me.endDate,
                    width: 100,
                    margin: '5 5 0 0',
                    increment: 10,
                    editable: false,
                    fieldLabel: 'End Time'.t(),
                    labelWidth: 'auto',
                    labelAlign: 'right',
                    format: 'h:i A',
                    listeners: {
                        change: function (el, date) {
                            me.endDate.setHours(date.getHours(), date.getMinutes(), 0, 0);
                            me.setValue();
                        }
                    }
                }]
            }],
            bbar: ['->', {
                text: 'Done'.t(),
                iconCls: 'fa fa-check',
                handler: function () {
                    me.collapse();
                }
            }]
        });

        return me.picker;
    },

    listeners: {
        // collapse: function () {
        //     this.publishValue();
        // },
        expand: function () {
            this.picker.down('#startDate').setValue(this.startDate);
            this.picker.down('#startTime').setValue(this.startDate);
            this.picker.down('#endDate').setValue(this.endDate);
            this.picker.down('#endTime').setValue(this.endDate);
        }
    },

    getStartDate: function () {
        return this.startDate;
    },

    setStartDate: function (date) {
        this.startDate = date;
        this.setValue();
    },

    getEndDate: function () {
        return this.endDate;
    },

    setEndDate: function (date) {
        this.endDate = date;
        this.setValue();
    },

    setValue: function () {
        var me = this, value;
        if (!me.endDate) {
            me.endDate = Util.serverToClientDate(new Date((Math.floor(rpc.systemManager.getMilliseconds()/600000) * 600000)));
        }
        value = Ext.Date.format(me.startDate, 'Y-m-d') + ' \u25F7 ' +
                Ext.Date.format(me.startDate, 'h:i A') + ' \u0020\u2794\u0020 ' +
                Ext.Date.format(me.endDate, 'Y-m-d') + ' \u25F7 ' +
                Ext.Date.format(me.endDate, 'h:i A');
        this.callParent([value]);
        return value;
    },

    publishValue: function () {
        var me = this;
        if (me.rendered) {
            me.publishState('startDate', me.startDate);
            me.publishState('endDate', me.endDate);
        }
    }
});
