package com.emsoft.pos.forms;

public class ResultadoValidacion {
    private final boolean valido;
    private final TipoLicencia tipo;
    private final int diasRestantes;
    private final boolean enGracia;
    private final boolean tieneClavesDisponibles;
    private final String mensaje;
    
   public static ResultadoValidacion valido(TipoLicencia tipo, int diasRestantes) {
    return new ResultadoValidacion(true, tipo, diasRestantes, false, true,
        "Licencia válida");
}
    
     public static ResultadoValidacion enGracia(TipoLicencia tipo, int diasGracia, boolean tieneClaves) {
        return new ResultadoValidacion(true, tipo, diasGracia, true, tieneClaves,
            "En período de gracia: " + diasGracia + " días restantes");
    }
    
   public static ResultadoValidacion vencido(String mensaje) {
    return new ResultadoValidacion(false, null, 0, false, false, mensaje);
}
    
   public static ResultadoValidacion error(String mensaje) {
    return new ResultadoValidacion(false, null, 0, false, false, mensaje);
}
    
     private ResultadoValidacion(boolean valido, TipoLicencia tipo, int diasRestantes, 
                               boolean enGracia, boolean tieneClavesDisponibles, String mensaje) {
        this.valido = valido;
        this.tipo = tipo;
        this.diasRestantes = diasRestantes;
        this.enGracia = enGracia;
        this.tieneClavesDisponibles = tieneClavesDisponibles;
        this.mensaje = mensaje;
    }
    
    public boolean isValido() { return valido; }
    public TipoLicencia getTipo() { return tipo; }
    public int getDiasRestantes() { return diasRestantes; }
    public boolean isEnGracia() { return enGracia; }
    public String getMensaje() { return mensaje; }
    public boolean tieneClavesDisponibles() { return tieneClavesDisponibles; }
}