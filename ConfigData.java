package com.emsoft.pos.forms;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;


public class ConfigData {
    private final Map<String, String> campos;

    public ConfigData(Map<String, String> campos) {
        this.campos = campos;
    }

    public String getCampo(String clave) {
        return campos.getOrDefault(clave, "");
    }

    public LocalDate getFecha(String clave) {
        try {
            return LocalDate.parse(getCampo(clave));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
