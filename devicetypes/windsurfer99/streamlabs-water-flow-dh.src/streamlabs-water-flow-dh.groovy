/**
 *  StreamLabs Water Flow DH
 *  Device Handler for StreamLabs Water Flow Meter: Cloud Connected Device
 *
 *  Copyright 2019 windsurfer99
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
metadata {
	definition (name: "StreamLabs Water Flow DH", namespace: "windsurfer99", author: "windsurfer99", cstHandler: true) {
		capability "Water Sensor"
        capability "Sensor"
        capability "Health Check"
        capability "refresh"
        command "changeToAway"
        command "changeToHome"
        attribute "todayFlow", "string"
        attribute "monthFlow", "string"
        attribute "yearFlow", "string"
        attribute "homeAway", "enum", ["home", "away"]
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2)  {
		standardTile("water", "device.water", width: 6, height: 4) {
			state "dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff"
			state "wet", icon:"st.alarm.water.wet", backgroundColor:"#00A0DC"
		}
		standardTile("refresh", "capability.refresh", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
			state "default", label:"Refresh", icon:"st.secondary.refresh", action:"refresh.refresh"
		}
		
		standardTile("homeAway", "device.homeAway", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
			state "home", icon:"st.nest.nest-home", defaultState:true, action:"changeToAway", backgroundColor:"#00A0DC"
			state "away", icon:"st.nest.nest-away", action:"changeToHome", backgroundColor:"#CCCCCC"
		}
		
		valueTile("todayFlow", "device.todayFlow", decoration: "flat", height: 2, width: 2) {
			state "todayFlow", label: 'Usage Today ${currentValue} Gal.'
		}

//		valueTile("todayFlow", "device.todayFlow", decoration: "flat", height: 2, width: 2) {
//			state "todayFlow", label: 'Usage Today\n\r ${currentValue} Gal.'
//		}

		valueTile("monthFlow", "device.monthFlow", decoration: "flat", height: 2, width: 2) {
			state "monthFlow", label: 'Usage this month ${currentValue} Gal.'
		}
 
		valueTile("yearFlow", "device.yearFlow", decoration: "flat", height: 2, width: 2) {
			state "yearFlow", label: 'Usage this year ${currentValue} Gal.'
		}

		main "water"
		details(["water", "homeAway", "refresh", "todayFlow", "monthFlow", "yearFlow"])
	}
}
//required implementations
def initialize(){
	state.init = true
}

// parse events into attributes; not really used with this type of Device Handler
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'water' attribute

}
//poll for changes; framework calls every 10 minutes
def poll() {
	log.debug "StreamLabs DH poll called"
	//sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "commandResponse")
}

def refresh(){
	def cloudData = parent.retrievecloudData() 
	log.debug "StreamLabs DH refresh- cloudData: ${cloudData}"
    state.todayFlow = cloudData.todayFlow
    state.thisMonthFlow = cloudData.thisMonthFlow
    state.thisYearFlow = cloudData.thisYearFlow
    state.homeAway = cloudData.homeAway
    state.wetDry = cloudData.inAlert ? "wet" : "dry"

	sendEvent(name: "todayFlow", value: Math.round(cloudData.todayFlow))
	sendEvent(name: "monthFlow", value: Math.round(cloudData.thisMonthFlow))
	sendEvent(name: "yearFlow", value: Math.round(cloudData.thisYearFlow))
	sendEvent(name: "water", value: state.wetDry)
	sendEvent(name: "homeAway", value: cloudData.homeAway)

	//sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "commandResponse")
	//runIn(2, getPower)
}

def uninstalled() {
	log.debug "StreamLabs DH uninstalled called for ${device.deviceNetworkId}"
    //delete me from parent Service Manager; this will leave in a strange state until parent recreates me
    if (state.init == true){
		parent.deleteSmartLabsDevice(device.deviceNetworkId)
    }
    state.init = false
}

//actions
//Tile action to change StreamLabs to home
def changeToHome() {
	log.debug "StreamLabs DH changeToHome called"
	parent.updateAway("home")
}

//Tile action to change StreamLabs to away
def changeToAway() {
	log.debug "StreamLabs DH changeToAway called"
	parent.updateAway("away")
}

//handle Events sent from Service Manager; typically wet & dry
def generateEvent(Map results) {
	log.debug "StreamLabs DH generateEvent parameters: '${results}'"
	sendEvent(results)
	return null
}