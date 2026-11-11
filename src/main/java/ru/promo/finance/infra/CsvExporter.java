package ru.promo.finance.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import ru.promo.finance.core.Operation;
import ru.promo.finance.core.ReportService;

/** RFC 4180 quoting, with formula neutralization for spreadsheet readers. */
public final class CsvExporter {
  public void export(Path file, ReportService.Report report) throws IOException {
    StringBuilder csv =
        new StringBuilder("id,date,type,category,amount,note,counterparty,transferId\r\n");
    for (Operation op : report.operations()) {
      csv.append(cell(op.id().toString()))
          .append(',')
          .append(cell(op.date().toString()))
          .append(',')
          .append(cell(op.type().name()))
          .append(',')
          .append(cell(op.category()))
          .append(',')
          .append(op.amount().toPlainString())
          .append(',')
          .append(cell(op.note()))
          .append(',')
          .append(cell(op.counterparty()))
          .append(',')
          .append(cell(op.transferId() == null ? null : op.transferId().toString()))
          .append("\r\n");
    }
    Files.writeString(file, csv, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
  }

  private String cell(String value) {
    String text = value == null ? "" : value;
    if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) text = "'" + text;
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }
}
