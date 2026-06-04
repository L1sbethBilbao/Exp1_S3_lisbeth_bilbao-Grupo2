package com.duoc.empresa_transportista_efs.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.duoc.empresa_transportista_efs.dto.GuiaConsultaResponse;
import com.duoc.empresa_transportista_efs.dto.GuiaCreadaResponse;
import com.duoc.empresa_transportista_efs.dto.GuiaMetadataDto;
import com.duoc.empresa_transportista_efs.exception.InvalidFileException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaDespachoService {

	private final EfsService efsService;
	private final S3Service s3Service;

	public GuiaCreadaResponse crearGuia(String fecha, String transportista, String nombreGuia, MultipartFile file)
			throws IOException {

		validarParametros(fecha, transportista, nombreGuia);

		String key = buildKey(fecha, transportista, nombreGuia);
		log.info("Creando guia con key: {}", key);

		efsService.saveToEfs(key, file);
		s3Service.upload(key, file);

		return GuiaCreadaResponse.builder()
				.key(key)
				.fecha(fecha.trim())
				.transportista(transportista.trim())
				.nombreGuia(normalizarNombreGuia(nombreGuia))
				.mensaje("Guia creada y almacenada en EFS y S3")
				.build();
	}

	public byte[] descargarGuia(String fecha, String transportista, String nombreGuia) {
		validarParametrosConsulta(fecha, transportista);
		String key = buildKey(fecha, transportista, nombreGuia);
		return s3Service.downloadAsBytes(key);
	}

	public GuiaCreadaResponse actualizarGuia(String fecha, String transportista, String nombreGuia,
			MultipartFile file) throws IOException {

		validarParametros(fecha, transportista, nombreGuia);

		String key = buildKey(fecha, transportista, nombreGuia);
		log.info("Actualizando guia con key: {}", key);

		efsService.saveToEfs(key, file);
		s3Service.upload(key, file);

		return GuiaCreadaResponse.builder()
				.key(key)
				.fecha(fecha.trim())
				.transportista(transportista.trim())
				.nombreGuia(normalizarNombreGuia(nombreGuia))
				.mensaje("Guia actualizada en EFS y S3")
				.build();
	}

	public void eliminarGuia(String fecha, String transportista, String nombreGuia) {
		validarParametrosConsulta(fecha, transportista);
		String key = buildKey(fecha, transportista, nombreGuia);

		s3Service.deleteObject(key);
		efsService.deleteFromEfs(key);
	}

	public GuiaConsultaResponse consultarGuias(String fecha, String transportista) {
		validarParametrosConsulta(fecha, transportista);

		String prefix = fecha.trim() + "/" + transportista.trim() + "/";
		List<GuiaMetadataDto> guias = s3Service.listByPrefix(prefix);

		return GuiaConsultaResponse.builder()
				.total(guias.size())
				.fecha(fecha.trim())
				.transportista(transportista.trim())
				.guias(guias)
				.build();
	}

	public String buildKey(String fecha, String transportista, String nombreGuia) {
		return fecha.trim() + "/" + transportista.trim() + "/" + normalizarNombreGuia(nombreGuia);
	}

	public String obtenerNombreArchivo(String key) {
		int lastSlash = key.lastIndexOf('/');
		return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
	}

	private String normalizarNombreGuia(String nombreGuia) {
		if (nombreGuia == null || nombreGuia.isBlank()) {
			throw new InvalidFileException("El nombre de la guia es obligatorio");
		}
		String nombre = nombreGuia.trim();
		if (!nombre.toLowerCase().endsWith(".pdf")) {
			nombre = nombre + ".pdf";
		}
		return nombre;
	}

	private void validarParametros(String fecha, String transportista, String nombreGuia) {
		validarParametrosConsulta(fecha, transportista);
		if (nombreGuia == null || nombreGuia.isBlank()) {
			throw new InvalidFileException("El nombre de la guia es obligatorio");
		}
	}

	private void validarParametrosConsulta(String fecha, String transportista) {
		if (fecha == null || fecha.isBlank()) {
			throw new InvalidFileException("La fecha es obligatoria");
		}
		if (transportista == null || transportista.isBlank()) {
			throw new InvalidFileException("El transportista es obligatorio");
		}
	}
}
