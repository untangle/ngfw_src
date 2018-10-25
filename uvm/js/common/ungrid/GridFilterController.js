Ext.define('Ung.cmp.GridFilterController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ungridfilter',

    control: {
        'ungridfilter': {
            afterrender: 'afterrender',
            update: 'updateFilterSummary'
        }
    },

    setStore: function(){
        var me = this,
            vm = me.getViewModel(),
            store = me.getView().up('grid') ? me.getView().up('grid').getStore() : me.getView().up('panel').down('grid').getStore();

        if(store == null || store.isEmptyStore){
            me.setStoreTask.delay( 100 );
        }else{
            store.on(
                'datachanged',
                me.updateFilterSummary,
                me,{
                    args: [true]
                });
            store.on('filterchange', Ext.bind(me.updateFilterSummary, me));
            me.updateFilterSummary();
        }
    },

    afterrender: function(){
        var me = this;
        me.setStoreTask = new Ext.util.DelayedTask( me.setStore, me );
        me.setStoreTask.delay( 100 );
    },

    changeFilterSearch: function (field) {
        var me = this,
            vm = me.getViewModel(),
            value = field.getValue(),
            grid = this.getView().up('grid') ? this.getView().up('grid') : this.getView().up('panel').down('grid'),
            store = grid.getStore(),
            routeFilter = field.up('panel').routeFilter;

        /**
         * Remove only the filters added through filter data box
         * leave alone the grid filters from columns or routes
         */
        store.getFilters().each(function (filter) {
            if (filter.isGridFilter || filter.source === 'route') {
                return;
            }
            // If filter string is not empty, allow event. Prevent if empty.
            store.removeFilter(filter, value != '' ? true : false);
        });

        // add route filter
        if (routeFilter) {
            store.getFilters().add(routeFilter);
        }

        if (!value) {
            field.getTrigger('clear').hide();
            vm.set('filterStyle', {fontWeight: 'normal'});
            return;
        }
        vm.set('filterStyle', {fontWeight: 'bold'});

        this.createFilter(value, grid, store, routeFilter);

        field.getTrigger('clear').show();
    },

    createFilter: function(value, grid, store, routeFilter){
        var me = this,
            cols = grid.getVisibleColumns();

        var regex = Ext.String.createRegex(value, false, false, true);

        store.getFilters().add(function (item) {
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
    },

    updateFilterSummary: function(checkReset){
        var me = this,
            view = this.getView(),
            vm = this.getViewModel(),
            store = this.getView().up('grid') ? this.getView().up('grid').getStore() : this.getView().up('panel').down('grid').getStore(),
            count = store.getCount();

        if( ( checkReset === true ) &&
            ( store.getFilters().getCount() == 0) ){
            /**
             * We're told to check if the filter was reset by an external source.
             * If so, and that external souce cleared filters, we should clear
             * the filter text. 
             */
            view.down('[name=filterSearch]').setValue('');
        }

        vm.set('matchesFound', view.down('[name=filterSearch]').getValue() != '' && count ? true : false);
        if(!count ){
            vm.set('filterSummary', '');
        }else{
            vm.set('filterSummary', Ext.String.format('Showing {0} of {1}'.t(), count, store.getData().getSource() ? store.getData().getSource().items.length : count));
        }
    }
});
