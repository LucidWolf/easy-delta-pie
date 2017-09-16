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
import easydeltapie.connect.machine.states.TemperatureState;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public interface Firmware {
    public static int REPETIER = 0;
    public static int MARLIN   = 1;
    
    // deals with com traffic and parsing that data
    public MachineState recieveData(String lineIn);

    // controls that will want to know what is happening and can block others
    public boolean addControl(DeltaComControl comControl);
    public void removeControl(DeltaComControl aThis);
    public boolean canAddControl();
    public void clearControls();
    
    // gets and sets for machine states
    public MachineState getMachineState(int stateType);
    public EepromState getEepromState();
    public EndStopState getEndStopState();
    public TemperatureState getTemperatureState();
    public PositionState getPositionState();
    
    public int getType();
    
}
