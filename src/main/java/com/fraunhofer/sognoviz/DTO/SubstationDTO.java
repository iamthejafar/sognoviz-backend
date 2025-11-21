package com.fraunhofer.sognoviz.DTO;

import com.powsybl.iidm.network.Country;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubstationDTO {
    private String id;
    private String name;
    private Country country;
}
