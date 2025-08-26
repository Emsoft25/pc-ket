package com.emsoft.pos.forms;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.json.*;

public class HardwareFingerprintProvider {

public static boolean existeArchivoActivo() {
        Path path = Paths.get(System.getProperty("user.dir"), "doc", "activo.dat");
        return Files.exists(path);
    }

public static String extraerCampoDesdeActivo(String prefijo) {
    Path path = Paths.get(System.getProperty("user.dir"), "doc", "activo.dat");

    try {
        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(prefijo + "=")) {
                return line.substring((prefijo + "=").length()).trim();
            } else if (line.startsWith(prefijo + ":")) {
                return line.substring((prefijo + ":").length()).trim();
            }
        }
    } catch (IOException e) {
        System.err.println("Error al leer activo.dat: " + e.getMessage());
    }

    return null;
}
public static LocalDate getFechaActualizadaDesdeActivo() {
    try {
        Path path = Paths.get(System.getProperty("user.dir"), "doc", "activo.dat");
        if (!Files.exists(path)) {
            Logger.getInstance().warn("⚠️ activo.dat no encontrado en ruta esperada.");
            return null;
        }

        List<String> lineas = Files.readAllLines(path);
        for (String linea : lineas) {
            if (linea.startsWith("Fecha_actualizada:")) {
                String fechaStr = linea.split(":", 2)[1].trim();
                return LocalDate.parse(fechaStr); // formato ISO yyyy-MM-dd
            }
        }

        Logger.getInstance().warn("⚠️ Campo Fecha_actualizada no encontrado en activo.dat.");
        return null;
    } catch (IOException | DateTimeParseException e) {
        Logger.getInstance().error("❌ Error al leer Fecha_actualizada desde activo.dat: " + e.getMessage());
        return null;
    }
}

public boolean verificarDllPresente() {
    Logger logger = Logger.getInstance();
    try {
        Path rutaDLL = Paths.get(System.getenv("APPDATA"), "EmsoftPOS", "copiasistema.dll");

        if (!Files.exists(rutaDLL)) {
            logger.warn("📁 DLL no encontrada en ruta esperada: " + rutaDLL.toString());
            return false;
        }

        logger.info("✅ DLL encontrada: " + rutaDLL.toString());
        return true;

        } catch (Exception e) {
            logger.error("💥 Error al verificar existencia de DLL: " + e.getMessage());
            return false;
        }
    }
public static boolean validarHuella() {
        try {
            Path ruta = Paths.get(System.getenv("APPDATA"), "EmsoftPOS", "copiasistema.dll");
            if (!Files.exists(ruta)) return false;

            String contenido = Files.readString(ruta);
            JSONObject json = new JSONObject(contenido);
            String discoRegistrado = json.optString("disco_id", "").trim();
            String discoActual = obtenerSerialDisco();

            return discoActual.equalsIgnoreCase(discoRegistrado);
        } catch (IOException | JSONException e) {
            Logger.getInstance().error("💥 Error validando huella: " + e.getMessage());
            return false;
        }
    }

public static String obtenerSerialDisco() {
        try {
            ProcessBuilder builder = new ProcessBuilder("powershell", "-Command",
            "(Get-WmiObject -Query \\\"SELECT VolumeSerialNumber FROM Win32_LogicalDisk WHERE DeviceID='C:'\\\").VolumeSerialNumber");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder salida = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) salida.append(linea.trim());
            process.waitFor();
            return salida.toString();
        } catch (IOException | InterruptedException e) {
            return "";
        }
    }

static LocalDate obtenerFechaDesdeDLL(String campo) {
        try {
            Path ruta = Paths.get(System.getenv("APPDATA"), "EmsoftPOS", "copiasistema.dll");
            if (!Files.exists(ruta)) return null;

            String contenido = Files.readString(ruta);
            JSONObject json = new JSONObject(contenido);
            String fechaStr = json.optString(campo, "").trim();

            return fechaStr.matches("\\d{4}-\\d{2}-\\d{2}") ? LocalDate.parse(fechaStr) : null;
        } catch (IOException | JSONException e) {
            return null;
        }
    }
