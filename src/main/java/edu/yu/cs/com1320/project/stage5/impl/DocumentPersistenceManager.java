package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.*;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import jakarta.xml.bind.DatatypeConverter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {

    private final File baseDir;
    private final Gson gson;

    public DocumentPersistenceManager(File baseDir) {
        this.baseDir = Objects.requireNonNullElseGet(baseDir, () -> Paths.get(System.getProperty("user.dir")).toFile());
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Document.class, documentSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(Document.class, documentDeserializer());
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();
    }

    private JsonSerializer<Document> documentSerializer() {
        return (doc, $, $$) -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("uri", doc.getKey().toString());
            if (doc.getDocumentTxt() != null) {
                obj.addProperty("text", doc.getDocumentTxt());
                JsonObject wordMapJson = new JsonObject();
                for (Map.Entry<String, Integer> wordCounts : doc.getWordMap().entrySet()) {
                    wordMapJson.add(wordCounts.getKey(), new JsonPrimitive(wordCounts.getValue()));
                }
                obj.add("word-map", wordMapJson);
            } else {//binary doc
                obj.addProperty("binary-data", DatatypeConverter.printBase64Binary(doc.getDocumentBinaryData()));
            }
            return obj;
        };
    }

    private JsonDeserializer<Document> documentDeserializer() {
        return (json, type, context) -> {
            JsonObject object = json.getAsJsonObject();
            URI uri = URI.create(object.get("uri").getAsString());
            if (object.has("text")) {//text doc
                Map<String, Integer> wordMap = new HashMap<>();
                JsonObject wordMapObject = object.get("word-map").getAsJsonObject();
                for (Map.Entry<String, JsonElement> wordToCount : wordMapObject.entrySet()) {
                    wordMap.put(wordToCount.getKey(), wordToCount.getValue().getAsInt());
                }
                return new DocumentImpl(uri, object.get("text").getAsString(), wordMap);
            } else {
                String encodedData = object.get("binary-data").getAsString();
                byte[] binaryData = DatatypeConverter.parseBase64Binary(encodedData);
                return new DocumentImpl(uri, binaryData);
            }
        };
    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
        if (uri == null || val == null) {
            throw new IllegalArgumentException("Null input!");
        }
        Path destination = transformURIToFileSystem(uri);
        write(destination, gson.toJson(val, Document.class));
    }

    private void write(Path destination, String json) throws IOException {
        if (!Files.exists(destination)) {
            Files.createDirectories(destination.getParent());
            Files.createFile(destination);
        }
        Files.writeString(destination, json);
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        if (uri == null) {
            throw new IllegalArgumentException("Null input to deserialize!");
        }
        Path origin = transformURIToFileSystem(uri);
        if(!Files.exists(origin)){
            return null;
        }
        JsonObject jsonObject;
        try {
            jsonObject = JsonParser.parseString(Files.readString(origin)).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return gson.fromJson(jsonObject, Document.class);
    }

    @Override
    public boolean delete(URI uri) throws IOException {
        Path fileToDelete = transformURIToFileSystem(uri);
        return Files.deleteIfExists(fileToDelete);
    }

    private Path transformURIToFileSystem(URI uri) {
        StringBuilder fileBuilder = new StringBuilder();
        if (uri.getAuthority() != null) {
            fileBuilder.append(removeInvalidCharacters(uri.getAuthority()));
        }
        if (uri.getPath() != null) {
            fileBuilder.append(removeInvalidCharacters(uri.getPath()));
        }
        String newPathString = fileBuilder.toString();
        if (!newPathString.endsWith(".json")) {
            newPathString = newPathString + ".json";
        }
        return Path.of(baseDir.getAbsolutePath(), newPathString);
    }

    private static String removeInvalidCharacters(String input) {
        return input.replaceAll("[:<@>\"|?*]", "");
    }
}
