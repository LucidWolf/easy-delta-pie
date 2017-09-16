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
import easydeltapie.connect.machine.states.EndStopState;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class LimitCheckController extends DeltaComControl{
    private final EasyDeltaPie dal;
    private boolean keepSpamming = true;
    public LimitCheckController(DeltaComPort com, EasyDeltaPie dal){
        super(com);
        this.dal = dal;
    }

    @Override
    public void recieveData(String lineIn, MachineState state) {
        if(state.getStateType() == MachineState.DATA_ENDSTOPS){
            // change labels based on endstop values
            dal.setEndStopStates((EndStopState)state);
        }
    }

    @Override
    public void run() {
        // spam M119 commands every second
        while(keepSpamming){
            // same for all repraps
            this.addCommandAndWait("M119");   
            this.waitOnMe(500);
        }
    }

    @Override
    public boolean allowOthers() {
        return false;
    }

    @Override
    public void destroy() {
        keepSpamming = false;
    }
    
}
