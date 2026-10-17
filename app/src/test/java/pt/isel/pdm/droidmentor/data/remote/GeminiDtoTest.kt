package pt.isel.pdm.droidmentor.data.remote // Tests DTO conversion in the network data layer.

import io.ktor.client.call.body // Decodes simulated responses with the real client converter.
import io.ktor.client.engine.config // Builds an engine factory that the client can own and close.
import io.ktor.client.engine.mock.MockEngine // Simulates HTTP without contacting Google.
import io.ktor.client.engine.mock.respond // Builds the simulated HTTP response.
import io.ktor.client.plugins.logging.Logger // Provides a log destination that does not print anything.
import io.ktor.client.request.post // Sends a simulated JSON request.
import io.ktor.client.request.setBody // Passes the request DTO to ContentNegotiation.
import io.ktor.http.ContentType // Marks the simulated response as JSON.
import io.ktor.http.HttpHeaders // Names the response content type header.
import io.ktor.http.content.TextContent // Reads the body produced by the client serializer.
import io.ktor.http.headersOf // Creates JSON response headers for MockEngine.
import java.util.UUID // Generates fictitious sensitive error text in memory.
import kotlinx.coroutines.test.runTest // Runs suspending client operations from JUnit.
import kotlinx.serialization.SerializationException // Identifies failures to decode malformed JSON models.
import kotlinx.serialization.encodeToString // Encodes a DTO using its generated serializer.
import kotlinx.serialization.json.Json // Uses the same JSON options as the app client.
import kotlinx.serialization.json.jsonArray // Reads array elements from encoded JSON.
import kotlinx.serialization.json.jsonObject // Reads named fields from encoded JSON.
import kotlinx.serialization.json.jsonPrimitive // Reads string and number values from encoded JSON.
import org.junit.Assert.assertEquals // Compares harmless test values and JSON field names.
import org.junit.Assert.assertFalse // Checks optional or forbidden fields are absent.
import org.junit.Assert.assertNull // Checks omitted response values remain unknown.
import org.junit.Assert.assertTrue // Checks privacy and decoding failures without exposing raw details.
import org.junit.Test // Marks each method as a JUnit test.
import pt.isel.pdm.droidmentor.data.remote.dto.ApiErrorEnvelope // Reads server error envelopes.
import pt.isel.pdm.droidmentor.data.remote.dto.Content // Builds text turns and separate instructions.
import pt.isel.pdm.droidmentor.data.remote.dto.GenerateContentRequest // Builds the stateless request DTO.
import pt.isel.pdm.droidmentor.data.remote.dto.GenerateContentResponse // Reads generated candidates and metadata.
import pt.isel.pdm.droidmentor.data.remote.dto.GenerationConfig // Supplies optional generation overrides.
import pt.isel.pdm.droidmentor.data.remote.dto.Part // Builds text-only request parts.
import pt.isel.pdm.droidmentor.domain.ConversationTurn // Provides pure domain turns to the mapper.
import pt.isel.pdm.droidmentor.domain.Role // Provides the domain author values.

/** Checks the Gemini JSON contract, domain conversion and real Ktor converter without real credentials. */
class GeminiDtoTest { // Groups only the tests needed by the DTO task.
    private val json = Json { // Decodes fixtures with the options already used by HttpClientFactory.
        ignoreUnknownKeys = true // Accepts server fields outside the supported M2 models.
        explicitNulls = false // Omits nullable fields when they have no value.
    } // Ends the test JSON configuration.

