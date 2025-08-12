package com.emsoft.pos.forms;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import java.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;

public class HardwareFingerprintProvider {

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
    public static String obtenerNombreEquipo() {
        try {
            String nombre = InetAddress.getLocalHost().getHostName();
            Logger.getInstance().info("🖥️ [HardwareFingerprintProvider] Nombre de equipo: " + nombre);
            return nombre;
        } catch (UnknownHostException e) {
            Logger.getInstance().error("💥 Error al obtener nombre de equipo: " + e.getMessage());
            return "";
        }
    }

    public static String obtenerMAC() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            byte[] macBytes = ni.getHardwareAddress();

            if (macBytes == null) return "";

            StringBuilder sb = new StringBuilder();
            for (byte b : macBytes) {
                sb.append(String.format("%02X-", b));
            }

            String mac = sb.toString().replaceAll("-$", "");
            Logger.getInstance().info("🔗 [HardwareFingerprintProvider] MAC: " + mac);
            return mac;
        } catch (SocketException | UnknownHostException e) {
            Logger.getInstance().error("💥 Error al obtener MAC: " + e.getMessage());
            return "";
        }
    }

    public static String obtenerHuellaFisica() {
        String serial = obtenerSerialDisco();
        String mac = obtenerMAC();
        String nombre = obtenerNombreEquipo();

        String huella = String.join("-", serial, mac, nombre);
        Logger.getInstance().info("🧬 [HardwareFingerprintProvider] Huella física generada: " + huella);
        return huella;
    }

    public static boolean validarHuellaConActivoDat(String huellaEsperada) {
        try {
            Path path = Paths.get("config\\activo.dat");
            if (!Files.exists(path)) {
                Logger.getInstance().warn("⚠️ activo.dat no encontrado.");
                return false;
            }

            String contenido = new String(Files.readAllBytes(path)).trim();
            boolean valido = contenido.equals(huellaEsperada);
            Logger.getInstance().info("📄 [HardwareFingerprintProvider] Validación activo.dat: " + valido);
            return valido;
        } catch (IOException e) {
            Logger.getInstance().error("💥 Error al validar activo.dat: " + e.getMessage());
            return false;
        }
    }

    public static void guardarHuellaEnActivoDat(String huella) {
        try {
            Path path = Paths.get("config\\activo.dat");
            Files.write(path, huella.getBytes());
            Logger.getInstance().info("💾 [HardwareFingerprintProvider] Huella guardada en activo.dat");
        } catch (IOException e) {
            Logger.getInstance().error("💥 Error al guardar huella en activo.dat: " + e.getMessage());
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
}
