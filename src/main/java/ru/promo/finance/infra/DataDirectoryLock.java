package ru.promo.finance.infra;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Prevents a second CLI process from overwriting a stale snapshot. */
public final class DataDirectoryLock implements AutoCloseable {
  private final FileChannel channel;
  private final FileLock lock;

  public DataDirectoryLock(Path directory) throws IOException {
    Files.createDirectories(directory);
    channel =
        FileChannel.open(
            directory.resolve("finance.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    try {
      lock = channel.tryLock();
      if (lock == null) throw new IOException("Каталог данных уже открыт другим процессом.");
    } catch (IOException | OverlappingFileLockException e) {
      channel.close();
      throw new IOException(
          "Не удалось заблокировать каталог данных: возможно, приложение уже запущено.", e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      lock.release();
    } finally {
      channel.close();
    }
  }
}
