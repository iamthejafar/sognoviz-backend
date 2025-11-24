package com.fraunhofer.sognoviz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraunhofer.sognoviz.DTO.SubstationDTO;
import com.fraunhofer.sognoviz.DTO.VoltageLevelDTO;
import com.fraunhofer.sognoviz.model.DiagramModel;
import com.fraunhofer.sognoviz.util.NetworkToJsonConverter;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.modification.tapchanger.PhaseTapPositionModification;
import com.powsybl.iidm.modification.topology.CreateBranchFeederBaysBuilder;
import com.powsybl.iidm.modification.topology.CreateFeederBayBuilder;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.SubstationPosition;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.metadata.DiagramMetadata;
import com.powsybl.sld.SingleLineDiagram;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating and modifying power grid diagrams using PowSyBl.
 * Handles Network Area Diagrams (NAD), Single Line Diagrams (SLD), and network modifications.
 */
@Slf4j
@Service
public class DiagramGeneratorService {

    private static final Path OUTPUT_DIR = Paths.get("./output/");
    private static final Path STORAGE_DIR = Paths.get("./cgmes/");

    // ==================== NETWORK LOADING ====================

    /**
     * Loads a network from various input formats (ZIP, folder, etc.)
     *
     * @param inputPath Path to the network file or directory
     * @return Loaded Network object
     * @throws IOException if loading fails
     */
    private Network loadNetwork(String inputPath) throws IOException {
        validateFilePath(inputPath);
        Path path = Path.of(inputPath);

        if (!Files.exists(path)) {
            throw new IOException("Network file does not exist: " + inputPath);
        }

        try {
            DataSource dataSource = DataSource.fromPath(path);
            Network network = Network.  read(dataSource);

            if (network == null) {
                throw new IOException("Failed to load network from: " + inputPath);
            }
            return network;
        } catch (Exception e) {
            log.error("Failed to load network from: {}", inputPath, e);
            throw new IOException("Failed to load network from file: " + inputPath, e);
        }
    }


    private Network loadNetworkWithGLProfile(String inputPath) throws IOException {
        validateFilePath(inputPath);
        Path path = Path.of(inputPath);


        if (!Files.exists(path)) {
            throw new IOException("Network file does not exist: " + inputPath);
        }

        Properties importParams = createImportProperties();

        try {
            DataSource dataSource = DataSource.fromPath(path);
            Network network = Network.read(dataSource, importParams);

            if (network == null) {
                throw new IOException("Failed to load network from: " + inputPath);
            }

            long substationsWithPosition = network.getSubstationStream()
                    .filter(s -> s.getExtension(SubstationPosition.class) != null)
                    .count();

            if (substationsWithPosition == 0) {
                throw new IOException("No SubstationPosition extensions found. GL profile may be missing from CGMES files.");
            }

            return network;

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to load network from: {}", inputPath, e);
            throw new IOException("Failed to load network from file: " + inputPath, e);
        }

    }

    /**
     * Creates default import properties for network loading
     */
    private Properties createImportProperties() {
        Properties properties = new Properties();

        // CRITICAL: Use the GL import post-processor
        properties.put("iidm.import.cgmes.post-processors", "cgmesGLImport");

        // Optional: Additional helpful settings
        properties.put("iidm.import.cgmes.store-cgmes-model-as-network-extension", "true");
        properties.put("iidm.import.cgmes.create-busbar-section-for-every-connectivity-node", "true");
        properties.put("iidm.import.cgmes.create-cgmes-export-mapping", "true");

        return properties;
    }


    // ==================== DIAGRAM GENERATION ====================

    /**
     * Generates a basic Network Area Diagram (NAD)
     *
     * @param inputPath Path to network file
     * @return Path to output directory containing SVG
     * @throws IOException if generation fails
     */
    public Path generateNAD(String inputPath) throws IOException {
        Network network = loadNetwork(inputPath);
        ensureDirectoryExists(OUTPUT_DIR);

        Path svgFile = OUTPUT_DIR.resolve("network.svg");
        NetworkAreaDiagram.draw(network, svgFile);

        log.info("Generated NAD at: {}", svgFile);
        return OUTPUT_DIR;
    }

