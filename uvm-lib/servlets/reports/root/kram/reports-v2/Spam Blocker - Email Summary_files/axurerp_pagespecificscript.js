
var PageName = 'Spam Blocker - Email Summary';
var PageId = 'pa9b01a3e334f4a04952e11af0b1004ec'
document.title = 'Spam Blocker - Email Summary';

if (top.location != self.location)
{
	if (parent.HandleMainFrameChanged) {
		parent.HandleMainFrameChanged();
	}
}

var $OnLoadVariable = '';

var $CSUM;

var hasQuery = false;
var query = window.location.hash.substring(1);
if (query.length > 0) hasQuery = true;
var vars = query.split("&");
for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split("=");
    if (pair[0].length > 0) eval("$" + pair[0] + " = decodeURI(pair[1]);");
} 

if (hasQuery && $CSUM != 1) {
alert('Prototype Warning: Variable values were truncated.');
}

function GetQuerystring() {
    return encodeURI('#OnLoadVariable=' + $OnLoadVariable + '&CSUM=1');
}

function PopulateVariables(value) {
  value = value.replace(/\[\[OnLoadVariable\]\]/g, $OnLoadVariable);
  value = value.replace(/\[\[PageName\]\]/g, PageName);
  return value;
}

function OnLoad() {

}

var u10 = document.getElementById('u10');
gv_vAlignTable['u10'] = 'top';
var u5 = document.getElementById('u5');

var u0 = document.getElementById('u0');
gv_vAlignTable['u0'] = 'top';
var u13 = document.getElementById('u13');

u13.style.cursor = 'pointer';
if (bIE) u13.attachEvent("onclick", Clicku13);
else u13.addEventListener("click", Clicku13, true);
function Clicku13(e)
{

if (true) {

	self.location.href="Spam Blocker - Email Incident.html" + GetQuerystring();

}

}
gv_vAlignTable['u13'] = 'top';
var u3 = document.getElementById('u3');
gv_vAlignTable['u3'] = 'top';
var u9 = document.getElementById('u9');
gv_vAlignTable['u9'] = 'top';
var u6 = document.getElementById('u6');
gv_vAlignTable['u6'] = 'top';
var u1 = document.getElementById('u1');

var u12 = document.getElementById('u12');

var u4 = document.getElementById('u4');
gv_vAlignTable['u4'] = 'top';
var u11 = document.getElementById('u11');

var u7 = document.getElementById('u7');

var u2 = document.getElementById('u2');

var u8 = document.getElementById('u8');

if (window.OnLoad) OnLoad();
