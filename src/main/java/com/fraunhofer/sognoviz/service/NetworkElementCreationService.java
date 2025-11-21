package com.fraunhofer.sognoviz.service;

import com.powsybl.iidm.modification.topology.CreateFeederBayBuilder;
import com.powsybl.iidm.modification.topology.CreateBranchFeederBaysBuilder;
import com.powsybl.iidm.network.*;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.svg.metadata.DiagramMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class NetworkElementCreationService {

    /**
     * Create a new load using CreateFeederBayBuilder
     */
    public Path createLoad(String inputPath, String loadId, String busOrBusbarId,
                           double p0, double q0, String voltageLevelId, String diagramId)
            throws IOException {

        Path storageDir = Paths.get("./cgmes/");
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        DiagramMetadata metadata = DiagramMetadata.parseJson(Path.of(inputPath));
        NadParameters nadParameters = new NadParameters()
                .setLayoutParameters(metadata.getLayoutParameters())
                .setSvgParameters(metadata.getSvgParameters());

        Network network = loadNetwork(storageDir.toString() + "/" + diagramId + ".zip");

        Path outputDir = storageDir.resolve(diagramId + "_modified");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Get voltage level
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        if (voltageLevel == null) {
            throw new IOException("Voltage level not found: " + voltageLevelId);
        }

        // Create LoadAdder with attributes
        LoadAdder loadAdder = voltageLevel.newLoad()
                .setId(loadId)
                .setName(loadId)
                .setP0(p0)
                .setQ0(q0)
                .setLoadType(LoadType.UNDEFINED);

        // Use the Builder pattern - this is the correct way!
        var modification = new CreateFeederBayBuilder()
                .withInjectionAdder(loadAdder)
                .withBusOrBusbarSectionId(busOrBusbarId)
                .build();

        // Apply modification with throwException = true
        modification.apply(network, true);

        // Redraw diagram
        NetworkAreaDiagram.draw(network, outputDir, nadParameters, VoltageLevelFilter.NO_FILTER);

        return outputDir;
    }

    /**
     * Create a new generator using CreateFeederBayBuilder
     */
    public Path createGenerator(String inputPath, String generatorId, String busOrBusbarId,
                                double targetP, double targetV, double minP, double maxP,
                                String voltageLevelId, String diagramId) throws IOException {

        Path storageDir = Paths.get("./cgmes/");
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        DiagramMetadata metadata = DiagramMetadata.parseJson(Path.of(inputPath));
        NadParameters nadParameters = new NadParameters()
                .setLayoutParameters(metadata.getLayoutParameters())
                .setSvgParameters(metadata.getSvgParameters());

        Network network = loadNetwork(storageDir.toString() + "/" + diagramId + ".zip");

        Path outputDir = storageDir.resolve(diagramId + "_modified");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Get voltage level
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        if (voltageLevel == null) {
            throw new IOException("Voltage level not found: " + voltageLevelId);
        }

        // Create GeneratorAdder
        GeneratorAdder generatorAdder = voltageLevel.newGenerator()
                .setId(generatorId)
                .setName(generatorId)
                .setTargetP(targetP)
                .setTargetV(targetV)
                .setMinP(minP)
                .setMaxP(maxP)
                .setVoltageRegulatorOn(true)
                .setEnergySource(EnergySource.OTHER);

        // Use the Builder pattern
        var modification = new CreateFeederBayBuilder()
                .withInjectionAdder(generatorAdder)
                .withBusOrBusbarSectionId(busOrBusbarId)
                .build();

        // Apply modification
        modification.apply(network, true);

        // Redraw diagram
        NetworkAreaDiagram.draw(network, outputDir, nadParameters, VoltageLevelFilter.NO_FILTER);

        return outputDir;
    }

    /**
     * Create a new line using CreateBranchFeederBaysBuilder
     */
    public Path createLine(String inputPath, String lineId,
                           String bus1Id, String bus2Id,
                           String voltageLevel1Id, String voltageLevel2Id,
                           double r, double x, double g1, double b1, double g2, double b2,
                           String diagramId) throws IOException {

        Path storageDir = Paths.get("./cgmes/");
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        DiagramMetadata metadata = DiagramMetadata.parseJson(Path.of(inputPath));
        NadParameters nadParameters = new NadParameters()
                .setLayoutParameters(metadata.getLayoutParameters())
                .setSvgParameters(metadata.getSvgParameters());

        Network network = loadNetwork(storageDir.toString() + "/" + diagramId + ".zip");

        Path outputDir = storageDir.resolve(diagramId + "_modified");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Verify voltage levels exist
        VoltageLevel vl1 = network.getVoltageLevel(voltageLevel1Id);
        VoltageLevel vl2 = network.getVoltageLevel(voltageLevel2Id);

        if (vl1 == null || vl2 == null) {
            throw new IOException("One or both voltage levels not found");
        }

        // Create LineAdder
        LineAdder lineAdder = network.newLine()
                .setId(lineId)
                .setName(lineId)
                .setVoltageLevel1(voltageLevel1Id)
                .setVoltageLevel2(voltageLevel2Id)
                .setR(r)
                .setX(x)
                .setG1(g1)
                .setB1(b1)
                .setG2(g2)
                .setB2(b2);

        // Use the Builder pattern
        var modification = new CreateBranchFeederBaysBuilder()
                .withBranchAdder(lineAdder)
                .withBusOrBusbarSectionId1(bus1Id)
                .withBusOrBusbarSectionId2(bus2Id)
                .build();

        // Apply modification
        modification.apply(network, true);

        // Redraw diagram
        NetworkAreaDiagram.draw(network, outputDir, nadParameters, VoltageLevelFilter.NO_FILTER);

        return outputDir;
    }

    /**
     * IMPORTANT NOTE: Substations and Voltage Levels CANNOT be created via NetworkModification!
     *
     * They must be created directly using the Network builder API.
     * Below are examples of how to do this.
     */

    /**
     * Create a Substation - Direct builder usage (NOT a NetworkModification)
     */
    public Path createSubstation(String inputPath, String substationId, String substationName,
                                 String country, String diagramId) throws IOException {

        Path storageDir = Paths.get("./cgmes/");
        Network network = loadNetwork(storageDir.toString() + "/" + diagramId + ".zip");

        // Direct builder usage - this is the ONLY way to create substations
        Substation substation = network.newSubstation()
                .setId(substationId)
                .setName(substationName)
                .setCountry(Country.valueOf(country))
                .add();

        Path outputDir = storageDir.resolve(diagramId + "_modified");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        DiagramMetadata metadata = DiagramMetadata.parseJson(Path.of(inputPath));
        NadParameters nadParameters = new NadParameters()
                .setLayoutParameters(metadata.getLayoutParameters())
                .setSvgParameters(metadata.getSvgParameters());

        NetworkAreaDiagram.draw(network, outputDir, nadParameters, VoltageLevelFilter.NO_FILTER);
        return outputDir;
    }

    /**
     * Create a Voltage Level - Direct builder usage (NOT a NetworkModification)
     */
    public Path createVoltageLevel(String inputPath, String substationId,
                                   String voltageLevelId, String voltageLevelName,
                                   double nominalV, TopologyKind topologyKind,
                                   String diagramId) throws IOException {

        Path storageDir = Paths.get("./cgmes/");
        Network network = loadNetwork(storageDir.toString() + "/" + diagramId + ".zip");

        Substation substation = network.getSubstation(substationId);
        if (substation == null) {
            throw new IOException("Substation not found: " + substationId);
        }

        // Direct builder usage - this is the ONLY way to create voltage levels
        VoltageLevel voltageLevel = substation.newVoltageLevel()
                .setId(voltageLevelId)
                .setName(voltageLevelName)
                .setNominalV(nominalV)
                .setTopologyKind(topologyKind)  // BUS_BREAKER or NODE_BREAKER
                .add();

        Path outputDir = storageDir.resolve(diagramId + "_modified");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        DiagramMetadata metadata = DiagramMetadata.parseJson(Path.of(inputPath));
        NadParameters nadParameters = new NadParameters()
                .setLayoutParameters(metadata.getLayoutParameters())
                .setSvgParameters(metadata.getSvgParameters());

        NetworkAreaDiagram.draw(network, outputDir, nadParameters, VoltageLevelFilter.NO_FILTER);
        return outputDir;
    }

    private Network loadNetwork(String filePath) throws IOException {
        throw new UnsupportedOperationException("Implement network loading logic");
    }
}