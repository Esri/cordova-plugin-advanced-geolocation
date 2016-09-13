/**
* @author Andy Gup
* This simple demo app displays GPS and Network geodata as well as satellite meta-data.
*
* The mapping engine in this sample uses Esri's ArcGIS API for JavaScript.
* It is a full-features, Enterprise mapping API. More details on the API as well as
* licensing info visit https://developers.arcgis.com/javascript/
*
* A lightweight Leaflet API is also available: https://github.com/Esri/esri-leaflet
*
*/
var app = {
    // Application Constructor
    initialize: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    // deviceready Event Handler. The scope of 'this' is the event.
    onDeviceReady: function() {

        var map;

        require([
            "dojo/on",
            "esri/map",
            "esri/graphic",
            "esri/geometry/Point",
            "esri/symbols/PictureMarkerSymbol",
            "dojo/domReady!"], function(on, Map, Graphic, Point, PictureMarkerSymbol) {

            var count = 0;
            var satDiv = document.getElementById("satData");
            var locationDiv = document.getElementById("locationData");

            // Displays GPS-derived locations
            var greenGPSSymbol = new PictureMarkerSymbol({
                "angle":0,
                "xoffset":0,
                "yoffset":13,
                "type":"esriPMS",
                "url":"img/green-pin.png",
                "width":35,
                "height":35
            });

            // Displays Network-derived locations
            var blueNetworkSymbol = new PictureMarkerSymbol({
                "angle":0,
                "xoffset":0,
                "yoffset":13,
                "type":"esriPMS",
                "url":"img/blue-pin.png",
                "width":35,
                "height":35
            });

            // Create our map
            map = new Map("map", {
              basemap: "topo",  //For full list of pre-defined basemaps, navigate to http://arcg.is/1JVo6Wd
              center: [-101,39], // longitude, latitude
              zoom: 4
            });

            // Wait until map is loaded before starting up location
            map.on("load",init);

            // Draw a graphic on the map
            function addGraphic(symbol, point){
                map.graphics.add(new Graphic(point, symbol));
            }

            // Required for mobile apps. Suspends drawing while map
            // is zooming in/out. Insures map is in a steady state
            // before we add a new graphic.
            function synchronizeMap(){
                map.on("zoom-start",function(){
                    console.log("Graphic drawing suspended");
                    map.graphics.suspend();
                })

                map.on("zoom-end",function(){
                    console.log("Graphic drawing resumed");
                    map.graphics.resume();
                })
            }

            function addSatelliteData(json){

                var date = new Date(parseInt(json["timestamp"]));
                var satellites = "<br /><span style='font-weight:bold;'>Satellite Data:</span> " + date.toUTCString() + "<br /><br />";

                for( var key in json){

                    if(json.hasOwnProperty(key)
                        && key.toLowerCase() != "provider"
                        && key.toLowerCase() != "timestamp"
                        && key.toLowerCase() != "error"){

                        satellites +=
                            "PRN: " + json[key].PRN +
                            ", fix: " + json[key].usedInFix +
                            ", azimuth: " + json[key].azimuth +
                            ", elevation: " + json[key].elevation + "<br />";
                    }
                }

                satDiv.innerHTML = satellites;
            }

            // Initialize the geolocation plugin
            function init(){
                synchronizeMap();

                // Zoom in once
                if(count == 0){
                    map.setLevel(15);
                    count = 1;
                }

                AdvancedGeolocation.start(function(data){

                    try{

                        // Don't draw anything if graphics layer suspended
                        if(!map.graphics.suspended){

                            var jsonObject = JSON.parse(data);

                            switch(jsonObject.provider){
                                case "gps":
                                    if(jsonObject.latitude != "0.0"){
                                        console.log("GPS location detected - lat:" +
                                            jsonObject.latitude + ", lon: " + jsonObject.longitude);
                                        var point = new Point(jsonObject.longitude, jsonObject.latitude);
                                        map.centerAt(point);
                                        addGraphic( greenGPSSymbol, point);
                                    }
                                    break;

                                case "network":
                                    if(jsonObject.latitude != "0.0"){
                                        console.log("Network location detected - lat:" +
                                            jsonObject.latitude + ", lon: " + jsonObject.longitude);
                                        var point = new Point(jsonObject.longitude, jsonObject.latitude);
                                        map.centerAt(point);
                                        addGraphic( blueNetworkSymbol, point);
                                    }
                                    break;

                                case "satellite":
                                    console.log("Satellites detected " + (Object.keys(jsonObject).length - 1));
                                    console.log("Satellite meta-data: " + data);
                                    addSatelliteData(jsonObject);
                                    break;

                                case "cell_info":
                                    console.log("cell_info JSON: " + data);
                                    break;

                                case "cell_location":
                                    console.log("cell_location JSON: " + data);
                                    break;

                                case "signal_strength":
                                    console.log("Signal strength JSON: " + data);
                                    break;
                            }
                        }
                    }
                    catch(exc){
                        console.log("Invalid JSON: " + exc);
                    }
                },
                function(error){
                    console.log("Error JSON: " + JSON.stringify(error));
                    var e = JSON.parse(error);
                    console.log("Error no.: " + e.error + ", Message: " + e.msg + ", Provider: " + e.provider);
                },
                /////////////////////////////////////////
                //
                // These are the required plugin options!
                // README has API details
                //
                /////////////////////////////////////////
                {
                    "minTime":0,
                    "minDistance":0,
                    "noWarn":false,
                    "providers":"all",
                    "useCache":true,
                    "satelliteData":true,
                    "buffer":true,
                    "bufferSize":10,
                    "signalStrength":false
                });
            }; //init
        }); // require
    } //onDeviceReady
};

app.initialize();