# Plan de construccion: Microservicio empresa-transportista-efs (Semana 3)

> **Estado:** Implementacion base completada. Pendiente: configurar AWS, secrets de GitHub, pruebas en EC2 y grabacion del video.

Documentos relacionados:
- [AWS_SETUP.md](AWS_SETUP.md) — Infraestructura cloud paso a paso
- [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) — Pruebas y guion del video

---

## Contexto

Microservicio Spring Boot que gestiona guias PDF usando **EFS** (temporal) + **S3** (persistente), desplegado en **EC2** via **Docker** y **GitHub Actions**.

Referencia del profesor: `ms-administracion-archivos` — misma capa de infraestructura (`AwsS3Service` + `EfsService`), adaptada al dominio transportista con keys `{fecha}/{transportista}/{guia}.pdf`.

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
├── controller/AwsS3Controller.java
├── service/
│   ├── GuiaDespachoService.java   ← logica de negocio (keys, fecha/transportista)
│   ├── AwsS3Service.java          ← igual al profesor (upload, download, delete, listObjects, moveObject)
│   └── EfsService.java            ← igual al profesor (saveToEfs)
├── dto/
│   ├── GuiaConsultaResponse.java
│   ├── GuiaCreadaResponse.java
│   ├── GuiaMetadataDto.java
│   ├── S3ObjectDto.java
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

## Paso 4 — API REST (igual al profesor + extensiones actividad)

| Metodo | Ruta | Accion |
|--------|------|--------|
| GET | `/s3/{bucket}/objects` | Listar objetos (profesor) |
| GET | `/s3/{bucket}/consulta` | Consultar por fecha/transportista (actividad) |
| GET | `/s3/{bucket}/object` | Descargar PDF |
| POST | `/s3/{bucket}/object` | Crear guia (EFS + S3) |
| PUT | `/s3/{bucket}/object` | Actualizar guia (actividad) |
| POST | `/s3/{bucket}/move` | Mover objeto (profesor) |
| DELETE | `/s3/{bucket}/object` | Eliminar guia (solo S3) |

Sin Spring Security esta semana.

---

## Paso 5 — Servicios

- **EfsService:** `saveToEfs(filename, file)` — igual al profesor
- **AwsS3Service:** `listObjects`, `downloadAsBytes`, `upload`, `moveObject`, `deleteObject` — igual al profesor (bucket como parametro)
- **GuiaDespachoService:** orquesta EFS + S3, construye keys, filtra consultas por prefijo `fecha/transportista/`. El bucket se lee de `aws.s3.bucket` en `application.properties`

---

## Paso 6 — Dockerfile

Multi-stage con Java 21. JAR: `empresa-transportista-efs-1.0.0.jar`. Carpeta `/app/efs`.

```bash
docker run -d --name empresa-transportista-efs \
  -p 8080:8080 \
  -v /home/ec2-user/efs:/app/efs \
  -e EFS_PATH=/app/efs \
  -e AWS_S3_BUCKET=tu-bucket \
  -e AWS_REGION=us-east-1 \
  TU_USUARIO/empresa-transportista-efs:latest
```

---

## Paso 7 — Infraestructura AWS

Ver [AWS_SETUP.md](AWS_SETUP.md).

Cadena EFS para el video:

```
Microservicio → /app/efs → EC2 /home/ec2-user/efs → Amazon EFS
```

---

## Paso 8 — CI/CD

Workflow: `.github/workflows/deploy.yml`

Push a `main` → Maven build → Docker Hub → SSH deploy EC2.

Secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `USER_SERVER`, `EC2_SSH_KEY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`, `AWS_REGION`, `AWS_S3_BUCKET`, `EFS_MOUNT_PATH`, `EFS_PATH`.

---

## Paso 9 — Pruebas y video

Coleccion Postman: `postman/Pruebas-Semana3.postman_collection.json`

Ver [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) para checklist completo por criterio de pauta.

---

## Orden de implementacion (estado)

| # | Tarea | Estado |
|---|-------|--------|
| 1 | Corregir pom.xml | Completado |
| 2 | application.properties | Completado |
| 3 | EfsService + AwsS3Service | Completado |
| 4 | GuiaDespachoService | Completado |
| 5 | AwsS3Controller | Completado |
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
