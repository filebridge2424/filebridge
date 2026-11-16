package com.filebridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class FileBridgeApplication {

    private static final Logger log = LoggerFactory.getLogger(FileBridgeApplication.class);

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(FileBridgeApplication.class, args);
    }

    /**
     * Automatically loads .env files from the working directory, root directory,
     * or backend directory if present, setting them as system properties so Spring Boot can resolve them.
     */
    private static void loadDotEnv() {
        List<Path> candidatePaths = List.of(
                Paths.get(".env"),
                Paths.get("../.env"),
                Paths.get("backend/.env"),
                Paths.get("../backend/.env")
        );

        for (Path path : candidatePaths) {
            if (Files.isRegularFile(path)) {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    log.info("Loading environment configuration from: {}", path.toAbsolutePath().normalize());
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int equalIdx = line.indexOf('=');
                        if (equalIdx > 0) {
                            String key = line.substring(0, equalIdx).trim();
                            String value = line.substring(equalIdx + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                if (value.length() >= 2) {
                                    value = value.substring(1, value.length() - 1);
                                }
                            }
                            if (System.getProperty(key) == null) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                    return;
                } catch (IOException e) {
                    log.warn("Could not read .env file at {}: {}", path, e.getMessage());
                }
            }
        }
    }
}
