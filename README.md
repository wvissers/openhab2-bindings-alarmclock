# <bindingName> Alarmclock

This is an experimental binding to a virtual alarm clock. I know there are different opinions to have an alarmclock like this as a binding rather than another trigger for a rule engine, but I think nevertheless that it makes some sense. Please let me explain.

The openHAB system is very strong in supporting sitemaps on a variety of devices, including mobile Android and iOS devices. It is a nice advantage if day-to-day operation of the openHAB system can be done on all those devices. This implies using only the limited set of widgets a sitemap supports, like Text, Frame, Switch, Slider, Setpoint etc.   

Suppose you have an openHAB rule to switch off some group of lights at a certain time, but you want to change the setting from time to time. When having the actual alarmclock available as a binding, you could include the setting feature to a standard sitemap. This is exactly what this binding offers. Please share different thoughts about this subject, because I realize from an architectural point of view this is kind of a strange binding. 

In addition to the standard alarmclock, there are sunrise/sunset clocks and a countdown timer included in this binding.

## Supported Things

The binding uses the system clock for the current time, and checks every minute if one of the trigger times (either on or off) are reached. It supports an alarmclock, settable with a precision of 1 minute for both on and off times. In addition, clocks that switches on at a specific time, and switches off at sunset (allowing for an offset from the sunset time) or the other way around are supported.
It also supports a timer, that counts down in seconds from a certain settable starting point.

## Quick start

For a quick start to review this binding, proceed as follows:

1. Download the repository as zip, and locate the file org.openhab.binding.alarmclock-2.4.0-SNAPSHOT.jar in the target folder.
2. Copy this file to the addons folder of the operational openHAB system.
3. Use the Paper UI to create a new thing, using the "Alarmclock" binding.
4. With the Paper UI, set the (default) times, days, offsets etc. to values of your choice. 
5. Add some items as desired.
6. See the alarmclock in action, e.g. using the Control section of the Paper UI.

## Discovery

Auto-discovery is not applicable to this binding. Default on and off times may be specified using e.g. the Paper UI. Just add a thing from the Alarmclock binding, choose the Alarmclock thing, and specify the on and off hours and minutes.

## Binding Configuration

There is no binding configuration necessary. Place the alarmclock jar file into the addons directory as described above and the binding will be supported.

## Thing Configuration

Configuring the alarmclock thing is quite straightforward. When creating the thing with the Paper UI you are prompted for entering the thing name, ontime (hour and minutes) and offtime (hour and minutes) or the other supported settings.  

## Channels

The channels can be retrieved from the Paper UI after configuring. They include:

Todo:


## Full Example

Create the thing using the Paper UI. It will show the relevant configuration settings and let you define the switching on/switching off times.

```
// Clock item definition example
String FF_Clock_On          "Alarm on [%s]"     <clock>  { channel = "alarmclock:alarm:example:onTime"}
String FF_Clock_Off         "Alarm off [%s]"    <clock>  { channel = "alarmclock:alarm:example:offTime"}
Switch FF_Clock_Status      "Status"            <clock>  { channel = "alarmclock:alarm:example:status" } 
Switch FF_Clock_Enabled     "Enabled"           <clock>  { channel = "alarmclock:alarm:example:enabled" } 
Switch FF_Clock_DayEnabled  "Active today [%s]" <clock>  { channel = "alarmclock:alarm:example:dayEnabled" } 
String FF_Clock_Days        "Days active [%s]"  <clock>  { channel = "alarmclock:alarm:example:days" } 
```

In a sitemap use these items, e.g. as follows:

```
Text label="Example" icon="clock" {
    Text     item=GF_Clock_On
    Text     item=GF_Clock_Off
    Switch   item=GF_Clock_Status
    Switch   item=GF_Clock_Enabled
    Switch   item=GF_Clock_DayEnabled
    Text     item=GF_Clock_Days
}

```

The alarmclock has a trigger channel, that triggers ON at the onTime moment, and OFF at the offTime moment. Use this channel in two separate rules to trigger different actions, e.g.

```
// Switch something on
rule "Something on"
    when
        Channel "alarmclock:alarm:example:triggered" triggered ON
    then
        sendCommand(Some_Item, ON)
end

// Switch something off.
rule "Something off"
    when
        Channel "alarmclock:alarm:example:triggered" triggered OFF
    then
        sendCommand(Some_Item, OFF)
end
```

