package com.emsoft.pos.forms;

import java.io.IOException;
import javax.swing.JOptionPane;

public class MessageScheduler {

private static boolean deseaActivar = false;
private static boolean decisionTomada = false;

public static void evaluarEstadoYMostrar() throws IOException {
    long dias = HardwareFingerprintProvider.getDiasTranscurridos();

    if (dias == 28) {
        mostrar("Quedan 2 días de prueba.");

    } else if (dias > 30 && dias < 60) {
        Logger.getInstance().info("⏳ Usuario sigue en período de gracia (" + dias + " días).");

        if (!decisionTomada) {
            deseaActivar = mostrarMensajeActivacion(dias);
            decisionTomada = true;

            if (deseaActivar) {
                String clave = RegistroService.solicitarClaveManual();
                if (clave != null && ClaveMensualGenerator.activarClaveMensual(clave)) {
                    Logger.getInstance().info("✅ Clave mensual activada desde mensaje.");
                } else {
                    Logger.getInstance().warn("❌ Clave inválida o cancelada. Continuando en período de gracia.");
                }
            } else {
                Logger.getInstance().info("⏳ Usuario decidió continuar en período de gracia.");
            }
        }

    } else if (dias == 58) {
        mostrar("Has emitido 2 facturas. Quedan 2 días para bloqueo.");

    } else if (dias >= 60) {
        mostrar("El sistema se ha bloqueado. Requiere activación.");
        RegistroService.requiereClaveMensual(); // sin preguntar
    }
}


public static boolean mostrarMensajeActivacion(long dias) {
        int respuesta = JOptionPane.showConfirmDialog(
            null,
            "⏳ Han pasado " + dias + " días desde el vencimiento.\n¿Desea activar una nueva clave mensual ahora?",
            "Activación mensual",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        return respuesta == JOptionPane.YES_OPTION;
    }

private static void mostrar(String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje);
    }

public static void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje, "Sistema POS", JOptionPane.WARNING_MESSAGE);
    }
}
