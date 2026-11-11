package ru.promo.finance.infra;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import ru.promo.finance.core.ApplicationState;
import ru.promo.finance.core.StateRepository;

/** One atomic file contains every wallet, including both sides of transfers. */
public final class JsonStateRepository implements StateRepository {
  private final Path file;
  private final JsonFiles json = new JsonFiles();

  public JsonStateRepository(Path file) {
    this.file = file.toAbsolutePath().normalize();
  }

  @Override
  public ApplicationState load() throws IOException {
    return Files.notExists(file)
        ? ApplicationState.empty()
        : json.read(file, ApplicationState.class);
  }

  @Override
  public void save(ApplicationState state) throws IOException {
    byte[] bytes = json.serialize(state).getBytes(StandardCharsets.UTF_8);
    if (bytes.length > 20 * 1024 * 1024)
      throw new IOException("Данные превышают допустимый размер 20 МиБ.");
    Files.createDirectories(file.getParent());
    Path temporary = Files.createTempFile(file.getParent(), "finance-", ".tmp");
    try {
      try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining()) channel.write(buffer);
        channel.force(true);
      }
      Files.move(
          temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
