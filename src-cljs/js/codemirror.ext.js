CodeMirror.prototype.on = function(type, func) {};
CodeMirror.prototype.off = function(type) {};

/**
 * @return {CodeMirrorObj}
 */
CodeMirror.prototype.doc = null;

/**
 * @return {CodeMirrorObj}
 */
CodeMirror.prototype.getDoc = function() {};

/**
 * @param {{line:number, ch:number}} pos
 * @return {number}
 */
CodeMirrorObj.prototype.indexFromPos = function(pos) {};

/**
 * @param {number} index
 * @return {{line:number, ch:number}}
 */
CodeMirrorObj.prototype.posFromIndex = function(index) {};

/**
 * @param {function(CodeMirrorLineHandle)} f
 */
CodeMirrorObj.prototype.eachLine = function(f) {};

/**
 * @return {CodeMirrorLineHandle} index
 */
CodeMirrorObj.prototype.getLineHandle = function(index) {};

CodeMirrorObj.prototype.addLineWidget = function(line, node, options) {};

CodeMirrorLineHandle.prototype.widgets = null;
//CodeMirrorLineHandle.prototype.handle = null;


//CodeMirrorObj.prototype.line = null;
//CodeMirrorLineHandle.prototype.widgets = null;