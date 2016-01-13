/**
 * @author Andy Gup
 * Reference: https://cordova.apache.org/docs/en/latest/guide/hybrid/plugins/index.html
 * 
 * Copyright 2016 Esri
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.​
 */
var exec = cordova.require('cordova/exec');

var AdvancedGeolocation = {

    start: function(successCallback, errorCallback, args) {
        var argsArray = [];
        if(args){
            argsArray.push(args);
        }
        exec(successCallback, errorCallback, "AdvancedGeolocation", "start", argsArray);
    },

    stop: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "AdvancedGeolocation", "stop", []);
    },

    kill: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "AdvancedGeolocation", "kill", []);
    }
}

module.exports = AdvancedGeolocation;