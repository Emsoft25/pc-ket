package com.emsoft.pos.forms;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class GestorLicencias {
    private static final Logger logger = Logger.getInstance();
    
    public static ResultadoValidacion validarLicencia() {
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
    
    private static ResultadoValidacion validarPorTipo(TipoLicencia tipo) {
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
                
            case MENSUAL:
                if (diasTranscurridos <= tipo.getDuracionDias()) {
                    int restantes = tipo.getDuracionDias() - (int)diasTranscurridos;
                    return ResultadoValidacion.valido(TipoLicencia.MENSUAL, restantes);
                } else if (diasTranscurridos <= tipo.getDuracionDias() + tipo.getDiasGracia()) {
                    int diasGracia = (int)(tipo.getDuracionDias() + tipo.getDiasGracia() - diasTranscurridos);
                    return ResultadoValidacion.enGracia(TipoLicencia.MENSUAL, diasGracia);
                } else {
                    return ResultadoValidacion.vencido("Licencia mensual vencida");
                }
                
            default:
                return ResultadoValidacion.error("Tipo de licencia desconocido");
        }
    }
}