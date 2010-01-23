RadioBox = function() 
{
    /* Create two variables to initialize these functions. */
    var onUpdateRadioButton = function( elem, checked )
    {
        if ( checked ) {
            this.getBaseSettings().authenticationType = elem.inputValue;
        }
    }.createDelegate(this);

    var onRenderRadioButton = function( elem )
    {
        elem.setValue(this.getBaseSettings().pageType);
    }.createDelegate(this);

    var sample  = {
        xtype : "radio",
        boxLabel : this.i18n._("custom-value"), /* Value to display */
        hideLabel : true,
        name : "common-name", /* This should be common to the other radios */
        inputValue : "value-to-save", /* Value to save */
        listeners : {
            "check" : onUpdateRadioButton,
            "render" : onRenderRadioButton
        }
    }
    
};

TimeField = function()
{
    /* Here are some sample time fields. */
    var fields = [{
        xtype : "utimefield", /* utimefield has extensions for appending the end of time, and defaults the format to 24 hour time */
        name : "startTime",
        dataIndex : "startTime",
        fieldLabel : this.i18n._("Start Time"),
        allowBlank : false
    },{
        xtype : "utimefield",
        endTime : true, /* Set endTime if you want 23:59 to show up. */
        name : "endTime",
        dataIndex : "endTime",
        fieldLabel : this.i18n._("End Time"),
        allowBlank : false
    }]
}

var ComboBox = function()
{
    /* This is a simple combo box with values that are from an enum */
    var sample = {
        xtype : "combo",
        name : "method",
        value : this.getRadiusServerSettings().method,
        store : [
            [ "CLEARTEXT", this.i18n._( "Cleartext" )],
            [ "PAP", this.i18n._( "PAP" )],
            [ "CHAP", this.i18n._( "CHAP" )],
            [ "MSCHAPV1", this.i18n._( "MSCHAPv1" )],
            [ "MSCHAPV2", this.i18n._( "MSCHAPv2" )]],
        /* Use the following to indent the combobox.  If you do this, make sure you change the label width to the same value in the enclosing fieldset or panel. */
        labelStyle : 'text-align: right; width: 250px',
        mode : "local",
        triggerAction : "all",
        fieldLabel : this.i18n._( "Authentication Method" ),
        width : 100,
        listWidth : 90,
        listClass : "x-combo-list-small",
        editable : false,
        listeners : {
            "change" : function( elem, newValue ) {
                this.getRadiusServerSettings().authenticationMethod = newValue;
            }.createDelegate( this )
        }
    }
}