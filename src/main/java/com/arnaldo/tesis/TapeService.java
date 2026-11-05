package com.arnaldo.tesis;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.CommandCall;

public class TapeService {

    /** Identificador del mensaje que IBM i devuelve cuando CHKTAP encuentra un volumen. */
    public static final String MSG_VOLUMEN_ENCONTRADO = "CPC6778";

    /** Longitud del Volume ID en los datos de sustitución del mensaje (&1 de CPC6778). */
    private static final int LONGITUD_VOLUME_ID = 6;

    /** CCSID por defecto (EBCDIC) si no se puede obtener el del sistema. */
    private static final int CCSID_POR_DEFECTO = 37;

    public String obtenerVolumeIdDesdeIbmI(AS400 system, String deviceName) {
        String volumeId = null;
        try {
            CommandCall command = new CommandCall(system);

            // Usamos CHKTAP: es más rápido y genera el mensaje de éxito CPC6778
            String mandato = "CHKTAP DEV("+ deviceName +")";

            if (command.run(mandato)) {
                // Si el comando funciona, buscamos el mensaje de confirmación por su ID,
                // que no depende del idioma ni de la versión del sistema operativo
                AS400Message[] messageList = command.getMessageList();
                for (AS400Message msg : messageList) {
                    if (MSG_VOLUMEN_ENCONTRADO.equals(msg.getID())) {
                        // 1) Fuente principal: los datos de sustitución del mensaje (campo &1),
                        //    independientes del idioma del texto
                        volumeId = volumeIdDesdeSustitucion(msg.getSubstitutionData(), ccsidDe(system));
                        // 2) Respaldo: el texto del mensaje, por si los datos no estuvieran disponibles
                        if (volumeId == null) {
                            volumeId = extraerVolumeId(msg.getText());
                        }
                        break;
                    }
                }
            } else {
                System.err.println("El comando CHKTAP falló. Mensajes:");
                for (AS400Message msg : command.getMessageList()) {
                    System.err.println("- " + msg.getID() + ": " + msg.getText());
                }
            }
        } catch (Exception e) {
            System.err.println("Error en la comunicación con el servidor Power: " + e.getMessage());
        }
        return volumeId;
    }

    /** CCSID del sistema IBM i, o 37 si no se puede determinar. */
    private static int ccsidDe(AS400 system) {
        try {
            int ccsid = system.getCcsid();
            return ccsid > 0 ? ccsid : CCSID_POR_DEFECTO;
        } catch (Exception e) {
            return CCSID_POR_DEFECTO;
        }
    }

    /**
     * Obtiene el Volume ID a partir de los datos de sustitución del mensaje CPC6778
     * ("Volume &1 found on device &2."), donde &1 es el identificador del volumen
     * (6 caracteres EBCDIC). Devuelve null si los datos no están disponibles.
     * (Método aislado para poder probarlo sin conexión al IBM i.)
     */
    public static String volumeIdDesdeSustitucion(byte[] datos, int ccsid) {
        if (datos == null || datos.length < LONGITUD_VOLUME_ID) return null;
        try {
            Object valor = new AS400Text(LONGITUD_VOLUME_ID, ccsid).toObject(datos, 0);
            String volumen = (valor != null) ? valor.toString().trim() : "";
            return volumen.isEmpty() ? null : volumen;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Respaldo: extrae el Volume ID del texto del mensaje CPC6778.
     * Busca la palabra "Volume"/"Volumen" y toma la palabra siguiente,
     * quitando el punto final si lo tuviera. Devuelve null si no lo encuentra.
     * (Método aislado para poder probarlo sin conexión al IBM i.)
     */
    public static String extraerVolumeId(String textoMensaje) {
        if (textoMensaje == null) return null;
        String[] palabras = textoMensaje.split(" ");
        for (int i = 0; i < palabras.length; i++) {
            if (palabras[i].equalsIgnoreCase("volumen") || palabras[i].equalsIgnoreCase("volume")) {
                if (i + 1 < palabras.length) {
                    return palabras[i + 1].replace(".", "").trim();
                }
            }
        }
        return null;
    }
}
