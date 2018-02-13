Ext.define('Ung.cmp.GridFilterController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.ungridfilter',

    filterEventList: function () {
        var me = this,
            field = me.getView(),
            value = field.getValue(),
            grid = field.up('panel').down('grid'),
            cols = grid.getVisibleColumns(),
            routeFilter = field.up('panel').routeFilter,
            gridStatus = field.up('panel').down('ungridstatus');

        /**
         * remove only the filters added through filer data box
         * leave alone the grid filters from columns or routes
         */
        grid.getStore().getFilters().each(function (filter) {
            if (filter.isGridFilter || filter.source === 'route') {
                return;
            }
            grid.getStore().removeFilter(filter);
        });
        // grid.getStore().clearFilter();

        // add route filter
        if (routeFilter) {
            grid.getStore().getFilters().add(routeFilter);
        }

        if (!value) {
            field.getTrigger('clear').hide();
            // the grid status update is done through grid filterchange event
            // if( gridStatus ){
            //     gridStatus.fireEvent('update');
            // }
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
    }
});
