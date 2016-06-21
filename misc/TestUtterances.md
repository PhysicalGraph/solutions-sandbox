# Test Utterances to try in the service simulator

## LaunchIntent
* Alexa, ask smart things
* Alexa, launch smart things

## HelpIntent
* Alexa, ask smart things for help
* Alexa, tell smart things to help me

## LockSupportedIntent
* Alexa, ask smart things which locks it knows about (one lock)
* Alexa, ask smart things what doors do you know (multiple locks)
* Alexa, ask smart things what locks do you know (no locks)

## LockLockIntent
* Alexa, ask smart things to lock the entry door (no door exists)
* Alexa, ask smart things to lock the entry door (locked)
* Alexa, ask smart things to lock the entry door (only one door but wrong name)
* Alexa, ask smart things lock entry door (unlocked)
* Alexa, ask Smart things to lock all doors (all doors unlocked)
* Alexa, ask Smart things lock my doors (some doors unlocked)
* Alexa, ask Smart things lock the door (multiple locks), clarification "Which door do you mean?"


### special cases
* cause jam in physical lock
 * Alexa, ask Smart things to lock __<physical lock name>__

* cause jam in physical lock. Set other locks to unlocked
 * Alexa, ask Smart things to lock all doors

## LockUnlockIntent
* Alexa, ask smart things to unlock the entry door (locked)
* Alexa, ask smart things unlock entry door (unlocked)
* Alexa, ask smart things to unlock the entry door (jammed)
* Alexa, ask Smart things to unlock all doors (all doors unlocked)
* Alexa, ask Smart things unlock my doors (some doors unlocked)
* Alexa, ask smart things to unlock the door (no doors exists)

## LockStatusIntent
for all locks
* ALexa, ask smart things are my doors locked (all unlocked)
* Alexa, tell smart things to check my doors (some locked)
* Alexa, ask smart things about the status of my doors (all locked)
* ALexa, ask smart things are my doors locked (no doors)

## LockQueryIntent
for a single, named lock
* Alexa, ask smartthings if my door is locked (no doors)
* Alexa, ask smartthings if my door is locked (only one door)
* Alexa, ask smartthings if my front door is locked (locked)
* Alexa, ask smartthings if my front door is locked (unlocked), clarification "Should I lock it?"
* Alexa, ask smartthings if my front door is locked (jammed)
* Alexa, ask smartthings if my front door is unlocked (locked)
* Alexa, ask smartthings if my front door is unlocked (unlocked)
* Alexa, ask smartthings if my front door is unlocked (jamed)
* Alexa, ask smartthings to tell me about front door (locked)
* Alexa, ask smartthings to tell me about front door (unlocked), clarification "Should I lock it?"
* Alexa, ask smartthings to tell me about front door (jammed)
* Alexa, ask smartthings to tell me about the door (only one door)
* Alexa, ask smartthings if my door is unlocked (multiple locks), clarification "Which door do you mean?"