package com.emsoft.pos.forms;

import java.time.LocalDate;


public class LicenseValidator {

    private final Logger logger = Logger.getInstance();

    public boolean licenciaActiva(ConfigData config) {
        String estado = config.getCampo("EstadoLicencia");
        boolean activa = "ACTIVA".equalsIgnoreCase(estado);
        logger.info("🔎 EstadoLicencia: " + estado);
        return activa;
    }

    public boolean licenciaExpirada(ConfigData config) {
        LocalDate fechaExpiracion = config.getFecha("FechaExpiracion");
        if (fechaExpiracion == null) {
            logger.warn("⚠️ FechaExpiracion inválida");
            return true;
        }
        boolean expirada = LocalDate.now().isAfter(fechaExpiracion);
        logger.info("📅 FechaExpiracion: " + fechaExpiracion + " | Expirada: " + expirada);
        return expirada;
    }
}

