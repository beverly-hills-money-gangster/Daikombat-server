package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.spawner.map.CompleteMap;
import com.beverly.hills.money.gang.spawner.map.GameMapAssets;
import com.beverly.hills.money.gang.spawner.map.MapData;
import com.beverly.hills.money.gang.util.HashUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LocalMapRegistry implements MapRegistry, Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(LocalMapRegistry.class);
  private final Map<String, CompleteMap> maps = new ConcurrentHashMap<>();

  private final String mapFolder;

  public LocalMapRegistry(final String mapFolder) throws URISyntaxException {
    this.mapFolder = mapFolder;
    URL url = LocalMapRegistry.class.getClassLoader().getResource(mapFolder);
    if (url == null) {
      throw new IllegalStateException("Map folder not found");
    }
    File[] folderEntries = new File(url.toURI()).listFiles();
    List<String> folders = new ArrayList<>();
    if (folderEntries == null) {
      throw new IllegalStateException("No map files found");
    }
    for (File file : folderEntries) {
      if (file.isDirectory()) {
        folders.add(file.getName());
      }
    }
    if (folders.isEmpty()) {
      throw new IllegalStateException("At least one map should be present");
    }
    // init maps
    folders.forEach(mapName -> maps.put(mapName, loadCompleteMap(mapName)));
  }

  public LocalMapRegistry() throws URISyntaxException {
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
