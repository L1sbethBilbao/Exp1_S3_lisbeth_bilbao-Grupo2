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

	@DeleteMapping
	public ResponseEntity<Void> eliminarGuia(
			@RequestParam(required = false) String key,
			@RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista,
			@RequestParam(required = false) String nombreGuia) {

		guiaDespachoService.eliminarGuia(fecha, transportista, nombreGuia, key);
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<GuiaConsultaResponse> consultarGuias(
			@RequestParam String fecha,
			@RequestParam(required = false) String transportista) {

		GuiaConsultaResponse response = guiaDespachoService.consultarGuias(fecha, transportista);
		return ResponseEntity.ok(response);
	}
}
