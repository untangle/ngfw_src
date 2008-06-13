Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/webui/ext/resources/images/default/s.gif';

Ung.RemapsSelectionModel = Ext.extend(  Ext.grid.CheckboxSelectionModel, {    
    onRowSelect : function( sm, rowIndex, record ) {
        this.remaps.updateActionItems();
    },

    onRowDeselect : function( sm, rowIndex, record ) {
        this.remaps.updateActionItems();
    },

    constructor : function( config ) {
        Ung.RemapsSelectionModel.superclass.constructor.apply(this, arguments);

        this.remaps = config.remaps;

        this.addListener('rowselect',this.onRowSelect, this );
        this.addListener('rowdeselect',this.onRowDeselect, this );
    }
} );

Ung.Remaps = function() {
    this.cm = null;
}

Ung.Remaps.prototype = {    
    init : function()
    {
        this.selectionModel = new Ung.RemapsSelectionModel( { remaps : this } );

        // the column model has information about grid columns
        // dataIndex maps the column to the specific data field in
        // the data store
        this.cm = new Ext.grid.ColumnModel([
            this.selectionModel,
            {
                header: i18n._( "Email Address" ),
                dataIndex: 'emailAddress',
            }]);

        this.reader = new Ext.data.ArrayReader(
        {},
        [ { name : 'emailAddress' } ]);

        this.store = new Ext.data.Store({
            reader : remaps.reader,
            data : inboxDetails.remapsData
        });
    
        this.forwardTo = new Ext.FormPanel( {
            region : "north",
            height : 100,
            defaultType : 'textfield',
            items : [{
                fieldLabel : i18n._( "Forward Quarantined Messages To" ),
                name : 'email_address',
                value : inboxDetails.forwardAddress
            }],
            buttons : [ {
                text : "Change Address",
                handler: function() {
                    var field = this.forwardTo.find( "name", "email_address" )[0];
                    var email = field.getValue();
                    quarantine.rpc.setRemap( this.setRemap.createDelegate( this ), 
                                             inboxDetails.token, email );
                    
                },
                scope : this
            }]
        } );

        this.deleteButton = new Ext.Button( {
            text : i18n._( "Delete Addresses" ),
            disabled : true,
            handler : function() {
                var addresses = [];
                this.selectionModel.each( function( record ) {
                    addresses.push( record.data.emailAddress ); 
                    return true;
                });

                this.selectionModel.clearSelections();
                this.deleteButton.setText( i18n._( "Delete Addresses" ));
                this.deleteButton.setDisabled( true );
                
                quarantine.rpc.deleteRemaps( this.deleteAddresses.createDelegate( this ), 
                                             inboxDetails.token, addresses );
            },
            scope : this
        });

        this.grid = new Ext.grid.GridPanel({
            region : "center",
            store : remaps.store,
            cm : remaps.cm,
            loadMask : true,
            frame : true,
            clicksToEdit : 1,
            sm : this.selectionModel,
            tbar : [ this.deleteButton ]
         });
    },

    setRemap : function( result, exception )
    {
        if ( exception ) {
            Ext.MessageBox.alert( i18n._( "Unable to set the forward." ), exception.message ); 
            return;
        }
    },
    
    deleteAddresses : function( result, exception )
    {
        if ( exception ) {
            Ext.MessageBox.alert( i18n._( "Unable to delete an entry." ), exception.message ); 
            return;
        }
        
        this.grid.setDisabled( false );

        try {
            /* Build a new set of data */
            for ( var c = 0 ; c < result.length ; c++ ) result[c] = [result[c]];
            remaps.store.loadData( result );
        } catch ( e ) {
            Ext.MessageBox.alert( i18n._( "Unable to update remap table." ), e ); 
            return;
        }
    },

    updateActionItems : function()
    {
        var count = this.selectionModel.getCount();
        var text = i18n.pluralise( i18n._( "Delete one Addressess" ), i18n.sprintf( i18n._( "Delete %d Addresses" ), count ), count );
        if ( count > 0 ) {
            this.deleteButton.setDisabled( false );
        } else {
            this.deleteButton.setDisabled( true );
            text = i18n._( "Delete Addresses" );
        }

        this.deleteButton.setText( text );
    }

};
