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
            v = me.getView(),
            vm = me.getViewModel();

        /*
         * Build source list from accompanying grid's column definitions
         */
        var masterGrid = v.up().down('grid');
        var columns = masterGrid.getColumns();

        var sourceConfig = {};
        columns.forEach( function(column){
            var displayName = column.text;
            if( column.ownerCt.text ){
                displayName = column.ownerCt.text + ' ' + displayName;
                //displayName = column.ownerCt.text + ' &#151 ' + displayName;
            }
            var config = {
                displayName: displayName
            };

            if( column.renderer && 
                !column.rtype ){
                config.renderer = column.renderer;
            }else{
                if( column.rtype ){
                    config.rtype = column.rtype;
                    config.renderer = 'columnRenderer';
                }
            }

            var key = column.dataIndex;
            sourceConfig[key] = config;
        });

        v.sourceConfig = Ext.apply({}, sourceConfig);
        v.configure(sourceConfig);
        v.reconfigure();

        masterGrid.getView().on('select', 'masterGridSelect', me );

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
            v = me.getView(),
            vm = me.getViewModel(),
            propertyRecord = record.getData();

        // hide these attributes always
        delete propertyRecord._id;
        delete propertyRecord.javaClass;
        delete propertyRecord.state;
        delete propertyRecord.attachments;

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

    columnRenderer: function(value, metaData, record, rowIndex, columnIndex, store, view){
        var rtype = view.grid.sourceConfig[record.id].rtype;
        if(rtype != null){
            return Renderer[rtype](value);
        }
    }
});