    /** Checks exact field names, separate instructions, full turn order and unchanged text. */
    @Test // Verifies the wire contract rather than just Kotlin data-class equality.
    fun requestContainsSeparateInstructionAndOrderedHistory() { // Encodes a completed exchange and the next user turn.
        val turns = listOf( // Supplies an already ordered history as the future builder will do.
            ConversationTurn(Role.USER, "Explain \"Flow\".\nKeep café and 🍝."), // Includes escaping, Unicode and a line break.
            ConversationTurn(Role.MODEL, " First answer. "), // Includes whitespace that must not be trimmed.
            ConversationTurn(Role.USER, "Show an example."), // Ends the supplied conversation with the next user turn.
        ) // Ends the test history.
        val request = GenerateContentRequest(instruction(), turns.map { it.toContent() }, GenerationConfig(0.7, 512)) // Converts every supplied turn in the data layer.
        val root = json.parseToJsonElement(json.encodeToString(request)).jsonObject // Inspects the exact outgoing JSON.
        assertEquals(setOf("system_instruction", "contents", "generationConfig"), root.keys) // Rejects credentials, session IDs and a camel-case instruction field.
        val system = root.getValue("system_instruction").jsonObject // Reads the instruction separately from contents.
        assertFalse(system.containsKey("role")) // Checks the persona does not pretend to be a conversation participant.
        assertEquals("Test mentor instructions", system.getValue("parts").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content) // Checks the instruction text is in its own part.
        val contents = root.getValue("contents").jsonArray // Reads all encoded conversation turns.
        assertEquals(listOf("user", "model", "user"), contents.map { it.jsonObject.getValue("role").jsonPrimitive.content }) // Checks explicit role mapping and original order.
        assertEquals(turns.map { it.text }, contents.map { it.jsonObject.getValue("parts").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content }) // Checks no turn or text character was dropped.
        val config = root.getValue("generationConfig").jsonObject // Reads the supplied model overrides.
        assertEquals("0.7", config.getValue("temperature").jsonPrimitive.content) // Checks the numeric sampling setting is encoded.
        assertEquals("512", config.getValue("maxOutputTokens").jsonPrimitive.content) // Checks the numeric token limit is encoded.
    } // Ends the request contract test.

    /** Checks absent overrides are omitted instead of sent as null or invented defaults. */
    @Test // Verifies the two supported ways to leave model settings unset.
    fun unsetGenerationSettingsAreOmitted() { // Encodes requests without chosen temperature or token limits.
        val request = GenerateContentRequest(instruction(), listOf(ConversationTurn(Role.USER, "Question").toContent())) // Leaves the optional configuration absent.
        val root = json.parseToJsonElement(json.encodeToString(request)).jsonObject // Reads the serialized request fields.
        assertFalse(root.containsKey("generationConfig")) // Checks no null configuration is sent.
        val withEmptyConfig = json.parseToJsonElement(json.encodeToString(request.copy(generationConfig = GenerationConfig()))).jsonObject // Explicitly supplies an empty settings object.
        assertTrue(withEmptyConfig.getValue("generationConfig").jsonObject.isEmpty()) // Checks empty settings contain no invented temperature or limit.
    } // Ends the optional configuration test.

    /** Checks the request model cannot silently omit the persona or conversation field. */
    @Test // Verifies both required top-level request fields.
    fun requestRequiresInstructionAndContents() { // Attempts to decode incomplete requests.
        expectMalformed { json.decodeFromString<GenerateContentRequest>("""{"contents":[]}""") } // Checks a missing instruction is rejected.
        expectMalformed { json.decodeFromString<GenerateContentRequest>("""{"system_instruction":{"parts":[{"text":"Mentor"}]}}""") } // Checks a missing history field is rejected.
    } // Ends the required request fields test.

