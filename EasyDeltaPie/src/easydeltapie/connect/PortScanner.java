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

import easydeltapie.EasyDeltaPie;
import gnu.io.CommPortIdentifier;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;




/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */

public class PortScanner implements Runnable{

    private final EasyDeltaPie delta;
    private final JComboBox combo_coms;
    
    public PortScanner(EasyDeltaPie delta){
        this.delta = delta;
        this.combo_coms = delta.getCombo_coms();
    }
    @Override
    public void run() {
        listPorts();
    }
    private void listPorts(){
        ArrayList<DeltaComPort> ports = new ArrayList<DeltaComPort>();
        java.util.Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        while ( portEnum.hasMoreElements()){
            CommPortIdentifier aPort = portEnum.nextElement();
            if(aPort.getPortType() == CommPortIdentifier.PORT_SERIAL){
                ports.add(new DeltaComPort(delta, aPort));
            }
        }
        combo_coms.setModel(new DefaultComboBoxModel(ports.toArray()));
    }
    
    private static String getPortTypeName ( int portType )
    {
        switch ( portType )
        {
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
}

