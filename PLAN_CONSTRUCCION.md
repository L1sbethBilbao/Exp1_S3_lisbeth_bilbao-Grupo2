# Plan de construccion: Microservicio empresa-transportista-efs (Semana 3)

> **Estado:** Implementacion base completada. Pendiente: configurar AWS, secrets de GitHub, pruebas en EC2 y grabacion del video.

Documentos relacionados:
- [AWS_SETUP.md](AWS_SETUP.md) — Infraestructura cloud paso a paso
- [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) — Pruebas y guion del video

---

## Contexto

Microservicio Spring Boot que gestiona guias PDF usando **EFS** (temporal) + **S3** (persistente), desplegado en **EC2** via **Docker** y **GitHub Actions**.

Referencia del profesor: `ms-administracion-archivos` — misma arquitectura (Controller → EfsService + S3Service), adaptada al dominio transportista con keys `{fecha}/{transportista}/{guia}.pdf`.

Repositorio Git: `https://github.com/L1sbethBilbao/Exp1_S3_lisbeth_bilbao-Grupo2.git`

---

## Paso 0 — Spring Initializr (referencia)

| Campo | Valor correcto |
|-------|----------------|
| Project | Maven |
| Language | Java |
| Spring Boot | **3.3.13** (no 4.0.6) |
| Group | com.duoc |
| Artifact | empresa-transportista-efs |
| Package | com.duoc.empresa_transportista_efs |
| Packaging | Jar |
| Configuration | Properties |
| Java | 21 |

**Dependencias Initializr:** Spring Web, Lombok.

**Agregar manualmente en pom.xml:** `spring-cloud-aws-starter-s3` + BOM 3.3.1.

**No usar:** JPA, Security, Actuator.

---

## Paso 1 — Estructura implementada

```
src/main/java/com/duoc/empresa_transportista_efs/
├── controller/GuiaDespachoController.java
├── service/
│   ├── GuiaDespachoService.java
│   ├── EfsService.java
│   └── S3Service.java
├── dto/
│   ├── GuiaConsultaResponse.java
│   ├── GuiaCreadaResponse.java
│   ├── GuiaMetadataDto.java
│   └── ErrorResponse.java
└── exception/
    ├── GlobalExceptionHandler.java
    └── S3*Exception.java, InvalidFileException.java
```

---

## Paso 2 — Configuracion

Archivo: `src/main/resources/application.properties`

- `server.port=8080`
- `efs.path=/app/efs`
- `aws.s3.bucket=${AWS_S3_BUCKET:tu-bucket-guias}`
- Logs AWS en DEBUG

Perfil local: no aplica — las pruebas se realizan directamente en EC2 con Postman.

---

## Paso 3 — Regla de negocio: key S3/EFS

```text
{fecha}/{transportista}/{nombreGuia}.pdf
```

Ejemplo: `20250604/TransportesSur/guia001.pdf`

Flujo POST: EFS primero → S3 despues.

---

## Paso 4 — API REST

| Metodo | Ruta | Accion |
|--------|------|--------|
| POST | `/api/guias` | Crear guia (EFS + S3) |
| GET | `/api/guias/download` | Descargar PDF |
| PUT | `/api/guias` | Actualizar guia |
| DELETE | `/api/guias` | Eliminar guia |
| GET | `/api/guias` | Consultar por fecha y transportista |

Sin Spring Security esta semana.

---

## Paso 5 — Servicios

- **EfsService:** escribe/elimina en `efs.path`
- **S3Service:** upload, download, delete, listByPrefix
- **GuiaDespachoService:** orquesta logica de negocio y buildKey

---

## Paso 6 — Dockerfile

Multi-stage con Java 21. JAR: `empresa-transportista-efs-1.0.0.jar`. Carpeta `/app/efs`.

```bash
docker run -d --name empresa-transportista-efs \
  -p 8080:8080 \
  -v /mnt/efs:/app/efs \
  -e AWS_S3_BUCKET=tu-bucket \
  TU_USUARIO/empresa-transportista-efs:latest
```

---

## Paso 7 — Infraestructura AWS

Ver [AWS_SETUP.md](AWS_SETUP.md).

Cadena EFS para el video:

```
Microservicio → /app/efs → EC2 /mnt/efs → Amazon EFS
```

---

## Paso 8 — CI/CD

Workflow: `.github/workflows/deploy.yml`

Push a `main` → Maven build → Docker Hub → SSH deploy EC2.

Secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `AWS_REGION`, `AWS_S3_BUCKET`.

---

## Paso 9 — Pruebas y video

Ver [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) para checklist completo por criterio de pauta.

---

## Orden de implementacion (estado)

| # | Tarea | Estado |
|---|-------|--------|
| 1 | Corregir pom.xml | Completado |
| 2 | application.properties | Completado |
| 3 | EfsService + S3Service | Completado |
| 4 | GuiaDespachoService | Completado |
| 5 | GuiaDespachoController | Completado |
| 6 | Excepciones globales | Completado |
| 7 | Dockerfile | Completado |
| 8 | GitHub Actions workflow | Completado |
| 9 | AWS (S3, EFS, EC2, IAM) | Pendiente — manual |
| 10 | Pruebas Postman en EC2 | Pendiente |
| 11 | Video demostracion | Pendiente |

---

## Que NO implementar

- Base de datos
- Spring Security / permisos
- Microservicios adicionales
- Frontend
