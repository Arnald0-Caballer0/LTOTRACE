package com.arnaldo.tesis;

public class LibraryModels {

    // Para el inicio de sesión
    class LoginRequest {
        public String username;
        public String password;
        public LoginRequest(String u, String p) { this.username = u; this.password = p; }
    }

    class LoginResponse {
        public String token; // El token Bearer que recibiremos [cite: 290]
    }

    // Para la información de la unidad (Drive)
    class V1DriveInfo {
        public String location;    // Ejemplo: "drive_M1R1" [cite: 1346]
        public String barcode;     // El código de la cinta cargada [cite: 1358]
        public int elementAddress; // Identificador físico (Drive 1 suele ser 1) [cite: 1360]
        public String state;       // loaded, unloaded, reading, etc. [cite: 1350]
    }
}
