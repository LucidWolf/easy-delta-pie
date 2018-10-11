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
import java.util.List;

/**
 * @author Escher3D
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class BedProbePoint {
    private final float x;
    private final float y;
    private final Float probeStartHeight;
    private float z = 0.0f;

    private final ArrayList<Float> zRaw = new ArrayList<Float>();
    private int intialSize;
    private int finalSize;
    private double cov;
    public BedProbePoint(float x, float y, Float probeStartHeight){
        this.x = x;
        this.y = y;
        this.probeStartHeight = probeStartHeight;
    }
    public void setZ(float z){
        this.z = z;
    }
    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float z() {
        if(probeStartHeight == null){
            return z;
        }
        return probeStartHeight - z;
    }

    public void addRawProbeZ(float z) {
        this.zRaw.add(z);
    }

    public void runStatisticalAnalysis(double maxCoefVariation) {
        this.intialSize = zRaw.size();
        float mean = getMean(zRaw);
        float sd = getStandardDeviation(zRaw, mean);
        while(maxCoefVariation < sd/mean*100){
            // remove a point and try again
            double maxDist = 0.0;
            int remove = 0;
            for(int i = 0; i<zRaw.size();i++){
                double val = zRaw.get(i);
                double test = Math.abs(val - mean);
                if(test > maxDist){
                    maxDist = test;
                    remove = i;
                }
            }
            zRaw.remove(remove);
            mean = getMean(zRaw);
            sd = getStandardDeviation(zRaw, mean);
        }
        this.finalSize = zRaw.size();
        z = mean;
        cov = sd/mean*100;
    }

    private static float getMean(List<Float> data) {
        float out = 0.0f;
        for(float f : data){
            out = out+f;
        }
        return out/data.size();   
    }
   private static float getStandardDeviation(List<Float> data, float mean) {
        float out = 0.0f;
        for(float f : data){
            out = out + (f-mean)*(f-mean);
        }
        out = (float)Math.sqrt(out/data.size());
        return out;   
   }

    public int pointsRemoved() {
        return this.intialSize-this.finalSize;
    }

    public double cov() {
        return cov;
    }
       
}
