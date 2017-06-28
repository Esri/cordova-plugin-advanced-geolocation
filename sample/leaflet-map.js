/**
* @author Andy Gup
* This simple demo app displays GPS and Network geodata as well as satellite meta-data.
*
* Esri Leaflet is a plugin to the lightweight Leaflet mapping library: https://github.com/Esri/esri-leaflet
*
*/
var app = {

    // Application Constructor
    initialize: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    // deviceready Event Handler. The scope of 'this' is the event.
    onDeviceReady: function() {

        // Reference: https://github.com/pointhi/leaflet-color-markers
        var greenIcon = new L.Icon({
            iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34],
            shadowSize: [41, 41]
        });

        var blueIcon = new L.Icon({
            iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png',
            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34],
            shadowSize: [41, 41]
        });

        var count = 0;
        var map = L.map("map").setView([39, -101], /* zoom-level */ 4);

        var basemap = L.esri.basemapLayer("Topographic");
        basemap.addTo(map);

        var satDiv = document.getElementById("satData");
        var locationDiv = document.getElementById("locationData");

        init();

        // Draw a graphic on the map
        function addGraphic(symbol, lat, lon){

            if(symbol == "green"){
                L.marker([lat, lon], {icon: greenIcon}).addTo(map);
            }
            else {
                L.marker([lat, lon], {icon: blueIcon}).addTo(map);
            }
        }

        function addLocationData(type, json){
            locationDiv.innerHTML = type + " Lat: " + Number(json.latitude).toFixed(4) +
                ", Lon: " +  Number(json.longitude).toFixed(4) +
                ", Acc: " + Number(json.accuracy).toFixed(4);
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

            // Zoom in once
            if(count == 0){
                map.setZoom(15);
                count = 1;
            }

            AdvancedGeolocation.start(function(data){

                try{

                    var jsonObject = JSON.parse(data);

                    switch(jsonObject.provider){
                        case "gps":
                            if(jsonObject.latitude != "0.0"){
                                addLocationData("GPS", jsonObject);
                                console.log("GPS location detected - lat:" +
                                    jsonObject.latitude + ", lon: " + jsonObject.longitude +
                                    ", accuracy: " + jsonObject.accuracy);
                                map.panTo(new L.LatLng(jsonObject.latitude, jsonObject.longitude))
                                addGraphic( "green", jsonObject.latitude, jsonObject.longitude);
                            }
                            break;

                        case "network":
                            if(jsonObject.latitude != "0.0"){
                                addLocationData("Network", jsonObject);
                                console.log("Network location detected - lat:" +
                                    jsonObject.latitude + ", lon: " + jsonObject.longitude +
                                    ", accuracy: " + jsonObject.accuracy);
                                map.panTo(new L.LatLng(jsonObject.latitude, jsonObject.longitude))
                                addGraphic( "blue", jsonObject.latitude, jsonObject.longitude);
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
    } //onDeviceReady
};

app.initialize();