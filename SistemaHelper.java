package com.emsoft.pos.forms;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;

public class SistemaHelper {

    public static String obtenerSerialDisco() {
        String comando = "(Get-WmiObject -Query \\\"SELECT VolumeSerialNumber FROM Win32_LogicalDisk WHERE DeviceID='C:'\\\").VolumeSerialNumber";
        ProcessBuilder builder = new ProcessBuilder("powershell", "-Command", comando);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder salida = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) {
                salida.append(linea.trim());
            }
            process.waitFor();
            return salida.toString();
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Error obteniendo el serial del disco: " + e.getMessage());
            return null;
        }
    }

    public static LocalDate getFechaDesdeDLL() {
        try {
            Path rutaDLL = Paths.get(System.getenv("APPDATA") + "/EmsoftPOS/copiasistema.dll");
            if (!Files.exists(rutaDLL)) return null;

            String contenido = Files.readString(rutaDLL);
            JSONObject json = new JSONObject(contenido);
            String fechaStr = json.optString("fecha_instalacion", "").trim();

            return fechaStr.matches("\\d{4}-\\d{2}-\\d{2}") ? LocalDate.parse(fechaStr) : null;
        } catch (IOException | JSONException e) {
            System.err.println("⚠️ Error leyendo fecha desde DLL: " + e.getMessage());
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
  
    public static boolean validarHuella() {
        Path rutaHuella = Paths.get(System.getenv("APPDATA") + "/EmsoftPOS/copiasistema.dll");

        if (!Files.exists(rutaHuella)) {
            MessageScheduler.mostrarAdvertencia("⚠️ No se encontró la huella del sistema.");
            return false;
        }

        try {
            String contenido = Files.readString(rutaHuella);
            JSONObject huella = new JSONObject(contenido);
            String discoHuella = huella.optString("disco_id", "").trim();
            String discoActual = obtenerSerialDisco();

            if (!discoActual.equalsIgnoreCase(discoHuella)) {
                MessageScheduler.mostrarAdvertencia("🚫 Registro no coincide con esta PC.");
                return false;
            }

        } catch (IOException | JSONException e) {
            MessageScheduler.mostrarAdvertencia("❌ Error al validar huella: " + e.getMessage());
            return false;
        }

        return true;
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
        
        Files.write(dllPath, Collections.singleton(json.toString(4)), StandardCharsets.UTF_8);
        System.out.println("📝 DLL actualizado con CI: " + ci + ", Titular: " + titular + ", Plan: " + plan);
    } catch (IOException | JSONException e) {
        System.err.println("❌ Error al actualizar datos en DLL: " + e.getMessage());
    }
}


public static void reconstruirActivoDesdeDLL() {
    try {
        Path dllPath = Paths.get(System.getenv("APPDATA") + "/EmsoftPOS/copiasistema.dll");
        if (!Files.exists(dllPath)) {
            JOptionPane.showMessageDialog(null, "❌ No se encontró el DLL. No se puede reconstruir activo.dat.");
            return;
        }

        String contenido = Files.readString(dllPath);
        JSONObject json = new JSONObject(contenido);

        String ci = json.optString("ci", "3000000");
        String titular = json.optString("titular", "Juan Peres");
        String fecha = json.optString("fecha_instalacion", LocalDate.now().toString());
        String plan = json.optString("plan", "MENSUAL").trim().toUpperCase();

        if (!"FULL".equals(plan)) {
            JOptionPane.showMessageDialog(null, "⚠️ El plan en el DLL no es FULL. No se reconstruye activo.dat automáticamente.");
            return;
        }

        List<String> lines = Arrays.asList(
            "CI: " + ci,
            "Titular: " + titular,
            "Fecha: " + fecha,
            "Plan: " + plan
        );

        Path path = Paths.get(System.getProperty("user.dir") + "/doc/activo.dat");
        Files.createDirectories(path.getParent());
        Files.write(path, lines);
        JOptionPane.showMessageDialog(null, "✅ activo.dat reconstruido correctamente desde DLL (modo FULL).");
        System.out.println("✅ activo.dat reconstruido desde DLL.");
    } catch (IOException | JSONException e) {
        JOptionPane.showMessageDialog(null, "❌ Error al reconstruir activo.dat: " + e.getMessage());
        System.err.println("❌ Error al reconstruir activo.dat: " + e.getMessage());
    }
}
//obtener datos de ACTIVO.DAT
    public static String extraerCampoDesdeActivo(String prefijo) {
    Path path = Paths.get(System.getProperty("user.dir") + "/doc/activo.dat");

    try {
        List<String> lines = Files.readAllLines(path);
        return lines.stream()
            .filter(line -> line.toLowerCase().startsWith(prefijo.toLowerCase()))
            .map(line -> line.substring(prefijo.length()).trim())
            .findFirst()
            .orElse(null);
    } catch (IOException e) {
        System.err.println("Error al leer activo.dat: " + e.getMessage());
        return null;
        }
    }

}
