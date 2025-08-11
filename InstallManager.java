package com.emsoft.pos.forms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JOptionPane;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author PC-CAsa
 */
public class InstallManager {

    private static final String INSTALL_FILE = System.getProperty("user.dir") + "/doc/activo.dat";
    private static final String PRUEBA_KEY = "EMSOFT-1234";
    private static final String MASTER_KEY = "EMSOFT-M2025";
    private static final String FULL_KEY = "EMSOFT-FULL";
    public static String getMasterKey() { return MASTER_KEY; }
    
public static boolean checkInstallation() {
    System.out.println("⏳ [InstallManager] Iniciando checkInstallation...");

    Path path = Paths.get(INSTALL_FILE);
    boolean activoExiste = Files.exists(path);

    try {
        // 🧱 Paso 0: Inicializar si no existe activo.dat
        if (!activoExiste) {
            System.out.println("📁 [InstallManager] activo.dat no encontrado. Requiere inicialización.");
            String input = JOptionPane.showInputDialog("🔒 Ingrese clave maestra para inicializar el sistema:");

            if (input == null || !input.equals(MASTER_KEY)) {
                JOptionPane.showMessageDialog(null, "❌ Clave incorrecta. No se puede inicializar.");
                return false;
            }

            List<String> lines = new ArrayList<>();
            lines.add("Fecha: " + LocalDate.now());
            for (int i = 1; i <= 5; i++) {
                lines.add(generateSimpleKey(i) + ":false");
            }

            Files.createDirectories(path.getParent());
            Files.write(path, lines);
            JOptionPane.showMessageDialog(null, "✅ Sistema inicializado correctamente.");
        }

        // 🔐 Paso 1: Solicitar clave de activación
        System.out.println("🔐 [InstallManager] Requiere activación manual.");
        String input = JOptionPane.showInputDialog("🔒 Ingrese clave para activar:");

        if (input == null || (!input.equals(MASTER_KEY) && !input.equals(FULL_KEY))) {
            JOptionPane.showMessageDialog(null, "❌ Clave incorrecta. No se puede continuar.");
            return false;
        }

        // 🧩 Paso 2: Obtener datos desde DLL
        String planDLL = SistemaHelper.getPlanDesdeDLL();
        LocalDate fechaInstalacion = SistemaHelper.getFechaDesdeDLL();
        List<String> lines = actualizarFecha(new ArrayList<>(), fechaInstalacion);

        // 🔒 Activación FULL
        if (input.equals(FULL_KEY)) {
            System.out.println("🔍 [InstallManager] Clave FULL detectada.");
            if ("FULL".equalsIgnoreCase(planDLL)) {
                SistemaHelper.reconstruirActivoDesdeDLL();
                JOptionPane.showMessageDialog(null, "✅ Sistema restaurado como FULL desde DLL.");
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "❌ El plan en el DLL no es FULL. No se puede activar como FULL.");
                return false;
            }
        }

        // 📦 Activación MENSUAL
        if (input.equals(MASTER_KEY)) {
            System.out.println("🔍 [InstallManager] Clave MENSUAL detectada.");
            lines.add("CI: 3000000");
            lines.add("Titular: Juan Peres");
            lines.add("Plan: MENSUAL");
            for (int i = 1; i <= 5; i++) {
                lines.add(generateSimpleKey(i) + ":disponible");
            }

            Files.write(path, lines);
            completarRegistroUsuario("MENSUAL");

            String ci = SistemaHelper.extraerCampoDesdeActivo("CI:");
            String titular = SistemaHelper.extraerCampoDesdeActivo("Titular:");
            SistemaHelper.actualizarDatosEnDLL(ci, titular, "MENSUAL");

            JOptionPane.showMessageDialog(null, "✅ Sistema activado como MENSUAL.");
            return true;
        }

    } catch (IOException e) {
        System.err.println("❌ [InstallManager] Error en checkInstallation: " + e.getMessage());
        return false;
    }

    System.out.println("⛔ [InstallManager] Finalizando checkInstallation sin activación.");
    return false;
}

public static boolean isFullActivated() throws IOException {
    Path path = Paths.get(System.getProperty("user.dir") + "/doc/activo.dat");
    String planActivo = leerPlanDesdeActivo(path); // lectura directa desde ruta
    String planDLL = SistemaHelper.getPlanDesdeDLL(); // desde DLL

    if (planActivo == null) {
        // activo.dat fue eliminado → no se reconstruye automáticamente
        return false; // se debe pedir clave FULL nuevamente
    }

    // Si el DLL dice FULL pero activo no coincide → adulteración
    if ("FULL".equalsIgnoreCase(planDLL) && !"FULL".equalsIgnoreCase(planActivo)) {
        System.out.println("Plan DLL: " + planDLL);
         System.out.println("Plan activo.dat: " + planActivo);
        return false; // se debe pedir clave FULL nuevamente
    }

    return "FULL".equalsIgnoreCase(planActivo) && "FULL".equalsIgnoreCase(planDLL);

}


