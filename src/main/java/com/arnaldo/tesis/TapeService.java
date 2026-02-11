package com.arnaldo.tesis;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;

public class TapeService {

    public String obtenerVolumeIdDesdeIbmI(AS400 system, String deviceName) {
        String volumeId = null;
        try {
            CommandCall command = new CommandCall(system);

            // Usamos CHKTAP: es más rápido y genera el mensaje de éxito CPC6778
            String mandato = "CHKTAP DEV("+ deviceName +")";
            //System.out.println("Ejecutando comando en IBM i: " + mandato);

            if (command.run(mandato)) {
                // Si el comando funciona, buscamos el mensaje de confirmación
                AS400Message[] messageList = command.getMessageList();
                for (AS400Message msg : messageList) {
                    // El mensaje CPC6778 dice: "Volume XXXXXX found on device YYYYYY."
                    if (msg.getID().equals("CPC6778")) {
                        String texto = msg.getText();
                        String[] palabras = texto.split(" ");

                        // Buscamos la posición de la palabra "volumen" (o "Volume")
                        for (int i = 0; i < palabras.length; i++) {
                            if (palabras[i].equalsIgnoreCase("volumen") || palabras[i].equalsIgnoreCase("volume")) {
                                // El ID de la cinta siempre es la siguiente palabra
                                if (i + 1 < palabras.length) {
                                    volumeId = palabras[i + 1].replace(".", "").trim(); // Quitamos el punto final si existe
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
            } else {
                System.err.println("El comando CHKTAP falló. Mensajes:");
                for (AS400Message msg : command.getMessageList()) {
                    System.err.println("- " + msg.getText());
                }
            }
        } catch (Exception e) {
            System.err.println("Error en la comunicación con el servidor Power: " + e.getMessage());
        }
        return volumeId;
    }
}