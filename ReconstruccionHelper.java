package com.emsoft.pos.forms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ReconstruccionHelper {

    private static final Logger logger = Logger.getInstance();

public static boolean reconstruirDesdeClave(String clave, LocalDate fechaInstalacion) {
    Logger logger = Logger.getInstance();

    if (clave == null || fechaInstalacion == null) {
        logger.error("⛔ Clave o fecha nula. No se puede reconstruir.");
        return false;
    }

    String planDLL = HardwareFingerprintProvider.getPlanDesdeDLL();
    String ciDLL = HardwareFingerprintProvider.leerCampoDesdeDLL("ci");
    String titularDLL = HardwareFingerprintProvider.leerCampoDesdeDLL("titular");

    // 🔐 Activación de prueba
    if (clave.equals(RegistroService.PRUEBA_KEY)) {
        logger.info("🔧 Clave de prueba detectada. Validando plan en DLL...");

        if (!"PRUEBA".equalsIgnoreCase(planDLL)) {
            logger.warn("⚠️ Plan en DLL no es PRUEBA. Clave de prueba no permitida.");
            return false;
        }

        long dias = ChronoUnit.DAYS.between(fechaInstalacion, LocalDate.now());
        if (dias > 30) {
            logger.warn("⛔ Periodo de prueba expirado. Clave de prueba no válida.");
            return false;
        }

        generarActivoPrueba(ciDLL, titularDLL, fechaInstalacion);
        HardwareFingerprintProvider.agregarFechaActualizada();
        logger.info("✅ activo.dat generado como PRUEBA");
        return true;
    }

    // 🔐 Activación FULL
    if (clave.equals(RegistroService.FULL_KEY)) {
        logger.info("🔧 Reconstruyendo como FULL por clave FULL...");
        generarActivoFull(ciDLL, titularDLL, fechaInstalacion);
        HardwareFingerprintProvider.actualizarDatosEnDLL(ciDLL, titularDLL, "FULL");
        HardwareFingerprintProvider.agregarFechaActualizada();
        return true;
    }

    // 🔐 Activación MENSUAL
    if (clave.equals(RegistroService.MASTER_KEY)) {
        logger.info("🔧 Clave MASTER_KEY detectada. Validando plan en DLL...");

        if (!"MENSUAL".equalsIgnoreCase(planDLL)) {
            logger.warn("⚠️ Plan en DLL no es MENSUAL. Se considera nueva instalación.");
            ciDLL = "3000000";
            titularDLL = "Juan Peres";
        }

        generarActivoMensual(ciDLL, titularDLL, fechaInstalacion);
        //--> HardwareFingerprintProvider.actualizarDatosEnDLL(ciDLL, titularDLL, "MENSUAL");
        // HardwareFingerprintProvider.agregarFechaActualizada();
        return true;
    }

    logger.error("⛔ Clave inválida. No se puede reconstruir.");
    return false;
}
public static void generarActivoPrueba(String ci, String titular, LocalDate fechaInstalacion) {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("CI", ci);
    campos.put("Titular", titular);
    campos.put("Fecha", fechaInstalacion.toString());
    campos.put("Fecha_actualizada", LocalDate.now().toString());
    campos.put("Plan", "PRUEBA");
    escribirActivo(campos);
    Logger.getInstance().info("📄 activo.dat generado como PRUEBA");
}

private static boolean escribirActivo(Map<String, String> campos) {
        Path path = Paths.get(System.getProperty("user.dir"), "doc", "activo.dat");
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (Map.Entry<String, String> entry : campos.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
            logger.info("✅ activo.dat generado correctamente en: " + path.toString());
            return true;
        } catch (IOException e) {
            logger.error("⛔ Error al escribir activo.dat: " + e.getMessage());
            return false;
        }
    }
public static void generarActivoFull(String ci, String titular, LocalDate fechaInstalacion) {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("CI", ci);
    campos.put("Titular", titular);
    campos.put("Fecha", fechaInstalacion.toString());
    campos.put("Plan", "FULL");

    escribirActivo(campos);
    Logger.getInstance().info("📄 activo.dat generado como FULL");
}
public static void generarActivoMensual(String ci, String titular, LocalDate fechaInstalacion ) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("CI", ci);
        campos.put("Titular", titular);
        campos.put("Fecha_pago", HardwareFingerprintProvider.agregarFechaActualizada());
        String fechaDLL = HardwareFingerprintProvider.agregarFechaActualizada();
        campos.put("Fecha_actualizada", fechaDLL != null ? fechaDLL : LocalDate.now().toString());
        campos.put("Plan", "MENSUAL");

        List<String> claves = ClaveMensualGenerator.generarClavesMensuales(ci);
        for (String clave : claves) {
            campos.put(clave.split(":")[0], clave.split(":")[1]);
        }
        escribirActivo(campos);
        Logger.getInstance().info("📄 activo.dat generado como MENSUAL");
    }
}
