Ext.define('Ung.cmp.GridFilterController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ungridfilter',

    control: {
        'ungridfilter': {
            afterrender: 'afterrender',
            update: 'updateFilterSummary'
        }
    },

    statusEl: null,
    statusTemplate: '<div class="x-grid-empty"><p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-search fa-2x"></i> <br/>{0}</p></div>',
    statusMessageTemplate: 'Filter showing {0} of {1}'.t(),
    emptyText: null,

    setStore: function(){
        var me = this,
            v = me.getView();

        if(v == null){
            return;
        }

        var store = v.up('grid') ? v.up('grid').getStore() : v.up('panel').down('grid').getStore();

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
        me.tooltip = Ext.create('Ext.tip.ToolTip',{
            target: me.getView(),
            html: ''
        });
    },

    changeFilterSearch: function (field) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            value = field.getValue();

        if(v == null){
            return;
        }

        var grid = v.up('grid') ? v.up('grid') : v.up('panel').down('grid'),
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
        var grouping = grid.getView().findFeature('grouping');

        if (!value) {
            field.getTrigger('clear').hide();
            if(grouping){
                if(grouping.initialConfig.startCollapsed){
                    grouping.collapseAll();
                }else{
                    grouping.expandAll();
                }
            }
            v.setBorder(false);
            return;
        }
        v.setBorder(true);

        this.createFilter(grid, store, routeFilter);
        if(grouping){
            grouping.expandAll();
        }

        field.getTrigger('clear').show();
    },

    createFilter: function(grid, store, routeFilter){
        var me = this,
            vm = me.getViewModel(),
            cols = grid.getVisibleColumns();

        var regex = Ext.String.createRegex(vm.get('searchValue'), false, false, true);

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
            v = me.getView(),
            vm = me.getViewModel();

        if(v == null){
            return;
        }

        var grid = v.up('grid') ? v.up('grid') : v.up('panel').down('grid'),
            store = grid.getStore(),
            count = store.getCount();

        if( ( checkReset === true ) &&
            ( store.getFilters().getCount() == 0) ){
            /**
             * We're told to check if the filter was reset by an external source.
             * If so, and that external souce cleared filters, we should clear
             * the filter text. 
             */
            v.down('[name=filterSearch]').setValue('');
        }

        // This is used by extensiosn (such as IPS) to determine whther matches exist.
        vm.set('filterMatchesFound', v.down('[name=filterSearch]').getValue() != '' && count ? true : false);

        var status = "";
        if(me.statusEl){
            Ext.removeNode(me.statusEl);
            me.statusEl = null;
            if(me.emptyText){
                grid.getView().emptyText = me.emptyText;
            }
        }
        if(store.getFilters().getCount() == 0){
            status = "No filter".t();
        }else{
            status = Ext.String.format(me.statusMessageTemplate, count, store.getData().getSource() ? store.getData().getSource().items.length : count);
            if(grid.getView().emptyText){
                me.emptyText = grid.getView().emptyText;
                grid.getView().emptyText = "";
            }
            me.statusEl = Ext.core.DomHelper.insertHtml('beforeEnd', grid.getView().getTargetEl().dom, Ext.String.format(me.statusTemplate,status));
        }
        me.tooltip.setHtml(status);

    }
});
