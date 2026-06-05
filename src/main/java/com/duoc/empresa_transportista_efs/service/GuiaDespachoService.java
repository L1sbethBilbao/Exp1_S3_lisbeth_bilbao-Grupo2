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

	public GuiaCreadaResponse crearGuia(String fecha, String transportista, String nombreGuia, String keyParam,
			MultipartFile file) throws IOException {

		String key = resolveKey(keyParam, fecha, transportista, nombreGuia);
		log.info("Creando guia con key: {}", key);

		efsService.saveToEfs(key, file);
		s3Service.upload(key, file);

		return GuiaCreadaResponse.builder()
				.key(key)
				.fecha(extraerFecha(key, fecha))
				.transportista(extraerTransportista(key, transportista))
				.nombreGuia(obtenerNombreArchivo(key))
				.mensaje("Guia creada y almacenada en EFS y S3")
				.build();
	}

	public byte[] descargarGuia(String fecha, String transportista, String nombreGuia, String keyParam) {
		String key = resolveKey(keyParam, fecha, transportista, nombreGuia);
		return s3Service.downloadAsBytes(key);
	}

	public GuiaCreadaResponse actualizarGuia(String fecha, String transportista, String nombreGuia, String keyParam,
			MultipartFile file) throws IOException {

		String key = resolveKey(keyParam, fecha, transportista, nombreGuia);
		log.info("Actualizando guia con key: {}", key);

		efsService.saveToEfs(key, file);
		s3Service.upload(key, file);

		return GuiaCreadaResponse.builder()
				.key(key)
				.fecha(extraerFecha(key, fecha))
				.transportista(extraerTransportista(key, transportista))
				.nombreGuia(obtenerNombreArchivo(key))
				.mensaje("Guia actualizada en EFS y S3")
				.build();
	}

	public void eliminarGuia(String fecha, String transportista, String nombreGuia, String keyParam) {
		String key = resolveKey(keyParam, fecha, transportista, nombreGuia);

		s3Service.deleteObject(key);
		efsService.deleteFromEfs(key);
	}

	public GuiaConsultaResponse consultarGuias(String fecha, String transportista) {
		validarFecha(fecha);

		String prefix = buildListPrefix(fecha, transportista);
		List<GuiaMetadataDto> guias = s3Service.listByPrefix(prefix);

		return GuiaConsultaResponse.builder()
				.total(guias.size())
				.fecha(fecha.trim())
				.transportista(transportista != null ? transportista.trim() : "")
				.guias(guias)
				.build();
	}

	/**
	 * Modo profesor (apuntes): key = pdfs/testEFS1.pdf
	 * Modo actividad: fecha/transportista/nombreGuia.pdf
	 */
	public String resolveKey(String keyParam, String fecha, String transportista, String nombreGuia) {
		if (keyParam != null && !keyParam.isBlank()) {
			return normalizarKey(keyParam);
		}
		validarParametros(fecha, transportista, nombreGuia);
		return buildKey(fecha, transportista, nombreGuia);
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

	private String normalizarKey(String key) {
		String normalized = key.trim().replace('\\', '/');
		if (!normalized.toLowerCase().endsWith(".pdf")) {
			normalized = normalized + ".pdf";
		}
		return normalized;
	}

	private String buildListPrefix(String fecha, String transportista) {
		if (transportista == null || transportista.isBlank()) {
			return fecha.trim() + "/";
		}
		return fecha.trim() + "/" + transportista.trim() + "/";
	}

	private String extraerFecha(String key, String fecha) {
		if (fecha != null && !fecha.isBlank()) {
			return fecha.trim();
		}
		int slash = key.indexOf('/');
		return slash >= 0 ? key.substring(0, slash) : key;
	}

	private String extraerTransportista(String key, String transportista) {
		if (transportista != null && !transportista.isBlank()) {
			return transportista.trim();
		}
		int first = key.indexOf('/');
		int last = key.lastIndexOf('/');
		if (first >= 0 && last > first) {
			return key.substring(first + 1, last);
		}
		return "";
	}

	private void validarFecha(String fecha) {
		if (fecha == null || fecha.isBlank()) {
			throw new InvalidFileException("La fecha es obligatoria");
		}
	}

	private void validarParametrosConsulta(String fecha, String transportista) {
		validarFecha(fecha);
	}

	private void validarParametros(String fecha, String transportista, String nombreGuia) {
		validarParametrosConsulta(fecha, transportista);
		if (transportista == null || transportista.isBlank()) {
			throw new InvalidFileException("El transportista es obligatorio");
		}
		if (nombreGuia == null || nombreGuia.isBlank()) {
			throw new InvalidFileException("El nombre de la guia es obligatorio");
		}
	}
}
