/**
 * Amazon Alexa
 *
 * Supports lights, dimmers and thermostats
 *
 * Author: SmartThings
 */
import groovy.transform.Field
import static groovy.json.JsonOutput.*


@Field String CUSTOM_SKILL_RESPONSE_FORMAT_VERSION = "1.0"

definition(
        name: "Amazon Alexa Kshuk (CoHo+Custom)",
        namespace: "smartthings",
        author: "SmartThings",
        description: "Used for development of Amazon Alexa - SmartThings integration",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-AmazonEcho.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-AmazonEcho@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-AmazonEcho@3x.png",
        oauth: [displayName: "Amazon Echo Dev (V2)", displayLink: ""]
)

// Version 1.2.0b build 20160617-00

// Changelist:
// 1.1.7
// Handle lastActivity == null for devices that actually support heart beat
// Reformatted code to tabs instead of spaces according to SmartThings standard

// 1.1.6
// Removed lastActivity = null offline check since it caused issues for virtual switches

// 1.1.5
// Fixed hub version parse bug

// 1.1.4
// NAC release candidate
// Cleaned up printtouts
// Added thermostats error message for unknown modes

// 1.1.3.5
// Fixed NAC 0%-turnOn bug
// Fixed thermostat Off error message

// 1.1.3.4
// Added Leviton Outlet

// 1.1.3.3
// Retry setup if failed
// Use delay for refresh() commands to distribute z-wave commands

// 1.1.3.2
// Added hub version check to block V1 hubs from using heartbeat

// 1.1.3.1
// First version to QA

// 1.1.3
// Add device health for "zwave switch" and "dimmer switch"
// Fixed NAC bug, if level is set to 0% and turn on is called then light should go to 100%

// 1.1.2
// Bugfixes - Increase/Decrease thermostat in auto mode temperature conversion
// Removed unnecessary log printouts

// 1.1.1 (first CoHo V2 release to production)
// Bugfixes

// 1.1.0
// Bugfixes

// 1.0.9
// Support for Amazon V2 connected home
// Added thermostat support
// Code cleanup
// Renamed endpoints to match Alexa APIs

// 1.0.8 (released)
// Disable heartbeat due to V2 local device type execution issue
// Catch exception in case "elevated priviliges" is removed

// 1.0.7 (released)
// Add level switches (dimming)

// 1.0.6
// Check device health for supported device (temporary fix using "hearbeat")
// Removed www.amazon.com from OAUTH

// 1.0.5
// Added friendly name
// Updated OAUTH url
// Updated text string when selecting devices

// Todos:
// Add support for manufacturerName/modelName
// Add support for appliance version
// Add support for capabilities in createFriendlyText()

preferences(oauthPage: "deviceAuthorization") {
    // Initial page where user can pick which switches should be available to the Echo
    // Assumption is that level switches all support regular switch as well. This is to avoid
    // having two inputs that might confuse the user
    page(name: "deviceAuthorization", title: "", nextPage: "instructionPage", uninstall: false) {
        section("Basic Skills") {
            paragraph "Alexa can control these devices directly:\n\n" +
                    "    \"Alexa, turn on my cat lamp\"\n" +
                    "    \"Alexa, set the temperature to 68 degrees\"\n\n"
            input "switches", "capability.switch", multiple: true, required: false, title: "My Switches"
            input "thermostats", "capability.thermostat", title: "My Thermostats", multiple: true, required: false
        }
        section("Advanced skills") {
            paragraph "You'll control the devices below little differently:\n\n" +
                    "You do not need to ask Alexa to discover these devices.\n" +
                    "They won't appear in the Smart Home section of your Alexa app.\n" +
                    "You have to ask SmartThings to control these devices. For example:\n" +
                    "   \"Alexa, tell SmartThings to lock my door\"\n" +
                    "   \"Alexa, ask SmartThings if my back door lock is locked\"\n\n" +
                    "Also, Alexa currently isn't allowed to unlock your door locks for you.\n"

            input "locks", "capability.lock", title: "My Door Locks", multiple: true, required: false
        }
        section("") {
            href(name: "href",
                    title: "Uninstall",
                    required: false,
                    description: "",
                    page: "uninstallPage")
        }
    }
    // Instructions for the user on how to run appliance discovery on Echo to update its device list
    page(name: "instructionPage", title: "Device Discovery", install: true) {
        section("") {
            paragraph "You have made a change to your device list.\n\n" +
                    "Now complete device discovery by saying the following to your Echo:"
            paragraph title: "\"Alexa, discover new devices\"", ""
        }
    }
    // Separate page for uninstalling, we dont want the user to accidentaly uninstall since the app can only be automatically reinstalled
    page(name: "uninstallPage", title: "Uninstall", uninstall: true, nextPage: "deviceAuthorization") {
        section("") {
            paragraph "If you uninstall this SmartApp, remember to unlink your SmartThings account from Echo:\n\n" +
                    "1. Open the Amazon Echo application\n" +
                    "2. Goto Settings > Connected Home > Device Links\n" +
                    "3. Choose \"Unlink from SmartThings\""
        }
    }
}

mappings {

    // handle custom skill
    path("/custom") {
        action:
        [
                GET:  "customGet",
                POST: "customPost"
        ]
    }

    // list all available devices
    path("/discovery") {
        action:
        [
                GET: "discovery"
        ]
    }

    // turn devices on an off
    path("/control/:id/:command") {
        action:
        [
                POST: "control"
        ]
    }
    // set devices to specific level
    path("/control/:id/:command/:value") { //
        action:
        [
                POST: "control"
        ]
    }
}

def installed() {
    log.debug settings
    initialize()
}

def updated() {
    log.debug settings
    initialize()
}

def initialize() {
    log.debug "initialize"

    unschedule()
    state.heartbeatDevices = [:]

    if (!checkIfV1Hub()) {
        // Refresh all z-wave devices to make sure we can identify them later
        refreshHeartbeatDevices()

        // settingUpAttempt handles retries of the heartbeat setup in case it fails for any reason
        // -1 is done, at attempt 5 it will give up
        state.settingUpAttempt = 0
        // Schedule setup of heartbeat monitor in 60 seconds to make sure previous call to refreshHeartbeatDevices() has completed
        runIn(60 * 1, setupHeartbeat)
    }
}

/**
 * Returns a list of all available devices, and each device's supported information.
 * Currently supports switches and thermostats.
 *
 * @return a list of available devices and each device's supported information
 */
def discovery() {
    def switchList = settings["switches"]?.collect { deviceItem(it) } ?: []
    def thermostatList = settings["thermostats"]?.collect { deviceItem(it) } ?: []
    def applianceList = switchList.plus thermostatList
    log.debug "discovery ${applianceList}"
    // Format according to Alexa API
    [discoveredAppliances: applianceList]
}

/////////////////////////////////////////////
/// Helpers to build Custom Skill responses
///
@Field Map customSkillSessionAttrs = [:]

