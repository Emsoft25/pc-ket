package com.emsoft.pos.forms;

import java.io.IOException;
import java.time.LocalDate;

public class TrialManager {

    private static final int DIAS_PRUEBA = 30;
    static final int DIAS_USO_LIMITADO = 30;
    static final int DIAS_GRACIA = 20;

    private final LicenseValidator licenseValidator = new LicenseValidator();
    private final Logger logger = Logger.getInstance();

    public EstadoTrial validarEstadoDelSistema() {
        logger.info("🔍 Iniciando validación del sistema...");

        if (!HardwareFingerprintProvider.existeArchivoActivo()) {
            logger.warn("❌ No se encontró activo.dat");
            return EstadoTrial.BLOQUEADO;
        }

        if (!HardwareFingerprintProvider.validarHuella()) {
            logger.warn("🚫 Huella del sistema inválida");
            return EstadoTrial.BLOQUEADO;
        }

        return licenseValidator.evaluarEstado();
    }

public static boolean validarEntornoLocal() throws IOException {
    Logger logger = Logger.getInstance();

    if (!HardwareFingerprintProvider.existeArchivoActivo()) {
        logger.warn("❌ activo.dat no encontrado. Se requiere clave para reconstrucción.");
        String clave = RegistroService.solicitarClaveManual();
        if (clave == null || clave.trim().isEmpty()) {
            logger.error("⛔ Clave no ingresada. No se puede continuar.");
            return false;
        }

        LocalDate fechaInstalacion = HardwareFingerprintProvider.getFechaInstalacionDesdeDLL();
        if (fechaInstalacion == null) fechaInstalacion = LocalDate.now();

        boolean exito = ReconstruccionHelper.reconstruirDesdeClave(clave.trim(), fechaInstalacion);
        if (!exito) {
            logger.error("⛔ Clave inválida. Estado final: BLOQUEADO");
            return false;
        }
    }

    TrialManager trialManager = new TrialManager();
    EstadoTrial estado = trialManager.validarEstadoDelSistema();
    long diasTranscurridos = HardwareFingerprintProvider.getDiasTranscurridos();
    System.out.println("📋 Estado actual del entorno: " + estado);
    System.out.println("📅 Dias transcurridos HardwareFingerprintProvider: " + diasTranscurridos);

    RegistroService registroService = new RegistroService();

    switch (estado) {
        case PRUEBA_INICIAL:
            MessageScheduler.evaluarEstadoYMostrar();
            if (HardwareFingerprintProvider.getDiasTranscurridos() > DIAS_PRUEBA) {
                logger.warn("⛔ Periodo de prueba vencido. Se requiere activación.");
                MessageScheduler.mostrarAdvertencia("⚠️ El periodo de prueba ha finalizado. Ingrese una clave válida para continuar.");

                String clave = RegistroService.solicitarClaveManual();
                if (clave == null || clave.trim().isEmpty()) {
                    logger.error("⛔ Clave no ingresada. El sistema permanecerá bloqueado.");
                    return false;
                }

                LocalDate fechaInstalacion = HardwareFingerprintProvider.getFechaInstalacionDesdeDLL();
                if (fechaInstalacion == null) fechaInstalacion = LocalDate.now();

                boolean exito = ReconstruccionHelper.reconstruirDesdeClave(clave.trim(), fechaInstalacion);
                if (!exito) {
                    logger.error("⛔ Clave inválida. El sistema permanecerá bloqueado.");
                    return false;
                }
            }

            if (!RegistroService.validarYActualizarDatosReales("PRUEBA")) return false;
            return true;

     case PLANMENSUAL:
            long dias = HardwareFingerprintProvider.getDiasTranscurridos();

            if (dias >= DIAS_USO_LIMITADO + DIAS_GRACIA ) {
                logger.info("🔐 Se requiere clave mensual para continuar.");
                String clave = RegistroService.solicitarClaveManual();
                if (clave == null || !ClaveMensualGenerator.activarClaveMensual(clave.trim())) {
                    logger.error("⛔ Clave mensual inválida o no ingresada.");
                    return false;
                }
            } else {
                logger.info("⏳ Usuario sigue en período de gracia (" + dias + " días). No se requiere clave aún.");
            }

        if (!RegistroService.validarYActualizarDatosReales("MENSUAL")) return false;
            MessageScheduler.evaluarEstadoYMostrar();
            return true;

        case FULL_ACTIVADO:
            if (!RegistroService.validarYActualizarDatosReales("FULL")) return false;
            registroService.completarRegistroUsuario("FULL");
            return true;

        case DESCONOCIDO:
        case BLOQUEADO:
            logger.warn("⚠️ Estado " + estado + ". Se requiere activación.");
            MessageScheduler.mostrarAdvertencia("🔒 El sistema está bloqueado o en estado desconocido. Ingrese una clave válida para continuar.");

            String clave = RegistroService.solicitarClaveManual();
            if (clave == null || clave.trim().isEmpty()) {
                logger.error("⛔ Clave no ingresada. El sistema permanecerá bloqueado.");
                return false;
            }

            LocalDate fechaInstalacion = HardwareFingerprintProvider.getFechaInstalacionDesdeDLL();
            if (fechaInstalacion == null) fechaInstalacion = LocalDate.now();

            boolean desbloqueado = ReconstruccionHelper.reconstruirDesdeClave(clave.trim(), fechaInstalacion);
            if (!desbloqueado) {
                logger.error("⛔ Clave inválida. El sistema permanecerá bloqueado.");
                return false;
            }

            String planFinal = HardwareFingerprintProvider.getPlanDesdeDLL();
			if (!RegistroService.validarYActualizarDatosReales(planFinal)) return false;
			registroService.completarRegistroUsuario(planFinal);

            logger.info("✅ Sistema desbloqueado correctamente.");
            return true;
        }

        return false;
    }


public static boolean estaBloqueado() {
        EstadoTrial estado = new TrialManager().validarEstadoDelSistema();
        return estado == EstadoTrial.BLOQUEADO || estado == EstadoTrial.DESCONOCIDO;
    }

}
