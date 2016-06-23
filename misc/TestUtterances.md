# Test Utterances to try in the service simulator
Based on the user stories described in confluence: https://smartthings.atlassian.net/wiki/display/AZ/Amazon+Locks+Custom+Skill

## Setup

### Devices
* multiple physical or simulated locks, named with standard names
 * __Test Lock__
 * __Front Lock__
 * __Back Lock__
 * __Entry Lock__

* Special case No. 1
 * __Jammed Lock__
   * physical deadbolt which can be put into a jammed or unknown state and reflects this in the DTH
   * simulated lock which can be set to a jammed state 'jammed'

* Special case No. 2
 * __Null Lock__
   * simulated lock DTH with lock attribute set to a null state to test SA robustness

* Special case No. 3
 * __Unknown Lock__
   * No actual device configured a.k.a. it doesn't exist

For simulated devices, the Simulated Jammable Lock DTH included in this repository will help as it can easily be set to the necessary states

Watch word: the simulator is fine with Alexa regardless of your config in the Alexa app.
\s{INVOCATION}\s - the invocation phrase of your skill (e.g., {INVOCATION})
\s{DEVICE-LOCK}\s - your main lock

## Story 4a. Alexa Start
* [x] Alexa, ask {INVOCATION}
* [x] Alexa, launch {INVOCATION}

## LockSupportedIntent (no story)
* Alexa, ask {INVOCATION} which locks it knows about
 * [x] _With one lock configured_
 * [x] _With multiple locks configured_
 * [x] _With no locks configured_

## LockLockIntent (4b. Single Door Lock and 4c. Multiple Door Lock)
* [x] Alexa, ask {INVOCATION} to lock the {DEVICE-LOCK} (no door exists)
* [x] Alexa, ask {INVOCATION} to lock the {DEVICE-LOCK} (locked)
* [x] Alexa, ask {INVOCATION} to lock the unknown door (only one door but wrong name)
* [x] Alexa, ask {INVOCATION} to lock the door (only one door but unspecified name)
* [x] Alexa, ask {INVOCATION} lock {DEVICE-LOCK} (unlocked)
* [x] Alexa, ask {INVOCATION} to lock all doors (all doors unlocked)
* [x] Alexa, ask {INVOCATION} lock my doors (some doors unlocked)
* [x] Alexa, ask {INVOCATION} lock the door (multiple locks)
 * [ ] clarification "Which door do you mean?"

### special cases
##### Door lock issue (4d.)
* Alexa, ask {INVOCATION} to lock Jammed Lock (_Setup: one lock configured, it is jammed, called by name_)
* Alexa, ask {INVOCATION} to lock all locks (_Setup: cause jam one lock. Set other locks to unlocked_)
* Alexa, ask {INVOCATION} to lock all locks (_Setup: only one lock configured and jammed_)
* Alexa, ask {INVOCATION} to lock all locks (_Setup: multiple jammed, but not all_)
* Alexa, ask {INVOCATION} to lock all locks (_Setup: multiple locks, all jammed_)

##### With only one lock configured (4bi.)
* [x] Alexa, ask {INVOCATION} to lock the mumblefoo lock

## LockUnlockIntent (4e. Unlock Fail)
* [x] Alexa, ask {INVOCATION} to unlock the {DEVICE-LOCK} (locked)
* [x] Alexa, ask {INVOCATION} unlock {DEVICE-LOCK} (unlocked)
* [x] Alexa, ask {INVOCATION} to unlock the {DEVICE-LOCK} (jammed)
* [x] Alexa, ask {INVOCATION} to unlock all doors (all doors unlocked)
* [x] Alexa, ask {INVOCATION} unlock my doors (some doors unlocked)
* [x] Alexa, ask {INVOCATION} to unlock the door (no doors exists)