Map buildBaseCustomSkillReponse() {
    Map customSkillResponseBase = [version: CUSTOM_SKILL_RESPONSE_FORMAT_VERSION] // FIXME - plumb this through better
    return customSkillResponseBase
}

Map buildSilentResponse() {
    Map customSkillResponse = buildBaseCustomSkillReponse()
    customSkillResponse.shouldEndSession = true
    return customSkillResponse
}

Map buildOutputSpeechObj(String sayText) {
    Map outputSpeechObj = [:]
    if (sayText.startsWith("<speak>") && sayText.endsWith("</speak>")) {
        outputSpeechObj.type = "SSML"
        outputSpeechObj.ssml = sayText
    } else {
        outputSpeechObj.type = "PlainText"
        outputSpeechObj.text = sayText
    }
    return outputSpeechObj
}

Map buildCustomSkillResponse(Map args) {
    Boolean shouldEndSessionArg = args?.shouldEndSession
    String sayText = args?.sayText
    String titleText = args?.titleText
    String cardText = args?.cardText
    String repromptText = args?.repromptText

    Map responseObj = [:]

    if (sayText) {
        Map outputSpeechObj = buildOutputSpeechObj(sayText)
        responseObj.outputSpeech = outputSpeechObj
    }

    if (titleText) {
        Map cardObj = [type: 'Simple', title: titleText, content: cardText?:sayText]
        responseObj.card = cardObj
    }

    Boolean shouldEndSessionDefault = true // default true

    // was a reprompt specified
    if (repromptText) {
        Map repromptSpeechObj = buildOutputSpeechObj(repromptText)
        responseObj.reprompt = [outputSpeech: repromptSpeechObj]
        shouldEndSessionDefault = false // default false if there's a reprompt
    }

    // explicit arg overrides defaults
    responseObj.shouldEndSession = shouldEndSessionArg?:shouldEndSessionDefault

    // now assemble the full custom skill response object
    Map customSkillResponse = buildBaseCustomSkillReponse()
    // If we have a session map to pass. add it if this response doesn't end the session
    if (transactionSessionAttributes && responseObj.shouldEndSession == false) {
        customSkillResponse.sessionAttributes = transactionSessionAttributes
    }
    customSkillResponse.response = responseObj

    return customSkillResponse
}

// Map buildCustomSkillResponse(String titleText, String sayText, String cardText=null, Boolean doesResponseEndSession=true) {
//     Map customSkillResponse = buildBaseCustomSkillReponse()
//     if (transactionSessionAttributes) {
//         customSkillResponse.transactionSessionAttributes = transactionSessionAttributes
//     }
//     Map outputSpeechObj = buildOutputSpeechObj(sayText)
//     Map cardObj = [type: 'Simple', title: titleText, content: cardText?:sayText]
//     Map responseObj = [outputSpeech: outputSpeechObj, card: cardObj, shouldEndSession: doesResponseEndSession]
//
//     customSkillResponse.response = responseObj
//     return customSkillResponse
// }
//
// Map buildCustomSkillResponse(String titleText, String sayText, String cardText=null, Map repromptOutputSpeech=[:]) {
//     Map customSkillResponse
//     if (repromptOutputSpeech) {
//         customSkillResponse = buildCustomSkillResponse(titleText, sayText, cardText, false)
//         customSkillResponse.reprompt = [outputSpeech: repromptOutputSpeech]
//     } else {
//         customSkillResponse = buildCustomSkillResponse(titleText, sayText, cardText)
//     }
//     return customSkillResponse
// }

Map buildErrorResponse(String errorMessage) {
    String title = "Skill Error"
    String say = "A skill error has occurred. Please look at the card in your Alexa app in order to report this error."
    String card = "To report this error, send an email to support@smartthings.com with the following information:\n$errorMessage"
    return buildCustomSkillResponse(titleText:title, sayText:say, cardText:card)
}

Map buildNoCardResponse(String sayText) {
    Map customSkillResponse = buildBaseCustomSkillReponse()
    customSkillResponse.outputSpeech = buildOutputSpeechObj(sayText)
    return customSkillResponse
}

String buildCommandCardTitle(String command, String device, String article='the') {
    String title = "$command $article $device"
    return title
}

Map buildCommandDeviceResponse(command, device, String outputText) {
    def titleText = "SmartThings, $command $device"
    return buildCustomSkillResponse(titleText:titleText, outputText:outputText)
}

Map buildResponseToQuestion(String questionText, String outputText) {
    def titleText = "SmartThings, $questionText"
    return buildCustomSkillResponse(titleText:titleText, outputText:outputText)
}

Map buildSuccessDeviceResponse(command, device, confirmationText='OK') {
    return buildCommandDeviceResponse(command, device, confirmationText)
}

def findDeviceById(String deviceId) {
    def device = transactionCandidateDevices?.find {
        log.debug device
        it.id == deviceId
    }
    return device
}


/**
 * handles custom skill GET requests
 * http request contains entire JSON message from AVS
 * @return returns a fully formed AVS Custom Skill Response object in JSON
 */
def customGet() {
    return launchCommandHandler()
}

@Field Boolean transactionIsNewSession = null
@Field String transactionIntentName = null
@Field String transactionStartIntent = null
@Field String transactionDeviceKind = null
@Field String transactionDeviceKindPlural = null
@Field List transactionCandidateDevices = []
@Field List transactionDevices = []
@Field Boolean transactionUsedAllDevicesSlot = false
@Field Map transactionSessionAttributes = [:]

/**
 * handles custom skill POST requests
 * http request contains entire JSON message from AVS
 * @return returns a fully formed AVS Custom Skill Response object in JSON
 */