    /**
     * Generates comprehensive NAD with multiple JSON metadata files for map visualization
     *
     * @param inputPath Path to network file
     * @return Path to temporary output directory containing all generated files
     * @throws IOException if generation fails
     */
    public Path generateNadForMap(String inputPath) throws IOException {
        Network network = loadNetworkWithGLProfile(inputPath);

        // Ensure OUTPUT_DIR exists first
        ensureDirectoryExists(OUTPUT_DIR);

        // Create unique temp directory inside OUTPUT_DIR
        Path outputD = Files.createTempDirectory(OUTPUT_DIR, "nad_");

        // Generate SVG diagram
        Path svgFile = outputD.resolve("network.svg");
        NetworkAreaDiagram.draw(network, svgFile);

        // Generate all JSON metadata files
        generateJsonMetadataFiles(network, outputD);

        log.info("Generated NAD with map data at: {}", outputD);
        return outputD;
    }

    /**
     * Generates all JSON metadata files for network visualization
     */
    private void generateJsonMetadataFiles(Network network, Path outputDir) throws IOException {
        // Network metadata (substations with voltage levels)
        writeJsonFile(
                NetworkToJsonConverter.convertNetworkToJson(network),
                outputDir.resolve("substation_locations.json")
        );

        // Substation positions
        writeJsonFile(
                NetworkToJsonConverter.convertSubstationPositionsToJson(network),
                outputDir.resolve("substation_positions.json")
        );

        // Line data with flows
        writeJsonFile(
                NetworkToJsonConverter.convertLinesToJson(network),
                outputDir.resolve("line_locations.json")
        );

        // Line positions
        writeJsonFile(
                NetworkToJsonConverter.convertLinePositionsToJson(network),
                outputDir.resolve("line_positions.json")
        );
    }

    /**
     * Generates Single Line Diagram (SLD) based on type
     *
     * @param inputPath Path to network file
     * @param type Type of diagram: "substation", "voltage", or "all"
     * @param id ID of substation or voltage level (ignored if type is "all")
     * @return Path to output directory
     * @throws IOException if generation fails
     */
    public Path generateSLD(String inputPath, String type, String id) throws IOException {
        Network network = loadNetwork(inputPath);
        ensureDirectoryExists(OUTPUT_DIR);

        Path svgFile = OUTPUT_DIR.resolve("sld.svg");

        switch (type.toLowerCase()) {
            case "substation":
                SingleLineDiagram.drawSubstation(network, id, svgFile);
                log.info("Generated SLD for substation: {}", id);
                break;

            case "voltage":
                SingleLineDiagram.drawVoltageLevel(network, id, svgFile);
                log.info("Generated SLD for voltage level: {}", id);
                break;

            default:
                List<String> substationIds = network.getSubstationStream()
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                SingleLineDiagram.drawMultiSubstations(network, substationIds, svgFile);
                log.info("Generated SLD for all substations");
        }

        return OUTPUT_DIR;
    }

    /**
     * Retrieves network structure data (substations and voltage levels)
     *
     * @param inputPath Path to network file
     * @return Map containing lists of SubstationDTO and VoltageLevelDTO
     * @throws IOException if loading fails
     */
    public Map<String, Object> getSldData(String inputPath) throws IOException {
        Network network = loadNetwork(inputPath);

        List<SubstationDTO> substations = network.getSubstationStream()
                .map(s -> new SubstationDTO(
                        s.getId(),
                        s.getOptionalName().orElse(""),
                        s.getCountry().orElse(Country.DE)
                ))
                .collect(Collectors.toList());

        List<VoltageLevelDTO> voltageLevels = network.getVoltageLevelStream()
                .map(vl -> new VoltageLevelDTO(
                        vl.getId(),
                        vl.getOptionalName().orElse(""),
                        vl.getNominalV(),
                        vl.getTopologyKind()
                ))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("substations", substations);
        result.put("voltageLevels", voltageLevels);

        log.info("Retrieved SLD data: {} substations, {} voltage levels",
                substations.size(), voltageLevels.size());
        return result;
    }

    // ==================== NETWORK MODIFICATIONS ====================

