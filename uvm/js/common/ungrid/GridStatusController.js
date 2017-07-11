Ext.define('Ung.cmp.GridStatusController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.ungridstatus',

    onUpdateGridStatus: function(){
        var me = this,
            v = me.getView(),
            store = v.up('panel').down('grid').getStore();

        var count = store.getCount();
        if( store.getData().getSource() ){
            v.update( Ext.String.format( v.tplFiltered, count, store.getData().getSource().items.length ) );
        }else{
            v.update( Ext.String.format( v.tplUnfiltered, count ) );
        }
    }
});
