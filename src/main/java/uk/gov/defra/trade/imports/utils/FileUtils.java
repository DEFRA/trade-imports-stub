package uk.gov.defra.trade.imports.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class FileUtils {

  private final ObjectMapper objectMapper;

  public <T> T getObjectFromFile(String fileName, Class<T> clazz) {
    try {
      var inputStream = getClass().getClassLoader()
          .getResourceAsStream(fileName);

      if (inputStream == null) {
        throw new IllegalArgumentException("File not found: " + fileName);
      }

      return objectMapper.readValue(inputStream, clazz);
    } catch (Exception e) {
      log.error("Failed to read from JSON file: {}", fileName, e);
      throw new RuntimeException("Failed to read from json file: " + fileName, e);
    }
  }
}
