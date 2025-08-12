package com.emsoft.pos.forms;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class InstallInfoProvider {

    public static long diasDesdeInstalacion() {
        LocalDate fechaInstalacion = HardwareFingerprintProvider.getFechaDesdeDLL(); // o desde activo.dat
        if (fechaInstalacion == null) return -1;

        return ChronoUnit.DAYS.between(fechaInstalacion, LocalDate.now());
    }
}