def customPost() {
    Map responseToLambda = [:]
    // request.JSON
    log.debug "the JSON body of the request\n${prettyPrint(toJson(request.JSON))}"
    def customSkillReq = request?.JSON // preserve the request

    // Extract session data
    transactionIsNewSession = customSkillReq?.session?.new
    transactionSessionAttributes = customSkillReq?.session?.attributes?:[:] // get the session attrs


    def requestObj = customSkillReq?.request

    // what was the intended command?
    transactionIntentName = customSkillReq?.request?.intent?.name

    // simplify the slot map
    Map interpretedSlots = [:]
    def intentSlots = customSkillReq?.request?.intent?.slots

    customSkillReq?.request?.intent?.slots?.each {
        key, valmap ->
        // log.debug "slot key is $key, valmap is $valmap"
        if (valmap?.value != null) {
            interpretedSlots.put(key, valmap?.value)
        }
    }

    log.debug "intentName: $transactionIntentName; Interpreted Slots $interpretedSlots; isNewSession: $transactionIsNewSession"


    /*
    set if user said an utterance a slot which references all devices of a type:
    - AllLocks
    - AllLocks
    NOTE: Utterances whould never include both the inidivual device slot and all devices slot
    in the same utrterance,
*/


    if (transactionIsNewSession) {
        // specific to locks
        if (transactionIntentName.startsWith('Lock')) {
            transactionDeviceKind = 'lock'
            transactionDeviceKindPlural = 'locks'
            transactionCandidateDevices.addAll(locks?:[])

            if (transactionCandidateDevices.size() == 0) {
                // User has no matching devices
                log.debug "No devices having the $transactionDeviceKind capability are among the user's SmartThings devices"
                return buildCustomSkillResponse(titleText:"No $transactionDeviceKindPlural devices found", sayText:"Sorry, I couldn't find any $transactionDeviceKindPlural connected to your SmartThings setup.")
            } else if (interpretedSlots?.AllLocks != null || transactionCandidateDevices.size() == 1) {
                if (interpretedSlots?.AllLocks != null) {
                    // user has explicitly specified "all locks"
                    transactionUsedAllDevicesSlot = true
                }
                transactionDevices = transactionCandidateDevices
            } else if (interpretedSlots?.WhichLock != null) {
                // User has specified a specific lock
                log.debug "Evaluating each known device to see if it matches ${interpretedSlots?.WhichLock}"
                List deviceNameCompLog = []
                transactionCandidateDevices.each {
                    device ->
                    String debugLine = "device display name '${device.displayName.toLowerCase()}' == WhichLock slot value '${interpretedSlots.WhichLock.toLowerCase()}' ? "
                    if (device.displayName.toLowerCase().replaceAll(' ','') == interpretedSlots.WhichLock.toLowerCase().replaceAll(' ','')) {
                        transactionDevices.add(device)
                        debugLine += 'YES'
                    } else {
                        debugLine += 'NO'
                    }
                    deviceNameCompLog << debugLine
                }
                if (deviceNameCompLog) {
                    log.debug deviceNameCompLog.join('   \n')
                }
                if (transactionDevices == null || transactionDevices.size() == 0) {
                    if (transactionCandidateDevices.size() == 1) {
                        // If we have an ambiguous WhichLock slot and ony have one lock, then assume we mean that one
                        transactionDevices = transactionCandidateDevices
                    } else {
                        return buildCustomSkillResponse(titleText:"Ambiguous ${transactionDeviceKind} ${interpretedSlots.WhichLock}", sayText:"I don't know which ${transactionDeviceKind} you mean by ${interpretedSlots.WhichLock}.")
                    }
                }
                // devices should be guaranteed not null and to have length past here
            }
        }
    } else {
        transactionStartIntent = transactionSessionAttributes?.initialIntent
        if (transactionStartIntent.startsWith('Lock')) {
            transactionDeviceKind = 'lock'
            transactionDeviceKindPlural = 'locks'
            transactionCandidateDevices.addAll(locks?:[])
        }

        log.debug "Evaluating each known device to see if it matches an ID in ${transactionSessionAttributes.deviceIds}"
        transactionDevices = []
        transactionCandidateDevices.each {
            device ->
            if (transactionSessionAttributes.deviceIds.contains(device.id)) {
                transactionDevices << device
            }
        }
        transactionSessionAttributes.deviceIds = null
    }

    // Dispatch the intent to its handler command
    switch (transactionIntentName) {
        case { it == 'LockUnlockIntent' && transactionIsNewSession }:
            responseToLambda = unlockFailCommandHandler(transactionDevices[0]) // Only one lock may be passed to unlock
            break
        case {it == 'LockDialogIntent' && transactionUsedAllDevicesSlot && transactionIsNewSession}:
        case { it == 'LockLockIntent' && transactionIsNewSession }:
            responseToLambda = lockCommandHandler(transactionDevices)
            break
        case { it == 'LockStatusIntent' && transactionIsNewSession }:
            if (transactionDevices.size() == 1 && interpretedSlots?.LockState != null) {
                responseToLambda = lockQueryHandler(transactionDevices[0], interpretedSlots.LockState)
            } else if (transactionDevices.size() > 0) {
                // user wanted status of one device
                responseToLambda = lockStatusHandler(transactionDevices)
            } else {
                responseToLambda = lockStatusHandler(transactionCandidateDevices)
            }
            break
        case { it == 'LockSupportedIntent' && transactionIsNewSession }:
            responseToLambda = whichDevicesCommand(transactionDeviceKind, transactionDeviceKindPlural, transactionCandidateDevices)
            break
        case { it == 'LaunchIntent' && transactionIsNewSession }:
        case { it == 'LockDialogIntent' && transactionIsNewSession }:
            responseToLambda = launchCommandHandler()
            break
        case { it == 'AMAZON.HelpIntent' && transactionIsNewSession }:
            responseToLambda = helpCommandHandler()
            break
        case { it == 'AMAZON.YesIntent' && !transactionIsNewSession }:
            responseToLambda = yesNoDialogHandler(true)
            break
        case { it == 'AMAZON.NoIntent' && !transactionIsNewSession }:
            responseToLambda = yesNoDialogHandler(false)
            break
        case 'AMAZON.stopIntent':
        case 'AMAZON.cancelIntent':
            responseToLambda = stopCommandHandler()
            break
        default:
            log.warn 'could not determine which kind of device this command is for'
            responseToLambda = buildCustomSkillResponse(titleText:'SmartThings', sayText:"I'm not sure what you wanted me to do.")
            break
    }

    log.debug "Returning this to the Lambda function:\n${prettyPrint(toJson(responseToLambda))}"
    return responseToLambda
}


/**
 * Sends a command to a device
 *
 * Supported commands:
 *     -TurnOnRequest
 *     -TurnOffRequest
 *     -SetPercentageRequest  (level between 0-100)
 *     -IncrementPercentageRequest  (+level adjustment, an adjustment resulting in a level >100 will set level to 100)
 *  -DecrementPercentageRequest  (-level adjustment, an adjustment resulting in a level < 0 will turn off switch)
 *  -SetTargetTemperatureRequest (expects temp in celcius, within allowed bounds if thermostat supports min/max attributes)
 *  -DecrementTargetTemperatureRequest (expects temp in celcius, within allowed bounds if thermostat supports min/max attributes)
 *  -IncrementTargetTemperatureRequest (expects temp in celcius, within allowed bounds if thermostat supports min/max attributes)
 *
 *  return 200 and JSON body with appropriate Amazon COHO error if applicable
 */
