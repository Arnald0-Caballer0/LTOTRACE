package com.arnaldo.tesis;

import com.ibm.as400.access.AS400;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Conexión al IBM i. Los datos (host, usuario, contraseña) ahora vienen de la
 * configuración cifrada en la base, no de valores fijos en el código.
 *
 * Como esta clase se usa de forma estática, usamos un "puente": Spring inyecta
 * el ConfiguracionService una vez al arrancar y lo guardamos en una referencia estática.
 */
@Component
public class IbmIConnection {

    private static ConfiguracionService cfg;

    @Autowired
    public void setCfg(ConfiguracionService c) {
        IbmIConnection.cfg = c;
    }

    private static AS400 system = null;

    public static AS400 getSystem() {
        if (system == null) {
            if (cfg == null) {
                System.err.println("Configuración aún no disponible para conectar al IBM i.");
                return null;
            }
            String host = cfg.get("ibmi.host");
            String user = cfg.get("ibmi.usuario");
            String pass = cfg.getSecreto("ibmi.password");
            system = new AS400(host, user, pass);
        }
        return system;
    }

    public static void disconnect() {
        if (system != null) {
            system.disconnectAllServices();
            system = null;
            System.out.println("Desconectado del IBM i");
        }
    }
    // Prueba la conexión al IBM i validando las credenciales (para "Probar conexión")
    public static boolean probar() {
        if (cfg == null) return false;
        try {
            AS400 sys = new AS400(cfg.get("ibmi.host"), cfg.get("ibmi.usuario"), cfg.getSecreto("ibmi.password"));
            boolean ok = sys.validateSignon();   // valida usuario/clave sin dejar la conexión abierta
            sys.disconnectAllServices();
            return ok;
        } catch (Exception e) {
            System.err.println("Error probando IBM i: " + e.getMessage());
            return false;
        }
    }

}
