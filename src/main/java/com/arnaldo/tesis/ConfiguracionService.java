package com.arnaldo.tesis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

/**
 * Lee y guarda parámetros de configuración.
 * Los valores "secretos" (contraseñas) se cifran con AES antes de guardarse,
 * usando una clave que vive FUERA de la base (application.properties).
 */
@Service
public class ConfiguracionService {

    @Autowired
    private ConfiguracionRepository repo;

    @Value("${lto.crypto.password}")
    private String cryptoPassword;

    @Value("${lto.crypto.salt:0458349c980526e7}")
    private String cryptoSalt;

    private TextEncryptor encryptor() {
        return Encryptors.text(cryptoPassword, cryptoSalt);
    }

    /** Valor tal cual (para datos no sensibles: ip, usuario, drive...). */
    public String get(String clave) {
        return repo.findById(clave).map(Configuracion::getValor).orElse("");
    }

    public void set(String clave, String valor) {
        repo.save(new Configuracion(clave, valor != null ? valor : ""));
    }

    /** Devuelve un secreto descifrado (para usarlo al conectar). */
    public String getSecreto(String clave) {
        String cif = get(clave);
        if (cif == null || cif.isEmpty()) return "";
        try {
            return encryptor().decrypt(cif);
        } catch (Exception e) {
            System.err.println("No se pudo descifrar '" + clave + "': " + e.getMessage());
            return "";
        }
    }

    /** Guarda un secreto cifrado. */
    public void setSecreto(String clave, String valorPlano) {
        String cif = (valorPlano == null || valorPlano.isEmpty()) ? "" : encryptor().encrypt(valorPlano);
        set(clave, cif);
    }

    public boolean tieneSecreto(String clave) {
        String v = get(clave);
        return v != null && !v.isEmpty();
    }
}