def control() {
    def data = request.JSON

    // Collect all devices
    def devices = []
    for (d in settings) {
        if (d.value)
            devices.addAll(d.value)
    }

    def command = params.command
    def device = devices?.find {
        it.id == params.id
    }
    def response = [:]

    log.debug "control, params: ${params}, request: ${data}, devices: ${devices*.id} params.id: ${params?.id} params.command: ${params?.command} params.value: ${params?.value}"

    if (!command) {
        // TODO There might be a better error code for this in the future
        response << [error: "DriverInternalError", payload: [:]]
        log.error "Command missing"
    } else if (!device) {
        response << [error: "NoSuchTargetError", payload: [:]]
        log.error "Device ${params?.id} is not found"
    } else if (!checkDeviceOnLine(device)) {
        response << [error: "TargetOfflineError", payload: [:]]
        log.warn "$device is offline"
    } else {
        // Set if command is to increase or decrease a value
        def changeValue = 0;
        // Set to -1 if command is to decrease, 1 if increase
        def changeSign = 1;

        // Handle command
        switch (command) {
            case "TurnOnRequest":
                onOffCommand(device, true, response)
                break;
            case "TurnOffRequest":
                onOffCommand(device, false, response)
                break;

            case "DecrementPercentageRequest":
                changeSign = -1;
            case "IncrementPercentageRequest":
                changeValue = Float.parseFloat(params.value)
            case "SetPercentageRequest":
                setPercentageCommand(device, params.value, changeValue, changeSign, response)
                break;

            case "DecrementTargetTemperatureRequest":
                changeSign = -1
            case "IncrementTargetTemperatureRequest":
                changeValue = Float.parseFloat(params.value)
            case "SetTargetTemperatureRequest":
                setTemperatureCommand(device, params.value, changeValue, changeSign, response);
                break;

            default:
                // TODO There might be a better error code for this in the future
                log.error "$command is an unknown command"
                response << [error: "DriverInternalError", payload: [:]]
        }
    }
    return response
}

/**
 * Extract supported information for a device to return to the Echo
 *
 * @param it a device
 * @return a map with supported information about a device
 */
private deviceItem(it) {
    def actions = []
    if (it.hasCapability("Switch")) {
        actions.add "turnOn"
        actions.add "turnOff"
    }
    if (it.hasCapability("Switch Level")) {
        actions.add "incrementPercentage"
        actions.add "decrementPercentage"
        actions.add "setPercentage"
    }
    if (it.hasCapability("Thermostat")) {
        actions.add "incrementTargetTemperature"
        actions.add "decrementTargetTemperature"
        actions.add "setTargetTemperature"
    }
    // Format according to Alexa API
    it ? [applianceId: it.id, manufacturerName: "SmartThings", modelName: it.name, version: "V1.0", friendlyName: it.displayName, friendlyDescription: createFriendlyText(it), isReachable: checkDeviceOnLine(it), actions: actions] : null
}

/**
 * Determine based on device type name what category it falls under (light, outlet, thermostat or unknown=device).
 *
 * @param displayName a device type name
 * @return Light, Outlet, Thermostat, Switch or Device
 */
private createFriendlyText(device) {
    // Friendly name prefix = SmartThings:
    def result = "SmartThings: "

    if (device.hasCapability("Thermostat")) {
        result += "Thermostat"
    } else if (device.hasCapability("Switch")) {
        // Strings indicating a device type is a light, see createFriendlyText()
        // dimmer should be checked for capability later once Amazon supports dimmer, for now call it light
        def lightStrings = ["light", "led", "bulb", "dimmer", "lamp"]

        def nameLowerCase = device.name ? device.name.toLowerCase() : ""

        if (nameLowerCase.contains("outlet")) {
            result += "Outlet"
        } else if (lightStrings.any { nameLowerCase.contains(it) }) {
            result += "Light"
        } else if (nameLowerCase.contains("switch")) {
            result += "Switch"
        } else {
            result += "Device"
        }

        if (device.hasCapability("Switch Level")) {
            result += " (dimmable)"
        }
    } else if (device.hasCapability("Lock")) {
        result += "Lock"
    } else {
        result += "Unknown"
    }
    return result
}

/////////////// Temperature conversion ///////////////
def fToC(tempInF) {
    // T(°C) = (T(°F) - 32) × 5/9
    return ((tempInF - 32) * 5 / 9)
}

def cToF(tempInC) {
    // T(°F) = T(°C) × 9/5 + 32
    return tempInC * 9 / 5 + 32
}

def celsiusToLocationUnit(temp) {
    if (getTemperatureScale() == "F") {
        return (temp * 9 / 5 + 32) as Float
    }
    return temp
}

def toCelsius(temp) {
    if (getTemperatureScale() == "F") {
        return ((temp - 32) * 5 / 9) as Float
    }
    return temp
}

def toFahrenheit(temp) {
    if (getTemperatureScale() == "C") {
        return (temp * 9 / 5 + 32) as Float
    }
    return temp
}

/////////////// Commands ///////////////

/**
 * Runs when the skill is run with no additional information. Introduces the skill and provides
 * additional information via a home card
 * @return AVS response message
 */
def launchCommandHandler() {
    String titleText = 'SmartThings Advanced Skill'
    String sayText = 'This will briefly introduce the skill, maybe give a couple examples, and direct the user to the Alexa app to view the card with more information'
    String cardText = 'This text is for a more in-depth description of the skill. More usage examples can be shown here.\n' +
            '  - "Ask SmartThings to lock my front door lock"\n' +
            '  - "Ask SmartThings which locks you know about"\n' +
            '  - For more information, you can, "Ask SmartThings for help"\n' +
            '  - But remember, no hyperlinks\n' +
            '  - And the card must have less than 8000 characters\n'
    return buildCustomSkillResponse(titleText:titleText, sayText:sayText, cardText:cardText)
}

def stopCommandHandler() {
    return buildSilentResponse()
}

def helpCommandHandler() {
    String titleText = 'SmartThings Advanced Skill Help'


    String sayText = 'This is less introductory and more help-oriented. Maybe we put more examples are on the card?'
    String cardText = 'You can ask me to:\n' +
            '  - lock a specific lock or all your locks: "Ask SmartThings to lock all my locks"\n' +
            '  - If you only have one lock: "Ask SmartThings to lock my lock"\n' +
            '  - give you status of a specific lock or all your locks\n' +
            '  - Ask for the status of a specific lock\n' +
            '\n' +
            'Epilogue, but keep this whole thing to less than 8000 char'
    String repromptText = 'What would you like to do?'
    Map repromptObj = buildOutputSpeechObj(repromptText)
    return buildCustomSkillResponse(titleText:titleText, sayText:sayText, cardText:cardText)
}

def contextHelpHandler() {
    switch (transactionIntentName) {
        case "LockStatusIntent":
            sayText = "You can say 'yes,' or 'no,' or 'stop'"
            break
        default:
            break
    }
}

/**
 * unlockFailCommandHandler -
 * No mass unlock - just one lock at a time
 * @return AVS response indicating that unlocking is not supported.
 */
def unlockFailCommandHandler(def singleDevice) {
    log.warn "Unlock ${singleDevice.displayName} *** NOT PERMITTED"
    String targetDeviceName = singleDevice.displayName
    if (transactionUsedAllDevicesSlot) {
        targetDeviceName = 'all locks'
    }
    // TODO Needs copy
    sendNotificationEvent("For security reasons, you are not allowed to unlock doors via Alexa")
    return buildCommandDeviceResponse("unlock", targetDeviceName, "For security reasons, that feature has been disabled. For more information, please refer to the notifications page in your SmartThings mobile app")
}

