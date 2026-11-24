package ar.edu.utn.odonto.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
class SerializacionDeFechasTest {

    @Autowired
    ObjectMapper mapper;

    @Test
    void unInstantSeSerializaComoTextoIso8601ConZ() throws Exception {
        String json = mapper.writeValueAsString(Map.of("creado", Instant.parse("2026-11-24T13:00:00Z")));

        assertThat(json).isEqualTo("{\"creado\":\"2026-11-24T13:00:00Z\"}");
    }

    @Test
    void unaFechaNoSeSerializaComoArregloNiNumero() throws Exception {
        String json = mapper.writeValueAsString(Map.of("dia", LocalDate.of(2026, 11, 24)));

        assertThat(json).isEqualTo("{\"dia\":\"2026-11-24\"}");
    }
}
