/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blt;

import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author rmfaller
 */
@SuppressWarnings("serial")
        
class Result extends HashMap<String,Long> {

    public String config;
    private final HashMap<String,Long> result;
    public int uid = 0;
    private String resulttype = null;
    public long processed = 0;

    public Result() {
        result = new HashMap<String,Long>();
    }
    
    public Result(int uid) {
        result = new HashMap<String,Long>();
        this.uid = uid;
    }

    public void put(int threadid, String attr, long value) {
        result.put(attr, value);
    }

    public void put(String attr, long value) {
        result.put(attr, value);
    }
    
    public void addTo(String attr, long value) {
        result.put(attr, (value + result.get(attr)));
        if (value == 1) {
            processed++;
        }
    }

    public void putResultType(String rt) {
        this.resulttype = rt;
    }

    public int getUid() {
        return (this.uid);
    }

    public String getResultType() {
        return (this.resulttype);
    }

    public String[] getAttributes() {
        String[] attrs = new String[result.size()];
        int i = 0;
        Iterator<String> iter = result.keySet().iterator();
        while (iter.hasNext()) {
            attrs[i] = iter.next();
            i++;
        }
        return (attrs);
    }

    public long get(String attr) {
        long value = 0;
        if (result.get(attr) != null) {
            value = (long) result.get(attr);
        }
        return (value);
    }

    public String getConfig() {
        return config;
    }

}