## LockStatusIntent (4f. Lock Status)
### for all locks
* [x] Alexa, ask {INVOCATION} about my locks
 * [x] one lock, is locked
 * [x] one lock, is unlocked
 * [x] one lock, is jammed
 * [x] one lock, is null state
 * [x] multiple locks, all locked
 * [x] multiple locks, all unlocked
 * [x] multiple locks, one jammed, remaining locked
 * [x] multiple locks, one jammed, remaining unlocked
 * [x] multiple locks, one jammed, remaining mixed locked & unlocked
 * [x] multiple locks, one null, one jammed, remaining mixed locked & unlocked
 * [x] no locks configured

###for a single, named lock
* [x] Alexa, ask {INVOCATION} if my {DEVICE-LOCK} is locked (locked)
* [x] Alexa, ask {INVOCATION} if my {DEVICE-LOCK} is locked (unlocked)
 * [ ] clarification "Should I lock it?"
* [x] Alexa, ask {INVOCATION} if my {DEVICE-LOCK} is locked (jammed)
* [x] Alexa, ask {INVOCATION} if my {DEVICE-LOCK} is unlocked (locked)
 * [ ] clarification, "Should I lock it?"
* [x] Alexa, ask {INVOCATION} if my {DEVICE-LOCK} is unlocked (unlocked)
* [x] Alexa, ask {INVOCATION} if my {DEVICE-LOCK} is unlocked (jammed)
* [x] Alexa, ask {INVOCATION} to tell me about {DEVICE-LOCK} (locked)
* [x] Alexa, ask {INVOCATION} to tell me about {DEVICE-LOCK} (unlocked),
 * [ ] clarification "Should I lock it?"
* [x] Alexa, ask {INVOCATION} to tell me about {DEVICE-LOCK} (jammed)
* [x] Alexa, ask {INVOCATION} to tell me about the door (only one door)
* [ ] Alexa, ask {INVOCATION} if my door is unlocked (multiple locks),
 * [ ] more than one lock - clarification "Which door do you mean?"
 * [x] for a single lock, unnamed lock - give status with name
* [x] Alexa, ask {INVOCATION} if my door is locked (no doors)
* [x] Alexa, ask {INVOCATION} if my door is locked (only one door)
* [x] Alexa, ask {INVOCATION} if my Unknown Door is locked (only one door but wrong name)


## DoorStatusIntent (4g. Door Status)
**NOT YET IMPLEMENTED**
for all doors
* Alexa, ask {INVOCATION} are my doors closed (all closed)
* Alexa, ask {INVOCATION} are my doors open (some closed)
* Alexa, tell {INVOCATION} to check my doors (some closed)
* Alexa, ask {INVOCATION} about the status of my doors (all open)
* Alexa, ask {INVOCATION} about the status of my doors (no doors)

## DoorQueryIntent (4g. Door Status)
**NOT YET IMPLEMENTED**
for a single, named door
* [ ] Alexa, ask {INVOCATION} if my {DEVICE-LOCK} is open (door is open)
* [ ] Alexa, ask {INVOCATION} if my {DEVICE-LOCK} is open (door is closed)
* [ ] Alexa, ask {INVOCATION} if my {DEVICE-LOCK} is closed (door is open)
* [ ] Alexa, ask {INVOCATION} if my {DEVICE-LOCK} is closed (door is closed)
* [ ] Alexa, ask {INVOCATION} if my door is open (no doors)
* [ ] Alexa, ask {INVOCATION} if my door is closed (only one door)
* [ ] Alexa, ask {INVOCATION} if my Unknown Door is closed (only one door but wrong name)


## BatteryStatusIntent (4h. Battery Status)
**NOT YET IMPLEMENTED**

* [ ] Alexa ask {INVOCATION} for the battery status of the {DEVICE-DOOR}
* [ ] Alexa ask {INVOCATION} for the battery status of the {DEVICE-LOCK}


## HelpIntent (4i. Help intent)
* [x] Alexa, ask {INVOCATION} for help
* [x] Alexa, tell {INVOCATION} to help me
