# SognoViz

A visualization tool for CGMES (Common Grid Model Exchange Standard) files, built with Spring Boot and PowSyBL libraries.

## Overview

SognoViz enables users to generate and visualize electrical network diagrams from CGMES files. It supports multiple diagram types including Network Area Diagrams (NAD), Network Map Diagrams, and Single Line Diagrams (SLD).


---

## API Endpoints

### Diagram Generation

#### Generate Network Area Diagram (NAD)

```http
POST /api/diagrams/nad
Content-Type: multipart/form-data
```

**Parameters:**
- `file` (MultipartFile) - CGMES file

**Response:** `DiagramModel`

**Flow:**  
Uploaded CGMES file, stored locally. From this file, network is loaded to generate metadata and SVG.

---

#### Generate Single Line Diagram (SLD)

**Step 1: Get Selection Data**

```http
POST /api/diagrams/sld/selectionData
Content-Type: multipart/form-data
```

**Parameters:**
- `file` (MultipartFile) - CGMES file

**Response:**
```json
{
  "id": "uuid",
  "sldData": {
    "substations": [...],
    "voltageLevels": [...]
  }
}
```

**Step 2: Generate SLD**

```http
POST /api/diagrams/sld?type={type}&selectionId={selectionId}&id={id}
```

**Query Parameters:**
- `type` (String, required) - Type of diagram: `"substation"` or `"voltagelevel"`
- `selectionId` (String, optional) - ID of substation or voltage level
- `id` (String, required) - Diagram ID from selection data endpoint

**Response:** `DiagramModel`

**Flow:**  
Uploaded CGMES file, storee locally, retrieve available substations and voltage levels for SLD generation. Generate a Single Line Diagram for a specific substation or voltage level.

---

#### Generate Network Map Diagram

```http
POST /api/diagrams/map
Content-Type: multipart/form-data
```

**Parameters:**
- `file` (MultipartFile) - CGMES file

**Response:** `NetworkMapModel`

**Flow:**  
User uploads CGMES file, stored it locally. From this file, network is loaded to generate metadata and SVG. Custom method is created to generate supporting JSONs such as line, line position, and substation position which are not provided by PowSyBL library.

---

### Diagram Management

#### Get All Diagrams

```http
GET /api/diagrams
```

Retrieve a list of all generated diagrams.

**Response:** `List<DiagramModel>`

---

#### Get Diagram by ID

```http
GET /api/diagrams/{id}
```

Retrieve a specific diagram by its ID.

**Path Parameters:**
- `id` (String) - Diagram identifier

**Response:** `DiagramModel`

---

#### Update Diagram

```http
PUT /api/diagrams/{id}
Content-Type: application/json
```

Update an existing diagram's metadata or content.

**Path Parameters:**
- `id` (String) - Diagram identifier

**Request Body:** `DiagramModel`

**Response:** `DiagramModel`

---

#### Delete Diagram

```http
DELETE /api/diagrams/{id}
```

Delete a diagram by its ID.

**Path Parameters:**
- `id` (String) - Diagram identifier

---

### Network Modifications

#### Remove Connectable

**Step 1: Update Diagram**

First, update the diagram using the Update Diagram API call.

**Step 2: Remove Connectable**

```http
POST /api/modifications/remove-connectable?equipmentId={equipmentId}&id={id}
```

Remove a connectable element (e.g., line, transformer) from an existing diagram and regenerate it.

**Query Parameters:**
- `equipmentId` (String, required) - ID of the equipment to remove
- `id` (String, required) - Diagram ID

**Response:** `DiagramModel`

**Flow:**  
Fetch the DiagramModel by ID to get the updated metadata, then load the complete network using stored CGMES file as network cannot be loaded using only JSON and SVG. Execute remove operation on loaded network. Use the updated network with removed component and the updated metadata that was fetched earlier with diagram model which has updated layout data to generate the SVG and new metadata.

---

## Data Models

### DiagramModel

```json
{
  "id": "String",           // Unique identifier
  "name": "String",         // Diagram name
  "diagramType": "String",  // NAD, SLD, or MAP
  "svg": "String",          // SVG content
  "metadata": "String"      // JSON metadata
}
```

### NetworkMapModel

```json
{
  // Extends DiagramModel
  "id": "String",
  "name": "String",
  "diagramType": "String",
  "svg": "String",
  "metadata": "String",
  "line": "String",                  // Line location data (JSON)
  "linePosition": "String",          // Line positioning data (JSON)
  "substation": "String",            // Substation location data (JSON)
  "substationPosition": "String"     // Substation positioning data (JSON)
}
```

---

## Getting Started

### Prerequisites

- CGMES-compliant network files

### Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`


### Issues
#### Unable to export CGMES files after modifcation
- There is no way to update the network with modified metadata json that is received from fronend. Without update Network there is no way to export the CGMES files.
- However we are still able to generate the svg and updated metadata by using draw() method where we pass layout and other parameters as nadParams.
  ```java
  NetworkAreaDiagram.draw(network, outputDir, nadParameters, VoltageLevelFilter.NO_FILTER);
  ```
---

#### Connectable id issue 
- When front end provide a connectable id it's not present in available connectables in loaded network.
- may be the component selected in front end is not connectable yet to figure out

### Future work
#### Implement method for adding elements

  
