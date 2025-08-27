package com.emsoft.pos.forms;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ReconstruccionHelper {
    private static final Logger logger = Logger.getInstance();

    public static boolean reconstruirDesdeClave(String clave, LocalDate fechaInstalacion) throws IOException {
        if (clave == null || fechaInstalacion == null) {
            logger.error("⛔ Clave o fecha nula. No se puede reconstruir.");
            return false;
        }

        // Obtener fecha exacta del DLL
        LocalDate fechaDLL = HardwareFingerprintProvider.getFechaInstalacionDesdeDLL();
        if (fechaDLL == null) fechaDLL = LocalDate.now();

        String planDLL = HardwareFingerprintProvider.getPlanDesdeDLL();
        String ciDLL = HardwareFingerprintProvider.leerCampoDesdeDLL("ci");
        String titularDLL = HardwareFingerprintProvider.leerCampoDesdeDLL("titular");

        // 🔐 CASO 1: CLAVE DE PRUEBA
        if (clave.equals(RegistroService.PRUEBA_KEY)) {
            logger.info("🔧 Clave de prueba detectada. Validando...");
            
            if (!"PRUEBA".equalsIgnoreCase(planDLL)) {
                logger.warn("⚠️ Plan DLL no es PRUEBA");
                return false;
            }

            long dias = ChronoUnit.DAYS.between(fechaInstalacion, LocalDate.now());
            if (dias > 30) {
                logger.warn("⛔ Periodo de prueba expirado");
                return false;
            }

            generarActivoPrueba(ciDLL, titularDLL, fechaDLL);
            HardwareFingerprintProvider.actualizarDatosEnDLL(ciDLL, titularDLL, "PRUEBA");
            HardwareFingerprintProvider.agregarFechaActualizada();
            RegistroService.actualizarPlanInicial("PRUEBA");
            logger.info("✅ PRUEBA reconstruida con fecha DLL: " + fechaDLL);
            return true;
        }

        // 🔐 CASO 2: CLAVE FULL
        if (clave.equals(RegistroService.FULL_KEY)) {
            logger.info("🔧 Reconstruyendo como FULL...");
            
            generarActivoFull(ciDLL, titularDLL, fechaDLL);
            HardwareFingerprintProvider.actualizarDatosEnDLL(ciDLL, titularDLL, "FULL");
            HardwareFingerprintProvider.agregarFechaActualizada();
            RegistroService.actualizarPlanInicial("FULL");
            logger.info("✅ FULL reconstruido con fecha DLL: " + fechaDLL);
            return true;
        }

        // 🔐 CASO 3: CLAVE MENSUAL
        if (clave.equals(RegistroService.MASTER_KEY)) {
            logger.info("🔧 Clave MENSUAL detectada...");
            
            // Determinar si necesita datos nuevos
            String ciActual = ciDLL;
            String titularActual = titularDLL;
            
            if ("PRUEBA".equalsIgnoreCase(planDLL)) {
                // Transición PRUEBA → MENSUAL: pedir datos reales
                ciActual = RegistroService.solicitarDato("Ingrese CI del titular (7-8 dígitos):", "\\d{7,8}");
                if (ciActual == null) return false;
                
                titularActual = RegistroService.solicitarDato("Ingrese nombre y apellido:", ".* .*");
                if (titularActual == null) return false;
            }

            // Generar activo mensual con claves
            generarActivoMensualCompleto(ciActual, titularActual, fechaDLL);
            
            // Actualizar DLL y sincronizar
            HardwareFingerprintProvider.actualizarDatosEnDLL(ciActual, titularActual, "MENSUAL");
            HardwareFingerprintProvider.agregarFechaActualizada();
            RegistroService.actualizarPlanInicial("MENSUAL");
            
            // Registrar si son datos reales
            if (!"3000000".equals(ciActual) && !"Juan Peres".equalsIgnoreCase(titularActual)) {
                RegistroService registro = new RegistroService();
                registro.completarRegistroUsuario("MENSUAL");
            }
            
            logger.info("✅ MENSUAL reconstruido con fecha DLL: " + fechaDLL);
            return true;
        }

        logger.error("⛔ Clave inválida");
        return false;
    }

    // 📄 GENERADORES ACTUALIZADOS

    public static void generarActivoPrueba(String ci, String titular, LocalDate fechaDLL) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("CI", ci != null ? ci : "3000000");
        campos.put("Titular", titular != null ? titular : "Cliente Prueba");
        campos.put("Fecha", fechaDLL.toString());
        campos.put("Fecha_actualizada", fechaDLL.toString());
        campos.put("Plan", "PRUEBA");
        escribirActivo(campos);
    }

    public static void generarActivoFull(String ci, String titular, LocalDate fechaDLL) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("CI", ci != null ? ci : "3000000");
        campos.put("Titular", titular != null ? titular : "Cliente Full");
        campos.put("Fecha", fechaDLL.toString());
        campos.put("Fecha_actualizada", fechaDLL.toString());
        campos.put("Plan", "FULL");
        escribirActivo(campos);
    }

    public static void generarActivoMensualCompleto(String ci, String titular, LocalDate fechaDLL) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("CI", ci != null ? ci : "3000000");
        campos.put("Titular", titular != null ? titular : "Cliente Mensual");
        campos.put("Fecha_pago", fechaDLL.plusDays(30).toString());
        campos.put("Fecha_actualizada", fechaDLL.toString());
        campos.put("Plan", "MENSUAL");

        // Agregar claves preinstaladas
        List<String> claves = ClaveMensualGenerator.generarClavesMensuales(ci);
        for (String clave : claves) {
            String[] partes = clave.split(":");
            campos.put(partes[0], partes[1]);
        }
        
        escribirActivo(campos);
    }

    // 📝 ESCRITOR UNIVERSAL
    private static boolean escribirActivo(Map<String, String> campos) {
        try {
            Path dir = Paths.get(System.getProperty("user.dir"), "doc");
            if (!Files.exists(dir)) Files.createDirectories(dir);
            
            Path path = dir.resolve("activo.dat");
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                for (Map.Entry<String, String> entry : campos.entrySet()) {
                    writer.write(entry.getKey() + ":" + entry.getValue());
                    writer.newLine();
                }
            }
            logger.info("✅ activo.dat generado: " + path);
            return true;
        } catch (IOException e) {
            logger.error("❌ Error escribiendo activo.dat: " + e.getMessage());
            return false;
        }
    }

    // 🔄 Método auxiliar para reemplazar generarActivoMensual (compatibilidad)
    @Deprecated
    public static void generarActivoMensual(String ci, String titular, LocalDate fechaInstalacion) {
        generarActivoMensualCompleto(ci, titular, fechaInstalacion);
    }
}