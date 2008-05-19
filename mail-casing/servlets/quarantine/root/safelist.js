Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/webui/ext/resources/images/default/s.gif';

Ung.Safelist = function() {
    this.cm = null;
}

var SafelistRecord = Ext.data.Record.create( [{name : 'emailAddress', type : 'string' }] );

Ung.Safelist.prototype = {    
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
                editor : new Ext.form.TextField()
            }]);

        this.reader = new Ext.data.ArrayReader(
        {},
        [ { name : 'emailAddress' } ]);

        this.store = new Ext.data.Store({
            reader : safelist.reader,
            data : safelistData
        });
    
        this.grid = new Ext.grid.EditorGridPanel({
            el : "quarantine-safelist",
            width : 300,
            height : 200,
            title : globalStrings.safelistGridTitle,    
            store : safelist.store,
            cm : safelist.cm,
            loadMask : true,
            frame : true,
            clicksToEdit : 1,
            plugins : checkColumn,
            tbar : [{
                text : 'Add Address',
                handler : function() {
                    safelist.grid.stopEditing();
                    safelist.store.insert( 0, new SafelistRecord( { emailAddress : "" }));
                    safelist.grid.startEditing( 0, 0 );
                }
            },{
                text : 'Save Safelist',
                handler : function() {
                    var items = [];
                    safelist.grid.store.each( function( record ) { 
                        if ( record.data.delete != true ) items.push( record.data.emailAddress ); 
                    } );

                    var getCount = function( result ) { return result.totalRecords };
                    
                    var getMessage = function( result, messages ) {
                        var safelist = globalStrings.safelistMessages[1];
                        var release = globalStrings.releaseMessages[1];
                        
                        if ( result.safelistCount <= 0 ) safelist = "";
                        if ( result.safelistCount == 1 ) safelist = globalStrings.safelistMessages[0];
                        safelist = sprintf( safelist, result.safelistCount );
                        
                        var count = quarantine.store.proxy.totalRecords - result.totalRecords;
                        if ( count <= 0 ) release = "";
                        if ( count == 1 ) release = globalStrings.releaseMessages[0];
                        release = sprintf( release, count );
                        
                        if ( safelist == "" && release == "" ) return "";
                        if ( safelist == "" ) return release;
                        if ( release == "" ) return safelist;
                        return safelist + " , " + release;
                    };
                    
                    var getSafelist = function( result ) { return result.safelist; };
                                        
                    var delegate = quarantine.refreshTable.createDelegate( quarantine, [ null, getCount, getMessage, getSafelist ], true );

                    quarantine.rpc.replaceSafelist( delegate, inboxDetails.token, items );
                }
            }]
    });

    safelist.grid.render();
    
    },
    
};

var safelist = new Ung.Safelist();

Ext.onReady(function() {
    safelist = new Ung.Safelist();
    safelist.init();    
});
