package com.emsoft.pos.forms;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class GestorLicencias {
    private static final Logger logger = Logger.getInstance();
    
    public static ResultadoValidacion validarLicencia() throws IOException {
        logger.info("🔍 Validando licencia del sistema...");
        
        // Validar integridad del sistema
        if (!HardwareFingerprintProvider.existeArchivoActivo()) {
            return ResultadoValidacion.error("Archivo activo.dat no encontrado");
        }
        
        if (!HardwareFingerprintProvider.validarHuella()) {
            return ResultadoValidacion.error("Huella del sistema inválida");
        }
        
        // Validar consistencia entre archivos
        if (!validarConsistenciaDatos()) {
            return ResultadoValidacion.error("Inconsistencia en archivos de licencia");
        }
        
        // Determinar tipo de licencia
        String plan = HardwareFingerprintProvider.getPlanDesdeDLL();
        if (plan == null || plan.trim().isEmpty()) {
            return ResultadoValidacion.vencido("No hay licencia activa");
        }
        
        TipoLicencia tipo = TipoLicencia.valueOf(plan.toUpperCase());
        
        // Validar según tipo
        return validarPorTipo(tipo);
    }
    
    private static boolean validarConsistenciaDatos() {
    String planActivo = HardwareFingerprintProvider.extraerCampoDesdeActivo("Plan");
    String planDLL = HardwareFingerprintProvider.getPlanDesdeDLL();
    
    // ✅ NUEVO: Permitir primera instalación
    if (planActivo == null || planActivo.trim().isEmpty()) {
        logger.info("📋 Primera instalación detectada");
        
        // Sincronizar activo.dat con DLL
        try {
            RegistroService.actualizarPlanInicial(planDLL != null ? planDLL : "PRUEBA");
            return true;
        } catch (IOException e) {
            logger.error("❌ Error sincronizando plan inicial: " + e.getMessage());
            return false;
        }
    }
    
    // Validar solo si ambos existen
    if (!planDLL.equalsIgnoreCase(planActivo)) {
        logger.error("⛔ Inconsistencia: DLL=" + planDLL + ", activo.dat=" + planActivo);
        return false;
        }

        // ... resto de validación de fechas ...
        return true;
    }
    
private static ResultadoValidacion validarPorTipo(TipoLicencia tipo) throws IOException {
    LocalDate fechaInicio = HardwareFingerprintProvider.getFechaInstalacionDesdeDLL();
    if (fechaInicio == null) {
        return ResultadoValidacion.error("Fecha de inicio no encontrada");
    }

    long diasTranscurridos = ChronoUnit.DAYS.between(fechaInicio, LocalDate.now());

    switch (tipo) {
        case FULL:
            return ResultadoValidacion.valido(TipoLicencia.FULL, Integer.MAX_VALUE);

        case PRUEBA:
            if (diasTranscurridos <= tipo.getDuracionDias()) {
                int restantes = tipo.getDuracionDias() - (int)diasTranscurridos;
                return ResultadoValidacion.valido(TipoLicencia.PRUEBA, restantes);
            } else {
                return ResultadoValidacion.vencido("Período de prueba vencido");
            }

        // En el método evaluarEstado(), cambiar el case MENSUAL:
        case MENSUAL:
            if (diasTranscurridos <= 30) {
                return ResultadoValidacion.valido(TipoLicencia.MENSUAL, 30 - (int)diasTranscurridos);
            } else if (diasTranscurridos <= 30 + 20) { // DIAS_MENSUAL + DIAS_GRACIA
                return ResultadoValidacion.enGracia(TipoLicencia.MENSUAL, 
                    (int)(50 - diasTranscurridos), true);
            } else {
                return ResultadoValidacion.vencido("Licencia y período de gracia vencidos");
            }

            default:
                return ResultadoValidacion.error("Tipo desconocido");
        }
    }    
      
}