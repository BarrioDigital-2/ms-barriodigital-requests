package cl.duoc.ms_barriodigital_requests.messaging;

/** Evento: cambio el estado de una solicitud. Se publica con routing key solicitud.status. */
public record SolicitudEstadoEvent(
        Long id,
        String estadoAnterior,
        String estadoNuevo,
        String fecha
) {
}
