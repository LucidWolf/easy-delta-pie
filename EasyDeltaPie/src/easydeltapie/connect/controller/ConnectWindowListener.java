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
package easydeltapie.connect.controller;

import easydeltapie.EasyDeltaPie;
import easydeltapie.connect.DeltaComPort;
import easydeltapie.connect.machine.MachineState;
import easydeltapie.connect.machine.states.EepromState;
import easydeltapie.connect.machine.states.PositionState;
import java.util.HashSet;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class ConnectWindowListener extends DeltaComControl{
    private final EasyDeltaPie dal;
    private final HashSet<Integer> toFilter = new HashSet<>();
    public ConnectWindowListener(DeltaComPort com, EasyDeltaPie dal){
        super(com);
        this.dal = dal;
        toFilter.add(MachineState.DATA_IDLE);
        toFilter.add(MachineState.DATA_COMMAND);
    }
    @Override
    public void recieveData(String lineIn, MachineState state) {
        int dataType = state.getStateType();
        if(dal.getFilterActive()){            
            for(int filter : toFilter){
                if(dataType == filter){
                    return;
                }
            }
        }
        if(dataType == MachineState.DATA_EEPROM){
            // should set to update only a type but i am lazy
            dal.fireEepromValueChanged((EepromState)state);
        }else if(dataType == MachineState.DATA_LOC){
            dal.setLocationData((PositionState)state);
        }
        if(!lineIn.isEmpty()){
            dal.addToOutput(lineIn+"\n");
        }
        
    }

    @Override
    public boolean allowOthers() {
        return true;
    }

    @Override
    public void destroy() {
        dal.addToOutput("Closing Communications \n");
        
    }
    @Override
    public void run() {
        // give time for serial and board to connect
        this.waitOnMe(1000);
        this.addCommandAndWait(com.getFirmwareParser().getEepromState().getEepromCommand());
        // give the eeprom command time to run
        this.waitOnMe(3000);
        dal.updatedConnectedViews(true);
        dal.fireEepromValueChanged(com.getFirmwareParser().getEepromState());

    }

    
}
