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

import easydeltapie.connect.machine.states.CommandState;
import easydeltapie.connect.machine.states.IdleState;
import easydeltapie.connect.machine.states.UnknownState;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public interface MachineState {
    // try to stack these in order of expected returns
    public static int DATA_UNKNOWN = -1;
    public static int DATA_IDLE = 0;
    public static int DATA_COMMAND = 1;
    public static int DATA_LOC = 2;
    public static int DATA_EEPROM = 3;
    public static int DATA_ENDSTOPS = 4;
    public static int DATA_TEMP = 5;
    public static int DATA_SINGLE_Z_PROBE = 6;
    
    public static MachineState UNKNOWN = new UnknownState();
    public static MachineState IDLE    = new IdleState();
    public static MachineState COMMAND = new CommandState();
    
    public int getStateType();    

    public String getRepetierKey();
    public void parseRepetier(String lineIn);
    public void parseMarlin(String lineIn);
    
}
