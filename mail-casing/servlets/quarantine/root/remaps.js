Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/webui/ext/resources/images/default/s.gif';

Ung.Remaps = function() {
    this.cm = null;
}

var RemapRecord = Ext.data.Record.create( [{name : 'emailAddress', type : 'string' }] );

Ung.Remaps.prototype = {    
    init : function()
    {
        var checkColumn = new Ext.grid.CheckColumn({
            header : "&nbsp;",
            dataIndex : "delete",
            width : 40
        });

        // the column model has information about grid columns
        // dataIndex maps the column to the specific data field in
        // the data store
        this.cm = new Ext.grid.ColumnModel([
            checkColumn,
            {
                header: globalStrings.headerEmailAddress,
                dataIndex: 'emailAddress',
                width: 200,
            }]);

        this.reader = new Ext.data.ArrayReader(
        {},
        [ { name : 'emailAddress' } ]);

        this.store = new Ext.data.Store({
            reader : remaps.reader,
            data : remapsData
        });
    
        this.grid = new Ext.grid.EditorGridPanel({
            el : "quarantine-remaps",
            width : 300,
            height : 200,
            title : globalStrings.remapsGridTitle,    
            store : remaps.store,
            cm : remaps.cm,
            loadMask : true,
            frame : true,
            clicksToEdit : 1,
            plugins : checkColumn,
            tbar : [{
                text : 'Save Remaps',
                handler : function() {
                    var items = [];
                    remaps.grid.store.each( function( record ) { 
                        if ( record.data.delete == true ) items.push( record.data.emailAddress ); 
                    } );
                                        
                    var delegate = remaps.rpcHandler.createDelegate( remaps );

                    quarantine.rpc.deleteRemaps( delegate, inboxDetails.token, items );
                }
            }]});

        remaps.grid.render();
    },
    
    rpcHandler : function( result, exception )
    {
        if ( exception ) {
            Ext.MessageBox.alert("Unable to update remap table.", exception.message ); 
            return;
        }

        try {
            /* Build a new set of data */
            for ( var c = 0 ; c < result.length ; c++ ) result[c] = [result[c]];
            remaps.store.loadData( result );
        } catch ( e ) {
            Ext.MessageBox.alert("Unable to update remap table.",e ); 
            return;
        }
    }
    
};

var remaps = null;

Ext.onReady(function() {
    remaps = new Ung.Remaps();
    remaps.init();    
});
