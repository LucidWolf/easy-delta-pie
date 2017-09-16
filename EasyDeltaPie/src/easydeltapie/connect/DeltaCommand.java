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

import easydeltapie.connect.controller.DeltaComControl;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class DeltaCommand {
    private final DeltaComControl dcc;
    private final String command;
    private boolean running = false;
    public DeltaCommand(DeltaComControl dcc, String command){
        this.dcc = dcc;
        this.command = command;
    }
    public void started(){
        running = true;
    }
    boolean isRunning() {
        return running;
    }
    @Override
    public String toString(){
        return command;
    }

    DeltaComControl getControl() {
        return dcc;
    }
}
