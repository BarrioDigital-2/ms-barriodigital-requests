package cl.duoc.ms_barriodigital_requests.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SolicitudControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void crearSolicitudDebePersistirDireccionYDescripcionParaSuLectura() throws Exception {
        String request = """
                {
                  "tipoTramite": "CERTIFICADO_RESIDENCIA",
                  "vecinoEmail": "vecino@duocuc.cl",
                  "direccion": "Av. Siempre Viva 123",
                  "descripcion": "Visita para verificar domicilio"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direccion").value("Av. Siempre Viva 123"))
                .andExpect(jsonPath("$.descripcion").value("Visita para verificar domicilio"))
                .andReturn();

        Number generatedId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
        long id = generatedId.longValue();

        mockMvc.perform(get("/api/requests/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("Av. Siempre Viva 123"))
                .andExpect(jsonPath("$.descripcion").value("Visita para verificar domicilio"));
    }

    @Test
    void crearSolicitudDebeAceptarDireccionYDescripcionOmitidas() throws Exception {
        String request = """
                {
                  "tipoTramite": "LICENCIA",
                  "vecinoEmail": "vecino@duocuc.cl"
                }
                """;

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direccion").doesNotExist())
                .andExpect(jsonPath("$.descripcion").doesNotExist());
    }

    @Test
    void crearSolicitudDebeValidarElMaximoDeCaracteresDeDireccionYDescripcion() throws Exception {
        String request = """
                {
                  "tipoTramite": "CERTIFICADO_RESIDENCIA",
                  "vecinoEmail": "vecino@duocuc.cl",
                  "direccion": "%s",
                  "descripcion": "%s"
                }
                """.formatted("a".repeat(201), "b".repeat(501));

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}