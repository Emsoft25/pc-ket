package com.emsoft.pos.forms;

import java.time.LocalDate;

public class MonthlyKeyManager {

    private final Logger logger = Logger.getInstance();

    public boolean claveMensualValida(ConfigData config) {
        String clave = config.getCampo("ClaveMensual");
        LocalDate fechaUso = config.getFecha("FechaUso");
        LocalDate fechaVencimiento = config.getFecha("FechaVencimiento");

        if (clave.isEmpty()) {
            logger.warn("🔑 ClaveMensual vacía");
            return false;
        }

        if (fechaUso == null || fechaVencimiento == null) {
            logger.warn("📅 Fechas de uso o vencimiento inválidas");
            return false;
        }

        LocalDate hoy = LocalDate.now();
        boolean dentroDelRango = !hoy.isBefore(fechaUso) && !hoy.isAfter(fechaVencimiento);

        logger.info("🗓️ FechaUso: " + fechaUso + " | FechaVencimiento: " + fechaVencimiento + " | Hoy: " + hoy);
        logger.info("🔐 Clave válida: " + dentroDelRango);

        return dentroDelRango;
    }
}
