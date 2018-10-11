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
package easydeltapie.connect;

import easydeltapie.connect.controller.ConnectWindowListener;
import easydeltapie.connect.controller.DeltaComControl;
import easydeltapie.EasyDeltaPie;
import easydeltapie.connect.machine.Firmware;
import easydeltapie.connect.machine.MachineState;
import easydeltapie.connect.machine.Marlin;
import easydeltapie.connect.machine.Repetier;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class DeltaComPort implements Runnable, SerialPortEventListener{
    // Milliseconds to block while waiting for port open 
    private static final int TIME_OUT = 2000;
    private final CommPortIdentifier port;
    private final EasyDeltaPie dal;
    private boolean running;
    private SerialPort serialPort;
    private PrintStream output;
    private BufferedReader input;
    
    private final ArrayDeque<DeltaCommand> commands = new ArrayDeque<>();
    private final ArrayList<DeltaCommand> commandsPriorToIdle = new ArrayList<>();
    
    private Firmware firmware;
    
    public DeltaComPort(EasyDeltaPie dal, CommPortIdentifier port) {
        this.dal = dal;
        this.port = port;
    }
    @Override
    public String toString(){
        return port.getName()+"-"+getPortTypeName(port.getPortType());
    }
    @Override
    public void run() {
        // called when connecting
        // reset parameters
        running = true;
        int baud = dal.getBaudRate();
        int type = dal.getFirmareType();
        if(type == Firmware.REPETIER){
            firmware = new Repetier();
        }else{
            firmware = new Marlin();
        }
        dal.setEepromValues(firmware.getEepromState());
        try {
                // open serial port, and use class name for the appName.
                serialPort = (SerialPort) port.open(this.getClass().getName(),
                                TIME_OUT);

                // set port parameters
                serialPort.setSerialPortParams(baud,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);

                // open the streams
                input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
                output = new PrintStream(serialPort.getOutputStream());

                // add event listeners
                serialPort.addEventListener(this);
                serialPort.notifyOnDataAvailable(true);
        } catch (Exception e) {
                System.err.println(e.toString());
                
        }
        Thread athread = new Thread(new ConnectWindowListener(this, dal)); 
        athread.start();
        // start command loop
        while(running){
            sleep();
            if(!commands.isEmpty()){
                // check to see if first command has run
                if(!commands.getFirst().isRunning()){
                    sendCommand(commands.getFirst());
                }
            }
        }
        
    }
    private synchronized void sleep(){
        try {
            wait(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(DeltaComPort.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    public synchronized void wake(){
        notifyAll();
    }
    public synchronized void close() {
        running = false;
        //dal.setEepromValues(new DefaultComboModel());
        // remove DeltaComInterfaces
        firmware.clearControls();
        if (serialPort != null) {
                serialPort.removeEventListener();
                serialPort.close();
        }
        this.notifyAll();
    }


    
    private static String getPortTypeName (int portType){
        switch (portType){
            case CommPortIdentifier.PORT_I2C:
                return "I2C";
            case CommPortIdentifier.PORT_PARALLEL:
                return "Parallel";
            case CommPortIdentifier.PORT_RAW:
                return "Raw";
            case CommPortIdentifier.PORT_RS485:
                return "RS485";
            case CommPortIdentifier.PORT_SERIAL:
                return "Serial";
            default:
                return "unknown type";
        }
    } 

    @Override
    public void serialEvent(SerialPortEvent spe) {
        if (spe.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                try {
                        String inputLine=input.readLine().trim();
                        MachineState state = firmware.recieveData(inputLine);
                        if(state.getStateType() == MachineState.DATA_COMMAND){
                            if(!commands.isEmpty() && commands.getFirst().isRunning()){
                                // dont know why i am so check heavy
                                DeltaCommand dc = commands.pollFirst();
                                dc.getControl().fireCommandComplete();
                                commandsPriorToIdle.add(dc);
                            }else{
                                // do nothing rewake the com sending thread
                                // send a desync warning? (Nah?)
                                wake();
                            }
                        }else if(state.getStateType() == MachineState.DATA_IDLE){
                            for(DeltaCommand dc : commandsPriorToIdle){
                                dc.getControl().fireIdleComplete();
                            }
                            if(!commands.isEmpty()){                                
                                wake();
                            }
                        }

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
    }

    public boolean addControl(DeltaComControl comControl) {
        boolean canAdd = firmware.addControl(comControl);
        dal.setActionButtonsActive(canAdd);
        return canAdd;
    }

    public synchronized void sendCommand(DeltaCommand command) {
        output.println(command.toString());
        command.started();
    }
    public synchronized void addCommand(DeltaCommand command) {
            commands.add(command);
            notifyAll();
    }

    public void removeInterface(DeltaComControl aThis) {
        firmware.removeControl(aThis);
        boolean canAdd = firmware.canAddControl();
        dal.setActionButtonsActive(canAdd);
    }


    public Firmware getFirmwareParser() {
        return firmware;
    }

}
