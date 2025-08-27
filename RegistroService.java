package com.emsoft.pos.forms;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.json.*;

public class RegistroService {
     private static final Logger logger = Logger.getInstance();
     public static final String INSTALL_FILE = System.getProperty("user.dir") + "/doc/activo.dat";
     public static final String PRUEBA_KEY = "EMSOFT-123";
     public static final String MASTER_KEY = "EMSOFT-2025";
     public static final String FULL_KEY = "EMSOFT-FULL";
     
  
         
// ✅ Método completo con fecha
public static void actualizarPlanInicial(String plan) throws IOException {
    Path path = Paths.get(INSTALL_FILE);
     LocalDate fechaDLL = HardwareFingerprintProvider.getFechaInstalacionDesdeDLL();
    
    List<String> lines = new ArrayList<>();
    lines.add("Plan: " + plan.toUpperCase());
    lines.add("Fecha_actualizada: " + fechaDLL);
    
    // Calcular fecha_pago según plan
    if ("MENSUAL".equalsIgnoreCase(plan)) {
        lines.add("Fecha_pago: " + fechaDLL.plusDays(ConfiguracionLicencia.DIAS_MENSUAL));
    }
    
    // Datos del DLL
    String ci = HardwareFingerprintProvider.leerCampoDesdeDLL("ci");
    String titular = HardwareFingerprintProvider.leerCampoDesdeDLL("titular");
    
    if (ci != null) lines.add("CI: " + ci);
    if (titular != null) lines.add("Titular: " + titular);
    
    // Agregar claves solo para mensual
    if ("MENSUAL".equalsIgnoreCase(plan)) {
        lines.addAll(ClaveMensualGenerator.generarClavesMensuales(ci != null ? ci : "3000000"));
    }
    
    Files.write(path, lines);
    logger.info("✅ activo.dat creado con fecha del DLL: " + fechaDLL);
    }

public static String solicitarClaveManual() {
    String input = JOptionPane.showInputDialog("🔒 Ingrese clave para activar:");
    return (input != null) ? input.trim() : null;
    }
public void completarRegistroUsuario(String modoLicencia) throws IOException {
        Path path = Paths.get(INSTALL_FILE);
        List<String> lines = Files.readAllLines(path);
        int indexCI = -1, indexTitular = -1;
        boolean necesitaRegistro = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim().toLowerCase();
            if (line.startsWith("ci:")) {
                indexCI = i;
                if (line.contains("3000000")) necesitaRegistro = true;
            }
            if (line.startsWith("titular:")) {
                indexTitular = i;
                if (line.contains("clientes varios") || line.contains("juan peres")) necesitaRegistro = true;
            }
        }

        if (!necesitaRegistro || indexCI == -1 || indexTitular == -1) return;

        JOptionPane.showMessageDialog(null, "🔔 Registro requerido para continuar.");
        String nuevoCI = solicitarDato("Ingrese CI (7 u 8 dígitos):", "\\d{7,8}");
        if (nuevoCI == null) return;

        String nuevoTitular = solicitarDato("Ingrese nombre y apellido del titular:", ".* .*");
        if (nuevoTitular == null) return;

        lines.set(indexCI, "CI: " + nuevoCI.trim());
        lines.set(indexTitular, "Titular: " + nuevoTitular.trim());

        LocalDate fecha = leerFecha(lines);
        if (fecha == null) {
            fecha = LocalDate.now();
            lines = actualizarFecha(lines, fecha);
        }

        lines = lines.stream()
            .filter(l -> !l.trim().toLowerCase().startsWith("plan:"))
            .collect(Collectors.toList());
        lines.add("Plan: " + modoLicencia.toUpperCase());

        Files.write(path, lines);

        String discoId = HardwareFingerprintProvider.obtenerSerialDisco();
        enviarRegistroWeb(nuevoCI.trim(), nuevoTitular.trim(), fecha, discoId, modoLicencia);

