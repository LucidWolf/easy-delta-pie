/*
 * Copyright (C) 2017 Escher3D
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
package easydeltapie.escher3D;

import java.util.ArrayList;

/**
 *
 * @author Escher3D
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class DeltaCalibration {
    private final boolean debug = false;
    // 3 factors (endstop corrections only)
    public static final int CALIB_ENDSTOPONLY = 3;
    // 4 factors (endstop corrections and delta radius)
    public static final int CALIB_ENDSTOP_RADIUS = 4;
    // 6 factors (endstop corrections, delta radius, and two tower angular position corrections)
    public static final int CALIB_ENDSTOP_RADIUS_TOWER = 6;
    // 7 factors (endstop corrections, delta radius, two tower angular position corrections, and diagonal rod length)
    public static final int CALIB_ENDSTOP_RADIUS_TOWER_ROD = 7;
    
    private final ArrayList<BedProbePoint> points;
    private final boolean normalise;
   
    private final DeltaParameters deltaParams;
    private double initialRmsError = Double.NaN;
    private double expectedRmsError = Double.NaN;

    
    public DeltaCalibration(DeltaParameters deltaParams, ArrayList<BedProbePoint> points, boolean normalise){
        this.points = points;
        this.normalise = normalise;
        this.deltaParams = deltaParams;
    }
    private double fsquare(double x) {
	return x * x;
    }    
    public void doDeltaCalibration(int numFactors) throws CalibrationException{
        int numPoints = points.size();
        if (numFactors != 3 && numFactors != 4 && numFactors != 6 && numFactors != 7) {
		throw new CalibrationException("Error: " + numFactors + " factors requested but only 3, 4, 6 and 7 supported");
	}
	if (numFactors > numPoints) {
		throw new CalibrationException("Error: need at least as many points as factors you want to calibrate");
	}

	//<>< ClearDebug();
        initialRmsError = 0.0;
	// Transform the probing points to motor endpoints and store them in a matrix, so that we can do multiple iterations using the same data
	Matrix probeMotorPositions = new Matrix(numPoints, 3);
	double corrections[] = new double[numPoints];
	for (int i = 0; i < numPoints; i++) {
		corrections[i] = 0.0;
		double xp = points.get(i).x(); 
                double yp = points.get(i).y();
                double machinePos[] = new double[3];
		machinePos[0] = (xp);
		machinePos[1] = (yp);
		machinePos[2] = (0.0);

		probeMotorPositions.data[i][0] = deltaParams.transform(machinePos, 0);
		probeMotorPositions.data[i][1] = deltaParams.transform(machinePos, 1);
		probeMotorPositions.data[i][2] = deltaParams.transform(machinePos, 2);

		initialRmsError += fsquare(points.get(i).zDRev());
	}

	//<>< DebugPrint(probeMotorPositions.Print("Motor positions:"));
	
	// Do 1 or more Newton-Raphson iterations
	int iteration = 0;
	for (;;) {
		// Build a Nx7 matrix of derivatives with respect to xa, xb, yc, za, zb, zc, diagonal.
		Matrix derivativeMatrix = new Matrix(numPoints, numFactors);
		for (int i = 0; i < numPoints; i++) {
			for (int j = 0; j < numFactors; j++) {
				derivativeMatrix.data[i][j] =
                                    deltaParams.computeDerivative(j, probeMotorPositions.data[i][0], probeMotorPositions.data[i][1], probeMotorPositions.data[i][2]);
			}
		}

		//<>< DebugPrint(derivativeMatrix.Print("Derivative matrix:"));

		// Now build the normal equations for least squares fitting
		Matrix normalMatrix = new Matrix(numFactors, numFactors + 1);
		for (int i = 0; i < numFactors; i++) {
			for (int j = 0; j < numFactors; j++) {
				double temp = derivativeMatrix.data[0][i] * derivativeMatrix.data[0][j];
				for (int k = 1; k < numPoints; k++) {
					temp += derivativeMatrix.data[k][i] * derivativeMatrix.data[k][j];
				}
				normalMatrix.data[i][j] = temp;
			}
			double temp = derivativeMatrix.data[0][i] * -(points.get(0).zDRev() + corrections[0]);
			for (int k = 1; k < numPoints; k++) {
				temp += derivativeMatrix.data[k][i] * -(points.get(k).zDRev() + corrections[k]);
			}
			normalMatrix.data[i][numFactors] = temp;
		}

		//<>< DebugPrint(normalMatrix.Print("Normal matrix:"));

		double solution[] = normalMatrix.gaussJordan(numFactors);
		
		for (int i = 0; i < numFactors; i++) {
			if (Double.isNaN(solution[i])) {
				throw new CalibrationException("Unable to calculate corrections. Please make sure the bed probe points are all distinct.");
			}
		}

		//<>< DebugPrint(normalMatrix.Print("Solved matrix:"));

		if (debug) {
			//<>< DebugPrint(PrintVector("Solution", solution));

			// Calculate and display the residuals
			double residuals[] = new double[numPoints];
			for (int i = 0; i < numPoints; i++) {
				double r = points.get(i).zDRev();
				for (int j = 0; j < numFactors; j++) {
					r += solution[j] * derivativeMatrix.data[i][j];
				}
				residuals[i] = (r);
			}
			//<>< DebugPrint(PrintVector("Residuals", residuals));
		}

		deltaParams.adjust(solution, normalise);

		// Calculate the expected probe heights using the new parameters
		{
			double expectedResiduals[] = new double[numPoints];
			double sumOfSquares = 0.0;
			for (int i = 0; i < numPoints; i++) {
				for (int axis = 0; axis < 3; ++axis) {
					probeMotorPositions.data[i][axis] += solution[axis];
				}
				double newZ = deltaParams.InverseTransform(probeMotorPositions.data[i][0], probeMotorPositions.data[i][1], probeMotorPositions.data[i][2]);
				corrections[i] = newZ;
				expectedResiduals[i] = points.get(i).zDRev() + newZ;
				sumOfSquares += fsquare(expectedResiduals[i]);
			}

			expectedRmsError = Math.sqrt(sumOfSquares/numPoints);
			//<>< DebugPrint(PrintVector("Expected probe error", expectedResiduals));
		}

		// Decide whether to do another iteration Two is slightly better than one, but three doesn't improve things.
		// Alternatively, we could stop when the expected RMS error is only slightly worse than the RMS of the residuals.
		iteration++;
		if (iteration == 3) { break; }
	}
    }    

    public DeltaParameters getParameters() throws CalibrationException {
        return deltaParams.makeClone();
    }

    public double getInitialError() {
        return this.initialRmsError;
    }
    public double getFinalError() {
        return this.expectedRmsError;
    }
}
