# ST_StreamLabs-Water-Flow
This is an unofficial **Classic** SmartThings interface to the [StreamLabs Smart Home Water Meter](https://www.streamlabswater.com/). It allows SmartThings to be able to control the StreamLabs Home/Away status as well as providing StreamLabs flow alerts in the form of SmartThings water leaks. This allows StreamLabs alerts to be interfaced to other SmartApps such as Smart Home Monitor. Note that the ability to delay (Pause) SmartThings declaring a water leak from a StreamLabs alert is an added feature of this implementation.

These modules are not intended to replace StreamLab's Phone App but to just provide an interface to SmartThings.
## Getting Started
The StreamLabs API runs in the "cloud"; for SmartThings to interrogate cloud interfaces, it requires two pieces: both a Device Handler and a Service Manager (a special kind of SmartApp). You will need to obtain the groovy source code for both from GitHub and install them into the SmartThings IDE. You should also have already created a StreamLabs account and successfully installed the Water Meter with its App.
### Obtaining Source Code & IDE Installation
You can either use the traditional cut-and-paste method or integration with GitHub to provide the source code to the SmartThings IDE. The Device Handler is called "StreamLabs Water Flow DH" and the Service Manager/SmartApp is called "StreamLabs Water Flow SM". If using GitHub integration, you will need the following information: Owner=windsurfer99, Name=ST_StreamLabs-Water-Flow, Branch=master.  Both the Device Handler and Service Manager should be set to 'Published' so the the Phone App can install them. For additional information on these standard SmartThings installation practices, some suggested documentation includes:
 - [FAQ: An Overview of Using Custom Code in SmartThings (SmartThings Classic)](https://community.smartthings.com/t/faq-an-overview-of-using-custom-code-in-smartthings-smartthings-classic/16772)
 - [FAQ: GitHub Integration How to Add and Update from Repositories](https://community.smartthings.com/t/faq-github-integration-how-to-add-and-update-from-repositories/39046)
### Getting an API Key
You will need to request an API key for your account from StreamLabs. As of my initial interface creation, the API was free but there are indications that there may be a cost in the future. Follow the instructions on the [StreamLabs Getting Started](https://developer.streamlabswater.com/docs/getting-started.html) page to request your API key. My interface only supports the API key mode of authentication (OAUTH2 is not currently supported), so make sure you follow the steps to request that. This will take some days for StreamLabs to approve your request.
### Install API Key
After installing the Device Handler and Service Manager and then receiving the key from StreamLabs, the key must be entered into the Service Manager.  In the SmartThings IDE, select the newly installed "StreamLabs Water Flow SM" on the "My SmartApps" page. Select "App Settings" in upper right of the  page. Expand "Settings" on the App Settings page and enter "api_key" in the name box (if not already populated) and the provided key in the value box. select "Update' on the bottom of the page.
### Phone App Service Manager Installation
You may want to be logged into the SmartThings IDE and viewing the "Live Logging" page for any errors that might occur during installation. Use the standard SmartApp installation methods to install the Service Manager in the Phone App. That is, go to the "Automation" page and the "SmartApps" tab. Scroll to the bottom and select "Add a SmartApp". At the bottom, select "My Apps". Select the "StreamLabs Water Flow SM". Configure the Service Manager. The following sections provide additional information on the options.

Once the Service Manager is installed, it will search the StreamLabs cloud for the Water Meter. If it is found, the Device Handler will then be automatically installed within about 30 seconds.
#### Assign Name for Service Manager
By default, the name will be "StreamLabs Water Flow SM". This can be changed if desired with this entry. Also this allows (untested) for a second Service Manager to be installed with a unique name to handle a home that has 2 SmartLabs devices.
#### Enter SmartThings modes when water meter should be away
This allows you to optionally control the Home/Away status of the StreamLabs Water Meter from SmartThings. If nothing is checked, SmartThings will not automatically control the Home/Away status. Alternately, one or more SmartThings modes can be selected. When SmartThings mode changes to one of the selected items, then the StreamLabs status will be changed to "Away". When the mode changes to one not selected, it will be changed to "Home".  Refer to the StreamLabs documentation on this functionality but in a nutshell, when it is "Away" a flow alert is posted after about 15 seconds of any amount of water flow. When "Home" than an alert is posted based on user defined parameters.
#### Enter StreamLabs location name assigned to StreamLabs flow meter
When the StreamLabs device is installed, you provided a name to be given to the location. This location name needs to be entered here so that it can be found. It is not case sensitive.
#### IDE Logging Level.
This is an optional value to control the level of logging in the IDE. Normally this can be left alone unless extensive debugging is desired.

### Device Handler Installation
As stated previously, the Device Handler will be automatically installed; you should not manually install it. If you remove the Service Manager, the Device Handler will be automatically removed also. After the Device Handler has been installed, the following options are available (Press the gear in the Handler to modify these):
#### Give your Device a Name
Allows you to change the name of the Device Handler from its default.
#### # of minutes for Pause
This entry (along with the User Interface) allows for pausing a StreamLabs alert from registering a SmartThings water leak. This is how long the pause will stay in effect.
#### IDE Live Logging Level
This is an optional value to control the level of logging in the IDE. Normally this can be left alone unless extensive debugging is desired.

### Device Handler User Interface
The following paragraphs describe the user interface elements.
#### Wet/Dry
This graphic at the top of the panel displays the current state of the Device: wet or dry. This Device Handler implements a standard water leak sensor device; as such this is read only and you cannot override the status.
#### Home/Away
This icon provides a graphical representation of the StreamLabs Home/Away status. The Home/Away status defines different algorithms within the Water Meter to determine if there is a leak. This icon is also a button; pressing it will toggle the StreamLabs status. Note that if one or more SmartThings modes have been set in the Service Manager configuration to control this status, toggling the status with this button will only be temporary until the next SmartThings mode change.
#### Pause
This is a feature totally separate from the StreamLabs cloud. If you anticipate using a water flow that would exceed the trip settings in the StreamLabs App, you can press the "Pause" button. (For example you will be watering the lawn for then next hour.) This will cause the Device Handler to start a countdown to ignore any StreamLabs alerts for the number of minutes set in the Device Handler configuration ("# of minutes for Pause"). Once the time limit has expired, if the StreamLabs is still in an alert condition, then the Device Handler will post a 'Wet' condition. Note that this button is also a icon displaying whether the Device Handler is paused or not. To manually cancel the pause, just press the button again. If the configuration parameter is 0 or not set, the pause will stay on until you press the button again (i.e., there is no automatic timeout).
#### Refresh
This button requests an update from the StreamLabs cloud of water usage and Home/Away state.
#### Usage Today
This displays the gallons used today (as reported by the StreamLabs cloud).
#### Usage This Month
This displays the gallons used this calendar month (as reported by the StreamLabs cloud).
#### Usage This Year
This displays the gallons used this calendar year (as reported by the StreamLabs cloud).
## Idiosyncrasies
### Polling
I was not able to determine a method to subscribe for StreamLabs alerts using an API Key interface (I assume a mechanism doesn't exist). Therefore the Service Manager polls the StreamLabs cloud every 3 minutes for a change in alerts. I picked this as a compromise to not overload the cloud but to also receive timely events.
### Water Usage
The water usage displayed in the Device Handler typically does not exactly match the usage displayed in the StreamLabs App. Based on a response in their forum, this is to be expected and is an artifact of their implementation.
## License
This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details
## Acknowledgments

* Hat tip to codersaur for his code to control the IDE logging.
* While I have ample software experience, this is my first attempt at SmartThings programming; hopefully I have adhered to most Best Practices.

<!--stackedit_data:
eyJoaXN0b3J5IjpbNTQ1NDQ5Njc2LDEyMDE1NTgwNSwxMzY4OT
IwNTkxLC04NTEwNDE0NzksMTE3MTg2NzU3NywtMTM4OTYyNDcy
Nyw0MzAzODU1NTEsLTY5ODI2NzY2NywtNjAxNDU3MzMxLDE2Mz
A5NTU3MTgsLTE5NTYyNDY1MTksLTEyNDIxMjQ3NzMsLTE4NTI3
NjY0NywtNDg5NzI4Mzc1LDYzOTg2MjcxMyw2NjQ4ODMxNDcsLT
ExMTY4NDEyNzUsMTEzNDU3NjQ3MSw2NDEyNzExNjJdfQ==
-->