package com.arnaldo.tesis;

import com.ibm.as400.access.AS400;

public class IbmIConnection {
    // Datos de tu LPAR de contingencia
    private static final String SYSTEM_NAME = "***IP***";
    private static final String USER = "qsecofr";
    private static final String PASSWORD = "***ELIMINADO***";

    private static AS400 system = null;

    public static AS400 getSystem() {
        if (system == null) {
            // Creamos la conexión al servidor Power
            system = new AS400(SYSTEM_NAME, USER, PASSWORD);
        }
        return system;
    }

    public static void disconnect() {
        if (system != null) {
            system.disconnectAllServices();
            system = null;
            System.out.println("🔌 Desconectado del IBM i");
        }
    }
}
