package com.emsoft.pos.forms;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;

public class GestorClavesMensuales {
    private static final Logger logger = Logger.getInstance();
    private static final int DIAS_MENSUAL = 30;
    private static final int DIAS_GRACIA = 20;

    // 🎯 Método principal de gestión
  public static boolean gestionarClaveMensual() throws IOException {
    String planDLL = HardwareFingerprintProvider.getPlanDesdeDLL();
    if (!"MENSUAL".equalsIgnoreCase(planDLL)) return false;

    EstadoClave estado = analizarEstadoActual();
    
    switch (estado) {
        case VIGENTE:
            return true;
            
        case GRACIA_DISPONIBLE:
            return manejarPeriodoGracia();
            
        case REQUIERE_NUEVA_CLAVE:
            return activarNuevaClave();
            
        default:
            return false;
    }
}
    // 📊 Análisis del estado
    private static EstadoClave analizarEstadoActual() throws IOException {
        LocalDate fechaActualizada = obtenerFechaActualizada();
        if (fechaActualizada == null) return EstadoClave.REQUIERE_NUEVA_CLAVE;

        long diasTranscurridos = ChronoUnit.DAYS.between(fechaActualizada, LocalDate.now());
        
        if (diasTranscurridos <= DIAS_MENSUAL) {
            return EstadoClave.VIGENTE;
        } else if (diasTranscurridos <= DIAS_MENSUAL + DIAS_GRACIA) {
            return EstadoClave.GRACIA_DISPONIBLE;
        } else {
            return EstadoClave.REQUIERE_NUEVA_CLAVE;
        }
    }

    // 🔄 Activación automática de claves
    private static boolean activarNuevaClave() throws IOException {
        // Buscar clave activa actual
        String claveActiva = obtenerClaveActiva();
        
        // Buscar siguiente clave disponible
        String claveDisponible = obtenerSiguienteClaveDisponible();
        
        if (claveDisponible == null) {
            // Generar nuevas claves
            generarNuevasClaves();
            claveDisponible = obtenerSiguienteClaveDisponible();
        }
        
        if (claveDisponible != null) {
            return activarClave(claveDisponible);
        }
        
        return false;
    }

    // 📅 Manejo del período de gracia
// 🆕 Método corregido sin duplicación
private static boolean manejarPeriodoGracia() throws IOException {
    int diasGracia = calcularDiasGraciaRestantes();
    
    int opcion = JOptionPane.showConfirmDialog(
        null,
        String.format("📅 Licencia vencida. Tiene %d días de gracia.\n" +
                     "¿Desea activar una clave mensual ahora?", diasGracia),
        "Activación mensual",
        JOptionPane.YES_NO_OPTION
    );
    
    if (opcion == JOptionPane.YES_OPTION) {
        return activarNuevaClave(); // Solo una llamada
    }
    
    return true; // Continuar en gracia
}
    // 🗓️ Obtener fecha de actualización
    private static LocalDate obtenerFechaActualizada() {
        try {
            Path path = Paths.get(RegistroService.INSTALL_FILE);
            List<String> lines = Files.readAllLines(path);
            
            for (String line : lines) {
                if (line.startsWith("Fecha_actualizada:")) {
                    return LocalDate.parse(line.split(":")[1].trim());
                }
            }
        } catch (IOException e) {
            logger.error("❌ Error leyendo fecha actualizada: " + e.getMessage());
        }
        return null;
    }

