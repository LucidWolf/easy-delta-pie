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

package easydeltapie.connect.controller;

import easydeltapie.connect.DeltaComPort;
import easydeltapie.connect.machine.MachineState;
import easydeltapie.connect.machine.states.EepromState;
import easydeltapie.connect.machine.states.SingleZProbeState;
import easydeltapie.escher3D.BedProbePoint;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class SingleProbeControl extends DeltaComControl{

    private BedProbePoint point = null;
    private final int retry;
    private final float initalZ = 10.0f;
    private final EepromState es;
    public SingleProbeControl(DeltaComPort com, int retry){
        super(com);
        this.es = com.getFirmwareParser().getEepromState();
        this.retry = retry;
    }

    @Override
    public void recieveData(String lineIn, MachineState state) {
        if(state.getStateType() == MachineState.DATA_SINGLE_Z_PROBE){         
            SingleZProbeState zprobe = (SingleZProbeState)state;
            point.addRawProbeZ(zprobe.z());
        }
    }

    @Override
    public void run() {
        // build the points
        float probeZStartHeight = (float)es.getProbeZStartHeight().getValueAsFloat();
        float height = (float)es.getHomedHeight().getValueAsFloat();
        //float probeZHeight = (float)es.getProbeZHeight().getValueAsFloat();
        point = new BedProbePoint(0.0f, 0.0f, probeZStartHeight);
        // home the machine first
        this.addCommandAndWait("G90");
        this.addCommandAndWait("G28");
        this.addCommandAndWait("G1 X0.0 Y0.0 Z"+initalZ);
        this.addCommandAndWait("G1 X"+point.x()+" Y"+point.y()+" Z"+initalZ);
        for(int i = 0; i < retry; i++){
            this.addCommandAndWait("G30");
        }
        this.addCommandAndWait("G28");
        // should have all the data now
        point.runStatisticalAnalysis(1.0);
        // get height and subtract probe value to 
        float nHeight = (float)(height - point.z());
        this.addCommandAndWait(es.getEepromWriteCommand(es.getHomedHeight(), ""+nHeight));
        this.addCommandAndWait(es.getEepromCommand());
        this.addCommandAndWait("G28");
        this.addCommandAndWait("G1 X0.0 Y0.0 Z"+probeZStartHeight, true);
        this.addCommandAndWait("M114", true);
        this.selfdestruct();
    }

    @Override
    public boolean allowOthers() {
        return false;
    }

    @Override
    protected void destroy() {
        
    }
}
