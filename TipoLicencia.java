package com.emsoft.pos.forms;

public enum TipoLicencia {
    PRUEBA(ConfiguracionLicencia.DIAS_PRUEBA, 0, "PRUEBA"),
    MENSUAL(ConfiguracionLicencia.DIAS_MENSUAL, ConfiguracionLicencia.DIAS_GRACIA_MENSUAL, "MENSUAL"),
    FULL(Integer.MAX_VALUE, 0, "FULL");
    
    private final int duracionDias;
    private final int diasGracia;
    private final String nombre;
    
    TipoLicencia(int duracionDias, int diasGracia, String nombre) {
        this.duracionDias = duracionDias;
        this.diasGracia = diasGracia;
        this.nombre = nombre;
    }
    
    public int getDuracionDias() { return duracionDias; }
    public int getDiasGracia() { return diasGracia; }
    public String getNombre() { return nombre; }
}