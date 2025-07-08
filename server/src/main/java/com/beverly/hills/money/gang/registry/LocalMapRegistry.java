package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.spawner.map.CompleteMap;
import com.beverly.hills.money.gang.spawner.map.GameMapAssets;
import com.beverly.hills.money.gang.spawner.map.MapData;
import com.beverly.hills.money.gang.util.HashUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class LocalMapRegistry implements MapRegistry, Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(LocalMapRegistry.class);
  private final Map<String, CompleteMap> maps = new ConcurrentHashMap<>();

  private final String mapFolder;

  public LocalMapRegistry(final String mapFolder) throws IOException {
    this.mapFolder = mapFolder;
    /*
       I have to do this because I have to get map folders from within jar.
       In this code, we get all tmx files and THEN get the folder name (which is also a map name)
     */
    var onlineMaps = new PathMatchingResourcePatternResolver()
        .getResources("classpath*:" + mapFolder + "/*/online_map.tmx");
    if (onlineMaps.length == 0) {
      throw new IllegalStateException("No maps found");
    }

    Arrays.stream(onlineMaps)
        .map(resource -> {
          try {
            // get folder name from the file name
            var folder = resource.getURI().toString().replace("/" + resource.getFilename(), "");
            // get map name
            return folder.substring(folder.lastIndexOf("/") + 1);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .forEach(mapName -> maps.put(mapName, loadCompleteMap(mapName)));
  }

  public LocalMapRegistry() throws IOException {
    this("maps");
  }

  public List<String> getMapNames() {
    return new ArrayList<>(maps.keySet());
  }

  private CompleteMap loadCompleteMap(final String name) {
    LOG.info("Load map '{}'", name);
    return CompleteMap.builder()
        .mapData(loadMapData(name))
        .assets(loadMapAssets(name))
        .build();
  }


  private MapData loadMapData(final String name) {
    String mapFile = mapFolder + "/" + name + "/online_map.tmx";
    try (InputStream is = LocalMapRegistry.class.getClassLoader().getResourceAsStream(mapFile)) {
      JAXBContext context = JAXBContext.newInstance(MapData.class);
      Unmarshaller unmarshaller = context.createUnmarshaller();
      return (MapData) unmarshaller.unmarshal(is);
    } catch (Exception e) {
      throw new IllegalStateException("Can't load map " + name);
    }
  }

  private GameMapAssets loadMapAssets(final String name) {
    String mapTMX = mapFolder + "/" + name + "/online_map.tmx";
    String atlasPNG = mapFolder + "/" + name + "/atlas.png";
    String atlasTSX = mapFolder + "/" + name + "/atlas.tsx";
    var atlasTSXBytes = loadBytes(atlasTSX);
    var atlasPNGBytes = loadBytes(atlasPNG);
    var mapTMXBytes = loadBytes(mapTMX);
    var hash = HashUtil.hash(List.of(atlasTSXBytes, atlasPNGBytes, mapTMXBytes));
    return GameMapAssets.builder()
        .atlasTsx(atlasTSXBytes)
        .atlasPng(atlasPNGBytes)
        .onlineMapTmx(mapTMXBytes)
        .hash(hash)
        .build();
  }

  private byte[] loadBytes(final String fileName) {
    try (InputStream is = LocalMapRegistry.class.getClassLoader().getResourceAsStream(fileName)) {
      if (is == null) {
        throw new IllegalStateException("Can't find input stream for " + fileName);
      }
      return IOUtils.toByteArray(is);
    } catch (Exception e) {
      throw new IllegalStateException("Can't load bytes for " + fileName);
    }
  }

  @Override
  public Optional<CompleteMap> getMap(String name) {
    return Optional.ofNullable(maps.get(name));
  }

  @Override
  public void close() {
    LOG.info("Close map registry");
    maps.clear();
  }

}
