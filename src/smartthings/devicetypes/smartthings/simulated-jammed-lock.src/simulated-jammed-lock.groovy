/**
 *  Copyright 2014 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Just enchanced version of Bob's Simulated Jammed Lock
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Simulated Jammable Lock", namespace: "smartthings/testing", author: "bob") {
		capability "Lock"
		capability "battery"
		command "jam"
		command "nullState"
	}

	// Simulated lock
	tiles {
		standardTile("toggle", "device.lock", width: 2, height: 2) {
			state "unknown", label:'unknown', action:"lock.unlock", icon:"st.unknown.unknown.unknown", backgroundColor:"#C1C1C1", defaultState: true
			state "unlocked", label:'unlocked', action:"lock.lock", icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff"
			state "locked", label:'locked', action:"lock.unlock", icon:"st.locks.lock.locked", backgroundColor:"#79b821"
			state "jammed", label:'jammed', action:"lock.unlock", icon:"st.unknown.unknown.unknown", backgroundColor:"#FFA81E"
		}
		standardTile("lock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'lock', action:"lock.lock", icon:"st.locks.lock.locked"
		}
		standardTile("unlock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'unlock', action:"lock.unlock", icon:"st.locks.lock.unlocked"
		}
		standardTile("jam", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'jam', action:"jam", icon:"st.unknown.unknown.unknown"
		}
		standardTile("null", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'null state', action:"nullState", icon:"st.unknown.unknown.unknown"
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:"%"
		}
		controlTile("batterySliderControl", "device.battery", "slider",
					height: 1, width: 2, range:"(1..100)") {
			state "battery", action:"setBatteryLevel"
		}

		main "toggle"
		details(["toggle", "lock", "unlock", "battery", "batterySliderControl", "jam", "null"])
	}
}

def updated() {
	log.trace "updated()"
	unlock()
	setBatteryLevel(94)
}

def parse(String description) {
	log.trace "parse $description"
	def pair = description.split(":")
	createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def lock() {
	log.trace "lock()"
	sendEvent(name: "lock", value: "locked")
}

def unlock() {
	log.trace "unlock()"
	sendEvent(name: "lock", value: "unlocked")
}

def jam() {
	log.trace "jam()"
	sendEvent(name: "lock", value: "jammed")
}

def nullState() {
	log.trace "nullState()"
	sendEvent(name: "lock", value: null)
}

def setBatteryLevel(Integer lvl) {
	log.trace "setBatteryLevel(level)"
	sendEvent(name: "battery", value: lvl)
}
