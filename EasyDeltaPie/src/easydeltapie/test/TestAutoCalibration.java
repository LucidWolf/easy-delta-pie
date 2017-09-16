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

package easydeltapie.test;

import easydeltapie.connect.machine.Firmware;
import easydeltapie.escher3D.BedProbePoint;
import easydeltapie.escher3D.CalibrationException;
import easydeltapie.escher3D.DeltaCalibration;
import easydeltapie.escher3D.DeltaParameters;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class TestAutoCalibration {
    private ArrayList<BedProbePoint> points = new ArrayList<BedProbePoint>();
    private DeltaParameters initParams;
    public static void main(String[] args) {
        TestAutoCalibration tac = new TestAutoCalibration();
        tac.test();
    }
    public TestAutoCalibration(){
        // 2 hours of my life gone...  the height error is negative in the javascript...
        //Point 0	X:0.00Y:95.00Nozzle height error:0.10
        BedProbePoint p0 = new BedProbePoint(0.00f,95.00f,0.0f);
        p0.addRawProbeZ(-0.10f);
        //Point 1	X:82.27Y:47.50Nozzle height error:0.25
        BedProbePoint p1 = new BedProbePoint(82.27f,47.50f,0.0f);
        p1.addRawProbeZ(-0.25f);
        //Point 2	X:82.27Y:-47.50Nozzle height error:0.2
        BedProbePoint p2 = new BedProbePoint(82.27f,-47.50f,0.0f);
        p2.addRawProbeZ(-0.2f);
        //Point 3	X:0.00Y:-95.00Nozzle height error:0.03
        BedProbePoint p3 = new BedProbePoint(0.00f,-95.00f,0.0f);
        p3.addRawProbeZ(-0.03f);
        //Point 4	X:-82.27Y:-47.50Nozzle height error:0.05
        BedProbePoint p4 = new BedProbePoint(-82.27f,-47.50f,0.0f);
        p4.addRawProbeZ(-0.05f);
        //Point 5	X:-82.27Y:47.50Nozzle height error:0.02
        BedProbePoint p5 = new BedProbePoint(-82.27f,47.50f,0.0f);
        p5.addRawProbeZ(-0.02f);
        //Point 6	X:0Y:0Nozzle height error:0.1
        BedProbePoint p6 = new BedProbePoint(0.00f,0.00f,0.0f);
        p6.addRawProbeZ(-0.1f);
        points.add(p0);
        points.add(p1);
        points.add(p2);
        points.add(p3);
        points.add(p4);
        points.add(p5);
        points.add(p6);
        
        try {
            //Initial diagonal rod length:
            //211.653
            //Initial delta radius:
            //93.720
            //Initial homed height:
            //285.620
            initParams = new DeltaParameters(Firmware.REPETIER,
                    211.653, 93.720, 285.620,
                    0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0);
            
        } catch (CalibrationException ex) {
            Logger.getLogger(TestAutoCalibration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void test(){

        for(BedProbePoint bp : points){
            bp.runStatisticalAnalysis(1.0);
        }
        DeltaCalibration dc = new DeltaCalibration(initParams, points, true);
        try {
            dc.doDeltaCalibration(4);
        } catch (CalibrationException ex) {
            Logger.getLogger(TestAutoCalibration.class.getName()).log(Level.SEVERE, null, ex);
        }
        DeltaParameters finalParams;
        try {
            //New endstop corrections:	X:0 Y:21    Z:14
            //New diagonal rod length:211.65  New delta radius:93.70  New homed height:285.61
            finalParams = dc.getParameters();
            // should not change
            double errorDiagRod = finalParams.getDiagonalRodLength() - 211.65;
            // 4 values should change
            double errorHorzRad = finalParams.getHorizontalRodRadius() - 93.70;
            double errorDeltaHeight = finalParams.getHomedHeight() - 285.61;
            double errorAStopAdjust = finalParams.getStopAdjusts()[0] - 0.0/100.;
            double errorBStopAdjust = finalParams.getStopAdjusts()[1] - 21.0/100;
            double errorCStopAdjust = finalParams.getStopAdjusts()[2] - 14.0/100;
            System.out.println();
        } catch (CalibrationException ex) {
            Logger.getLogger(TestAutoCalibration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
        EPR:0 1028 0 Language
        EPR:2 75 250000 Baudrate
        EPR:3 129 1183.385 Filament printed [m]
        EPR:2 125 1180828 Printer active [s]
        EPR:2 79 0 Max. inactive time [ms,0=off]
        EPR:2 83 360000 Stop stepper after inactivity [ms,0=off]
        EPR:3 11 100.0000 Steps per mm
        EPR:3 23 200.000 Max. feedrate [mm/s]
        EPR:3 35 50.000 Homing feedrate [mm/s]
        EPR:3 39 20.000 Max. jerk [mm/s]
        EPR:3 133 0.000 X min pos [mm]
        EPR:3 137 0.000 Y min pos [mm]
        EPR:3 141 0.000 Z min pos [mm]
        EPR:3 145 95.000 X max length [mm]
        EPR:3 149 95.000 Y max length [mm]
        EPR:3 153 285.620 Z max length [mm]
        EPR:1 891 70 Segments/s for travel
        EPR:1 889 180 Segments/s for printing
        EPR:3 59 1000.000 Acceleration [mm/s^2]
        EPR:3 71 2000.000 Travel acceleration [mm/s^2]
        EPR:3 881 211.653 Diagonal rod length [mm]
        EPR:3 885 93.720 Horizontal rod radius at 0,0 [mm]
        EPR:3 925 95.000 Max printable radius [mm]
        EPR:1 893 100 Tower X endstop offset [steps]
        EPR:1 895 22 Tower Y endstop offset [steps]
        EPR:1 897 0 Tower Z endstop offset [steps]
        EPR:3 901 210.000 Alpha A(210):
        EPR:3 905 330.000 Alpha B(330):
        EPR:3 909 90.000 Alpha C(90):
        EPR:3 913 0.000 Delta Radius A(0):
        EPR:3 917 0.000 Delta Radius B(0):
        EPR:3 921 0.000 Delta Radius C(0):
        EPR:3 933 -0.272 Corr. diagonal A [mm]
        EPR:3 937 -0.062 Corr. diagonal B [mm]
        EPR:3 941 0.335 Corr. diagonal C [mm]
        EPR:3 1024 0.000 Coating thickness [mm]
        EPR:3 808 -0.330 Z-probe height [mm]
        EPR:3 929 5.000 Max. z-probe - bed dist. [mm]
        EPR:3 812 5.000 Z-probe speed [mm/s]
        EPR:3 840 60.000 Z-probe x-y-speed [mm/s]
        EPR:3 800 0.000 Z-probe offset x [mm]
        EPR:3 804 0.000 Z-probe offset y [mm]
        EPR:3 816 50.000 Z-probe X1 [mm]
        EPR:3 820 30.000 Z-probe Y1 [mm]
        EPR:3 824 -70.000 Z-probe X2 [mm]
        EPR:3 828 0.000 Z-probe Y2 [mm]
        EPR:3 832 0.000 Z-probe X3 [mm]
        EPR:3 836 -70.000 Z-probe Y3 [mm]
        EPR:3 1036 0.000 Z-probe bending correction A [mm]
        EPR:3 1040 0.000 Z-probe bending correction B [mm]
        EPR:3 1044 0.000 Z-probe bending correction C [mm]
        EPR:0 880 0 Autolevel active (1/0)
        EPR:3 976 0.000 tanXY Axis Compensation
        EPR:3 980 0.000 tanYZ Axis Compensation
        EPR:3 984 0.000 tanXZ Axis Compensation
        EPR:0 106 1 Bed Heat Manager [0-3]
        EPR:0 107 255 Bed PID drive max
        EPR:0 124 80 Bed PID drive min
        EPR:3 108 196.000 Bed PID P-gain
        EPR:3 112 33.000 Bed PID I-gain
        EPR:3 116 290.000 Bed PID D-gain
        EPR:0 120 255 Bed PID max value [0-255]
        EPR:0 1020 0 Enable retraction conversion [0/1]
        EPR:3 992 3.000 Retraction length [mm]
        EPR:3 1000 40.000 Retraction speed [mm/s]
        EPR:3 1004 0.000 Retraction z-lift [mm]
        EPR:3 1008 0.000 Extra extrusion on undo retract [mm]
        EPR:3 1016 20.000 Retraction undo speed
        EPR:3 200 150.000 Extr.1 steps per mm
        EPR:3 204 30.000 Extr.1 max. feedrate [mm/s]
        EPR:3 208 10.000 Extr.1 start feedrate [mm/s]
        EPR:3 212 4000.000 Extr.1 acceleration [mm/s^2]
        EPR:0 216 1 Extr.1 heat manager [0-3]
        EPR:0 217 230 Extr.1 PID drive max
        EPR:0 245 60 Extr.1 PID drive min
        EPR:3 218 24.0000 Extr.1 PID P-gain/dead-time
        EPR:3 222 0.8800 Extr.1 PID I-gain
        EPR:3 226 80.0000 Extr.1 PID D-gain
        EPR:0 230 255 Extr.1 PID max value [0-255]
        EPR:2 231 0 Extr.1 X-offset [steps]
        EPR:2 235 0 Extr.1 Y-offset [steps]
        EPR:2 290 0 Extr.1 Z-offset [steps]
        EPR:1 239 1 Extr.1 temp. stabilize time [s]
        EPR:1 250 150 Extr.1 temp. for retraction when heating [C]
        EPR:1 252 0 Extr.1 distance to retract when heating [mm]
        EPR:0 254 255 Extr.1 extruder cooler speed [0-255]
        * Escher 3D test case
        Steps/mm (for Repetier only):		
        100.0000
        Initial endstop corrections:	
        X:0.0
        Y:0.0
        Z:0.0
        Initial diagonal rod length:		
        211.653
        Initial delta radius:		
        93.720
        Initial homed height:		
        285.620
        Initial tower angular position corrections:	
        X:0.0
        Y:0.0
        Z:0.0
        Printable bed radius:		
        95
        Number of probe points:		
        7
        Number of factors to calibrate:		
        4

        Suggest probe points
        //Point 0	X:0.00Y:95.00Nozzle height error:0.10
        //Point 1	X:82.27Y:47.50Nozzle height error:0.25
        //Point 2	X:82.27Y:-47.50Nozzle height error:0.2
        //Point 3	X:0.00Y:-95.00Nozzle height error:0.03
        //Point 4	X:-82.27Y:-47.50Nozzle height error:0.05
        //Point 5	X:-82.27Y:47.50Nozzle height error:0.02
        //Point 6	X:0Y:0Nozzle height error:0.1
        Calculate
        Success! Calibrated 4 factors using 7 points, deviation before 0.13 after 0.04 
        New endstop corrections:	X:0 Y:21    Z:14
        New diagonal rod length:211.65  New delta radius:93.70  New homed height:285.61
        New tower position angle corrections:	X:0.00Y:0.00Z:0.00
  
  
  **/
}
