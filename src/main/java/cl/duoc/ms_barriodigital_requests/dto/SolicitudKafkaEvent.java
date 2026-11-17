package cl.duoc.ms_barriodigital_requests.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudKafkaEvent {

    private String eventId;
    private String tipo;
    private Long solicitudId;
    private String comunaId;
    private String tipoTramite;
    private String estadoAnterior;
    private String estado;
    private String usuario;
    private String origen;
    private String timestamp;
}