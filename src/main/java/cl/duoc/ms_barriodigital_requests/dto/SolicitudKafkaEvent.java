package cl.duoc.ms_barriodigital_requests.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudKafkaEvent {

    private Long id;
    private String tipo;        // CREADA, ACTUALIZADA, CERRADA
    private String comunaId;
    private String titulo;
    private String descripcion;
    private String estado;
    private String timestamp;
}