    // 🔑 Activar clave específica
    static boolean activarClave(String nuevaClave) throws IOException {
        Path path = Paths.get(RegistroService.INSTALL_FILE);
        List<String> lines = Files.readAllLines(path);
        
        LocalDate fechaActual = LocalDate.now();
        LocalDate nuevaFechaPago = fechaActual.plusDays(DIAS_MENSUAL);
        
        List<String> nuevasLineas = new ArrayList<>();
        boolean claveActivada = false;
        
        for (String line : lines) {
            String lineaLimpia = line.trim();
            
            // Marcar clave activa como usada
            if (lineaLimpia.endsWith(":activo")) {
                String clave = lineaLimpia.split(":")[0];
                nuevasLineas.add(clave + ":usado");
            }
            // Activar nueva clave
            else if (lineaLimpia.startsWith(nuevaClave + ":disponible")) {
                nuevasLineas.add(nuevaClave + ":activo");
                claveActivada = true;
            }
            // Actualizar fechas
            else if (lineaLimpia.startsWith("Fecha_actualizada:")) {
                nuevasLineas.add("Fecha_actualizada: " + fechaActual);
            }
            else if (lineaLimpia.startsWith("Fecha_pago:")) {
                nuevasLineas.add("Fecha_pago: " + nuevaFechaPago);
            }
            else {
                nuevasLineas.add(lineaLimpia);
            }
        }
        
        // Agregar fecha_pago si no existe
        if (!clavesContieneFechaPago(nuevasLineas)) {
            nuevasLineas.add("Fecha_pago: " + nuevaFechaPago);
        }
        
        if (claveActivada) {
            Files.write(path, nuevasLineas);
            logger.info("✅ Clave activada: " + nuevaClave);
            logger.info("📅 Nueva fecha de pago: " + nuevaFechaPago);
            return true;
        }
        
        return false;
    }

    // 🔄 Generar nuevas claves cuando se acaban
    private static void generarNuevasClaves() throws IOException {
        Path path = Paths.get(RegistroService.INSTALL_FILE);
        List<String> lines = Files.readAllLines(path);
        
        // Limpiar claves usadas
        List<String> nuevasLineas = lines.stream()
            .filter(l -> !l.trim().matches("^[A-Z0-9]+:(usado|disponible)$"))
            .collect(Collectors.toList());
        
        // Generar 5 nuevas claves
        List<String> nuevasClaves = ClaveMensualGenerator.generarClavesMensuales("AUTO");
        
        // Agregar nuevas claves
        nuevasLineas.addAll(nuevasClaves);
        
        Files.write(path, nuevasLineas);
        logger.info("🔄 Nuevas claves mensuales generadas: " + nuevasClaves.size());
    }

    // 🔍 Utilidades
    private static String obtenerClaveActiva() throws IOException {
        Path path = Paths.get(RegistroService.INSTALL_FILE);
        List<String> lines = Files.readAllLines(path);
        
        return lines.stream()
            .filter(l -> l.trim().endsWith(":activo"))
            .map(l -> l.split(":")[0])
            .findFirst()
            .orElse(null);
    }

private static String obtenerSiguienteClaveDisponible() throws IOException {
    Path path = Paths.get(RegistroService.INSTALL_FILE);
    List<String> lines = Files.readAllLines(path);
    
    return lines.stream()
        .filter(l -> l.trim().endsWith(":disponible"))
        .map(l -> l.split(":")[0].trim())
        .findFirst()
        .orElse(null);
}
public static String solicitarClaveManual() {
    String clave = JOptionPane.showInputDialog(
        null,
        "🔑 Ingrese la clave mensual proporcionada:",
        "Activación requerida",
        JOptionPane.PLAIN_MESSAGE
        );
        return clave != null ? clave.trim() : null;
    }

public static boolean validarClaveManual(String claveIngresada) throws IOException {
    if (claveIngresada == null || claveIngresada.trim().isEmpty()) {
        return false;
    }
    
    Path path = Paths.get(RegistroService.INSTALL_FILE);
    List<String> lines = Files.readAllLines(path);
    
    // Buscar exactamente la clave con ":disponible"
    for (String line : lines) {
        String[] parts = line.trim().split(":");
        if (parts.length == 2 && 
            parts[0].trim().equalsIgnoreCase(claveIngresada.trim()) && 
            parts[1].trim().equalsIgnoreCase("disponible")) {
            return true;
        }
    }
    
    return false;
}

private static int calcularDiasGraciaRestantes() {
        LocalDate fechaActualizada = obtenerFechaActualizada();
        if (fechaActualizada == null) return 0;
        
        long dias = ChronoUnit.DAYS.between(fechaActualizada, LocalDate.now());
        return Math.max(0, DIAS_MENSUAL + DIAS_GRACIA - (int)dias);
    }

    private static boolean clavesContieneFechaPago(List<String> lineas) {
        return lineas.stream().anyMatch(l -> l.trim().startsWith("Fecha_pago:"));
    }
}

enum EstadoClave {
    VIGENTE,
    GRACIA_DISPONIBLE,
    REQUIERE_NUEVA_CLAVE,
    BLOQUEADO
}