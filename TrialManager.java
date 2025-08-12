package com.emsoft.pos.forms;

public class TrialManager {

   private final HardwareFingerprintProvider hardwareProvider;
    private final LicenseValidator licenseValidator;
    private final MonthlyKeyManager monthlyKeyManager;
    private final Logger logger;

    public TrialManager() {
        this.hardwareProvider = new HardwareFingerprintProvider();
        this.licenseValidator = new LicenseValidator();
        this.monthlyKeyManager = new MonthlyKeyManager();
        this.logger = Logger.getInstance();
    }

    public TrialState validarEstadoDelSistema() {
        logger.info("🔍 Iniciando validación del sistema...");

        if (!HardwareFingerprintProvider.existeArchivoActivo()) {
            logger.warn("❌ No se encontró activo.dat");
            return TrialState.ARCHIVO_INEXISTENTE;
        }

        ConfigData config = HardwareFingerprintProvider.leerActivoDat();
        if (config == null) {
            logger.error("⚠️ Error al leer activo.dat");
            return TrialState.CONFIG_ERROR;
        }

        if (licenseValidator.licenciaExpirada(config)) {
            logger.warn("⛔ Licencia expirada");
            return TrialState.LICENCIA_EXPIRADA;
        }

        if (licenseValidator.licenciaActiva(config)) {
            logger.info("✅ Licencia activa");
            return TrialState.LICENCIA_ACTIVA;
        }

        if (monthlyKeyManager.claveMensualValida(config)) {
            logger.info("🔑 Clave mensual válida");
            return TrialState.CLAVE_MENSUAL_VALIDA;
        }

        logger.warn("🕒 Prueba inicial en curso");
        return TrialState.PRUEBA_INICIAL;
    }
}
