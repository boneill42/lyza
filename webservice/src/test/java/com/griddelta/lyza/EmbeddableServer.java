package com.griddelta.lyza;

import com.griddelta.lyza.LyzaApplication;


public class EmbeddableServer implements Runnable {
    String[] args = null;
    
    public EmbeddableServer(String[] args){
        this.args = args;        
    }

    public void run() {
        try {
            LyzaApplication.main(this.args);
        } catch (Exception e) {
            e.printStackTrace();
        }       
    }
}
