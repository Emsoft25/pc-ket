//  Emsoft Pos -Punto de venta
// Copyright (c) 2024 Emsoft Informática

package com.emsoft.pos.forms;

import com.formdev.flatlaf.FlatLightLaf;
import com.emsoft.format.Formats;
import com.emsoft.pos.instance.InstanceQuery;
import com.emsoft.pos.ticket.TicketInfo;
import lombok.extern.slf4j.Slf4j;
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
public class StartPOS {

    private StartPOS() {
    }

    public static boolean registerApp() {

        InstanceQuery i = null;
        try {
            i = new InstanceQuery();
            i.getAppMessage().restoreWindow();
            return false;
        } catch (RemoteException | NotBoundException e) {
            return true;
        }
    }
    public static void main(final String args[]) throws IOException {
        TrialManager trialManager = new TrialManager();
        TrialState estado = trialManager.validarEstadoDelSistema();

        if (estado == TrialState.ARCHIVO_INEXISTENTE ||
            estado == TrialState.CONFIG_ERROR ||
            estado == TrialState.LICENCIA_EXPIRADA) {
            System.out.println("⛔ Estado inválido: " + estado);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            if (!registerApp()) {
                System.exit(1);
            }
            // Verificación de instalación y reserva de claves
            InstalacionValidator validator = new InstalacionValidator();
            if (!validator.validar()) {
                System.exit(1);
             }
            AppConfig config = new AppConfig(args);

            config.load();

            String slang = config.getProperty("user.language");
            String scountry = config.getProperty("user.country");
            String svariant = config.getProperty("user.variant");
            if (slang != null
                    && !slang.equals("")
                    && scountry != null
                    && svariant != null) {
                Locale.setDefault(new Locale(slang, scountry, svariant));
            }

            Formats.setIntegerPattern(config.getProperty("format.integer"));
            Formats.setDoublePattern(config.getProperty("format.double"));
            Formats.setCurrencyPattern(config.getProperty("format.currency"));
            Formats.setPercentPattern(config.getProperty("format.percent"));
            Formats.setDatePattern(config.getProperty("format.date"));
            Formats.setTimePattern(config.getProperty("format.time"));
            Formats.setDateTimePattern(config.getProperty("format.datetime"));

            // Set the look and feel
            FlatLightLaf.setup();

            try {
                String defaultLafClassName = config.getProperty("swing.defaultlaf");
                Class<?> lafClass = Class.forName(defaultLafClassName);
                Object laf = lafClass.getDeclaredConstructor().newInstance();
                if (!(laf instanceof MetalLookAndFeel) && laf instanceof LookAndFeel) {
                    UIManager.setLookAndFeel((LookAndFeel) laf);
                } else {
                    UIManager.setLookAndFeel("com.formdev.flatlaf.intellijthemes.FlatGrayIJTheme");
                }
            } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException | UnsupportedLookAndFeelException e) {
                log.error("Cannot set Look and Feel ${0}", e.getMessage());
            }

            String hostname = config.getProperty("machine.hostname");
            TicketInfo.setHostname(hostname);

            String screenmode = config.getProperty("machine.screenmode");

            if ("fullscreen".equals(screenmode)) {
                JRootKiosk rootkiosk = new JRootKiosk();
                try {
                    try {
                        rootkiosk.initFrame(config);
                    } catch (NoSuchMethodException ex) {
                        Logger.getLogger(StartPOS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                }
            } else {
                JRootFrame rootframe = new JRootFrame();
                try {
                    rootframe.initFrame(config);
                } catch (NoSuchMethodException ex) {
                    log.error(ex.getMessage());
                }
            }
        });
    }
}
