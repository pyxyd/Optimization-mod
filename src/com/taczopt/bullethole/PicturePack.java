package com.taczopt.bullethole;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.resource.PathPackResources;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class PicturePack {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String PACK_NAME = "taczopt_picture";
    private static final String NAMESPACE = "taczbullethole";
    private static final String SPRITE_PATH = "block/bullet_hole";
    private static final String STAGED_ROOT = "assets/" + NAMESPACE + "/textures/" + SPRITE_PATH + ".png";
    private static final String PREFERRED = "bullet_hole.png";

    private static final String BUNDLED = "/assets/taczopt_picture/" + PREFERRED;

    private static final String MIRROR =
            "https://cdn.jsdelivr.net/gh/pyxyd/Optimization-mod@main/src/resources/default-bullet-hole.png";
    private static final int MAX_DOWNLOAD = 1 << 20;
    private static final int TIMEOUT_MS = 8000;

    private static Path root;
    private static boolean staged;
    private static boolean logged;

    private PicturePack() {
    }

    public static Path root() {
        if (root == null) {
            root = FMLPaths.GAMEDIR.get().resolve("mods").resolve("Bullet-hole-picture");
        }
        return root;
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        stage();
        event.addRepositorySource(new Source());
        refreshAsync();
    }

    private static Path stagedRoot() {
        stage();
        return root().resolve("assets");
    }

    private static void stage() {
        if (staged) {
            return;
        }
        staged = true;

        Path base = root();
        Path assets = base.resolve("assets");
        Path target = base.resolve(STAGED_ROOT);
        try {
            Path chosen = pickImage(base);
            boolean playerSupplied = chosen != null;
            if (!playerSupplied) {
                chosen = bundledDefault(base);
            }
            if (chosen == null) {
                logMissing();
                return;
            }

            deleteRecursively(assets);
            Files.createDirectories(target.getParent());
            Files.copy(chosen, target, StandardCopyOption.REPLACE_EXISTING);
            writeMeta(base);

            if (!logged) {
                logged = true;
                LOGGER.info("[taczopt] picture pack: using {} from {} -> {}",
                        playerSupplied ? "your" : "the bundled", chosen, STAGED_ROOT);
            }
        } catch (Throwable t) {
            LOGGER.warn("[taczopt] picture pack staging failed for {} -- custom bullet hole disabled", base, t);
        }
    }

    private static Path pickImage(Path base) {
        List<Path> images = images(base);
        return images.isEmpty() ? null : images.get(0);
    }

    private static Path bundledDefault(Path base) {
        InputStream in = PicturePack.class.getResourceAsStream(BUNDLED);
        if (in == null) {
            return null;
        }
        Path out = base.resolve(PREFERRED);
        try {
            Files.createDirectories(base);
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            return Files.size(out) > 0L ? out : null;
        } catch (IOException e) {
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    private static void refreshAsync() {
        Thread worker = new Thread(PicturePack::refresh, "taczopt-picture-mirror");
        worker.setDaemon(true);
        worker.start();
    }

    private static void refresh() {
        Path base = root();
        if (Files.isRegularFile(base.resolve(PREFERRED))) {
            return;
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(MIRROR).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "taczopt");
            if (conn.getResponseCode() != 200) {
                return;
            }
            byte[] data = readCapped(conn);
            if (!isPng(data)) {
                LOGGER.info("[taczopt] picture mirror returned no usable png -- the bundled one is in use");
                return;
            }
            Files.createDirectories(base);
            Files.write(base.resolve(PREFERRED), data);
            LOGGER.info("[taczopt] picture mirror fetched {} bytes into {} -- applies from the next launch", data.length, base);
        } catch (Throwable ignored) {

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static byte[] readCapped(HttpURLConnection conn) throws IOException {
        InputStream in = conn.getInputStream();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_DOWNLOAD) {
                    return new byte[0];
                }
            }
            return out.toByteArray();
        } finally {
            closeQuietly(in);
        }
    }

    private static boolean isPng(byte[] data) {
        return data.length > 8
                && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G'
                && (data[4] & 0xFF) == 0x0D && (data[5] & 0xFF) == 0x0A
                && (data[6] & 0xFF) == 0x1A && (data[7] & 0xFF) == 0x0A;
    }

    private static void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException ignored) {

        }
    }

    private static List<Path> images(Path base) {
        List<Path> out = new ArrayList<Path>();
        if (!Files.isDirectory(base)) {
            return out;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".png")) {
                    continue;
                }
                try {
                    if (Files.size(p) <= 0L) {
                        continue;
                    }
                } catch (IOException ignored) {
                    continue;
                }
                out.add(p);
            }
        } catch (IOException e) {
            return out;
        }
        Collections.sort(out, (a, b) -> {
            boolean pa = PREFERRED.equalsIgnoreCase(a.getFileName().toString());
            boolean pb = PREFERRED.equalsIgnoreCase(b.getFileName().toString());
            if (pa != pb) {
                return pa ? -1 : 1;
            }
            return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
        });
        return out;
    }

    private static void writeMeta(Path base) throws IOException {
        String meta = "{\n"
                + "  \"pack\": {\n"
                + "    \"pack_format\": 15,\n"
                + "    \"description\": \"Optimization for Tacz - bullet hole picture (auto-generated)\"\n"
                + "  }\n"
                + "}\n";
        Files.write(base.resolve("pack.mcmeta"), meta.getBytes("UTF-8"));
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        List<Path> all = new ArrayList<Path>();
        collect(dir, all);
        for (int i = all.size() - 1; i >= 0; i--) {
            Files.deleteIfExists(all.get(i));
        }
        Files.deleteIfExists(dir);
    }

    private static void collect(Path dir, List<Path> out) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                out.add(p);
                if (Files.isDirectory(p)) {
                    collect(p, out);
                }
            }
        }
    }

    private static void deleteEmptyDirectories(Path dir) {
        try {
            if (!Files.isDirectory(dir)) {
                return;
            }
            List<Path> all = new ArrayList<Path>();
            collect(dir, all);
            for (int i = all.size() - 1; i >= 0; i--) {
                Path p = all.get(i);
                if (Files.isDirectory(p)) {
                    try (DirectoryStream<Path> s = Files.newDirectoryStream(p)) {
                        if (!s.iterator().hasNext()) {
                            Files.deleteIfExists(p);
                        }
                    }
                }
            }
        } catch (IOException ignored) {

        }
    }

    private static void logMissing() {
        if (logged) {
            return;
        }
        logged = true;
        LOGGER.info("[taczopt] no .png in {} and no bundled default -- guns outside the tacz namespace keep the TaCZ bullet hole", root());
    }

    private static final class Source implements RepositorySource {

        @Override
        public void m_7686_(Consumer<Pack> consumer) {
            final Path base = root();
            try {
                Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
                    @Override
                    public net.minecraft.server.packs.PackResources m_247679_(String packId) {
                        return new PathPackResources(packId, false, stagedRoot());
                    }
                };
                Pack pack = Pack.m_245429_(
                        PACK_NAME,
                        Component.m_237113_("Optimization for Tacz - bullet hole picture"),
                        true,
                        supplier,
                        PackType.CLIENT_RESOURCES,
                        Pack.Position.TOP,
                        PackSource.f_10528_);
                if (pack != null) {
                    consumer.accept(pack);
                }
            } catch (Throwable t) {
                LOGGER.warn("[taczopt] picture pack registration failed for {}", base, t);
            }
        }
    }
}
