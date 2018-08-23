/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blt;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author rmfaller
 */
public class BLT {

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        JSONParser jsonparser = new JSONParser();
        JSONObject config = null;
        JSONObject jobconfig = null;
        JSONObject wlconfig = null;
        JSONObject taskconfig = null;
        Result[][] result = null;
        String confighome = null;
        Workload[] workload = null;
        Task[][] task = null;
        Worker[][] worker = null;
        FileReader fr;
        if (args.length == 0) {
            confighome = "./sample/config.json";
        } else {
            confighome = args[0];
            if (("--help".equals(confighome)) || ("-h".equals(confighome))) {
                confighome = null;
                help();
            }
        }
        if (confighome != null) {
            config = readFile(confighome);
            if (config != null) {
                jobconfig = readFile((String) config.get("job"));
                if (jobconfig != null) {
                    JSONArray wla = (JSONArray) jobconfig.get("workload");
                    workload = new Workload[wla.size()];
                    task = new Task[wla.size()][];
                    worker = new Worker[wla.size()][];
                    result = new Result[wla.size()][];
                    int threadid = 0;
                    boolean found = true;
                    for (int i = 0; i < wla.size(); i++) {
                        wlconfig = readFile(config.get("workload") + (String) ((JSONObject) wla.get(i)).get("name") + ".json");
                        if (wlconfig != null) {
                            JSONArray taska = (JSONArray) wlconfig.get("task");
                            task[i] = new Task[taska.size()];
                            Long threadcount = (Long) ((JSONObject) wla.get(i)).get("threads");
                            worker[i] = new Worker[threadcount.intValue()];
                            result[i] = new Result[threadcount.intValue()];
                            for (int j = 0; j < taska.size(); j++) {
                                taskconfig = readFile(config.get("task") + (String) ((JSONObject) taska.get(j)).get("name") + ".json");
                                if (taskconfig != null) {
                                    task[i][j] = new Task(taskconfig);
                                } else {
                                    found = false;
                                }
                            }
                            workload[i] = new Workload((JSONObject) wla.get(i), wlconfig, task[i]);
                            for (int j = 0; j < threadcount; j++) {
                                result[i][j] = new Result();
                                worker[i][j] = new Worker(threadid, i, jobconfig, workload[i], task[i], result[i][j]);
                                threadid++;
                            }
                        } else {
                            found = false;
                        }
                    }
                    if (found) {
                        for (int i = 0; i < worker.length; i++) {
                            for (int j = 0; j < worker[i].length; j++) {
                                worker[i][j].start();
                            }
                        }
                        for (int i = 0; i < worker.length; i++) {
                            for (int j = 0; j < worker[i].length; j++) {
                                try {
                                    worker[i][j].join();
//                                    System.out.println("Workload = " + i + " + Thread = " + j + " :: " + result[i][j].config);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(BLT.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    } else {
                        help();
                    }
                }
            }
        }
    }

    static void help() {
        String help = "\nBLT usage: java -jar ${BLT_HOME}/dist/BLT.jar [FILE]"
                + "\noptions:"
                + "\n\tif not specified [FILE] defaults to ${BLT_HOME}/sample/config.json\n"
                + "\n\t[FILE] example: ${BLT_HOME}/mytest/myconfig.json\n"
                + "\n\t--help | -h this output\n"
                + "\nExamples:"
                + "";
        System.out.println(help);
        System.out.println("java -jar ${BLT_HOME}/dist/BLT.jar\n");
        System.out.println("java -jar ${BLT_HOME}/dist/BLT.jar ${BLT_HOME}/mytest/myconfig.json\n");
    }

    private static JSONObject readFile(String filename) {
        FileReader fr;
        JSONObject jo;
        JSONParser jsonparser = new JSONParser();
        try {
            fr = new FileReader(filename);
            jo = (JSONObject) jsonparser.parse(fr);
        } catch (IOException | ParseException ex) {
            Logger.getLogger(BLT.class.getName()).log(Level.SEVERE, null, ex);
            jo = null;
        }
        return jo;
    }

}
