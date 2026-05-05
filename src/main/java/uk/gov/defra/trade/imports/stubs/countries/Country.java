package uk.gov.defra.trade.imports.stubs.countries;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Country {

  private String code;
  private String name;
  private List<String> classifiers;
  private List<String> internalClassifiers;
}