@Field final List KNOWN_LOCK_STATES = ['locked', 'unlocked']
def lockCommandHandler(List deviceList) {
    log.trace "lockCommandHandler($deviceList)"
    String titleObject = "the ${deviceList[0]}"
    if (transactionUsedAllDevicesSlot == true) {
        titleObject = 'all locks'
    }

    List lockingDeviceDisplayNames = []
    List badStateDeviceDisplayNames = []
    List loopLogs = []
    deviceList.each {
        device ->
        String currentState = device?.currentValue('lock')
        if (KNOWN_LOCK_STATES.contains(currentState)) {
            // lock is in a known state

            String logLine = "Issuing lock command to ${device.displayName}"
            device.lock()
            if (currentState == 'locked') {
                logLine += " (we see ${device.displayName} as already in a locked state, but we issued another lock command anyway)"
            }
            lockingDeviceDisplayNames << device.displayName
            loopLogs << logLine
        } else {
            // lock is in an unknonwn state
            String warnMsg = "$device.displayName is in an unknown state: $currentState"
            loopLogs << "WARN: $warnMsg"
            log.warn warnMsg
            badStateDeviceDisplayNames << device.displayName
        }
    }
    log.info loopLogs.join('   \n')

    List responseSpeeches = []
    // prepare the response
    if (badStateDeviceDisplayNames.size() > 0) {
        String devicesInThisState = convoList(devicesByState.unknown)
        String theVerb = deviceVerb(badStateDeviceDisplayNames.size())
        String theIndicator = deviceIndicator(badStateDeviceDisplayNames.size())
        badStateSpeech = "The ${convoList(badStateDeviceDisplayNames)} $theVerb in an unknown state.\nPlease check $theIndicator and then try again."
        responseSpeeches << badStateSpeech
    }
    if (lockingDeviceDisplayNames.size() > 0) {
        String lockingSpeech = "Locking the ${convoList(lockingDeviceDisplayNames)}."
        responseSpeeches << lockingSpeech
    }

    String responseSpeech = responseSpeeches.join('\n')

    return buildSuccessDeviceResponse("lock", titleObject, responseSpeech)
}

def lockStatusHandler(List deviceList) {
    log.trace "lockStatusHandler($deviceList)"
    String statusTarget = "your locks"
    if (deviceList.size() == 1) {
        statusTarget = deviceList[0].displayName
    }
    List outputSpeeches = []
    // initialize the device state map for each state we care about
    Map devicesByState = [unknown:[]]
    KNOWN_LOCK_STATES.each {
        devicesByState[it] = []
    }
    deviceList.each {
        // FIXME - use unknown state code from lockCommandHandler here as well
        device ->
        String deviceCurrentState = device?.currentValue('lock')?:'unknown'
        if (KNOWN_LOCK_STATES.contains(deviceCurrentState)) {
            devicesByState[deviceCurrentState] << device.displayName
        } else {
            devicesByState.unknown << device.displayName
        }
        //outputText += "Your ${device.displayName} is ${device.currentValue('lock')}. \n"
    }
    if (devicesByState.unknown) {
        String devicesInThisState = convoList(devicesByState.unknown)
        String theVerb = deviceVerb(devicesByState.unknown.size())
        String theIndicator = deviceIndicator(devicesByState.unknown.size())
        outputSpeeches << "The $devicesInThisState $theVerb in an unknown state,  Please check $theIndicator and then try again."
    }
    KNOWN_LOCK_STATES.each {
        knownState ->
        if (devicesByState[knownState]) {
            String devicesInThisState = convoList(devicesByState[knownState])
            String theVerb = deviceVerb(devicesByState[knownState].size())
            String theIndicator = deviceIndicator(devicesByState[knownState].size())
            outputSpeeches << "The $devicesInThisState $theVerb $knownState."
        }
    }
    return buildCommandDeviceResponse("what is the status of the", statusTarget, outputSpeeches.join('\n'))
}

/**
 * status of a single lock asked about by name and state
 * Ask smart things if ny front door lock is locked
 * @param   device  the lock being queried
 * @param   queryState  the state the user asked if the lock was in
 * @return  AVS Custom Skill response formatted map (will be converted to JSON)
 */
def lockQueryHandler(def singleDevice, String queryState) {
    log.trace "lockQueryHandler(${singleDevice.displayName}, $queryState)"
    Set lockCapabilityStates = ['locked', 'unlocked']
    Set lockedSynonyms = ['locked', 'closed', 'shut']
    Set unlockedSynonyms = ['unlocked', 'opened', 'open']

    String normalQueryState = 'locked'
    if (unlockedSynonyms.contains(queryState.toLowerCase())) {
        normalQueryState = 'unlocked'
    } else if (!lockedSynonyms.contains(queryState.toLowerCase())) {
        // canonicalLockState is still locked
        log.warn "We don't know about the lock state queried: $queryState. Assuming it means 'locked'"
    }

    def deviceCurrentState = singleDevice?.currentValue('lock')?.toLowerCase()?:'unknown'

    String outputText = ""
    if (normalQueryState == deviceCurrentState.toLowerCase()) {
        // Yes!
        outputText = "Yes, your ${singleDevice.displayName} is $normalQueryState."
    } else if (!KNOWN_LOCK_STATES.contains(deviceCurrentState.toLowerCase())) {
        // Uh oh
        outputText = "Your ${singleDevice.displayName} is in an unknown state. Please check the lock and then try again."
    } else {
        // No
        outputText = "No, your ${singleDevice.displayName} is ${deviceCurrentState}."
    }

    // If unlocked, follow up and ask if they'd like to lock it
    String repromptText = ""
    if (deviceCurrentState == 'unlocked') {
        // build up a session for the next stage of the dialog
        transactionSessionAttributes.deviceIds = [singleDevice.id]
        transactionSessionAttributes.deviceId  = singleDevice.id
        transactionSessionAttributes.initialIntent = transactionIntentName
        transactionSessionAttributes.validResponseIntents = ['AMAZON.YesIntent', 'AMAZON.NoIntent']
        transactionSessionAttributes.nextHandler = 'doLockDialogHandler'
        repromptText = "Would you like me to lock the ${singleDevice.displayName} for you?"
        transactionSessionAttributes.posedQuestion = repromptText
        outputText += " Would you like me to lock it for you?"
    }

    return buildCustomSkillResponse(titleText:"Is my ${singleDevice.displayName} $queryState?", sayText:outputText, repromptText:repromptText)
}

/**
 * The user asked which devices Alexa knows about. Which class of devices depends on the intent the user issued
 * @param   transactionDeviceKind The type of device, singular
 * @param   transactionDeviceKindPlural The type of device, plural
 * @param   [description]
 * @return  [description]
 */
def whichDevicesCommand(String transactionDeviceKind=device, String transactionDeviceKindPlural, List deviceList) {
    if (!transactionDeviceKind) transactionDeviceKind = 'device'
    if (!transactionDeviceKindPlural) transactionDeviceKindPlural = 'devices'
    String devicesOutput = ""
    if (deviceList && deviceList.size() == 1 ) {
        devicesOutput = "I know about one $transactionDeviceKind, ${deviceList[0].displayName}."
    } else if (deviceList && deviceList.size() > 1 ) {
        devicesOutput =  "The $transactionDeviceKindPlural that I can operate are: \n"
        Integer ctr = 1
        List deviceNames = []
        deviceList.each {
            device ->
            deviceNames << device.displayName
        }
        devicesOutput += convoList(deviceNames)
    } else {
        devicesOutput = "I don't know about any $transactionDeviceKindPlural."
    }
    return buildCommandDeviceResponse("Which $transactionDeviceKindPlural can I control?", devicesOutput)
}

