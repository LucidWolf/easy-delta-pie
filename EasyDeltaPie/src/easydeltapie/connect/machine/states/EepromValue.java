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
public class EepromValue implements Comparable{
    public static final int BIT = 0;
    public static final int INT16 = 1;
    public static final int INT32 = 2;
    public static final int FLOAT = 3;
    
    private final int type;
    private final int position;
    private final String value;
    private final String desc;
    public EepromValue(int type, int position, String value, String desc){
        this.type = type;
        this.position = position;
        this.value = value;
        this.desc = desc;
    }
    @Override
    public String toString(){
        return desc;
    }
    public String getValue(){
        return value;
    }
    public int getType(){
        return type;
    }
    public boolean checkValue(String aValue){
        boolean out = false;
        if(type == EepromValue.BIT){
            try{
                Boolean.parseBoolean(aValue);
                out = true;
            }catch(Exception e){}
        }else if(type == EepromValue.INT16 || type == EepromValue.INT32){
            try{
                Integer.parseInt(aValue);
                out = true;
            }catch(Exception e){}
        }else if(type == EepromValue.FLOAT){
            try{
                Float.parseFloat(aValue);
                out = true;
            }catch(Exception e){}
        }
        return out;
    }

    public int getPosition() {
        return position;
    }

    public float getValueAsFloat() {
        float out = 0.0f;
        try{out = Float.parseFloat(value);}catch(Exception e){}
        return out;
    }

    @Override
    public int compareTo(Object o) {
        return this.toString().compareTo(o.toString());
    }
}
