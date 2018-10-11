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
public class TemperatureState implements MachineState{
    private final static String REPETIER_TAGS_TEMP[] = {"T:","B:","B@:","@:"};

    public void TemperatureState(){
        
    }

    @Override
    public int getStateType() {
        return MachineState.DATA_TEMP;
    }

    @Override
    public String getRepetierKey() {
        return "T:";
    }

    @Override
    public void parseRepetier(String lineIn) {
        //Map<String, String> data = parseRepiterLineDetailed(lineIn, REPETIER_TAGS_TEMP);
        //<>< do something
    }

    @Override
    public void parseMarlin(String lineIn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
