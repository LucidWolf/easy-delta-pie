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
public class PositionState implements MachineState{
    private final static String TAGS_LOC[] = {"X:","Y:","Z:","E:"};

    private float x = 0.0f;
    private float y = 0.0f;
    private float z = 0.0f;
    private float e = 0.0f;
    public PositionState(){
        
    }

    @Override
    public int getStateType() {
        return MachineState.DATA_LOC;
    }

    @Override
    public String getRepetierKey() {
        return "X:";
    }

    @Override
    public void parseRepetier(String inLine) {
        //X:0.00 Y:0.00 Z:0.000 E:0.0000
        Map<String, String> data = parseRepiterLineDetailed(inLine, TAGS_LOC);
            x = Float.parseFloat(data.get(TAGS_LOC[0]));
            y = Float.parseFloat(data.get(TAGS_LOC[1]));
            z = Float.parseFloat(data.get(TAGS_LOC[2]));
            e = Float.parseFloat(data.get(TAGS_LOC[3]));
    }

    @Override
    public void parseMarlin(String inLine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    public float getX(){
        return x;
    }
    public float getY(){
        return y;
    }
    public float getZ(){
        return z;
    }
    public float getExtruder(){
        return e;
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