    /**
     * Removes a connectable element from the network
     */
    public Path removeConnectable(String equipmentId, DiagramModel model)
            throws IOException {


        return executeModification(model, (network, nadParams, outputDir) -> {
            Connectable<?> connectable = network.getConnectable(equipmentId);

            if (connectable == null) {
                throw new IOException("Connectable not found: " + equipmentId);
            }

            connectable.remove();
            log.info("Removed connectable: {}", equipmentId);
        });
    }

//    /**
//     * Creates a new load in the network
//     */
//    public Path createLoad(String inputPath, String loadId, String busOrBusbarId,
//                           double p0, double q0, String voltageLevelId, String diagramId)
//            throws IOException {
//
//        return executeModification(inputPath, diagramId, (network, nadParams, outputDir) -> {
//            VoltageLevel voltageLevel = getVoltageLevelOrThrow(network, voltageLevelId);
//
//            LoadAdder loadAdder = voltageLevel.newLoad()
//                    .setId(loadId)
//                    .setName(loadId)
//                    .setP0(p0)
//                    .setQ0(q0)
//                    .setLoadType(LoadType.UNDEFINED);
//
//            new CreateFeederBayBuilder()
//                    .withInjectionAdder(loadAdder)
//                    .withBusOrBusbarSectionId(busOrBusbarId)
//                    .build()
//                    .apply(network, true);
//
//            log.info("Created load: {} at voltage level: {}", loadId, voltageLevelId);
//        });
//    }
//
//    /**
//     * Creates a new generator in the network
//     */
//    public Path createGenerator(String inputPath, String generatorId, String busOrBusbarId,
//                                double targetP, double targetV, double minP, double maxP,
//                                String voltageLevelId, String diagramId) throws IOException {
//
//        return executeModification(inputPath, diagramId, (network, nadParams, outputDir) -> {
//            VoltageLevel voltageLevel = getVoltageLevelOrThrow(network, voltageLevelId);
//
//            GeneratorAdder generatorAdder = voltageLevel.newGenerator()
//                    .setId(generatorId)
//                    .setName(generatorId)
//                    .setTargetP(targetP)
//                    .setTargetV(targetV)
//                    .setMinP(minP)
//                    .setMaxP(maxP)
//                    .setVoltageRegulatorOn(true)
//                    .setEnergySource(EnergySource.OTHER);
//
//            new CreateFeederBayBuilder()
//                    .withInjectionAdder(generatorAdder)
//                    .withBusOrBusbarSectionId(busOrBusbarId)
//                    .build()
//                    .apply(network, true);
//
//            log.info("Created generator: {} at voltage level: {}", generatorId, voltageLevelId);
//        });
//    }
//
//    /**
//     * Creates a new line connecting two voltage levels
//     */
//    public Path createLine(String inputPath, String lineId,
//                           String bus1Id, String bus2Id,
//                           String voltageLevel1Id, String voltageLevel2Id,
//                           double r, double x, double g1, double b1, double g2, double b2,
//                           String diagramId) throws IOException {
//
//        return executeModification(inputPath, diagramId, (network, nadParams, outputDir) -> {
//            // Validate both voltage levels exist
//            getVoltageLevelOrThrow(network, voltageLevel1Id);
//            getVoltageLevelOrThrow(network, voltageLevel2Id);
//
//            LineAdder lineAdder = network.newLine()
//                    .setId(lineId)
//                    .setName(lineId)
//                    .setVoltageLevel1(voltageLevel1Id)
//                    .setVoltageLevel2(voltageLevel2Id)
//                    .setR(r)
//                    .setX(x)
//                    .setG1(g1)
//                    .setB1(b1)
//                    .setG2(g2)
//                    .setB2(b2);
//
//            new CreateBranchFeederBaysBuilder()
//                    .withBranchAdder(lineAdder)
//                    .withBusOrBusbarSectionId1(bus1Id)
//                    .withBusOrBusbarSectionId2(bus2Id)
//                    .build()
//                    .apply(network, true);
//
//            log.info("Created line: {} between {} and {}", lineId, voltageLevel1Id, voltageLevel2Id);
//        });
//    }
//
//    /**
//     * Creates a new substation in the network
//     */
//    public Path createSubstation(String inputPath, String substationId, String substationName,
//                                 String country, String diagramId) throws IOException {
//
//        return executeModification(inputPath, diagramId, (network, nadParams, outputDir) -> {
//            network.newSubstation()
//                    .setId(substationId)
//                    .setName(substationName)
//                    .setCountry(Country.valueOf(country))
//                    .add();
//
//            log.info("Created substation: {} in country: {}", substationId, country);
//        });
//    }
//
//    /**
//     * Creates a new voltage level within a substation
//     */
//    public Path createVoltageLevel(String inputPath, String substationId,
//                                   String voltageLevelId, String voltageLevelName,
//                                   double nominalV, String topologyKind, String diagramId)
//            throws IOException {
//
//        return executeModification(inputPath, diagramId, (network, nadParams, outputDir) -> {
//            Substation substation = getSubstationOrThrow(network, substationId);
//
//            substation.newVoltageLevel()
//                    .setId(voltageLevelId)
//                    .setName(voltageLevelName)
//                    .setNominalV(nominalV)
//                    .setTopologyKind(TopologyKind.valueOf(topologyKind))
//                    .add();
//
//            log.info("Created voltage level: {} in substation: {}", voltageLevelId, substationId);
//        });
//    }
//
//    /**
//     * Sets the phase tap position of a transformer
//     */
//    public Path setPhaseTapPosition(String inputPath, String transformerId,
//                                    int tapPosition, boolean relative, String diagramId)
//            throws IOException {
//
//        return executeModification(inputPath, diagramId, (network, nadParams, outputDir) -> {
//            new PhaseTapPositionModification(transformerId, tapPosition, relative)
//                    .apply(network);
//
//            log.info("Set phase tap position for transformer: {} to position: {} (relative: {})",
//                    transformerId, tapPosition, relative);
//        });
//    }



