/**
 * Override needed so when month/year is changed via menus, the select event to be fired
 */
Ext.define('Ung.reports.cmp.DatePicker', {
    extend: 'Ext.picker.Date',
    alias: 'widget.datepicker-conditions',

    onOkClick: function(picker, value) {
        var me = this,
            month = value[0],
            year = value[1],
            date = new Date(year, month, me.getActive().getDate());

        if (date.getMonth() !== month) {
            // 'fix' the JS rolling date conversion if needed
            date = Ext.Date.getLastDateOfMonth(new Date(year, month, 1));
        }
        me.setValue(date);
        me.fireEvent('select', me, me.value);
        me.hideMonthPicker();
    },

    showPrevMonth: function(e) {
        var me = this,
            date = Ext.Date.add(me.activeDate, Ext.Date.MONTH, -1);
        me.setValue(date);
        me.fireEvent('select', me, me.value);
    },

    showNextMonth: function(e) {
        var me = this,
            date = Ext.Date.add(me.activeDate, Ext.Date.MONTH, 1);
        me.setValue(date);
        me.fireEvent('select', me, me.value);
    },

    showPrevYear: function() {
        var me = this,
            date = Ext.Date.add(me.activeDate, Ext.Date.YEAR, -1);
        me.setValue(date);
        me.fireEvent('select', me, me.value);
    },

    showNextYear: function() {
        var me = this,
            date = Ext.Date.add(me.activeDate, Ext.Date.YEAR, 1);
        me.setValue(date);
        me.fireEvent('select', me, me.value);
    }
});
