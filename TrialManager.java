package com.emsoft.pos.forms;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.swing.JOptionPane;
import org.json.JSONException;
import org.json.JSONObject;

public class TrialManager {
    private static final int DIAS_PRUEBA = 30;
    private static final int DIAS_USO_LIMITADO = 30;
    private static final int DIAS_GRACIA = 20;

    public enum EstadoTrial {
        PRUEBA_INICIAL,
        PLANMENSUAL,
        FULL_ACTIVADO,
        BLOQUEADO,
        DESCONOCIDO
    }

    public static EstadoTrial obtenerEstadoTrial() throws IOException {
        LocalDate fechaInstalacion = InstallManager.getInstallDate();
        if (fechaInstalacion == null) {
            System.err.println("⚠️ Fecha de instalación no encontrada.");
            return EstadoTrial.DESCONOCIDO;
        }

        long diasTranscurridos = ChronoUnit.DAYS.between(fechaInstalacion, LocalDate.now());
        System.out.println("📅 Días desde instalación: " + diasTranscurridos);

        if (diasTranscurridos <= DIAS_PRUEBA) {
            return EstadoTrial.PRUEBA_INICIAL;
        } else if (diasTranscurridos <= DIAS_PRUEBA + DIAS_USO_LIMITADO) {
            return EstadoTrial.FULL_ACTIVADO;
        } else if (diasTranscurridos <= DIAS_PRUEBA + DIAS_USO_LIMITADO + DIAS_GRACIA) {
            return EstadoTrial.PLANMENSUAL;
        } else {
            return EstadoTrial.BLOQUEADO;
        }
    }

   public static boolean validarEntornoLocal() throws IOException {
    if (!SistemaHelper.validarHuella()) {
        MessageScheduler.mostrarAdvertencia("⚠️ No se encontró la huella del sistema. Esta PC no está autorizada.");
        return false;
    }

    try {
        Path rutaHuella = Paths.get(System.getenv("APPDATA") + "/EmsoftPOS/copiasistema.dll");
        String contenido = Files.readString(rutaHuella);
        JSONObject huella = new JSONObject(contenido);
        String discoHuella = huella.optString("disco_id", "").trim();
        String discoActual = SistemaHelper.obtenerSerialDisco();

        System.out.println("🔍 DISCO ACTUAL: " + discoActual);
        System.out.println("🔍 DISCO REGISTRADO: " + discoHuella);

        if (!discoActual.equalsIgnoreCase(discoHuella)) {
            MessageScheduler.mostrarAdvertencia("🚫 El disco actual no coincide con el registrado.\nEntorno modificado o clonado.");
            return false;
        }

        // 🧠 Validar estado del sistema
        EstadoTrial estado = obtenerEstadoTrial();
        long diasTranscurridos = ChronoUnit.DAYS.between(InstallManager.getInstallDate(), LocalDate.now());

        switch (estado) {
            case PRUEBA_INICIAL:
                return validarEstadoPruebaInicial(diasTranscurridos);

            case FULL_ACTIVADO:
                return validarEstadoFullActivado();

            case PLANMENSUAL:
                return validarEstadoPlanMensual();

            case BLOQUEADO:
            case DESCONOCIDO:
            default:
                System.out.println("⛔ Sistema bloqueado o estado desconocido.");
                return validarDesbloqueoPorClave();
        }

    } catch (IOException | JSONException e) {
        MessageScheduler.mostrarAdvertencia("❌ Error al validar el entorno: " + e.getMessage());
        return false;
    }
}


  private static boolean validarEstadoPruebaInicial(long diasTranscurridos) throws IOException {
    System.out.println("🧪 Estado de prueba inicial. Días: " + diasTranscurridos);

    if (diasTranscurridos <= DIAS_PRUEBA) {
        return true;
    } else {
        System.out.println("⛔ Prueba inicial vencida. Se requiere clave.");
        InstallManager.checkInstallation();
return true;

    } // ← faltaba este cierre
}

    private static boolean validarEstadoPlanMensual() throws IOException {
        System.out.println("📦 Validando estado PLAN MENSUAL...");

        if (SistemaHelper.validarHuella()) {
             String ci = SistemaHelper.extraerCampoDesdeActivo("ci:");
              String plan = SistemaHelper.extraerCampoDesdeActivo("plan:");

            if (!"GENÉRICO".equalsIgnoreCase(ci) && !"GENÉRICO".equalsIgnoreCase(plan)) {
                MessageScheduler.evaluarEstadoYMostrar();
                return true;
            } else {
                System.out.println("⚠️ CI o Plan genéricos. No se puede validar mensual.");
            }
        } else {
            System.out.println("❌ Huella inválida.");
        }

        return validarDesbloqueoPorClave();
    }

    private static boolean validarEstadoFullActivado() throws IOException {
        System.out.println("🔒 Validando estado FULL ACTIVADO...");

        if (InstallManager.isFullActivated()) {
            System.out.println("✅ Activación FULL confirmada.");
            return true;
        } else {
            System.out.println("❌ Activación FULL inválida.");
            return false;
        }
    }

    private static boolean validarDesbloqueoPorClave() throws IOException {
        MessageScheduler.mostrarAdvertencia("⏳ El período ha terminado.\nIngrese clave mensual o maestra:");

        String clave = JOptionPane.showInputDialog("🔐 Clave de activación:");
        if (clave == null || clave.trim().isEmpty()) return false;

        if (clave.equals(InstallManager.getMasterKey())) {
            MessageScheduler.mostrarAdvertencia("✅ Clave maestra aceptada. Acceso completo.");
            InstallManager.activarLicenciaFull();
            return true;
        }

        try {
            if (InstallManager.activarSerialMensual(clave.trim())) {
                MessageScheduler.evaluarEstadoYMostrar();
                MessageScheduler.mostrarAdvertencia("✅ Activación válida. Licencia extendida.");
                return true;
            }
        } catch (IOException e) {
            MessageScheduler.mostrarAdvertencia("❌ Error al activar: " + e.getMessage());
        }

        MessageScheduler.mostrarAdvertencia("❌ Clave inválida o ya usada.");
        return false;
    }

    public static long obtenerDiasRestantes() {
        LocalDate fechaInstalacion = InstallManager.getInstallDate();
        if (fechaInstalacion == null) return 0;

        long diasTranscurridos = ChronoUnit.DAYS.between(fechaInstalacion, LocalDate.now());
        long diasTotales = DIAS_PRUEBA + DIAS_GRACIA + DIAS_USO_LIMITADO;
        return Math.max(0, diasTotales - diasTranscurridos);
    }
}