public static String getPlanDesdeDLL() {
        try {
            Path rutaDLL = Paths.get(System.getenv("APPDATA") + "/EmsoftPOS/copiasistema.dll");
            if (!Files.exists(rutaDLL)) return null;

            String contenido = Files.readString(rutaDLL);
            JSONObject json = new JSONObject(contenido);
           System.out.println("📦 Plan detectado desde DLL: " + json.optString("plan", ""));
            return json.optString("plan", "").trim();
            
        } catch (IOException | JSONException e) {
            System.err.println("⚠️ Error leyendo plan desde DLL: " + e.getMessage());
            return null;
        }
    }
public static LocalDate getFechaInstalacionDesdeDLL() {
        return obtenerFechaDesdeDLL("fecha_instalacion");
    }

public static LocalDate getFechaActualizacionDesdeDLL() {
        return obtenerFechaDesdeDLL("fecha_actualizada");
    }
public static void actualizarDatosEnDLL(String ci, String titular, String plan) {
        try {
            Path dllPath = Paths.get(System.getenv("APPDATA") + "/EmsoftPOS/copiasistema.dll");
            if (!Files.exists(dllPath)) return;

            String contenido = Files.readString(dllPath);
            JSONObject json = new JSONObject(contenido);

            json.put("ci", ci.trim());
            json.put("titular", titular.trim());
            json.put("plan", plan.toUpperCase());
            json.put("fecha_pago", LocalDate.now().plusMonths(1).toString());
            Files.write(dllPath, Collections.singleton(json.toString(4)), StandardCharsets.UTF_8);
            System.out.println("📝 DLL actualizado con CI: " + ci + ", Titular: " + titular + ", Plan: " + plan);
        } catch (IOException | JSONException e) {
            System.err.println("❌ Error al actualizar datos en DLL: " + e.getMessage());
        }
    }
static String ci;
static String titular;
public static String getCI() {
    return ci;
}

public static String getTitular() {
    return titular;
}

public static String leerCampoDesdeDLL(String campo) {
    try {
        Path ruta = Paths.get(System.getenv("APPDATA"), "EmsoftPOS", "copiasistema.dll");
        if (!Files.exists(ruta)) return null;
        String contenido = Files.readString(ruta);
        JSONObject json = new JSONObject(contenido);
        return json.optString(campo, "").trim();
    } catch (IOException | JSONException e) {
            return null;
        }
    }

public static String agregarFechaActualizada() {
    try {
        Path dllPath = Paths.get(System.getenv("APPDATA"), "EmsoftPOS", "copiasistema.dll");
        if (!Files.exists(dllPath)) return null;

        String contenido = Files.readString(dllPath);
        JSONObject json = new JSONObject(contenido);

        String nuevaFecha = LocalDate.now().toString();
        json.put("fecha_actualizada", nuevaFecha);
        
        
         LocalDate fechaPago = LocalDate.now().plusDays(30);
            json.put("fecha_pago", fechaPago);

        Files.write(dllPath, Collections.singleton(json.toString(4)), StandardCharsets.UTF_8);
        Logger.getInstance().info("📝 DLL actualizado con fecha_actualizada: " + nuevaFecha);

        return nuevaFecha;
    } catch (IOException | JSONException e) {
        Logger.getInstance().error("❌ Error al actualizar fecha_actualizada en DLL: " + e.getMessage());
        return null;
        }
    }
public static long getDiasTranscurridos() {
    LocalDate fechaReferencia = getFechaInstalacionDesdeDLL();
    if (fechaReferencia == null) {
        fechaReferencia = getFechaInstalacionDesdeDLL();
        Logger.getInstance().warn("⚠️ Usando fecha de instalación como fallback.");
    }

    return ChronoUnit.DAYS.between(fechaReferencia, LocalDate.now());
}

}