Map yesNoDialogHandler(Boolean isResponseYes) {
    if (transactionIsNewSession) {
        // Yes or No aren't valid for a new session
        return buildNoCardResponse('I\'m not sure what you mean by that right now')
    } else if (!isIntentValid()) {
        // Yes an No are only valid responses if the session indicates that it was an appropriate response
        badUtteranceResponse = "That doesn't make sense to me right now."
        if (transactionSessionAttributes?.posedQuestion) {
            badUtteranceResponse += posedQuestion
        }
        return buildNoCardResponse(badUtteranceResponse)
    } else if (!transactionSessionAttributes?.nextHandler) {
        return buildErrorResponse("Session is missing nextHandler for $transactionIntentName")
    }
    // now handle the yes/no response
    return "${transactionSessionAttributes?.nextHandler}"(isResponseYes)
}

Map doLockDialogHandler(Boolean isResponseYes) {
    Map responseObject
    if (isResponseYes) {
        responseObject = lockCommandHandler(transactionDevices)
    } else {
        // The answer was no
        responseObject = buildNoCardResponse("OK");
    }
    return responseObject
}

Boolean isIntentValid() {
    if (transactionIntentName == null || transactionSessionAttributes?.validResponseIntents == null)
        return true
    else if (transactionIntentName && transactionSessionAttributes?.validResponseIntents &&
        transactionSessionAttributes.validResponseIntents.contains(transactionIntentName)) {
        return true
    }
    return false
}

///////////////////////////////////////////////////////////////////////////////
/// Message building utilities
///
String pluralizer(Integer howMany, String singularWord, String pluralWord) {
    String useWord = pluralWord
    if (howMany == 1) {
        useWord = singularWord
    }
    return useWord
}

String deviceVerb(Integer howMany, String singularVerb='is', String pluralVerb='are') {
    return pluralizer(howMany, singularVerb, pluralVerb)
}

String deviceIndicator(Integer howMany, String singularInd="this $transactionDeviceKind", String pluralInd="these $transactionDeviceKindPlural") {
    return pluralizer(howMany, singularInd, pluralInd)
}

String convoList(List listOfStrings, String conjunction="and") {
    log.trace "convoList($listOfStrings, $conjunction)"
    String conversationalList = ""
    Integer numStringsToJoin = listOfStrings.size()
    Integer ctr = numStringsToJoin
    listOfStrings.each {
        element ->
        if (numStringsToJoin == 1 && ctr == 1) {
            conversationalList += "$element"
        } else if (ctr == 1) {
            conversationalList += " $conjunction $element"
        } else {
            conversationalList += " $element,"
            if (numStringsToJoin > 2) {
                // No comma for "this and that"
                conversationalList += ","
            }
        }
        ctr--
    }
    return conversationalList
}


/**
 * Turn on or off a device
 *
 * @param device a device to update
 * @param turnOn true if device should be turned on, false otherwise
 * @param response the response to modify according to result
 */
def onOffCommand(device, turnOn, response) {
    if (turnOn) {
        log.debug "Turn on $device"

        if (device.currentSwitch == "on") {
            // Call on() anyways just in case platform is out of sync and currentLevel is wrong
            response << [error: "AlreadySetToTargetError", payload: [:]]
        }
        device.on()
    } else {
        log.debug "Turn off $device"
        if (device.currentSwitch == "off") {
            // Call off() anyways just in case platform is out of sync and currentLevel is wrong
            response << [error: "AlreadySetToTargetError", payload: [:]]
        }
        device.off()
    }
}

/**
 * Set a level switch to a specific level or increase/decrease it
 *
 * @param device a device to update
 * @param value the new level to set to (if changeValue == 0, otherwise ignored)
 * @param changeValue the amount to increase or decrease the current level with
 * @param changeSign -1 if a level decrease, 1 if a level increase
 * @param response the response to modify according to result
 */
def setPercentageCommand(device, value, changeValue, changeSign, response) {
    Float newLevel = Float.parseFloat(value)

    if (changeValue != 0) {
        log.debug "Change $device level with ${changeValue * changeSign}"
    } else {
        log.debug "Set $device level to $newLevel"
    }

    if (!device.hasCapability("Switch Level")) {
        response << [error: DriverInternalError, payload: [:]]
        log.error "$device does not support this command"
    } else if (changeValue != 0) {
        newLevel = device.currentLevel
        // If device is off, brighten should start from 0 and not current value
        if (device.currentValue("switch") == "off") {
            newLevel = 0
        }
        newLevel += (changeValue * changeSign)

        if (newLevel > 100) {
            newLevel = 100
        } else if (newLevel < 0) {
            newLevel = 0
        }
        log.debug "Change $device to $newLevel (diff=${changeValue * changeSign}, currentValue=$device.currentLevel)"

        if (device.currentLevel == newLevel) {
            // Call setLevel anyways just in case platform is out of sync and currentLevel is wrong
            response << [error: "AlreadySetToTargetError", payload: [:]]
        }
        device.setLevel(newLevel)
    } else if (newLevel < 0 || newLevel > 100) {
        response << [error: "ValueOutOfRangeError", payload: [minimumValue: 0, maximumValue: 100]]
        log.error "Level $newLevel is outside of allowed range, (0-100)"
    } else {
        if (device.currentLevel == newLevel) {
            // Call setLevel anyways just in case platform is out of sync and currentLevel is wrong
            response << [error: "AlreadySetToTargetError", payload: [:]]
        }
        device.setLevel(newLevel)
    }
}
/**
 * Check if a temperature is within a thermostats allowed range
 *
 * @param device a device to check
 * @param temperature the requested temperature to check
 * @param response the response to modify according to result
 *
 * @result true if temperature is acceptable or thermostat does not provide min/max values
 */
