package com.emsoft.pos.forms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ClaveMensualGenerator {

    public static String generateSimpleKey() {
    return (char) ('A' + new Random().nextInt(26)) + String.valueOf(100 + new Random().nextInt(900));
}

public static List<String> generarClavesMensuales(String ci) {
    List<String> claves = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
        claves.add(generateSimpleKey() + ":disponible");
    }
    Logger.getInstance().info("🔐 Claves mensuales generadas: " + claves);
    return claves;
    }

public static boolean activarClaveMensual(String claveIngresada) throws IOException {
    Path path = Paths.get(RegistroService.INSTALL_FILE);
    List<String> lines = Files.readAllLines(path);
    boolean claveValida = false;
    boolean fechaActualizadaPresente = false;
    for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i).trim();
        
        // Marcar la clave actualmente activa como usada
        if (line.endsWith(":activo")) {
            String claveActiva = line.split(":")[0];
            lines.set(i, claveActiva + ":usado");
        }
        
        // Activar la nueva clave si está disponible
        if (line.startsWith(claveIngresada + ":disponible")) {
            lines.set(i, claveIngresada + ":activo");
            claveValida = true;
        }
        // 🔍 Verificar si quedan claves disponibles después de activar
boolean quedanDisponibles = lines.stream()
    .anyMatch(l -> l.trim().endsWith(":disponible"));

if (!quedanDisponibles) {
    Logger.getInstance().warn("⚠️ No quedan claves disponibles. Regenerando nuevas claves.");

    // Generar 5 nuevas claves únicas
    Set<String> nuevasClaves = new LinkedHashSet<>();
    while (nuevasClaves.size() < 5) {
        String clave = ClaveMensualGenerator.generateSimpleKey();
        nuevasClaves.add(clave + ":disponible");
    }

    // Reemplazar las líneas que contienen claves usadas o activas
    int reemplazos = 0;
    for (int j = 0; j < lines.size(); j++) {
         line = lines.get(j).trim();
        if (line.matches("^[A-Z0-9]+:(usado|activo)$") && reemplazos < 5) {
            String nuevaClave = nuevasClaves.stream().skip(reemplazos).findFirst().orElse(null);
            if (nuevaClave != null) {
                lines.set(j, nuevaClave);
                reemplazos++;
            }
        }
    }

    // Si no se encontraron suficientes líneas para reemplazar, agregar las restantes
    if (reemplazos < 5) {
        nuevasClaves.stream().skip(reemplazos).forEach(lines::add);
    }

    Logger.getInstance().info("✅ Nuevas claves generadas: " + nuevasClaves);
}

        // Actualizar la fecha_actualizada si ya existe
        if (line.startsWith("Fecha_actualizada:")) {
            lines.set(i, "Fecha_actualizada: " + LocalDate.now());
            fechaActualizadaPresente = true;
        }
    }
    // Si no existía la línea de fecha_actualizada, agregarla
    if (!fechaActualizadaPresente) {
        lines.add("Fecha_actualizada: " + LocalDate.now());
    }
    // Agrega plan
    lines = lines.stream().filter(l -> !l.startsWith("Plan:")).collect(Collectors.toList());
    lines.add("Plan: MENSUAL");

    if (!claveValida) {
        Logger.getInstance().warn("⚠️ Clave ingresada no válida o ya usada.");
        return false;
    }
    Files.write(path, lines);
    Logger.getInstance().info("✅ Clave mensual activada: " + claveIngresada);
    return true;
    }
}