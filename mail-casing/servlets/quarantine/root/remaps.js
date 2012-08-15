Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';

Ext.define('Ung.RemapsSelectionModel', {    
    extend:'Ext.selection.CheckboxModel',
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
});

Ung.Remaps = function() {
    this.cm = null;
}

Ung.Remaps.prototype = {    
    init : function()
    {
        this.selectionModel = new Ung.RemapsSelectionModel( { remaps : this } );

        this.columns = [{
            header: i18n._( "Email Address" ),
            dataIndex: 'emailAddress',
            flex: 1,
            menuDisabled: true
        }];

        this.store = Ext.create('Ext.data.ArrayStore', {
            fields:[{name:'emailAddress'}],
            data : inboxDetails.remapsData
        }); 

        this.deleteButton = Ext.create('Ext.button.Button', {
            iconCls:'icon-delete-row',                    
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
                
                quarantine.rpc.deleteRemaps( Ext.bind(this.deleteAddresses,this ), 
                                             inboxDetails.token, addresses );
            },
            scope : this
        });
    
        this.grid = Ext.create('Ext.grid.Panel',{
            anchor : '100% -100',
            cls:'quarantine-received-messages-grid',
            enableColumnHide: false,
            enableColumnMove: false,
            columns: this.columns,
            loadMask : true,
            frame : true,
            clicksToEdit : 1,
            height : 200,
            selModel : this.selectionModel,
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
                "change" : {
                    fn : Ext.bind(function(elem, checked) {
                        if (checked) this.setHasForwardAddress( true );
                    },this)
                 }
            }});
        Ext.QuickTips.init();  
        var tt1 = Ext.create('Ext.tip.ToolTip',{
            target: Ext.get('radio1'),
            title: 'Forward Quarantined Messages to....'
        });
        
        this.emailAddressField = Ext.create('Ext.form.TextField',{
            fieldLabel : i18n._( "Address" ),
            name : "email_address",
            value : inboxDetails.forwardAddress,
            cls:'quarantine-left-indented'
        });

        items.push( this.emailAddressField );
        this.changeAddressButton = Ext.create('Ext.button.Button', {
            text : i18n._("Change Address"),
            handler: Ext.bind(function() {
                    var email = this.emailAddressField.getValue();
                quarantine.rpc.setRemap( Ext.bind(this.setRemap,this ), inboxDetails.token, email );
            }, this),
            cls:'quarantine-left-indented'
        });
        items.push(this.changeAddressButton);

        items.push ({ 
            xtype : 'radio',
            name : "acceptQuantineMessages",
            id: 'radio2',
            boxLabel : i18n._( "Received Quarantined Messages From:" ),
            checked : inboxDetails.forwardAddress == "",
            hideLabel : true,
            cls:'quarantine-received-messages',
            listeners : {
                "change" : {
                    fn : Ext.bind(function(elem, checked) {
                        if (checked) this.setHasForwardAddress( false );
                    },this)
                 }
            }});
        var tt2 = Ext.create('Ext.tip.ToolTip',{
            target: Ext.get('radio2'),
            title: 'Forward Quarantined Messages to....',
            html:'Forward Quarantined messages to ...'
        });
        items.push( this.grid );

        this.panel = Ext.create('Ext.panel.Panel',{
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
                                              Ext.String.format( i18n._( "Deleted {0} Remaps" ), count ),
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
        var text = i18n.pluralise( i18n._( "Delete one Addressess" ), Ext.String.format( i18n._( "Delete {0} Addresses" ), count ), count );
        if ( count > 0 ) {
            this.deleteButton.setDisabled( false );
        } else {
            this.deleteButton.setDisabled( true );
            text = i18n._( "Delete Addresses" );
        }

        this.deleteButton.setText( text );
    }

};
