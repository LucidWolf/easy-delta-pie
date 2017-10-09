/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package easydeltapie;

/**
 *
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class RodLengthCalc {

    /**
    public static void main(String[] args) {
        // TODO code application logic here
        float test = getNewRodLenght(65.0f, 210.0f, 95.0f, 60.0f);
    }
    **/
    public static float getNewRodLenght(float l, float dx, float r, float lx) {
        double eps = 0.001;
        // l = actual from current rod
        // dx = current rod lenght
        // r = rod radius at zero height
        // lx = length eXspected
        // calculate delta H based on lx and rod
        // d = wanted rod lenght
        double dh = Math.sqrt(dx*dx-(r-lx)*(r-lx))- Math.sqrt(dx*dx - r*r);
        // differ from purple here Equation 3 going to 
        // solve analiticaly so take Equ 3 set to zero and GO NEWT!!
        int loopMax = 5;
        double d = dx;
        for(int i = 0; i < loopMax; i++){
            double act = equation3(d,r,dh,l);
            //System.out.println("Loop "+i+": d="+d+" act="+act);
            if(Math.abs(act) < eps){
                //debug
                //System.out.println("Oh yeah in "+i);
                break;
            }
            double der = (equation3(d+eps,r,dh,l) - equation3(d,r,dh,l))/eps;
            d = d-act/der;
        }
        return (float)d;        
    }
    public static double equation3(double d, double r, double dh, double l){
        return r-l-Math.sqrt(d*d-Math.pow(Math.sqrt(d*d-r*r)+dh,2));
    }


}
