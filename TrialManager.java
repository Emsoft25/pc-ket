package com.emsoft.pos.forms;

import java.io.IOException;
import java.time.LocalDate;

public class TrialManager {
    private static final Logger logger = Logger.getInstance();
    
   public static boolean validarEntornoLocal() throws IOException {
    logger.info("🔍 Iniciando validación del entorno...");
    
    // Paso 1: Si no existe activo.dat, inicializar con DLL
    if (!HardwareFingerprintProvider.existeArchivoActivo()) {
        String planDLL = HardwareFingerprintProvider.getPlanDesdeDLL();
        if (planDLL == null) planDLL = "PRUEBA"; // Default
        
        RegistroService.actualizarPlanInicial(planDLL);
        logger.info("📋 Primera instalación completada con plan: " + planDLL);
    }
    
    // Paso 2: Validar licencia actual (ahora sí existirá)
    ResultadoValidacion resultado = GestorLicencias.validarLicencia();
    
    if (!resultado.isValido()) {
        return manejarLicenciaInvalida(resultado);
    }
    
    // Paso 3: Registrar datos si es necesario
    return registrarDatosUsuario(resultado.getTipo());
}
    
    private static boolean reconstruirSistema() throws IOException {
        logger.warn("❌ activo.dat no encontrado. Requiere reconstrucción.");
        
        String clave = RegistroService.solicitarClaveManual();
        if (clave == null || clave.trim().isEmpty()) {
            logger.error("⛔ Clave no ingresada");
            return false;
        }
        
        LocalDate fechaInstalacion = HardwareFingerprintProvider.getFechaInstalacionDesdeDLL();
        if (fechaInstalacion == null) fechaInstalacion = LocalDate.now();
        
        boolean exito = ReconstruccionHelper.reconstruirDesdeClave(clave.trim(), fechaInstalacion);
        if (!exito) {
            logger.error("⛔ Clave inválida");
            return false;
        }
        
        // Volver a validar después de reconstruir
        return validarEntornoLocal();
    }
    
    private static boolean manejarLicenciaInvalida(ResultadoValidacion resultado) {
        logger.warn("⚠️ " + resultado.getMensaje());
        
        // Si es vencimiento, permitir activación
        if (resultado.getMensaje().contains("vencido")) {
            String clave = RegistroService.solicitarClaveManual();
            if (clave != null && !clave.trim().isEmpty()) {
                LocalDate fechaInstalacion = HardwareFingerprintProvider.getFechaInstalacionDesdeDLL();
                if (fechaInstalacion == null) fechaInstalacion = LocalDate.now();
                
                return ReconstruccionHelper.reconstruirDesdeClave(clave.trim(), fechaInstalacion);
            }
        }
        
        return false;
    }
    
    private static boolean registrarDatosUsuario(TipoLicencia tipo) {
        try {
            RegistroService registro = new RegistroService();
            return registro.validarYActualizarDatosReales(tipo.getNombre());
        } catch (Exception e) {
            logger.error("❌ Error registrando datos: " + e.getMessage());
            return false;
        }
    }
}