def isTemperatureWithinRange(device, Float temperature, response) {
    Float min = null
    Float max = null
    Float minLimitF = 45
    Float maxLimitF = 90
    Float minLimitC = 8
    Float maxLimitC = 32

    switch (device.currentThermostatMode) {
        case "emergency heat":
        case "heat":
            if (device.currentMinHeatingSetpoint != null && device.currentMaxHeatingSetpoint != null) {
                min = device.currentMinHeatingSetpoint.floatValue()
                max = device.currentMaxHeatingSetpoint.floatValue()
            } else if (getTemperatureScale() == "F") {
                min = minLimitF
                max = maxLimitF
            } else {
                min = minLimitC
                max = maxLimitC
            }
            break;
        case "cool":
            if (device.currentMinCoolingSetpoint != null && device.currentMaxCoolingSetpoint != null) {
                min = device.currentMinCoolingSetpoint.floatValue()
                max = device.currentMaxCoolingSetpoint.floatValue()
            } else if (getTemperatureScale() == "F") {
                min = minLimitF
                max = maxLimitF
            } else {
                min = minLimitC
                max = maxLimitC
            }
            break;
        case "auto":
            if (device.currentMinCoolingSetpoint != null && device.currentMaxHeatingSetpoint != null) {
                min = device.currentMinCoolingSetpoint.floatValue()
                max = device.currentMaxHeatingSetpoint.floatValue()
            } else if (getTemperatureScale() == "F") {
                min = minLimitF
                max = maxLimitF
            } else {
                min = minLimitC
                max = maxLimitC
            }
            break;
    }

    if (min != max && (temperature < min || temperature > max)) {
        response << [error: "ValueOutOfRangeError", payload: [minimumValue: toCelsius(min), maximumValue: toCelsius(max)]]
        log.error "Temperature $temperature is outside of allowed range, ($min-$max)"
        return false
    }
    return true
}

/**
 * Set a thermostat to a specific temperature or increase/decrease it
 *
 * @param device a device to update
 * @param value the new temperature to set to in celsius (if changeValue == 0, otherwise ignored)
 * @param changeValue the temperature to increase or decrease the current level with in celsius
 * @param changeSign -1 if a temperature decrease, 1 if a temperature increase
 * @param response the response to modify according to result
 */
def setTemperatureCommand(device, value, changeValue, changeSign, response) {
    Float newTemp = Float.parseFloat(value)
    if (changeValue != 0) {
        log.debug "Change $device temperature with ${changeValue * changeSign}"
    } else {
        log.debug "Set $device temperature to $newTemp"
    }

    if (!device.hasCapability("Thermostat")) {
        response << [error: "DriverInternalError", payload: [:]]
        log.error "$device does not support this command"
    } else {
        log.debug "Mode: $device.currentThermostatMode TemperatureScale: ${getTemperatureScale()}"

        switch (device.currentThermostatMode) {
            case "emergency heat":
            case "heat":
                log.debug "currentHeatingSetpoint: $device.currentHeatingSetpoint currentThermostatSetpoint: $device.currentThermostatSetpoint"
                if (changeValue != 0) {
                    if (getTemperatureScale() == "F")
                        newTemp = cToF(fToC(device.currentHeatingSetpoint) + (changeValue * changeSign))
                    else
                        newTemp = device.currentHeatingSetpoint + (changeValue * changeSign)
                } else if (getTemperatureScale() == "F") {
                    newTemp = cToF(newTemp)
                }
                newTemp = newTemp.round()

                if (newTemp == device.currentHeatingSetpoint) {
                    response << [error: "AlreadySetToTargetError", payload: [:]]
                } else if (isTemperatureWithinRange(device, newTemp, response)) {
                    log.debug "set to $newTemp (heat)"
                    newTemp = newTemp.round()
                    response << [targetTemperature: [value: "${toCelsius(newTemp)}"], temperatureMode: [value: "Heating"]]
                    device.setHeatingSetpoint(newTemp)
                }
                break;
            case "cool":
                log.debug "currentCoolingSetpoint: $device.currentCoolingSetpoint currentThermostatSetpoint: $device.currentThermostatSetpoint"
                if (changeValue != 0) {
                    if (getTemperatureScale() == "F")
                        newTemp = cToF(fToC(device.currentCoolingSetpoint) + (changeValue * changeSign))
                    else
                        newTemp = device.currentCoolingSetpoint + (changeValue * changeSign)
                } else if (getTemperatureScale() == "F") {
                    newTemp = cToF(newTemp)
                }
                newTemp = newTemp.round()

                if (newTemp == device.currentCoolingSetpoint) {
                    response << [error: "AlreadySetToTargetError", payload: [:]]
                } else if (isTemperatureWithinRange(device, newTemp, response)) {
                    log.debug "set to $newTemp (cool)"
                    response << [targetTemperature: [value: "${toCelsius(newTemp)}"], temperatureMode: [value: "Cooling"]]
                    device.setCoolingSetpoint(newTemp)
                }
                break;
            case "auto":
                log.debug "currentHeatingSetpoint: $device.currentHeatingSetpoint currentCoolingSetpoint: $device.currentCoolingSetpoint currentThermostatSetpoint: $device.currentThermostatSetpoint"
                if (changeValue != 0) {
                    if (getTemperatureScale() == "F")
                        newTemp = cToF(fToC(device.currentThermostatSetpoint) + (changeValue * changeSign))
                    else
                        newTemp = device.currentThermostatSetpoint + (changeValue * changeSign)
                } else if (getTemperatureScale() == "F") {
                    newTemp = cToF(newTemp)
                }
                newTemp = newTemp.round()

                log.debug "set to $newTemp (auto)"
                if (newTemp == device.currentThermostatSetpoint) {
                    response << [error: "AlreadySetToTargetError", payload: [:]]
                } else if (isTemperatureWithinRange(device, newTemp, response)) {
                    response << [targetTemperature: [value: "${toCelsius(newTemp)}"], temperatureMode: [value: "Auto"]]
                    device.setHeatingSetpoint(newTemp)
                    device.setCoolingSetpoint(newTemp)
                }
                break;
            case "off":
                response << [error: "UnwillingToSetValueError", payload: [errorInfo: [code: "ThermostatIsOff", description: "Thermostat is in Off mode"]]]
                log.warn "$device is Off"
                break;
            default:
                response << [error: "NotSupportedInCurrentModeError", payload: [currentDeviceMode: "OTHER"]]
                log.error "Unsupported thermostat mode"
        }
    }
}

/**
 * Sends a refresh() command to all devices with device type Z-Wave Switch and Dimmer Switch. This will force them
 * to populate the MSR and manufacturer data fields used by this app to identify the brand and model number of each Z-Wave device.
 */
private refreshHeartbeatDevices() {
    // Used to wait one second between polls, because issuing Z-wave commands to offline devices can cause trouble if done to fast
    def delayCounter = 0
    switches?.each {
        switch (it.getTypeName()) {
            case "Z-Wave Switch":
            case "Dimmer Switch":
                // refresh to populate MSR data as well as verifying online status
                if (it.hasCapability("Refresh")) {
                    it.refresh([delay: delayCounter++ * 1000])
                }
                break
        }
    }
}

/**
 * Setup heartbeat service that will periodically poll heartbeat supported devices that have not been polled or checked in
 * in a timely manner.
 */
