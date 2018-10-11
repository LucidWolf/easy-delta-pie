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

package easydeltapie.connect.machine.states;

import easydeltapie.connect.machine.Firmware;
import easydeltapie.connect.machine.MachineState;
import easydeltapie.escher3D.CalibrationException;
import easydeltapie.escher3D.DeltaParameters;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class EepromState extends AbstractListModel implements MachineState, ComboBoxModel{
    private final HashMap<Integer,EepromValue> eeprom = new HashMap<>();
    private final ArrayList<EepromValue> modelList = new ArrayList<EepromValue>();
    private String command;
    private EepromValue stepsPerMM = null;
    private EepromValue maxBuildRadius = null;
    private EepromValue diagonalRodLength = null;
    private EepromValue diagonalRodAdjustA = null;
    private EepromValue diagonalRodAdjustB = null;
    private EepromValue diagonalRodAdjustC = null;
    private EepromValue horizontalRodRadius = null;
    private EepromValue homedHeight = null; 
    private EepromValue aStopAdjust = null;
    private EepromValue bStopAdjust = null;
    private EepromValue cStopAdjust = null;
    private EepromValue aAngle = null;
    private EepromValue bAngle = null;
    private EepromValue cAngle = null;
    private EepromValue probeZStartHeight = null;
    private EepromValue probeZHeight = null;
    private final int firmware;
    private EepromValue lastSelected = null;
    public EepromState(int firmware){
        this.firmware = firmware;
    }
    public List<EepromValue> getValues() {
        ArrayList<EepromValue> out = new ArrayList<EepromValue>();
        out.addAll(this.eeprom.values());
        return out;
    }

    @Override
    public int getStateType() {
        return MachineState.DATA_EEPROM;
    }

    @Override
    public String getRepetierKey() {
        return "EPR:";
    }
    @Override
    public void parseMarlin(String inLine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public synchronized void parseRepetier(String lineIn) {
        try{
            StringTokenizer tok = new StringTokenizer(lineIn.substring(getRepetierKey().length()));
            int type = Integer.parseInt(tok.nextToken());
            int position = Integer.parseInt(tok.nextToken());
            String value = tok.nextToken();
            StringBuilder sb = new StringBuilder();
            while(tok.hasMoreTokens()){
                sb.append(tok.nextToken()).append(" ");
            }
            String desc = sb.toString().trim();
            EepromValue aVal = new EepromValue(type, position, value, desc);
            if(eeprom.get(position)==null){
                eeprom.put(position, aVal);
                modelList.clear();
                modelList.addAll(eeprom.values());
                Collections.sort(modelList);
                this.fireIntervalAdded(this, modelList.size(), modelList.size());
                this.fireContentsChanged(this, 0, modelList.size());
            }else{
                EepromValue old = eeprom.put(position, aVal);
                int index = modelList.indexOf(old);
                modelList.remove(index);
                modelList.add(index, aVal);
                this.fireContentsChanged(this, index, index);
            }
            if(desc.equalsIgnoreCase("Max printable radius [mm]")){
                maxBuildRadius = aVal;
            }else if(desc.equalsIgnoreCase("Steps per mm")){
                this.stepsPerMM = aVal;
            }else if(desc.equalsIgnoreCase("Diagonal rod length [mm]")){
                this.diagonalRodLength = aVal;
            }else if(desc.equalsIgnoreCase("Horizontal rod radius at 0,0 [mm]")){
                this.horizontalRodRadius = aVal;
            }else if(desc.equalsIgnoreCase("Z max length [mm]")){
                this.homedHeight = aVal;
            }else if(desc.equalsIgnoreCase("Tower X endstop offset [steps]")){
                this.aStopAdjust = aVal;
            }else if(desc.equalsIgnoreCase("Tower Y endstop offset [steps]")){
                this.bStopAdjust = aVal;
            }else if(desc.equalsIgnoreCase("Tower Z endstop offset [steps]")){
                this.cStopAdjust = aVal;
            }else if(desc.equalsIgnoreCase("Alpha A(210):")){
                this.aAngle = aVal;
            }else if(desc.equalsIgnoreCase("Alpha B(330):")){
                this.bAngle = aVal;
            }else if(desc.equalsIgnoreCase("Alpha C(90):")){
                this.cAngle = aVal;
            }else if(desc.equalsIgnoreCase("Max. z-probe - bed dist. [mm]")){
                this.probeZStartHeight = aVal;
            }else if(desc.equalsIgnoreCase("Z-probe height [mm]")){
                this.probeZHeight = aVal;
            }else if(desc.equalsIgnoreCase("Corr. diagonal A [mm]")){
                this.diagonalRodAdjustA = aVal;
            }else if(desc.equalsIgnoreCase("Corr. diagonal B [mm]")){
                this.diagonalRodAdjustB = aVal;
            }else if(desc.equalsIgnoreCase("Corr. diagonal C [mm]")){
                this.diagonalRodAdjustC = aVal;
            }
        }catch(Exception e){
            
        }
    }
    
    @Override
    public void printOut(PrintWriter pw) {
        for(EepromValue ev: modelList){
            //type, position, value, desc
            pw.println(ev.getType()+"\t"+ev.getPosition()+"\t"+ev.getValue()+"\t"+ev.toString());
        }
    }

    @Override
    public ArrayList<String> readIn(BufferedReader br) {
        ArrayList<String> commands = new ArrayList<>();
        ArrayList<EepromValue> values = new ArrayList<>();
        boolean error = false;
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                StringTokenizer tok = new StringTokenizer(line,"\t");
                try{
                    int type = Integer.parseInt(tok.nextToken());
                    int position = Integer.parseInt(tok.nextToken());
                    String value = tok.nextToken();
                    String desc = tok.nextToken();
                    values.add(new EepromValue(type, position, value, desc));
                }catch(Exception e){
                   error = true;
                }
            }        
        } catch (IOException ex) {
            error = true;
        }
        if(!error){
            
            for(EepromValue ev : values){
                // check if ev matches
                EepromValue cur = this.eeprom.get(ev.getPosition());
                if(cur == null){
                    // <>< Say why we stoped?
                    error = true;
                    break;
                }else{
                    // only add it to the commands if it changes the values
                    if(cur.getValue().compareToIgnoreCase(ev.getValue())!=0){
                        commands.add(this.getEepromWriteCommand(ev, ev.getValue()));
                    }
                }
            }
            if(error){
                commands.clear();
            }else{
                commands.add(this.getEepromCommand());
            }
        }else{
            // tell the user?
        }
        return commands;
    }
    public EepromValue getProbeZStartHeight() {
        return probeZStartHeight;
    }    
    public EepromValue getProbeZHeight() {
        return probeZHeight;
    }
    public EepromValue getStepsPerMM() {
        return stepsPerMM;
    }
    public EepromValue getDiagonalRodLength() {
        return diagonalRodLength;
    }
    public float[] getDiagonalRodLenghts(){
        float out[] = new float[3];
        float rod = diagonalRodLength.getValueAsFloat();
        if(firmware == Firmware.REPETIER){
            out[0] = rod + diagonalRodAdjustA.getValueAsFloat();
            out[1] = rod + diagonalRodAdjustB.getValueAsFloat();
            out[2] = rod + diagonalRodAdjustC.getValueAsFloat();
        }else{
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        return out;
    }
    // all three rod lenghts in mm
    public List<String> getDiagonalRodLenghtsEepromCommands(float[] lengths){
        List<String> out = new ArrayList<String>();
        if(firmware == Firmware.REPETIER){
            float avg = (lengths[0]+lengths[1]+lengths[2])/3.0f;
            float a = lengths[0] - avg;
            float b = lengths[1] - avg;
            float c = lengths[2] - avg;
            out.add(this.getEepromWriteCommand(this.diagonalRodLength, ""+avg));
            out.add(this.getEepromWriteCommand(this.diagonalRodAdjustA, ""+a));
            out.add(this.getEepromWriteCommand(this.diagonalRodAdjustB, ""+b));
            out.add(this.getEepromWriteCommand(this.diagonalRodAdjustC, ""+c));
        }else{
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        return out;
    }
    // output in distance mm not steps
    public float[] getTowerStopAdjusts(){
        float out[] = new float[3];
        out[0] = this.aStopAdjust.getValueAsFloat();
        out[1] = this.bStopAdjust.getValueAsFloat();
        out[2] = this.cStopAdjust.getValueAsFloat();
        if(Float.isNaN(out[0]) || Float.isNaN(out[1]) || Float.isNaN(out[2])){
            return null;
        }
        if(firmware == Firmware.REPETIER){
            float stepsMM = this.stepsPerMM.getValueAsFloat();
            out[0] = out[0]/stepsMM;
            out[0] = out[1]/stepsMM;
            out[0] = out[2]/stepsMM;
        }
        return out;
    }
    // input in mm not steps
    public List<String> getTowerStopAdjustEepromCommands(float stops[]) {
        List<String> out = new ArrayList<String>();
        if(firmware == Firmware.REPETIER){
            float step = this.stepsPerMM.getValueAsFloat();
            int aStopStep = (int)Math.round(stops[0]*step);
            int bStopStep = (int)Math.round(stops[1]*step);
            int cStopStep = (int)Math.round(stops[2]*step);
            out.add(getEepromWriteCommand(this.aStopAdjust, ""+aStopStep));
            out.add(getEepromWriteCommand(this.bStopAdjust, ""+bStopStep));
            out.add(getEepromWriteCommand(this.cStopAdjust, ""+cStopStep));
        }
        else{
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return out;
    }
    // gets tower angles in absolute
    public float[] getTowerAngleAdjusts(){
        float out[] = new float[3];
        if(firmware == Firmware.REPETIER){
            out[0] = this.aAngle.getValueAsFloat();
            out[1] = this.bAngle.getValueAsFloat();
            out[2] = this.cAngle.getValueAsFloat();
        }else{
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return out;
    }
    public List<String> getTowerAngleAdjustEepromCommand(float angleA, float angleB, float angleC) {
        List<String> out = new ArrayList<String>();
        if(firmware == Firmware.REPETIER){
            out.add(getEepromWriteCommand(this.aAngle, ""+angleA));
            out.add(getEepromWriteCommand(this.bAngle, ""+angleB));
            out.add(getEepromWriteCommand(this.cAngle, ""+angleC));
        }
        else{
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return out;
    }

    public EepromValue getHorizontalRodRadius(){
        return horizontalRodRadius;
    }

    public EepromValue getMaxBuildRadius() {
        return maxBuildRadius;
    }
    public EepromValue getHomedHeight() {
        return homedHeight;
    }

    public String getEepromCommand() {
        return "M205";
    }
    public String getEepromWriteCommand(EepromValue ev, String value) {
        StringBuilder sb = new StringBuilder();
        if(firmware == Firmware.REPETIER){
            sb.append("M206 ");
            if(ev.getType() == EepromValue.FLOAT){
                sb.append("T").append(ev.getType()).append(" P");
                sb.append(ev.getPosition()).append(" X").append(value).append(" ");
            }else{
                sb.append("T").append(ev.getType()).append(" P");
                sb.append(ev.getPosition()).append(" S").append(value).append(" ");
            }
        }else{
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        return sb.toString();
    }
    public DeltaParameters getParametersForEscher3D() throws CalibrationException{
        double diagonalRodLengthD = diagonalRodLength.getValueAsFloat();
        double horizontalRodRadiusD = horizontalRodRadius.getValueAsFloat();
        double homedHeightD = homedHeight.getValueAsFloat(); 
        double aStopAdjustD = aStopAdjust.getValueAsFloat(); 
        double bStopAdjustD = bStopAdjust.getValueAsFloat(); 
        double cStopAdjustD = cStopAdjust.getValueAsFloat(); 
        double aAngleAdjD = 0.0;
        double bAngleAdjD = 0.0;
        double cAngleAdjD = 0.0;
        if(firmware == Firmware.REPETIER){
            aAngleAdjD = aAngle.getValueAsFloat()-210.0f;
            bAngleAdjD = bAngle.getValueAsFloat()-330.0f;
            cAngleAdjD = cAngle.getValueAsFloat()-90.0f;            
            aStopAdjustD = aStopAdjust.getValueAsFloat()/stepsPerMM.getValueAsFloat(); 
            bStopAdjustD = bStopAdjust.getValueAsFloat()/stepsPerMM.getValueAsFloat(); 
            cStopAdjustD = cStopAdjust.getValueAsFloat()/stepsPerMM.getValueAsFloat(); 
          }else{
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        return new DeltaParameters(firmware, diagonalRodLengthD, horizontalRodRadiusD, homedHeightD, 
        aStopAdjustD, bStopAdjustD, cStopAdjustD, 
        aAngleAdjD, bAngleAdjD, cAngleAdjD);
    }


    @Override
    public synchronized int getSize() {
        return this.eeprom.size();
    }

    @Override
    public synchronized Object getElementAt(int index) {
        Object out = modelList.get(index);
        return out;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if(anItem instanceof EepromValue){
            lastSelected = (EepromValue)anItem;
        }
    }

    @Override
    public Object getSelectedItem() {
        return lastSelected;
    }






}
