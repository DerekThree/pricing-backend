package com.pricing.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractAlignmentTests {

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ResourceLoader resourceLoader;

	@Test
	void keepsSpringDocSpecAlignedWithOpenApiYaml() throws Exception {
		JsonNode contractSpec = loadContractSpec();
		JsonNode generatedSpec = loadGeneratedSpec();
		assertEquals(
				comparablePaths(contractSpec.path("paths"), contractSpec, contractSpec.path("paths")),
				comparablePaths(generatedSpec.path("paths"), generatedSpec, contractSpec.path("paths")),
				"OpenAPI paths should match"
		);
		assertEquals(
				comparableSchemas(contractSpec.path("components").path("schemas"), contractSpec),
				comparableSchemas(generatedSpec.path("components").path("schemas"), generatedSpec),
				"OpenAPI schemas should match"
		);
	}

	private JsonNode loadContractSpec() throws IOException {
		Resource contract = resourceLoader.getResource("classpath:/static/openapi.yaml");
		return YAML_MAPPER.readTree(contract.getInputStream());
	}

	private JsonNode loadGeneratedSpec() throws Exception {
		MvcResult result = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn();

		return JSON_MAPPER.readTree(result.getResponse().getContentAsString());
	}

	private ArrayNode comparableTags(JsonNode tags) {
		ArrayNode comparable = JSON_MAPPER.createArrayNode();
		List<JsonNode> normalizedTags = new ArrayList<>();

		for (JsonNode tag : tags) {
			if (tag.isTextual()) {
				normalizedTags.add(tag);
				continue;
			}

			ObjectNode normalized = JSON_MAPPER.createObjectNode();
			copyIfPresent(tag, normalized, "name");
			normalizedTags.add(normalized);
		}

		normalizedTags.stream()
				.map(JsonNode::toString)
				.distinct()
				.sorted()
				.map(value -> {
					try {
						return JSON_MAPPER.readTree(value);
					} catch (IOException exception) {
						throw new IllegalStateException("Failed to normalize OpenAPI tag", exception);
					}
				})
				.forEach(comparable::add);
		return comparable;
	}

	private ObjectNode comparablePaths(JsonNode paths, JsonNode root, JsonNode contractPaths) {
		ObjectNode comparable = JSON_MAPPER.createObjectNode();
		paths.fieldNames().forEachRemaining(path -> comparable.set(
				path,
				comparablePathItem(paths.get(path), root, contractPaths.path(path))));
		return comparable;
	}

	private ObjectNode comparablePathItem(
			JsonNode pathItem,
			JsonNode root,
			JsonNode contractPathItem) {
		ObjectNode comparable = JSON_MAPPER.createObjectNode();
		JsonNode sharedParameters = pathItem.path("parameters");

		pathItem.fieldNames().forEachRemaining(method -> {
			if ("parameters".equals(method)) {
				return;
			}

			comparable.set(method, comparableOperation(
					pathItem.get(method),
					sharedParameters,
					root,
					contractPathItem.path(method)));
		});
		return comparable;
	}

	private ObjectNode comparableOperation(
			JsonNode operation,
			JsonNode sharedParameters,
			JsonNode root,
			JsonNode contractOperation) {
		ObjectNode comparable = JSON_MAPPER.createObjectNode();
		comparable.set("tags", comparableTags(operation.path("tags")));
		copyIfPresent(operation, comparable, "summary");
		if (contractOperation.has("operationId")) {
			copyIfPresent(operation, comparable, "operationId");
		}

		if (operation.has("parameters") || sharedParameters.isArray()) {
			ArrayNode parameters = JSON_MAPPER.createArrayNode();
			List<JsonNode> normalizedParameters = new ArrayList<>();

			for (JsonNode parameter : sharedParameters) {
				normalizedParameters.add(comparableParameter(resolveReference(parameter, root), root));
			}

			for (JsonNode parameter : operation.path("parameters")) {
				normalizedParameters.add(comparableParameter(resolveReference(parameter, root), root));
			}

			normalizedParameters.stream().sorted(Comparator.comparing(JsonNode::toString)).forEach(parameters::add);
			comparable.set("parameters", parameters);
		}

		if (operation.has("requestBody")) {
			comparable.set("requestBody", comparableRequestBody(resolveReference(operation.get("requestBody"), root), root));
		}

		comparable.set("responses", comparableResponses(operation.path("responses"), root));
		return comparable;
	}

	private ObjectNode comparableParameter(JsonNode parameter, JsonNode root) {
		ObjectNode comparable = JSON_MAPPER.createObjectNode();
		copyIfPresent(parameter, comparable, "name");
		copyIfPresent(parameter, comparable, "in");
		copyIfPresent(parameter, comparable, "description");
		copyIfPresent(parameter, comparable, "required");

		if (parameter.has("schema")) {
			comparable.set("schema", comparableSchema(parameter.get("schema"), root));
		}

		return comparable;
	}

	private ObjectNode comparableRequestBody(JsonNode requestBody, JsonNode root) {
		ObjectNode comparable = JSON_MAPPER.createObjectNode();
		copyIfPresent(requestBody, comparable, "required");

		JsonNode schema = requestBody.path("content").path("application/json").path("schema");
		if (!schema.isMissingNode()) {
			comparable.set("schema", comparableSchema(schema, root));
		}

		return comparable;
	}

	private ObjectNode comparableResponses(JsonNode responses, JsonNode root) {
		ObjectNode comparable = JSON_MAPPER.createObjectNode();
		responses.fieldNames().forEachRemaining(code -> comparable.set(code, comparableResponse(responses.get(code), root)));
		return comparable;
	}

	private ObjectNode comparableResponse(JsonNode response, JsonNode root) {
		JsonNode resolvedResponse = resolveReference(response, root);
		ObjectNode comparable = JSON_MAPPER.createObjectNode();
		copyIfPresent(resolvedResponse, comparable, "description");

		JsonNode schema = resolvedResponse.path("content").path("application/json").path("schema");
		if (!schema.isMissingNode()) {
			comparable.set("schema", comparableSchema(schema, root));
		}

		return comparable;
	}

	private ObjectNode comparableSchemas(JsonNode schemas, JsonNode root) {
		ObjectNode comparable = JSON_MAPPER.createObjectNode();
		schemas.fieldNames().forEachRemaining(name -> {
			JsonNode schema = schemas.get(name);
			if (!schema.has("properties") && !schema.has("items") && !schema.has("allOf")) {
				return;
			}

			comparable.set(name, comparableSchema(schema, root));
		});
		return comparable;
	}

	private JsonNode comparableSchema(JsonNode schema, JsonNode root) {
		JsonNode resolvedSchema = resolveReference(schema, root);

		if (resolvedSchema.isMissingNode() || resolvedSchema.isNull()) {
			return resolvedSchema;
		}

		if (resolvedSchema.isArray()) {
			ArrayNode comparable = JSON_MAPPER.createArrayNode();
			List<JsonNode> normalizedItems = new ArrayList<>();

			for (JsonNode item : resolvedSchema) {
				normalizedItems.add(comparableSchema(item, root));
			}

			normalizedItems.stream().sorted(Comparator.comparing(JsonNode::toString)).forEach(comparable::add);
			return comparable;
		}

		if (!resolvedSchema.isObject()) {
			return resolvedSchema;
		}

		if (resolvedSchema.has("allOf")) {
			return comparableAllOfSchema(resolvedSchema.get("allOf"), root);
		}

		if (resolvedSchema.has("oneOf")) {
			ObjectNode comparable = JSON_MAPPER.createObjectNode();
			comparable.put("type", "object");
			return comparable;
		}

		ObjectNode comparable = JSON_MAPPER.createObjectNode();
		copyIfPresent(resolvedSchema, comparable, "type");
		copyIfPresent(resolvedSchema, comparable, "format");
		copyIfPresent(resolvedSchema, comparable, "pattern");
		copyIfPresent(resolvedSchema, comparable, "minLength");
		copyIfMeaningfulMaxLength(resolvedSchema, comparable);
		copyIfPresent(resolvedSchema, comparable, "minimum");
		copyIfPresent(resolvedSchema, comparable, "maximum");
		copyIfPresent(resolvedSchema, comparable, "minItems");
		copyIfPresent(resolvedSchema, comparable, "uniqueItems");

		if (resolvedSchema.has("enum")) {
			comparable.set("enum", sortedComparableArray(resolvedSchema.get("enum"), root));
		}

		if (resolvedSchema.has("required")) {
			comparable.set("required", sortedComparableArray(resolvedSchema.get("required"), root));
		}

		if (resolvedSchema.has("items")) {
			comparable.set("items", comparableSchema(resolvedSchema.get("items"), root));
		}

		if (resolvedSchema.has("properties")) {
			ObjectNode properties = JSON_MAPPER.createObjectNode();
			resolvedSchema.path("properties")
					.fieldNames()
					.forEachRemaining(name -> properties.set(name, comparableSchema(resolvedSchema.path("properties").get(name), root)));
			comparable.set("properties", properties);
		}

		return comparable;
	}

	private ObjectNode comparableAllOfSchema(JsonNode allOf, JsonNode root) {
		ObjectNode merged = JSON_MAPPER.createObjectNode();

		for (JsonNode item : allOf) {
			JsonNode comparableItem = comparableSchema(item, root);
			if (!comparableItem.isObject()) {
				continue;
			}

			mergeObjectNode(merged, (ObjectNode) comparableItem);
		}

		return merged;
	}

	private ArrayNode sortedComparableArray(JsonNode array, JsonNode root) {
		ArrayNode comparable = JSON_MAPPER.createArrayNode();
		List<JsonNode> normalizedItems = new ArrayList<>();

		for (JsonNode item : array) {
			normalizedItems.add(item.isContainerNode() ? comparableSchema(item, root) : item);
		}

		normalizedItems.stream().sorted(Comparator.comparing(JsonNode::toString)).forEach(comparable::add);
		return comparable;
	}

	private JsonNode resolveReference(JsonNode node, JsonNode root) {
		if (!node.isObject() || !node.has("$ref")) {
			return node;
		}

		String ref = node.get("$ref").asText();
		if (!ref.startsWith("#/")) {
			return node;
		}

		JsonNode resolved = root;
		for (String segment : ref.substring(2).split("/")) {
			resolved = resolved.path(segment);
		}

		return resolved;
	}

	private void copyIfPresent(JsonNode source, ObjectNode target, String field) {
		if (!source.has(field)) {
			return;
		}

		JsonNode value = source.get(field);
		if (value.isNumber()) {
			target.put(field, value.asText());
			return;
		}

		target.set(field, value);
	}

	private void mergeObjectNode(ObjectNode target, ObjectNode source) {
		source.fields().forEachRemaining(entry -> {
			String field = entry.getKey();
			JsonNode value = entry.getValue();

			if (!target.has(field)) {
				target.set(field, value.deepCopy());
				return;
			}

			JsonNode existing = target.get(field);
			if (existing.isObject() && value.isObject()) {
				mergeObjectNode((ObjectNode) existing, (ObjectNode) value);
				return;
			}

			if (existing.isArray() && value.isArray()) {
				target.set(field, mergeArrays(existing, value));
				return;
			}

			target.set(field, value.deepCopy());
		});
	}

	private ArrayNode mergeArrays(JsonNode left, JsonNode right) {
		ArrayNode merged = JSON_MAPPER.createArrayNode();
		List<JsonNode> normalizedItems = new ArrayList<>();

		left.forEach(normalizedItems::add);
		right.forEach(normalizedItems::add);

		normalizedItems.stream()
				.map(JsonNode::toString)
				.distinct()
				.sorted()
				.map(value -> {
					try {
						return JSON_MAPPER.readTree(value);
					} catch (IOException exception) {
						throw new IllegalStateException("Failed to merge OpenAPI schema arrays", exception);
					}
				})
				.forEach(merged::add);
		return merged;
	}

	private void copyIfMeaningfulMaxLength(JsonNode source, ObjectNode target) {
		if (!source.has("maxLength")) {
			return;
		}

		int maxLength = source.get("maxLength").asInt();
		if (maxLength == Integer.MAX_VALUE) {
			return;
		}

		target.put("maxLength", source.get("maxLength").asText());
	}
}
