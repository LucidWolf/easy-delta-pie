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

import java.io.IOException;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class FirmwareLoader {
    public void upload() throws IOException{
        String hexfile = "e:\\somefolder\\Blink.cpp.hex";
        String exefile = "D:\\arduino-dev\\arduino-1.0.3\\hardware\\tools\\avr\\bin\\avrdude";
        String conffile = "D:\\arduino-dev\\arduino-1.0.3\\hardware\\tools\\avr\\etc\\avrdude.conf";
        String opts = " -v -v -v -v -patmega328p -carduino -P\\.\\COM8 -b115200 -D -V ";
        String cmd = exefile +" -C"+ conffile + opts +" -Uflash:w:" + hexfile +":i";

        Process proc = Runtime.getRuntime().exec(cmd);
        //int retcode = waitFor(proc);
    }
}
