// https://cordova.apache.org/docs/en/latest/guide/hybrid/plugins/index.html
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