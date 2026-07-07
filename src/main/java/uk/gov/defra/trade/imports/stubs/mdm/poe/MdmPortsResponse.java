package uk.gov.defra.trade.imports.stubs.mdm.poe;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmPortsResponse {

  private List<MdmPortOfEntry> result;
}
