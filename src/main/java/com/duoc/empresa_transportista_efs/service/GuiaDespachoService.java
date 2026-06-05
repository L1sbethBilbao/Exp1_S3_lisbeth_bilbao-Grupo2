package com.duoc.empresa_transportista_efs.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
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
	private final AwsS3Service awsS3Service;

	@Value("${aws.s3.bucket}")
	private String bucket;

	/**
	 * Crea una guia de despacho y la almacena en EFS y S3.
	 *
	 * @param fecha          Fecha de la guia (ej. 20211)
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @param keyParam       Clave completa opcional (modo profesor)
	 * @param file           Archivo PDF a subir
	 * @return Respuesta con los datos de la guia creada
	 * @throws IOException si ocurre un error al guardar en EFS
	 */
	public GuiaCreadaResponse crearGuia(String fecha, String transportista, String nombreGuia, String keyParam,
			MultipartFile file) throws IOException {

		String key = resolveKey(keyParam, fecha, transportista, nombreGuia);
		log.info("Creando guia con key: {}", key);

		efsService.saveToEfs(key, file);
		awsS3Service.upload(bucket, key, file);

		return GuiaCreadaResponse.builder()
				.key(key)
				.fecha(extraerFecha(key, fecha))
				.transportista(extraerTransportista(key, transportista))
				.nombreGuia(obtenerNombreArchivo(key))
				.mensaje("Guia creada y almacenada en EFS y S3")
				.build();
	}

	/**
	 * Descarga una guia de despacho desde S3.
	 *
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @param keyParam       Clave completa opcional (modo profesor)
	 * @return Contenido del archivo en bytes
	 */
	public byte[] descargarGuia(String fecha, String transportista, String nombreGuia, String keyParam) {
		String key = resolveKey(keyParam, fecha, transportista, nombreGuia);
		return awsS3Service.downloadAsBytes(bucket, key);
	}

	/**
	 * Actualiza una guia de despacho existente en EFS y S3.
	 *
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @param keyParam       Clave completa opcional (modo profesor)
	 * @param file           Nuevo archivo PDF
	 * @return Respuesta con los datos de la guia actualizada
	 * @throws IOException si ocurre un error al guardar en EFS
	 */
	public GuiaCreadaResponse actualizarGuia(String fecha, String transportista, String nombreGuia, String keyParam,
			MultipartFile file) throws IOException {

		String key = resolveKey(keyParam, fecha, transportista, nombreGuia);
		log.info("Actualizando guia con key: {}", key);

		efsService.saveToEfs(key, file);
		awsS3Service.upload(bucket, key, file);

		return GuiaCreadaResponse.builder()
				.key(key)
				.fecha(extraerFecha(key, fecha))
				.transportista(extraerTransportista(key, transportista))
				.nombreGuia(obtenerNombreArchivo(key))
				.mensaje("Guia actualizada en EFS y S3")
				.build();
	}

	/**
	 * Elimina una guia de despacho de S3.
	 *
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @param keyParam       Clave completa opcional (modo profesor)
	 */
	public void eliminarGuia(String fecha, String transportista, String nombreGuia, String keyParam) {
		String key = resolveKey(keyParam, fecha, transportista, nombreGuia);
		awsS3Service.deleteObject(bucket, key);
	}

	/**
	 * Consulta guias de despacho por fecha y transportista.
	 *
	 * @param fecha          Fecha de busqueda (obligatoria)
	 * @param transportista  Nombre del transportista (opcional)
	 * @return Lista de guias con sus metadatos
	 */
	public GuiaConsultaResponse consultarGuias(String fecha, String transportista) {
		validarFecha(fecha);

		String prefix = buildListPrefix(fecha, transportista);
		List<GuiaMetadataDto> guias = awsS3Service.listObjects(bucket).stream()
				.filter(obj -> obj.getKey().startsWith(prefix))
				.map(obj -> new GuiaMetadataDto(obj.getKey(), obj.getSize(), obj.getLastModified()))
				.collect(Collectors.toList());

		return GuiaConsultaResponse.builder()
				.total(guias.size())
				.fecha(fecha.trim())
				.transportista(transportista != null ? transportista.trim() : "")
				.guias(guias)
				.build();
	}

	/**
	 * Resuelve la clave S3/EFS de una guia.
	 * Modo profesor: key = pdfs/testEFS1.pdf
	 * Modo actividad: fecha/transportista/nombreGuia.pdf
	 *
	 * @param keyParam       Clave completa opcional
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @return Clave normalizada del objeto
	 */
	public String resolveKey(String keyParam, String fecha, String transportista, String nombreGuia) {
		if (keyParam != null && !keyParam.isBlank()) {
			return normalizarKey(keyParam);
		}
		validarParametros(fecha, transportista, nombreGuia);
		return buildKey(fecha, transportista, nombreGuia);
	}

	/**
	 * Construye la clave S3/EFS con el formato fecha/transportista/nombreGuia.pdf.
	 *
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @return Clave construida
	 */
	public String buildKey(String fecha, String transportista, String nombreGuia) {
		return fecha.trim() + "/" + transportista.trim() + "/" + normalizarNombreGuia(nombreGuia);
	}

	/**
	 * Obtiene el nombre del archivo a partir de la clave S3/EFS.
	 *
	 * @param key Clave del objeto
	 * @return Nombre del archivo (ultimo segmento de la ruta)
	 */
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
