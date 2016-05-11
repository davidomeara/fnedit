// Copyright 2016 David O'Meara
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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