    /** Checks multipart text, thought markers, token counts and unknown nested metadata survive decoding. */
    @Test // Uses the server's field names in a representative response fixture.
    fun responsePreservesTextPartsAndUsage() { // Reads supported fields while ignoring extra metadata.
        val payload = """{"candidates":[{"content":{"role":"model","parts":[{"text":"Reasoning summary","thought":true},{"text":"Use Flow."},{"text":" Then collect it.","extra":1}],"extra":true},"finishReason":"STOP","index":0}],"usageMetadata":{"promptTokenCount":12,"candidatesTokenCount":8,"totalTokenCount":25,"thoughtsTokenCount":5,"extra":[]},"modelVersion":"test-model"}""" // Uses only harmless test data and additional unsupported fields.
        val response = json.decodeFromString<GenerateContentResponse>(payload) // Decodes using the generated DTO serializers.
        val candidate = response.candidates.single() // Reads the one generated alternative.
        assertEquals("model", candidate.content?.role) // Checks the wire role is retained.
        assertEquals(listOf("Reasoning summary", "Use Flow.", " Then collect it."), candidate.content?.parts?.map { it.text }) // Checks all text parts remain ordered and unchanged.
        assertEquals(true, candidate.content?.parts?.first()?.thought) // Checks thought text can be identified separately by the data source.
        assertNull(candidate.content?.parts?.get(1)?.thought) // Checks an absent thought marker stays absent.
        assertEquals("STOP", candidate.finishReason) // Checks the stopping reason is preserved.
        assertEquals(12, response.usageMetadata?.promptTokenCount) // Checks input token usage.
        assertEquals(8, response.usageMetadata?.candidatesTokenCount) // Checks answer token usage.
        assertEquals(25, response.usageMetadata?.totalTokenCount) // Checks the reported total is not recalculated.
        assertEquals(5, response.usageMetadata?.thoughtsTokenCount) // Checks thought usage is retained separately.
    } // Ends the complete response test.

    /** Checks omitted optional fields can represent an empty response without fabricated values. */
    @Test // Verifies empty envelopes, empty candidates and partial token metadata.
    fun optionalResponseFieldsStayAbsent() { // Reads valid JSON with no usable generated text.
        val empty = json.decodeFromString<GenerateContentResponse>("{}") // Reads an envelope with no candidate field.
        assertTrue(empty.candidates.isEmpty()) // Checks there is no invented response candidate.
        assertNull(empty.usageMetadata) // Checks missing usage is not treated as zero usage.
        assertNull(empty.promptFeedback) // Checks a missing block signal remains absent.
        val partial = json.decodeFromString<GenerateContentResponse>("""{"candidates":[{}],"usageMetadata":{"promptTokenCount":4}}""") // Reads a candidate without content and incomplete usage.
        assertNull(partial.candidates.single().content) // Checks content is allowed to be absent.
        assertNull(partial.candidates.single().finishReason) // Checks no stopping reason is invented.
        assertEquals(4, partial.usageMetadata?.promptTokenCount) // Checks the provided count survives.
        assertNull(partial.usageMetadata?.totalTokenCount) // Checks the absent total stays unknown.
        val explicitNulls = json.decodeFromString<GenerateContentResponse>("""{"candidates":[{"content":null,"finishReason":null}],"usageMetadata":null,"promptFeedback":null}""") // Reads explicit nulls only for nullable fields.
        assertNull(explicitNulls.candidates.single().content) // Checks nullable content accepts an explicit null.
        assertNull(explicitNulls.usageMetadata) // Checks nullable metadata accepts an explicit null.
    } // Ends the missing response fields test.

    /** Checks a blocked prompt is readable even when the server returns no candidates. */
    @Test // Preserves the signal needed by the future typed error mapper.
    fun promptBlockWithoutCandidatesIsReadable() { // Reads a prompt-level block response.
        val response = json.decodeFromString<GenerateContentResponse>("""{"promptFeedback":{"blockReason":"SAFETY","safetyRatings":[]}}""") // Ignores unsupported safety-rating details while retaining the block reason.
        assertTrue(response.candidates.isEmpty()) // Checks no generated answer is inferred.
        assertEquals("SAFETY", response.promptFeedback?.blockReason) // Checks the block signal is retained.
    } // Ends the prompt block test.

