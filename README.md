# LTOTRACE

Sistema web de gestión automatizada y trazabilidad de medios de almacenamiento LTO en plataformas IBM i.
Desarrollado como trabajo de tesis de Ingeniería Informática (UNIDA, Asunción, 2026).

LTOTRACE vincula el código de barras físico de cada cartucho, leído por la librería IBM TS4300, con el
Volume ID lógico que usa IBM i, y registra la ubicación, los movimientos, el desgaste, los retiros
(con acta en PDF), la auditoría de eventos y la conciliación del inventario.

## Tecnologías

- Java 17 y Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Thymeleaf)
- PostgreSQL 16
- JT400 (IBM Toolbox for Java) para la integración con IBM i
- API REST de la librería IBM TS4300 (Apache HttpClient 5)
- JUnit 5, Mockito y MockMvc para las pruebas automatizadas

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `src/main/java` | Código de la aplicación (controladores, servicios, acceso a datos e integraciones) |
| `src/main/resources` | Configuración (`application.properties`), plantillas Thymeleaf y recursos estáticos |
| `src/test/java` | Pruebas automatizadas |
| `sql/` | Script de creación de la base de datos, permisos del usuario de la aplicación y migraciones |
| `scripts/` | Script de respaldo diario de la base de datos |

## Configuración

El repositorio no contiene credenciales. Antes de ejecutar la aplicación se definen estas variables de entorno:

| Variable | Obligatoria | Descripción |
|---|---|---|
| `SPRING_DATASOURCE_PASSWORD` | Sí | Contraseña del usuario de la base de datos |
| `LTO_CRYPTO_PASSWORD` | Sí | Clave con la que se cifran las credenciales de la TS4300 y del IBM i |
| `LTO_KEYSTORE_PASSWORD` | Sí | Contraseña del almacén del certificado HTTPS |
| `SPRING_DATASOURCE_USERNAME` | No | Usuario de la base de datos (por defecto `ltotrace_app`) |
| `SPRING_DATASOURCE_URL` | No | Dirección JDBC (por defecto `jdbc:postgresql://localhost:5432/atopa`) |
| `LTO_KEYSTORE_PATH` | No | Ubicación del certificado (por defecto `file:/home/ltotrace/ltotrace.p12`) |

En el servidor se definen en `/etc/ltotrace/ltotrace.env`, con permisos 600, y el servicio de systemd las carga.

## Compilación y pruebas

```bash
mvn clean package
```

El empaquetado ejecuta las pruebas automatizadas. El procedimiento completo de instalación, despliegue,
seguridad y respaldo se describe en el Manual Técnico de la tesis.
