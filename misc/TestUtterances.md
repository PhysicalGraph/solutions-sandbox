# Test Utterances to try in the service simulator

## LaunchIntent (4a. Alexa Start)
* [x] Alexa, ask smart things
* [x] Alexa, launch smart things

## LockSupportedIntent (no story)
* [x] Alexa, ask smart things which locks it knows about (one lock)
* [x] Alexa, ask smart things what doors do you know (multiple locks)
* [x] Alexa, ask smart things what locks do you know (no locks)

## LockLockIntent (4b. Single Door Lock and 4c. Multiple Door Lock)
* Alexa, ask smart things to lock the entry door (no door exists)
* Alexa, ask smart things to lock the entry door (locked)
* Alexa, ask smart things to lock the unknown door (only one door but wrong name)
* Alexa, ask smart things to lock the door (only one door but unspecified name)
* Alexa, ask smart things lock entry door (unlocked)
* Alexa, ask Smart things to lock all doors (all doors unlocked)
* Alexa, ask Smart things lock my doors (some doors unlocked)
* Alexa, ask Smart things lock the door (multiple locks), clarification "Which door do you mean?"

### special cases
* Door lock issue (4d.)
 * Alexa, ask Smart things to lock __<physical lock name>__ (one lock configured, it is jammed, called by name)
 * Alexa, ask smart things to lock all locks (_Setup: cause jam one lock. Set other locks to unlocked_)
 * Alexa, ask smart things to lock all locks (_Setup: only one lock configured and jammed_)
 * Alexa, ask smart things to lock all locks (_Setup: multiple jammed, but not all_)
 * Alexa, ask smart things to lock all locks (_Setup: multiple locks, all jammed_)

* With only one lock configured (4bi.)
 * [x] Alexa, ask Smart things to lock the mumblefoo lock

## LockUnlockIntent (4e. Unlock Fail)
* [x] Alexa, ask smart things to unlock the entry door (locked)
* [x] Alexa, ask smart things unlock entry door (unlocked)
* [x] Alexa, ask smart things to unlock the entry door (jammed)
* [x] Alexa, ask Smart things to unlock all doors (all doors unlocked)
* [x] Alexa, ask Smart things unlock my doors (some doors unlocked)
* [x] Alexa, ask smart things to unlock the door (no doors exists)

## LockStatusIntent (4f. Lock Status)
### for all locks
* [x] Alexa, ask smart things are my doors locked (all unlocked)
* [x] Alexa, tell smart things to check my doors (some locked)
* [x] Alexa, ask smart things about the status of my doors (all locked)
* [x] Alexa, ask smart things are my doors locked (no doors)

###for a single, named lock
* [x] Alexa, ask smartthings if my front door is locked (locked)
* [x] Alexa, ask smartthings if my front door is locked (unlocked), clarification "Should I lock it?"
* [x] Alexa, ask smartthings if my front door is locked (jammed)
 * java.lang.NullPointerException: Cannot invoke method toLowerCase() on null object
	at org.kohsuke.groovy.sandbox.impl.Checker$1.call(Checker.java:66)
 * line 767 of smartapp

* [x] Alexa, ask smartthings if my front door is unlocked (locked)
 * [ ]Clarification, "Should I lock it?"
* [x] Alexa, ask smartthings if my front door is unlocked (unlocked)
* [x] Alexa, ask smartthings if my front door is unlocked (jammed)
* [x] Alexa, ask smartthings to tell me about front door (locked)
* [x] Alexa, ask smartthings to tell me about front door (unlocked),
 * [ ] clarification "Should I lock it?"
* [x] Alexa, ask smartthings to tell me about front door (jammed)
* [x] Alexa, ask smartthings to tell me about the door (only one door)
* [ ] Alexa, ask smartthings if my door is unlocked (multiple locks),
 * [ ] more than one lock - clarification "Which door do you mean?"
 * [x] for a single lock, unnamed lock - give status with name
* [x] Alexa, ask smartthings if my door is locked (no doors)
* [x] Alexa, ask smartthings if my door is locked (only one door)
* [x] Alexa, ask smartthings if my unknown door is locked (only one door but wrong name)


## DoorStatusIntent (4g. Door Status)
**NOT YET IMPLEMENTED**
for all doors
* Alexa, ask smart things are my doors closed (all closed)
* Alexa, ask smart things are my doors open (some closed)
* Alexa, tell smart things to check my doors (some closed)
* Alexa, ask smart things about the status of my doors (all open)
* Alexa, ask smart things about the status of my doors (no doors)

## DoorQueryIntent (4g. Door Status)
**NOT YET IMPLEMENTED**
for a single, named door
* Alexa, ask smart things if my front door is open (door is open)
* Alexa, ask smart things if my front door is open (door is closed)
* Alexa, ask smart things if my front door is closed (door is open)
* Alexa, ask smart things if my front door is closed (door is closed)
* Alexa, ask smartthings if my door is open (no doors)
* Alexa, ask smartthings if my door is closed (only one door)
* Alexa, ask smartthings if my unknown door is closed (only one door but wrong name)


## BatteryStatusIntent (4h. Battery Status)
**NOT YET IMPLEMENTED**
*  Alexa ask SmartThings for the battery status of the entry lock
*  Alexa ask SmartThings for the battery status of the entry door


## HelpIntent (4i. Help intent)
* [x] Alexa, ask smart things for help - **FAIL - no reprompt, should end session**
* [x] Alexa, tell smart things to help me