    /** Checks candidate blocks and new server reason names do not fail enum decoding. */
    @Test // Verifies stopping and blocking reasons remain forward-compatible strings.
    fun blockedAndNewFinishReasonsArePreserved() { // Reads candidates without generated content.
        val response = json.decodeFromString<GenerateContentResponse>("""{"candidates":[{"finishReason":"SAFETY"},{"finishReason":"FUTURE_REASON"}],"promptFeedback":{"blockReason":"FUTURE_BLOCK"}}""") // Includes a known block and fictitious future reason names.
        assertEquals(listOf("SAFETY", "FUTURE_REASON"), response.candidates.map { it.finishReason }) // Checks both reason names survive unchanged.
        assertTrue(response.candidates.all { it.content == null }) // Checks neither candidate receives invented content.
        assertEquals("FUTURE_BLOCK", response.promptFeedback?.blockReason) // Checks a future prompt block reason also remains readable.
    } // Ends the reason compatibility test.

    /** Checks unsupported response parts are not converted into fabricated answer text. */
    @Test // Verifies text remains nullable for Gemini's multipart response format.
    fun partsWithoutTextStayReadable() { // Reads empty content and parts that contain no supported text field.
        val emptyContent = json.decodeFromString<GenerateContentResponse>("""{"candidates":[{"content":{}}]}""") // Reads a content object with omitted parts.
        assertTrue(emptyContent.candidates.single().content?.parts?.isEmpty() == true) // Checks omitted parts become an empty list.
        val response = json.decodeFromString<GenerateContentResponse>("""{"candidates":[{"content":{"parts":[{}, {"text":null}, {"unknownPart":{"value":1}}]}}]}""") // Includes empty, explicitly null and unsupported parts.
        val parts = response.candidates.single().content?.parts.orEmpty() // Reads the preserved part positions.
        assertEquals(3, parts.size) // Checks the decoder did not reorder or remove parts.
        assertTrue(parts.all { it.text == null }) // Checks none is mistaken for an answer.
    } // Ends the missing text test.

    /** Checks server errors decode but diagnostic text cannot echo a sensitive error field. */
    @Test // Uses a generated fictitious value rather than a real credential.
    fun apiErrorDecodesWithoutLeakingDiagnosticText() { // Reads an error envelope only in memory.
        val dummy = UUID.randomUUID().toString() // Generates raw server text that must not be printed.
        val payload = """{"error":{"code":400,"message":"$dummy","status":"INVALID_ARGUMENT","details":[{"extra":true}]}}""" // Simulates an error containing sensitive text and unsupported details.
        val envelope = json.decodeFromString<ApiErrorEnvelope>(payload) // Decodes supported fields without logging the envelope.
        assertEquals(400, envelope.error.code) // Checks the numeric code survives.
        assertEquals("INVALID_ARGUMENT", envelope.error.status) // Checks the server status survives.
        assertTrue("Raw error text must stay in memory", envelope.error.message == dummy) // Avoids an equality assertion that would print the generated value on failure.
        assertTrue("Error diagnostics must hide raw fields", !envelope.error.toString().contains(dummy)) // Checks the error body's safe representation.
        assertTrue("Envelope diagnostics must hide raw fields", !envelope.toString().contains(dummy)) // Checks the generated envelope representation also hides the body fields.
    } // Ends the error envelope and privacy test.

    /** Checks structurally invalid response fields remain decoding failures for the future mapper. */
    @Test // Distinguishes malformed wire data from a valid empty response.
    fun malformedResponseIsRejected() { // Supplies invalid JSON and wrong types for supported fields.
        val payloads = listOf( // Lists independent malformed response cases.
            "{", // Represents a truncated JSON object.
            """{"candidates":{}}""", // Uses an object where the candidate array is required.
            """{"candidates":null}""", // Uses null for a non-nullable array field.
            """{"candidates":[{"content":"wrong"}]}""", // Uses a string where a content object is required.
            """{"candidates":[{"content":{"parts":{}}}]}""", // Uses an object where the parts array is required.
            """{"candidates":[{"content":{"parts":[{"text":{}}]}}]}""", // Uses an object where text is required.
            """{"usageMetadata":{"totalTokenCount":[]}}""", // Uses an array where an integer count is required.
        ) // Ends the malformed fixture list.
        for (payload in payloads) { // Checks every malformed field without printing raw response text.
            expectMalformed { json.decodeFromString<GenerateContentResponse>(payload) } // Requires a serialization failure instead of a fabricated empty answer.
        } // Ends the malformed response loop.
    } // Ends the malformed response test.

