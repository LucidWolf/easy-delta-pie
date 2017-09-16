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
package easydeltapie.connect.machine.states;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class EndStopValue {
    private final String eStop;
    private final boolean high;

    public EndStopValue(String eStop, boolean high) {
        this.eStop = eStop;
        this.high = high;
    }
    public boolean isHigh(){
        return high;
    }
    @Override
    public String toString(){
        return eStop;
    }
}
