/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blt;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rmfaller
 */
class Spinner extends Thread {

    boolean continueToRun = true;
    int sleeptime = 100;
    Result[][] r;

    Spinner(int i, Result[][] r) {
        this.sleeptime = i;
        this.r = r;
    }

    @Override
    public void run() {
        String anim = "|/-\\";
//        String anim = "bBlLtT";
//        String anim = ".oO@*";
//        String anim = "⠁⠂⠄⡀⢀⠠⠐⠈";
        long starttime = new Date().getTime();
        int x = 0;
        long p;
        while (continueToRun) {
            p = 0;
            for (int i = 0; i < r.length; i++) {
                for (int j = 0; j < r[i].length; j++) {
                    p = r[i][j].processed + p;
                }
            }
//            System.out.print("\r Processing " + anim.charAt(x++ % anim.length()) + " for " + ((new Date().getTime()) - starttime) + "ms" + " approximate tasks completed: " + p);
            System.out.print("\r Approximately " + p + " tasks completed in " + ((new Date().getTime()) - starttime) + "ms");
            try {
                Thread.sleep(this.sleeptime);
            } catch (InterruptedException ex) {
                Logger.getLogger(Spinner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}