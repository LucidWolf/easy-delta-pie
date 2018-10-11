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

import easydeltapie.EasyDeltaPie;
import easydeltapie.connect.DeltaComPort;
import easydeltapie.connect.machine.Firmware;
import easydeltapie.connect.machine.MachineState;
import easydeltapie.connect.machine.states.EepromState;
import easydeltapie.connect.machine.states.SingleZProbeState;
import easydeltapie.escher3D.BedProbePoint;
import easydeltapie.escher3D.CalibrationException;
import easydeltapie.escher3D.DeltaCalibration;
import easydeltapie.escher3D.DeltaParameters;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class AutoLevelControl extends DeltaComControl{
    private final float[] testAngles = {-150f, -90f, -30f, 30f, 90f, 150f, -150f, -90f, -30f, 30f, 90f, 150f, 0f};
    private final float[] testPercent = {1f, 1f, 1f, 1f, 1f, 1f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.0f};
    private final EasyDeltaPie dal;
    private final ArrayList<BedProbePoint> points = new ArrayList<BedProbePoint>();
    private final int perRadius;
    private final int retry;
    private final int totalProbe;
    private int probecount = 0;
    private int lastPoint = -1;

    private final float initalZ = 10.0f;
    private final double maxCoefVariation;
    private final EepromState es;
    public AutoLevelControl(DeltaComPort com, EasyDeltaPie dal, int perRadius, int retry, double maxCoefVariation){
        super(com);
        this.dal = dal;
        this.es = com.getFirmwareParser().getEepromState();
        this.perRadius = perRadius;
        this.retry = retry;
        totalProbe = retry*9;
        this.maxCoefVariation = maxCoefVariation;
    }

    @Override
    public void recieveData(String lineIn, MachineState state) {
        if(state.getStateType() == MachineState.DATA_SINGLE_Z_PROBE){
            probecount++;           
            SingleZProbeState zprobe = (SingleZProbeState)state;
            int currentPoint = (int)((probecount-1)/retry);
            int currentPointCount = probecount - currentPoint*retry;
            int complete = (int)(1.0*probecount/totalProbe*100.0);
            if(lastPoint != currentPoint){
                dal.updateAutoLevelStatus(complete,"Current point \t"+(currentPoint+1));
            }
            dal.updateAutoLevelStatus(complete,"Probe("+currentPointCount+") \t"+zprobe.z());
            points.get(currentPoint).addRawProbeZ(zprobe.z());
            lastPoint = currentPoint;
        }
    }

    @Override
    public void run() {
        // build the points
        double testRadius = es.getMaxBuildRadius().getValueAsFloat()*perRadius/100.0;
        float probeZStartHeight = (float)es.getProbeZStartHeight().getValueAsFloat();
        dal.updateAutoLevelStatus(0,"Probe radius = "+(testRadius));
        dal.updateAutoLevelStatus(0,"Probe goal = "+(probeZStartHeight));
        for(int i = 0; i < testAngles.length; i++){
            float x = (float)(testRadius*testPercent[i]*Math.cos(testAngles[i]*Math.PI/180.0));
            float y = (float)(testRadius*testPercent[i]*Math.sin(testAngles[i]*Math.PI/180.0));
            points.add(new BedProbePoint(x, y, probeZStartHeight));
        }
        // home the machine first
        this.addCommandAndWait("G90");
        this.addCommandAndWait("G28");
        this.addCommandAndWait("G1 X0.0 Y0.0 Z"+initalZ);
        for(BedProbePoint bp : points){
            this.addCommandAndWait("G1 X"+bp.x()+" Y"+bp.y()+" Z"+initalZ);
            for(int i = 0; i < retry; i++){
                this.addCommandAndWait("G30");
            }
        }
        this.addCommandAndWait("G28");
        // should have all the data now
        int count = 1;
        for(BedProbePoint bp : points){
            bp.runStatisticalAnalysis(maxCoefVariation);
            dal.updateAutoLevelStatus(99,"Point("+count+") Z \t"+(float)bp.z());
            dal.updateAutoLevelStatus(99,"Point("+count+") COV \t"+(float)bp.cov());
            dal.updateAutoLevelStatus(99,"Point("+count+") P's removed \t"+bp.pointsRemoved());
            count++;
        }
         DeltaParameters finalParam = null;
        try {
            // run Escher3D routines
            DeltaParameters params = es.getParametersForEscher3D();
            DeltaParameters startParam = params.makeClone();
            DeltaCalibration calibrate = new DeltaCalibration(params, points, true);
            calibrate.doDeltaCalibration(4);
            dal.updateAutoLevelStatus(100,"Calibration Complete:");
            dal.updateAutoLevelStatus(100,"Description\tOld Value\t New Value");
            dal.updateAutoLevelStatus(100,"Error\t"+f(calibrate.getInitialError())+"\t"+f(calibrate.getFinalError()));
            finalParam = calibrate.getParameters();
            double[] endsN = finalParam.getStopAdjusts();
            double[] endsO = startParam.getStopAdjusts();
            dal.updateAutoLevelStatus(100,"Rad @ 0.0\t"+f(startParam.getHorizontalRodRadius())+"\t"+f(finalParam.getHorizontalRodRadius()));
            dal.updateAutoLevelStatus(100,"Height\t"+f(startParam.getHomedHeight())+"\t"+f(finalParam.getHomedHeight()));
            dal.updateAutoLevelStatus(100,"Aadjust\t"+f(endsO[0])+"\t"+f(endsN[0]));
            dal.updateAutoLevelStatus(100,"Badjust\t"+f(endsO[1])+"\t"+f(endsN[1]));
            dal.updateAutoLevelStatus(100,"Cadjust\t"+f(endsO[2])+"\t"+f(endsN[2]));
            int opt = JOptionPane.showConfirmDialog(dal,dal.getStatusString(), "Update Firmware?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if(opt == JOptionPane.YES_OPTION){
                ArrayList<String> commands = finalParam.setCommandsForEscher3D(es);
                commands.add(es.getEepromCommand());
                for(String s : commands){
                    this.addCommandAndWait(s);
                }
                this.addCommandAndWait("G28");
            }
            this.selfdestruct();

        } catch (CalibrationException ex) {
            Logger.getLogger(AutoLevelControl.class.getName()).log(Level.SEVERE, null, ex);
            dal.updateAutoLevelStatus(99,"Error with Autolevel calibrate Routine");
            this.selfdestruct();
        }
        
    }

    @Override
    public boolean allowOthers() {
        return false;
    }

    @Override
    protected void destroy() {
        
    }

    private String f(double d) {
        return String.format("%1$.3f", d);
    }
}
