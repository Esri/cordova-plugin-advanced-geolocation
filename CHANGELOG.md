# cordova-plugin-advanced-geolocation - Changelog

## Version 1.5 - December 9, 2016
No breaking changes.

**Bug Fixes**
* Closes #43 - Plugin fails to build on Ionic. Include StopLocation Class in plugin.xml


## Version 1.4 - November 22, 2016
Possible breaking changes.

**Enhancements**
* Closes #40 - provide success callback(s) for stop() and kill(). 

**Bug Fixes**
* Closes #41 - removes memory leaks created by improper method modifiers
* Implements a cancelable Future that is returned from the ExecutureService.submit() method. The submit() method replaced the old execute(Runnable) method.

## Version 1.3.2 - October 12, 2016

No breaking changes.

**Enhancements**
* Closes #33 - removed old code comment.
* Closes #37 - missing reference in plugin.xml.

## Version 1.3.1 - September 15, 2016

No breaking changes.

**Enhancements**
* Closes #34 - Change JSONHelper.signalStrengthJSON provider property to `signal_strength`.
* Minor documentation updates.

## Version 1.3.0 - September 13, 2016

Has breaking changes. An additional configuration option is now required.

**Enhancements**
* Closes #31 - adds signal strength support via configuration options. Supported types include Lte, GSM, WCDMA and CDMA devices.

## Version 1.2.0 - September 11, 2016

No breaking changes.

**Enhancements**
* This release focused on improving the robustness of the startup and shutdown processes.
* The plugin now explicitly checks for a thread interrupt on all location providers before sending location updates. Previous versions lazily assume that the thread would be terminated. 

**Bug Fixes**
* Closes #26 - Android OS NullPointerException on initialization
* Closes #28 - LocationManager error on shutdown

## Version 1.1.1 - September 8, 2016

No breaking changes.

**Enhancements**
* Added a Leaflet mapping sample.

## Version 1.1.0 - August 24, 2016

No breaking changes.

**Bug Fixes**
* Closes #22 - Fix accidental thread interrupt clearing


## Version 1.0.0 - August 17, 2016

Has breaking changes. This is a v1 implementation so any improvements and suggestions are welcome!

**Enhancements**
* Handles Android 6 permissions with native system prompts. Continues to handle previous Android versions exactly the same as before. 
* Improved incompatible version protection. If using this library on an unsupported platform it should protect against incompatibility errors where functionality is not available on a specific Android version. If you come across something that fails please open an issue.
* Significantly improved error handling. Errors are now reported as JSON Objects that include an error number and message. Errors messages are now pervasively collected where possible.
* Improved sample app and fixed various bugs. 

**Known Issues**
* Does not provide a rationale message explaining why the library requires location information. There is a GPSPermsDeniedDialogFragment in the project and other stubs reserved for either custom implementation or as inclusion for future functionality.


## Version 0.5.1 - July 13, 2016

No breaking changes.

**Enhancements**
* Changed cell data configuration option verification from API 17 to 18 as per the Android SDK docs. CellIdentityWcdma was added at API 18 so that's now the least common denominator.

## Version 0.5.0 - July 13, 2016

No breaking changes.

**Enhancements**
* Disable cellular data configuration option if Android API Level is less than v17. This functionality is not available on those phones and could cause app to crash.
* Updates docs. Added notices that Android N will have breaking changes in terms of how GPS is implemented.

## Version 0.4.0 - May 6, 2016

No breaking changes.

**Enhancements**
* Updated API docs to correctly reflect what data is returns by GPS and Network.

**Bug Fixes**
* GPSController no longer returns a fake location if parsedLocation is null.
* NetworkController also no longer returns a fake location if parsedLocation is null. 

## Version 0.3.1 - May 4, 2016

No breaking changes.

**Bug Fixes**
* Adds missing CellLocationController to plugin.xml

## Version 0.3.0 - April 22, 2016

No breaking changes.

**Enhancements**
* Closes #2 - Added ability to access cell tower meta-data
* Various doc improvements

## Version 0.2.0 - April 19, 2016

No breaking changes.

**Enhancements**
* Various doc improvements

**Bug Fixes**
* Closes #3 - Bug in `map.js` throwing null values.