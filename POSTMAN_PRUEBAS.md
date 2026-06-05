# Guia de pruebas Postman y checklist de video

Base URL: `http://<IP-ELASTICA-EC2>:8080`

Reemplaza los valores de ejemplo segun tu entorno.

**Coleccion Postman:** `postman/Empresa-Transportista-EFS.postman_collection.json`

---

## Cruce con actividad, pauta y apuntes

| Fuente | Requisito | Como se cumple en este proyecto |
|--------|-----------|----------------------------------|
| **actividad_S3.txt** | EFS temporal | `EfsService` guarda en `/app/efs/{fecha}/{transportista}/` |
| **actividad_S3.txt** | S3 por fecha/transportista | Key: `20250604/TransportesSur/guia001.pdf` |
| **actividad_S3.txt** | Crear, modificar, eliminar, consultar, descargar | 5 endpoints en `/api/guias` |
| **actividad_S3.txt** | Sin validacion permisos descarga | No hay Spring Security (profesor: proximas clases) |
| **actividad_S3.txt** | Docker Hub + GitHub Actions EC2 | `deploy.yml` |
| **pauta 1** | EFS organizado | Misma key que S3; ver `ls` en EC2 y `docker exec` |
| **pauta 2** | S3 automatico ordenado | POST sube a bucket con prefijo fecha/transportista |
| **pauta 3** | Modificar en S3 | PUT actualiza mismo objeto |
| **pauta 4** | Descargar contenido | `GET /api/guias/download` devuelve bytes del PDF |
| **pauta 5** | Consultar con filtros | `GET /api/guias?fecha=&transportista=` devuelve `total` |
| **pauta 6** | Pipeline CI/CD | Push a `main` dispara workflow |
| **pauta 7** | Video explicativo | Guion abajo + apuntes comandos EC2 |
| **apuntes** | `df -h`, `ls`, `docker exec` | Seccion 6 — carpeta `20250604/TransportesSur/` (no `pdfs/` del demo del profesor) |

**Nota:** El demo del profesor (`ms-administracion-archivos`) usa key `pdfs/testEFS1.pdf`. Tu actividad pide **fecha/transportista**, por eso en EFS veras carpetas `20250604/TransportesSur/` y no `pdfs/`.

---

## Orden para el VIDEO (apuntes + pauta)

1. **POST** — crear guia (Pauta 1 y 2)
2. **EC2** — `df -h`, `cd /home/ec2-user/efs`, `ls`, `docker exec` (apuntes 1-7)
3. **Consola S3** — mostrar objeto creado (apunte 11)
4. **PUT** — modificar guia (Pauta 3, apunte 12)
5. **Consola S3** — mostrar archivo modificado
6. **GET download** — descargar PDF y abrirlo (Pauta 4, apunte 13)
7. **GET consulta** — mostrar `total` con filtros (Pauta 5, apunte 14)
8. **DELETE** — eliminar guia (apunte 11 paso 5)
9. **Consola S3** — verificar que ya no existe
10. **Git push** — pipeline en vivo (Pauta 6, apunte 15)
11. **Explicar con detalle** (Pauta 7, apunte 16)

---

## 1. Crear guia (POST) — Pauta 1 y 2

```
POST http://<IP>:8080/api/guias
Content-Type: multipart/form-data
```

| Campo | Tipo | Valor ejemplo |
|-------|------|---------------|
| file | File | guia001.pdf |
| fecha | Text | 20250604 |
| transportista | Text | TransportesSur |
| nombreGuia | Text | guia001 |

**Respuesta esperada:** `201 Created`

```json
{
  "key": "20250604/TransportesSur/guia001.pdf",
  "fecha": "20250604",
  "transportista": "TransportesSur",
  "nombreGuia": "guia001.pdf",
  "mensaje": "Guia creada y almacenada en EFS y S3"
}
```

**Verificar en EC2:**

```bash
ls /home/ec2-user/efs/20250604/TransportesSur/
# Debe aparecer guia001.pdf
```

