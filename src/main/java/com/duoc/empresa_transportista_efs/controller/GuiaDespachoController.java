package com.duoc.empresa_transportista_efs.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.duoc.empresa_transportista_efs.dto.GuiaConsultaResponse;
import com.duoc.empresa_transportista_efs.dto.GuiaCreadaResponse;
import com.duoc.empresa_transportista_efs.service.GuiaDespachoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
public class GuiaDespachoController {

	private final GuiaDespachoService guiaDespachoService;

	/**
	 * Crea una guia de despacho y la almacena en EFS y S3.
	 *
	 * @param file           Archivo PDF a subir
	 * @param key            Clave completa opcional (modo profesor)
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @return Respuesta con los datos de la guia creada
	 */
	@PostMapping
	public ResponseEntity<GuiaCreadaResponse> crearGuia(
			@RequestParam("file") MultipartFile file,
			@RequestParam(required = false) String key,
			@RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista,
			@RequestParam(required = false) String nombreGuia) throws IOException {

		GuiaCreadaResponse response = guiaDespachoService.crearGuia(fecha, transportista, nombreGuia, key, file);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Descarga una guia de despacho desde S3.
	 *
	 * @param key            Clave completa opcional (modo profesor)
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @return Archivo PDF descargado como bytes
	 */
	@GetMapping("/download")
	public ResponseEntity<byte[]> descargarGuia(
			@RequestParam(required = false) String key,
			@RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista,
			@RequestParam(required = false) String nombreGuia) {

		String resolvedKey = guiaDespachoService.resolveKey(key, fecha, transportista, nombreGuia);
		byte[] fileBytes = guiaDespachoService.descargarGuia(fecha, transportista, nombreGuia, key);
		String filename = guiaDespachoService.obtenerNombreArchivo(resolvedKey);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.APPLICATION_PDF)
				.body(fileBytes);
	}

	/**
	 * Actualiza una guia de despacho existente en EFS y S3.
	 *
	 * @param file           Nuevo archivo PDF
	 * @param key            Clave completa opcional (modo profesor)
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @return Respuesta con los datos de la guia actualizada
	 */
	@PutMapping
	public ResponseEntity<GuiaCreadaResponse> actualizarGuia(
			@RequestParam("file") MultipartFile file,
			@RequestParam(required = false) String key,
			@RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista,
			@RequestParam(required = false) String nombreGuia) throws IOException {

		GuiaCreadaResponse response = guiaDespachoService.actualizarGuia(fecha, transportista, nombreGuia, key, file);
		return ResponseEntity.ok(response);
	}

	/**
	 * Elimina una guia de despacho de S3.
	 *
	 * @param key            Clave completa opcional (modo profesor)
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @return Respuesta sin contenido
	 */
	@DeleteMapping
	public ResponseEntity<Void> eliminarGuia(
			@RequestParam(required = false) String key,
			@RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista,
			@RequestParam(required = false) String nombreGuia) {

		guiaDespachoService.eliminarGuia(fecha, transportista, nombreGuia, key);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Consulta guias de despacho por fecha y transportista.
	 *
	 * @param fecha          Fecha de busqueda (obligatoria)
	 * @param transportista  Nombre del transportista (opcional)
	 * @return Lista de guias con sus metadatos
	 */
	@GetMapping
	public ResponseEntity<GuiaConsultaResponse> consultarGuias(
			@RequestParam String fecha,
			@RequestParam(required = false) String transportista) {

		GuiaConsultaResponse response = guiaDespachoService.consultarGuias(fecha, transportista);
		return ResponseEntity.ok(response);
	}
}
