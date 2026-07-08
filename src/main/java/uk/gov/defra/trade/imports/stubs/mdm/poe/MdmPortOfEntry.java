package uk.gov.defra.trade.imports.stubs.mdm.poe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmPortOfEntry {

  private String id;
  private String code;
  private String name;
}
