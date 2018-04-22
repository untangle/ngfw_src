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

        v.sourceConfig = Ext.apply({}, sourceConfig);
        v.configure(sourceConfig);
        v.reconfigure();

        // this.getStore().sort('group');
        // this.getStore().group('group');

        // this.getStore().on({
        //     datachanged: Ext.bind(function( store ){
        //         var columns = this.up().down('grid').getColumns();
        //         store.each(function(record){
        //             var groupName = '';
        //             var recordName = record.get('name');
        //             columns.find( function(column){
        //                 if( column.dataIndex == recordName ){
        //                     if( column.ownerCt.text ){
        //                         groupName = column.ownerCt.text;
        //                     }
        //                 }
        //             });
        //             record.set('group', groupName);
        //         });
        //     },this)
        // });
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

        for( var k in propertyRecord ){
            if( propertyRecord[k] == null ){
                continue;
            }
            /* If object only contains a JavaClass and one other
            /* field, use that field as the new non-object value.
            /* It works for timestamp...it should work for others. */
            if( ( typeof( propertyRecord[k] ) == 'object' ) &&
                 ( Object.keys(propertyRecord[k]).length == 2 ) &&
                 ( 'javaClass' in propertyRecord[k] ) ){
                var value = '';
                Object.keys(propertyRecord[k]).forEach( function(key){
                    if( key != 'javaClass' ){
                        value = propertyRecord[k][key];
                    }
                });
                propertyRecord[k] = value;
            }
            /*
             * Encode objects and arrays for details
             */
            if( ( typeof( propertyRecord[k] ) == 'object' ) ||
                ( typeof( propertyRecord[k] ) == 'array' ) ){
                propertyRecord[k] = Ext.encode( propertyRecord[k] );
            }
        }

        vm.set( me.getBindRecordName(), propertyRecord );
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
