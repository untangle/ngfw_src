dojo.provide("untangle.Button");

dojo.require("dijit.form.Button");

dojo.declare("untangle.Button", dijit.form.Button,
	{
	baseClass: "untangleButton",
	templatePath: dojo.moduleUrl("untangle","templates/Button.html"),
    templateString: "",
    label:"",
    imageSrc:""
});
//Ext.untangle.Button = Ext.extend(Ext.Button,