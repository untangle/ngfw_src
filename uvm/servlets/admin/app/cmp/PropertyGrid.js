Ext.define('Ung.cmp.PropertyGrid', {
    extend: 'Ext.grid.property.Grid',
    alias: 'widget.unpropertygrid',

    // requires: [
    //     'Ung.cmp.PropertyGridController'
    // ],
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

    // columnLines: false,

    cls: 'prop-grid',

    viewConfig: {
        getRowClass: function(record) {
            if (record.get('value') === null || record.get('value') === '') {
                return 'empty';
            }
            return;
        }
    },

    nameColumnWidth: 150,

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
                var config = {
                    displayName: column.text
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

        }
    }
});
