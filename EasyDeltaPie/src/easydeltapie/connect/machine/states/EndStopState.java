/*
 * Copyright (C) 2017 LucidWolf <https://github.com/LucidWolf>
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

package easydeltapie.connect.machine.states;

import easydeltapie.connect.machine.MachineState;
import static easydeltapie.connect.machine.Repetier.parseRepiterLineDetailed;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class EndStopState implements MachineState{
    private final static String REPETIER_TAGS_ENDSTOPS[] = {"x_max:","y_max:","z_max:","Z-probe state:"};
    
    private LinkedHashMap<String,EndStopValue> endStops = new LinkedHashMap<String,EndStopValue>();
    @Override
    public int getStateType() {
        return MachineState.DATA_ENDSTOPS;
    }
    @Override
    public void parseRepetier(String lineIn) {
            Map<String, String> data = parseRepiterLineDetailed(lineIn.substring(getRepetierKey().length()), REPETIER_TAGS_ENDSTOPS);
            if(!data.isEmpty()){
                endStops.clear();
            }
            for(Map.Entry<String,String> e : data.entrySet()){
                endStops.put(e.getKey(), new EndStopValue(e.getKey(),!e.getValue().equalsIgnoreCase("L")));
            }
    }
    @Override
    public void parseMarlin(String lineIn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
    }
    @Override
    public String getRepetierKey() {
        return "endstops hit:";
    }

    public List<EndStopValue> getValues() {
        ArrayList<EndStopValue> out = new ArrayList<EndStopValue>();
        out.addAll(endStops.values());
        return out;
    }

    @Override
    public void printOut(PrintWriter pw) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<String> readIn(BufferedReader br) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
