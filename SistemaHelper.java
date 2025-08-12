package com.emsoft.pos.forms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SistemaHelper {

    private final Logger logger = Logger.getInstance();

    public boolean existeArchivoActivo() {
        File archivo = new File("activo.dat");
        boolean existe = archivo.exists();
        logger.info("📁 Verificando existencia de activo.dat: " + existe);
        return existe;
    }

    public ConfigData leerActivoDat() {
        try (BufferedReader reader = new BufferedReader(new FileReader("activo.dat"))) {
            Map<String, String> campos = new HashMap<>();
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split("=");
                if (partes.length == 2) {
                    campos.put(partes[0].trim(), partes[1].trim());
                }
            }
            logger.info("📄 activo.dat leído correctamente");
            return new ConfigData(campos);
        } catch (IOException e) {
            logger.error("❌ Error al leer activo.dat: " + e.getMessage());
            return null;
        }
    }
}

