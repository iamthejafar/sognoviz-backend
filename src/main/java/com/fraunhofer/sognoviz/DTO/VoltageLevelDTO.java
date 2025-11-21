package com.fraunhofer.sognoviz.DTO;

import com.powsybl.iidm.network.TopologyKind;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoltageLevelDTO {
    private String id;
    private String name;
    private double nominalV;
    private TopologyKind topologyKind;

    // constructors, getters, setters
}