        if (!"3000000".equals(nuevoCI.trim()) &&
            !nuevoTitular.trim().equalsIgnoreCase("Juan Peres") &&
            !nuevoTitular.trim().equalsIgnoreCase("Clientes Varios")) {

            HardwareFingerprintProvider.actualizarDatosEnDLL(nuevoCI.trim(), nuevoTitular.trim(), modoLicencia);
            System.out.println("📝 DLL actualizado con datos reales.");
            } else {
            System.out.println("⛔ DLL no actualizado: datos genéricos detectados.");
        }
    JOptionPane.showMessageDialog(null, "✅ Registro completado.");
 }
public boolean enviarRegistroWeb(String ci, String titular, LocalDate fecha, String discoId, String modo) {
        try {
            JSONArray clavesArray = new JSONArray();
            Path activoPath = Paths.get(INSTALL_FILE);

            if (Files.exists(activoPath)) {
                List<String> lineas = Files.readAllLines(activoPath, StandardCharsets.UTF_8);
                for (String linea : lineas) {
                    if (linea.contains(":")) {
                        String[] partes = linea.split(":");
                        if (partes.length == 2 && partes[0].matches("^[A-Z0-9]+$")) {
                            JSONObject claveObj = new JSONObject();
                            claveObj.put("codigo", partes[0].trim());
                            claveObj.put("estado", partes[1].trim());
                            clavesArray.put(claveObj);
                        }
                    }
                }
            }
            JSONObject datos = new JSONObject();
            datos.put("ci", ci);
            datos.put("titular", titular);
            datos.put("fecha", fecha.toString());
            datos.put("disco_id", discoId);
            datos.put("plan", HardwareFingerprintProvider.getPlanDesdeDLL());
            datos.put("estado", "activo");
            datos.put("claves", clavesArray);
            datos.put("MASTER_KEY", MASTER_KEY);

            URL url = new URL("http://localhost/emsoftpy/registro_cliente.php");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = datos.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder respuesta = new StringBuilder();
                String linea;
                while ((linea = br.readLine()) != null) {
                    respuesta.append(linea.trim());
                }
                System.out.println("📨 Respuesta del servidor: " + respuesta);
            }
            System.out.println("📦 Datos enviados al servidor:");
            System.out.println(datos.toString(4));
            return con.getResponseCode() == 200;

            } catch (IOException | JSONException e) {
         return false;
    } 
 }

static String solicitarDato(String mensaje, String regex) {
        String input;
        do {
            input = JOptionPane.showInputDialog(mensaje);
            if (input == null) return null;
        } while (!input.matches(regex));
        return input;
    }

private static LocalDate leerFecha(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith("Fecha:")) {
                try { return LocalDate.parse(line.substring(6).trim()); }
                catch (DateTimeParseException ignored) {}
            }
        }
        return null;
    }
private static List<String> actualizarFecha(List<String> lines, LocalDate nuevaFecha) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Fecha:")) {
                lines.set(i, "Fecha: " + nuevaFecha);
                return lines;
            }
        }
        lines.add("Fecha: " + nuevaFecha);
        return lines;
    }
