/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blt;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author rmfaller
 */
class Worker extends Thread {

    private int threadid;
    private Task[] task;
    private JSONObject jobconfig;
    private Workload workload;
    private Result result;
    private int taskset;
    private boolean keepopen = true;

    public Worker() {
    }

    Worker(int j, int i, JSONObject jobconfig, Workload workload, Task[] task, Result result) {
        threadid = j;
        this.jobconfig = jobconfig;
        this.workload = workload;
        this.result = result;
        this.task = task;
        taskset = i;
        try {
            keepopen = (boolean) workload.wlconfig.get("maintain-connection");
        } catch (NullPointerException ex) {
            keepopen = false;
        }
    }


public void run() {
        StringBuffer sb = new StringBuffer(threadid + " : " + workload.jobconfig + "\n\t Taskset " + taskset + ": " + workload.wlconfig + "\n");
        for (int i = 0; i < task.length; i++) {
            sb.append("\t\ttask " + i + ": " + task[i].task + "\n");
        }
        String s = new String(sb + "\n====================================\n");
        result.config = s;
        System.out.println(workload.jobconfig.get("iteration"));
        for (int i = 0; i < (Long) workload.jobconfig.get("iteration"); i++) {
           for (int j = 0; j < task.length; j++) {
               for (int k = 0; k < ((JSONArray)workload.wlconfig.get("task")).get("iteration"); k++) {
                   System.out.println(i + j + k);
               }
           } 
        }
    }
}
