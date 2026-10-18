package cl.duoc.ms_barriodigital_requests.messaging;

/** Evento: se creo una solicitud. Se publica con routing key solicitud.created. */
public record SolicitudCreadaEvent(
        Long id,
        String tipoTramite,
        String vecinoEmail,
        String estado,
        String fecha
) {
}
