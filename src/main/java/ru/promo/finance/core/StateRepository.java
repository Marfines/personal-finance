package ru.promo.finance.core;

import java.io.IOException;

/** Port: the domain does not depend on the storage format. */
public interface StateRepository {
  ApplicationState load() throws IOException;

  void save(ApplicationState state) throws IOException;
}
