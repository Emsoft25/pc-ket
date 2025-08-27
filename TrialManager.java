package com.emsoft.pos.forms;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JOptionPane;

public class TrialManager {
    private static final Logger logger = Logger.getInstance();
    
    public static boolean validarEntornoLocal() throws IOException {
        logger.info("🔍 Iniciando validación del entorno...");
        
        // Paso 1: Reconstruir si no existe activo.dat
        if (!HardwareFingerprintProvider.existeArchivoActivo()) {
            return reconstruirSistema();
        }
        
        // Paso 2: Validar licencia actual
        ResultadoValidacion resultado = GestorLicencias.validarLicencia();
        
        if (!resultado.isValido()) {
            return manejarEstadoLicencia(resultado);
        }
        
        // Paso 3: Registrar datos si es necesario
        return registrarDatosUsuario(resultado.getTipo());
    }
    
    // 🎯 MANEJO UNIFICADO DE ESTADOS
    private static boolean manejarEstadoLicencia(ResultadoValidacion resultado) throws IOException {
        if (resultado.getTipo() == TipoLicencia.MENSUAL) {
            return manejarMensualCompleto();
        }
        
        if (resultado.isEnGracia()) {
            return manejarPeriodoGracia(resultado);
        }
        
        if (resultado.getMensaje().contains("prueba vencido")) {
            return manejarPostPrueba();
        }
        
        return false;
    }
    
    // 🔄 MANEJO MENSUAL CORREGIDO
    private static boolean manejarMensualCompleto() throws IOException {
        EstadoClave estado = GestorClavesMensuales.analizarEstadoActual();
        
        switch (estado) {
            case VIGENTE:
                return true;
                
            case GRACIA_DISPONIBLE:
                return manejarPeriodoGraciaMensual();
                
            case REQUIERE_NUEVA_CLAVE:
                return manejarVencimientoMensual();
                
            default:
                return false;
        }
    }
    
    // 📅 PERÍODO DE GRACIA CON OPCIÓN DE IGNORAR
    private static boolean manejarPeriodoGracia(ResultadoValidacion resultado) throws IOException {
        int diasGracia = resultado.getDiasRestantes();
        
        int opcion = JOptionPane.showConfirmDialog(
            null,
            String.format("📅 Licencia vencida. Tiene %d días de gracia.\n" +
                         "¿Desea activar una clave ahora? (NO = continuar en gracia)", diasGracia),
            "Período de gracia",
            JOptionPane.YES_NO_OPTION
        );
        
        if (opcion == JOptionPane.YES_OPTION) {
            return activarNuevaClaveMensual();
        }
        
        // Continuar en gracia
        logger.info("⏳ Continuando en período de gracia: " + diasGracia + " días");
        return true;
    }
    
    // 🆕 PERÍODO DE GRACIA MENSUAL ESPECÍFICO
    private static boolean manejarPeriodoGraciaMensual() throws IOException {
        int diasGracia = GestorClavesMensuales.calcularDiasGraciaRestantes();
        
        int opcion = JOptionPane.showConfirmDialog(
            null,
            String.format("📅 Licencia mensual vencida. Tiene %d días de gracia.\n" +
                         "¿Desea activar una clave ahora? (NO = continuar en gracia)", diasGracia),
            "Gracia MENSUAL",
            JOptionPane.YES_NO_OPTION
        );
        
        if (opcion == JOptionPane.YES_OPTION) {
            return activarNuevaClaveMensual();
        }
        
        logger.info("⏳ Continuando en gracia MENSUAL: " + diasGracia + " días");
        return true;
    }
    
    // 🔑 VENCIMIENTO MENSUAL - CLAVE OBLIGATORIA
    private static boolean manejarVencimientoMensual() throws IOException {
        String clave = GestorClavesMensuales.solicitarClaveManual();
        if (clave == null) return false;

        if (GestorClavesMensuales.validarClaveManual(clave)) {
            return GestorClavesMensuales.activarClave(clave);
        } else {
            JOptionPane.showMessageDialog(null, 
                "❌ Clave inválida o no disponible. Intente nuevamente.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return manejarVencimientoMensual(); // Reintentar
        }
    }
    
    // 🔁 ACTIVACIÓN DE NUEVA CLAVE
    private static boolean activarNuevaClaveMensual() throws IOException {
        String clave = GestorClavesMensuales.solicitarClaveManual();
        if (clave == null) return false;
        
        boolean exito = GestorClavesMensuales.activarClave(clave);
        if (!exito) {
            JOptionPane.showMessageDialog(null, 
                "❌ Error al activar clave. Intente nuevamente.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return activarNuevaClaveMensual(); // Reintentar
        }
        
        return true;
    }
    
    // 🔄 POST PRUEBA (mantener compatibilidad)
    private static boolean manejarPostPrueba() throws IOException {
        String clave = GestorClavesMensuales.solicitarClaveManual();
        if (clave == null) return false;

        String tipoClave = determinarTipoClave(clave);
        
        switch (tipoClave) {
            case "PRUEBA":
                JOptionPane.showMessageDialog(null, 
                    "❌ La clave es Inválida para prueba. Vuelva a intentar", 
                    "Clave Inválida", 
                    JOptionPane.ERROR_MESSAGE);
                return manejarPostPrueba();
                
            case "MENSUAL":
                return manejarMensualCompleto();
                
            case "FULL":
                return manejarTransicionFull(clave);
                
            default:
                return false;
        }
    }
    
    // 🔄 MÉTODOS AUXILIARES
    
    private static String determinarTipoClave(String clave) {
        if (clave.equals(RegistroService.PRUEBA_KEY)) return "PRUEBA";
        if (clave.equals(RegistroService.MASTER_KEY)) return "MENSUAL";
        if (clave.equals(RegistroService.FULL_KEY)) return "FULL";
        
        try {
            if (GestorClavesMensuales.validarClaveManual(clave)) return "MENSUAL";
        } catch (IOException e) {
            logger.error("❌ Error validando clave: " + e.getMessage());
        }
        
        return "INVALIDA";
    }
    
    private static boolean manejarTransicionFull(String clave) throws IOException {
        String planDLL = HardwareFingerprintProvider.getPlanDesdeDLL();
        
        if ("PRUEBA".equalsIgnoreCase(planDLL)) {
            String nuevoCI = RegistroService.solicitarDato("Ingrese CI del titular:", "\\d{7,8}");
            if (nuevoCI == null) return false;
            
            String nuevoTitular = RegistroService.solicitarDato("Ingrese nombre completo:", ".* .*");
            if (nuevoTitular == null) return false;
            
            ReconstruccionHelper.generarActivoFull(nuevoCI, nuevoTitular, LocalDate.now());
            HardwareFingerprintProvider.actualizarDatosEnDLL(nuevoCI, nuevoTitular, "FULL");
            
        } else {
            // MENSUAL → FULL: usar datos existentes
            ReconstruccionHelper.generarActivoFull(
                HardwareFingerprintProvider.leerCampoDesdeDLL("ci"),
                HardwareFingerprintProvider.leerCampoDesdeDLL("titular"),
                LocalDate.now()
            );
        }
        
        return true;
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
        
        return validarEntornoLocal();
    }
    
    private static boolean registrarDatosUsuario(TipoLicencia tipo) {
        try {
            return RegistroService.validarYActualizarDatosReales(tipo.getNombre());
        } catch (Exception e) {
            logger.error("❌ Error registrando datos: " + e.getMessage());
            return false;
        }
    }
}