def setupHeartbeat() {
    if (state.settingUpAttempt == -1) {
        return
    } else if (state.settingUpAttempt < 5) {
        // Schedule a check in 2 min that setupHeartbeat() actually completed ok
        // There are instances when SmartApp execution takes to long and time outs
        // and that would prevent the heartbeat loop from ever starting
        state.settingUpAttempt = state.settingUpAttempt + 1
        runIn(2 * 60, setupHeartbeat)
    } else {
        log.error "setupHeartbeat failed"
        return
    }

    // Devices are checked every 30 min, and offline devices are polled every 2h
    // (4 cycles of 30 min each, offline refresh() is done on cycle 0)
    state.heartbeatPollCycle = 0

    // Setup device health poll, store a list of device ids and online status for all supported device type handlers
    switches?.each {
        def timeout = getDeviceHeartbeatTimeout(it)
        if (timeout > 0) {
            state.heartbeatDevices[it.id] = [online: true, timeout: timeout]
            // , label: it.label ?: it.name (useful for debugging)
        }
    }

    if (state.heartbeatDevices != null && !state.heartbeatDevices.isEmpty()) {
        // TODO this should ideally happen immediately the first time
        runEvery30Minutes(deviceHeartbeatCheck)
    }
    // Setup succeeded
    state.settingUpAttempt = -1
}

/**
 * Every 30 min cycle Amazon Echo SmartApp will for each heartbeat device:
 * 1. If device has been heard from in the last 25 min, mark as Online (it must have been polled by hub (every 15 min) or checked in (every 10 min))
 * 2. If device has been heard from in the last 25-35 min, mark as Online and send a refresh() (it was probably polled by Echo SmartApp so poll again
 *    to maintain device online, 30 min +-5 to account for delays in scheduler)
 * 3. if device was not heard from in the last 35 min but was heard from last cycle, mark it as Offline and send a refresh() in case status is wrong
 * 4. If devices was Offline last cycle, then try a refresh() every fourth cycle (i.e. 2h) for all devices marked as Offline
 */
def deviceHeartbeatCheck() {
    if (state.heartbeatDevices == null || state.heartbeatDevices.isEmpty()) {
        // This should not be happening unless state was damaged
        log.warn "No heartbeat devices available, will stop checking"
        unschedule()
        return
    }

    Calendar time25 = Calendar.getInstance()
    time25.add(Calendar.MINUTE, -25)

    Calendar time35 = Calendar.getInstance()
    time35.add(Calendar.MINUTE, -35)

    // Only poll offline devices every 2h
    if (state.heartbeatPollCycle == 0) {
        log.debug "Polling Offline devices"
    }

    // Used to delay one second between polls, because issuing Z-wave commands to offline devices too fast can cause trouble
    def delayCounter = 0

    switches?.each {
        int deviceTimeout = getDeviceHeartbeatTimeout(it)
        if (deviceTimeout > 0 && it.getLastActivity() != null) {
            Date deviceLastChecking = new Date(it.getLastActivity()?.getTime())
            if (deviceLastChecking?.after(time25.getTime())) {
                state.heartbeatDevices[it.id]?.online = true
            } else if (deviceLastChecking?.after(time35.getTime())) {
                state.heartbeatDevices[it.id]?.online = true
                if (it.hasCapability("Refresh")) {
                    it.refresh([delay: delayCounter++ * 1000])
                    log.debug "refreshing $it (regular poll)"
                }
            } else {
                // Device did not report in, mark as offline and if cycle 0 or it was previously online, then issue a refresh() command
                def previousStatus = state.heartbeatDevices[it.id]?.online
                state.heartbeatDevices[it.id]?.online = false
                if ((previousStatus || state.heartbeatPollCycle == 0) && it.hasCapability("Refresh")) {
                    it.refresh([delay: delayCounter++ * 1000])
                    log.debug "refreshing $it (one time or first cycle) ($delayCounter)"
                }
            }
        }
    }
    // Update cycle number
    state.heartbeatPollCycle = (state.heartbeatPollCycle + 1) % 4
}

/**
 * Check if device is a supported heartbeat device. If it is, will return the maximum time interval that the devices is expected to be heard from within.
 *
 * @param device device to get heartbeat timeout for
 * @return number of minutes after last activity that the device should be considered offline for, or 0 if no support for heartbeat
 */
private getDeviceHeartbeatTimeout(device) {
    def timeout = 0

    switch (device.getTypeName()) {
        case "SmartPower Outlet":
            timeout = 35
            break
        case "Z-Wave Switch":
        case "Dimmer Switch":
            if (device.getData()?.MSR != null) {
                switch (device.getData()?.MSR) {
                    case "001D-1B03-0334":  // ZWAVE In-Wall Switch (dimmable) (DZMX1-1LZ)
                    case "001D-1C02-0334":  // ZWAVE Leviton In-Wall Switch (non-dimmable) (DZS15-1LZ)
                    case "001D-1D04-0334":  // ZWAVE Leviton Receptacle (DZR15-1LZ)
                    case "001D-1A02-0334":  // ZWAVE Plug in Appliance Module (Non-Dimmable) (DZPA1-1LW)
                    case "001D-1902-0334":  // ZWAVE Plug in Lamp Dimmer Module (DZPD3-1LW)
                        timeout = 60
                        break
                }
            }
            break
    }

    // Check DTHs with ambiguous names in type name
    if (timeout == 0) {
        switch (device.name) {
            case "OSRAM LIGHTIFY LED Tunable White 60W":
                timeout = 35
                break
        }

    }
    return timeout
}

/**
 * Determine if a device is online, temporary solution is to check only a restricted number of supported devices
 * for lastActivity time. If timestamp has been updated the last X min (depending on DTH expectations), then the device is
 * considered online.
 *
 * If device is found to be offline, report that back to Echo but issue a refresh() command in case device is actually
 * online. If status is indeed online, then if user tries again after a few seconds it will work.
 *
 * @param device
 * @return true if device is considered online or device heartbeat is not supported
 */
private checkDeviceOnLine(device) {
    // Default is true for all devices that don't support heartbeat
    boolean result = true

    // timeout interval in minutes (after this time device is considered offline)
    // if 0 then device type is not supported and online status will default to true
    def timeout = getDeviceHeartbeatTimeout(device)
    if (timeout != 0) {
        if (device.getLastActivity() == null) {
            // getLastActivity() == null means platform has not seen any activity for a long time and erased the field
            result = false
        } else {
        Calendar c = Calendar.getInstance()
        c.add(Calendar.MINUTE, -timeout)
        Date deviceLastChecking = new Date(device.getLastActivity()?.getTime())
        if (!deviceLastChecking.after(c.getTime())) {
            // No heartbeat found but expected
            // Send refresh in case device is actually online but just missed last poll
            if (device.hasCapability("Refresh")) {
                device.refresh()
            }
            result = false
            }
        }
    }

    return result
}

/**
 * Checks if the current hub is V1 which we do not support heartbeat for
 * @return true if V1, false if newer model like V2 or TV
 */
private checkIfV1Hub() {
    boolean v1Found = false
    location.hubs.each {
        if ("$it.type".toUpperCase() == "PHYSICAL") {
            try {
                def id = Integer.parseInt(it.hub?.hardwareID, 16)
                if ((id >= 1 && id <= 5)) { // 1-5 are all V1 hubs
                    v1Found = true
                }
            } catch (NumberFormatException e) {
                // Something went wrong with parsing, assume not V1 hub since we know that is a numeric value from 1-5
            }
        }
    }
    return v1Found
}
