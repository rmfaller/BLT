/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
    private long waittostart = 0;

    public Worker() {
    }

    Worker(int j, int workloadset, JSONObject jobconfig, JSONObject workloadconfig, JSONObject[] taskconfig, Result result, JSONObject bltenv, JSONObject reserved, long waittostart) {
        this.threadid = j;
        this.workloadset = workloadset;
        this.jobconfig = jobconfig;
        this.workloadconfig = workloadconfig;
        this.taskconfig = taskconfig;
        this.result = result;
        this.bltenv = bltenv;
        this.reserved = reserved;
        this.waittostart = waittostart;
        JSONArray taska = (JSONArray) workloadconfig.get("task");
        for (int i = 0; i < taska.size(); i++) {
//            if ((((String) ((JSONObject) taska.get(i)).get("name")).compareTo("sleep")) != 0) {
//                System.out.println("name=" + ((JSONObject) taska.get(i)).get("name").toString());
/*                result.put(this.threadid + "-" + ((JSONObject) taska.get(i)).get("name").toString() + "-passed", 0);
                result.put(this.threadid + "-" + ((JSONObject) taska.get(i)).get("name").toString() + "-passedtime", 0);
                result.put(this.threadid + "-" + ((JSONObject) taska.get(i)).get("name").toString() + "-failed", 0);
                result.put(this.threadid + "-" + ((JSONObject) taska.get(i)).get("name").toString() + "-failedtime", 0);
                result.put(this.threadid + "-" + ((JSONObject) taska.get(i)).get("name").toString() + "-exceeded", 0);
                result.put(this.threadid + "-" + ((JSONObject) taska.get(i)).get("name").toString() + "-exceededtime", 0);
                result.put(this.threadid + "-" + ((JSONObject) taska.get(i)).get("name").toString() + "-threshold-to-error", getLong(i, "threshold-to-error"));
                result.put(this.threadid + "-" + ((JSONObject) taska.get(i)).get("name").toString() + "-threshold-to-fail", getLong(i, "threshold-to-fail"));
                result.put(this.threadid + "-" + ((JSONObject) taska.get(i)).get("name").toString() + "-skipped", 0); */
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "-passed", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "-passedtime", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "-failed", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "-failedtime", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "-exceeded", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "-exceededtime", 0);
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "-threshold-to-error", getLong(i, "threshold-to-error"));
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "-threshold-to-fail", getLong(i, "threshold-to-fail"));
            result.put(((JSONObject) taska.get(i)).get("name").toString() + "-skipped", 0);
//            }
        }
