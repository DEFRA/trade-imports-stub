package uk.gov.defra.trade.imports.stubs.mdm.countries;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmCountry {

  private String name;
  private String longName;
  private String effectiveAlpha2;
  private String effectiveAlpha3;
  private String effectiveAlias;
  private String effectiveLongName;
  private Boolean independent;
  private String statusName;
  private String system;
  private String systemAlias;
  private String systemAlpha2;
  private String systemAlpha3;
  private String systemLongName;
  private List<MdmBlock> blocks;
}
