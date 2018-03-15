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
        { text: '1 Hour ago'.t(), value: Ext.Date.subtract(new Date(), Ext.Date.HOUR, 1) },
        { text: '3 Hours ago', value: Ext.Date.subtract(new Date(), Ext.Date.HOUR, 3) },
        { text: '12 Hours ago', value: Ext.Date.subtract(new Date(), Ext.Date.HOUR, 12) },
        { text: 'Today', value: Ext.Date.clearTime(new Date()) },
        { text: '24 Hours ago', value: Ext.Date.subtract(new Date(), Ext.Date.HOUR, 24) },
        { text: 'Yesterday', value: Ext.Date.subtract(Ext.Date.clearTime(new Date()), Ext.Date.DAY, 1) },
        { text: 'This Week', value: Ext.Date.subtract(Ext.Date.clearTime(new Date()), Ext.Date.DAY, (new Date()).getDay()) },
        { text: 'This Month', value: Ext.Date.getFirstDateOfMonth(new Date()) }
    ],

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
                    if (item.type !== 'range') {
                        // menu.up('button').setText(item.text);
                        menu.up('timeconditions').setSince(item.value);
                        menu.up('timeconditions').setUntil(null);
                        menu.up('timeconditions').setRange();
                    } else {
                        this.up('timeconditions').setCustomRange();
                    }
                }
            }
        }
    }],

    /** Updates the since/until dates and publishes into viewmodel */
    setRange: function () {
        var me = this, btnText;

        this.range = {
            since: this.getSince(),
            until: this.getUntil()
        };

        var existingRange = Ext.Array.findBy(me.ranges, function (r) {
            return Ext.Date.isEqual(r.value, me.getRange().since);
        });

        if (existingRange) {
            btnText = existingRange.text;
        } else {
            btnText = Ext.Date.format(this.getSince(), rpc.translations.timestamp_fmt);
            if (this.getUntil()) {
                btnText += ' \u0020\u2794\u0020 ' + Ext.Date.format(this.getUntil(), rpc.translations.timestamp_fmt);
            }
        }

        this.down('component').setHtml('<strong>' + (this.getUntil() ? 'Time Range'.t() : 'Since'.t()) + ':</strong>');
        this.down('button').setText(btnText);
        this.publishState();
    },

    /** Sets the menu item default ranges */
    setMenuItems: function () {
        var menu = this.down('button').getMenu();
        menu.removeAll();
        menu.add(this.ranges);
        menu.add({
            xtype: 'menuseparator'
        }, {
            text: 'Time Range...',
            iconCls: 'fa fa-calendar',
            type: 'range'
        });
    },


    afterRender: function () {
        this.callParent(arguments);
        this.setSince(Ext.Date.clearTime(new Date())); // initially loads today data
        this.setMenuItems();
        this.setRange();
    },

    /**
     * shows a dialog to setup some specific date/time range
     */
    setCustomRange: function () {
        var me = this;
        var since = Ext.clone(this.getSince()),
            until = this.getUntil() ? Ext.clone(this.getUntil()) : new Date(); // until needs to be a date in this range picker

        var dialog = this.add({
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
                    xtype: 'datepicker',
                    itemId: 'startDate',
                    value: since,
                    showToday: false,
                    // margin: '5 5 0 5',
                    maxDate: Util.serverToClientDate(new Date(Util.getMilliseconds())),
                    listeners: {
                        select: function (el, date) {
                            since.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());
                            // me.setValue();
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
                    xtype: 'datepicker',
                    itemId: 'endDate',
                    value: until,
                    showToday: false,
                    // margin: '5 5 0 0',
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
                handler: function (el) {
                    me.setSince(since);
                    me.setUntil(!dialog.down('#untilNow').getValue() ? until : null);
                    me.setRange();
                    dialog.hide();
                }
            }]
        });

        dialog.showBy(me.down('button'), 'tl-bl?');
    }
});
