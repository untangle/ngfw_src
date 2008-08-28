Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';

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
                dataIndex: 'emailAddress'
            }]);

        this.reader = new Ext.data.ArrayReader(
        {},
        [ { name : 'emailAddress' } ]);

        this.store = new Ext.data.Store({
            reader : remaps.reader,
            data : inboxDetails.remapsData
        });


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
    
        this.forwardTo = new Ext.FormPanel( {
            height : 100,
            defaultType : 'textfield',
            items : [{
                fieldLabel : i18n._( "Forward Quarantined Messages To:" ),
                name : 'email_address',
                value : inboxDetails.forwardAddress
            }],
            buttons : [ {
                text : "Apply",
                handler: function() {
                    var field = this.forwardTo.find( "name", "email_address" )[0];
                    var email = field.getValue();
                    quarantine.rpc.setRemap( this.setRemap.createDelegate( this ), 
                                             inboxDetails.token, email );
                    
                },
                scope : this
            }]
        } );

        this.grid = new Ext.grid.GridPanel({
            autoExpandColumn : 1,
            anchor : '100% -100',
            store : remaps.store,
			cls:'quarantine-received-messages-grid',
            cm : remaps.cm,
            loadMask : true,
            frame : true,
            clicksToEdit : 1,
            sm : this.selectionModel,
            tbar : [ this.deleteButton ]
         });

        var items = [];
        
        items.push ({ 
            xtype : 'radio',
            boxLabel : i18n._( "Forward Quarantined Messages To:" ),
            hideLabel : true,
			id:'radio1',
            name : "acceptQuantineMessages",
			ctCls:'quarantine-label',			
            checked : inboxDetails.forwardAddress != "",
            listeners : {
                "check" : {
                    fn : function(elem, checked) {
                        if (checked) this.setHasForwardAddress( true );
                    }.createDelegate(this)
                 }
            }});
		//var radio1 = items[items.length-1].getId();
		Ext.QuickTips.init()		
		var tt1 = new Ext.ToolTip({
			target: Ext.get('radio1'),
			title: 'Forward Quarantined Messages to....'
		});
		
        this.emailAddressField = new Ext.form.TextField({
            fieldLabel : i18n._( "Address" ),
            name : "email_address",
            value : inboxDetails.forwardAddress ,
			cls:'quarantine-left-indented'
			}
			);

        items.push( this.emailAddressField );
		this.changeAddressButton = new Ext.Button({
            text : "Change Address",
            handler: function() {
                var field = this.forwardTo.find( "name", "email_address" )[0];
                var email = field.getValue();
                quarantine.rpc.setRemap( this.setRemap.createDelegate( this ), 
                                         inboxDetails.token, email );
            },
			cls:'quarantine-change-address',
            scope : this		
		});
		items.push(this.changeAddressButton);
        /*
		items.push({
	            xtype : 'button',
	            text : "Change Address",
	            handler: function() {
	                var field = this.forwardTo.find( "name", "email_address" )[0];
	                var email = field.getValue();
	                quarantine.rpc.setRemap( this.setRemap.createDelegate( this ), 
	                                         inboxDetails.token, email );
	            },
				cls:'quarantine-change-address',
	            scope : this });
			*/

        items.push ({ 
            xtype : 'radio',
            name : "acceptQuantineMessages",
			id: 'radio2',
            boxLabel : i18n._( "Received Quarantined Messages From:" ),
            checked : inboxDetails.forwardAddress == "",
            hideLabel : true,
			cls:'quarantine-received-messages',
            listeners : {
                check : {
                    fn : function(elem, checked) {
                        if (checked) this.setHasForwardAddress( false );
                    }.createDelegate(this)
                 }
            }});
		var tt2 = new Ext.ToolTip({
			target: Ext.get('radio2'),
			title: 'Forward Quarantined Messages to....',
			html:'Forward Quarantined messages to ...'
		});
        items.push( this.grid );

        this.panel = new Ext.Panel( {
            title : i18n._("Forward or Receive Quarantines" ),
            items : items });

        this.setHasForwardAddress( inboxDetails.forwardAddress != "" );
    },

    setHasForwardAddress : function( isForwarded )
    {
        if ( isForwarded ) {
            this.emailAddressField.enable();
			this.changeAddressButton.enable();
            this.grid.disable();
        } else {
            this.emailAddressField.disable();
			this.changeAddressButton.disable();
            this.grid.enable();            
        }
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

        var count = this.store.getCount();
        try {
            /* Build a new set of data */
            for ( var c = 0 ; c < result.length ; c++ ) result[c] = [result[c]];
            
            /* If necessary display a message */
            if ( count != result.length ) {
                count = count - result.length;
                var message = i18n.pluralise( i18n._( "Delete one Remap" ), 
                                              String.format( i18n._( "Deleted {0} Remaps" ), count ),
                                              count );
                quarantine.showMessage( message );
            }
            this.store.loadData( result );
        } catch ( e ) {
            Ext.MessageBox.alert( i18n._( "Unable to update remap table." ), e ); 
            return;
        }
    },

    updateActionItems : function()
    {
        var count = this.selectionModel.getCount();
        var text = i18n.pluralise( i18n._( "Delete one Addressess" ), String.format( i18n._( "Delete {0} Addresses" ), count ), count );
        if ( count > 0 ) {
            this.deleteButton.setDisabled( false );
        } else {
            this.deleteButton.setDisabled( true );
            text = i18n._( "Delete Addresses" );
        }

        this.deleteButton.setText( text );
    }

};
