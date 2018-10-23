Ext.define('Ung.cmp.GridFilterController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ungridfilter',

    control: {
        'ungridfilter': {
            afterrender: 'afterrender',
            update: 'updateFilterSummary'
        }
    },

    afterrender: function(){
        var me = this;
        /**
         * Attach listeners to grid's store events.
         */
        var store = this.getView().up('panel').down('grid').getStore();
        /**
         * A store clearFilter will fire datachanged.
         * If this happens, pass argument to updateFilterSunmary to check
         * filter count.
         * If we try to unconditionalluy check via a filterchange, we'll end up always
         * clearing the filter text.
         */
        store.on(
            'datachanged',
            this.updateFilterSummary,
            me,{
                args: [true]
            });
        store.on('filterchange', Ext.bind(this.updateFilterSummary, me));
    },

    changeFilter: function (field) {
        var me = this,
            vm = me.getViewModel(),
            value = field.getValue(),
            grid = field.up('panel').down('grid'),
            cols = grid.getVisibleColumns(),
            routeFilter = field.up('panel').routeFilter;

        /**
         * Remove only the filters added through filter data box
         * leave alone the grid filters from columns or routes
         */
        grid.getStore().getFilters().each(function (filter) {
            if (filter.isGridFilter || filter.source === 'route') {
                return;
            }
            // If filter string is not empty, allow event. Prevent if empty.
            grid.getStore().removeFilter(filter, value != '' ? true : false);
        });

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
                str.push(typeof val === 'object' ? Renderer.timestamp(val) : val.toString());
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
    },

    updateFilterSummary: function(checkReset){
        var me = this,
            view = this.getView(),
            vm = this.getViewModel(),
            store = view.up('panel').down('grid').getStore(),
            count = store.getCount();

        if( ( checkReset === true ) &&
            ( store.getFilters().getCount() == 0) ){
            /**
             * We're told to check if the filter was reset by an external source.
             * If so, and that external souce cleared filters, we should clear
             * the filter text. 
             */
            view.down('textfield').setValue('');
        }

        vm.set('filterSummary', Ext.String.format('Showing {0} of {1}'.t(), count, store.getData().getSource() ? store.getData().getSource().items.length : count));
    }
});
