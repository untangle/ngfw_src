// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

var openImg = new Image();
openImg.src = "open.gif";
var closedImg = new Image();
closedImg.src = "closed.gif";

function showDir(dir)
{
  var dirElem = $(dir);

  if (0 == dirElem.childNodes.length) {

    new Ajax.Request("ls",
                     { method: "get",
                       parameters: "url=" + dir,
                       onComplete: function(req)
                                   {
                                     var resp = parseDomFromString(req.responseText);
                                     var root = resp.getElementsByTagName('root')[0];
                                     addChildDirectories(dirElem, root);
                                   }
                     });
  } else {
    toggleTree(dir);
  }
}

function addChildDirectories(target, dom)
{
  var path = target.getAttribute("id");

  for (var i = 0; i < dom.childNodes.length; i++) {
    if ("dir" == dom.childNodes[i].tagName) {
      var name = dom.childNodes[i].getAttribute("name");
      var childPath = path + name;

      var trig = document.createElement("span");
      trig.setAttribute("class", "trigger");
      trig.setAttribute("onClick", "showDir(\"" + childPath + "\");");

      var img = document.createElement("img");
      img.setAttribute("src", "closed.gif");
      img.setAttribute("id", "I" + childPath);
      trig.appendChild(img);

      var text = document.createTextNode(name);
      trig.appendChild(text);

      var br = document.createElement("br");
      trig.appendChild(br);

      var dir = document.createElement("span");
      dir.setAttribute("class", "dir");
      dir.setAttribute("id", childPath);

      target.appendChild(trig);
      target.appendChild(dir);

    }

    toggleTree(path);
  }
}

function toggleTree(dir)
{
  var dirElem = $(dir);
  var dirStyle = dirElem.style;
  var dirImg = $("I" + dir);

  if (dirElem.display == "block") {
    dirStyle.display = "none";
    dirImg.src = closedImg.src;
  } else {
    dirStyle.display = "block";
    dirImg.src = openImg.src;
  }
}

function parseDomFromString(text)
{
  return Try.these(function()
                   {
                     return new DOMParser().parseFromString(text, 'text/xml');
                   },
                   function()
                   {
                     var xmlDom = new ActiveXObject("Microsoft.XMLDOM")
                     xmlDom.async = "false"
                     xmlDom.loadXML(text)
                     return xmlDom;
                   });
}