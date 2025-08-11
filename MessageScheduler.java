/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.emsoft.pos.forms;

import javax.swing.JOptionPane;

/**
 *
 * @author PC-CAsa
 */
public class MessageScheduler {
    public static void evaluarEstadoYMostrar() {
        long dias = InstallManager.diasDesdeInstalacion();
        
        if (dias == 28) {
            mostrar("Quedan 2 días de prueba.");
        } else if (dias == 58) {
            mostrar("Has emitido 2 facturas. Quedan 2 días para bloqueo.");
        } else if (dias >= 60) {
            mostrar("El sistema se ha bloqueado. Requiere activación.");
        }
        // y así con los demás
    }

    private static void mostrar(String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje);
    }
    public static void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje, "Sistema POS", JOptionPane.WARNING_MESSAGE);
    }
}
