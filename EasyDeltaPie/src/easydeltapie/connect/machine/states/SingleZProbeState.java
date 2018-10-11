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
import java.util.Map;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class SingleZProbeState implements MachineState{
     private final static String REPETIER_Z_PROBE[] = {"Z-probe:","X:","Y:"};
    private float x = Float.NaN;
    private float y = Float.NaN;
    private float z = Float.NaN;
    @Override
    public int getStateType() {
        return MachineState.DATA_SINGLE_Z_PROBE;
    }

    @Override
    public String getRepetierKey() {
        return "Z-probe:";
    }

    @Override
    public void parseRepetier(String lineIn) {
        // Z-probe:4.57 X:85.50 Y:0.00
        Map<String, String> data = parseRepiterLineDetailed(lineIn, REPETIER_Z_PROBE);
        try{
            z = Float.parseFloat(data.get(REPETIER_Z_PROBE[0]));
            x = Float.parseFloat(data.get(REPETIER_Z_PROBE[1]));
            y = Float.parseFloat(data.get(REPETIER_Z_PROBE[2]));
        }catch(Exception e){
            // do nothing
        }
        
    }

    @Override
    public void parseMarlin(String lineIn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    public float z(){
        return z;
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
