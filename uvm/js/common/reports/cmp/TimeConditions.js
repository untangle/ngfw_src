/**
 * Reports Timerange Conditions used in Reports
  */
Ext.define('Ung.reports.cmp.TimeConditions', {
    extend: 'Ext.container.Container',
    alias: 'widget.timeconditions',

    publishes: 'range',
    margin: '0 5',

    layout: {
        type: 'hbox',
        align: 'middle'
    },

    config: {
        range: null,
        since: null, // initial since today
        until: null
    },

    ranges: [
        { text: '1 Hour ago'.t(), value: { since: Ext.Date.subtract(Util.serverToClientDate(new Date()), Ext.Date.HOUR, 1), until: null } },
        { text: '6 Hours ago', value: { since: Ext.Date.subtract(Util.serverToClientDate(new Date()), Ext.Date.HOUR, 6), until: null } },
        // { text: '12 Hours ago', value: { since: Ext.Date.subtract(new Date(), Ext.Date.HOUR, 12), until: null } },
        { text: 'Today', value: { since: Ext.Date.clearTime(Util.serverToClientDate(new Date())), until: null } },
        // { text: '24 Hours ago', value: { since: Ext.Date.subtract(new Date(), Ext.Date.HOUR, 24), until: null } },
        { text: 'Yesterday', value: { since: Ext.Date.subtract(Ext.Date.clearTime(Util.serverToClientDate(new Date())), Ext.Date.DAY, 1), until: null } },
        { text: 'This Week', value: { since: Ext.Date.subtract(Ext.Date.clearTime(Util.serverToClientDate(new Date())), Ext.Date.DAY, (Util.serverToClientDate(new Date())).getDay()), until: null } },
        { text: 'Last Week', value: { since: Ext.Date.subtract(Ext.Date.clearTime(Util.serverToClientDate(new Date())), Ext.Date.DAY, (Util.serverToClientDate(new Date())).getDay() + 7), until: null } },
        { text: 'This Month', value: { since: Ext.Date.getFirstDateOfMonth(Util.serverToClientDate(new Date())), until: null } }
    ],

    rangeHistory: [],

    controller: {
        listen: {
            global: {
                timerangechange: 'onTimeRangeChange'
            }
        },
        // applies timerange from zoomed in time reports
        onTimeRangeChange: function (range) {
            if (!range) { return; }
            var me = this,
                start = Util.serverToClientDate(new Date(range.min)),
                end = Util.serverToClientDate(new Date(range.max));

            // round dates to 10 minutes itervals
            start.setMinutes(Math.floor(start.getMinutes()/10) * 10, 0, 0);
            end.setMinutes(Math.ceil(end.getMinutes()/10) * 10, 0, 0);

            me.getView().setRange({
                since: start,
                until: end
            }, true);
        }
    },

    items: [{
        xtype: 'component',
        margin: '0 5 0 0',
        style: {
            fontSize: '11px'
        },
        html: '<strong>' + 'Since:'.t() + '</strong>'
    }, {
        xtype: 'button',
        // iconCls: 'fa fa-calendar',
        iconCls: 'fa fa-clock-o',
        text: 'Today'.t(),
        focusable: false,
        menu: {
            plain: true,
            showSeparator: false,
            mouseLeaveDelay: 0,
            listeners: {
                click: function (menu, item) {
                    if (item.type === 'range') {
                        menu.up('timeconditions').setCustomRange();
                        return;
                    }
                    if (item.type === 'rangehistory') {
                        return;
                    }
                    menu.up('timeconditions').setRange(item.value);
                }
            }
        }
    }],

    /** Updates the since/until dates and publishes into viewmodel */
    setRange: function (val, addToHistory) {
        var me = this, btnText;
        this.range = val;

        var existingRange = Ext.Array.findBy(me.ranges, function (r) {
            return Ext.Date.isEqual(r.value.since, me.getRange().since);
        });

        if (existingRange && !this.range.until) {
            btnText = existingRange.text;
        } else {
            var timestampFormat = Rpc.directData('rpc.translations.timestamp_fmt');
            btnText = Ext.Date.format(this.range.since, timestampFormat);
            if (this.range.until) {
                btnText += ' \u0020\u2794\u0020 ' + Ext.Date.format(this.range.until, timestampFormat);
            }
            if (addToHistory) {
                if (me.rangeHistory.length >= 5) {
                    Ext.Array.removeAt(me.rangeHistory, me.rangeHistory.length - 1);
                }
                me.rangeHistory.unshift({
                    text: btnText,
                    inHistory: true,
                    value: Ext.clone(val)
                });
            }
            me.setRangeHistory();
        }

        this.down('component').setHtml('<strong>' + (this.range.until ? 'Time Range'.t() : 'Since'.t()) + ':</strong>');
        this.down('button').setText(btnText);
        this.publishState();
    },

    /** Sets the menu item default ranges */
    setRangeHistory: function () {
        var me = this, menu = this.down('button').getMenu(), rangeMenuItems = [];

        if (menu.down('#historyItem')) {
            menu.remove(menu.down('#historyItem'));
        }
        if (this.rangeHistory.length > 0) {
            rangeMenuItems = Ext.clone(this.rangeHistory);
            rangeMenuItems.push({
                xtype: 'menuseparator'
            }, {
                text: 'Clear History'.t(),
                iconCls: 'fa fa-eraser'
            });
            menu.add({
                text: 'Time Range History'.t(),
                itemId: 'historyItem',
                iconCls: 'fa fa-history',
                type: 'rangehistory',
                menu: {
                    plain: true,
                    showSeparator: false,
                    mouseLeaveDelay: 0,
                    items: rangeMenuItems,
                    listeners: {
                        click: function (menu, item) {
                            if (item.value) {
                                me.setRange(item.value, false);
                            } else {
                                me.rangeHistory = [];
                                me.setRangeHistory();
                            }
                        }
                    }
                }
            });
        }
    },


    afterRender: function () {
        var menu = this.down('button').getMenu();
        this.callParent(arguments);
        this.setRange({
            since: Ext.Date.clearTime(Util.serverToClientDate(new Date())),
            until: null
        }); // initially loads today data

        menu.add(this.ranges);
        menu.add({
            xtype: 'menuseparator'
        }, {
            text: 'Time Range...',
            iconCls: 'fa fa-calendar',
            type: 'range'
        });
    },

    /**
     * shows a dialog to setup some specific date/time range
     */
    setCustomRange: function () {
        var me = this, range = this.getRange(), d = Util.serverToClientDate(new Date()),
            since, until;

        d.setMinutes(Math.floor(d.getMinutes()/10) * 10, 0, 0); // round new date to 10 mminutes interval

        since = Ext.Date.clone(range.since);
        until = range.until ? Ext.Date.clone(range.until) : d; // until needs to be a date in this range picker

        if (me.dialog) {
            me.dialog.down('#startDate').setValue(since);
            me.dialog.down('#startTime').setValue(since);
            me.dialog.down('#endDate').setValue(until);
            me.dialog.down('#endTime').setValue(until);
            me.dialog.showBy(me.down('button'), 'tl-bl?');
            return;
        }

        me.dialog = me.add({
            xtype: 'window',
            renderTo: Ext.getBody(),
            modal: true,
            draggable: false,
            resizable: false,
            closable: false,
            header: {
                title: 'Select Date/Time range'.t(),
                iconCls: 'fa fa-calendar'
            },
            layout: {
                type: 'hbox',
                align: 'end'
            },
            frame: false,
            border: false,
            bodyBorder: false,
            defaults: {
                bodyStyle: {
                    background: 'transparent'
                },
                border: false,
            },
            items: [{
                xtype: 'panel',
                margin: '0 2 0 0',
                layout: { type: 'vbox', align: 'stretch', pack: 'end' },
                dockedItems: [{
                    xtype: 'toolbar',
                    docked: 'top',
                    height: 24,
                    style: { background: 'transparent' },
                    border: false,
                    items: [{
                        xtype: 'component',
                        html: '<strong>' + 'Since'.t() + '</strong>'
                    }]
                }],
                items: [{
                    xtype: 'datepicker-conditions',
                    itemId: 'startDate',
                    value: since,
                    showToday: false,
                    maxDate: Util.serverToClientDate(new Date(Util.getMilliseconds())),
                    listeners: {
                        select: function (el, date) {
                            since.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());
                        }
                    }
                }, {
                    xtype: 'timefield',
                    itemId: 'startTime',
                    value: since,
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
                            since.setHours(date.getHours(), date.getMinutes(), 0, 0);
                            // me.setValue();
                        }
                    }
                }]
            }, {
                xtype: 'panel',
                margin: '0 0 0 2',
                // border: false,
                // bodyBorder: false,
                layout: { type: 'vbox', align: 'stretch', pack: 'end' },
                dockedItems: [{
                    xtype: 'toolbar',
                    docked: 'top',
                    border: false,
                    height: 24,
                    style: { background: 'transparent' },
                    items: [{
                        xtype: 'component',
                        html: '<strong>' + 'Until'.t() + '</strong>'
                    }, '->', {
                        xtype: 'checkbox',
                        itemId: 'untilNow',
                        boxLabel: 'Now'.t(),
                        value: !me.getUntil(),
                        listeners: {
                            change: function (el, val) {
                                var ed = el.up('panel').down('datepicker'),
                                    et = el.up('panel').down('timefield');
                                ed.setDisabled(val);
                                et.setDisabled(val);
                            }
                        }
                    }]
                }],

                items: [{
                    xtype: 'datepicker-conditions',
                    itemId: 'endDate',
                    value: until,
                    showToday: false,
                    maxDate: Util.serverToClientDate(new Date(Util.getMilliseconds())),
                    disabled: !me.getUntil(),
                    listeners: {
                        select: function (el, date) {
                            until.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());
                        }
                    }
                }, {
                    xtype: 'timefield',
                    itemId: 'endTime',
                    disabled: !me.getUntil(),
                    value: until,
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
                            until.setHours(date.getHours(), date.getMinutes(), 0, 0);
                        }
                    }
                }]
            }],
            buttons: [{
                text: 'Cancel'.t(),
                iconCls: 'fa fa-ban',
                handler: function (el) {
                    el.up('window').hide();
                }
            }, {
                text: 'Apply'.t(),
                iconCls: 'fa fa-check',
                handler: function () {
                    var toHistory = !me.dialog.down('#untilNow').getValue();
                    me.setRange({
                        since: since,
                        until: toHistory ? until : null
                    }, toHistory);
                    me.dialog.hide();
                }
            }]
        });

        me.dialog.showBy(me.down('button'), 'tl-bl?');
    }
});
