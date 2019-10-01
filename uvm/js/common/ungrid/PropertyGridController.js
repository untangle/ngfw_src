Ext.define('Ung.cmp.PropertyGridController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.unpropertygrid',

    getBindRecordName: function(){
        var me = this,
            v = me.getView();

        var bindRecordName = 'propertyRecord';
        if( v.initialConfig.bind &&
            ( typeof( v.initialConfig.bind ) == 'object' ) &&
            v.initialConfig.bind.source ){
            bindRecordName = v.initialConfig.bind.source.substring( 1, v.initialConfig.bind.source.length - 1);
        }

        return bindRecordName;
    },

    /*
     * If property grid started collapsed it will have no data.
     * On expansion, force population based on first row in master grid
     */
    onBeforeExpand: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        if( vm.get(me.getBindRecordName()) == null ){
            v.up().down('grid').getView().getSelectionModel().select(0);
        }
    },

    onBeforeRender: function(){
        var me = this,
            v = me.getView();

        var sourceConfig = v.initialConfig.sourceConfig ? v.initialConfig.sourceConfig : {};

        /*
         * Build source list from accompanying grid's column definitions
         */
        var masterGrid = v.up().down('grid');
        if(masterGrid != v){
            var columns = masterGrid.getColumns();

            columns.forEach( function(column){
                var displayName = column.text;
                if( column.ownerCt.text ){
                    displayName = column.ownerCt.text + ' ' + displayName;
                }
                var config = {
                    displayName: displayName
                };

                config.renderer = column.renderer;

                var key = column.dataIndex;
                sourceConfig[key] = config;
            });
            masterGrid.getView().on('select', 'masterGridSelect', me );
        }

        me.getView().getStore().setFields(['name', 'value', 'category']);
        me.getView().getStore().setGroupField('category');

        v.sourceConfig = Ext.apply({}, sourceConfig);
        v.configure(sourceConfig);
        v.reconfigure();
    },

    /*
     * When row selected by master grid, have the property grid properly massage
     * data suitable for property grid.
     *
     * So keep in mind that this is all in the contet of the "grid master" we're attached to,
     * not this property grid.
     */
    masterGridSelect: function (grid, record) {
        var me = this,
            vm = me.getViewModel(),
            propertyRecord = record.getData();

        // hide these attributes always
        delete propertyRecord._id;
        delete propertyRecord.javaClass;
        delete propertyRecord.state;
        delete propertyRecord.attachments;
        delete propertyRecord.tags;

        var data = [];
        Ext.Object.each( propertyRecord, function(key, value){
            if(value != null){
                data.push({
                    name: key,
                    value: value,
                    category: 'Event'.t()
                });
            }
        });

        me.getView().getStore().loadData(data);
    },

    /**
     * Used for extra column actions which can be added to the grid but are very specific to that context
     * The grid requires to have defined a parentView tied to the controller on which action method is implemented
     * action - is an extra configuration set on actioncolumn and represents the name of the method to be called
     * see Users/UsersController implementation
     */
    externalAction: function (v, rowIndex, colIndex, item, e, record) {
        var view = this.getView(),
            parentController = null,
            action = item && item.action ? item.action : v.action;

        while( view != null){
            parentController = view.getController();

            if( parentController && parentController[action]){
                break;
            }
            view = view.up();
        }

        if (!parentController) {
            console.log('Unable to get the extra controller');
            return;
        }

        // call the action from the extra controller in extra controller scope, and pass all the actioncolumn arguments
        if (action) {
            parentController[action].apply(parentController, arguments);
        } else {
            console.log('External action not defined!');
        }
    }
});
