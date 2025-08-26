package com.emsoft.pos.forms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LicenseValidator {

    private final Logger logger = Logger.getInstance();

public EstadoTrial evaluarEstado() {
    String plan = HardwareFingerprintProvider.extraerCampoDesdeActivo("Plan");
    String planDLL = HardwareFingerprintProvider.getPlanDesdeDLL();

    if (planDLL == null || !planDLL.equalsIgnoreCase(plan)) {
        logger.error("⛔ Inconsistencia entre plan en DLL (" + planDLL + ") y activo.dat (" + plan + "). Posible manipulación.");
        return EstadoTrial.BLOQUEADO;
    }

    // 🔍 Validación de sincronización de fechas entre activo.dat y DLL
    LocalDate fechaActivo = HardwareFingerprintProvider.getFechaActualizadaDesdeActivo();
    LocalDate fechaDLL = HardwareFingerprintProvider.getFechaActualizacionDesdeDLL();

    if (fechaActivo != null && fechaDLL != null) {
        long desfase = ChronoUnit.DAYS.between(fechaDLL, fechaActivo);
        if (Math.abs(desfase) > 1) {
            logger.error("⛔ Desfase entre fechas: activo.dat (" + fechaActivo + ") vs DLL (" + fechaDLL + "). Desfase: " + desfase + " días.");
            return EstadoTrial.BLOQUEADO;
        }
    } else {
        logger.warn("⚠️ No se pudo obtener fechas para validar desfase.");
        logger.info("📅 Fecha desde activo.dat: " + fechaActivo);
        logger.info("📅 Fecha desde DLL: " + fechaDLL);

    }

    if (plan == null || plan.trim().isEmpty()) {
        logger.warn("⚠️ Campo 'Plan' no encontrado o vacío en activo.dat.");
        return EstadoTrial.PRUEBA_INICIAL;
    }

    plan = plan.toUpperCase().trim();
    logger.info("📄 Plan detectado en activo: " + plan);

    switch (plan) {
        case "FULL":
            return EstadoTrial.FULL_ACTIVADO;

        case "MENSUAL":
            return evaluarMensual();

        case "PRUEBA":
            return evaluarPrueba();

        default:
            logger.warn("⚠️ Plan desconocido: " + plan);
            return EstadoTrial.PRUEBA_INICIAL;
    }
}


private EstadoTrial validarPeriodo(String tipo, LocalDate fechaReferencia, int limiteDias) {
    if (fechaReferencia == null) {
        logger.error("⛔ Fecha de referencia nula para plan " + tipo + ". Bloqueo inmediato.");
        return EstadoTrial.BLOQUEADO;
    }

    long dias = ChronoUnit.DAYS.between(fechaReferencia, LocalDate.now());
    logger.info("📆 Dias transcurridos desde " + tipo + ": " + dias);

    if (dias > limiteDias) {
        logger.warn("⛔ Periodo vencido para plan " + tipo + ". Bloqueo activado.");
        return EstadoTrial.BLOQUEADO;
    }

    return null; // aún válido
}

private EstadoTrial evaluarPrueba() {
    LocalDate fechaInstalacion = HardwareFingerprintProvider.getFechaInstalacionDesdeDLL();
    EstadoTrial estado = validarPeriodo("PRUEBA", fechaInstalacion, 30);
    return estado != null ? estado : EstadoTrial.PRUEBA_INICIAL;
}
private static final int DIAS_USO_LIMITADO = 30;
private static final int DIAS_GRACIA = 20;

private EstadoTrial evaluarMensual() {
   long diasTranscurridos = HardwareFingerprintProvider.getDiasTranscurridos();
    if (diasTranscurridos <= DIAS_USO_LIMITADO) {
        return EstadoTrial.PLANMENSUAL; // dentro del uso normal
    }
    if (diasTranscurridos <= DIAS_USO_LIMITADO + DIAS_GRACIA) {
            return EstadoTrial.PLANMENSUAL;
    }
    // 🔒 Fuera del período de gracia: intentar activar clave
    String planDLL = HardwareFingerprintProvider.getPlanDesdeDLL();
    if ("MENSUAL".equalsIgnoreCase(planDLL)) {
        try {
            List<String> claves = Files.readAllLines(Paths.get(RegistroService.INSTALL_FILE));
            for (String linea : claves) {
                if (linea.endsWith(":disponible")) {
                    String claveDisponible = linea.split(":")[0];
                    if (RegistroService.activarSerialMensual(claveDisponible)) {
                        Logger.getInstance().info("✅ Clave mensual activada automáticamente: " + claveDisponible);
                        return EstadoTrial.PLANMENSUAL;
                    }
                }
            }
            Logger.getInstance().warn("⚠️ No se encontraron claves mensuales disponibles.");
        } catch (IOException e) {
            Logger.getInstance().error("❌ Error leyendo activo.dat: " + e.getMessage());
        }
    }

    return EstadoTrial.BLOQUEADO;
}

}
