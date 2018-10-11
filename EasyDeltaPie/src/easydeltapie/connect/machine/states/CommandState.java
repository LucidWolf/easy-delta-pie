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
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class CommandState implements MachineState{

    @Override
    public int getStateType() {
        return MachineState.DATA_COMMAND;
    }

    @Override
    public String getRepetierKey() {
        return "ok";
    }

    @Override
    public void parseRepetier(String lineIn) {
    }

    @Override
    public void parseMarlin(String lineIn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void printOut(PrintWriter pw) {
        
    }

    @Override
    public ArrayList<String> readIn(BufferedReader br) {
        return null;
    }

}

