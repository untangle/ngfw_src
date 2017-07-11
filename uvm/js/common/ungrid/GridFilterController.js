Ext.define('Ung.cmp.GridFilterController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.ungridfilter',

    filterEventList: function (field, value) {
        var me = this,
            v = me.getView(),
            grid = v.up('panel').down('grid'),
            cols = grid.getVisibleColumns(),
            routeFilter = v.up('panel').routeFilter;

        grid.getStore().clearFilter();

        // add route filter
        if (routeFilter) {
            grid.getStore().getFilters().add(routeFilter);
        }

        if (!value) {
            field.getTrigger('clear').hide();
            return;
        }

        var regex = Ext.String.createRegex(value, false, false, true);

        grid.getStore().getFilters().add(function (item) {
            var str = [], filtered = false;

            Ext.Array.each(cols, function (col) {
                var val = item.get(col.dataIndex);
                if (!val) { return; }
                str.push(typeof val === 'object' ? Util.timestampFormat(val) : val.toString());
            });
            if (regex.test(str.join('|'))) { filtered = true; }

            // exclude if record does not meet route filter
            if (routeFilter) {
                if (item.get(routeFilter.property) !== routeFilter.value) {
                    filtered = false;
                }
            }
            return filtered;
        });

        field.getTrigger('clear').show();

        var gridStatus = v.up('panel').down('ungridstatus');
        if( gridStatus ){
            gridStatus.fireEvent('update');
        }
    }
});
