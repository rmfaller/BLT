/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blt;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
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
        boolean showjob = false;
        boolean showcurl = false;
        boolean summary = false;
        boolean progress = false;
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
                    case "-v":
                    case "--csv":
                        csv = true;
                        break;
                    case "-c":
                    case "--curl":
                        showcurl = true;
                        break;
                    case "-j":
                    case "--job":
                        showjob = true;
                        break;
                    case "-s":
                    case "--summary":
                        summary = true;
                        break;
                    case "-p":
                    case "--progress":
                        progress = true;
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
                            Long threadgroupsize = (Long) ((JSONObject) wla.get(i)).get("thread-group-size");
                            Long threadinterval = (Long) ((JSONObject) wla.get(i)).get("thread-interval");
                            worker[i] = new Worker[threadcount.intValue()];
                            result[i] = new Result[threadcount.intValue()];
                            for (int j = 0; j < taska.size(); j++) {
                                taskconfig[i][j] = readFile(config.get("task") + (String) ((JSONObject) taska.get(j)).get("name") + ".json");
                                if (taskconfig != null) {
                                } else {
                                    found = false;
                                }
                            }
                            long startdelay = (long) ((JSONObject) wla.get(i)).get("start-delay");
                            long threadstartdelay = 0;
                            for (int j = 0; j < threadcount; j++) {
                                result[i][j] = new Result(j);
                                if (threadgroupsize > 0) {
                                    if ((j % threadgroupsize) == 0) {
                                        threadstartdelay = ((j % threadgroupsize) + (startdelay * threadinterval));
                                        startdelay++;
                                    }
                                }
                                worker[i][j] = new Worker(threadid, i, jobconfig, workloadconfig[i], taskconfig[i], result[i][j], bltenv, reserved, threadstartdelay);
                                threadid++;
                            }
                        } else {
                            found = false;
                        }
                    }
                    if (found) {
                        Spinner spinner = new Spinner(128, result);
                        if (progress) {
                            spinner.start();
                        }
                        jobstart = new Date().getTime();
                        for (int i = 0; i < worker.length; i++) {
                            for (int j = 0; j < worker[i].length; j++) {
                                // if workloadconfig[i] != serial - dumb idea; if serial create multiple BLT commands 
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
                        if (progress) {
                            spinner.continueToRun = false;
                            System.out.println("..... done. Total lapsed time = " + (jobstop - jobstart) + "ms\n");
                        }
                        long millis = (jobstop - jobstart);
                        long days = TimeUnit.MILLISECONDS.toDays(millis);
                        millis -= TimeUnit.DAYS.toMillis(days);
                        long hours = TimeUnit.MILLISECONDS.toHours(millis);
                        millis -= TimeUnit.HOURS.toMillis(hours);
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
                        millis -= TimeUnit.MINUTES.toMillis(minutes);
                        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
                        StringBuilder sbt = new StringBuilder(64);
                        sbt.append(days);
                        sbt.append(" Days ");
                        sbt.append(hours);
                        sbt.append(" Hours ");
                        sbt.append(minutes);
                        sbt.append(" Minutes ");
                        sbt.append(seconds);
                        sbt.append(" Seconds");
                        for (int i = 0; i < result.length; i++) {
                            if ((Long) ((JSONObject) ((JSONArray) jobconfig.get("workload")).get(i)).get("threads") > 0) {
                                if (csv) {
                                    System.out.println("CSV for job = " + jobconfig.get("name") + " and workload = " + workloadconfig[i].get("name")
                                            + " completed on " + new Date().toString() + " running for " + sbt.toString() + " :");
                                    System.out.print("job,thread,workload");
                                    String[] rattr = result[i][0].getAttributes();
                                    Arrays.sort(rattr);
                                    for (int j = 0; j < rattr.length; j++) {
                                        System.out.print("," + rattr[j]);
                                    }
                                    System.out.println();
                                    for (int j = 0; j < result[i].length; j++) {
                                        System.out.print(jobconfig.get("name") + "," + result[i][j].uid + "," + workloadconfig[i].get("name"));
                                        for (int l = 0; l < rattr.length; l++) {
                                            System.out.print("," + result[i][j].get(rattr[l]));
                                        }
                                        System.out.println();
                                    }
                                    System.out.println();
                                }
                                if (showjob) {
                                    System.out.println("Configuration for job = " + jobconfig.get("name") + " and workload = " + workloadconfig[i].get("name")
                                            + " completed on " + new Date().toString() + " running for " + sbt.toString() + ":");
                                    System.out.println(config.toString());
                                    System.out.println(result[i][0].config);
                                }
                                if (showcurl) {
                                    System.out.println("Sample cURL commands for job = " + jobconfig.get("name") + " and workload = " + workloadconfig[i].get("name")
                                            + " completed on " + new Date().toString() + " running for " + sbt.toString() + ":");
                                    System.out.println(result[i][0].curler);
                                }
                            }
                        }
                        if (summary) {
                            summary(result, jobconfig, workloadconfig, sbt);
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
                + "\n\t[FILE] example: ${BLT_HOME}/mytest/myconfig.json and does require a layout similar to ${BLT_HOME}/sample/\n"
                + "\n\t--csv      | -v displays results in a comma delimited format\n"
                + "\n\t--curl     | -c displays example cURL commands used against REST endpoints for this test\n"
                + "\n\t--job      | -j displays JSON configuration data used for the test\n"
                + "\n\t--summary  | -s displays summary of test\n"
                + "\n\t--progress | -p displays progress while BLT is running\n"
                + "\n\t--help     | -h this output\n"
                + "\nExamples:"
                + "";
        System.out.println(help);
        System.out.println("java -jar ${BLT_HOME}/dist/BLT.jar\n");
        System.out.println("java -jar ${BLT_HOME}/dist/BLT.jar --csv --curl --job --summary --progress ${BLT_HOME}/mytest/myconfig.json\n");
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

    private static void summary(Result[][] result, JSONObject jobconfig, JSONObject[] workloadconfig, StringBuilder sbt) {
        Result[] tr = new Result[result.length];
        for (int i = 0; i < result.length; i++) {
            if ((Long) ((JSONObject) ((JSONArray) jobconfig.get("workload")).get(i)).get("threads") > 0) {
                System.out.println("Summary for job = " + jobconfig.get("name") + " and workload = " + workloadconfig[i].get("name")
                        + " completed on " + new Date().toString() + " running for " + sbt.toString() + ":");
                System.out.printf("%-23s", "Operation");
                System.out.printf("%4s", " Thds");
                System.out.printf("%8s", " TxTotal");
                System.out.printf("%10s", "AccmTime");
                System.out.printf("%5s", "T2E");
                System.out.printf("%5s", "T2F");
                System.out.printf("%10s", "  TxPass");
                System.out.printf("%10s", "PassTime");
                System.out.printf("%10s", " TxExced");
                System.out.printf("%10s", "ExcdTime");
                System.out.printf("%10s", "  TxFail");
                System.out.printf("%10s", "Failtime");
                System.out.printf("%10s", " Skipped");
                System.out.printf("%12s", "CbdPsOps");
                System.out.printf("%10s", " ThrdOps");
                System.out.printf("%10s", "Avrms/op");
                System.out.printf("%9s", " Success");
                System.out.printf("%9s", "  Exceed");
                System.out.printf("%9s", "    Fail");
                System.out.println();
                String[] rattr = result[i][0].getAttributes();
                Arrays.sort(rattr);
                int taskcount = 0;
                tr[i] = new Result(i);
                for (int r = 0; r < rattr.length; r++) {
                    tr[i].put(rattr[r], 0);
                    if ((!rattr[r].endsWith("~threshold-to-fail")) && (!rattr[r].endsWith("~threshold-to-error"))) {
                        for (int j = 0; j < result[i].length; j++) {
                            tr[i].addTo(rattr[r], result[i][j].get(rattr[r]));
                        }
                    } else {
                        if (rattr[r].endsWith("~threshold-to-fail")) {
                            tr[i].addTo(rattr[r], result[i][0].get(rattr[r]));
                        }
                        if (rattr[r].endsWith("~threshold-to-error")) {
                            tr[i].addTo(rattr[r], result[i][0].get(rattr[r]));
                        }
                    }
                    if (rattr[r].endsWith("~passed")) {
                        taskcount++;
                    }
//                System.out.println(rattr[r] + " = " + tr[i].get(rattr[r]));
                }
                String[] tasks = new String[taskcount];
                int x = 0;
                for (int r = 0; r < rattr.length; r++) {
                    if (rattr[r].endsWith("~passed")) {
                        tasks[x] = new String(rattr[r].split("~passed")[0]);
                        x++;
                    }
                }
                long totaltxtotal = 0;
                long totalaccmtime = 0;
                long totalpassed = 0;
                long totalpassedtime = 0;
                long totalexceeded = 0;
                long totalexceededtime = 0;
                long totalfailed = 0;
                long totalfailedtime = 0;
                long totalskipped = 0;
                float totalops = 0;
                float totalthreadops = 0;
                boolean include;
                for (int t = 0; t < tasks.length; t++) {
                    if ((tr[i].get(tasks[t] + "~threshold-to-error") > 0) || (tr[i].get(tasks[t] + "~threshold-to-fail") > 0) || (tr[i].get(tasks[t] + "~skipped") > 0)) {
                        include = true;
                    } else {
                        include = false;
                    }
                    System.out.printf("%-24s", tasks[t]);
                    System.out.printf("%4s", result[i].length);
                    long txtotal = 0;
                    long accmtime = 0;
                    long t2e = 0;
                    long t2f = 0;
                    long passed = 0;
                    long passedtime = 0;
                    long exceeded = 0;
                    long exceededtime = 0;
                    long failed = 0;
                    long failedtime = 0;
                    long skipped = 0;
                    float ops = 0;
                    float threadops = 0;
                    for (int r = 0; r < rattr.length; r++) {
                        StringBuilder sb = new StringBuilder();
                        String[] sa = rattr[r].split("~");
                        for (int s = 0; s < (sa.length - 1); s++) {
                            sb.append(sa[s]);
                        }
//                        System.out.println(sb.toString() + "----" + tasks[t]);
//                       if (rattr[r].startsWith(tasks[t])) {
                        if ((tasks[t]).compareTo(sb.toString()) == 0) {
                            if ((rattr[r].endsWith("~threshold-to-error"))) {
                                t2e = tr[i].get(rattr[r]);
                            }
                            if ((rattr[r].endsWith("~threshold-to-fail"))) {
                                t2f = tr[i].get(rattr[r]);
                            }
                            if ((rattr[r].endsWith("~skipped"))) {
                                skipped = skipped + tr[i].get(rattr[r]);
                                if (include) {
                                    totalskipped = totalskipped + tr[i].get(rattr[r]);
                                }
                            }
                            if ((rattr[r].endsWith("~passed")) || (rattr[r].endsWith("~exceeded")) || (rattr[r].endsWith("~failed"))) {
                                txtotal = txtotal + tr[i].get(rattr[r]);
                                if (include) {
                                    totaltxtotal = totaltxtotal + tr[i].get(rattr[r]);
                                }
                                if (rattr[r].endsWith("~passed")) {
                                    passed = passed + tr[i].get(rattr[r]);
                                    if (include) {
                                        totalpassed = totalpassed + tr[i].get(rattr[r]);
                                    }
                                }
                                if (rattr[r].endsWith("~exceeded")) {
                                    exceeded = exceeded + tr[i].get(rattr[r]);
                                    if (include) {
                                        totalexceeded = totalexceeded + tr[i].get(rattr[r]);
                                    }
                                }
                                if (rattr[r].endsWith("~failed")) {
                                    failed = failed + tr[i].get(rattr[r]);
                                    if (include) {
                                        totalfailed = totalfailed + tr[i].get(rattr[r]);
                                    }
                                }
                            }
                            if ((rattr[r].endsWith("~passedtime")) || (rattr[r].endsWith("~exceededtime")) || (rattr[r].endsWith("~failedtime"))) {
                                accmtime = accmtime + tr[i].get(rattr[r]);
                                if (include) {
                                    totalaccmtime = totalaccmtime + tr[i].get(rattr[r]);
                                }
                                if (rattr[r].endsWith("~passedtime")) {
                                    passedtime = passedtime + tr[i].get(rattr[r]);
                                    if (include) {
                                        totalpassedtime = totalpassedtime + tr[i].get(rattr[r]);
                                    }
                                }
                                if (rattr[r].endsWith("~exceededtime")) {
                                    exceededtime = exceededtime + tr[i].get(rattr[r]);
                                    if (include) {
                                        totalexceededtime = totalexceededtime + tr[i].get(rattr[r]);
                                    }
                                }
                                if (rattr[r].endsWith("~failedtime")) {
                                    failedtime = failedtime + tr[i].get(rattr[r]);
                                    if (include) {
                                        totalfailedtime = totalfailedtime + tr[i].get(rattr[r]);
                                    }
                                }
                            }
                        }
                    }
                    System.out.format("%8s", txtotal);
                    System.out.format("%10s", accmtime);
                    System.out.format("%5s", t2e);
                    System.out.format("%5s", t2f);
                    System.out.format("%10s", passed);
                    System.out.format("%10s", passedtime);
                    System.out.format("%10s", exceeded);
                    System.out.format("%10s", exceededtime);
                    System.out.format("%10s", failed);
                    System.out.format("%10s", failedtime);
                    System.out.format("%10s", skipped);
                    ops = (((passed + exceeded) / (float) (passedtime + exceededtime)) * 1000) * result[i].length;
                    System.out.format("%10.2f%s", ops, "/s");
                    threadops = (ops / result[i].length);
//                    threadops = (((passed + exceeded) / (float) (passedtime + exceededtime)) * 1000) / (float) result[i].length;
                    System.out.format("%8.2f%s", threadops, "/s");
                    if (include) {
                        totalops = totalops + ops;
                        totalthreadops = totalthreadops + threadops;
                    }
                    System.out.format("%8.2f%s", ((passedtime + exceededtime) / (float) (passed + exceeded)) / (float) result[i].length, "ms");
                    System.out.format("%8.2f%s", (passed / (float) txtotal) * 100, "%");
                    System.out.format("%8.2f%s", (exceeded / (float) txtotal) * 100, "%");
                    System.out.format("%8.2f%s", (failed / (float) txtotal) * 100, "%");
                    System.out.println();
                }

                System.out.printf("%-24s", "Totals");
                System.out.printf("%4s", result[i].length);
                System.out.format("%8s", totaltxtotal);
                System.out.format("%10s", totalaccmtime);
                System.out.format("%5s", "     ");
                System.out.format("%5s", "     ");
                System.out.format("%10s", totalpassed);
                System.out.format("%10s", totalpassedtime);
                System.out.format("%10s", totalexceeded);
                System.out.format("%10s", totalexceededtime);
                System.out.format("%10s", totalfailed);
                System.out.format("%10s", totalfailedtime);
                System.out.format("%10s", totalskipped);
                System.out.format("%10.2f%s", totalops, "/s");
                System.out.format("%8.2f%s", totalthreadops, "/s");
                System.out.format("%8.2f%s", ((totalpassedtime + totalexceededtime) / (float) (totalpassed + totalexceeded)) / (float) result[i].length, "ms");
                System.out.println("\nexcludes sleep & skipped\n");
            }
        }
    }
}
