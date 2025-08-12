package com.emsoft.pos.forms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONException;
import org.json.JSONObject;

public class InstalacionValidator {

    private final Logger logger = Logger.getInstance();
    private final HardwareFingerprintProvider hardwareProvider = new HardwareFingerprintProvider();

    public boolean validar() {
        logger.info("🔍 [InstalacionValidator] Iniciando validación de entorno...");

        // Validar existencia de activo.dat
        if (!HardwareFingerprintProvider.existeArchivoActivo()) {
            logger.warn("❌ activo.dat no encontrado. El sistema no puede continuar.");
            return false;
        }

        // Validar huella del sistema (DLL)
        if (!validarHuellaDLL()) {
            logger.warn("❌ Huella del sistema inválida o no encontrada.");
            return false;
        }

        // Validar disco registrado
        if (!validarDisco()) {
            logger.warn("❌ El disco actual no coincide con el registrado.");
            return false;
        }

        logger.info("✅ Entorno validado correctamente.");
        return true;
    }

    private boolean validarHuellaDLL() {
        try {
            Path rutaHuella = Paths.get(System.getenv("APPDATA") + "/EmsoftPOS/copiasistema.dll");
            if (!Files.exists(rutaHuella)) {
                logger.warn("📁 DLL de huella no encontrada.");
                return false;
            }

            String contenido = Files.readString(rutaHuella);
            JSONObject huella = new JSONObject(contenido);
            String discoHuella = huella.optString("disco_id", "").trim();

            return !discoHuella.isEmpty();
        } catch (IOException | JSONException e) {
            logger.error("💥 Error al validar huella DLL: " + e.getMessage());
            return false;
        }
    }

private boolean validarDisco() {
    try {
        String serialActual = HardwareFingerprintProvider.obtenerSerialDisco();
        Path rutaHuella = Paths.get(System.getenv("APPDATA") + "/EmsoftPOS/copiasistema.dll");

        if (!Files.exists(rutaHuella)) {
            logger.warn("⚠️ Archivo de huella no encontrado: " + rutaHuella.toString());
            return false;
        }
        String contenido = Files.readString(rutaHuella);
        JSONObject huella = new JSONObject(contenido);
        String discoRegistrado = huella.optString("disco_id", "").trim();

        logger.info("🔍 Disco actual: " + serialActual);
        logger.info("🔍 Disco registrado: " + discoRegistrado);

        return serialActual.equalsIgnoreCase(discoRegistrado);
    } catch (IOException | JSONException e) {
        logger.error("💥 Error al validar disco: " + e.getMessage());
        return false;
    }
}

}
