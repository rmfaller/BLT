/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blt;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
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
        JSONObject bltenv = null;
        JSONObject reserved = null;
        JSONObject jobconfig = null;
        JSONObject[] workloadconfig = null;
        JSONObject[][] taskconfig = null;
        JSONArray taska = null;
        Result[][] result = null;
        String confighome = null;
        Worker[][] worker = null;
        boolean csv = false;
        FileReader fr;
        long jobstart = 0;
        long jobstop = 0;
        if (args.length == 0) {
            confighome = "./sample/config.json";
        } else {
            for (int i = 0; i < (args.length); i++) {
                switch (args[i]) {
                    case "-h":
                    case "--help":
                        confighome = null;
                        break;
                    case "-c":
                    case "--csv":
                        csv = true;
                        break;
                    default:
                        confighome = args[args.length - 1];
                        break;
                }
            }
            if (("--help".equals(confighome)) || ("-h".equals(confighome))) {
                confighome = null;
                help();
            }
        }
        if (confighome != null) {
            config = readFile(confighome);
            if (config != null) {
                bltenv = (JSONObject) config.get("BLT-environment");
                reserved = (JSONObject) config.get("BLT-reserved");
                jobconfig = readFile((String) config.get("job"));
//                System.out.println(jobconfig);
                if (jobconfig != null) {
                    JSONArray wla = (JSONArray) jobconfig.get("workload");
                    workloadconfig = new JSONObject[wla.size()];
                    taskconfig = new JSONObject[wla.size()][];
                    worker = new Worker[wla.size()][];
                    result = new Result[wla.size()][];
                    int threadid = 0;
                    boolean found = true;
                    for (int i = 0; i < wla.size(); i++) {
                        workloadconfig[i] = readFile(config.get("workload") + (String) ((JSONObject) wla.get(i)).get("name") + ".json");
                        if (workloadconfig != null) {
                            taska = (JSONArray) workloadconfig[i].get("task");
                            taskconfig[i] = new JSONObject[taska.size()];
                            Long threadcount = (Long) ((JSONObject) wla.get(i)).get("threads");
                            worker[i] = new Worker[threadcount.intValue()];
                            result[i] = new Result[threadcount.intValue()];
                            for (int j = 0; j < taska.size(); j++) {
                                taskconfig[i][j] = readFile(config.get("task") + (String) ((JSONObject) taska.get(j)).get("name") + ".json");
                                if (taskconfig != null) {
                                } else {
                                    found = false;
                                }
                            }
                            for (int j = 0; j < threadcount; j++) {
                                result[i][j] = new Result(j);
                                worker[i][j] = new Worker(threadid, i, jobconfig, workloadconfig[i], taskconfig[i], result[i][j], bltenv, reserved);
                                threadid++;
                            }
                        } else {
                            found = false;
                        }
                    }
                    if (found) {
                        Spinner spinner = new Spinner(128, result);
                        spinner.start();
                        jobstart = new Date().getTime();
                        for (int i = 0; i < worker.length; i++) {
                            for (int j = 0; j < worker[i].length; j++) {
                                worker[i][j].start();
                            }
                        }
                        for (int i = 0; i < worker.length; i++) {
                            for (int j = 0; j < worker[i].length; j++) {
                                try {
                                    worker[i][j].join();
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(BLT.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                        jobstop = new Date().getTime();
                        spinner.continueToRun = false;
                        System.out.println("..... done.");
                        if (csv) {
                            for (int i = 0; i < result.length; i++) {
                                System.out.print("job,thread,workload,task");
                                String[] rattr = result[i][0].getAttributes();
                                for (int j = 0; j < rattr.length; j++) {
                                    System.out.print("," + rattr[j]);
                                }
                                System.out.println();
                                for (int j = 0; j < result[i].length; j++) {
                                    for (int k = 0; k < taska.size(); k++) {
                                        System.out.print(jobconfig.get("name") + "," + result[i][j].uid + "," + workloadconfig[i].get("name") + "," + ((JSONObject) taska.get(k)).get("name"));
                                        for (int l = 0; l < rattr.length; l++) {
                                            System.out.print("," + result[i][j].get(rattr[l]));
                                        }
                                        System.out.println();
                                    }
                                }
                                System.out.println("===================================================");
                            }
                        }
                    } else {
                        help();
                    }
                }
            }
        } else {
            help();
        }
    }

    static void help() {
        String help = "\nBLT usage: java -jar ${BLT_HOME}/dist/BLT.jar [FILE]"
                + "\noptions:"
                + "\n\tif not specified [FILE] defaults to ${BLT_HOME}/sample/config.json\n"
                + "\n\t[FILE] example: ${BLT_HOME}/mytest/myconfig.json\n"
                + "\n\t--csv | -c displays results in a comma delimited format\n"
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
            help();
        }
        return jo;
    }

}