//        if (workloadconfig.containsKey("maintain-connection")) {
//            keepopen = (boolean) workloadconfig.get("maintain-connection");
//        }
    }

    @Override
    public void run() {
        int index = 0;
        URL[] url;
        HttpURLConnection[] conn;
        JSONParser jp = new JSONParser();
        JSONArray taska = (JSONArray) workloadconfig.get("task");
        JSONArray workloada = (JSONArray) jobconfig.get("workload");
        JSONObject[] state = new JSONObject[taska.size()];
        Long minvalue = getLong(0, "minvalue");
        Long maxvalue = getLong(0, "maxvalue");
        Long randomvalue;
        JSONArray slp = null;
        boolean worked;
        long taskstart = 0;
        long taskstop = 0;
        Long iteration = (Long) ((JSONObject) workloada.get(workloadset)).get("iteration");
        if ((waittostart > 0) && (iteration > 0)) {
            try {
                Thread.sleep(waittostart);
            } catch (InterruptedException ex) {
                Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (int i = 0; i < iteration; i++) {
            worked = true;
            randomvalue = (long) (Math.random() * ((long) getLong(0, "maxvalue") + 1));
            for (index = 0; index < taska.size(); index++) {
                slp = getJSONArray(index, "service-location-port");
                url = new URL[slp.size()];
                conn = new HttpURLConnection[slp.size()];
                int instance = 0;
                // from spl, if greater than 1 select the instance
                // may make conn and url not an array
                if (((String) slp.get(0)).compareTo("$BLT-SLEEP") != 0) {
                    if (worked) {
                        String urlstring = (String) slp.get(instance) + (String) taskconfig[index].get("url-endpoint") + (String) taskconfig[index].get("url-payload");
                        urlstring = replaceVariable(urlstring);
                        urlstring = updateReserved(index, urlstring, state, minvalue, maxvalue, randomvalue);
                        if ((getLong(index, "threshold-to-fail")) < getLong(index, "threshold-to-error")) {
                            System.err.println("Fail threshold set lower than error threshold. Rookie mistake. Results will be inconclusive!");
                        }
                        taskstart = new Date().getTime();
                        try {
                            url[instance] = new URI(urlstring).toURL();
                            conn[instance] = (HttpURLConnection) url[instance].openConnection();
                            conn[instance].setDoOutput(true);
                            conn[instance].setConnectTimeout(getLong(index, "threshold-to-fail").intValue());
                            conn[instance].setReadTimeout(getLong(index, "threshold-to-fail").intValue());
                            conn[instance].setRequestMethod(getString(index, "request"));
//                    System.out.println(((JSONObject) taskconfig[index].get("header")).toString());
                            Iterator<String> iter = ((JSONObject) taskconfig[index].get("header")).keySet().iterator();
                            while (iter.hasNext()) {
                                String headerattr = iter.next();
                                String headervalue = (String) ((JSONObject) taskconfig[index].get("header")).get(headerattr);
//                            System.out.println("pre-head= " + headerattr + " attr: " + headervalue);
                                headerattr = replaceVariable(headerattr);
                                headervalue = replaceVariable(headervalue);
                                headerattr = updateReserved(index, headerattr, state, minvalue, maxvalue, randomvalue);
                                headervalue = updateReserved(index, headervalue, state, minvalue, maxvalue, randomvalue);
//                            System.out.println("post-head= " + headerattr + " attr: " + headervalue + " t= " + getLong(index, "threshold").intValue() + " wls:" + workloadset + " wlc " + workloadconfig.get("name"));
                                conn[instance].setRequestProperty(headerattr, headervalue);
                            }
                            String dp = null;
                            if (taskconfig[index].containsKey("data-payload")) {
                                dp = ((JSONObject) taskconfig[index].get("data-payload")).toString();
                                dp = replaceVariable(dp);
                                dp = updateReserved(index, dp, state, minvalue, maxvalue, randomvalue);
                                OutputStreamWriter cwr = new OutputStreamWriter(conn[instance].getOutputStream());
                                cwr.write(dp);
                                cwr.close();
                            }
                            BufferedReader reader = null;
                            reader = new BufferedReader(new InputStreamReader(conn[instance].getInputStream()));
                            String rl = null;
                            StringBuilder sbin = new StringBuilder();
                            while ((rl = reader.readLine()) != null) {
                                sbin.append(rl);
                            }
//                            System.out.println("iteration= " + i + "  sbin= " + sbin);
                            reader.close();
                            state[index] = (JSONObject) jp.parse(sbin.toString());
                        } catch (URISyntaxException | ParseException | IOException ex) {
                            worked = false;
                            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        taskstop = new Date().getTime();
                        if (worked) {
                            if ((getLong(index, "threshold-to-error")) >= (taskstop - taskstart)) {
//                                result.addTo(this.threadid + "-" + ((JSONObject) taska.get(index)).get("name").toString() + "-passedtime", (taskstop - taskstart));
//                                result.addTo(this.threadid + "-" + ((JSONObject) taska.get(index)).get("name").toString() + "-passed", 1);
                                result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "-passedtime", (taskstop - taskstart));
                                result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "-passed", 1);
                            } else {
                                result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "-exceededtime", (taskstop - taskstart));
                                result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "-exceeded", 1);
                            }
                        } else {
                            result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "-failedtime", (taskstop - taskstart));
                            result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "-failed", 1);
                            if (getBoolean(index, "continue-on-fail")) {
                                worked = true;
                            }
                        }
                    } else {
                        result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "-skipped", 1);
                    }
                } else {
                    try {
                        Thread.sleep((Long) taskconfig[index].get("sleep-time"));
                        result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "-passedtime", (Long) taskconfig[index].get("sleep-time"));
                        result.addTo(((JSONObject) taska.get(index)).get("name").toString() + "-passed", 1);
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
        StringBuffer sb = new StringBuffer("Job: \n" + jobconfig.toJSONString() + "\n\t Workload(s): \n\t" + workloadconfig.toJSONString() + "\n\t\tTask(s): \n");
        for (int i = 0; i < taskconfig.length; i++) {
            sb.append("\t\t" + taskconfig[i].toJSONString()).append("\n");
        }
        result.config = sb.toString();
    }

    private String getString(int index, String key) {
        String value = null;
        JSONArray ja;
        if (taskconfig[index].containsKey(key)) {
            value = (String) taskconfig[index].get(key);
        } else {
            ja = (JSONArray) workloadconfig.get("task");
            if (((JSONObject) ja.get(index)).containsKey(key)) {
                value = (String) (((JSONObject) ja.get(index)).get(key));
            } else {
                if (workloadconfig.containsKey(key)) {
                    value = (String) workloadconfig.get(key);
                } else {
                    ja = (JSONArray) jobconfig.get("workload");
                    for (int i = 0; i < ja.size(); i++) {
                        if (((String) ((JSONObject) ja.get(i)).get("name")).compareTo((String) (workloadconfig.get("name"))) == 0) {
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
            if (((JSONObject) ja.get(index)).containsKey(key)) {
                value = (boolean) (((JSONObject) ja.get(index)).get(key));
                found = true;
            } else {
                if (workloadconfig.containsKey(key)) {
                    value = (boolean) workloadconfig.get(key);
                    found = true;
                } else {
                    ja = (JSONArray) jobconfig.get("workload");
                    for (int i = 0; i < ja.size(); i++) {
                        if (((String) ((JSONObject) ja.get(i)).get("name")).compareTo((String) (workloadconfig.get("name"))) == 0) {
                            if (((JSONObject) ja.get(i)).containsKey(key)) {
                                value = (boolean) (((JSONObject) ja.get(i)).get(key));
                                found = true;
                            }
                        }
                    }
                    if ((jobconfig.containsKey(key)) && (!found)) {
                        value = (boolean) jobconfig.get(key);
                    } else {
                        value = false;
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
            if (((JSONObject) ja.get(index)).containsKey(key)) {
                value = (Long) (((JSONObject) ja.get(index)).get(key));
            } else {
                if (workloadconfig.containsKey(key)) {
                    value = (Long) workloadconfig.get(key);
                } else {
                    ja = (JSONArray) jobconfig.get("workload");
                    for (int i = 0; i < ja.size(); i++) {
                        if (((String) ((JSONObject) ja.get(i)).get("name")).compareTo((String) (workloadconfig.get("name"))) == 0) {
                            if (((JSONObject) ja.get(i)).containsKey(key)) {
                                value = (Long) (((JSONObject) ja.get(i)).get(key));
                            }
                        }
                    }
                    if ((jobconfig.containsKey(key)) && (value == null)) {
                        value = (Long) jobconfig.get(key);
                    } else {
                        value = new Long(0);
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
//        System.out.println("tmp= " + tmpstring);
        return tmpstring;
    }

}
