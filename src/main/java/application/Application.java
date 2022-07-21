package application;

import util.CardFinder;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Application {
    public static void main(String[] args) throws URISyntaxException, IOException {
        Map<String, BufferedImage> valueMap = new HashMap<>();
        Map<String, BufferedImage> suitMap = new HashMap<>();
        URI uri = Application.class.getResource("/cards").toURI();
        Path startPath;
        boolean isJar = uri.getScheme().equals("jar");
        if (isJar) {
            try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                startPath = fileSystem.getPath("/cards");
            }
        } else startPath = Paths.get(uri);
        List<Path> pathList = Files.walk(startPath).filter(Files::isRegularFile).collect(Collectors.toList());
        for (Path path : pathList) {
            BufferedImage image;
            if (isJar) image = ImageIO.read(Application.class.getResourceAsStream(path.toString()));
            else image = ImageIO.read(new File(path.toString()));
            String name = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.'));
            if (path.getName(path.getNameCount() - 2).toString().equals("values")) valueMap.put(name, image);
            else suitMap.put(name, image);
        }
        CardFinder cardFinder = new CardFinder(valueMap, suitMap);
        List<File> files = Files.find(Paths.get(args[0]), Integer.MAX_VALUE, (path, attributes) ->
                attributes.isRegularFile()
                && (path.getFileName().toString().matches(".*\\.png")
                || path.getFileName().toString().matches(".*\\.jpg")
                || path.getFileName().toString().matches(".*\\.jpeg")))
                .map(e -> new File(e.toString()))
                .collect(Collectors.toList());
        long time = System.currentTimeMillis();
        for (File file : files) {
            BufferedImage image = ImageIO.read(file);
            System.out.println(file.getName() + " - " + cardFinder.getCardSequence(image));
        }
        System.out.println(((System.currentTimeMillis() - time) / 1000) + " seconds spent for " + files.size() + " files");
    }
}