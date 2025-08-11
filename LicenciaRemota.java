package com.emsoft.pos.forms;

import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.json.JSONException;

public class LicenciaRemota {

    // 📍 Objeto que representa los datos recibidos del servidor
    public static class EstadoLicencia {
        private final String ci;
        private final String titular;
        private final String estado;         // "mensual", "full", etc.
        private final LocalDate fechaActiva;

        public EstadoLicencia(String ci, String titular, String estado, LocalDate fechaActiva) {
            this.ci = ci;
            this.titular = titular;
            this.estado = estado;
            this.fechaActiva = fechaActiva;
        }

        public String getCI() { return ci; }
        public String getTitular() { return titular; }
        public String getEstado() { return estado; }
        public LocalDate getFechaActiva() { return fechaActiva; }

        public boolean esLicenciaActiva() {
            return fechaActiva != null && fechaActiva.isAfter(LocalDate.now().minusDays(30));
        }

        public boolean esLicenciaFull() {
            return estado.equalsIgnoreCase("full");
        }

        @Override
        public String toString() {
            return "CI: " + ci + "\nTitular: " + titular + "\nEstado: " + estado + "\nFecha activa: " + fechaActiva;
        }
    }

    // 🌐 Consulta al servidor por discoId
public static EstadoLicencia consultarPorDisco(String discoId) {
    try {
        String urlStr = "http://localhost/emsoftpy/activacion.php?disco_id=" + URLEncoder.encode(discoId, "UTF-8");
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            System.err.println("❌ Error HTTP: " + conn.getResponseCode());
            return null;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) response.append(line);
        conn.disconnect();

        String contenido = response.toString().trim();

        if (!contenido.startsWith("{")) {
            System.err.println("⚠️ Respuesta remota no es JSON:\n" + contenido);
            return null;
        }

        JSONObject json = new JSONObject(contenido);
        if (json.has("error")) {
            System.out.println("🚫 " + json.getString("error"));
            return null;
        }

        String ci = json.optString("ci", "3000000");
        String titular = json.optString("nombre", "Desconocido");
        String estado = json.optString("modo", "mensual");
        String fechaStr = json.optString("vencimiento", "");

        LocalDate fecha = fechaStr.matches("\\d{4}-\\d{2}-\\d{2}") ? LocalDate.parse(fechaStr) : null;
        return new EstadoLicencia(ci, titular, estado, fecha);

    } catch (IOException | JSONException e) {
        System.err.println("💥 Excepción: " + e.getMessage());
        return null;
    }
}

    // 🧾 Extrae licencia local desde activo.dat
    public static EstadoLicencia extraerDesdeActivo() {
        Path ruta = Paths.get(System.getProperty("user.dir"), "doc", "activo.dat");

        try {
            List<String> lineas = Files.readAllLines(ruta);
            String ci = "", titular = "", estado = "";
            LocalDate fecha = null;

            for (String linea : lineas) {
                if (linea.startsWith("CI:")) {
                    ci = linea.substring(3).trim();
                } else if (linea.startsWith("Titular:")) {
                    titular = linea.substring(8).trim();
                } else if (linea.startsWith("Fecha:")) {
                    fecha = LocalDate.parse(linea.substring(6).trim());
                } else if (linea.contains(":activo")) {
                    estado = linea.split(":")[0].trim();
                }
            }

            if (!ci.isEmpty() && !titular.isEmpty() && fecha != null) {
                return new EstadoLicencia(ci, titular, estado, fecha);
            }

        } catch (IOException | RuntimeException e) {
            System.err.println("❌ Error leyendo activo.dat: " + e.getMessage());
        }

        return null;
    }
}
