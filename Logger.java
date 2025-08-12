package com.emsoft.pos.forms;

public class Logger {

    private static Logger instance;

    private Logger() {}

    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public void info(String mensaje) {
        System.out.println("[INFO] " + mensaje);
    }

    public void warn(String mensaje) {
        System.out.println("[WARN] " + mensaje);
    }

    public void error(String mensaje) {
        System.out.println("[ERROR] " + mensaje);
    }
}