    /** Checks an unrelated or structurally invalid error body cannot masquerade as an API error envelope. */
    @Test // Verifies the envelope's required object and numeric code type.
    fun malformedErrorEnvelopeIsRejected() { // Decodes invalid error envelopes.
        expectMalformed { json.decodeFromString<ApiErrorEnvelope>("{}") } // Checks the required error field is present.
        expectMalformed { json.decodeFromString<ApiErrorEnvelope>("""{"error":"wrong"}""") } // Checks the error field contains an object.
        expectMalformed { json.decodeFromString<ApiErrorEnvelope>("""{"error":{"code":[]}}""") } // Checks a numeric code is not replaced by another type.
    } // Ends the malformed error envelope test.

    /** Checks the actual Ktor converter uses these DTOs and the exact instruction field name. */
    @Test // Exercises request encoding and response decoding through HttpClientFactory.
    fun ktorUsesDtoContractForRequestsAndResponses() = runTest { // Runs a simulated Gemini request with the production JSON setup.
        val engine = MockEngine.config { // Lets the client create and own the simulated engine.
            addHandler { request -> // Inspects the real serialized request body.
                val root = json.parseToJsonElement((request.body as TextContent).text).jsonObject // Reads the JSON produced by ContentNegotiation.
                assertEquals(setOf("system_instruction", "contents"), root.keys) // Checks the exact required fields and omitted optional configuration.
                assertEquals("user", root.getValue("contents").jsonArray.single().jsonObject.getValue("role").jsonPrimitive.content) // Checks domain conversion reached the wire.
                respond("""{"candidates":[{"content":{"parts":[{"text":"Answer"}]},"finishReason":"STOP"}],"extra":true}""", headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) // Returns a response with an unsupported extra field.
            } // Ends the simulated request handler.
        } // Ends the engine factory configuration.
        val silentLogger = object : Logger { // Supplies a destination that never prints test data.
            /** Ignores all messages because this test runs with logging disabled. */
            override fun log(message: String) = Unit // Keeps the test logger silent.
        } // Ends the silent logger.
        HttpClientFactory.create(false, engine, silentLogger).use { client -> // Uses the actual app configuration without contacting Google.
            val response = client.post("v1beta/models/test-model:generateContent") { // Sends the stateless DTO to a harmless simulated endpoint.
                setBody(GenerateContentRequest(instruction(), listOf(ConversationTurn(Role.USER, "Question").toContent()))) // Requires instructions and full supplied contents.
            }.body<GenerateContentResponse>() // Decodes the simulated response through the installed converter.
            assertEquals("Answer", response.candidates.single().content?.parts?.single()?.text) // Checks the supported answer text survives the client pipeline.
        } // Closes the client and its owned simulated engine.
    } // Ends the Ktor integration test.

    private fun instruction() = Content(parts = listOf(Part(text = "Test mentor instructions"))) // Provides test-only instructions without implementing the real mentor persona.

    private fun expectMalformed(action: () -> Unit) { // Checks decoding failure without exposing exception messages.
        var rejected = false // Records whether serialization rejected the invalid data.
        try { // Attempts the selected decode operation.
            action() // Runs the fixture decoder.
        } catch (error: SerializationException) { // Catches only the expected JSON-model failure in test code.
            rejected = true // Records that invalid wire data was rejected.
        } // Ends the expected failure handling.
        assertTrue("Malformed JSON models must be rejected", rejected) // Reports only a generic failure message.
    } // Ends the malformed-data helper.
} // Ends the Gemini DTO test class.