    // ==================== HELPER METHODS ====================

    /**
     * Executes a network modification with common setup and teardown logic
     */
    @FunctionalInterface
    private interface ModificationAction {
        void execute(Network network, NadParameters nadParams, Path outputDir) throws IOException;
    }

    private Path executeModification(DiagramModel model,
                                     ModificationAction action) throws IOException {
        ensureDirectoryExists(STORAGE_DIR);

        ObjectMapper objectMapper = new ObjectMapper();

        // Convert metadata Map -> DiagramMetadata
        DiagramMetadata metadata = objectMapper.convertValue(model.getMetadata(), DiagramMetadata.class);

        NadParameters nadParameters = new NadParameters()
                .setLayoutParameters(metadata.getLayoutParameters())
                .setSvgParameters(metadata.getSvgParameters());

        Network network = loadNetwork(STORAGE_DIR.resolve(model.getName() + ".zip").toString());

        // Prepare output directory
        Path outputDir = STORAGE_DIR.resolve(model.getName());
        ensureDirectoryExists(outputDir);

        // Execute modification
        action.execute(network, nadParameters, outputDir);

        // Redraw diagram
        NetworkAreaDiagram.draw(network, outputDir, nadParameters, VoltageLevelFilter.NO_FILTER);

        return outputDir;
    }


    /**
     * Retrieves a voltage level or throws an exception if not found
     */
    private VoltageLevel getVoltageLevelOrThrow(Network network, String voltageLevelId)
            throws IOException {
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        if (voltageLevel == null) {
            throw new IOException("Voltage level not found: " + voltageLevelId);
        }
        return voltageLevel;
    }

    /**
     * Retrieves a substation or throws an exception if not found
     */
    private Substation getSubstationOrThrow(Network network, String substationId)
            throws IOException {
        Substation substation = network.getSubstation(substationId);
        if (substation == null) {
            throw new IOException("Substation not found: " + substationId);
        }
        return substation;
    }

    /**
     * Ensures a directory exists, creating it if necessary
     */
    private void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    /**
     * Writes JSON string to a file
     */
    private void writeJsonFile(String json, Path filePath) throws IOException {
        Files.writeString(filePath, json);
        log.debug("Wrote JSON file: {}", filePath);
    }

    /**
     * Validates that a file path is not null or empty
     */
    private void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
    }
}