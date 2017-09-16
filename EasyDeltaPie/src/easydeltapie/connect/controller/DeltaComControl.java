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

import easydeltapie.connect.DeltaComPort;
import easydeltapie.connect.DeltaCommand;
import easydeltapie.connect.machine.MachineState;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public abstract class DeltaComControl implements Runnable{
    protected final DeltaComPort com;
    protected boolean initalized = false;
    private boolean running = false;
    protected DeltaComControl(DeltaComPort com){
        this.com = com;
        register();
    }
    private void register(){
        initalized = com.addControl(this);
    }
    public abstract void recieveData(String lineIn, MachineState state);
    @Override
    public abstract void run();
    public abstract boolean allowOthers();
    protected abstract void destroy();
    public void selfdestruct(){
        com.removeInterface(this);
        this.destroy();
    }
    protected void addCommand(String command){
        com.addCommand(new DeltaCommand(this, command));
    }
    protected synchronized void addCommandAndWait(String command){
        addCommand(command);
        running = true;
        while(running){
            try {
                wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(DeltaComControl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    protected synchronized void waitOnMe(int i) {
        if(i<=0){
            i = 100;
        }
        try {
            wait(i);
        } catch (InterruptedException ex) {
            Logger.getLogger(DeltaComControl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void fireCommandComplete(){
        running = false;
        notifyAll();
    }
    
    
}
