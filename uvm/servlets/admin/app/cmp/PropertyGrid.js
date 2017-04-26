Ext.define('Ung.cmp.PropertyGrid', {
    extend: 'Ext.grid.property.Grid',
    alias: 'widget.unpropertygrid',

    controller: 'unpropertygrid',

    editable: false,
    width: 400,
    split: true,
    collapsible: true,
    resizable: true,
    shadow: false,
    animCollapse: false,
    titleCollapse: true,
    collapsed: false,

    cls: 'prop-grid',

    viewConfig: {
        getRowClass: function(record) {
            if (record.get('value') === null || record.get('value') === '') {
                return 'empty';
            }
            return;
        }
    },

    nameColumnWidth: 200,

    // features: [{
    //     ftype: 'grouping',
    //     groupHeaderTpl: '{name}'
    // }],

    listeners: {
        beforeedit: function () {
            return false;
        },
        beforerender: function(){
            /*
             * Build source list from accompanying grid's column definitions
             */
            var columns = this.up().down('grid').getColumns();
            var sourceConfig = {};
            columns.forEach( function(column){
                var displayName = column.text;
                if( column.ownerCt.text ){
                    displayName = column.ownerCt.text + ' &#151 ' + displayName;
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
            this.sourceConfig = Ext.apply({}, sourceConfig);

            this.configure(sourceConfig);
            this.reconfigure();

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

        }
    }
});
