package cl.duoc.ms_barriodigital_requests.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Slf4j
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Value("${catalog.service.url}") String catalogUrl) {
        // Timeouts cortos: si catalog no responde, no dejamos al funcionario esperando
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
                .baseUrl(catalogUrl)
                .requestFactory(factory)
                .build();
    }

    // Pide a catalog que descuente un cupo. Si no se puede, lanza una excepción y la admisión no ocurre
    public void reservarCupo(String tipoTramite) {
        try {
            restClient.post()
                    .uri("/api/v1/catalog/procedures/{tipo}/reserve", tipoTramite)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Catalog] Cupo reservado para {}", tipoTramite);
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay cupo disponible para el trámite " + tipoTramite);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El tipo de trámite " + tipoTramite + " no existe en el catálogo");
        } catch (ResourceAccessException e) {
            // Catalog apagado o sin responder a tiempo: por seguridad no se admite
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El catálogo no responde; no se puede admitir el trámite");
        }
    }
}