public static String leerPlanDesdeActivo(Path path) {
    if (!Files.exists(path)) return null;

    try (BufferedReader reader = Files.newBufferedReader(path)) {
        String linea;
        while ((linea = reader.readLine()) != null) {
            if (linea.trim().startsWith("plan=")) {
                return linea.trim().substring(5).toUpperCase(); // extrae y normaliza
            }
        }
    } catch (IOException e) {
        // Podés loguear el error si querés trazabilidad
    }

    return null; // si no se encuentra la línea o hay error
}

public static void activarLicenciaFull() throws IOException {
    Path path = Paths.get(INSTALL_FILE);

    if (!Files.exists(path)) {
        String planDLL = SistemaHelper.getPlanDesdeDLL();

        if ("FULL".equalsIgnoreCase(planDLL)) {
            SistemaHelper.reconstruirActivoDesdeDLL();
            JOptionPane.showMessageDialog(null, "✅ Sistema restaurado como FULL desde DLL.");
            return;
        } else if ("MENSUAL".equalsIgnoreCase(planDLL)) {
            JOptionPane.showMessageDialog(null,
                "⚠️ El sistema está en modo MENSUAL.\n" +
                "Debe ingresar los datos del cliente para activar como FULL.");

            completarRegistroUsuario("FULL");
            return;
        } else {
            JOptionPane.showMessageDialog(null,
                "❓ Plan desconocido en DLL.\n" +
                "No se puede continuar con la activación.");
            return;
        }
    }
    // Si activo.dat existe, seguimos con la lógica original
    List<String> lines = Files.readAllLines(path);
    boolean yaActivado = lines.stream()
        .anyMatch(l -> l.trim().equalsIgnoreCase(FULL_KEY + ":activo"));

    if (!yaActivado) {
        lines.add(FULL_KEY + ":activo");
        lines = actualizarFecha(lines, LocalDate.now());

        lines = lines.stream()
            .filter(l -> !l.trim().toLowerCase().startsWith("plan:"))
            .collect(Collectors.toList());
        lines.add("Plan: FULL");
        Files.createDirectories(path.getParent());
        Files.write(path, lines);
        completarRegistroUsuario("FULL");
        JOptionPane.showMessageDialog(null, "✅ Licencia perpetua activada.");
    }
}


  public static void completarRegistroUsuario(String modoLicencia) throws IOException {
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

    // 🔧 Verificar o generar fecha
    LocalDate fecha = leerFecha(lines);
    if (fecha == null) {
        fecha = LocalDate.now();
        lines = actualizarFecha(lines, fecha);
    }

    // 🧹 Eliminar duplicados de "Plan:"
    lines = lines.stream()
        .filter(l -> !l.trim().toLowerCase().startsWith("plan:"))
        .collect(Collectors.toList());

    // ✅ Agregar plan según modoLicencia
    lines.add("Plan: " + modoLicencia.toUpperCase());

    Files.write(path, lines);

    String discoId = SistemaHelper.obtenerSerialDisco();

    System.out.println("📆 Fecha que se va a enviar: " + fecha);
    enviarRegistroWeb(nuevoCI.trim(), nuevoTitular.trim(), fecha, discoId, modoLicencia);

    // ✅ Validar que los datos no sean genéricos antes de actualizar el DLL
    if (!"3000000".equals(nuevoCI.trim()) &&
        !nuevoTitular.trim().equalsIgnoreCase("Juan Peres") &&
        !nuevoTitular.trim().equalsIgnoreCase("Clientes Varios")) {

        SistemaHelper.actualizarDatosEnDLL(nuevoCI.trim(), nuevoTitular.trim(), modoLicencia);
        System.out.println("📝 DLL actualizado con datos reales.");
    } else {
        System.out.println("⛔ DLL no actualizado: datos genéricos detectados.");
    }

    JOptionPane.showMessageDialog(null, "✅ Registro completado.");
}

   
public static boolean enviarRegistroWeb(String ci, String titular, LocalDate fecha, String discoId, String modo) {
    try {
        // Construimos el arreglo claves desde activo.dat
        JSONArray clavesArray = new JSONArray();
        Path activoPath = Paths.get("C:\\LaPapa_sistema\\doc\\activo.dat");

        if (Files.exists(activoPath)) {
            List<String> lineas = Files.readAllLines(activoPath, StandardCharsets.UTF_8);

            // Saltamos la primera línea si contiene CI
            int inicioClaves = 0;
            if (!lineas.isEmpty() && lineas.get(0).trim().startsWith("CI:")) {
                inicioClaves = 1;
            }

            for (int i = inicioClaves; i < lineas.size(); i++) {
                String linea = lineas.get(i).trim();
                if (linea.contains(":")) {
                    String[] partes = linea.split(":");
                    if (partes.length == 2 && partes[0].matches("^[A-Z0-9]+$")) {
                        String codigo = partes[0].replaceAll("\\s+", "");
                        String estado = partes[1].replaceAll("\\s+", "");
                        JSONObject claveObj = new JSONObject();
                        claveObj.put("codigo", codigo); // 👈 Este debe ser el nombre correcto
                        claveObj.put("estado", estado);
                        clavesArray.put(claveObj);
                    }
                }
            }
        }
        // Creamos el objeto principal de datos
        JSONObject datos = new JSONObject();
        datos.put("ci", ci);
        datos.put("titular", titular);
        datos.put("fecha", fecha.toString());
        datos.put("disco_id", discoId);
        datos.put("plan", SistemaHelper.getPlanDesdeDLL());
        datos.put("estado", "activo");
        datos.put("claves", clavesArray);
        datos.put("MASTER_KEY", MASTER_KEY); // ✅ Se incluye la clave maestra

        // Enviamos los datos al servidor
        URL url = new URL("http://localhost/emsoftpy/registro_cliente.php");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

             // Enviar JSON
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = datos.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        // Leer respuesta del servidor
        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder respuesta = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) {
                respuesta.append(linea.trim());
            }
            System.out.println("📨 Respuesta del servidor: " + respuesta);
        }
