package com.arnaldo.tesis;

import com.ibm.as400.access.AS400;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- PRUEBA DE INFRAESTRUCTURA ---");
        DatabaseConnection.getConnection();

        System.out.println("\n--- CONECTANDO AL SERVIDOR POWER ---");
        AS400 system = IbmIConnection.getSystem();

        try {
            String barcodePrueba = "ABC001";
            String unidadTape = "TAP07";

            TapeService tapeService = new TapeService();
            CintaDAO cintaDAO = new CintaDAO();

            System.out.println("Iniciando proceso de Relacionar Nombre...");

            // Llamamos al nuevo método pasando el objeto 'system'
            String nombreEncontrado = tapeService.obtenerVolumeIdDesdeIbmI(system, unidadTape);

            if (nombreEncontrado != null) {
                System.out.println("ℹIBM i confirmó físicamente que la cinta es: " + nombreEncontrado);
                cintaDAO.actualizarVolumeId(barcodePrueba, nombreEncontrado);
            } else {
                System.err.println("No se pudo extraer el nombre de los mensajes del sistema.");
            }
        } finally {
            IbmIConnection.disconnect();
        }
    }
}
