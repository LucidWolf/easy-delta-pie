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
import easydeltapie.connect.machine.MachineState;
import java.util.ArrayList;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class SingleCommandControl extends DeltaComControl{
    private final ArrayList<String> commands = new ArrayList<>();
    public SingleCommandControl(DeltaComPort com, String command){
        super(com);
        this.commands.add(command);
    }
    public SingleCommandControl(DeltaComPort com, ArrayList<String> commands){
        super(com);
        this.commands.addAll(commands);
    }
    @Override
    public void recieveData(String lineIn, MachineState dataType) {
        
    }

    @Override
    public boolean allowOthers() {
        return false;
    }

    @Override
    public void run() {
        for(String command:commands){
            this.addCommandAndWait(command);
        }
        this.addCommandAndWait("M114", true);
        this.selfdestruct();
    }

    @Override
    public void destroy() {
        // nothing to do
    }
    
}
