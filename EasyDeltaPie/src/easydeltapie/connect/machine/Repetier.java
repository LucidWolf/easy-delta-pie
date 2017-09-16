/*
 * Copyright (C) 2017 LucidWolf
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package easydeltapie.connect.machine;

import easydeltapie.connect.controller.DeltaComControl;
import easydeltapie.connect.machine.states.EepromState;
import easydeltapie.connect.machine.states.EepromValue;
import easydeltapie.connect.machine.states.EndStopState;
import easydeltapie.connect.machine.states.PositionState;
import easydeltapie.connect.machine.states.SingleZProbeState;
import easydeltapie.connect.machine.states.TemperatureState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class Repetier implements Firmware{
    
    
    private final ArrayList<DeltaComControl> interfaces = new ArrayList<DeltaComControl>();
    private final HashMap<Integer,MachineState> states = new HashMap<Integer,MachineState>();
    
    private final EepromState eeprom = new EepromState(Firmware.REPETIER);
    private final EndStopState endStop = new EndStopState();
    private final PositionState position = new PositionState();
    private final TemperatureState temp = new TemperatureState();
    private final SingleZProbeState zProbe = new SingleZProbeState();
    
    //private final TreeMap<String,EndStopValue> endStops = new TreeMap<String,EndStopValue>();
    public Repetier(){
        states.put(MachineState.DATA_UNKNOWN, MachineState.UNKNOWN);
        states.put(MachineState.DATA_IDLE, MachineState.IDLE);
        states.put(MachineState.DATA_COMMAND, MachineState.COMMAND);
        states.put(eeprom.getStateType(),eeprom);
        states.put(endStop.getStateType(),endStop);
        states.put(position.getStateType(),position);
        states.put(temp.getStateType(),temp);
        states.put(zProbe.getStateType(),zProbe);
    }
    // slower access but does it all
    @Override
    public MachineState getMachineState(int stateType) {
        MachineState out = states.get(stateType);
        if(out == null){
            out = states.get(MachineState.DATA_UNKNOWN);
        }
        return out;
    }
    
    @Override
    public EepromState getEepromState() {
        return eeprom;
    }

    @Override
    public EndStopState getEndStopState() {
        return endStop;
    }

    @Override
    public TemperatureState getTemperatureState() {
        return temp;
    }

    @Override
    public PositionState getPositionState() {
        return position;
    }
    @Override
    public boolean addControl(DeltaComControl comControl) {
        boolean canAdd = true;
        for(DeltaComControl dci : interfaces){
            canAdd = canAdd & dci.allowOthers();
        }
        if(canAdd || comControl.allowOthers()){
            interfaces.add(comControl);
            canAdd = canAdd & comControl.allowOthers();
        }
        return canAdd;
    }
    @Override
    public MachineState recieveData(String lineIn) {
        MachineState stateRecived = this.getMachineState(MachineState.DATA_UNKNOWN);
        // run through machine states to see if its something
        for(MachineState state : states.values()){
            if(safeCompare(lineIn,state.getRepetierKey())){
                stateRecived = state;
                state.parseRepetier(lineIn);
                break;
            }
        }
        for(DeltaComControl dci : interfaces){
            dci.recieveData(lineIn, stateRecived);
        }
        return stateRecived;
    }
    



    @Override
    public void removeControl(DeltaComControl aThis) {
        interfaces.remove(aThis);
    }

    @Override
    public boolean canAddControl() {
        boolean canAdd = true;
        for(DeltaComControl dci : interfaces){
            canAdd = canAdd & dci.allowOthers();
        }
        return canAdd;
    }

    @Override
    public void clearControls() {
        interfaces.clear();
    }

    public static boolean safeCompare(String lineIn, String compare) {
        if(compare.isEmpty()){return false;}
        if(lineIn.length()<compare.length()){return false;}
        return safeSubString(lineIn,0,compare.length()).equalsIgnoreCase(compare);
    }
    public static String safeSubString(String lineIn, int start, int end) {
        if(end > lineIn.length()){
            end = lineIn.length();
        }
        if(start > end){
            return "";
        }
        return lineIn.substring(start, end);
    }
    public static Map<String,String> parseRepiterLineSimple(String lineIn) {
        LinkedHashMap<String,String> out = new LinkedHashMap<String,String>();
        //endstops hit: x_max:L y_max:L z_max:L Z-probe state:L
        //T:139.84 /0 B:24.72 /0 B@:0 @:0
        StringTokenizer tok = new StringTokenizer(lineIn,":");
        String lastLabel = null;
        while(tok.hasMoreTokens()){
            String piece = tok.nextToken().trim();
            int index = 0;
            if(lastLabel != null){
                String value;
                index = piece.indexOf(" ");
                if(index == -1){
                    value = piece.trim();
                    index = 0;
                }else{
                    value = piece.substring(0, index).trim();
                }
                out.put(lastLabel, value);
            }
            lastLabel = piece.substring(index).trim();
        }
        return out;
    }
    public static Map<String,String> parseRepiterLineDetailed(String lineIn, String[]tags) {
        LinkedHashMap<String,String> out = new LinkedHashMap<String,String>();
        TreeMap<Integer,String> tagMap = new TreeMap<Integer,String>();
        for(String aTag:tags){
            int index = lineIn.indexOf(aTag);
            if(index >= 0){
                tagMap.put(index, aTag);
            }
        }
        Entry<Integer,String> lastDiv = null;
        for(Entry<Integer,String> div : tagMap.entrySet()){
            if(lastDiv != null){
                String value = lineIn.substring(lastDiv.getKey()+lastDiv.getValue().length(), div.getKey()).trim();
                out.put(lastDiv.getValue(), value);
            }
            lastDiv = div;
        }
        if(lastDiv != null){
            out.put(lastDiv.getValue(), lineIn.substring(lastDiv.getKey()+lastDiv.getValue().length()).trim());
        }
        return out;
    }

    @Override
    public int getType() {
        return Firmware.REPETIER;
    }

    
}