public static boolean requiereClaveMensual() {
    try {
        List<String> lines = Files.readAllLines(Paths.get(INSTALL_FILE));
        for (String line : lines) {
            // CAMBIO: Usar Fecha_actualizada en lugar de Fecha
            if (line.startsWith("Fecha_actualizada:")) {
                String fechaStr = line.split(":")[1].trim();
                LocalDate fecha = LocalDate.parse(fechaStr);
                long dias = ChronoUnit.DAYS.between(fecha, LocalDate.now());
                return dias > ConfiguracionLicencia.DIAS_MENSUAL + ConfiguracionLicencia.DIAS_GRACIA_MENSUAL;
            }
        }
    } catch (IOException e) {
        Logger.getInstance().error("⚠️ Error leyendo Fecha_actualizada: " + e.getMessage());
    }
    return true;
}
public static boolean activarSerialMensual(String claveIngresada) throws IOException {
    Path path = Paths.get(INSTALL_FILE);
    List<String> lines = Files.readAllLines(path);
    LocalDate fechaActual = leerFecha(lines);

    if (fechaActual == null) {
        JOptionPane.showMessageDialog(null, "❌ Fecha inválida en activo.dat");
        return false;
    }

    String clave = claveIngresada.trim();
    boolean claveEncontrada = false;
    boolean claveActivada = false;

    for (int i = 0; i < lines.size(); i++) {
        String[] partes = lines.get(i).split(":");
        if (partes.length < 2) continue;

        String serial = partes[0].trim();
        String estado = partes[1].trim().toLowerCase();

        // Desactivar cualquier clave previamente activa
        if (estado.equals("activo")) {
            lines.set(i, serial + ":usado");
        }

        // Buscar la clave ingresada
        if (serial.equalsIgnoreCase(clave)) {
            claveEncontrada = true;

            if (estado.equals("disponible")) {
                lines.set(i, serial + ":activo");
                lines = actualizarFecha(lines, fechaActual.plusDays(30));
                claveActivada = true;
                break;
            } else {
                JOptionPane.showMessageDialog(null, "❌ Clave ya usada o activa.");
                return false;
            }
        }
    }

    if (!claveEncontrada) {
        JOptionPane.showMessageDialog(null, "❌ Clave no encontrada.");
        return false;
    }

    if (claveActivada) {
        Files.write(path, lines);
        return true;
    }

    return false;
}

public static boolean claveMensualActiva() {
    try {
        List<String> lines = Files.readAllLines(Paths.get(INSTALL_FILE));
        return lines.stream().anyMatch(l -> l.endsWith(":activo"));
    } catch (IOException e) {
        return false;
        }
    }

public static boolean validarYActualizarDatosReales(String plan) {
    String ci = HardwareFingerprintProvider.leerCampoDesdeDLL("ci");
    String titular = HardwareFingerprintProvider.leerCampoDesdeDLL("titular");

    // ✅ Siempre verificar si hay datos genéricos
    if ("3000000".equals(ci) || "Juan Peres".equalsIgnoreCase(titular) || "Clientes Varios".equalsIgnoreCase(titular)) {
        Logger.getInstance().warn("⚠️ Datos genéricos detectados. Se requiere ingreso manual.");

        String nuevoCI = solicitarDato("Ingrese CI (7 u 8 dígitos):", "\\d{7,8}");
        if (nuevoCI == null) return false;

        String nuevoTitular = solicitarDato("Ingrese nombre y apellido del titular:", ".* .*");
        if (nuevoTitular == null) return false;

        HardwareFingerprintProvider.actualizarDatosEnDLL(nuevoCI.trim(), nuevoTitular.trim(), plan);
        
        // 🔄 Actualizar también en activo.dat si existe
        try {
            RegistroService.actualizarDatosEnActivo(nuevoCI.trim(), nuevoTitular.trim(), plan);
        } catch (IOException e) {
            Logger.getInstance().error("❌ Error actualizando activo.dat: " + e.getMessage());
        }
        
        Logger.getInstance().info("✅ Datos reales actualizados en DLL y activo.dat");
    }

    return true; // Siempre retorna true si no hay error
}
public static void actualizarDatosEnActivo(String ci, String titular, String plan) throws IOException {
    Path path = Paths.get(INSTALL_FILE);
    if (!Files.exists(path)) return;

    List<String> lines = Files.readAllLines(path);
    
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("CI:")) {
                lines.set(i, "CI: " + ci);
            } else if (line.startsWith("Titular:")) {
                lines.set(i, "Titular: " + titular);
            } else if (line.startsWith("Plan:")) {
                lines.set(i, "Plan: " + plan.toUpperCase());
            }
        }

        Files.write(path, lines);
    }
}