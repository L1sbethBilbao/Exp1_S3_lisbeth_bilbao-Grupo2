# Guia de pruebas Postman y checklist de video

Base URL: `http://<IP-ELASTICA-EC2>:8080`

Reemplaza los valores de ejemplo segun tu entorno.

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

## 6. Demostracion EFS en video — Pauta 1 maximo

En EC2, ejecutar en este orden:

```bash
df -h
cd /home/ec2-user/efs
ls
cd 20250604/TransportesSur/
ls

sudo docker exec -it empresa-transportista-efs bash
df -h
ls /app/efs/20250604/TransportesSur/
exit
```

Explicar: el micro escribe en `/app/efs`, Docker mapea a `/home/ec2-user/efs` en Linux, y eso esta montado en Amazon EFS.

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
