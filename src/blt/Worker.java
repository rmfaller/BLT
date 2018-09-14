/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blt;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
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
@SuppressWarnings("unchecked")

class Worker extends Thread {

    private int threadid;
    private JSONObject jobconfig;
    private JSONObject workloadconfig;
    private JSONObject[] taskconfig;
    private Result result;
    private int workloadset;
    private JSONObject bltenv = null;
    private JSONObject reserved = null;
    private long threadstartdelay = 0;

    public Worker() {
    }

    Worker(int j, int workloadset, JSONObject jobconfig, JSONObject workloadconfig, JSONObject[] taskconfig, Result result, JSONObject bltenv, JSONObject reserved, long threadstartdelay) {
        this.threadid = j;
        this.workloadset = workloadset;
        this.jobconfig = jobconfig;
        this.workloadconfig = workloadconfig;
        this.taskconfig = taskconfig;
        this.result = result;
        this.bltenv = bltenv;
        this.reserved = reserved;
        this.threadstartdelay = threadstartdelay;
        JSONArray taska = (JSONArray) workloadconfig.get("task");
        for (int i = 0; i < taska.size(); i++) {
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "~passed", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "~passedtime", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "~failed", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "~failedtime", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "~exceeded", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "~exceededtime", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "~skipped", 0);
        }
    }

    @Override
    public void run() {
        int index = 0;
        JSONArray taska = (JSONArray) workloadconfig.get("task");
        JSONArray workloada = (JSONArray) jobconfig.get("workload");
        JSONObject[] state = new JSONObject[taska.size()];
        URL[] url;
        HttpURLConnection[] conn;
        URL[][] turl = new URL[taska.size()][];
        HttpURLConnection[][] tconn = new HttpURLConnection[taska.size()][];
        boolean[][] connopen = new boolean[taska.size()][];
        JSONParser jp = new JSONParser();
        Long minvalue = getLong(0, "minvalue");
        Long maxvalue = getLong(0, "maxvalue");
        Long randomvalue;
        JSONArray slp = null;
        boolean worked;
        boolean retry = false;
        long taskstart = 0;
        long taskstop = 0;
        int instance = 0;
        StringBuilder taskstring;
        FileWriter taskfile;
        Long iteration = (Long) ((JSONObject) workloada.get(workloadset)).get("iteration");
        if ((threadstartdelay > 0) && (iteration > 0)) {
            try {
                Thread.sleep(threadstartdelay);
            } catch (InterruptedException ex) {
                Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        /*        for (int i = 0; i < taska.size(); i++) {
            slp = getJSONArray(i, "service-location-port");
            connopen[i] = new boolean[slp.size()];
            turl[i] = new URL[slp.size()];
            tconn[i] = new HttpURLConnection[slp.size()];
            for (int j = 0; j < slp.size(); j++) {
                connopen[i][j] = false;
            }
        } */
        for (int i = 0; i < iteration; i++) {
            worked = true;
            randomvalue = (long) (Math.random() * ((long) getLong(0, "maxvalue") + 1));
            for (index = 0; index < taska.size(); index++) {
                slp = getJSONArray(index, "service-location-port");
                url = new URL[slp.size()];
                conn = new HttpURLConnection[slp.size()];
                if (slp.size() > 1) {
                    if (getString(index, "loading-style").compareTo("roundrobin") == 0) {
                        instance++;
                        if (instance >= slp.size()) {
                            instance = 0;
                        }
                    }
                    if (getString(index, "loading-style").compareTo("random") == 0) {
                        Double r = (Math.random() * (slp.size() + 1));
                        instance = r.intValue();
                        if (instance >= slp.size()) {
                            instance = 0;
                        }
                    }
                    if (getString(index, "loading-style").compareTo("ha") == 0) {
                        retry = true;
                        instance = 0;
                    }
                }
                // from spl, if greater than 1 select the instance
                // may make conn and url not an array
                if (((String) slp.get(0)).compareTo("$BLT-SLEEP") != 0) {
                    taskstring = new StringBuilder();
                    if (worked) {
                        String urlstring = (String) slp.get(instance) + (String) taskconfig[index].get("url-endpoint") + (String) taskconfig[index].get("url-payload");
                        urlstring = replaceVariable(urlstring);
                        urlstring = updateReserved(index, urlstring, state, minvalue, maxvalue, randomvalue);
                        result.put(((JSONObject) taska.get(index)).get("name").toString() + "~threshold-to-error", getLong(index, "threshold-to-error").longValue());
                        result.put(((JSONObject) taska.get(index)).get("name").toString() + "~threshold-to-fail", getLong(index, "threshold-to-fail").longValue());
                        if ((getLong(index, "threshold-to-fail")) < getLong(index, "threshold-to-error")) {
                            System.err.println("Fail threshold " + getLong(index, "threshold-to-fail") + " set lower than error threshold "
                                    + getLong(index, "threshold-to-error") + ". Rookie mistake. Results will be inconclusive!");
                        }
                        taskstart = new Date().getTime();
                        if (getString(index, "create-file") != null) {
                            String tmps = (String) slp.get(instance);
                            tmps = replaceVariable(tmps);
                            tmps = updateReserved(index, tmps, state, minvalue, maxvalue, randomvalue);
                            taskstring.append("{ \"name\": \"" + getString(index, "name") + "\",");
                            taskstring.append("\"request\": \"" + getString(index, "request") + "\", \"service-location-port\": [\"" + tmps + "\"],");
                            tmps = (String) taskconfig[index].get("url-endpoint");
                            tmps = replaceVariable(tmps);
                            tmps = updateReserved(index, tmps, state, minvalue, maxvalue, randomvalue);
                            taskstring.append("\"url-endpoint\": \"" + tmps + "\",");
                            tmps = (String) taskconfig[index].get("url-payload");
                            tmps = replaceVariable(tmps);
                            tmps = updateReserved(index, tmps, state, minvalue, maxvalue, randomvalue);
                            taskstring.append("\"url-payload\": \"" + tmps + "\", \"header\": {");
                        }
                        try {
                            url[instance] = new URI(urlstring).toURL();
                            conn[instance] = (HttpURLConnection) url[instance].openConnection();
                            conn[instance].setDoOutput(true);
                            conn[instance].setConnectTimeout(getLong(index, "threshold-to-fail").intValue());
                            conn[instance].setReadTimeout(getLong(index, "threshold-to-fail").intValue());
                            conn[instance].setRequestMethod(getString(index, "request"));
                            Iterator<String> iter = ((JSONObject) taskconfig[index].get("header")).keySet().iterator();
                            while (iter.hasNext()) {
                                String headerattr = iter.next();
                                String headervalue = (String) ((JSONObject) taskconfig[index].get("header")).get(headerattr);
                                headerattr = replaceVariable(headerattr);
                                headervalue = replaceVariable(headervalue);
                                headerattr = updateReserved(index, headerattr, state, minvalue, maxvalue, randomvalue);
                                headervalue = updateReserved(index, headervalue, state, minvalue, maxvalue, randomvalue);
                                taskstring.append("\"" + headerattr + "\": \"" + headervalue + "\"");
                                if (iter.hasNext()) {
                                    taskstring.append(",");
                                }
                                conn[instance].setRequestProperty(headerattr, headervalue);
                            }
                            taskstring.append("}");
                            String dp = null;
                            if (taskconfig[index].containsKey("data-payload")) {
                                dp = ((JSONObject) taskconfig[index].get("data-payload")).toString();
                                dp = replaceVariable(dp);
                                dp = updateReserved(index, dp, state, minvalue, maxvalue, randomvalue);
                                taskstring.append(",\"data-payload\": " + dp);
                                OutputStreamWriter cwr = new OutputStreamWriter(conn[instance].getOutputStream());
                                cwr.write(dp);
                                cwr.close();
                            }
                            taskstring.append("}");
                            taskfile = new FileWriter("./bulk-task/" + (String) ((JSONObject) workloada.get(workloadset)).get("name") + "-"
                                    + ((JSONObject) taska.get(index)).get("name").toString() + "-" + this.threadid, true);
                            taskfile.append(taskstring.toString());
                            taskfile.close();
//                            System.out.println(taskstring.toString());
                            BufferedReader reader = null;
                            reader = new BufferedReader(new InputStreamReader(conn[instance].getInputStream()));
                            String rl = null;
                            StringBuilder sbin = new StringBuilder();
                            while ((rl = reader.readLine()) != null) {
                                sbin.append(rl);
                            }
                            reader.close();
                            state[index] = (JSONObject) jp.parse(sbin.toString());
                        } catch (URISyntaxException | ParseException | IOException ex) {
                            worked = false;
                            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
                            if (retry) {
                                instance++;
                                if (instance >= slp.size()) {
                                    instance = 0;
                                    retry = false;
                                }
                            }
                        }
                        taskstop = new Date().getTime();
                        if (worked) {
                            if ((getLong(index, "threshold-to-error")) >= (taskstop - taskstart)) {
                                result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "~passedtime", (taskstop - taskstart));
                                result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "~passed", 1);
                            } else {
                                result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "~exceededtime", (taskstop - taskstart));
                                result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "~exceeded", 1);
                            }
                        } else {
                            result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "~failedtime", (taskstop - taskstart));
                            result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "~failed", 1);
                            if (getBoolean(index, "continue-on-fail")) {
                                worked = true;
                            }
                        }
                    } else {
                        result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "~skipped", 1);
                    }
                } else {
                    try {
                        Thread.sleep((Long) taskconfig[index].get("sleep-time"));
                        result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "~passedtime", (Long) taskconfig[index].get("sleep-time"));
                        result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "~passed", 1);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            minvalue++;
            maxvalue--;
            if (minvalue > getLong(0, "maxvalue")) {
                minvalue = getLong(0, "minvalue");
            }
            if (maxvalue < getLong(0, "minvalue")) {
                maxvalue = getLong(0, "maxvalue");
            }
        }
        StringBuilder sb = new StringBuilder("Job: \n" + jobconfig.toJSONString() + "\n\t Workload(s): \n\t" + workloadconfig.toJSONString() + "\n\t\tTask(s): \n");
        StringBuilder cb = new StringBuilder();
        for (int i = 0; i < taskconfig.length; i++) {
            sb.append("\t\t" + taskconfig[i].toJSONString()).append("\n");
        }
        result.config = sb.toString();
        for (int i = 0; i < taskconfig.length; i++) {
            if (taskconfig[i].get("request") != null) {
                cb.append("\t\tcurl --request ").append((String) taskconfig[i].get("request")).append(" \\\n");
                JSONObject c = (JSONObject) taskconfig[i].get("header");
                for (Object key : c.keySet()) {
                    cb.append("\t\t\t--header \"" + (String) key + ":" + c.get(key) + "\" \\\n");
                }
                if (taskconfig[i].containsKey("data-payload")) {
                    c = (JSONObject) taskconfig[i].get("data-payload");
                    cb.append("\t\t\t--data \"" + c.toJSONString() + "\" \\\n");
                }
                cb.append("\t\t\t" + getJSONArray(i, "service-location-port").get(0) + taskconfig[i].get("url-endpoint") + taskconfig[i].get("url-payload") + "\n");
            }
        }
        result.curler = cb.toString();
    }

    private String getString(int index, String key) {
        String value = null;
        JSONArray ja;
        if (taskconfig[index].containsKey(key)) {
            value = (String) taskconfig[index].get(key);
        } else {
            ja = (JSONArray) workloadconfig.get("task");
            if ((((JSONObject) ja.get(index)).containsKey(key)) && (value == null)) {
                value = (String) (((JSONObject) ja.get(index)).get(key));
            } else {
                if ((workloadconfig.containsKey(key)) && (value == null)) {
                    value = (String) workloadconfig.get(key);
                } else {
                    ja = (JSONArray) jobconfig.get("workload");
                    for (int i = 0; i < ja.size(); i++) {
                        if ((((String) ((JSONObject) ja.get(i)).get("name")).compareTo((String) (workloadconfig.get("name"))) == 0) && (value == null)) {
                            if (((JSONObject) ja.get(i)).containsKey(key)) {
                                value = (String) (((JSONObject) ja.get(i)).get(key));
                            }
                        }
                    }
                    if ((jobconfig.containsKey(key)) && (value == null)) {
                        value = (String) jobconfig.get(key);
                    } else {
                        value = null;
                    }
                }
            }
        }
        return value;
    }

    private boolean getBoolean(int index, String key) {
        boolean value = true;
        boolean found = false;
        JSONArray ja;
        if (taskconfig[index].containsKey(key)) {
            value = (boolean) taskconfig[index].get(key);
            found = true;
        } else {
            ja = (JSONArray) workloadconfig.get("task");
            if ((((JSONObject) ja.get(index)).containsKey(key)) && (!found)) {
                value = (boolean) (((JSONObject) ja.get(index)).get(key));
                found = true;
            } else {
                if ((workloadconfig.containsKey(key)) && (!found)) {
                    value = (boolean) workloadconfig.get(key);
                    found = true;
                } else {
                    ja = (JSONArray) jobconfig.get("workload");
                    for (int i = 0; i < ja.size(); i++) {
                        if ((((String) ((JSONObject) ja.get(i)).get("name")).compareTo((String) (workloadconfig.get("name"))) == 0) && (!found)) {
                            if (((JSONObject) ja.get(i)).containsKey(key)) {
                                value = (boolean) (((JSONObject) ja.get(i)).get(key));
                                found = true;
                            }
                        }
                    }
                    if ((jobconfig.containsKey(key)) && (!found)) {
                        value = (boolean) jobconfig.get(key);
                    } else {
                        if (!found) {
                            value = false;
                        }
                    }
                }
            }
        }
        return value;
    }

    private Long getLong(int index, String key) {
        Long value = null;
        JSONArray ja;
        if (taskconfig[index].containsKey(key)) {
            value = (Long) taskconfig[index].get(key);
        } else {
            ja = (JSONArray) workloadconfig.get("task");
            if ((((JSONObject) ja.get(index)).containsKey(key)) && (value == null)) {
                value = (Long) (((JSONObject) ja.get(index)).get(key));
            } else {
                if ((workloadconfig.containsKey(key)) && (value == null)) {
                    value = (Long) workloadconfig.get(key);
                } else {
                    ja = (JSONArray) jobconfig.get("workload");
                    for (int i = 0; i < ja.size(); i++) {
                        if ((((String) ((JSONObject) ja.get(i)).get("name")).compareTo((String) (workloadconfig.get("name"))) == 0) && (value == null)) {
                            if (((JSONObject) ja.get(i)).containsKey(key)) {
                                value = (Long) (((JSONObject) ja.get(i)).get(key));
                            }
                        }
                    }
                    if ((jobconfig.containsKey(key)) && (value == null)) {
                        value = (Long) jobconfig.get(key);
                    } else {
                        if (value == null) {
                            value = new Long(0);
                        }
                    }
                }
            }
        }
        return value;
    }

    private JSONArray getJSONArray(int index, String key) {
        JSONArray ja;
        if (taskconfig[index].containsKey(key)) {
            ja = (JSONArray) taskconfig[index].get(key);
        } else {
            if (workloadconfig.containsKey(key)) {
                ja = (JSONArray) workloadconfig.get(key);
            } else {
                ja = (JSONArray) jobconfig.get(key);
            }
        }
        return ja;
    }

    private String replaceVariable(String tmpstring) {
        String ks = null;
        String kv = null;
        for (Object key : bltenv.keySet()) {
            ks = (String) key;
            if (bltenv.get(key).getClass() == Long.class) {
                kv = ((Long) bltenv.get(ks)).toString();
            } else {
                if (bltenv.get(key).getClass() == String.class) {
                    kv = (String) bltenv.get(ks);
                } else {
                    kv = ((Boolean) bltenv.get(ks)).toString();
                }
            }
            tmpstring = tmpstring.replace(ks, kv);
        }
        return tmpstring;
    }

    private String updateReserved(int index, String tmpstring, JSONObject[] state, Long minvalue, Long maxvalue, Long randomvalue) {
        String ks = null;
        String kv = null;
        for (Object key : reserved.keySet()) {
            ks = (String) key;
            switch (ks) {
                case "$BLT-RANDOM-NUMBER":
                    tmpstring = tmpstring.replace(ks, randomvalue.toString());
                    break;
                case "$BLT-TOKEN-PAYLOAD":
                    kv = (String) reserved.get(ks);
                    boolean found = false;
                    int i = state.length - 1;
                    while ((!found) && (i >= 0)) {
                        if (state[i] != null) {
                            if (state[i].containsKey(kv)) {
                                tmpstring = tmpstring.replace(ks, (String) state[i].get(kv));
                                found = true;
                            }
                        }
                        i--;
                    }
                    break;
                case "$BLT-INCREMENT":
                    tmpstring = tmpstring.replace(ks, minvalue.toString());
                    break;
                case "$BLT-DECREMENT":
                    tmpstring = tmpstring.replace(ks, maxvalue.toString());
                    break;
                case "$BLT-THREADID":
                    tmpstring = tmpstring.replace(ks, new Integer(this.threadid).toString());
                    break;
                default:
                    break;
            }
        }
        return tmpstring;
    }

}
