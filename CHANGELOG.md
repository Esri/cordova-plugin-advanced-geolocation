# cordova-plugin-advanced-geolocation - Changelog

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