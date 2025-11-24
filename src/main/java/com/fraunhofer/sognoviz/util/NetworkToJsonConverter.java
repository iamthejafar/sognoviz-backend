package com.fraunhofer.sognoviz.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.Coordinate;
import com.powsybl.iidm.network.extensions.LinePosition;
import com.powsybl.iidm.network.extensions.SubstationPosition;
import com.powsybl.iidm.network.extensions.SubstationPositionAdder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkToJsonConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Converts a Network to JSON format with substations and their voltage levels
     */
    public static String convertNetworkToJson(Network network) {
        try {
            List<SubstationData> substationDataList = extractSubstationData(network);
            return mapper.writeValueAsString(substationDataList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert network to JSON", e);
        }
    }

    /**
     * Converts substations with geographical positions to JSON (SPOS format)
     */
    public static String convertSubstationPositionsToJson(Network network) {
        try {
            List<SubstationPositionData> sposDataList = extractSubstationPositions(network);

            if(sposDataList.isEmpty()) throw new RuntimeException("CGMES Does not have GL Data");
            return mapper.writeValueAsString(sposDataList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert substation positions to JSON", e);
        }
    }

    /**
     * Converts lines with their properties to JSON (lineMap format)
     */
    public static String convertLinesToJson(Network network) {
        try {
            List<LineLocation> lineLocationList = extractLineData(network);
            return mapper.writeValueAsString(lineLocationList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert lines to JSON", e);
        }
    }

    /**
     * Converts lines with geographical positions to JSON (linePOS format)
     */
    public static String convertLinePositionsToJson(Network network) {
        try {
            List<LinePositionData> linePosDataList = extractLinePositions(network);

            if(linePosDataList.isEmpty()) throw new RuntimeException("CGMES Does not have GL Data");
            return mapper.writeValueAsString(linePosDataList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert line positions to JSON", e);
        }
    }

    /**
     * Extracts substation data from the network
     */
    public static List<SubstationData> extractSubstationData(Network network) {
        List<SubstationData> substationDataList = new ArrayList<>();

        network.getSubstationStream().forEach(substation -> {
            SubstationData substationData = new SubstationData();
            substationData.setId(substation.getId());
            substationData.setName(substation.getNameOrId());

            List<VoltageLevelData> voltageLevelDataList = new ArrayList<>();
            substation.getVoltageLevelStream().forEach(voltageLevel -> {
                VoltageLevelData voltageLevelData = new VoltageLevelData();
                voltageLevelData.setId(voltageLevel.getId());
                voltageLevelData.setSubstationId(substation.getId());
                voltageLevelData.setNominalV(voltageLevel.getNominalV());

                voltageLevelDataList.add(voltageLevelData);
            });

            substationData.setVoltageLevels(voltageLevelDataList);
            substationDataList.add(substationData);
        });

        return substationDataList;
    }

    /**
     * Extracts substation positions (SPOS format)
     */


    /**
     * Extracts line data (lineMap format)
     */
    public static List<LineLocation> extractLineData(Network network) {
        List<LineLocation> lineLocationList = new ArrayList<>();

        network.getLineStream().forEach(line -> {
            LineLocation lineLocation = new LineLocation();
            lineLocation.setId(line.getId());
            lineLocation.setName(line.getNameOrId());

            Terminal terminal1 = line.getTerminal1();
            Terminal terminal2 = line.getTerminal2();

            lineLocation.setVoltageLevelId1(terminal1.getVoltageLevel().getId());
            lineLocation.setVoltageLevelId2(terminal2.getVoltageLevel().getId());
            lineLocation.setTerminal1Connected(terminal1.isConnected());
            lineLocation.setTerminal2Connected(terminal2.isConnected());

            // Get power flows (P in MW)
            lineLocation.setP1(terminal1.isConnected() ? terminal1.getP() : Double.NaN);
            lineLocation.setP2(terminal2.isConnected() ? terminal2.getP() : Double.NaN);

            // Get current flows (I in Amperes)
            lineLocation.setI1(terminal1.isConnected() ? terminal1.getI() : Double.NaN);
            lineLocation.setI2(terminal2.isConnected() ? terminal2.getI() : Double.NaN);

            lineLocationList.add(lineLocation);
        });

        return lineLocationList;
    }


    public static List<SubstationPositionData> extractSubstationPositions(Network network) {
        List<SubstationPositionData> sposDataList = new ArrayList<>();

        network.getSubstationStream().forEach(substation -> {

// Extension is now attached to substation and can be retrieved later
            SubstationPosition substationPosition =
                    substation.getExtension(SubstationPosition.class);

            if (substationPosition != null) {
                Coordinate coordinate = substationPosition.getCoordinate();

                SubstationPositionData sposData = new SubstationPositionData();
                sposData.setId(substation.getId());

                CoordinateData coordData = new CoordinateData();
                coordData.setLat(coordinate.getLatitude());
                coordData.setLon(coordinate.getLongitude());

                sposData.setCoordinate(coordData);
                sposDataList.add(sposData);
            }
        });

        return sposDataList;
    }
    /**
     * Extracts line positions (linePOS format)
     */
    public static List<LinePositionData> extractLinePositions(Network network) {
        List<LinePositionData> linePosDataList = new ArrayList<>();
        network.getLineStream().forEach(line -> {
            LinePosition<Line> linePosition = line.getExtension(LinePosition.class);

            if (linePosition != null) {
                LinePositionData linePosData = new LinePositionData();
                linePosData.setId(line.getId());

                List<CoordinateData> coordinates = linePosition.getCoordinates().stream()
                        .map(coord -> {
                            CoordinateData coordData = new CoordinateData();
                            coordData.setLat(coord.getLatitude());
                            coordData.setLon(coord.getLongitude());
                            return coordData;
                        })
                        .collect(Collectors.toList());

                linePosData.setCoordinates(coordinates);
                linePosDataList.add(linePosData);
            }
        });

        return linePosDataList;
    }

    // ==================== Data Transfer Objects ====================

    public static class SubstationData {
        private String id;
        private String name;
        private List<VoltageLevelData> voltageLevels;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<VoltageLevelData> getVoltageLevels() {
            return voltageLevels;
        }

        public void setVoltageLevels(List<VoltageLevelData> voltageLevels) {
            this.voltageLevels = voltageLevels;
        }
    }

    public static class VoltageLevelData {
        private String id;
        private String substationId;
        private double nominalV;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSubstationId() {
            return substationId;
        }

        public void setSubstationId(String substationId) {
            this.substationId = substationId;
        }

        public double getNominalV() {
            return nominalV;
        }

        public void setNominalV(double nominalV) {
            this.nominalV = nominalV;
        }
    }

    public static class SubstationPositionData {
        private String id;
        private CoordinateData coordinate;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public CoordinateData getCoordinate() {
            return coordinate;
        }

        public void setCoordinate(CoordinateData coordinate) {
            this.coordinate = coordinate;
        }
    }

    public static class CoordinateData {
        private double lat;
        private double lon;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }
    }

    public static class LineLocation {
        private String id;
        private String voltageLevelId1;
        private String voltageLevelId2;
        private String name;
        private boolean terminal1Connected;
        private boolean terminal2Connected;
        private double p1;
        private double p2;
        private double i1;
        private double i2;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getVoltageLevelId1() {
            return voltageLevelId1;
        }

        public void setVoltageLevelId1(String voltageLevelId1) {
            this.voltageLevelId1 = voltageLevelId1;
        }

        public String getVoltageLevelId2() {
            return voltageLevelId2;
        }

        public void setVoltageLevelId2(String voltageLevelId2) {
            this.voltageLevelId2 = voltageLevelId2;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isTerminal1Connected() {
            return terminal1Connected;
        }

        public void setTerminal1Connected(boolean terminal1Connected) {
            this.terminal1Connected = terminal1Connected;
        }

        public boolean isTerminal2Connected() {
            return terminal2Connected;
        }

        public void setTerminal2Connected(boolean terminal2Connected) {
            this.terminal2Connected = terminal2Connected;
        }

        public double getP1() {
            return p1;
        }

        public void setP1(double p1) {
            this.p1 = p1;
        }

        public double getP2() {
            return p2;
        }

        public void setP2(double p2) {
            this.p2 = p2;
        }

        public double getI1() {
            return i1;
        }

        public void setI1(double i1) {
            this.i1 = i1;
        }

        public double getI2() {
            return i2;
        }

        public void setI2(double i2) {
            this.i2 = i2;
        }
    }

    public static class LinePositionData {
        private String id;
        private List<CoordinateData> coordinates;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<CoordinateData> getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(List<CoordinateData> coordinates) {
            this.coordinates = coordinates;
        }
    }
}