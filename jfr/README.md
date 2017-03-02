# JFR utility
load a jfr into memory and export events without using Mission Control
## building
  1. find your current Oracle jdk install with Mission control [`e.g. /usr/java/jdk1.8.0_121`]
     copy the following jars into `lib`
     - `./jre/lib/jfr.jar`
     - `./lib/missioncontrol/plugins/com.jrockit.mc.common_${version}.jar`
     - `./lib/missioncontrol/plugins/com.jrockit.mc.flightrecorder_${version}.jar`
  2. run `$ gradle uberjar`
  3. java -jar ./build/libs/jfr-${release}.jar
     add -Xmx with enough heap space or the jfr size (usually the same amount you would give to mission control)

## commands
 - load / unload
 - list-jfrs
 - list-events
 - event-export
