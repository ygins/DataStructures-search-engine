package project.util;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestingFiles {

    public static final Path DIR = Paths.get(System.getProperty("user.dir"),"testing");
    public static final Path RELATIVE_DIR = Paths.get(URI.create("file:///testing"));

    public static void recursiveDelete(Path dir) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    recursiveDelete(path);
                } else {
                    Files.deleteIfExists(path);
                }
            }
            Files.deleteIfExists(dir);
        }
    }

    public static void deleteTestingFiles() throws IOException{
        if(Files.exists(DIR)){
            recursiveDelete(DIR);
        }
    }

    public static URI createTestingDirectoryURI(String path){
        return URI.create(path);
    }

    public static void assertNoFilesCreated() throws IOException{
        Assertions.assertTrue(emptyExceptForDirectories(DIR));
    }

    public static void assertNotOnDisk(URI uri){
        Assertions.assertFalse(Files.exists(toTestingFile(uri)));
    }

    public static void assertOnDisk(URI uri){
        Assertions.assertTrue(Files.exists(toTestingFile(uri)));
    }

    public static void assertOnDiskAt(URI uri, String path){
        Path fromURI = toTestingFile(uri);
        Path fromString = toTestingFile(URI.create(path));
        Assertions.assertEquals(fromString, fromURI);
        Assertions.assertTrue(Files.exists(fromString));
    }
    private static Path toTestingFile(URI uri){
        StringBuilder fileBuilder = new StringBuilder();
        if(uri.getAuthority()!=null){
            fileBuilder.append(removeInvalidCharacters(uri.getAuthority()));
        }
        if(uri.getPath()!=null){
            fileBuilder.append(removeInvalidCharacters(uri.getPath()));
        }
        String newPathString = fileBuilder.toString();
        if(!newPathString.endsWith(".json")){
            newPathString = newPathString+".json";
        }
        return Path.of(DIR.toString(), newPathString);
    }


    private static String removeInvalidCharacters(String input){
        return input.replaceAll("[:<>\"|@?*]","");
    }

    private static boolean emptyExceptForDirectories(Path dir) throws IOException {
        try(DirectoryStream<Path> children = Files.newDirectoryStream(dir)){
            for(Path child:children){
                if(Files.isDirectory(child) && !emptyExceptForDirectories(child)){
                    return false;
                }
            }
        }
        return true;
    }
}
