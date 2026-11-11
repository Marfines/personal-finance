package ru.promo.finance.infra;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class JsonFiles {
  private static final long MAX_BYTES = 20 * 1024 * 1024;
  private final ObjectMapper mapper =
      new ObjectMapper(
              JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
          .registerModule(new JavaTimeModule())
          .enable(SerializationFeature.INDENT_OUTPUT)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);

  public <T> T read(Path path, Class<T> type) throws IOException {
    if (Files.size(path) > MAX_BYTES)
      throw new IOException("Файл превышает допустимый размер 20 МиБ.");
    try {
      T result = mapper.readValue(Files.readString(path, StandardCharsets.UTF_8), type);
      if (result == null) throw new IllegalArgumentException("Пустой снимок.");
      return result;
    } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IOException("Некорректный JSON или данные в файле: " + path.getFileName(), e);
    }
  }

  public String serialize(Object value) throws IOException {
    return mapper.writeValueAsString(value) + System.lineSeparator();
  }

  public void export(Path path, Object value) throws IOException {
    Files.writeString(
        path, serialize(value), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
  }
}