**Verificar en consola AWS S3:** objeto con key `20250604/TransportesSur/guia001.pdf`

---

## 2. Consultar guias (GET) — Pauta 5

```
GET http://<IP>:8080/api/guias?fecha=20250604&transportista=TransportesSur
```

**Respuesta esperada:**

```json
{
  "total": 1,
  "fecha": "20250604",
  "transportista": "TransportesSur",
  "guias": [
    {
      "key": "20250604/TransportesSur/guia001.pdf",
      "size": 12345,
      "lastModified": "..."
    }
  ]
}
```

Importante: esto **cuenta y lista** archivos. No confundir con descargar.

---

## 3. Descargar guia (GET) — Pauta 4

```
GET http://<IP>:8080/api/guias/download?fecha=20250604&transportista=TransportesSur&nombreGuia=guia001
```

**Respuesta esperada:** archivo PDF binario (Save Response → guardar y abrir el PDF).

No basta con listar; debes **obtener el contenido** del archivo.

---

## 4. Modificar guia (PUT) — Pauta 3

```
PUT http://<IP>:8080/api/guias
Content-Type: multipart/form-data
```

Mismos campos que POST, pero con un PDF **diferente** (contenido modificado).

**Verificar:** en consola S3, el archivo mantiene la misma key pero cambia tamano/fecha de modificacion.

---

## 5. Eliminar guia (DELETE) — Pauta 2

```
DELETE http://<IP>:8080/api/guias?fecha=20250604&transportista=TransportesSur&nombreGuia=guia001
```

**Respuesta esperada:** `204 No Content`

**Verificar:** el objeto ya no aparece en la consola S3.

---

## 6. Demostracion EFS en video — Pauta 1 maximo (apuntes clase)

En EC2, ejecutar en este orden (equivalente a apuntes `cd pdfs/` pero con tu key `fecha/transportista`):

```bash
df -h
ls
cd /home/ec2-user/efs
ls
cd 20250604/TransportesSur/
ls

sudo docker exec -it empresa-transportista-efs bash
df -h
ls /app/efs/20250604/TransportesSur/
exit
```

Explicar la cadena (apuntes): micro escribe en `/app/efs` → Docker mapea a `/home/ec2-user/efs` en Linux → Linux escribe en Amazon EFS.

---

## 7. Pipeline CI/CD en vivo — Pauta 6

1. Hacer un cambio menor (ej. comentario en README)
2. `git add . && git commit -m "trigger deploy" && git push origin main`
3. Mostrar GitHub Actions ejecutandose
4. Verificar que la app responde en EC2 despues del deploy

---

## 8. Guion del video — Pauta 7

Orden sugerido:

1. Presentar el caso (empresa transportista, guias de despacho)
2. Mostrar arquitectura: EC2 + Docker + EFS + S3 + GitHub Actions
3. Crear guia con Postman
4. Mostrar EFS en EC2 y dentro del contenedor
5. Mostrar objeto en S3
6. Consultar historial con filtros
7. Descargar PDF y abrirlo
8. Modificar guia y ver cambio en S3
9. Eliminar guia y verificar en S3
10. Push a main y pipeline en vivo
11. Explicar **por que** funciona cada parte (no solo mostrar pantallas)

---

## Pruebas solo en EC2 (sin entorno local)

Todas las pruebas se hacen contra la instancia desplegada:

```
http://<IP-ELASTICA-EC2>:8080/api/guias
```

Orden recomendado:

1. Seguir [AWS_SETUP.md](AWS_SETUP.md) (S3, EFS, EC2, IAM, Docker)
2. Desplegar el contenedor con volumen EFS montado
3. Probar cada endpoint con Postman usando la IP elástica
4. Verificar EFS en la consola SSH (`ls /home/ec2-user/efs`, `docker exec`, `df -h`)
5. Verificar objetos en la consola AWS S3
6. Grabar el video con ese flujo en vivo