System.out.println("📦 Datos enviados al servidor:");
System.out.println("📦 Claves preparadas:");
System.out.println(datos.toString(4)); // El parámetro '4' añade indentación para mejor lectura

        int code = con.getResponseCode();
        return code == 200;

    } catch (IOException | JSONException e) {
        return false;
    }
    
}
    public static boolean activarSerialMensual(String claveIngresada) throws IOException {
        Path path = Paths.get(INSTALL_FILE);
        List<String> lines = Files.readAllLines(path);
        LocalDate fechaActual = leerFecha(lines);
        if (fechaActual == null) return false;

        for (int i = 0; i < lines.size(); i++) {
            String[] partes = lines.get(i).split(":");
            if (partes.length >= 2 && partes[1].equalsIgnoreCase("activo")) {
                lines.set(i, partes[0] + ":usado");
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            String[] partes = lines.get(i).split(":");
            if (partes.length >= 2 && partes[0].equalsIgnoreCase(claveIngresada.trim())) {
                if (partes[1].equalsIgnoreCase("disponible")) {
                    lines.set(i, partes[0] + ":activo");
                    lines = actualizarFecha(lines, fechaActual.plusDays(30));
                    Files.write(path, lines);
                    return true;
                } else {
                    JOptionPane.showMessageDialog(null, "❌ Clave ya usada o activa.");
                    return false;
                }
            }
        }

        JOptionPane.showMessageDialog(null, "❌ Clave no encontrada.");
        return false;
    }
private static final Logger logger = Logger.getLogger(InstallManager.class.getName());

    public static long diasDesdeInstalacion() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(INSTALL_FILE));
            LocalDate fecha = leerFecha(lines);
            return (fecha != null) ? ChronoUnit.DAYS.between(fecha, LocalDate.now()) : -1;
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
            return -1;
        }
    }
public static String getPlanActual() throws IOException {
    Path path = Paths.get(INSTALL_FILE);
    if (Files.exists(path)) {
        for (String line : Files.readAllLines(path)) {
            if (line.trim().toLowerCase().startsWith("plan:")) {
                return line.substring(5).trim().toUpperCase();
            }
        }
    }

    return SistemaHelper.getPlanDesdeDLL(); // fallback
}

    public static LocalDate getInstallDate() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(INSTALL_FILE));
            return leerFecha(lines);
        } catch (IOException e) {
            return null;
        }
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

    private static String solicitarDato(String mensaje, String regex) {
        String input;
        do {
            input = JOptionPane.showInputDialog(mensaje);
            if (input == null) return null;
        } while (!input.matches(regex));
        return input;
    }

    private static String generateSimpleKey(int par) {
        return (char) ('A' + new Random().nextInt(26)) + String.valueOf(100 + new Random().nextInt(900));
    }
}


