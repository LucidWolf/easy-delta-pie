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

import easydeltapie.connect.machine.states.EepromState;
import java.util.ArrayList;

/**
 *
 * @author Escher3D
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class DeltaParameters {
    
    private final double degreesToRadians = Math.PI / 180.0;;
    private double diagonalRodLength;
    private double horizontalRodRadius;
    private double homedHeight;
    private double aStopAdjust;
    private double bStopAdjust;
    private double cStopAdjust;
    private double aAngleAdj;
    private double bAngleAdj;
    private final double cAngleAdj;
    
    private double[] towerX;
    private double[] towerY;
    private double Xbc;
    private double Xca;
    private double Xab;
    private double Ybc;
    private double Yca;
    private double Yab;
    private double coreFa;
    private double coreFb;
    private double coreFc;
    private double Q;
    private double Q2;
    private double D2;
    private double homedCarriageHeight;
    private final int firmware;
    
    
    public DeltaParameters(int firmware, double diagonalRodLength, double horizontalRodRadius, double homedHeight, 
        double aStopAdjust, double bStopAdjust, double cStopAdjust, 
        double aAngleAdj, double bAngleAdj, double cAngleAdj) throws CalibrationException {
	this.firmware = firmware;
        this.diagonalRodLength = diagonalRodLength;
	this.horizontalRodRadius = horizontalRodRadius;
	this.homedHeight = homedHeight;
	this.aStopAdjust = aStopAdjust;
	this.bStopAdjust = bStopAdjust;
	this.cStopAdjust = cStopAdjust;
	this.aAngleAdj = aAngleAdj;
	this.bAngleAdj = bAngleAdj;
	this.cAngleAdj = cAngleAdj;
	this.recalc();
    }
    public void recalc() throws CalibrationException{
	this.towerX = new double[3];
	this.towerY = new double[3];
        
	this.towerX[0] = (-(this.horizontalRodRadius * Math.cos((30 + this.aAngleAdj) * degreesToRadians)));
	this.towerY[0] = (-(this.horizontalRodRadius * Math.sin((30 + this.aAngleAdj) * degreesToRadians)));
	this.towerX[1] = (+(this.horizontalRodRadius * Math.cos((30 - this.bAngleAdj) * degreesToRadians)));
	this.towerY[1] = (-(this.horizontalRodRadius * Math.sin((30 - this.bAngleAdj) * degreesToRadians)));
	this.towerX[2] = (-(this.horizontalRodRadius * Math.sin(this.cAngleAdj * degreesToRadians)));
	this.towerY[2] = (+(this.horizontalRodRadius * Math.cos(this.cAngleAdj * degreesToRadians)));

	this.Xbc = this.towerX[2] - this.towerX[1];
	this.Xca = this.towerX[0] - this.towerX[2];
	this.Xab = this.towerX[1] - this.towerX[0];
	this.Ybc = this.towerY[2] - this.towerY[1];
	this.Yca = this.towerY[0] - this.towerY[2];
	this.Yab = this.towerY[1] - this.towerY[0];
	this.coreFa = fsquare(this.towerX[0]) + fsquare(this.towerY[0]);
	this.coreFb = fsquare(this.towerX[1]) + fsquare(this.towerY[1]);
	this.coreFc = fsquare(this.towerX[2]) + fsquare(this.towerY[2]);
	this.Q = 2 * (this.Xca * this.Yab - this.Xab * this.Yca);
	this.Q2 = fsquare(this.Q);
	this.D2 = fsquare(this.diagonalRodLength);

	// Calculate the base carriage height when the printer is homed.
	double tempHeight = this.diagonalRodLength;		// any sensible height will do here, probably even zero
	this.homedCarriageHeight = this.homedHeight + tempHeight - this.InverseTransform(tempHeight, tempHeight, tempHeight);
}
    private double fsquare(double x) {
	return x * x;
    }    

    protected double InverseTransform(double Ha, double Hb, double Hc) throws CalibrationException{
	double Fa = this.coreFa + fsquare(Ha);
	double Fb = this.coreFb + fsquare(Hb);
	double Fc = this.coreFc + fsquare(Hc);

	// Setup PQRSU such that x = -(S - uz)/P, y = (P - Rz)/Q
	double P = (this.Xbc * Fa) + (this.Xca * Fb) + (this.Xab * Fc);
	double S = (this.Ybc * Fa) + (this.Yca * Fb) + (this.Yab * Fc);

	double R = 2 * ((this.Xbc * Ha) + (this.Xca * Hb) + (this.Xab * Hc));
	double U = 2 * ((this.Ybc * Ha) + (this.Yca * Hb) + (this.Yab * Hc));

	double R2 = fsquare(R), U2 = fsquare(U);

	double A = U2 + R2 + this.Q2;
	double minusHalfB = S * U + P * R + Ha * this.Q2 + this.towerX[0] * U * this.Q - this.towerY[0] * R * this.Q;
	double C = fsquare(S + this.towerX[0] * this.Q) + fsquare(P - this.towerY[0] * this.Q) + (fsquare(Ha) - this.D2) * this.Q2;

	double rslt = (minusHalfB - Math.sqrt(fsquare(minusHalfB) - A * C)) / A;
	if (Double.isNaN(rslt)) {
		throw new CalibrationException("At least one probe point is not reachable. Please correct your delta radius, diagonal rod length, or probe coordniates.");
	}
	return rslt;
    }
    // Perform 3, 4, 6 or 7-factor adjustment.
    // The input vector contains the following parameters in this order:
    //  X, Y and Z endstop adjustments
    //  If we are doing 4-factor adjustment, the next argument is the delta radius. Otherwise:
    //  X tower X position adjustment
    //  Y tower X position adjustment
    //  Z tower Y position adjustment
    //  Diagonal rod length adjustment
    public void adjust(double v[], boolean norm) throws CalibrationException {
        int numFactors = v.length;
        double oldCarriageHeightA = this.homedCarriageHeight + this.aStopAdjust;	// save for later

        // Update endstop adjustments
        this.aStopAdjust += v[0];
        this.bStopAdjust += v[1];
        this.cStopAdjust += v[2];
        if (norm) {
                this.normaliseEndstopAdjustments();
        }

        if (numFactors >= 4) {
                this.horizontalRodRadius += v[3];

                if (numFactors >= 6) {
                        this.aAngleAdj += v[4];
                        this.bAngleAdj += v[5];

                        if (numFactors == 7) {
                                this.diagonalRodLength += v[6];
                        }
                }

                this.recalc();
        }

        // Adjusting the diagonal and the tower positions affects the homed carriage height.
        // We need to adjust homedHeight to allow for this, to get the change that was requested in the endstop corrections.
        double heightError = this.homedCarriageHeight + this.aStopAdjust - oldCarriageHeightA - v[0];
        this.homedHeight -= heightError;
        this.homedCarriageHeight -= heightError;
}
// Make the average of the endstop adjustments zero, or make all emndstop corrections negative, without changing the individual homed carriage heights
    private void normaliseEndstopAdjustments() {
	double eav = Math.min(this.aStopAdjust, Math.min(this.bStopAdjust, this.cStopAdjust));
	this.aStopAdjust -= eav;
	this.bStopAdjust -= eav;
	this.cStopAdjust -= eav;
	this.homedHeight += eav;
	this.homedCarriageHeight += eav;				// no need for a full recalc, this is sufficient
    }  

    protected double transform(double[] machinePos, int axis) {
	return machinePos[2] + Math.sqrt(this.D2 - fsquare(machinePos[0] - this.towerX[axis]) - fsquare(machinePos[1] - this.towerY[axis]));
    }

    protected double computeDerivative(int deriv, double ha, double hb, double hc) throws CalibrationException {
	double perturb = 0.2;			// perturbation amount in mm or degrees
	DeltaParameters hiParams = new DeltaParameters(this.firmware, this.diagonalRodLength, this.horizontalRodRadius, this.homedHeight, this.aStopAdjust, this.bStopAdjust, this.cStopAdjust, this.aAngleAdj, this.bAngleAdj, this.cAngleAdj);
	DeltaParameters loParams = new DeltaParameters(this.firmware, this.diagonalRodLength, this.horizontalRodRadius, this.homedHeight, this.aStopAdjust, this.bStopAdjust, this.cStopAdjust, this.aAngleAdj, this.bAngleAdj, this.cAngleAdj);
	switch(deriv)
	{
	case 0:
	case 1:
	case 2:
		break;

	case 3:
		hiParams.horizontalRodRadius += perturb;
		loParams.horizontalRodRadius -= perturb;
		break;

	case 4:
		hiParams.aAngleAdj += perturb;
		loParams.aAngleAdj -= perturb;
		break;

	case 5:
		hiParams.bAngleAdj += perturb;
		loParams.bAngleAdj -= perturb;
		break;

	case 6:
		hiParams.diagonalRodLength += perturb;
		loParams.diagonalRodLength -= perturb;
		break;
	}

	hiParams.recalc();
	loParams.recalc();

	double zHi = hiParams.InverseTransform((deriv == 0) ? ha + perturb : ha, (deriv == 1) ? hb + perturb : hb, (deriv == 2) ? hc + perturb : hc);
	double zLo = loParams.InverseTransform((deriv == 0) ? ha - perturb : ha, (deriv == 1) ? hb - perturb : hb, (deriv == 2) ? hc - perturb : hc);

	return (zHi - zLo)/(2 * perturb);

    }

    public DeltaParameters makeClone() throws CalibrationException {
        return new DeltaParameters(firmware, diagonalRodLength, horizontalRodRadius, homedHeight, 
        aStopAdjust, bStopAdjust, cStopAdjust, 
        aAngleAdj, bAngleAdj, cAngleAdj);
    }
    public double getDiagonalRodLength() {
        return diagonalRodLength;
    }
    public double getHorizontalRodRadius() {
        return horizontalRodRadius;
    }
    public double getHomedHeight() {
        return homedHeight;
    }
    public double[] getStopAdjusts(){
        double out[] = new double[3];
        out[0] = aStopAdjust;
        out[1] = bStopAdjust;
        out[2] = cStopAdjust;
        return out;
    }
    public double[] getTowerAngleAdjusts(){
        double out[] = new double[3];
        out[0] = aAngleAdj;
        out[1] = bAngleAdj;
        out[2] = cAngleAdj;
        return out;
    }

    public ArrayList<String> setCommandsForEscher3D(EepromState es) {
        ArrayList<String> out = new ArrayList<String>();
        // set horizontal rod radius
        out.add(es.getEepromWriteCommand(es.getHorizontalRodRadius(), ""+(float)this.horizontalRodRadius));
        // set height
        out.add(es.getEepromWriteCommand(es.getHomedHeight(), ""+(float)this.homedHeight));
        float val[] = new float[3];
        val[0] = (float)this.aStopAdjust;
        val[1] = (float)this.bStopAdjust;
        val[2] = (float)this.cStopAdjust;
        out.addAll(es.getTowerStopAdjustEepromCommands(val));
        return out;
    }
}
